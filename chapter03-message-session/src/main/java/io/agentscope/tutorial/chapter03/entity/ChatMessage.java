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

import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import jakarta.persistence.*;
import java.util.List;

/**
 * 消息实体类 - 存储对话历史.
 *
 * <p>本类是 JPA 实体，用于将聊天消息持久化到关系型数据库。对应数据库表：{@code chat_messages}。
 * 实现了与 AgentScope {@link Msg} 的双向转换。
 *
 * <h2>核心功能</h2>
 * <ul>
 *   <li>将 AgentScope 的 {@link Msg} 对象转换为 JPA 实体进行持久化</li>
 *   <li>从数据库读取实体后转换回 {@link Msg} 对象</li>
 *   <li>支持序列号排序，保证消息顺序</li>
 * </ul>
 *
 * <h2>表结构</h2>
 * <pre>
 * CREATE TABLE chat_messages (
 *     message_id VARCHAR(64) PRIMARY KEY,
 *     session_id VARCHAR(64) NOT NULL,
 *     role VARCHAR(20) NOT NULL,
 *     content TEXT,
 *     name VARCHAR(100),
 *     metadata TEXT,
 *     created_at VARCHAR(50),
 *     sequence_num INT
 * );
 * </pre>
 *
 * <h2>与 AgentScope Msg 的映射</h2>
 * <ul>
 *   <li>{@code messageId} ↔ {@code Msg.id}</li>
 *   <li>{@code role} ↔ {@code Msg.role.name()}</li>
 *   <li>{@code content} ↔ {@code Msg.content} 中所有 TextBlock 的文本拼接</li>
 *   <li>{@code name} ↔ {@code Msg.name}</li>
 *   <li>{@code createdAt} ↔ {@code Msg.timestamp}</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 * @since 1.0
 * @see Msg
 * @see ChatSession
 */
@Entity
@Table(name = "chat_messages", indexes = {
        // 复合索引：按会话ID和序列号排序查询消息
        @Index(name = "idx_messages_session", columnList = "session_id, sequence_num")
})
public class ChatMessage {

    /**
     * 消息唯一标识符.
     *
     * <p>与 AgentScope {@link Msg#getId()} 对应。
     */
    @Id
    @Column(name = "message_id", length = 64)
    private String messageId;

    /**
     * 所属会话ID.
     *
     * <p>外键关联到 {@code chat_sessions.session_id}。
     */
    @Column(name = "session_id", length = 64, nullable = false)
    private String sessionId;

    /**
     * 消息角色.
     *
     * <p>取值范围：{@code USER}、{@code ASSISTANT}、{@code SYSTEM}、{@code TOOL}。
     * 对应 {@link MsgRole} 枚举。
     */
    @Column(name = "role", length = 20, nullable = false)
    private String role;

    /**
     * 消息文本内容.
     *
     * <p>存储 {@link Msg} 中所有 {@link TextBlock} 的文本拼接结果。
     */
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    /**
     * 消息发送者名称.
     *
     * <p>如用户昵称或 Agent 名称。
     */
    @Column(name = "name", length = 100)
    private String name;

    /**
     * 消息元数据（JSON 格式）.
     *
     * <p>可存储额外信息，如 token 数量、处理时间等。
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    /**
     * 消息创建时间戳（ISO 8601 格式字符串）.
     */
    @Column(name = "created_at", length = 50)
    private String createdAt;

    /**
     * 消息序列号.
     *
     * <p>用于保证消息在会话中的顺序，从 0 开始递增。
     */
    @Column(name = "sequence_num")
    private Integer sequenceNum;

    /**
     * JPA 默认构造函数.
     */
    public ChatMessage() {
    }

    /**
     * 从 AgentScope Msg 创建实体.
     *
     * <p>这是 AgentScope {@link Msg} 到 JPA 实体的转换方法。
     * 将 Msg 中的所有 {@link TextBlock} 内容提取并拼接。
     *
     * <p>使用示例：
     * <pre>{@code
     * Msg msg = Msg.builder()
     *     .role(MsgRole.USER)
     *     .textContent("你好")
     *     .build();
     * ChatMessage entity = ChatMessage.fromMsg(msg, sessionId, 0);
     * messageRepository.save(entity);
     * }</pre>
     *
     * @param msg        AgentScope 消息对象
     * @param sessionId  会话ID
     * @param sequenceNum 消息在会话中的序列号
     * @return ChatMessage 实体
     */
    public static ChatMessage fromMsg(Msg msg, String sessionId, int sequenceNum) {
        ChatMessage entity = new ChatMessage();
        entity.setMessageId(msg.getId());
        entity.setSessionId(sessionId);
        entity.setRole(msg.getRole().name());
        // 从消息内容块中提取纯文本
        entity.setContent(extractTextContent(msg));
        entity.setName(msg.getName());
        entity.setCreatedAt(msg.getTimestamp());
        entity.setSequenceNum(sequenceNum);
        return entity;
    }

