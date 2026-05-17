package io.agentscope.tutorial.chapter15.sandbox;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.harness.agent.filesystem.model.ExecuteResponse;
import io.agentscope.harness.agent.sandbox.ExecResult;
import io.agentscope.harness.agent.sandbox.Sandbox;
import io.agentscope.harness.agent.sandbox.SandboxState;
import io.agentscope.harness.agent.sandbox.WorkspaceSpec;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 内存沙箱实现 for testing purposes.
 *
 * <p>This sandbox simulates code execution without using Docker or external processes.
 * It provides:
 * <ul>
 *   <li>Isolated in-memory file system per workspace</li>
 *   <li>Command execution simulation</li>
 *   <li>Timeout control</li>
 *   <li>Output capture</li>
 * </ul>
 *
 * <p>For production use, consider using Docker-based sandbox (DockerSandbox)
 * or cloud-based sandbox (E2bSandbox, DaytonaSandbox).
 */
@Slf4j
public class InMemorySandbox implements Sandbox {

    private static final AtomicLong SANDBOX_COUNTER = new AtomicLong(1000);
    private static final int DEFAULT_TIMEOUT = 60;

    private final String sandboxId;
    private final ConcurrentHashMap<String, byte[]> fileSystem = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> environment = new ConcurrentHashMap<>();
    private volatile boolean running = false;
    private volatile boolean stopped = false;

    private final int defaultTimeout;
    private WorkspaceSpec workspaceSpec;

    public InMemorySandbox() {
        this(DEFAULT_TIMEOUT);
    }

    public InMemorySandbox(int defaultTimeout) {
        this.sandboxId = "inmemory-sandbox-" + SANDBOX_COUNTER.incrementAndGet();
        this.defaultTimeout = defaultTimeout;
        this.environment.put("USER", "sandbox");
        this.environment.put("HOME", "/workspace");
        this.environment.put("PATH", "/usr/local/bin:/usr/bin:/bin");
        log.info("Created InMemorySandbox: {}", sandboxId);
    }

    @Override
    public void start() throws Exception {
        if (running) {
            log.warn("Sandbox {} is already running", sandboxId);
            return;
        }
        running = true;
        stopped = false;
        log.info("Started InMemorySandbox: {}", sandboxId);

        // Initialize default files if workspace spec is provided
        if (workspaceSpec != null && workspaceSpec.getEntries() != null) {
            for (Map.Entry<String, io.agentscope.harness.agent.sandbox.layout.WorkspaceEntry> entry
                    : workspaceSpec.getEntries().entrySet()) {
                if (entry.getValue() instanceof io.agentscope.harness.agent.sandbox.layout.FileEntry fileEntry) {
                    String path = entry.getKey();
                    if (!path.startsWith("/")) {
                        path = workspaceSpec.getRoot() + "/" + path;
                    }
                    writeFile(path, fileEntry.getContent());
                }
            }
        }
    }

    @Override
    public void stop() throws Exception {
        stopped = true;
        log.info("Stopped InMemorySandbox: {}", sandboxId);
    }

    @Override
    public void shutdown() throws Exception {
        running = false;
        stopped = true;
        fileSystem.clear();
        environment.clear();
        log.info("Shutdown InMemorySandbox: {}", sandboxId);
    }

    @Override
    public void close() throws Exception {
        stop();
        shutdown();
    }

    @Override
    public boolean isRunning() {
        return running && !stopped;
    }

    @Override
    public SandboxState getState() {
        return new InMemorySandboxState(sandboxId, workspaceSpec);
    }

