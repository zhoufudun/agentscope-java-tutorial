package io.agentscope.tutorial.chapter12.controller;

import io.agentscope.core.plan.PlanNotebook;
import io.agentscope.core.plan.model.Plan;
import io.agentscope.core.plan.model.PlanState;
import io.agentscope.core.plan.model.SubTask;
import io.agentscope.tutorial.chapter12.dto.PlanRequest;
import io.agentscope.tutorial.chapter12.dto.SubTaskRequest;
import io.agentscope.tutorial.chapter12.hook.PlanChangeHook;
import io.agentscope.tutorial.chapter12.service.ChatService;
import io.agentscope.tutorial.chapter12.service.PlanService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;

/**
 * 计划管理控制器 - 提供计划的 CRUD 操作接口.
 *
 * <p>REST API 端点：
 * <ul>
 *   <li>GET  /api/plan           - 获取当前计划</li>
 *   <li>GET  /api/plan/stream    - SSE 流式推送计划状态</li>
 *   <li>POST /api/plan           - 创建新计划</li>
 *   <li>PUT  /api/plan           - 更新计划基本信息</li>
 *   <li>DELETE /api/plan/subtasks/{idx} - 删除子任务</li>
 *   <li>PATCH /api/plan/subtasks/{idx}/state - 更新子任务状态</li>
 *   <li>POST /api/plan/subtasks/{idx}/finish - 完成子任务</li>
 *   <li>POST /api/plan/finish    - 完成计划</li>
 *   <li>GET  /api/plan/history   - 获取历史计划列表</li>
 *   <li>GET  /api/plan/stats     - 获取计划统计</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 */
@RestController
@RequestMapping("/api/plan")
public class PlanController {

    private static final Logger log = LoggerFactory.getLogger(PlanController.class);

    private final ChatService chatService;
    private final PlanService planService;
    private final PlanNotebook planNotebook;

    public PlanController(ChatService chatService, PlanService planService, PlanNotebook planNotebook) {
        this.chatService = chatService;
        this.planService = planService;
        this.planNotebook = planNotebook;
    }

    /**
     * 获取当前计划详情.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getCurrentPlan() {
        Plan plan = chatService.getCurrentPlan();
        if (plan == null) {
            return ResponseEntity.ok(Map.of(
                    "hasPlan", false,
                    "message", "No active plan"
            ));
        }
        return ResponseEntity.ok(toPlanMap(plan));
    }

    /**
     * SSE 流式推送计划状态变化.
     */
    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> getPlanStream() {
        return planService.getPlanStream()
                .map(response -> {
                    try {
                        return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(response);
                    } catch (Exception e) {
                        log.warn("Failed to serialize plan response", e);
                        return "{}";
                    }
                });
    }

