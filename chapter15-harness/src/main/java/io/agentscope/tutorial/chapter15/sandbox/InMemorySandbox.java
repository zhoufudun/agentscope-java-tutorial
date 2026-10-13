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
 * 内存沙箱实现（用于测试和演示）.
 *
 * <h2>概述</h2>
 * <p>InMemorySandbox 是 Harness Sandbox 接口的内存实现。它在内存中模拟
 * 代码执行环境，不依赖 Docker 或外部进程，非常适合开发、测试和演示。
 *
 * <h2>核心特性</h2>
 * <ul>
 *   <li><b>内存文件系统</b>：每个沙箱有独立的内存文件系统</li>
 *   <li><b>命令模拟</b>：支持常用 shell 命令的模拟执行</li>
 *   <li><b>超时控制</b>：支持执行超时设置</li>
 *   <li><b>输出捕获</b>：捕获并返回命令输出</li>
 *   <li><b>状态持久化</b>：支持工作空间的持久化和恢复</li>
 * </ul>
 *
 * <h2>支持的命令</h2>
 * <ul>
 *   <li>ls [path] - 列出目录内容</li>
 *   <li>cat [file] - 读取文件内容</li>
 *   <li>echo [text] - 输出文本</li>
 *   <li>pwd - 打印当前目录</li>
 *   <li>mkdir [path] - 创建目录</li>
 *   <li>touch [file] - 创建空文件</li>
 *   <li>rm [path] - 删除文件/目录</li>
 *   <li>write [path] [content] - 写入文件</li>
 *   <li>env - 显示环境变量</li>
 *   <li>export VAR=value - 设置环境变量</li>
 * </ul>
 *
 * <h2>生产环境建议</h2>
 * <p>本实现仅用于演示目的。在生产环境中，建议使用：
 * <ul>
 *   <li>DockerSandbox - 基于 Docker 容器的隔离沙箱</li>
 *   <li>E2bSandbox - 云端沙箱服务</li>
 *   <li>DaytonaSandbox - 云端开发环境</li>
 * </ul>
 *
 * <h2>线程安全</h2>
 * <p>使用 ConcurrentHashMap 存储文件系统和环境变量，支持并发访问。
 *
 * @author AgentScope Tutorial
 * @see Sandbox
 * @see SandboxState
 */
@Slf4j
public class InMemorySandbox implements Sandbox {

    /**
     * 沙箱 ID 计数器.
     * 使用 AtomicLong 保证线程安全的 ID 生成。
     */
    private static final AtomicLong SANDBOX_COUNTER = new AtomicLong(1000);

    /**
     * 默认超时时间（秒）.
     */
    private static final int DEFAULT_TIMEOUT = 60;

    /**
     * 沙箱唯一标识符.
     * 格式：inmemory-sandbox-{序号}
     */
    private final String sandboxId;

    /**
     * 内存文件系统.
     * 键为文件路径，值为文件内容（字节数组）。
     * 使用 ConcurrentHashMap 保证线程安全。
     */
    private final ConcurrentHashMap<String, byte[]> fileSystem = new ConcurrentHashMap<>();

    /**
     * 环境变量.
     * 模拟沙箱内的环境变量。
     */
    private final ConcurrentHashMap<String, String> environment = new ConcurrentHashMap<>();

    /**
     * 运行状态标志.
     * 使用 volatile 保证可见性。
     */
    private volatile boolean running = false;

    /**
     * 停止状态标志.
     */
    private volatile boolean stopped = false;

    /**
     * 默认超时时间（秒）.
     */
    private final int defaultTimeout;

    /**
     * 工作空间配置.
     * 定义工作空间的根目录和初始文件。
     */
    private WorkspaceSpec workspaceSpec;

    /**
     * 使用默认超时时间创建沙箱.
     */
    public InMemorySandbox() {
        this(DEFAULT_TIMEOUT);
    }

