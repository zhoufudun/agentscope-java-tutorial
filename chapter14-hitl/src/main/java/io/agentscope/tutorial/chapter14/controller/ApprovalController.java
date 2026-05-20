package io.agentscope.tutorial.chapter14.controller;

import io.agentscope.tutorial.chapter14.entity.ApprovalRequest;
import io.agentscope.tutorial.chapter14.service.ApprovalService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 审批管理控制器.
 *
 * <p>提供审批请求的查询和手动审批接口.
 *
 * @author AgentScope Tutorial
 */
@RestController
@RequestMapping("/api/approval")
public class ApprovalController {

    private final ApprovalService approvalService;

    public ApprovalController(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    /**
     * 获取所有待审批请求（全局）.
     */
    @GetMapping("/pending")
    public ResponseEntity<List<ApprovalRequest>> getAllPending() {
        List<ApprovalRequest> pending = approvalService.getAllPendingRequests();
        return ResponseEntity.ok(pending);
    }

    /**
     * 获取指定会话的待审批请求.
     */
    @GetMapping("/pending/{sessionId}")
    public ResponseEntity<List<ApprovalRequest>> getPendingBySession(@PathVariable String sessionId) {
        List<ApprovalRequest> pending = approvalService.getPendingRequests(sessionId);
        return ResponseEntity.ok(pending);
    }

    /**
     * 获取指定会话的所有审批请求.
     */
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<List<ApprovalRequest>> getRequestsBySession(@PathVariable String sessionId) {
        List<ApprovalRequest> requests = approvalService.getRequestsBySession(sessionId);
        return ResponseEntity.ok(requests);
    }

    /**
     * 获取审批请求详情.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApprovalRequest> getById(@PathVariable Long id) {
        Optional<ApprovalRequest> opt = approvalService.findById(id);
        return opt.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 执行审批（手动审批接口）.
     * 可用于管理后台或审批流程系统调用.
     *
     * @param id       审批请求 ID
     * @param approved 审批决策
     * @param approver 审批人
     * @param comment  审批意见
     */
    @PostMapping("/{id}")
    public ResponseEntity<Map<String, Object>> approve(
            @PathVariable Long id,
            @RequestParam boolean approved,
            @RequestParam(defaultValue = "admin") String approver,
            @RequestParam(required = false) String comment) {

        Optional<ApprovalRequest> result =
                approvalService.approve(id, approved, approver, comment);

        if (result.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "error", "审批请求不存在或状态不正确"));
        }

        ApprovalRequest request = result.get();
        return ResponseEntity.ok(
                Map.of(
                        "success", true,
                        "approvalId", request.getId(),
                        "status", request.getStatus().name(),
                        "approver", request.getApprover()));
    }

    /**
     * 根据工具调用 ID 执行审批.
     */
    @PostMapping("/tool-call/{sessionId}/{toolCallId}")
    public ResponseEntity<Map<String, Object>> approveByToolCallId(
            @PathVariable String sessionId,
            @PathVariable String toolCallId,
            @RequestParam boolean approved,
            @RequestParam(defaultValue = "admin") String approver,
            @RequestParam(required = false) String comment) {

        Optional<ApprovalRequest> result =
                approvalService.approveByToolCallId(sessionId, toolCallId, approved, approver, comment);

        if (result.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "error", "审批请求不存在或状态不正确"));
        }

        ApprovalRequest request = result.get();
        return ResponseEntity.ok(
                Map.of(
                        "success", true,
                        "approvalId", request.getId(),
                        "toolCallId", request.getToolCallId(),
                        "status", request.getStatus().name(),
                        "approver", request.getApprover()));
    }

    /**
     * 批量审批：拒绝指定会话的所有待审批请求.
     * 通常用于会话超时或用户主动取消时调用.
     */
    @PostMapping("/reject-all/{sessionId}")
    public ResponseEntity<Map<String, Object>> rejectAll(
            @PathVariable String sessionId,
            @RequestParam(defaultValue = "admin") String approver,
            @RequestParam(required = false) String reason) {

        int count = approvalService.batchApproveBySession(
                sessionId, false, approver, reason != null ? reason : "批量拒绝");
        return ResponseEntity.ok(
                Map.of(
                        "success", true,
                        "rejectedCount", count,
                        "sessionId", sessionId));
    }

    /**
     * 批量批准：批准指定会话的所有待审批请求.
     */
    @PostMapping("/approve-all/{sessionId}")
    public ResponseEntity<Map<String, Object>> approveAll(
            @PathVariable String sessionId,
            @RequestParam(defaultValue = "admin") String approver,
            @RequestParam(required = false) String reason) {

        int count = approvalService.batchApproveBySession(
                sessionId, true, approver, reason != null ? reason : "批量批准");
        return ResponseEntity.ok(
                Map.of(
                        "success", true,
                        "approvedCount", count,
                        "sessionId", sessionId));
    }

    /**
     * 获取审批统计数据.
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        ApprovalService.ApprovalStats stats = approvalService.getStats();
        return ResponseEntity.ok(
                Map.of(
                        "total", stats.total(),
                        "pending", stats.pending(),
                        "approved", stats.approved(),
                        "rejected", stats.rejected(),
                        "timeout", stats.timeout()));
    }
}