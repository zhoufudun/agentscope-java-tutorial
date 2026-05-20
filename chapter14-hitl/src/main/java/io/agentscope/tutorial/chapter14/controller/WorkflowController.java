package io.agentscope.tutorial.chapter14.controller;

import io.agentscope.tutorial.chapter14.service.ChatService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * 工作流控制器.
 *
 * <p>提供工作流相关的接口，包括会话管理和审批恢复.
 *
 * @author AgentScope Tutorial
 */
@RestController
@RequestMapping("/api/workflow")
public class WorkflowController {

    private final ChatService chatService;

    public WorkflowController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * 恢复工作流（审批后）.
     *
     * @param sessionId  会话 ID
     * @param toolCallId 工具调用 ID
     * @param approved   是否批准
     * @param approver   审批人
     * @param comment    审批意见
     * @return SSE 事件流
     */
    @PostMapping(value = "/resume", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Map<String, Object>>> resume(
            @RequestParam String sessionId,
            @RequestParam String toolCallId,
            @RequestParam boolean approved,
            @RequestParam(defaultValue = "admin") String approver,
            @RequestParam(required = false) String comment) {

        Flux<Map<String, Object>> events =
                chatService.handleApproval(sessionId, toolCallId, approved, approver, comment);
        return events.map(data -> ServerSentEvent.<Map<String, Object>>builder().data(data).build());
    }

    /**
     * 中断工作流.
     */
    @PostMapping("/interrupt/{sessionId}")
    public ResponseEntity<Map<String, Object>> interrupt(@PathVariable String sessionId) {
        chatService.interrupt(sessionId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "工作流已中断",
                "sessionId", sessionId));
    }

    /**
     * 重置工作流（清空会话）.
     */
    @DeleteMapping("/reset/{sessionId}")
    public ResponseEntity<Map<String, Object>> reset(@PathVariable String sessionId) {
        chatService.clearSession(sessionId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "工作流已重置",
                "sessionId", sessionId));
    }

    /**
     * 获取工作流状态.
     */
    @GetMapping("/status/{sessionId}")
    public ResponseEntity<Map<String, Object>> getStatus(@PathVariable String sessionId) {
        // 返回工作流状态信息
        return ResponseEntity.ok(Map.of(
                "sessionId", sessionId,
                "status", "UNKNOWN",
                "message", "工作流状态查询功能开发中"));
    }
}