    /**
     * 使用指定超时时间创建沙箱.
     *
     * <p>构造函数会：
     * <ol>
     *   <li>生成唯一的沙箱 ID</li>
     *   <li>初始化默认环境变量</li>
     * </ol>
     *
     * @param defaultTimeout 默认执行超时时间（秒）
     */
    public InMemorySandbox(int defaultTimeout) {
        this.sandboxId = "inmemory-sandbox-" + SANDBOX_COUNTER.incrementAndGet();
        this.defaultTimeout = defaultTimeout;
        // 初始化默认环境变量
        this.environment.put("USER", "sandbox");           // 用户名
        this.environment.put("HOME", "/workspace");        // 主目录
        this.environment.put("PATH", "/usr/local/bin:/usr/bin:/bin");  // PATH
        log.info("创建 InMemorySandbox: {}", sandboxId);
    }

    /**
     * 启动沙箱.
     *
     * <p>启动沙箱并初始化工作空间。如果配置了 WorkspaceSpec，
     * 会根据配置创建初始文件。
     *
     * @throws Exception 如果启动失败
     */
    @Override
    public void start() throws Exception {
        if (running) {
            log.warn("沙箱 {} 已经在运行", sandboxId);
            return;
        }
        running = true;
        stopped = false;
        log.info("启动 InMemorySandbox: {}", sandboxId);

        // 如果提供了 workspace spec，初始化默认文件
        if (workspaceSpec != null && workspaceSpec.getEntries() != null) {
            for (Map.Entry<String, io.agentscope.harness.agent.sandbox.layout.WorkspaceEntry> entry
                    : workspaceSpec.getEntries().entrySet()) {
                // 处理文件条目
                if (entry.getValue() instanceof io.agentscope.harness.agent.sandbox.layout.FileEntry fileEntry) {
                    String path = entry.getKey();
                    // 确保路径是绝对路径
                    if (!path.startsWith("/")) {
                        path = workspaceSpec.getRoot() + "/" + path;
                    }
                    writeFile(path, fileEntry.getContent());
                }
            }
        }
    }

    /**
     * 停止沙箱.
     *
     * <p>停止沙箱但保留文件系统状态。
     * 与 shutdown() 不同，stop() 后可以重新 start()。
     *
     * @throws Exception 如果停止失败
     */
    @Override
    public void stop() throws Exception {
        stopped = true;
        log.info("停止 InMemorySandbox: {}", sandboxId);
    }

    /**
     * 关闭沙箱.
     *
     * <p>完全关闭沙箱并清理所有资源：
     * <ul>
     *   <li>清空文件系统</li>
     *   <li>清空环境变量</li>
     *   <li>设置为非运行状态</li>
     * </ul>
     *
     * @throws Exception 如果关闭失败
     */
    @Override
    public void shutdown() throws Exception {
        running = false;
        stopped = true;
        fileSystem.clear();
        environment.clear();
        log.info("关闭 InMemorySandbox: {}", sandboxId);
    }

    /**
     * 关闭资源（AutoCloseable 接口）.
     *
     * @throws Exception 如果关闭失败
     */
    @Override
    public void close() throws Exception {
        stop();
        shutdown();
    }

    /**
     * 检查沙箱是否正在运行.
     *
     * @return true 如果沙箱正在运行且未停止
     */
    @Override
    public boolean isRunning() {
        return running && !stopped;
    }

    /**
     * 获取沙箱状态.
     *
     * @return 沙箱状态对象，包含 ID 和 WorkspaceSpec
     */
    @Override
    public SandboxState getState() {
        return new InMemorySandboxState(sandboxId, workspaceSpec);
    }

