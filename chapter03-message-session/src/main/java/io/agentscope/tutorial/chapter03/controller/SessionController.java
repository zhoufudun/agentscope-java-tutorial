package io.agentscope.tutorial.chapter03.controller;

import io.agentscope.tutorial.chapter03.entity.ChatSession;
import io.agentscope.tutorial.chapter03.service.ChatService;
import io.agentscope.tutorial.chapter03.service.SessionService;
import io.agentscope.tutorial.chapter03.service.ChatResponseDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 会话控制器
 *
 * 提供会话管理的 REST API：
 * - 创建会话
 * - 查询会话列表
 * - 删除会话
 * - 清空会话消息
 *
 * @author AgentScope Tutorial
 */
@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private static final Logger log = LoggerFactory.getLogger(SessionController.class);

    private final SessionService sessionService;
    private final ChatService chatService;

    public SessionController(SessionService sessionService, ChatService chatService) {
        this.sessionService = sessionService;
        this.chatService = chatService;
    }

    /**
     * 创建新会话
     * POST /api/sessions
     */
    @PostMapping
    public ResponseEntity<ChatSession> createSession(@RequestBody CreateSessionRequest request) {
        String userId = request.getUserId() != null ? request.getUserId() : "default_user";
        String title = request.getTitle();

        ChatSession session = chatService.createNewSession(userId, title);
        log.info("创建会话: sessionId={}, userId={}, title={}", session.getSessionId(), userId, title);

        return ResponseEntity.ok(session);
    }

    /**
     * 获取会话列表
     * GET /api/sessions
     */
    @GetMapping
    public ResponseEntity<List<ChatSession>> listSessions(
            @RequestParam(defaultValue = "default_user") String userId) {
        List<ChatSession> sessions = sessionService.getUserSessions(userId);
        return ResponseEntity.ok(sessions);
    }

    /**
     * 获取单个会话
     * GET /api/sessions/{sessionId}
     */
    @GetMapping("/{sessionId}")
    public ResponseEntity<ChatSession> getSession(@PathVariable String sessionId) {
        return sessionService.getSession(sessionId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 获取会话历史
     * GET /api/sessions/{sessionId}/history
     */
    @GetMapping("/{sessionId}/history")
    public ResponseEntity<List<ChatResponseDTO.MessageInfo>> getSessionHistory(
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

    /**
     * 更新会话标题
     * PATCH /api/sessions/{sessionId}
     */
    @PatchMapping("/{sessionId}")
    public ResponseEntity<ChatSession> updateSessionTitle(
            @PathVariable String sessionId,
            @RequestBody UpdateSessionRequest request) {
        if (request.getTitle() == null || request.getTitle().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        sessionService.updateSessionTitle(sessionId, request.getTitle());
        return sessionService.getSession(sessionId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 清空会话消息
     * DELETE /api/sessions/{sessionId}/messages
     */
    @DeleteMapping("/{sessionId}/messages")
    public ResponseEntity<Void> clearSession(@PathVariable String sessionId) {
        chatService.clearSession(sessionId);
        log.info("清空会话消息: sessionId={}", sessionId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 删除会话
     * DELETE /api/sessions/{sessionId}
     */
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> deleteSession(@PathVariable String sessionId) {
        chatService.deleteSession(sessionId);
        log.info("删除会话: sessionId={}", sessionId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 导出对话日志（调试用）
     * GET /api/sessions/{sessionId}/export
     */
    @GetMapping("/{sessionId}/export")
    public ResponseEntity<String> exportConversation(@PathVariable String sessionId) {
        String log = chatService.exportConversationLog(sessionId);
        return ResponseEntity.ok()
                .header("Content-Type", "application/json; charset=utf-8")
                .body(log);
    }

    /**
     * 重置对话历史（内存）
     * POST /api/sessions/{sessionId}/reset
     */
    @PostMapping("/{sessionId}/reset")
    public ResponseEntity<Void> resetConversation(@PathVariable String sessionId) {
        chatService.resetConversationHistory(sessionId);
        log.info("重置对话历史: sessionId={}", sessionId);
        return ResponseEntity.ok().build();
    }

    // ==================== 请求 DTO ====================

    public static class CreateSessionRequest {
        private String userId;
        private String title;

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }
    }

    public static class UpdateSessionRequest {
        private String title;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }
    }
}