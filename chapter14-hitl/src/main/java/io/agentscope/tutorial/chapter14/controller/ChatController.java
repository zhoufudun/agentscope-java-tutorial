package io.agentscope.tutorial.chapter14.controller;

import io.agentscope.tutorial.chapter14.service.ChatService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * 聊天控制器.
 *
 * <p>提供聊天接口，支持 SSE 流式响应和审批决策回传.
 *
 * @author AgentScope Tutorial
 */
@RestController
@RequestMapping("/api")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * 发送聊天消息.
     * 返回 SSE 流式响应，实时推送代理推理和执行过程.
     *
     * @param request 包含 sessionId 和 message 的请求体
     * @return SSE 事件流
     */
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Map<String, Object>>> chat(@RequestBody Map<String, String> request) {
        String sessionId = request.getOr默认("sessionId", "default");
        String message = request.get("message");

        if (message == null || message.isBlank()) {
            return Flux.just(
                    ServerSentEvent.<Map<String, Object>>builder()
                            .data(Map.of("type", "ERROR", "error", "消息不能为空"))
                            .build());
        }

        Flux<Map<String, Object>> events = chatService.chat(sessionId, message);
        return events.map(data -> ServerSentEvent.<Map<String, Object>>builder().data(data).build());
    }

    /**
     * 处理审批决策.
     * 当前端用户点击"批准"或"拒绝"按钮时调用此接口，恢复代理执行.
     *
     * @param request 包含审批决策的请求体
     * @return SSE 事件流（恢复执行后的响应）
     */
    @PostMapping(value = "/approve", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Map<String, Object>>> approve(@RequestBody Map<String, Object> request) {
        String sessionId = (String) request.getOr默认("sessionId", "default");
        String toolCallId = (String) request.get("toolCallId");
        boolean approved = Boolean.TRUE.equals(request.get("approved"));
        String approver = (String) request.getOr默认("approver", "admin");
        String comment = (String) request.get("comment");

        if (toolCallId == null || toolCallId.isBlank()) {
            return Flux.just(
                    ServerSentEvent.<Map<String, Object>>builder()
                            .data(Map.of("type", "ERROR", "error", "toolCallId 不能为空"))
                            .build());
        }

        Flux<Map<String, Object>> events =
                chatService.handleApproval(sessionId, toolCallId, approved, approver, comment);
        return events.map(data -> ServerSentEvent.<Map<String, Object>>builder().data(data).build());
    }

    /**
     * 中断正在执行的代理.
     */
    @PostMapping("/interrupt/{sessionId}")
    public ResponseEntity<Map<String, Object>> interrupt(@PathVariable String sessionId) {
        chatService.interrupt(sessionId);
        return ResponseEntity.ok(Map.of("success", true, "message", "代理已中断"));
    }

    /**
     * 清空会话.
     */
    @DeleteMapping("/session/{sessionId}")
    public ResponseEntity<Map<String, Object>> clearSession(@PathVariable String sessionId) {
        chatService.clearSession(sessionId);
        return ResponseEntity.ok(Map.of("success", true, "message", "会话已清空"));
    }
}