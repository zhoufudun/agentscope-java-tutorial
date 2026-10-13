package io.agentscope.tutorial.chapter15.controller;

import io.agentscope.tutorial.chapter15.model.ExecutionRequest;
import io.agentscope.tutorial.chapter15.model.ExecutionResult;
import io.agentscope.tutorial.chapter15.service.CodeExecutionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 代码执行 REST 控制器.
 *
 * <h2>概述</h2>
 * <p>本控制器提供代码执行相关的 REST API 端点，是 Harness 安全执行环境的主要入口点。
 * 它封装了 {@link CodeExecutionService} 的功能，通过 HTTP 接口暴露给外部调用者。
 *
 * <h2>核心功能</h2>
 * <ul>
 *   <li><b>代码执行</b>：在沙箱中安全执行各种语言的代码</li>
 *   <li><b>命令执行</b>：直接执行 shell 命令</li>
 *   <li><b>沙箱管理</b>：查询和销毁沙箱实例</li>
 *   <li><b>统计信息</b>：获取执行统计和健康状态</li>
 * </ul>
 *
 * <h2>API 端点</h2>
 * <ul>
 *   <li>POST /api/execute - 在沙箱中执行代码</li>
 *   <li>POST /api/execute/command - 执行 shell 命令</li>
 *   <li>GET /api/execute/sandbox/{workspace} - 获取沙箱信息</li>
 *   <li>DELETE /api/execute/sandbox/{workspace} - 销毁沙箱</li>
 *   <li>DELETE /api/execute/sandbox - 销毁所有沙箱</li>
 *   <li>GET /api/execute/stats - 获取执行统计</li>
 *   <li>GET /api/execute/health - 健康检查</li>
 * </ul>
 *
 * <h2>安全考虑</h2>
 * <p>所有执行请求都经过安全验证：
 * <ul>
 *   <li>代码内容检查（危险模式黑名单）</li>
 *   <li>命令白名单检查</li>
 *   <li>超时限制</li>
 *   <li>工作空间隔离</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 * @see CodeExecutionService
 * @see ExecutionRequest
 * @see ExecutionResult
 */
@Slf4j  // Lombok 注解，自动生成日志对象
@RestController  // 标记为 REST 控制器，方法返回值直接序列化为 JSON
@RequestMapping("/api/execute")  // 所有端点以 /api/execute 为前缀
@RequiredArgsConstructor  // Lombok 注解，自动生成构造函数注入
public class ExecutionController {

    /**
     * 代码执行服务.
     * 通过构造函数注入，处理实际的代码执行逻辑。
     */
    private final CodeExecutionService codeExecutionService;

    /**
     * 在沙箱环境中执行代码.
     *
     * <p>这是代码执行的主要端点。接收代码内容、语言类型和执行参数，
     * 在隔离的沙箱环境中执行代码，并返回执行结果。
     *
     * <h3>请求示例</h3>
     * <pre>{@code
     * POST /api/execute
     * Content-Type: application/json
     *
     * {
     *   "code": "System.out.println(\"Hello, World!\");",
     *   "language": "java",
     *   "timeoutSeconds": 60,
     *   "workspace": "my-workspace"
     * }
     * }</pre>
     *
     * <h3>支持的语言</h3>
     * <ul>
     *   <li>java - Java 代码</li>
     *   <li>python/python3 - Python 代码</li>
     *   <li>javascript/js - JavaScript 代码</li>
     *   <li>shell/bash - Shell 脚本</li>
     * </ul>
     *
     * @param request 执行请求，包含代码、语言、超时和工作空间信息
     * @return 执行结果，包含输出、退出码和执行时间
     */
    @PostMapping
    public ResponseEntity<ExecutionResult> execute(@Valid @RequestBody ExecutionRequest request) {
        log.info("执行 {} 代码，工作空间: {}",
                request.getLanguage(), request.getWorkspace());

        // 调用服务层执行代码，RuntimeContext 为 null（本示例不使用）
        ExecutionResult result = codeExecutionService.execute(request, null);

        // 根据执行结果返回相应的 HTTP 状态码
        if (result.isSuccess()) {
            return ResponseEntity.ok(result);  // 200 OK
        } else {
            return ResponseEntity.badRequest().body(result);  // 400 Bad Request
        }
    }

