package io.agentscope.tutorial.chapter12.controller;

import io.agentscope.tutorial.chapter12.model.Plan;
import io.agentscope.tutorial.chapter12.dto.ChatRequest;
import io.agentscope.tutorial.chapter12.service.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * 对话控制器 - 处理与 Agent 的交互.
 *
 * <p>提供对话接口，支持流式响应（SSE）。
 *
 * <p>端点：
 * <ul>
 *   <li>POST /api/chat        - 发送消息，获取流式响应</li>
 *   <li>POST /api/chat/resume - 恢复暂停中的 Agent</li>
 *   <li>POST /api/chat/stop   - 请求停止 Agent</li>
 *   <li>GET  /api/chat/status - 获取 Agent 状态</li>
 *   <li>POST /api/reset       - 重置 Agent 和计划</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 */
@RestController
@RequestMapping("/api")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * 发送消息并获取流式响应.
     *
     * <p>返回 Server-Sent Events (SSE) 流，每个事件是一个 JSON 对象：
     * <ul>
     *   <li>{"t":"think","d":"..."} - 思维内容</li>
     *   <li>{"t":"text","d":"..."} - 文本响应</li>
     *   <li>{"t":"tool","n":"...","d":"..."} - 工具结果</li>
     *   <li>{"t":"paused"} - Agent 暂停等待确认</li>
     *   <li>{"t":"ctx",...} - 调试上下文信息</li>
     * </ul>
     */
    @PostMapping(path = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(
            @RequestBody ChatRequest request,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = "default";
        }

        log.info("Chat request [session={}]: {}", sessionId, truncate(request.getMessage(), 100));

        return chatService.chat(sessionId, request.getMessage());
    }

    /**
     * 恢复暂停中的 Agent.
     */
    @PostMapping(path = "/chat/resume", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> resume(
            @RequestHeader(value = "X-Session-Id", defaultValue = "default") String sessionId) {
        log.info("Resume request [session={}]", sessionId);
        return chatService.resume(sessionId);
    }

    /**
     * 请求停止 Agent（在下一个计划工具执行后暂停）.
     */
    @PostMapping("/chat/stop")
    public Map<String, Object> stop() {
        log.info("Stop requested");
        chatService.requestStop();
        return Map.of(
                "status", "stop_requested",
                "message", "Agent will pause after next plan tool"
        );
    }

    /**
     * 获取 Agent 状态.
     */
    @GetMapping("/chat/status")
    public Map<String, Object> getStatus() {
        boolean paused = chatService.isPaused();
        Plan plan = chatService.getCurrentPlan();

        return Map.of(
                "paused", paused,
                "hasPlan", plan != null,
                "planName", plan != null ? plan.getName() : null,
                "subtaskCount", plan != null && plan.getSubtasks() != null ? plan.getSubtasks().size() : 0
        );
    }

    /**
     * 重置 Agent 和计划.
     */
    @PostMapping("/reset")
    public Map<String, Object> reset() {
        log.info("Reset requested");
        chatService.reset();
        return Map.of(
                "status", "reset",
                "message", "Agent and plan have been reset"
        );
    }

    /**
     * 健康检查端点.
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "service", "chapter12-plan-tutorial",
                "version", "1.0.0"
        );
    }

    // ==================== 私有辅助方法 ====================

    private String truncate(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return s.substring(0, max) + "...";
    }
}