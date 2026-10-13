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

import io.agentscope.tutorial.chapter03.service.ChatService;
import io.agentscope.tutorial.chapter03.service.ChatResponseDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 聊天控制器 - 处理对话相关的 HTTP 请求.
 *
 * <p>本控制器提供聊天功能的 REST API，是用户与 Agent 进行对话的主要入口。
 *
 * <h2>API 端点</h2>
 * <ul>
 *   <li>{@code POST /api/chat} - 发送消息（使用 JSON 请求体）</li>
 *   <li>{@code POST /api/chat/message} - 发送消息（使用请求参数，简化版）</li>
 *   <li>{@code GET /api/chat/{sessionId}/history} - 获取对话历史</li>
 * </ul>
 *
 * <h2>请求示例</h2>
 * <pre>{@code
 * # 发送消息
 * curl -X POST http://localhost:8080/api/chat \
 *   -H "Content-Type: application/json" \
 *   -d '{"sessionId":"abc123","message":"你好"}'
 *
 * # 获取历史
 * curl http://localhost:8080/api/chat/abc123/history
 * }</pre>
 *
 * @author AgentScope Tutorial
 * @since 1.0
 * @see ChatService
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    /**
     * 聊天服务，处理核心对话逻辑.
     */
    private final ChatService chatService;

    /**
     * 构造函数，通过依赖注入获取 ChatService.
     *
     * @param chatService 聊天服务
     */
    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * 发送消息并获取 Agent 响应.
     *
     * <p>这是主要的对话接口，接收 JSON 格式的请求体。
     *
     * <p>请求体示例：
     * <pre>{@code
     * {
     *   "sessionId": "会话ID",
     *   "message": "用户消息",
     *   "systemPrompt": "可选的系统提示词"
     * }
     * }</pre>
     *
     * @param request 聊天请求，包含 sessionId 和 message
     * @return 对话响应，包含 Agent 的回复
     */
    @PostMapping
    public ResponseEntity<ChatResponseDTO> chat(@Valid @RequestBody ChatRequest request) {
        log.info("收到聊天请求: sessionId={}, messageLength={}",
                request.getSessionId(),
                request.getMessage() != null ? request.getMessage().length() : 0);

        // 调用聊天服务处理消息
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
     * 发送消息（简化版本，使用请求参数）.
     *
     * <p>适合简单的测试场景，参数直接通过 URL 传递。
     *
     * <p>请求示例：
     * <pre>{@code
     * POST /api/chat/message?sessionId=abc123&message=你好&systemPrompt=你是助手
     * }</pre>
     *
     * @param sessionId 会话ID
     * @param message   用户消息
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
     * 获取会话的对话历史.
     *
     * <p>返回指定会话中的所有消息（不包含系统消息），
     * 用于在前端展示对话记录。
     *
     * @param sessionId 会话ID
     * @return 消息列表，每条消息包含 id、role、content、timestamp
     */
    @GetMapping("/{sessionId}/history")
    public ResponseEntity<java.util.List<ChatResponseDTO.MessageInfo>> getHistory(
            @PathVariable String sessionId) {
        // 从服务获取原始消息历史
        var history = chatService.getSessionHistory(sessionId);
        // 过滤掉系统消息，转换为 MessageInfo 格式
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

    /**
     * 聊天请求 DTO.
     *
     * <p>封装聊天 API 的请求参数，支持 JSR-303 参数校验。
     */
    public static class ChatRequest {

        /**
         * 会话ID，用于关联多轮对话.
         */
        @NotBlank(message = "会话 ID 不能为空")
        private String sessionId;

        /**
         * 用户发送的消息内容.
         */
        @NotBlank(message = "用户消息不能为空")
        private String message;

        /**
         * 系统提示词（可选）.
         *
         * <p>用于定制 Agent 的行为和角色。
         */
        private String systemPrompt;

        /**
         * 额外的请求参数（可选）.
         *
         * <p>可传递自定义配置，如 temperature、maxTokens 等。
         */
        private java.util.Map<String, Object> options;

        // ========== Getter 和 Setter ==========

        /**
         * 获取会话ID.
         * @return 会话ID
         */
        public String getSessionId() {
            return sessionId;
        }

        /**
         * 设置会话ID.
         * @param sessionId 会话ID
         */
        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }

        /**
         * 获取用户消息.
         * @return 用户消息
         */
        public String getMessage() {
            return message;
        }

        /**
         * 设置用户消息.
         * @param message 用户消息
         */
        public void setMessage(String message) {
            this.message = message;
        }

        /**
         * 获取系统提示词.
         * @return 系统提示词
         */
        public String getSystemPrompt() {
            return systemPrompt;
        }

        /**
         * 设置系统提示词.
         * @param systemPrompt 系统提示词
         */
        public void setSystemPrompt(String systemPrompt) {
            this.systemPrompt = systemPrompt;
        }

        /**
         * 获取额外选项.
         * @return 选项映射
         */
        public java.util.Map<String, Object> getOptions() {
            return options;
        }

        /**
         * 设置额外选项.
         * @param options 选项映射
         */
        public void setOptions(java.util.Map<String, Object> options) {
            this.options = options;
        }
    }
}