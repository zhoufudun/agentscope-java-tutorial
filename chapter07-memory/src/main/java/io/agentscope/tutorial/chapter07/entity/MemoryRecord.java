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
 * 记忆记录实体类
 *
 * <p>表示记忆系统中存储的单条消息。该实体持久化到数据库，
 * 并支持与 AgentScope 的 {@link Msg} 类型进行双向转换。
 *
 * <p><b>核心特性</b>：
 * <ul>
 *   <li><b>多会话隔离</b> - 通过 session_id 字段隔离不同会话的记忆</li>
 *   <li><b>消息排序</b> - 通过 message_index 字段维护消息顺序</li>
 *   <li><b>记忆压缩</b> - 通过 is_summarized 标记支持记忆压缩功能</li>
 *   <li><b>数据库兼容</b> - 同时支持 H2（开发）和 MySQL（生产）数据库</li>
 *   <li><b>多模态支持</b> - 通过 content_type 字段支持文本、图片、音频等类型</li>
 * </ul>
 *
 * <p><b>数据库索引</b>：
 * <ul>
 *   <li>idx_session_id - 会话查询加速</li>
 *   <li>idx_created_at - 时间范围查询加速</li>
 *   <li>idx_session_index - 会话内排序查询加速</li>
 * </ul>
 *
 * <p><b>与 AgentScope 的集成</b>：
 * 该实体实现了 {@link State} 接口，可作为 AgentScope 2.x 状态持久化的一部分。
 * 使用 {@link #fromMsg(Msg, String, int)} 和 {@link #toMsg()} 方法进行与
 * {@link Msg} 之间的转换。
 *
 * @author AgentScope Tutorial
 * @since 1.0
 */
@Entity
@Table(name = "memory_records",
       indexes = {
           // 会话 ID 索引：加速按会话查询记忆
           @Index(name = "idx_session_id", columnList = "sessionId"),
           // 创建时间索引：加速时间范围查询
           @Index(name = "idx_created_at", columnList = "createdAt"),
           // 复合索引：加速会话内按顺序获取消息
           @Index(name = "idx_session_index", columnList = "sessionId, messageIndex")
       })
public class MemoryRecord implements State {

    /** 主键 ID，自动生成 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 会话标识符
     *
     * <p>用于多会话隔离，每个用户/对话拥有独立的会话 ID。
     * 同一会话的所有消息共享相同的 sessionId。
     */
    @Column(name = "session_id", nullable = false, length = 255)
    private String sessionId;

    /**
     * 消息角色
     *
     * <p>标识消息的发送者类型：
     * <ul>
     *   <li>user - 用户消息</li>
     *   <li>assistant - AI 助手回复</li>
     *   <li>system - 系统消息（如记忆摘要）</li>
     *   <li>tool - 工具调用结果</li>
     * </ul>
     */
    @Column(name = "role", nullable = false, length = 50)
    private String role;

    /**
     * 消息发送者名称
     *
     * <p>可选字段，用于标识具体的发送者身份。
     * 例如用户名、Agent 名称或工具名称。
     */
    @Column(name = "name", length = 100)
    private String name;

    /**
     * 消息内容
     *
     * <p>存储为纯文本格式。多模态内容会先提取文本部分再存储。
     * 使用 TEXT 类型以支持长文本内容。
     */
    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    /**
     * 内容类型
     *
     * <p>用于多模态消息支持：
     * <ul>
     *   <li>text - 纯文本（默认）</li>
     *   <li>image - 图片消息</li>
     *   <li>audio - 音频消息</li>
     *   <li>video - 视频消息</li>
     * </ul>
     */
    @Column(name = "content_type", length = 50)
    private String contentType = "text";

    /**
     * 消息创建时间
     *
     * <p>记录消息的生成时间，用于时间范围查询和排序。
     * 在持久化前自动设置（{@link #onCreate()}）。
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * 消息索引
     *
     * <p>会话内的消息顺序编号，从 0 开始递增。
     * 用于维护对话的时间顺序，确保消息按正确顺序展示。
     */
    @Column(name = "message_index", nullable = false)
    private Integer messageIndex;

    /**
     * 是否已被摘要压缩
     *
     * <p>标记该消息是否已被记忆压缩功能处理：
     * <ul>
     *   <li>false - 原始消息，未被压缩</li>
     *   <li>true - 已被包含在摘要中，可能被删除以节省空间</li>
     * </ul>
     */
    @Column(name = "is_summarized")
    private Boolean isSummarized = false;

    /**
     * JPA 持久化前回调
     *
     * <p>在实体保存到数据库前自动执行，确保必要字段有默认值：
     * <ul>
     *   <li>createdAt - 设置为当前时间</li>
     *   <li>isSummarized - 设置为 false</li>
     *   <li>contentType - 设置为 "text"</li>
     * </ul>
     */
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

    // ==================== 构造函数 ====================

    /**
     * 默认构造函数
     *
     * <p>JPA 要求的无参构造函数。
     */
    public MemoryRecord() {
    }

    /**
     * 带参构造函数
     *
     * @param sessionId 会话标识符
     * @param role 消息角色（user/assistant/system/tool）
     * @param content 消息内容
     * @param name 发送者名称
     * @param messageIndex 消息在会话中的索引
     */
    public MemoryRecord(String sessionId, String role, String content, String name, int messageIndex) {
        this.sessionId = sessionId;
        this.role = role;
        this.content = content;
        this.name = name;
        this.messageIndex = messageIndex;
        this.createdAt = LocalDateTime.now();
    }

    // ==================== 类型转换方法 ====================

    /**
     * 从 AgentScope 消息创建记忆记录
     *
     * <p>将 AgentScope 的 {@link Msg} 对象转换为可持久化的 {@link MemoryRecord}。
     * 该方法会：
     * <ul>
     *   <li>提取消息的文本内容（从 ContentBlock 列表中）</li>
     *   <li>检测内容类型（文本/图片/音频等）</li>
     *   <li>保留角色和发送者信息</li>
     * </ul>
     *
     * @param msg AgentScope 消息对象
     * @param sessionId 会话标识符
     * @param messageIndex 消息在会话中的索引位置
     * @return 新创建的记忆记录实例
     */
    public static MemoryRecord fromMsg(Msg msg, String sessionId, int messageIndex) {
        MemoryRecord record = new MemoryRecord();
        record.setSessionId(sessionId);
        // 将枚举转换为小写字符串存储
        record.setRole(msg.getRole().name().toLowerCase());
        record.setName(msg.getName());
        // 从 ContentBlock 列表中提取纯文本内容
        record.setContent(extractTextContent(msg.getContent()));
        record.setMessageIndex(messageIndex);
        // 检测并设置内容类型
        record.setContentType(detectContentType(msg.getContent()));
        return record;
    }

    /**
     * 将记忆记录转换为 AgentScope 消息
     *
     * <p>将持久化的记忆记录还原为 AgentScope 的 {@link Msg} 对象，
     * 以便在 Agent 对话中使用。
     *
     * @return 对应的 AgentScope 消息对象
     */
    public Msg toMsg() {
        return Msg.builder()
                .name(name)
                // 将小写字符串转换回枚举类型
                .role(MsgRole.valueOf(role.toUpperCase()))
                // 包装为 TextBlock
                .content(TextBlock.builder().text(content).build())
                .build();
    }

    /**
     * 从内容块列表中提取纯文本内容
     *
     * <p>AgentScope 的消息内容可能包含多个 ContentBlock（文本、图片等），
     * 该方法提取所有 TextBlock 的文本内容并合并。
     *
     * @param content ContentBlock 列表
     * @return 合并后的纯文本内容
     */
    private static String extractTextContent(List<ContentBlock> content) {
        if (content == null || content.isEmpty()) {
            return "";
        }
        return content.stream()
                // 只处理文本块
                .filter(TextBlock.class::isInstance)
                .map(TextBlock.class::cast)
                .map(TextBlock::getText)
                // 多个文本块用换行符连接
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");
    }

    /**
     * 检测内容的主要类型
     *
     * <p>根据第一个 ContentBlock 的类型判断整体内容类型。
     * 支持的类型包括：text、image、audio、video。
     *
     * @param content ContentBlock 列表
     * @return 内容类型字符串
     */
    private static String detectContentType(List<ContentBlock> content) {
        if (content == null || content.isEmpty()) {
            return "text";
        }
        // 根据第一个内容块的类型判断
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

    // ==================== Getter 和 Setter 方法 ====================

    /**
     * 获取主键 ID
     *
     * @return 主键 ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置主键 ID
     *
     * @param id 主键 ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取会话标识符
     *
     * @return 会话 ID
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * 设置会话标识符
     *
     * @param sessionId 会话 ID
     */
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * 获取消息角色
     *
     * @return 角色字符串（user/assistant/system/tool）
     */
    public String getRole() {
        return role;
    }

    /**
     * 设置消息角色
     *
     * @param role 角色字符串
     */
    public void setRole(String role) {
        this.role = role;
    }

    /**
     * 获取发送者名称
     *
     * @return 发送者名称
     */
    public String getName() {
        return name;
    }

    /**
     * 设置发送者名称
     *
     * @param name 发送者名称
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 获取消息内容
     *
     * @return 消息文本内容
     */
    public String getContent() {
        return content;
    }

    /**
     * 设置消息内容
     *
     * @param content 消息文本内容
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * 获取内容类型
     *
     * @return 内容类型（text/image/audio/video）
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * 设置内容类型
     *
     * @param contentType 内容类型
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /**
     * 获取创建时间
     *
     * @return 创建时间
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 设置创建时间
     *
     * @param createdAt 创建时间
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * 获取消息索引
     *
     * @return 消息在会话中的索引位置
     */
    public Integer getMessageIndex() {
        return messageIndex;
    }

    /**
     * 设置消息索引
     *
     * @param messageIndex 消息索引
     */
    public void setMessageIndex(Integer messageIndex) {
        this.messageIndex = messageIndex;
    }

    /**
     * 获取是否已被摘要
     *
     * @return 是否已被摘要压缩
     */
    public Boolean getIsSummarized() {
        return isSummarized;
    }

    /**
     * 设置是否已被摘要
     *
     * @param isSummarized 是否已被摘要压缩
     */
    public void setIsSummarized(Boolean isSummarized) {
        this.isSummarized = isSummarized;
    }

    /**
     * 基于 ID 的相等性判断
     *
     * @param o 比较对象
     * @return 如果 ID 相同则返回 true
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MemoryRecord that = (MemoryRecord) o;
        return Objects.equals(id, that.id);
    }

    /**
     * 基于 ID 的哈希码计算
     *
     * @return 哈希码
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /**
     * 返回记录的字符串表示
     *
     * <p>内容过长时会自动截断到 50 个字符。
     *
     * @return 格式化的字符串表示
     */
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