    /**
     * 在沙箱中执行命令.
     *
     * <h3>支持的命令</h3>
     * <ul>
     *   <li>ls [path] - 列出目录内容</li>
     *   <li>cat [file] - 读取文件内容</li>
     *   <li>echo [text] - 输出文本到 stdout</li>
     *   <li>pwd - 打印工作目录</li>
     *   <li>mkdir [path] - 创建目录</li>
     *   <li>touch [file] - 创建空文件</li>
     *   <li>rm [path] - 删除文件或目录</li>
     *   <li>write [path] [content] - 写入内容到文件</li>
     *   <li>env - 显示环境变量</li>
     *   <li>export VAR=value - 设置环境变量</li>
     * </ul>
     *
     * @param runtimeContext 每次调用的代理上下文（可能为 null）
     * @param command 要执行的 shell 命令
     * @param timeoutSeconds 最大等待时间（null 使用默认值）
     * @return 执行结果，包含退出码和输出
     * @throws Exception 如果执行失败
     */
    @Override
    public ExecResult exec(RuntimeContext runtimeContext, String command, Integer timeoutSeconds)
            throws Exception {
        // 检查沙箱是否在运行
        if (!isRunning()) {
            throw new IllegalStateException("沙箱 " + sandboxId + " 未运行");
        }

        // 确定有效超时时间
        int effectiveTimeout = (timeoutSeconds != null) ? timeoutSeconds : defaultTimeout;
        long startTime = System.currentTimeMillis();

        try {
            // 解析命令和参数
            String[] parts = parseCommand(command);
            String cmd = parts[0];
            String[] args = parts.length > 1 ? java.util.Arrays.copyOfRange(parts, 1, parts.length) : new String[0];

            String output;
            int exitCode;

            // 根据命令类型执行相应操作
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
                    // 返回工作空间根目录
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
                    // 对于不支持的命令，返回模拟输出
                    output = simulateCommandExecution(cmd, args);
                    exitCode = 0;
            }

            long executionTime = System.currentTimeMillis() - startTime;
            log.debug("在沙箱 {} 中执行 '{}': exitCode={}, time={}ms",
                    sandboxId, command, exitCode, executionTime);

            return new ExecResult(exitCode, output, "", false);

        } catch (Exception e) {
            log.error("在沙箱 {} 中执行命令出错: {}", sandboxId, command, e);
            return new ExecResult(1, "", "Error: " + e.getMessage(), false);
        }
    }

    /**
     * 执行特定语言的代码.
     *
     * <p>这是代码执行的核心方法，支持 Java、Python 和 JavaScript。
     * 在内存沙箱中，代码执行是模拟的，主要用于演示目的。
     *
     * <h3>Java 代码执行</h3>
     * <p>检测是否包含 main 方法，返回模拟输出。
     *
     * <h3>Python 代码执行</h3>
     * <p>解析 print() 语句并返回模拟输出。
     *
     * <h3>JavaScript 代码执行</h3>
     * <p>解析 console.log() 语句并返回模拟输出。
     *
     * @param code 要执行的代码
     * @param language 编程语言（java, python, javascript）
     * @param timeoutSeconds 超时时间（秒）
     * @return 执行结果
     */
    public ExecResult executeCode(String code, String language, Integer timeoutSeconds) {
        if (!isRunning()) {
            return new ExecResult(1, "", "沙箱未运行", false);
        }

        int effectiveTimeout = (timeoutSeconds != null) ? timeoutSeconds : defaultTimeout;
        String output;
        int exitCode;

        switch (language.toLowerCase()) {
            case "java":
                // 模拟 Java 执行（实际实现需要编译器和进程）
                if (code.contains("public static void main")) {
                    output = "Java 代码已执行（模拟）\n[注意: 真实执行需要 Docker 沙箱]";
                    exitCode = 0;
                } else {
                    output = "Java 编译错误: 未找到 main 方法";
                    exitCode = 1;
                }
                break;
            case "python":
            case "python3":
                // 模拟 Python 执行 - 解析 print() 语句
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
                            .orElse("(无输出)");
                    exitCode = 0;
                } else {
                    output = "Python 代码已执行（模拟）\n[注意: 真实执行需要 Docker 沙箱]";
                    exitCode = 0;
                }
                break;
            case "javascript":
            case "js":
                // 模拟 JavaScript 执行 - 解析 console.log() 语句
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
                            .orElse("(无输出)");
                    exitCode = 0;
                } else {
                    output = "JavaScript 代码已执行（模拟）\n[注意: 真实执行需要 Docker 沙箱]";
                    exitCode = 0;
                }
                break;
            default:
                output = "不支持的语言: " + language + "\n支持的语言: java, python, javascript";
                exitCode = 1;
        }

        return new ExecResult(exitCode, output, "", false);
    }

    /**
     * 持久化工作空间.
     *
     * <p>将当前文件系统状态序列化为输入流，可用于：
     * <ul>
     *   <li>备份工作空间</li>
     *   <li>在沙箱之间迁移数据</li>
     *   <li>持久化存储</li>
     * </ul>
     *
     * @return 包含序列化文件系统的输入流
     * @throws Exception 如果序列化失败
     */
    @Override
    public InputStream persistWorkspace() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // 简单的序列化格式: 路径|大小|内容\n
        for (Map.Entry<String, byte[]> entry : fileSystem.entrySet()) {
            String line = entry.getKey() + "|" + entry.getValue().length + "|"
                    + StandardCharsets.UTF_8.decode(java.nio.ByteBuffer.wrap(entry.getValue())).toString()
                    + "\n";
            baos.write(line.getBytes(StandardCharsets.UTF_8));
        }
        return new ByteArrayInputStream(baos.toByteArray());
    }

    /**
     * 恢复工作空间.
     *
     * <p>从输入流反序列化文件系统状态。
     * 会清空当前文件系统并用归档内容替换。
     *
     * @param archive 包含序列化文件系统的输入流
     * @throws Exception 如果反序列化失败
     */
    @Override
    public void hydrateWorkspace(InputStream archive) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int len;
        while ((len = archive.read(buffer)) != -1) {
            baos.write(buffer, 0, len);
        }
        String content = baos.toString(StandardCharsets.UTF_8);

        // 清空当前文件系统
        fileSystem.clear();
        // 解析并恢复文件
        for (String line : content.split("\n")) {
            if (line.isEmpty()) continue;
            String[] parts = line.split("\\|", 3);
            if (parts.length >= 3) {
                String path = parts[0];
                byte[] data = parts[2].getBytes(StandardCharsets.UTF_8);
                fileSystem.put(path, data);
            }
        }
        log.info("恢复工作空间，共 {} 个文件", fileSystem.size());
    }

    // ========== 文件系统操作方法 ==========

    /**
     * 写入文件.
     *
     * @param path 文件路径
     * @param content 文件内容
     */
    public void writeFile(String path, String content) {
        fileSystem.put(normalizePath(path), content.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 读取文件.
     *
     * @param path 文件路径
     * @return 文件内容，如果文件不存在返回 null
     */
    public String readFile(String path) {
        byte[] data = fileSystem.get(normalizePath(path));
        if (data == null) {
            return null;
        }
        return StandardCharsets.UTF_8.decode(java.nio.ByteBuffer.wrap(data)).toString();
    }

    /**
     * 检查文件是否存在.
     *
     * @param path 文件路径
     * @return true 如果文件存在
     */
    public boolean fileExists(String path) {
        return fileSystem.containsKey(normalizePath(path));
    }

    /**
     * 删除文件.
     *
     * @param path 文件路径
     */
    public void deleteFile(String path) {
        fileSystem.remove(normalizePath(path));
    }

    /**
     * 获取文件系统的只读视图.
     *
     * @return 不可修改的文件系统映射
     */
    public Map<String, byte[]> getFileSystem() {
        return java.util.Collections.unmodifiableMap(fileSystem);
    }

    /**
     * 获取沙箱 ID.
     *
     * @return 沙箱唯一标识符
     */
    public String getSandboxId() {
        return sandboxId;
    }

    // ========== 私有辅助方法 ==========

    /**
     * 解析命令字符串为命令和参数数组.
     */
    private String[] parseCommand(String command) {
        return command.trim().split("\\s+");
    }

    /**
     * 规范化路径.
     * 移除尾部斜杠，处理空路径。
     */
    private String normalizePath(String path) {
        if (path == null) {
            return "/";
        }
        // 移除尾部斜杠
        while (path.endsWith("/") && path.length() > 1) {
            path = path.substring(0, path.length() - 1);
        }
        return path.isEmpty() ? "/" : path;
    }

    /**
     * 执行 ls 命令 - 列出目录内容.
     */
    private String executeLs(String[] args) {
        String targetDir = (args.length > 0) ? args[0] : "/";
        String normalizedDir = normalizePath(targetDir);

        StringBuilder sb = new StringBuilder();
        String dirPrefix = normalizedDir.equals("/") ? "" : normalizedDir + "/";

        // 过滤出目标目录下的直接子项
        fileSystem.keySet().stream()
                .filter(path -> path.startsWith(dirPrefix))
                .map(path -> path.substring(dirPrefix.length()))
                .filter(path -> !path.contains("/"))  // 只显示直接子项
                .sorted()
                .forEach(name -> sb.append(name).append("\n"));

        return sb.length() > 0 ? sb.toString() : "";
    }

    /**
     * 执行 cat 命令 - 读取文件内容.
     */
    private String executeCat(String[] args) {
        if (args.length == 0) {
            return "Error: cat 需要文件路径参数";
        }
        String content = readFile(args[0]);
        if (content == null) {
            return "Error: 文件不存在: " + args[0];
        }
        return content;
    }

    /**
     * 执行 echo 命令 - 输出文本.
     */
    private String executeEcho(String[] args) {
        return String.join(" ", args);
    }

    /**
     * 执行 mkdir 命令 - 创建目录.
     * 在内存沙箱中，目录在文件写入时隐式创建。
     */
    private String executeMkdir(String[] args) {
        if (args.length == 0) {
            return "Error: mkdir 需要路径参数";
        }
        // 在内存沙箱中，目录在写入文件时隐式创建
        return "";
    }

    /**
     * 执行 touch 命令 - 创建空文件.
     */
    private String executeTouch(String[] args) {
        if (args.length == 0) {
            return "Error: touch 需要文件路径参数";
        }
        if (!fileExists(args[0])) {
            writeFile(args[0], "");
        }
        return "";
    }

    /**
     * 执行 rm 命令 - 删除文件或目录.
     */
    private String executeRm(String[] args) {
        if (args.length == 0) {
            return "Error: rm 需要路径参数";
        }
        String path = normalizePath(args[0]);

        // 检查危险操作
        if (path.equals("/") || path.equals("") || path.equals("/workspace")) {
            return "Error: 不能删除根目录或工作空间";
        }

        // 删除文件及其下所有文件
        fileSystem.keySet().removeIf(p -> p.equals(path) || p.startsWith(path + "/"));
        return "";
    }

    /**
     * 执行 write 命令 - 写入文件.
     */
    private String executeWrite(String[] args) {
        if (args.length < 2) {
            return "Error: write 需要路径和内容参数";
        }
        String path = args[0];
        String content = commandArgToString(args, 1);
        writeFile(path, content);
        return "";
    }

    /**
     * 执行 env 命令 - 显示环境变量.
     */
    private String executeEnv() {
        StringBuilder sb = new StringBuilder();
        environment.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> sb.append(e.getKey()).append("=").append(e.getValue()).append("\n"));
        return sb.toString();
    }

    /**
     * 执行 export 命令 - 设置环境变量.
     */
    private String executeExport(String[] args) {
        if (args.length == 0) {
            return "Error: export 需要 VAR=value 格式的参数";
        }
        String[] kv = args[0].split("=", 2);
        if (kv.length == 2) {
            environment.put(kv[0], kv[1]);
            return "";
        }
        return "Error: export 需要 VAR=value 格式";
    }

    /**
     * 模拟执行不支持的命令.
     */
    private String simulateCommandExecution(String cmd, String[] args) {
        return "模拟执行: " + cmd + " " + String.join(" ", args)
                + "\n[注意: 真实执行需要 Docker 沙箱]";
    }

    /**
     * 将命令参数数组转换为字符串.
     */
    private String commandArgToString(String[] args, int start) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (i > start) sb.append(" ");
            sb.append(args[i]);
        }
        return sb.toString();
    }

    /**
     * 设置工作空间配置.
     *
     * @param workspaceSpec 工作空间配置
     */
    public void setWorkspaceSpec(WorkspaceSpec workspaceSpec) {
        this.workspaceSpec = workspaceSpec;
    }

    /**
     * 获取工作空间配置.
     *
     * @return 工作空间配置
     */
    public WorkspaceSpec getWorkspaceSpec() {
        return workspaceSpec;
    }

    /**
     * 沙箱状态内部类.
     *
     * <p>用于序列化和传递沙箱状态信息。
     */
    public static class InMemorySandboxState extends SandboxState {
        /**
         * 沙箱 ID.
         */
        private final String id;

        /**
         * 创建沙箱状态.
         *
         * @param sandboxId 沙箱 ID
         * @param spec 工作空间配置
         */
        public InMemorySandboxState(String sandboxId, WorkspaceSpec spec) {
            this.id = sandboxId;
            if (spec != null) {
                setWorkspaceSpec(spec.copy());
            }
        }

        /**
         * 获取沙箱 ID.
         *
         * @return 沙箱 ID
         */
        public String getId() {
            return id;
        }
    }
}