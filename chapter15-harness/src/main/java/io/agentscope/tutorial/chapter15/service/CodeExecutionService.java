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
 * 代码执行服务 - 管理沙箱生命周期和代码执行.
 *
 * <h2>概述</h2>
 * <p>本服务是 Harness 安全执行环境的核心组件，负责：
 * <ul>
 *   <li>沙箱的创建、管理和销毁</li>
 *   <li>代码和命令的安全执行</li>
 *   <li>安全验证（危险模式检测、命令白名单）</li>
 *   <li>工作空间隔离（每个会话/用户独立的沙箱）</li>
 * </ul>
 *
 * <h2>沙箱池管理</h2>
 * <p>使用 ConcurrentHashMap 实现沙箱池，每个工作空间对应一个沙箱实例：
 * <ul>
 *   <li>按需创建：首次访问时创建沙箱</li>
 *   <li>复用机制：同一工作空间复用已有沙箱</li>
 *   <li>容量限制：达到上限时淘汰旧沙箱</li>
 *   <li>手动销毁：支持单个或批量销毁沙箱</li>
 * </ul>
 *
 * <h2>执行流程</h2>
 * <ol>
 *   <li>安全验证：检查代码/命令是否包含危险模式</li>
 *   <li>获取沙箱：从池中获取或创建新沙箱</li>
 *   <li>执行代码：在沙箱中执行代码/命令</li>
 *   <li>返回结果：封装执行结果并返回</li>
 * </ol>
 *
 * <h2>生产环境建议</h2>
 * <p>本示例使用 {@link InMemorySandbox}，在生产环境中建议替换为：
 * <ul>
 *   <li>DockerSandboxClient - 基于 Docker 的容器隔离</li>
 *   <li>E2bSandboxClient - 云端沙箱服务</li>
 *   <li>DaytonaSandboxClient - 云端开发环境</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 * @see InMemorySandbox
 * @see HarnessConfig
 */
@Slf4j
@Service  // Spring 服务组件
@RequiredArgsConstructor  // 构造函数注入
public class CodeExecutionService {

    /**
     * Harness 配置.
     * 包含沙箱和执行的安全配置。
     */
    private final HarnessConfig harnessConfig;

    /**
     * 沙箱池.
     * 映射：工作空间 ID -> 沙箱实例
     * 使用 ConcurrentHashMap 保证线程安全。
     */
    private final Map<String, Sandbox> sandboxPool = new ConcurrentHashMap<>();

    /**
     * 沙箱池最大容量.
     * 在生产环境中应使用更复杂的淘汰策略。
     */
    private static final int MAX_SANDBOXES = 100;

