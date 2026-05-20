package io.agentscope.tutorial.chapter03.controller;

import io.agentscope.tutorial.chapter03.service.ChatService;
import io.agentscope.tutorial.chapter03.service.ChatResponseDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 聊天控制器
 *
 * 提供聊天功能的 REST API：
 * - 发送消息
 * - 流式聊天（预留）
 * - 获取对话状态
 *
 * @author AgentScope Tutorial
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * 发送消息
     * POST /api/chat
     *
     * @param request 聊天请求
     * @return 对话响应
     */
    @PostMapping
    public ResponseEntity<ChatResponseDTO> chat(@Valid @RequestBody ChatRequest request) {
        log.info("收到聊天请求: sessionId={}, messageLength={}",
                request.getSessionId(),
                request.getMessage() != null ? request.getMessage().length() : 0);

        ChatResponseDTO response = chatService.chat(
                request.getSessionId(),
                request.getMessage(),
                request.getSystemPrompt()
        );

        log.info("聊天响应: sessionId={}, duration={}ms, contentLength={}",
                response.getSessionId(),
                response.getDurationMs(),
                response.getContent() != null ? response.getContent().length() : 0);

        return ResponseEntity.ok(response);
    }

    /**
     * 发送消息（简化版本）
     * POST /api/chat/message
     *
     * @param sessionId 会话ID
     * @param message   消息内容
     * @param systemPrompt 系统提示词（可选）
     * @return 对话响应
     */
    @PostMapping("/message")
    public ResponseEntity<ChatResponseDTO> sendMessage(
            @RequestParam @NotBlank String sessionId,
            @RequestParam @NotBlank String message,
            @RequestParam(required = false) String systemPrompt) {

        ChatResponseDTO response = chatService.chat(sessionId, message, systemPrompt);
        return ResponseEntity.ok(response);
    }

    /**
     * 获取当前对话历史
     * GET /api/chat/{sessionId}/history
     */
    @GetMapping("/{sessionId}/history")
    public ResponseEntity<java.util.List<ChatResponseDTO.MessageInfo>> getHistory(
            @PathVariable String sessionId) {
        var history = chatService.getSessionHistory(sessionId);
        var infos = history.stream()
                .filter(m -> m.getRole() != io.agentscope.core.message.MsgRole.SYSTEM)
                .map(m -> new ChatResponseDTO.MessageInfo(
                        m.getId(),
                        m.getRole().name(),
                        m.getTextContent(),
                        m.getTimestamp()
                ))
                .toList();
        return ResponseEntity.ok(infos);
    }

    // ==================== 请求 DTO ====================

    public static class ChatRequest {
        @NotBlank(message = "会话 ID 不能为空")
        private String sessionId;

        @NotBlank(message = "用户消息不能为空")
        private String message;

        // 可选：系统提示词
        private String systemPrompt;

        // 可选：额外参数
        private java.util.Map<String, Object> options;

        // Getter 和 Setter

        public String getSessionId() {
            return sessionId;
        }

        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getSystemPrompt() {
            return systemPrompt;
        }

        public void setSystemPrompt(String systemPrompt) {
            this.systemPrompt = systemPrompt;
        }

        public java.util.Map<String, Object> getOptions() {
            return options;
        }

        public void setOptions(java.util.Map<String, Object> options) {
            this.options = options;
        }
    }
}