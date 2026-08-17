package io.agentscope.tutorial.chapter05.controller;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.Event;
import io.agentscope.core.agent.StreamOptions;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.tutorial.chapter05.service.ToolService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * Agent 对话控制器
 *
 * <p>提供与 Agent 对话的 REST API 接口。
 *
 * @author AgentScope Tutorial
 * @since 1.0
 */
@RestController
@RequestMapping("/api/agent")
public class ChatController {

    private final ReActAgent agent;
    private final ToolService toolService;

    public ChatController(ReActAgent agent, ToolService toolService) {
        this.agent = agent;
        this.toolService = toolService;
    }

    /**
     * 发送消息给 Agent
     *
     * @param request 包含消息内容的请求
     * @return Agent 的回复
     */
    @PostMapping("/chat")
    public Map<String, Object> chat(@RequestBody ChatRequest request) {
        try {
            Msg userMsg = Msg.builder()
                .role(MsgRole.USER)
                .content(TextBlock.builder().text(request.getMessage()).build())
                .build();

            Msg response = agent.call(userMsg).block();

            String reply = response != null ? response.getTextContent() : "";

            return Map.of(
                "success", true,
                "reply", reply,
                "generateReason", response != null ? response.getGenerateReason().toString() : ""
            );
        } catch (Exception e) {
            return Map.of(
                "success", false,
                "error", e.getMessage()
            );
        }
    }

    /**
     * 流式对话
     *
     * @param request 包含消息的请求
     * @return SSE 流
     */
    @PostMapping("/chat/stream")
    public Flux<String> chatStream(@RequestBody ChatRequest request) {
        Msg userMsg = Msg.builder()
            .role(MsgRole.USER)
            .content(TextBlock.builder().text(request.getMessage()).build())
            .build();

        return Flux.create(sink -> {
            try {
                // 使用流式响应
                Flux<Event> stream = agent.stream(userMsg, StreamOptions.defaults());
                stream.subscribe(
                    event -> {
                        // 发送事件片段
                        String content = event.toString();
                        sink.next("data: " + content + "\n\n");
                    },
                    error -> {
                        sink.error(error);
                    },
                    () -> {
                        sink.complete();
                    }
                );
            } catch (Exception e) {
                sink.error(e);
            }
        });
    }

    /**
     * 重置 Agent 会话
     */
    @PostMapping("/reset")
    public Map<String, Object> reset() {
        try {
            // 2.x：会话历史保存在 AgentState 中，清空可变上下文即可重置会话
            agent.getAgentState().contextMutable().clear();
            return Map.of(
                "success", true,
                "message", "会话已重置"
            );
        } catch (Exception e) {
            return Map.of(
                "success", false,
                "error", e.getMessage()
            );
        }
    }

    /**
     * 获取 Agent 状态
     */
    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        return Map.of(
            "name", agent.getName(),
            "tools", toolService.getRegisteredTools(),
            "activeGroups", toolService.getActiveGroups(),
            "memorySize", agent.getAgentState().getContext().size()
        );
    }

    // ==================== 请求类 ====================

    public static class ChatRequest {
        private String message;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}