    /**
     * 转换为 AgentScope Msg.
     *
     * <p>这是 JPA 实体到 AgentScope {@link Msg} 的转换方法。
     * 从数据库读取消息后，使用此方法还原为 Msg 对象。
     *
     * <p>使用示例：
     * <pre>{@code
     * List<ChatMessage> entities = messageRepository.findBySessionId(sessionId);
     * List<Msg> messages = entities.stream()
     *     .map(ChatMessage::toMsg)
     *     .collect(Collectors.toList());
     * }</pre>
     *
     * @return AgentScope 消息对象
     */
    public Msg toMsg() {
        return Msg.builder()
                .id(messageId)
                .name(name)
                .role(MsgRole.valueOf(role))
                // 使用 textContent 快捷方法创建 TextBlock
                .textContent(content != null ? content : "")
                .timestamp(createdAt)
                .build();
    }

    /**
     * 提取消息中的纯文本内容.
     *
     * <p>遍历消息的所有内容块，筛选出 {@link TextBlock} 类型，
     * 并将它们的文本内容用换行符拼接。
     *
     * @param msg AgentScope 消息对象
     * @return 提取的纯文本内容；如果没有文本块则返回空字符串
     */
    private static String extractTextContent(Msg msg) {
        if (msg.getContent() == null || msg.getContent().isEmpty()) {
            return "";
        }
        // 使用 Stream API 筛选 TextBlock 并拼接文本
        return msg.getContent().stream()
                .filter(block -> block instanceof TextBlock)
                .map(block -> ((TextBlock) block).getText())
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");
    }

    // ==================== Getter 和 Setter ====================

    /**
     * 获取消息ID.
     *
     * @return 消息唯一标识符
     */
    public String getMessageId() {
        return messageId;
    }

    /**
     * 设置消息ID.
     *
     * @param messageId 消息唯一标识符
     */
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    /**
     * 获取会话ID.
     *
     * @return 所属会话的标识符
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * 设置会话ID.
     *
     * @param sessionId 所属会话的标识符
     */
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * 获取消息角色.
     *
     * @return 角色字符串（USER/ASSISTANT/SYSTEM/TOOL）
     */
    public String getRole() {
        return role;
    }

    /**
     * 设置消息角色.
     *
     * @param role 角色字符串
     */
    public void setRole(String role) {
        this.role = role;
    }

    /**
     * 获取消息内容.
     *
     * @return 消息文本内容
     */
    public String getContent() {
        return content;
    }

    /**
     * 设置消息内容.
     *
     * @param content 消息文本内容
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * 获取发送者名称.
     *
     * @return 消息发送者名称
     */
    public String getName() {
        return name;
    }

    /**
     * 设置发送者名称.
     *
     * @param name 消息发送者名称
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 获取消息元数据.
     *
     * @return 元数据（JSON 格式）
     */
    public String getMetadata() {
        return metadata;
    }

    /**
     * 设置消息元数据.
     *
     * @param metadata 元数据（JSON 格式）
     */
    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    /**
     * 获取创建时间.
     *
     * @return 创建时间戳字符串
     */
    public String getCreatedAt() {
        return createdAt;
    }

    /**
     * 设置创建时间.
     *
     * @param createdAt 创建时间戳字符串
     */
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * 获取序列号.
     *
     * @return 消息在会话中的序列号
     */
    public Integer getSequenceNum() {
        return sequenceNum;
    }

    /**
     * 设置序列号.
     *
     * @param sequenceNum 消息在会话中的序列号
     */
    public void setSequenceNum(Integer sequenceNum) {
        this.sequenceNum = sequenceNum;
    }

    /**
     * 返回消息的字符串表示.
     *
     * <p>内容过长时会截断显示，便于日志输出。
     *
     * @return 包含关键信息的字符串
     */
    @Override
    public String toString() {
        return "ChatMessage{" +
                "messageId='" + messageId + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", role='" + role + '\'' +
                ", content='" + (content != null && content.length() > 50 ? content.substring(0, 50) + "..." : content) + '\'' +
                ", sequenceNum=" + sequenceNum +
                '}';
    }
}