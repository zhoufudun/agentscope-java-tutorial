package io.agentscope.tutorial.chapter03.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 会话实体 - 存储在 H2/MySQL 数据库中
 *
 * 对应数据库表: chat_sessions
 *
 * @author AgentScope Tutorial
 */
@Entity
@Table(name = "chat_sessions", indexes = {
        @Index(name = "idx_sessions_user_id", columnList = "user_id"),
        @Index(name = "idx_sessions_updated", columnList = "updated_at DESC")
})
public class ChatSession {

    @Id
    @Column(name = "session_id", length = 64)
    private String sessionId;

    @Column(name = "user_id", length = 64, nullable = false)
    private String userId;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "message_count")
    private Integer messageCount = 0;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    /**
     * JPA 默认构造函数
     */
    public ChatSession() {
    }

    /**
     * 创建新会话
     *
     * @param sessionId 会话ID
     * @param userId    用户ID
     * @param title     会话标题
     */
    public ChatSession(String sessionId, String userId, String title) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.title = title;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.messageCount = 0;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (messageCount == null) {
            messageCount = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getter 和 Setter

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(Integer messageCount) {
        this.messageCount = messageCount;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    @Override
    public String toString() {
        return "ChatSession{" +
                "sessionId='" + sessionId + '\'' +
                ", userId='" + userId + '\'' +
                ", title='" + title + '\'' +
                ", messageCount=" + messageCount +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}