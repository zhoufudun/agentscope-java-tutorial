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
package io.agentscope.tutorial.chapter03.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 会话实体类 - 存储在 H2/MySQL 数据库中.
 *
 * <p>本类是 JPA 实体，用于将会话元数据持久化到关系型数据库。对应数据库表：{@code chat_sessions}。
 *
 * <h2>设计说明</h2>
 * <p>在 AgentScope 的架构中，会话管理可以采用多种方式：
 * <ul>
 *   <li><b>JPA 方式（本类）</b>：将会话元数据存储在关系型数据库，便于查询和管理</li>
 *   <li><b>AgentStateStore 方式</b>：使用 AgentScope 原生的状态存储，适合框架内部使用</li>
 * </ul>
 *
 * <h2>表结构</h2>
 * <pre>
 * CREATE TABLE chat_sessions (
 *     session_id VARCHAR(64) PRIMARY KEY,
 *     user_id VARCHAR(64) NOT NULL,
 *     title VARCHAR(255),
 *     created_at TIMESTAMP,
 *     updated_at TIMESTAMP,
 *     message_count INT DEFAULT 0,
 *     metadata TEXT
 * );
 * </pre>
 *
 * <h2>索引设计</h2>
 * <ul>
 *   <li>{@code idx_sessions_user_id}：按用户ID查询会话列表</li>
 *   <li>{@code idx_sessions_updated}：按更新时间排序，用于获取最近会话</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 * @since 1.0
 * @see ChatMessage
 */
@Entity
@Table(name = "chat_sessions", indexes = {
        // 用户ID索引，加速按用户查询会话列表
        @Index(name = "idx_sessions_user_id", columnList = "user_id"),
        // 更新时间降序索引，加速获取最近活跃的会话
        @Index(name = "idx_sessions_updated", columnList = "updated_at DESC")
})
public class ChatSession {

    /**
     * 会话唯一标识符（UUID 格式）.
     *
     * <p>作为主键，长度限制 64 字符以适应 UUID 格式。
     */
    @Id
    @Column(name = "session_id", length = 64)
    private String sessionId;

    /**
     * 用户标识符.
     *
     * <p>用于区分不同用户的会话，支持多租户场景。
     */
    @Column(name = "user_id", length = 64, nullable = false)
    private String userId;

    /**
     * 会话标题.
     *
     * <p>通常取自第一条消息的摘要，或由用户自定义。
     */
    @Column(name = "title", length = 255)
    private String title;

    /**
     * 会话创建时间.
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * 会话最后更新时间.
     *
     * <p>每次有新消息时自动更新，用于按活跃度排序。
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 会话中的消息数量.
     *
     * <p>用于快速显示会话信息，避免每次都进行 COUNT 查询。
     */
    @Column(name = "message_count")
    private Integer messageCount = 0;

    /**
     * 会话元数据（JSON 格式）.
     *
     * <p>可存储自定义配置，如系统提示词、模型参数等。
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    /**
     * JPA 默认构造函数.
     *
     * <p>JPA 规范要求实体类必须有无参构造函数。
     */
    public ChatSession() {
    }

    /**
     * 创建新会话.
     *
     * <p>自动设置创建时间和更新时间为当前时间，消息数初始化为 0。
     *
     * @param sessionId 会话ID（UUID 格式）
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

    /**
     * JPA 生命周期回调 - 持久化前执行.
     *
     * <p>确保新实体保存时有正确的时间戳和默认值。
     */
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

    /**
     * JPA 生命周期回调 - 更新前执行.
     *
     * <p>自动更新 updatedAt 时间戳。
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ==================== Getter 和 Setter ====================

    /**
     * 获取会话ID.
     *
     * @return 会话唯一标识符
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * 设置会话ID.
     *
     * @param sessionId 会话唯一标识符
     */
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * 获取用户ID.
     *
     * @return 用户标识符
     */
    public String getUserId() {
        return userId;
    }

    /**
     * 设置用户ID.
     *
     * @param userId 用户标识符
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * 获取会话标题.
     *
     * @return 会话标题
     */
    public String getTitle() {
        return title;
    }

    /**
     * 设置会话标题.
     *
     * @param title 会话标题
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * 获取创建时间.
     *
     * @return 会话创建时间
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 设置创建时间.
     *
     * @param createdAt 会话创建时间
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * 获取更新时间.
     *
     * @return 会话最后更新时间
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 设置更新时间.
     *
     * @param updatedAt 会话最后更新时间
     */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * 获取消息数量.
     *
     * @return 会话中的消息数量
     */
    public Integer getMessageCount() {
        return messageCount;
    }

    /**
     * 设置消息数量.
     *
     * @param messageCount 会话中的消息数量
     */
    public void setMessageCount(Integer messageCount) {
        this.messageCount = messageCount;
    }

    /**
     * 获取元数据.
     *
     * @return 会话元数据（JSON 格式）
     */
    public String getMetadata() {
        return metadata;
    }

    /**
     * 设置元数据.
     *
     * @param metadata 会话元数据（JSON 格式）
     */
    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    /**
     * 返回会话的字符串表示.
     *
     * @return 包含关键信息的字符串
     */
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