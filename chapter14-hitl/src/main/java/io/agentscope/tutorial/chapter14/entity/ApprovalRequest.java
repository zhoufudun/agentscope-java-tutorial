package io.agentscope.tutorial.chapter14.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 审批请求实体.
 *
 * <p>记录需要人工审批的工具调用信息，包括工具名、参数、状态和审批结果.
 *
 * @author AgentScope Tutorial
 */
@Entity
@Table(name = "approval_requests")
public class ApprovalRequest {

    /**
     * 审批状态枚举.
     */
    public enum Status {
        /** 待审批 */
        PENDING,
        /** 已批准，工具将执行 */
        APPROVED,
        /** 已拒绝，工具结果将被标记为失败 */
        REJECTED,
        /** 审批超时 */
        TIMEOUT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 会话 ID，用于关联到具体的代理会话 */
    @Column(name = "session_id", nullable = false)
    private String sessionId;

    /** 工具调用 ID（来自 ToolUseBlock） */
    @Column(name = "tool_call_id", nullable = false)
    private String toolCallId;

    /** 工具名称 */
    @Column(name = "tool_name", nullable = false)
    private String toolName;

    /** 工具输入参数（JSON 字符串存储） */
    @Column(name = "tool_input", columnDefinition = "TEXT")
    private String toolInput;

    /** 审批状态 */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status = Status.PENDING;

    /** 审批人（可为空，直到有人审批） */
    @Column(name = "approver")
    private String approver;

    /** 审批意见 */
    @Column(name = "comment")
    private String comment;

    /** 审批时间 */
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    /** 创建时间 */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** 超时时间（秒），默认 3600 秒（1 小时） */
    @Column(name = "timeout_seconds")
    private Integer timeoutSeconds = 3600;

    /** 代理名称（可选） */
    @Column(name = "agent_name")
    private String agentName;

    /** 工具输出结果（审批通过后填充） */
    @Column(name = "tool_output", columnDefinition = "TEXT")
    private String toolOutput;

    // ==================== 构造函数 ====================

    public ApprovalRequest() {
        this.createdAt = LocalDateTime.now();
    }

    public ApprovalRequest(String sessionId, String toolCallId, String toolName, String toolInput) {
        this();
        this.sessionId = sessionId;
        this.toolCallId = toolCallId;
        this.toolName = toolName;
        this.toolInput = toolInput;
    }

    // ==================== 业务方法 ====================

    /**
     * 检查审批是否已超时.
     *
     * @return true 如果已超时
     */
    public boolean isTimeout() {
        return LocalDateTime.now().isAfter(createdAt.plusSeconds(timeoutSeconds));
    }

    /**
     * 执行审批操作.
     *
     * @param approved true 表示批准，false 表示拒绝
     * @param approver 审批人
     * @param comment  审批意见
     */
    public void approve(boolean approved, String approver, String comment) {
        this.status = approved ? Status.APPROVED : Status.REJECTED;
        this.approver = approver;
        this.comment = comment;
        this.approvedAt = LocalDateTime.now();
    }

    /**
     * 标记为超时状态.
     */
    public void markTimeout() {
        this.status = Status.TIMEOUT;
        this.comment = "审批超时自动关闭";
        this.approvedAt = LocalDateTime.now();
    }

    /**
     * 检查是否需要人工审批.
     */
    public boolean needsApproval() {
        return this.status == Status.PENDING && !isTimeout();
    }

    // ==================== Getter & Setter ====================

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

    public String getToolCallId() {
        return toolCallId;
    }

    public void setToolCallId(String toolCallId) {
        this.toolCallId = toolCallId;
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public String getToolInput() {
        return toolInput;
    }

    public void setToolInput(String toolInput) {
        this.toolInput = toolInput;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getApprover() {
        return approver;
    }

    public void setApprover(String approver) {
        this.approver = approver;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(Integer timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    public String getToolOutput() {
        return toolOutput;
    }

    public void setToolOutput(String toolOutput) {
        this.toolOutput = toolOutput;
    }

    @Override
    public String toString() {
        return "ApprovalRequest{" +
                "id=" + id +
                ", sessionId='" + sessionId + '\'' +
                ", toolCallId='" + toolCallId + '\'' +
                ", toolName='" + toolName + '\'' +
                ", status=" + status +
                ", approver='" + approver + '\'' +
                '}';
    }
}