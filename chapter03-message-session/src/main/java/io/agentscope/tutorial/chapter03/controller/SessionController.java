/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
 * 会话控制器 - 处理会话管理相关的 HTTP 请求.
 *
 * <p>本控制器提供会话生命周期管理的 REST API，是多会话场景的核心接口。
 *
 * <h2>API 端点</h2>
 * <ul>
 *   <li>{@code POST /api/sessions} - 创建新会话</li>
 *   <li>{@code GET /api/sessions} - 获取用户的会话列表</li>
 *   <li>{@code GET /api/sessions/{id}} - 获取单个会话详情</li>
 *   <li>{@code GET /api/sessions/{id}/history} - 获取会话消息历史</li>
 *   <li>{@code PATCH /api/sessions/{id}} - 更新会话标题</li>
 *   <li>{@code DELETE /api/sessions/{id}/messages} - 清空会话消息</li>
 *   <li>{@code DELETE /api/sessions/{id}} - 删除会话</li>
 *   <li>{@code GET /api/sessions/{id}/export} - 导出对话日志</li>
 *   <li>{@code POST /api/sessions/{id}/reset} - 重置内存中的对话历史</li>
 * </ul>
 *
 * <h2>使用场景</h2>
 * <ul>
 *   <li>多用户、多会话的聊天应用</li>
 *   <li>会话历史的持久化和恢复</li>
 *   <li>对话日志的导出和分析</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 * @since 1.0
 * @see SessionService
 * @see ChatService
 */
@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private static final Logger log = LoggerFactory.getLogger(SessionController.class);

    /**
     * 会话服务，处理会话的 CRUD 操作.
     */
    private final SessionService sessionService;

    /**
     * 聊天服务，处理对话相关操作.
     */
    private final ChatService chatService;

    /**
     * 构造函数，通过依赖注入获取服务.
     *
     * @param sessionService 会话服务
     * @param chatService 聊天服务
     */
    public SessionController(SessionService sessionService, ChatService chatService) {
        this.sessionService = sessionService;
        this.chatService = chatService;
    }

    /**
     * 创建新会话.
     *
     * <p>请求体示例：
     * <pre>{@code
     * {
     *   "userId": "user123",
     *   "title": "关于AgentScope的讨论"
     * }
     * }</pre>
     *
     * @param request 创建会话请求
     * @return 创建的会话实体
     */
    @PostMapping
    public ResponseEntity<ChatSession> createSession(@RequestBody CreateSessionRequest request) {
        // 如果未提供 userId，使用默认值
        String userId = request.getUserId() != null ? request.getUserId() : "default_user";
        String title = request.getTitle();

        ChatSession session = chatService.createNewSession(userId, title);
        log.info("创建会话: sessionId={}, userId={}, title={}", session.getSessionId(), userId, title);

        return ResponseEntity.ok(session);
    }

    /**
     * 获取用户的所有会话列表.
     *
     * <p>返回按更新时间降序排列的会话列表，最近活跃的会话在前。
     *
     * @param userId 用户ID，默认为 "default_user"
     * @return 会话列表
     */
    @GetMapping
    public ResponseEntity<List<ChatSession>> listSessions(
            @RequestParam(defaultValue = "default_user") String userId) {
        List<ChatSession> sessions = sessionService.getUserSessions(userId);
        return ResponseEntity.ok(sessions);
    }

    /**
     * 获取单个会话的详细信息.
     *
     * @param sessionId 会话ID
     * @return 会话实体，如果不存在返回 404
     */
    @GetMapping("/{sessionId}")
    public ResponseEntity<ChatSession> getSession(@PathVariable String sessionId) {
        return sessionService.getSession(sessionId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 获取会话的消息历史.
     *
     * <p>返回会话中的所有消息（不包含系统消息），用于前端展示。
     *
     * @param sessionId 会话ID
     * @return 消息列表
     */
    @GetMapping("/{sessionId}/history")
    public ResponseEntity<List<ChatResponseDTO.MessageInfo>> getSessionHistory(
            @PathVariable String sessionId) {
        var history = chatService.getSessionHistory(sessionId);
        // 过滤系统消息，转换为 DTO
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
     * 更新会话标题.
     *
     * <p>使用 PATCH 方法进行部分更新，只修改标题字段。
     *
     * @param sessionId 会话ID
     * @param request 更新请求，包含新标题
     * @return 更新后的会话实体
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
     * 清空会话的所有消息.
     *
     * <p>保留会话本身，只清除消息历史。适用于重新开始对话的场景。
     *
     * @param sessionId 会话ID
     * @return 204 No Content
     */
    @DeleteMapping("/{sessionId}/messages")
    public ResponseEntity<Void> clearSession(@PathVariable String sessionId) {
        chatService.clearSession(sessionId);
        log.info("清空会话消息: sessionId={}", sessionId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 删除会话及其所有消息.
     *
     * <p>彻底删除会话，包括会话元数据和所有消息历史。此操作不可恢复。
     *
     * @param sessionId 会话ID
     * @return 204 No Content
     */
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> deleteSession(@PathVariable String sessionId) {
        chatService.deleteSession(sessionId);
        log.info("删除会话: sessionId={}", sessionId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 导出对话日志.
     *
     * <p>以 JSON 格式导出会话的完整对话记录，用于调试或数据分析。
     *
     * @param sessionId 会话ID
     * @return JSON 格式的对话日志
     */
    @GetMapping("/{sessionId}/export")
    public ResponseEntity<String> exportConversation(@PathVariable String sessionId) {
        String logContent = chatService.exportConversationLog(sessionId);
        return ResponseEntity.ok()
                .header("Content-Type", "application/json; charset=utf-8")
                .body(logContent);
    }

    /**
     * 重置对话历史（内存）.
     *
     * <p>清除内存中的对话历史缓存，但保留数据库中的持久化数据。
     * 下次对话时会重新从数据库加载历史。
     *
     * @param sessionId 会话ID
     * @return 200 OK
     */
    @PostMapping("/{sessionId}/reset")
    public ResponseEntity<Void> resetConversation(@PathVariable String sessionId) {
        chatService.resetConversationHistory(sessionId);
        log.info("重置对话历史: sessionId={}", sessionId);
        return ResponseEntity.ok().build();
    }

    // ==================== 请求 DTO ====================

    /**
     * 创建会话请求 DTO.
     */
    public static class CreateSessionRequest {

        /**
         * 用户ID，用于区分不同用户的会话.
         */
        private String userId;

        /**
         * 会话标题.
         */
        private String title;

        /**
         * 获取用户ID.
         * @return 用户ID
         */
        public String getUserId() {
            return userId;
        }

        /**
         * 设置用户ID.
         * @param userId 用户ID
         */
        public void setUserId(String userId) {
            this.userId = userId;
        }

        /**
         * 获取会话标题.
         * @return 会话标题
         */
        public String getTitle() {
            return title;
        }

        /**
         * 设置会话标题.
         * @param title 会话标题
         */
        public void setTitle(String title) {
            this.title = title;
        }
    }

    /**
     * 更新会话请求 DTO.
     */
    public static class UpdateSessionRequest {

        /**
         * 新的会话标题.
         */
        private String title;

        /**
         * 获取标题.
         * @return 标题
         */
        public String getTitle() {
            return title;
        }

        /**
         * 设置标题.
         * @param title 标题
         */
        public void setTitle(String title) {
            this.title = title;
        }
    }
}