    /**
     * Executes a command in the sandbox.
     *
     * <p>Supported commands:
     * <ul>
     *   <li>ls [path] - list directory contents</li>
     *   <li>cat <file> - read file content</li>
     *   <li>echo <text> - echo text to stdout</li>
     *   <li>pwd - print working directory</li>
     *   <li>mkdir <path> - create directory</li>
     *   <li>touch <file> - create empty file</li>
     *   <li>rm <path> - remove file or directory</li>
     *   <li>write <path> <content> - write content to file</li>
     *   <li>env - show environment variables</li>
     *   <li>export VAR=value - set environment variable</li>
     * </ul>
     *
     * @param runtimeContext per-call agent context (may be null)
     * @param command shell command to execute
     * @param timeoutSeconds max wait time (null uses default)
     */
    @Override
    public ExecResult exec(RuntimeContext runtimeContext, String command, Integer timeoutSeconds)
            throws Exception {
        if (!isRunning()) {
            throw new IllegalStateException("Sandbox " + sandboxId + " is not running");
        }

        int effectiveTimeout = (timeoutSeconds != null) ? timeoutSeconds : defaultTimeout;
        long startTime = System.currentTimeMillis();

        try {
            String[] parts = parseCommand(command);
            String cmd = parts[0];
            String[] args = parts.length > 1 ? java.util.Arrays.copyOfRange(parts, 1, parts.length) : new String[0];

            String output;
            int exitCode;

            switch (cmd.toLowerCase()) {
                case "ls":
                    output = executeLs(args);
                    exitCode = 0;
                    break;
                case "cat":
                    output = executeCat(args);
                    exitCode = output.startsWith("Error") ? 1 : 0;
                    break;
                case "echo":
                    output = executeEcho(args);
                    exitCode = 0;
                    break;
                case "pwd":
                    output = workspaceSpec != null ? workspaceSpec.getRoot() : "/workspace";
                    exitCode = 0;
                    break;
                case "mkdir":
                    output = executeMkdir(args);
                    exitCode = output.startsWith("Error") ? 1 : 0;
                    break;
                case "touch":
                    output = executeTouch(args);
                    exitCode = output.startsWith("Error") ? 1 : 0;
                    break;
                case "rm":
                    output = executeRm(args);
                    exitCode = output.startsWith("Error") ? 1 : 0;
                    break;
                case "write":
                    output = executeWrite(args);
                    exitCode = output.startsWith("Error") ? 1 : 0;
                    break;
                case "env":
                    output = executeEnv();
                    exitCode = 0;
                    break;
                case "export":
                    output = executeExport(args);
                    exitCode = 0;
                    break;
                default:
                    // Simulate running other commands (for demo purposes)
                    output = simulateCommandExecution(cmd, args);
                    exitCode = 0;
            }

            long executionTime = System.currentTimeMillis() - startTime;
            log.debug("Executed '{}' in sandbox {}: exitCode={}, time={}ms",
                    command, sandboxId, exitCode, executionTime);

            return new ExecResult(exitCode, output, "", false);

        } catch (Exception e) {
            log.error("Error executing command in sandbox {}: {}", sandboxId, command, e);
            return new ExecResult(1, "", "Error: " + e.getMessage(), false);
        }
    }

    /**
     * Executes code with language-specific interpreter.
     */
    public ExecResult executeCode(String code, String language, Integer timeoutSeconds) {
        if (!isRunning()) {
            return new ExecResult(1, "", "Sandbox is not running", false);
        }

        int effectiveTimeout = (timeoutSeconds != null) ? timeoutSeconds : defaultTimeout;
        String output;
        int exitCode;

        switch (language.toLowerCase()) {
            case "java":
                // Simulate Java execution (in real implementation, would useCompiler + process)
                if (code.contains("public static void main")) {
                    output = "Java code executed (simulated)\n[Note: Real execution requires Docker sandbox]";
                    exitCode = 0;
                } else {
                    output = "Java compilation: No main method found";
                    exitCode = 1;
                }
                break;
            case "python":
            case "python3":
                // Simulate Python execution
                if (code.contains("print(")) {
                    output = code.lines()
                            .filter(line -> line.contains("print("))
                            .map(line -> {
                                int start = line.indexOf("print(") + 6;
                                int end = line.lastIndexOf(")");
                                if (start > 5 && end > start) {
                                    return line.substring(start, end).replace("\"", "").replace("'", "");
                                }
                                return "";
                            })
                            .filter(s -> !s.isEmpty())
                            .reduce((a, b) -> a + "\n" + b)
                            .orElse("(no output)");
                    exitCode = 0;
                } else {
                    output = "Python executed (simulated)\n[Note: Real execution requires Docker sandbox]";
                    exitCode = 0;
                }
                break;
            case "javascript":
            case "js":
                // Simulate JavaScript execution
                if (code.contains("console.log")) {
                    output = code.lines()
                            .filter(line -> line.contains("console.log"))
                            .map(line -> {
                                int start = line.indexOf("console.log(") + 11;
                                int end = line.lastIndexOf(")");
                                if (start > 10 && end > start) {
                                    return line.substring(start, end).replace("\"", "").replace("'", "");
                                }
                                return "";
                            })
                            .filter(s -> !s.isEmpty())
                            .reduce((a, b) -> a + "\n" + b)
                            .orElse("(no output)");
                    exitCode = 0;
                } else {
                    output = "JavaScript executed (simulated)\n[Note: Real execution requires Docker sandbox]";
                    exitCode = 0;
                }
                break;
            default:
                output = "Unsupported language: " + language + "\nSupported: java, python, javascript";
                exitCode = 1;
        }

        return new ExecResult(exitCode, output, "", false);
    }

