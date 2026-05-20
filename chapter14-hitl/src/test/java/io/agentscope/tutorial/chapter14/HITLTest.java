package io.agentscope.tutorial.chapter14;

import io.agentscope.tutorial.chapter14.entity.ApprovalRequest;
import io.agentscope.tutorial.chapter14.entity.ApprovalRequest.Status;
import io.agentscope.tutorial.chapter14.repository.ApprovalRepository;
import io.agentscope.tutorial.chapter14.service.ApprovalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HITL 审批系统测试类.
 *
 * @author AgentScope Tutorial
 */
@SpringBootTest
@ActiveProfiles("dev")
class HITLTest {

    @Autowired
    private ApprovalRepository approvalRepository;

    @Autowired
    private ApprovalService approvalService;

    @BeforeEach
    void setUp() {
        // 清空测试数据
        approvalRepository.deleteAll();
    }

    // ==================== 审批请求创建测试 ====================

    @Test
    void testCreateApprovalRequest() {
        // 创建审批请求
        ApprovalRequest request = new ApprovalRequest(
                "test-session-1",
                "tool-call-001",
                "send_email",
                "{\"to\":\"user@company.com\",\"subject\":\"测试\"}"
        );
        request.setTimeoutSeconds(3600);

        ApprovalRequest saved = approvalService.createApprovalRequest(request);

        assertNotNull(saved);
        assertNotNull(saved.getId());
        assertEquals("test-session-1", saved.getSessionId());
        assertEquals("tool-call-001", saved.getToolCallId());
        assertEquals("send_email", saved.getToolName());
        assertEquals(Status.PENDING, saved.getStatus());
    }

    // ==================== 审批决策测试 ====================

    @Test
    void testApprove() {
        // 创建审批请求
        ApprovalRequest request = new ApprovalRequest(
                "test-session-2",
                "tool-call-002",
                "delete_data",
                "{\"table\":\"users\",\"record_id\":\"123\"}"
        );
        approvalService.createApprovalRequest(request);

        // 执行批准
        Optional<ApprovalRequest> result = approvalService.approve(
                request.getId(), true, "admin", "测试批准");

        assertTrue(result.isPresent());
        assertEquals(Status.APPROVED, result.get().getStatus());
        assertEquals("admin", result.get().getApprover());
        assertNotNull(result.get().getApprovedAt());
    }

    @Test
    void testReject() {
        // 创建审批请求
        ApprovalRequest request = new ApprovalRequest(
                "test-session-3",
                "tool-call-003",
                "send_email",
                "{\"to\":\"test@company.com\"}"
        );
        approvalService.createApprovalRequest(request);

        // 执行拒绝
        Optional<ApprovalRequest> result = approvalService.approve(
                request.getId(), false, "admin", "测试拒绝");

        assertTrue(result.isPresent());
        assertEquals(Status.REJECTED, result.get().getStatus());
        assertEquals("测试拒绝", result.get().getComment());
    }

    @Test
    void testCannotApproveNonPendingRequest() {
        // 创建并批准一个请求
        ApprovalRequest request = new ApprovalRequest(
                "test-session-4",
                "tool-call-004",
                "send_email",
                "{}"
        );
        approvalService.createApprovalRequest(request);
        approvalService.approve(request.getId(), true, "admin", "第一次批准");

        // 尝试再次批准
        Optional<ApprovalRequest> result = approvalService.approve(
                request.getId(), false, "admin", "第二次批准");

        assertTrue(result.isEmpty());
    }

    // ==================== 查询测试 ====================

    @Test
    void testFindBySessionIdAndToolCallId() {
        ApprovalRequest request = new ApprovalRequest(
                "test-session-5",
                "tool-call-005",
                "send_email",
                "{}"
        );
        approvalService.createApprovalRequest(request);

        Optional<ApprovalRequest> result = approvalService.findBySessionAndToolCallId(
                "test-session-5", "tool-call-005");

        assertTrue(result.isPresent());
        assertEquals(request.getId(), result.get().getId());
    }

    @Test
    void testGetPendingRequests() {
        // 创建多个待审批请求
        ApprovalRequest request1 = null;
        for (int i = 1; i <= 3; i++) {
            ApprovalRequest request = new ApprovalRequest(
                    "test-session-6",
                    "tool-call-00" + i,
                    "send_email",
                    "{}"
            );
            ApprovalRequest saved = approvalService.createApprovalRequest(request);
            if (i == 1) {
                request1 = saved;
            }
        }

        // 批准第一个
        assert request1 != null;
        approvalService.approve(request1.getId(), true, "admin", "批准1");

        List<ApprovalRequest> pending = approvalService.getPendingRequests("test-session-6");
        assertEquals(2, pending.size());
    }

    @Test
    void testGetAllPendingRequests() {
        // 创建不同会话的待审批请求
        for (int i = 1; i <= 3; i++) {
            ApprovalRequest request = new ApprovalRequest(
                    "session-" + i,
                    "tool-call-" + i,
                    "send_email",
                    "{}"
            );
            approvalService.createApprovalRequest(request);
        }

        List<ApprovalRequest> pending = approvalService.getAllPendingRequests();
        assertEquals(3, pending.size());
    }

