package io.agentscope.tutorial.chapter15.service;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.harness.agent.sandbox.ExecResult;
import io.agentscope.harness.agent.sandbox.Sandbox;
import io.agentscope.tutorial.chapter15.config.HarnessConfig;
import io.agentscope.tutorial.chapter15.model.ExecutionRequest;
import io.agentscope.tutorial.chapter15.model.ExecutionResult;
import io.agentscope.tutorial.chapter15.sandbox.InMemorySandbox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Code execution service that manages sandbox lifecycle and code execution.
 *
 * <p>This service provides:
 * <ul>
 *   <li>Sandbox creation and lifecycle management</li>
 *   <li>Code execution with timeout control</li>
 *   <li>Security validation before execution</li>
 *   <li>Workspace isolation per session/user</li>
 * </ul>
 *
 * <p>For production use, replace {@link InMemorySandbox} with Docker-based sandbox
 * (DockerSandboxClient) or cloud-based sandbox (E2bSandboxClient, DaytonaSandboxClient).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CodeExecutionService {

    private final HarnessConfig harnessConfig;

    // Map of workspace ID to sandbox instance
    private final Map<String, Sandbox> sandboxPool = new ConcurrentHashMap<>();

    // Simple lock for sandbox creation (in production, use a proper pool)
    private static final int MAX_SANDBOXES = 100;

    /**
     * Executes code in a sandbox environment.
     *
     * @param request the execution request
     * @param runtimeContext the runtime context (may be null)
     * @return execution result
     */
    public ExecutionResult execute(ExecutionRequest request, RuntimeContext runtimeContext) {
        long startTime = System.currentTimeMillis();

        try {
            // Validate security
            if (harnessConfig.containsBlockedPatterns(request.getCode())) {
                log.warn("Blocked pattern detected in code from workspace: {}", request.getWorkspace());
                return ExecutionResult.failure(
                        null,
                        "Code contains blocked patterns",
                        request.getWorkspace(),
                        request.getTimeoutSeconds()
                );
            }

            // Get or create sandbox for workspace
            Sandbox sandbox = getSandbox(request.getWorkspace());
            if (sandbox == null) {
                return ExecutionResult.failure(
                        null,
                        "Failed to create sandbox",
                        request.getWorkspace(),
                        request.getTimeoutSeconds()
                );
            }

            int effectiveTimeout = harnessConfig.getEffectiveTimeout(request.getTimeoutSeconds());

            // Execute based on language
            ExecResult execResult;
            if (request.getCode().contains("public static void main")
                    || request.getCode().contains("def ")
                    || request.getCode().contains("function")
                    || request.getCode().contains("console.log")
                    || request.getCode().contains("print(")) {
                // Code execution mode
                if (sandbox instanceof InMemorySandbox ims) {
                    execResult = ims.executeCode(
                            request.getCode(),
                            request.getLanguage(),
                            effectiveTimeout
                    );
                } else {
                    // For real sandboxes, execute as shell command
                    execResult = sandbox.exec(
                            runtimeContext,
                            getExecutionCommand(request),
                            effectiveTimeout
                    );
                }
            } else {
                // Shell command mode
                String command = request.getCode();
                if (!harnessConfig.isCommandAllowed(command)) {
                    return ExecutionResult.failure(
                            sandbox.getState().getSessionId(),
                            "Command not allowed",
                            request.getWorkspace(),
                            request.getTimeoutSeconds()
                    );
                }
                execResult = sandbox.exec(runtimeContext, command, effectiveTimeout);
            }

            long executionTime = System.currentTimeMillis() - startTime;

            return ExecutionResult.builder()
                    .success(execResult.ok())
                    .exitCode(execResult.exitCode())
                    .stdout(execResult.stdout())
                    .stderr(execResult.stderr())
                    .executionTimeMs(executionTime)
                    .workspace(request.getWorkspace())
                    .sandboxId(sandbox.getState().getSessionId())
                    .truncated(execResult.truncated())
                    .executedAt(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Execution failed for workspace {}: {}", request.getWorkspace(), e.getMessage(), e);
            return ExecutionResult.failure(
                    null,
                    "Execution error: " + e.getMessage(),
                    request.getWorkspace(),
                    request.getTimeoutSeconds()
            );
        }
    }

    /**
     * Executes a shell command directly.
     */
    public ExecutionResult executeCommand(String command, String workspace, Integer timeoutSeconds) {
        long startTime = System.currentTimeMillis();

        try {
            if (harnessConfig.containsBlockedPatterns(command)) {
                return ExecutionResult.failure(
                        null,
                        "Command contains blocked patterns",
                        workspace,
                        timeoutSeconds
                );
            }

            Sandbox sandbox = getSandbox(workspace);
            if (sandbox == null) {
                return ExecutionResult.failure(
                        null,
                        "Failed to get sandbox",
                        workspace,
                        timeoutSeconds
                );
            }

            int effectiveTimeout = harnessConfig.getEffectiveTimeout(timeoutSeconds);
            ExecResult result = sandbox.exec(null, command, effectiveTimeout);

            long executionTime = System.currentTimeMillis() - startTime;

            return ExecutionResult.builder()
                    .success(result.ok())
                    .exitCode(result.exitCode())
                    .stdout(result.stdout())
                    .stderr(result.stderr())
                    .executionTimeMs(executionTime)
                    .workspace(workspace)
                    .sandboxId(sandbox.getState().getSessionId())
                    .truncated(result.truncated())
                    .executedAt(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Command execution failed: {}", e.getMessage(), e);
            return ExecutionResult.failure(null, e.getMessage(), workspace, timeoutSeconds);
        }
    }

    /**
     * Gets or creates a sandbox for the given workspace.
     */
    public Sandbox getSandbox(String workspaceId) {
        return sandboxPool.computeIfAbsent(workspaceId, this::createSandbox);
    }

    /**
     * Creates a new sandbox for the workspace.
     */
    private Sandbox createSandbox(String workspaceId) {
        if (sandboxPool.size() >= MAX_SANDBOXES) {
            log.warn("Sandbox pool full, evicting oldest sandbox");
            // In production, implement proper eviction policy
            String oldest = sandboxPool.keySet().iterator().next();
            destroySandbox(oldest);
        }

        try {
            InMemorySandbox sandbox = new InMemorySandbox(harnessConfig.getSandbox().getDefaultTimeout());
            sandbox.setWorkspaceSpec(create默认WorkspaceSpec());
            sandbox.start();
            log.info("Created sandbox {} for workspace {}", sandbox.getSandboxId(), workspaceId);
            return sandbox;
        } catch (Exception e) {
            log.error("Failed to create sandbox for workspace {}: {}", workspaceId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Creates default workspace spec.
     */
    private io.agentscope.harness.agent.sandbox.WorkspaceSpec create默认WorkspaceSpec() {
        io.agentscope.harness.agent.sandbox.WorkspaceSpec spec =
                new io.agentscope.harness.agent.sandbox.WorkspaceSpec();
        spec.setRoot(harnessConfig.getSandbox().getWorkspaceRoot());

        // Add default files
        spec.getEntries().put("README.md",
                new io.agentscope.harness.agent.sandbox.layout.FileEntry(
                        "# Workspace\n\nWelcome to the Harness sandbox environment."));
        spec.getEntries().put(".workspace",
                new io.agentscope.harness.agent.sandbox.layout.FileEntry(
                        "workspace_id=default\n"));

        return spec;
    }

    /**
     * Gets the execution command for the language.
     */
    private String getExecutionCommand(ExecutionRequest request) {
        return switch (request.getLanguage().toLowerCase()) {
            case "java" -> "java " + request.getCode();
            case "python", "python3" -> "python3 -c '" + request.getCode().replace("'", "'\\''") + "'";
            case "javascript", "js" -> "node -e '" + request.getCode().replace("'", "'\\''") + "'";
            case "shell", "bash" -> request.getCode();
            default -> request.getCode();
        };
    }

    /**
     * Destroys a sandbox.
     */
    public void destroySandbox(String workspaceId) {
        Sandbox sandbox = sandboxPool.remove(workspaceId);
        if (sandbox != null) {
            try {
                sandbox.close();
                log.info("Destroyed sandbox for workspace {}", workspaceId);
            } catch (Exception e) {
                log.error("Error destroying sandbox for workspace {}: {}", workspaceId, e.getMessage());
            }
        }
    }

    /**
     * Gets current sandbox count.
     */
    public int getSandboxCount() {
        return sandboxPool.size();
    }

    /**
     * Destroys all sandboxes.
     */
    public void destroyAllSandboxes() {
        sandboxPool.keySet().forEach(this::destroySandbox);
    }

    /**
     * Gets information about a sandbox.
     */
    public SandboxInfo getSandboxInfo(String workspaceId) {
        Sandbox sandbox = sandboxPool.get(workspaceId);
        if (sandbox == null) {
            return null;
        }
        return new SandboxInfo(
                sandbox.getState().getSessionId(),
                sandbox.isRunning(),
                sandbox instanceof InMemorySandbox ims ? ims.getFileSystem().size() : 0
        );
    }

    /**
     * Sandbox information record.
     */
    public record SandboxInfo(String id, boolean running, int fileCount) {}
}