    /**
     * 在沙箱环境中执行代码.
     *
     * <h3>执行流程</h3>
     * <ol>
     *   <li>安全验证：检查代码是否包含危险模式</li>
     *   <li>获取/创建沙箱</li>
     *   <li>根据代码类型选择执行方式</li>
     *   <li>封装并返回执行结果</li>
     * </ol>
     *
     * <h3>代码类型检测</h3>
     * <p>根据代码内容自动判断执行模式：
     * <ul>
     *   <li>包含 main 方法、def、function 等 -> 代码执行模式</li>
     *   <li>其他 -> Shell 命令模式</li>
     * </ul>
     *
     * @param request 执行请求，包含代码、语言、超时等
     * @param runtimeContext 运行时上下文（可以为 null）
     * @return 执行结果
     */
    public ExecutionResult execute(ExecutionRequest request, RuntimeContext runtimeContext) {
        long startTime = System.currentTimeMillis();

        try {
            // === 1. 安全验证 ===
            // 检查代码是否包含危险模式（如 rm -rf /, DROP DATABASE 等）
            if (harnessConfig.containsBlockedPatterns(request.getCode())) {
                log.warn("检测到危险模式，工作空间: {}", request.getWorkspace());
                return ExecutionResult.failure(
                        null,
                        "代码包含被阻止的危险模式",
                        request.getWorkspace(),
                        request.getTimeoutSeconds()
                );
            }

            // === 2. 获取或创建沙箱 ===
            Sandbox sandbox = getSandbox(request.getWorkspace());
            if (sandbox == null) {
                return ExecutionResult.failure(
                        null,
                        "无法创建沙箱",
                        request.getWorkspace(),
                        request.getTimeoutSeconds()
                );
            }

            // 获取有效超时时间（受配置的最大值限制）
            int effectiveTimeout = harnessConfig.getEffectiveTimeout(request.getTimeoutSeconds());

            // === 3. 执行代码 ===
            ExecResult execResult;
            // 根据代码内容判断执行模式
            if (request.getCode().contains("public static void main")
                    || request.getCode().contains("def ")
                    || request.getCode().contains("function")
                    || request.getCode().contains("console.log")
                    || request.getCode().contains("print(")) {
                // 代码执行模式：使用语言特定的解释器
                if (sandbox instanceof InMemorySandbox ims) {
                    execResult = ims.executeCode(
                            request.getCode(),
                            request.getLanguage(),
                            effectiveTimeout
                    );
                } else {
                    // 对于真实沙箱，构建执行命令
                    execResult = sandbox.exec(
                            runtimeContext,
                            getExecutionCommand(request),
                            effectiveTimeout
                    );
                }
            } else {
                // Shell 命令模式：直接执行命令
                String command = request.getCode();
                // 验证命令是否在白名单中
                if (!harnessConfig.isCommandAllowed(command)) {
                    return ExecutionResult.failure(
                            sandbox.getState().getSessionId(),
                            "命令不在允许列表中",
                            request.getWorkspace(),
                            request.getTimeoutSeconds()
                    );
                }
                execResult = sandbox.exec(runtimeContext, command, effectiveTimeout);
            }

            // === 4. 封装返回结果 ===
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
            log.error("工作空间 {} 执行失败: {}", request.getWorkspace(), e.getMessage(), e);
            return ExecutionResult.failure(
                    null,
                    "执行错误: " + e.getMessage(),
                    request.getWorkspace(),
                    request.getTimeoutSeconds()
            );
        }
    }

    /**
     * 直接执行 shell 命令.
     *
     * <p>此方法用于直接执行 shell 命令，不进行语言类型判断。
     * 仍然会进行安全验证。
     *
     * @param command 要执行的命令
     * @param workspace 工作空间 ID
     * @param timeoutSeconds 超时时间（秒）
     * @return 执行结果
     */
    public ExecutionResult executeCommand(String command, String workspace, Integer timeoutSeconds) {
        long startTime = System.currentTimeMillis();

        try {
            // 安全验证：检查危险模式
            if (harnessConfig.containsBlockedPatterns(command)) {
                return ExecutionResult.failure(
                        null,
                        "命令包含被阻止的危险模式",
                        workspace,
                        timeoutSeconds
                );
            }

            // 获取沙箱
            Sandbox sandbox = getSandbox(workspace);
            if (sandbox == null) {
                return ExecutionResult.failure(
                        null,
                        "无法获取沙箱",
                        workspace,
                        timeoutSeconds
                );
            }

            // 执行命令
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
            log.error("命令执行失败: {}", e.getMessage(), e);
            return ExecutionResult.failure(null, e.getMessage(), workspace, timeoutSeconds);
        }
    }

    /**
     * 获取或创建工作空间的沙箱.
     *
     * <p>使用 computeIfAbsent 保证线程安全的懒加载：
     * <ul>
     *   <li>如果沙箱已存在，直接返回</li>
     *   <li>如果不存在，创建新沙箱并缓存</li>
     * </ul>
     *
     * @param workspaceId 工作空间标识符
     * @return 沙箱实例，如果创建失败返回 null
     */
    public Sandbox getSandbox(String workspaceId) {
        return sandboxPool.computeIfAbsent(workspaceId, this::createSandbox);
    }

