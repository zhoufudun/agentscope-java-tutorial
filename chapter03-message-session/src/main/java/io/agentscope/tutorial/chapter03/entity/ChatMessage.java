package io.agentscope.tutorial.chapter03.entity;

import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import jakarta.persistence.*;
import java.util.List;

/**
 * 消息实体 - 存储对话历史
 *
 * 对应数据库表: chat_messages
 * 实现了与 AgentScope Msg 的双向转换
 *
 * @author AgentScope Tutorial
 */
@Entity
@Table(name = "chat_messages", indexes = {
        @Index(name = "idx_messages_session", columnList = "session_id, sequence_num")
})
public class ChatMessage {

    @Id
    @Column(name = "message_id", length = 64)
    private String messageId;

    @Column(name = "session_id", length = 64, nullable = false)
    private String sessionId;

    @Column(name = "role", length = 20, nullable = false)
    private String role;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "created_at", length = 50)
    private String createdAt;

    @Column(name = "sequence_num")
    private Integer sequenceNum;

    /**
     * JPA 默认构造函数
     */
    public ChatMessage() {
    }

    /**
     * 从 AgentScope Msg 创建实体
     *
     * @param msg        AgentScope 消息对象
     * @param sessionId  会话ID
     * @param sequenceNum 序列号
     * @return ChatMessage 实体
     */
    public static ChatMessage fromMsg(Msg msg, String sessionId, int sequenceNum) {
        ChatMessage entity = new ChatMessage();
        entity.setMessageId(msg.getId());
        entity.setSessionId(sessionId);
        entity.setRole(msg.getRole().name());
        entity.setContent(extractTextContent(msg));
        entity.setName(msg.getName());
        entity.setCreatedAt(msg.getTimestamp());
        entity.setSequenceNum(sequenceNum);
        return entity;
    }

    /**
     * 转换为 AgentScope Msg
     *
     * @return AgentScope 消息对象
     */
    public Msg toMsg() {
        return Msg.builder()
                .id(messageId)
                .name(name)
                .role(MsgRole.valueOf(role))
                .textContent(content != null ? content : "")
                .timestamp(createdAt)
                .build();
    }

    /**
     * 提取消息中的纯文本内容
     */
    private static String extractTextContent(Msg msg) {
        if (msg.getContent() == null || msg.getContent().isEmpty()) {
            return "";
        }
        return msg.getContent().stream()
                .filter(block -> block instanceof TextBlock)
                .map(block -> ((TextBlock) block).getText())
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");
    }

    // Getter 和 Setter

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getSequenceNum() {
        return sequenceNum;
    }

    public void setSequenceNum(Integer sequenceNum) {
        this.sequenceNum = sequenceNum;
    }

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