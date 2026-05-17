package io.agentscope.tutorial.chapter07.entity;

import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.state.State;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Memory Record Entity
 *
 * <p>Represents a stored message in the memory system. This entity is persisted
 * to the database and supports conversion to/from AgentScope's Msg type.
 *
 * <p>Features:
 * <ul>
 *   <li>Supports multi-session isolation via session_id</li>
 *   <li>Tracks message order via message_index</li>
 *   <li>Supports memory compression with is_summarized flag</li>
 *   <li>Compatible with both H2 (dev) and MySQL (prod)</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 */
@Entity
@Table(name = "memory_records",
       indexes = {
           @Index(name = "idx_session_id", columnList = "sessionId"),
           @Index(name = "idx_created_at", columnList = "createdAt"),
           @Index(name = "idx_session_index", columnList = "sessionId, messageIndex")
       })
public class MemoryRecord implements State {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Session identifier for multi-session isolation.
     */
    @Column(name = "session_id", nullable = false, length = 255)
    private String sessionId;

    /**
     * Message role: USER, ASSISTANT, SYSTEM, TOOL
     */
    @Column(name = "role", nullable = false, length = 50)
    private String role;

    /**
     * Optional name of the message sender.
     */
    @Column(name = "name", length = 100)
    private String name;

    /**
     * Message content stored as text (extracted from content blocks).
     */
    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    /**
     * Content type for multi-modal support (text, image, audio, etc.).
     */
    @Column(name = "content_type", length = 50)
    private String contentType = "text";

    /**
     * Timestamp when the message was created.
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * Message index within the session for maintaining order.
     */
    @Column(name = "message_index", nullable = false)
    private Integer messageIndex;

    /**
     * Flag indicating if this message has been compressed/summarized.
     */
    @Column(name = "is_summarized")
    private Boolean isSummarized = false;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (isSummarized == null) {
            isSummarized = false;
        }
        if (contentType == null) {
            contentType = "text";
        }
    }

    // ==================== 构造函数s ====================

    public MemoryRecord() {
    }

    public MemoryRecord(String sessionId, String role, String content, String name, int messageIndex) {
        this.sessionId = sessionId;
        this.role = role;
        this.content = content;
        this.name = name;
        this.messageIndex = messageIndex;
        this.createdAt = LocalDateTime.now();
    }

    // ==================== Conversion Methods ====================

    /**
     * Creates a MemoryRecord from a Msg object.
     *
     * @param msg The AgentScope message
     * @param sessionId The session identifier
     * @param messageIndex The index of this message in the session
     * @return A new MemoryRecord instance
     */
    public static MemoryRecord fromMsg(Msg msg, String sessionId, int messageIndex) {
        MemoryRecord record = new MemoryRecord();
        record.setSessionId(sessionId);
        record.setRole(msg.getRole().name().toLowerCase());
        record.setName(msg.getName());
        record.setContent(extractTextContent(msg.getContent()));
        record.setMessageIndex(messageIndex);
        record.setContentType(detectContentType(msg.getContent()));
        return record;
    }

    /**
     * Converts this record back to an AgentScope Msg.
     *
     * @return A Msg representation of this memory record
     */
    public Msg toMsg() {
        return Msg.builder()
                .name(name)
                .role(MsgRole.valueOf(role.toUpperCase()))
                .content(TextBlock.builder().text(content).build())
                .build();
    }

    /**
     * Extracts plain text from content blocks.
     */
    private static String extractTextContent(List<ContentBlock> content) {
        if (content == null || content.isEmpty()) {
            return "";
        }
        return content.stream()
                .filter(TextBlock.class::isInstance)
                .map(TextBlock.class::cast)
                .map(TextBlock::getText)
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");
    }

    /**
     * Detects the primary content type from content blocks.
     */
    private static String detectContentType(List<ContentBlock> content) {
        if (content == null || content.isEmpty()) {
            return "text";
        }
        Class<? extends ContentBlock> firstBlockType = content.get(0).getClass();
        if (io.agentscope.core.message.ImageBlock.class.isAssignableFrom(firstBlockType)) {
            return "image";
        } else if (io.agentscope.core.message.AudioBlock.class.isAssignableFrom(firstBlockType)) {
            return "audio";
        } else if (io.agentscope.core.message.VideoBlock.class.isAssignableFrom(firstBlockType)) {
            return "video";
        }
        return "text";
    }

    // ==================== Getters and Setters ====================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getMessageIndex() {
        return messageIndex;
    }

    public void setMessageIndex(Integer messageIndex) {
        this.messageIndex = messageIndex;
    }

    public Boolean getIsSummarized() {
        return isSummarized;
    }

    public void setIsSummarized(Boolean isSummarized) {
        this.isSummarized = isSummarized;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MemoryRecord that = (MemoryRecord) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "MemoryRecord{" +
                "id=" + id +
                ", sessionId='" + sessionId + '\'' +
                ", role='" + role + '\'' +
                ", name='" + name + '\'' +
                ", content='" + (content != null && content.length() > 50 ? content.substring(0, 50) + "..." : content) + '\'' +
                ", messageIndex=" + messageIndex +
                ", isSummarized=" + isSummarized +
                '}';
    }
}