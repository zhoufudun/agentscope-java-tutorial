package io.agentscope.tutorial.chapter03.service;

import java.util.List;

/**
 * 对话响应 DTO
 *
 * 用于 API 响应返回值
 *
 * @author AgentScope Tutorial
 */
public class ChatResponseDTO {

    private String sessionId;
    private String messageId;
    private String content;
    private String role;
    private long tokens;
    private long durationMs;
    private List<MessageInfo> history;

    // 内部类：消息信息
    public static class MessageInfo {
        private String id;
        private String role;
        private String content;
        private String timestamp;

        public MessageInfo() {
        }

        public MessageInfo(String id, String role, String content, String timestamp) {
            this.id = id;
            this.role = role;
            this.content = content;
            this.timestamp = timestamp;
        }

        // Getter 和 Setter

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }
    }

    // Builder 模式

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final ChatResponseDTO response = new ChatResponseDTO();

        public Builder sessionId(String sessionId) {
            response.sessionId = sessionId;
            return this;
        }

        public Builder messageId(String messageId) {
            response.messageId = messageId;
            return this;
        }

        public Builder content(String content) {
            response.content = content;
            return this;
        }

        public Builder role(String role) {
            response.role = role;
            return this;
        }

        public Builder tokens(long tokens) {
            response.tokens = tokens;
            return this;
        }

        public Builder durationMs(long durationMs) {
            response.durationMs = durationMs;
            return this;
        }

        public Builder history(List<MessageInfo> history) {
            response.history = history;
            return this;
        }

        public ChatResponseDTO build() {
            return response;
        }
    }

    // Getter

    public String getSessionId() {
        return sessionId;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getContent() {
        return content;
    }

    public String getRole() {
        return role;
    }

    public long getTokens() {
        return tokens;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public List<MessageInfo> getHistory() {
        return history;
    }

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