    /**
     * 创建新计划.
     */
    @PostMapping
    public Mono<ResponseEntity<Map<String, Object>>> createPlan(@RequestBody PlanRequest request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            return Mono.just(ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Plan name is required"
            )));
        }

        // 转换子任务
        List<Map<String, Object>> subtasks = new ArrayList<>();
        if (request.getSubtasks() != null) {
            for (SubTaskRequest st : request.getSubtasks()) {
                subtasks.add(Map.of(
                        "name", st.getName() != null ? st.getName() : "Unnamed",
                        "description", st.getDescription() != null ? st.getDescription() : "",
                        "expected_outcome", st.getExpectedOutcome() != null ? st.getExpectedOutcome() : ""
                ));
            }
        }

        return planNotebook.createPlan(
                        request.getName(),
                        request.getDescription() != null ? request.getDescription() : "",
                        request.getExpectedOutcome() != null ? request.getExpectedOutcome() : "",
                        subtasks
                )
                .map(result -> ResponseEntity.ok(Map.<String, Object>of(
                        "success", true,
                        "message", result,
                        "planName", request.getName()
                )))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("success", false, "error", e.getMessage()))));
    }

    /**
     * 更新计划基本信息.
     */
    @PutMapping
    public Mono<ResponseEntity<Map<String, Object>>> updatePlanInfo(@RequestBody PlanRequest request) {
        return planNotebook.updatePlanInfo(
                        request.getName(),
                        request.getDescription(),
                        request.getExpectedOutcome()
                )
                .map(result -> ResponseEntity.ok(Map.<String, Object>of(
                        "success", true,
                        "message", result
                )))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("success", false, "error", e.getMessage()))));
    }

    /**
     * 添加子任务.
     */
    @PostMapping("/subtasks")
    public Mono<ResponseEntity<Map<String, Object>>> addSubtask(@RequestBody SubTaskRequest request) {
        int index = planNotebook.getCurrentPlan() != null
                ? planNotebook.getCurrentPlan().getSubtasks().size()
                : 0;

        Map<String, Object> subtaskMap = Map.of(
                "name", request.getName() != null ? request.getName() : "Unnamed",
                "description", request.getDescription() != null ? request.getDescription() : "",
                "expected_outcome", request.getExpectedOutcome() != null ? request.getExpectedOutcome() : ""
        );

        return planNotebook.reviseCurrentPlan(index, "add", subtaskMap)
                .map(result -> ResponseEntity.ok(Map.<String, Object>of(
                        "success", true,
                        "message", result
                )))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("success", false, "error", e.getMessage()))));
    }

    /**
     * 删除子任务.
     */
    @DeleteMapping("/subtasks/{index}")
    public Mono<ResponseEntity<Map<String, Object>>> deleteSubtask(@PathVariable int index) {
        return planNotebook.reviseCurrentPlan(index, "delete", null)
                .map(result -> ResponseEntity.ok(Map.<String, Object>of(
                        "success", true,
                        "message", result
                )))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("success", false, "error", e.getMessage()))));
    }

    /**
     * 更新子任务状态.
     */
    @PatchMapping("/subtasks/{index}/state")
    public Mono<ResponseEntity<Map<String, Object>>> updateSubtaskState(
            @PathVariable int index,
            @RequestBody Map<String, String> body) {
        String state = body.get("state");
        if (state == null) {
            return Mono.just(ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "State is required"
            )));
        }

        return planNotebook.updateSubtaskState(index, state)
                .map(result -> ResponseEntity.ok(Map.<String, Object>of(
                        "success", true,
                        "message", result
                )))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("success", false, "error", e.getMessage()))));
    }

    /**
     * 完成子任务.
     */
    @PostMapping("/subtasks/{index}/finish")
    public Mono<ResponseEntity<Map<String, Object>>> finishSubtask(
            @PathVariable int index,
            @RequestBody Map<String, String> body) {
        String outcome = body.get("outcome");
        if (outcome == null || outcome.isBlank()) {
            return Mono.just(ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Outcome is required"
            )));
        }

        return planNotebook.finishSubtask(index, outcome)
                .map(result -> ResponseEntity.ok(Map.<String, Object>of(
                        "success", true,
                        "message", result
                )))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("success", false, "error", e.getMessage()))));
    }

    /**
     * 完成计划.
     */
    @PostMapping("/finish")
    public Mono<ResponseEntity<Map<String, Object>>> finishPlan(@RequestBody Map<String, String> body) {
        String state = body.getOrDefault("state", "done");
        String outcome = body.get("outcome");

        return planNotebook.finishPlan(state, outcome)
                .map(result -> ResponseEntity.ok(Map.<String, Object>of(
                        "success", true,
                        "message", result
                )))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("success", false, "error", e.getMessage()))));
    }

    /**
     * 获取历史计划列表.
     */
    @GetMapping("/history")
    public Mono<ResponseEntity<List<Map<String, Object>>>> getHistory() {
        return planService.getPlans()
                .map(plans -> {
                    List<Map<String, Object>> result = new ArrayList<>();
                    for (Plan p : plans) {
                        result.add(toPlanMap(p));
                    }
                    return ResponseEntity.ok(result);
                })
                .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(List.of())));
    }

    /**
     * 获取计划统计信息.
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        PlanService.PlanStatistics stats = planService.getStatistics();
        return ResponseEntity.ok(Map.of(
                "total", stats.total(),
                "done", stats.done(),
                "abandoned", stats.abandoned(),
                "inProgress", stats.inProgress(),
                "todo", stats.todo()
        ));
    }

    /**
     * 格式化计划为 Markdown.
     */
    @GetMapping("/markdown")
    public ResponseEntity<Map<String, Object>> getPlanMarkdown() {
        Plan plan = chatService.getCurrentPlan();
        String markdown = PlanChangeHook.formatAsMarkdown(plan);
        PlanChangeHook.PlanProgress progress = PlanChangeHook.getProgress(plan);

        return ResponseEntity.ok(Map.of(
                "markdown", markdown,
                "progress", Map.of(
                        "total", progress.total(),
                        "done", progress.done(),
                        "percentDone", progress.percentDone()
                )
        ));
    }

    // ==================== 私有辅助方法 ====================

    private Map<String, Object> toPlanMap(Plan plan) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("hasPlan", true);
        result.put("id", plan.getId());
        result.put("name", plan.getName());
        result.put("description", plan.getDescription());
        result.put("expectedOutcome", plan.getExpectedOutcome());
        result.put("outcome", plan.getOutcome());
        result.put("state", plan.getState().getValue());
        result.put("createdAt", plan.getCreatedAt());
        result.put("finishedAt", plan.getFinishedAt());

        // 子任务列表
        if (plan.getSubtasks() != null) {
            List<Map<String, Object>> subtasks = new ArrayList<>();
            for (int i = 0; i < plan.getSubtasks().size(); i++) {
                SubTask st = plan.getSubtasks().get(i);
                Map<String, Object> subtaskMap = new LinkedHashMap<>();
                subtaskMap.put("index", i);
                subtaskMap.put("name", st.getName());
                subtaskMap.put("description", st.getDescription());
                subtaskMap.put("expectedOutcome", st.getExpectedOutcome());
                subtaskMap.put("outcome", st.getOutcome());
                subtaskMap.put("state", st.getState().getValue());
                subtaskMap.put("createdAt", st.getCreatedAt());
                subtaskMap.put("finishedAt", st.getFinishedAt());
                subtasks.add(subtaskMap);
            }
            result.put("subtasks", subtasks);
        }

        // 进度统计
        PlanChangeHook.PlanProgress progress = PlanChangeHook.getProgress(plan);
        result.put("progress", Map.of(
                "total", progress.total(),
                "done", progress.done(),
                "inProgress", progress.inProgress(),
                "abandoned", progress.abandoned(),
                "todo", progress.todo(),
                "percentDone", progress.percentDone()
        ));

        return result;
    }
}