    // ==================== 批量审批测试 ====================

    @Test
    void testBatchApproveBySession() {
        // 创建多个待审批请求
        for (int i = 1; i <= 3; i++) {
            ApprovalRequest request = new ApprovalRequest(
                    "test-session-7",
                    "tool-call-00" + i,
                    "send_email",
                    "{}"
            );
            approvalService.createApprovalRequest(request);
        }

        // 批量拒绝
        int count = approvalService.batchApproveBySession(
                "test-session-7", false, "admin", "批量拒绝");

        assertEquals(3, count);

        // 验证所有请求都已拒绝
        List<ApprovalRequest> requests = approvalService.getRequestsBySession("test-session-7");
        assertTrue(requests.stream().allMatch(r -> r.getStatus() == Status.REJECTED));
    }

    // ==================== 超时测试 ====================

    @Test
    void testTimeout() {
        // 创建一个超时的审批请求
        ApprovalRequest request = new ApprovalRequest(
                "test-session-8",
                "tool-call-008",
                "send_email",
                "{}"
        );
        request.setCreatedAt(LocalDateTime.now().minusSeconds(7200)); // 2小时前
        request.setTimeoutSeconds(3600); // 1小时超时
        approvalRepository.save(request);

        // 执行超时处理
        approvalService.handleTimeoutRequests();

        // 验证请求已标记为超时
        Optional<ApprovalRequest> result = approvalService.findById(request.getId());
        assertTrue(result.isPresent());
        assertEquals(Status.TIMEOUT, result.get().getStatus());
    }

    // ==================== 删除测试 ====================

    @Test
    void testDeleteBySessionId() {
        // 创建多个审批请求
        for (int i = 1; i <= 3; i++) {
            ApprovalRequest request = new ApprovalRequest(
                    "test-session-9",
                    "tool-call-00" + i,
                    "send_email",
                    "{}"
            );
            approvalService.createApprovalRequest(request);
        }

        // 删除会话的所有审批记录
        approvalService.deleteBySessionId("test-session-9");

        // 验证已删除
        List<ApprovalRequest> requests = approvalService.getRequestsBySession("test-session-9");
        assertTrue(requests.isEmpty());
    }

    // ==================== 统计数据测试 ====================

    @Test
    void testGetStats() {
        // 创建不同状态的审批请求
        ApprovalRequest pending = new ApprovalRequest("s1", "t1", "send_email", "{}");
        approvalService.createApprovalRequest(pending);

        ApprovalRequest approved = new ApprovalRequest("s2", "t2", "send_email", "{}");
        approvalService.createApprovalRequest(approved);
        approvalService.approve(approved.getId(), true, "admin", "通过");

        ApprovalRequest rejected = new ApprovalRequest("s3", "t3", "send_email", "{}");
        approvalService.createApprovalRequest(rejected);
        approvalService.approve(rejected.getId(), false, "admin", "拒绝");

        ApprovalService.ApprovalStats stats = approvalService.getStats();

        assertEquals(3, stats.total());
        assertEquals(1, stats.pending());
        assertEquals(1, stats.approved());
        assertEquals(1, stats.rejected());
    }

    // ==================== 实体方法测试 ====================

    @Test
    void testApprovalRequestMethods() {
        ApprovalRequest request = new ApprovalRequest(
                "session", "tool-call", "send_email", "{}");

        // 测试 isTimeout
        assertFalse(request.isTimeout());

        // 测试超时情况
        request.setCreatedAt(LocalDateTime.now().minusHours(2));
        request.setTimeoutSeconds(3600);
        assertTrue(request.isTimeout());

        // 测试 approve
        request.approve(true, "admin", "测试");
        assertEquals(Status.APPROVED, request.getStatus());
        assertEquals("admin", request.getApprover());
        assertNotNull(request.getApprovedAt());

        // 测试 markTimeout
        ApprovalRequest timeoutRequest = new ApprovalRequest("s", "t", "send_email", "{}");
        timeoutRequest.markTimeout();
        assertEquals(Status.TIMEOUT, timeoutRequest.getStatus());
    }

    @Test
    void testNeedsApproval() {
        ApprovalRequest request = new ApprovalRequest(
                "session", "tool-call", "send_email", "{}");

        // PENDING 且未超时
        assertTrue(request.needsApproval());

        // 批准后不再需要审批
        request.approve(true, "admin", "");
        assertFalse(request.needsApproval());

        // 超时后不再需要审批
        ApprovalRequest timeoutRequest = new ApprovalRequest("s", "t", "send_email", "{}");
        timeoutRequest.setCreatedAt(LocalDateTime.now().minusHours(2));
        timeoutRequest.setTimeoutSeconds(3600);
        assertFalse(timeoutRequest.needsApproval());
    }
}