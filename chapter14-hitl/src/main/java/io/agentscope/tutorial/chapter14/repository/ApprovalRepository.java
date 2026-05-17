package io.agentscope.tutorial.chapter14.repository;

import io.agentscope.tutorial.chapter14.entity.ApprovalRequest;
import io.agentscope.tutorial.chapter14.entity.ApprovalRequest.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 审批请求 JPA 仓库.
 *
 * <p>提供审批请求的 CRUD 操作和复杂查询.
 *
 * @author AgentScope Tutorial
 */
@Repository
public interface ApprovalRepository extends JpaRepository<ApprovalRequest, Long> {

    /**
     * 根据会话 ID 和工具调用 ID 查找审批请求.
     * 用于验证某个特定的工具调用是否已有待审批请求.
     */
    Optional<ApprovalRequest> findBySessionIdAndToolCallId(String sessionId, String toolCallId);

    /**
     * 查找指定会话的所有待审批请求.
     */
    List<ApprovalRequest> findBySessionIdAndStatus(String sessionId, Status status);

    /**
     * 查找所有待审批请求（全局）.
     */
    List<ApprovalRequest> findByStatus(Status status);

    /**
     * 根据会话 ID 查找所有审批请求（不限状态）.
     */
    List<ApprovalRequest> findBySessionId(String sessionId);

    /**
     * 查找超时但仍为待审批状态的请求.
     * 这些请求需要被标记为 TIMEOUT.
     */
    @Query("SELECT a FROM ApprovalRequest a WHERE a.status = :status AND a.createdAt < :deadline")
    List<ApprovalRequest> findTimeoutPendingRequests(
            @Param("status") Status status,
            @Param("deadline") LocalDateTime deadline);

    /**
     * 批量更新超时请求的状态.
     *
     * @param oldStatus 原状态
     * @param newStatus 新状态
     * @param deadline 超时截止时间
     * @return 更新的记录数
     */
    @Modifying
    @Query("UPDATE ApprovalRequest a SET a.status = :newStatus, a.comment = '审批超时自动关闭', a.approvedAt = CURRENT_TIMESTAMP WHERE a.status = :oldStatus AND a.createdAt < :deadline")
    int updateTimeoutRequests(
            @Param("oldStatus") Status oldStatus,
            @Param("newStatus") Status newStatus,
            @Param("deadline") LocalDateTime deadline);

    /**
     * 根据会话 ID 删除所有审批请求.
     */
    void deleteBySessionId(String sessionId);

    /**
     * 统计指定状态的审批请求数量.
     */
    long countByStatus(Status status);

    /**
     * 查找指定会话中最早的待审批请求.
     */
    Optional<ApprovalRequest> findFirstBySessionIdAndStatusOrderByCreatedAtAsc(String sessionId, Status status);
}