    /**
     * 直接执行 shell 命令.
     *
     * <p>此端点允许直接执行 shell 命令，而不需要指定语言。
     * 命令会经过白名单和黑名单检查。
     *
     * <h3>请求示例</h3>
     * <pre>{@code
     * POST /api/execute/command
     * Content-Type: application/json
     *
     * {
     *   "command": "ls -la /workspace",
     *   "workspace": "my-workspace",
     *   "timeoutSeconds": 30
     * }
     * }</pre>
     *
     * @param request 请求体，包含 command、workspace（可选）和 timeoutSeconds（可选）
     * @return 执行结果
     */
    @PostMapping("/command")
    public ResponseEntity<ExecutionResult> executeCommand(
            @RequestBody Map<String, Object> request) {

        // 从请求中提取参数
        String command = (String) request.get("command");
        String workspace = (String) request.getOrDefault("workspace", "default");
        Integer timeout = request.get("timeoutSeconds") != null
                ? ((Number) request.get("timeoutSeconds")).intValue()
                : null;

        // 验证命令不为空
        if (command == null || command.isBlank()) {
            return ResponseEntity.badRequest().body(
                    ExecutionResult.failure(null, "命令不能为空", workspace, timeout)
            );
        }

        log.info("在工作空间 {} 执行命令: {}", workspace, command);
        ExecutionResult result = codeExecutionService.executeCommand(command, workspace, timeout);

        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 获取沙箱实例信息.
     *
     * <p>查询指定工作空间的沙箱状态，包括：
     * <ul>
     *   <li>沙箱 ID</li>
     *   <li>运行状态</li>
     *   <li>文件数量</li>
     * </ul>
     *
     * @param workspace 工作空间 ID
     * @return 沙箱信息，如果不存在则返回 404
     */
    @GetMapping("/sandbox/{workspace}")
    public ResponseEntity<Object> getSandboxInfo(@PathVariable String workspace) {
        CodeExecutionService.SandboxInfo info = codeExecutionService.getSandboxInfo(workspace);
        if (info == null) {
            // 沙箱不存在
            return ResponseEntity.notFound().build();
        }
        // 返回沙箱状态信息
        return ResponseEntity.ok(Map.of(
                "sandboxId", info.id(),
                "running", info.running(),
                "fileCount", info.fileCount()
        ));
    }

    /**
     * 销毁沙箱实例.
     *
     * <p>停止并清理指定工作空间的沙箱。沙箱中的所有文件和状态都将被删除。
     * 这是一个清理操作，通常在任务完成后调用。
     *
     * @param workspace 要销毁的工作空间 ID
     * @return 200 OK 表示成功
     */
    @DeleteMapping("/sandbox/{workspace}")
    public ResponseEntity<Void> destroySandbox(@PathVariable String workspace) {
        log.info("销毁工作空间 {} 的沙箱", workspace);
        codeExecutionService.destroySandbox(workspace);
        return ResponseEntity.ok().build();
    }

    /**
     * 获取执行统计信息.
     *
     * <p>返回当前系统的执行统计，包括：
     * <ul>
     *   <li>活跃沙箱数量</li>
     *   <li>最大沙箱数量限制</li>
     * </ul>
     *
     * @return 统计信息
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(Map.of(
                "activeSandboxes", codeExecutionService.getSandboxCount(),
                "maxSandboxes", 100  // 配置的最大值
        ));
    }

    /**
     * 销毁所有沙箱实例.
     *
     * <p>这是一个管理操作，用于清理所有沙箱。
     * 通常在系统关闭或重置时使用。
     *
     * <h3>注意事项</h3>
     * <p>此操作会影响所有用户的工作空间，应谨慎使用。
     *
     * @return 200 OK 表示成功
     */
    @DeleteMapping("/sandbox")
    public ResponseEntity<Void> destroyAllSandboxes() {
        log.info("销毁所有沙箱");
        codeExecutionService.destroyAllSandboxes();
        return ResponseEntity.ok().build();
    }

    /**
     * 健康检查端点.
     *
     * <p>用于监控系统健康状态，返回服务状态和基本指标。
     * 可以被负载均衡器或监控系统调用。
     *
     * <h3>响应示例</h3>
     * <pre>{@code
     * {
     *   "status": "UP",
     *   "service": "CodeExecutionService",
     *   "activeSandboxes": "3"
     * }
     * }</pre>
     *
     * @return 健康状态信息
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "CodeExecutionService",
                "activeSandboxes", String.valueOf(codeExecutionService.getSandboxCount())
        ));
    }
}