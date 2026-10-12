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
package io.agentscope.tutorial.chapter03.service;

import java.util.List;

/**
 * 对话响应 DTO（Data Transfer Object）.
 *
 * <p>用于封装聊天 API 的响应数据，在控制器和前端之间传递。
 *
 * <h2>字段说明</h2>
 * <ul>
 *   <li>{@code sessionId} - 会话ID，用于关联多轮对话</li>
 *   <li>{@code messageId} - 本次回复的消息ID</li>
 *   <li>{@code content} - Agent 回复的文本内容</li>
 *   <li>{@code role} - 消息角色（通常为 ASSISTANT）</li>
 *   <li>{@code tokens} - 本次请求消耗的 token 数量</li>
 *   <li>{@code durationMs} - 处理耗时（毫秒）</li>
 *   <li>{@code history} - 对话历史摘要</li>
 * </ul>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * ChatResponseDTO response = ChatResponseDTO.builder()
 *     .sessionId("session-123")
 *     .content("你好！有什么可以帮助你的？")
 *     .role("ASSISTANT")
 *     .durationMs(1500)
 *     .build();
 * }</pre>
 *
 * @author AgentScope Tutorial
 * @since 1.0
 */
public class ChatResponseDTO {

    /**
     * 会话ID.
     */
    private String sessionId;

    /**
     * 消息ID.
     */
    private String messageId;

    /**
     * 回复内容.
     */
    private String content;

    /**
     * 消息角色（ASSISTANT/USER/SYSTEM）.
     */
    private String role;

    /**
     * Token 消耗数量.
     */
    private long tokens;

    /**
     * 处理耗时（毫秒）.
     */
    private long durationMs;

    /**
     * 对话历史.
     */
    private List<MessageInfo> history;

    /**
     * 消息信息内部类.
     *
     * <p>用于在对话历史中表示单条消息的摘要信息。
     */
    public static class MessageInfo {

        /**
         * 消息ID.
         */
        private String id;

        /**
         * 消息角色.
         */
        private String role;

        /**
         * 消息内容.
         */
        private String content;

        /**
         * 时间戳.
         */
        private String timestamp;

        /**
         * 默认构造函数.
         */
        public MessageInfo() {
        }

        /**
         * 全参数构造函数.
         *
         * @param id 消息ID
         * @param role 消息角色
         * @param content 消息内容
         * @param timestamp 时间戳
         */
        public MessageInfo(String id, String role, String content, String timestamp) {
            this.id = id;
            this.role = role;
            this.content = content;
            this.timestamp = timestamp;
        }

        // ========== Getter 和 Setter ==========

        /**
         * 获取消息ID.
         * @return 消息ID
         */
        public String getId() {
            return id;
        }

        /**
         * 设置消息ID.
         * @param id 消息ID
         */
        public void setId(String id) {
            this.id = id;
        }

        /**
         * 获取消息角色.
         * @return 消息角色
         */
        public String getRole() {
            return role;
        }

        /**
         * 设置消息角色.
         * @param role 消息角色
         */
        public void setRole(String role) {
            this.role = role;
        }

        /**
         * 获取消息内容.
         * @return 消息内容
         */
        public String getContent() {
            return content;
        }

        /**
         * 设置消息内容.
         * @param content 消息内容
         */
        public void setContent(String content) {
            this.content = content;
        }

        /**
         * 获取时间戳.
         * @return 时间戳
         */
        public String getTimestamp() {
            return timestamp;
        }

        /**
         * 设置时间戳.
         * @param timestamp 时间戳
         */
        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }
    }

    // ==================== Builder 模式 ====================

    /**
     * 创建 Builder 实例.
     *
     * @return Builder 实例
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder 内部类 - 实现流式构建模式.
     */
    public static class Builder {

        /**
         * 正在构建的响应对象.
         */
        private final ChatResponseDTO response = new ChatResponseDTO();

        /**
         * 设置会话ID.
         * @param sessionId 会话ID
         * @return Builder 实例（链式调用）
         */
        public Builder sessionId(String sessionId) {
            response.sessionId = sessionId;
            return this;
        }

        /**
         * 设置消息ID.
         * @param messageId 消息ID
         * @return Builder 实例
         */
        public Builder messageId(String messageId) {
            response.messageId = messageId;
            return this;
        }

        /**
         * 设置回复内容.
         * @param content 回复内容
         * @return Builder 实例
         */
        public Builder content(String content) {
            response.content = content;
            return this;
        }

        /**
         * 设置消息角色.
         * @param role 消息角色
         * @return Builder 实例
         */
        public Builder role(String role) {
            response.role = role;
            return this;
        }

        /**
         * 设置 token 消耗.
         * @param tokens token 数量
         * @return Builder 实例
         */
        public Builder tokens(long tokens) {
            response.tokens = tokens;
            return this;
        }

        /**
         * 设置处理耗时.
         * @param durationMs 耗时（毫秒）
         * @return Builder 实例
         */
        public Builder durationMs(long durationMs) {
            response.durationMs = durationMs;
            return this;
        }

        /**
         * 设置对话历史.
         * @param history 消息列表
         * @return Builder 实例
         */
        public Builder history(List<MessageInfo> history) {
            response.history = history;
            return this;
        }

        /**
         * 构建最终的响应对象.
         * @return ChatResponseDTO 实例
         */
        public ChatResponseDTO build() {
            return response;
        }
    }

    // ==================== Getter 方法 ====================

    /**
     * 获取会话ID.
     * @return 会话ID
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * 获取消息ID.
     * @return 消息ID
     */
    public String getMessageId() {
        return messageId;
    }

    /**
     * 获取回复内容.
     * @return 回复内容
     */
    public String getContent() {
        return content;
    }

    /**
     * 获取消息角色.
     * @return 消息角色
     */
    public String getRole() {
        return role;
    }

    /**
     * 获取 token 消耗.
     * @return token 数量
     */
    public long getTokens() {
        return tokens;
    }

    /**
     * 获取处理耗时.
     * @return 耗时（毫秒）
     */
    public long getDurationMs() {
        return durationMs;
    }

    /**
     * 获取对话历史.
     * @return 消息列表
     */
    public List<MessageInfo> getHistory() {
        return history;
    }

    /**
     * 返回响应的字符串表示.
     *
     * @return 包含关键信息的字符串（内容过长时截断）
     */
    @Override
    public String toString() {
        return "ChatResponseDTO{" +
                "sessionId='" + sessionId + '\'' +
                ", messageId='" + messageId + '\'' +
                ", content='" + (content != null && content.length() > 50 ? content.substring(0, 50) + "..." : content) + '\'' +
                ", role='" + role + '\'' +
                ", durationMs=" + durationMs +
                ", historySize=" + (history != null ? history.size() : 0) +
                '}';
    }
}