    @Override
    public InputStream persistWorkspace() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // Simple serialization format: path|size|data\n
        for (Map.Entry<String, byte[]> entry : fileSystem.entrySet()) {
            String line = entry.getKey() + "|" + entry.getValue().length + "|"
                    + StandardCharsets.UTF_8.decode(java.nio.ByteBuffer.wrap(entry.getValue())).toString()
                    + "\n";
            baos.write(line.getBytes(StandardCharsets.UTF_8));
        }
        return new ByteArrayInputStream(baos.toByteArray());
    }

    @Override
    public void hydrateWorkspace(InputStream archive) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int len;
        while ((len = archive.read(buffer)) != -1) {
            baos.write(buffer, 0, len);
        }
        String content = baos.toString(StandardCharsets.UTF_8);

        fileSystem.clear();
        for (String line : content.split("\n")) {
            if (line.isEmpty()) continue;
            String[] parts = line.split("\\|", 3);
            if (parts.length >= 3) {
                String path = parts[0];
                byte[] data = parts[2].getBytes(StandardCharsets.UTF_8);
                fileSystem.put(path, data);
            }
        }
        log.info("Hydrated workspace with {} files", fileSystem.size());
    }

    // === File system operations ===

    public void writeFile(String path, String content) {
        fileSystem.put(normalizePath(path), content.getBytes(StandardCharsets.UTF_8));
    }

    public String readFile(String path) {
        byte[] data = fileSystem.get(normalizePath(path));
        if (data == null) {
            return null;
        }
        return StandardCharsets.UTF_8.decode(java.nio.ByteBuffer.wrap(data)).toString();
    }

    public boolean fileExists(String path) {
        return fileSystem.containsKey(normalizePath(path));
    }

    public void deleteFile(String path) {
        fileSystem.remove(normalizePath(path));
    }

    public Map<String, byte[]> getFileSystem() {
        return java.util.Collections.unmodifiableMap(fileSystem);
    }

    public String getSandboxId() {
        return sandboxId;
    }

    // === Private helper methods ===

    private String[] parseCommand(String command) {
        return command.trim().split("\\s+");
    }

    private String normalizePath(String path) {
        if (path == null) {
            return "/";
        }
        // Simple normalization: remove trailing slashes
        while (path.endsWith("/") && path.length() > 1) {
            path = path.substring(0, path.length() - 1);
        }
        return path.isEmpty() ? "/" : path;
    }

    private String executeLs(String[] args) {
        String targetDir = (args.length > 0) ? args[0] : "/";
        String normalizedDir = normalizePath(targetDir);

        StringBuilder sb = new StringBuilder();
        String dirPrefix = normalizedDir.equals("/") ? "" : normalizedDir + "/";

        fileSystem.keySet().stream()
                .filter(path -> path.startsWith(dirPrefix))
                .map(path -> path.substring(dirPrefix.length()))
                .filter(path -> !path.contains("/"))
                .sorted()
                .forEach(name -> sb.append(name).append("\n"));

        return sb.length() > 0 ? sb.toString() : "";
    }

    private String executeCat(String[] args) {
        if (args.length == 0) {
            return "Error: cat requires a file path";
        }
        String content = readFile(args[0]);
        if (content == null) {
            return "Error: file not found: " + args[0];
        }
        return content;
    }

    private String executeEcho(String[] args) {
        return String.join(" ", args);
    }

    private String executeMkdir(String[] args) {
        if (args.length == 0) {
            return "Error: mkdir requires a path";
        }
        // In in-memory sandbox, directories are created implicitly when files are written
        return "";
    }

    private String executeTouch(String[] args) {
        if (args.length == 0) {
            return "Error: touch requires a file path";
        }
        if (!fileExists(args[0])) {
            writeFile(args[0], "");
        }
        return "";
    }

    private String executeRm(String[] args) {
        if (args.length == 0) {
            return "Error: rm requires a path";
        }
        String path = normalizePath(args[0]);

        // Check for dangerous operations
        if (path.equals("/") || path.equals("") || path.equals("/workspace")) {
            return "Error: cannot remove root or workspace";
        }

        // Remove file and all files under it
        fileSystem.keySet().removeIf(p -> p.equals(path) || p.startsWith(path + "/"));
        return "";
    }

    private String executeWrite(String[] args) {
        if (args.length < 2) {
            return "Error: write requires path and content";
        }
        String path = args[0];
        String content = commandArgToString(args, 1);
        writeFile(path, content);
        return "";
    }

    private String executeEnv() {
        StringBuilder sb = new StringBuilder();
        environment.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> sb.append(e.getKey()).append("=").append(e.getValue()).append("\n"));
        return sb.toString();
    }

    private String executeExport(String[] args) {
        if (args.length == 0) {
            return "Error: export requires VAR=value";
        }
        String[] kv = args[0].split("=", 2);
        if (kv.length == 2) {
            environment.put(kv[0], kv[1]);
            return "";
        }
        return "Error: export requires VAR=value format";
    }

    private String simulateCommandExecution(String cmd, String[] args) {
        // Simulate other commands for demonstration
        return "Simulated execution of: " + cmd + " " + String.join(" ", args)
                + "\n[Note: Real execution requires Docker sandbox]";
    }

    private String commandArgToString(String[] args, int start) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (i > start) sb.append(" ");
            sb.append(args[i]);
        }
        return sb.toString();
    }

    /**
     * 设置 workspace spec for this sandbox.
     */
    public void setWorkspaceSpec(WorkspaceSpec workspaceSpec) {
        this.workspaceSpec = workspaceSpec;
    }

    /**
     * Gets the workspace spec.
     */
    public WorkspaceSpec getWorkspaceSpec() {
        return workspaceSpec;
    }

    /**
     * Inner class for sandbox state serialization.
     */
    public static class InMemorySandboxState extends SandboxState {
        private final String id;

        public InMemorySandboxState(String sandboxId, WorkspaceSpec spec) {
            this.id = sandboxId;
            if (spec != null) {
                setWorkspaceSpec(spec.copy());
            }
        }

        public String getId() {
            return id;
        }
    }
}