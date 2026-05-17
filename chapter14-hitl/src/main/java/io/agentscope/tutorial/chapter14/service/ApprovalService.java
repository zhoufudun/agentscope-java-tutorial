package io.agentscope.tutorial.chapter14.service;

import io.agentscope.tutorial.chapter14.entity.ApprovalRequest;
import io.agentscope.tutorial.chapter14.entity.ApprovalRequest.Status;
import io.agentscope.tutorial.chapter14.repository.ApprovalRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 审批服务.
 *
 * <p>负责审批请求的创建、查询、审批决策和超时处理.
 *
 * @author AgentScope Tutorial
 */
@Service
public class ApprovalService {

    private static final Logger log = LoggerFactory.getLogger(ApprovalService.class);

    private final ApprovalRepository repository;

    public ApprovalService(ApprovalRepository repository) {
        this.repository = repository;
    }

    /**
     * 创建新的审批请求.
     *
     * @param request 审批请求实体
     * @return 保存后的审批请求（含 ID）
     */
    @Transactional
    public ApprovalRequest createApprovalRequest(ApprovalRequest request) {
        ApprovalRequest saved = repository.save(request);
        log.info("审批请求已创建: id={}, session={}, tool={}, toolCallId={}",
                saved.getId(), saved.getSessionId(), saved.getToolName(), saved.getToolCallId());
        return saved;
    }

    /**
     * 根据 ID 获取审批请求.
     */
    public Optional<ApprovalRequest> findById(Long id) {
        return repository.findById(id);
    }

    /**
     * 根据会话 ID 和工具调用 ID 查找审批请求.
     * 用于恢复代理执行时验证审批状态.
     */
    public Optional<ApprovalRequest> findBySessionAndToolCallId(String sessionId, String toolCallId) {
        return repository.findBySessionIdAndToolCallId(sessionId, toolCallId);
    }

    /**
     * 获取指定会话的所有待审批请求.
     */
    public List<ApprovalRequest> getPendingRequests(String sessionId) {
        return repository.findBySessionIdAndStatus(sessionId, Status.PENDING);
    }

    /**
     * 获取所有待审批请求（全局）.
     */
    public List<ApprovalRequest> getAllPendingRequests() {
        return repository.findByStatus(Status.PENDING);
    }

    /**
     * 根据会话 ID 获取所有审批请求.
     */
    public List<ApprovalRequest> getRequestsBySession(String sessionId) {
        return repository.findBySessionId(sessionId);
    }

    /**
     * 执行审批决策.
     *
     * @param approvalId 审批请求 ID
     * @param approved   true = 批准，false = 拒绝
     * @param approver   审批人名称
     * @param comment    审批意见（可选）
     * @return 审批后的请求，如果不存在则返回空
     */
    @Transactional
    public Optional<ApprovalRequest> approve(Long approvalId, boolean approved, String approver, String comment) {
        Optional<ApprovalRequest> opt = repository.findById(approvalId);
        if (opt.isEmpty()) {
            log.warn("审批请求不存在: id={}", approvalId);
            return Optional.empty();
        }

        ApprovalRequest request = opt.get();
        if (request.getStatus() != Status.PENDING) {
            log.warn("审批请求状态不是 PENDING，无法审批: id={}, status={}", approvalId, request.getStatus());
            return Optional.empty();
        }

        request.approve(approved, approver, comment);
        ApprovalRequest saved = repository.save(request);

        log.info("审批完成: id={}, tool={}, toolCallId={}, decision={}, approver={}",
                saved.getId(), saved.getToolName(), saved.getToolCallId(),
                saved.getStatus(), saved.getApprover());

        return Optional.of(saved);
    }

    /**
     * 执行审批决策（通过 toolCallId）.
     *
     * @param sessionId  会话 ID
     * @param toolCallId 工具调用 ID
     * @param approved   true = 批准，false = 拒绝
     * @param approver   审批人名称
     * @param comment    审批意见（可选）
     * @return 审批后的请求，如果不存在则返回空
     */
    @Transactional
    public Optional<ApprovalRequest> approveByToolCallId(
            String sessionId, String toolCallId, boolean approved, String approver, String comment) {
        Optional<ApprovalRequest> opt = repository.findBySessionIdAndToolCallId(sessionId, toolCallId);
        if (opt.isEmpty()) {
            log.warn("审批请求不存在: sessionId={}, toolCallId={}", sessionId, toolCallId);
            return Optional.empty();
        }
        return approve(opt.get().getId(), approved, approver, comment);
    }

    /**
     * 根据会话 ID 执行批量审批决策.
     * 通常用于会话结束时清理所有待审批项.
     *
     * @param sessionId 会话 ID
     * @param approved  true = 全部批准，false = 全部拒绝
     * @param approver  审批人
     * @param comment   审批意见
     * @return 被处理的请求数量
     */
    @Transactional
    public int batchApproveBySession(String sessionId, boolean approved, String approver, String comment) {
        List<ApprovalRequest> pending = repository.findBySessionIdAndStatus(sessionId, Status.PENDING);
        for (ApprovalRequest request : pending) {
            request.approve(approved, approver, comment + " [会话结束自动处理]");
            repository.save(request);
        }
        log.info("批量审批完成: session={}, count={}, decision={}",
                sessionId, pending.size(), approved ? "APPROVED" : "REJECTED");
        return pending.size();
    }

    /**
     * 定时任务：处理审批超时.
     * 每分钟执行一次，将超时的待审批请求标记为 TIMEOUT.
     * 默认超时时间为 3600 秒（1 小时），可在实体中自定义.
     */
    @Scheduled(fixedRate = 60000) // 每 60 秒执行一次
    @Transactional
    public void handleTimeoutRequests() {
        LocalDateTime deadline = LocalDateTime.now().minusSeconds(60);
        int updated = repository.updateTimeoutRequests(Status.PENDING, Status.TIMEOUT, deadline);
        if (updated > 0) {
            log.warn("自动处理了 {} 个超时审批请求", updated);
        }
    }

    /**
     * 删除会话的所有审批记录.
     */
    @Transactional
    public void deleteBySessionId(String sessionId) {
        repository.deleteBySessionId(sessionId);
        log.info("已删除会话的所有审批记录: session={}", sessionId);
    }

    /**
     * 获取统计数据.
     */
    public ApprovalStats getStats() {
        return new ApprovalStats(
                repository.count(),
                repository.countByStatus(Status.PENDING),
                repository.countByStatus(Status.APPROVED),
                repository.countByStatus(Status.REJECTED),
                repository.countByStatus(Status.TIMEOUT)
        );
    }

    /**
     * 审批统计数据.
     */
    public record ApprovalStats(
            long total,
            long pending,
            long approved,
            long rejected,
            long timeout
    ) {}
}