    /**
     * 为工作空间创建新沙箱.
     *
     * <p>创建流程：
     * <ol>
     *   <li>检查池容量，必要时淘汰旧沙箱</li>
     *   <li>创建 InMemorySandbox 实例</li>
     *   <li>配置工作空间规格</li>
     *   <li>启动沙箱</li>
     * </ol>
     *
     * @param workspaceId 工作空间标识符
     * @return 新创建的沙箱，如果创建失败返回 null
     */
    private Sandbox createSandbox(String workspaceId) {
        // 检查池容量
        if (sandboxPool.size() >= MAX_SANDBOXES) {
            log.warn("沙箱池已满，淘汰最旧的沙箱");
            // 在生产环境中应实现更好的淘汰策略（如 LRU）
            String oldest = sandboxPool.keySet().iterator().next();
            destroySandbox(oldest);
        }

        try {
            // 创建沙箱
            InMemorySandbox sandbox = new InMemorySandbox(harnessConfig.getSandbox().getDefaultTimeout());
            // 配置工作空间规格
            sandbox.setWorkspaceSpec(create默认WorkspaceSpec());
            // 启动沙箱
            sandbox.start();
            log.info("为工作空间 {} 创建沙箱 {}", workspaceId, sandbox.getSandboxId());
            return sandbox;
        } catch (Exception e) {
            log.error("为工作空间 {} 创建沙箱失败: {}", workspaceId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 创建默认工作空间规格.
     *
     * <p>配置工作空间的初始状态：
     * <ul>
     *   <li>设置根目录路径</li>
     *   <li>创建默认文件（README.md, .workspace）</li>
     * </ul>
     *
     * @return 工作空间规格对象
     */
    private io.agentscope.harness.agent.sandbox.WorkspaceSpec create默认WorkspaceSpec() {
        io.agentscope.harness.agent.sandbox.WorkspaceSpec spec =
                new io.agentscope.harness.agent.sandbox.WorkspaceSpec();
        // 设置工作空间根目录
        spec.setRoot(harnessConfig.getSandbox().getWorkspaceRoot());

        // 添加默认文件
        spec.getEntries().put("README.md",
                new io.agentscope.harness.agent.sandbox.layout.FileEntry(
                        "# 工作空间\n\n欢迎使用 Harness 沙箱环境。"));
        spec.getEntries().put(".workspace",
                new io.agentscope.harness.agent.sandbox.layout.FileEntry(
                        "workspace_id=default\n"));

        return spec;
    }

    /**
     * 获取语言特定的执行命令.
     *
     * <p>根据编程语言构建相应的执行命令：
     * <ul>
     *   <li>java: java 命令</li>
     *   <li>python: python3 -c '...'</li>
     *   <li>javascript: node -e '...'</li>
     *   <li>shell: 直接执行</li>
     * </ul>
     *
     * @param request 执行请求
     * @return 构建的执行命令
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
     * 销毁沙箱.
     *
     * <p>从池中移除沙箱并释放资源。
     *
     * @param workspaceId 要销毁的工作空间 ID
     */
    public void destroySandbox(String workspaceId) {
        Sandbox sandbox = sandboxPool.remove(workspaceId);
        if (sandbox != null) {
            try {
                sandbox.close();
                log.info("已销毁工作空间 {} 的沙箱", workspaceId);
            } catch (Exception e) {
                log.error("销毁工作空间 {} 的沙箱时出错: {}", workspaceId, e.getMessage());
            }
        }
    }

    /**
     * 获取当前沙箱数量.
     *
     * @return 活跃的沙箱数量
     */
    public int getSandboxCount() {
        return sandboxPool.size();
    }

    /**
     * 销毁所有沙箱.
     *
     * <p>清理所有沙箱，通常在系统关闭时调用。
     */
    public void destroyAllSandboxes() {
        sandboxPool.keySet().forEach(this::destroySandbox);
    }

    /**
     * 获取沙箱信息.
     *
     * @param workspaceId 工作空间 ID
     * @return 沙箱信息，如果不存在返回 null
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
     * 沙箱信息记录.
     *
     * <p>使用 Java Record 定义不可变的沙箱信息数据结构。
     *
     * @param id 沙箱 ID
     * @param running 是否正在运行
     * @param fileCount 文件数量
     */
    public record SandboxInfo(String id, boolean running, int fileCount) {}
}