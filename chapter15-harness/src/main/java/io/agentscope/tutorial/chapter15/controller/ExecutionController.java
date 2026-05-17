package io.agentscope.tutorial.chapter15.controller;

import io.agentscope.tutorial.chapter15.model.ExecutionRequest;
import io.agentscope.tutorial.chapter15.model.ExecutionResult;
import io.agentscope.tutorial.chapter15.service.CodeExecutionService;
import jakarta.validation.Valid;
import lombok.RequiredArgs构造函数;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for code execution operations.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>POST /api/execute - 在沙箱中执行代码</li>
 *   <li>POST /api/execute/command - Execute shell command</li>
 *   <li>GET /api/execute/sandbox/{workspace} - Get sandbox info</li>
 *   <li>DELETE /api/execute/sandbox/{workspace} - Destroy sandbox</li>
 *   <li>GET /api/execute/stats - Get execution statistics</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/execute")
@RequiredArgs构造函数
public class ExecutionController {

    private final CodeExecutionService codeExecutionService;

    /**
     * Executes code in a sandbox environment.
     *
     * <p>Example request:
     * <pre>{@code
     * {
     *   "code": "System.out.println(\"Hello, World!\");",
     *   "language": "java",
     *   "timeoutSeconds": 60,
     *   "workspace": "my-workspace"
     * }
     * }</pre>
     */
    @PostMapping
    public ResponseEntity<ExecutionResult> execute(@Valid @RequestBody ExecutionRequest request) {
        log.info("Executing {} code in workspace: {}",
                request.getLanguage(), request.getWorkspace());

        ExecutionResult result = codeExecutionService.execute(request, null);

        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * Executes a shell command directly.
     */
    @PostMapping("/command")
    public ResponseEntity<ExecutionResult> executeCommand(
            @RequestBody Map<String, Object> request) {

        String command = (String) request.get("command");
        String workspace = (String) request.getOr默认("workspace", "default");
        Integer timeout = request.get("timeoutSeconds") != null
                ? ((Number) request.get("timeoutSeconds")).intValue()
                : null;

        if (command == null || command.isBlank()) {
            return ResponseEntity.badRequest().body(
                    ExecutionResult.failure(null, "Command cannot be blank", workspace, timeout)
            );
        }

        log.info("Executing command in workspace {}: {}", workspace, command);
        ExecutionResult result = codeExecutionService.executeCommand(command, workspace, timeout);

        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * Gets information about a sandbox instance.
     */
    @GetMapping("/sandbox/{workspace}")
    public ResponseEntity<Object> getSandboxInfo(@PathVariable String workspace) {
        CodeExecutionService.SandboxInfo info = codeExecutionService.getSandboxInfo(workspace);
        if (info == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of(
                "sandboxId", info.id(),
                "running", info.running(),
                "fileCount", info.fileCount()
        ));
    }

    /**
     * Destroys a sandbox instance.
     */
    @DeleteMapping("/sandbox/{workspace}")
    public ResponseEntity<Void> destroySandbox(@PathVariable String workspace) {
        log.info("Destroying sandbox for workspace: {}", workspace);
        codeExecutionService.destroySandbox(workspace);
        return ResponseEntity.ok().build();
    }

    /**
     * Gets execution statistics.
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(Map.of(
                "activeSandboxes", codeExecutionService.getSandboxCount(),
                "maxSandboxes", 100
        ));
    }

    /**
     * Destroys all sandboxes.
     */
    @DeleteMapping("/sandbox")
    public ResponseEntity<Void> destroyAllSandboxes() {
        log.info("Destroying all sandboxes");
        codeExecutionService.destroyAllSandboxes();
        return ResponseEntity.ok().build();
    }

    /**
     * 健康检查端点.
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