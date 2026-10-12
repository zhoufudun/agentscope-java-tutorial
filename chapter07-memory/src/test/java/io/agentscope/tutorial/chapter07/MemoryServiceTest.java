package io.agentscope.tutorial.chapter07;

import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.tutorial.chapter07.entity.MemoryRecord;
import io.agentscope.tutorial.chapter07.repository.MemoryRepository;
import io.agentscope.tutorial.chapter07.service.MemoryService;
import io.agentscope.tutorial.chapter07.config.MemoryConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 记忆服务单元测试
 *
 * <p>测试记忆系统的核心功能，包括：
 * <ul>
 *   <li><b>记忆记录存储与检索</b> - 验证消息的持久化和读取</li>
 *   <li><b>会话隔离</b> - 验证不同会话的记忆相互独立</li>
 *   <li><b>记忆搜索</b> - 验证关键词搜索功能</li>
 *   <li><b>统计聚合</b> - 验证记忆统计信息的准确性</li>
 *   <li><b>Agent 管理</b> - 验证会话 Agent 的创建和缓存</li>
 * </ul>
 *
 * <p><b>测试策略</b>：
 * 使用 Mockito 模拟 Repository 和 Config 依赖，隔离测试记忆服务的业务逻辑。
 * 使用 {@link io.agentscope.core.state.InMemoryAgentStateStore} 作为 2.x 状态存储的测试替身。
 *
 * @author AgentScope Tutorial
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class MemoryServiceTest {

    /** 模拟的记忆仓库 */
    @Mock
    private MemoryRepository memoryRepository;

    /** 模拟的记忆配置 */
    @Mock
    private MemoryConfig memoryConfig;

    /** 模拟的 AI 模型 */
    @Mock
    private io.agentscope.core.model.Model model;

    /** 被测试的记忆服务 */
    private MemoryService memoryService;

    /**
     * 2.x 会话状态存储
     *
     * <p>使用内存实现，便于单元测试隔离，无需外部依赖。
     */
    private io.agentscope.core.state.AgentStateStore agentStateStore;

    /** 测试会话 ID 1 */
    private static final String TEST_SESSION_ID = "test-session-001";

    /** 测试会话 ID 2（用于隔离测试） */
    private static final String TEST_SESSION_ID_2 = "test-session-002";

    /**
     * 测试初始化
     *
     * <p>在每个测试方法执行前：
     * <ul>
     *   <li>创建新的 InMemoryAgentStateStore 实例</li>
     *   <li>使用模拟依赖构建 MemoryService</li>
     * </ul>
     */
    @BeforeEach
    void setUp() {
        agentStateStore = new io.agentscope.core.state.InMemoryAgentStateStore();
        memoryService = new MemoryService(memoryRepository, memoryConfig, model, agentStateStore);
    }

    // ==================== 记忆记录测试 ====================

    /**
     * 测试消息的保存和检索
     *
     * <p>验证：
     * <ul>
     *   <li>消息能够成功保存到仓库</li>
     *   <li>保存时正确调用了 countBySessionId 获取索引</li>
     * </ul>
     */
    @Test
    @DisplayName("应该能够保存和检索记忆记录")
    void testSaveAndRetrieve() {
        // 准备测试数据
        Msg testMessage = createTestMessage("Hello, world!", MsgRole.USER, "user");

        // 配置模拟行为
        when(memoryRepository.countBySessionId(TEST_SESSION_ID)).thenReturn(0L);
        when(memoryRepository.save(any(MemoryRecord.class))).thenAnswer(inv -> {
            MemoryRecord record = inv.getArgument(0);
            record.setId(1L);
            return record;
        });

        // 执行测试
        memoryService.addMessage(TEST_SESSION_ID, testMessage);

        // 验证结果
        verify(memoryRepository, times(1)).save(any(MemoryRecord.class));
        verify(memoryRepository, times(1)).countBySessionId(TEST_SESSION_ID);
    }

    /**
     * 测试 Msg 到 MemoryRecord 的转换
     *
     * <p>验证转换后的 MemoryRecord 包含正确的：
     * <ul>
     *   <li>会话 ID</li>
     *   <li>角色（小写）</li>
     *   <li>发送者名称</li>
     *   <li>消息内容</li>
     *   <li>消息索引</li>
     * </ul>
     */
    @Test
    @DisplayName("应该正确将 Msg 转换为 MemoryRecord")
    void testMsgToMemoryRecordConversion() {
        // 准备测试数据
        Msg msg = Msg.builder()
                .name("testUser")
                .role(MsgRole.USER)
                .content(TextBlock.builder().text("Test content").build())
                .build();

        // 执行转换
        MemoryRecord record = MemoryRecord.fromMsg(msg, TEST_SESSION_ID, 0);

        // 验证转换结果
        assertEquals(TEST_SESSION_ID, record.getSessionId());
        assertEquals("user", record.getRole());
        assertEquals("testUser", record.getName());
        assertEquals("Test content", record.getContent());
        assertEquals(0, record.getMessageIndex());
    }

    /**
     * 测试 MemoryRecord 到 Msg 的转换
     *
     * <p>验证转换后的 Msg 包含正确的：
     * <ul>
     *   <li>角色（转换为枚举）</li>
     *   <li>发送者名称</li>
     *   <li>文本内容</li>
     * </ul>
     */
    @Test
    @DisplayName("应该正确将 MemoryRecord 转换为 Msg")
    void testMemoryRecordToMsgConversion() {
        // 准备测试数据
        MemoryRecord record = new MemoryRecord(TEST_SESSION_ID, "user", "Test content", "user", 0);
        record.setCreatedAt(LocalDateTime.now());

        // 执行转换
        Msg msg = record.toMsg();

        // 验证转换结果
        assertEquals("user", msg.getRole().name().toLowerCase());
        assertEquals("user", msg.getName());
        assertEquals("Test content", msg.getTextContent());
    }

    // ==================== 会话隔离测试 ====================

    /**
     * 测试不同会话的记忆隔离
     *
     * <p>验证：
     * <ul>
     *   <li>不同会话返回不同的记忆记录</li>
     *   <li>记忆内容不会跨会话泄露</li>
     * </ul>
     */
    @Test
    @DisplayName("应该隔离不同会话的记忆")
    void testSessionIsolation() {
        // 准备两个会话的测试数据
        List<MemoryRecord> session1Records = List.of(
                createMemoryRecord(1L, TEST_SESSION_ID, "user", "Message 1", 0),
                createMemoryRecord(2L, TEST_SESSION_ID, "assistant", "Response 1", 1)
        );

        List<MemoryRecord> session2Records = List.of(
                createMemoryRecord(3L, TEST_SESSION_ID_2, "user", "Different message", 0)
        );

        // 配置模拟行为
        when(memoryRepository.findBySessionIdOrderByMessageIndex(TEST_SESSION_ID))
                .thenReturn(session1Records);
        when(memoryRepository.findBySessionIdOrderByMessageIndex(TEST_SESSION_ID_2))
                .thenReturn(session2Records);

        // 执行测试
        List<MemoryRecord> records1 = memoryService.getAllRecords(TEST_SESSION_ID);
        List<MemoryRecord> records2 = memoryService.getAllRecords(TEST_SESSION_ID_2);

        // 验证隔离性
        assertEquals(2, records1.size());
        assertEquals(1, records2.size());
        assertEquals("Message 1", records1.get(0).getContent());
        assertEquals("Different message", records2.get(0).getContent());
    }

    /**
     * 测试清除会话记忆时的独立性
     *
     * <p>验证清除一个会话的记忆不会影响其他会话。
     */
    @Test
    @DisplayName("应该独立清除会话记忆")
    void testClearSessionMemory() {
        // 执行清除
        memoryService.clearSessionMemory(TEST_SESSION_ID);

        // 验证只删除了指定会话
        verify(memoryRepository, times(1)).deleteBySessionId(TEST_SESSION_ID);
        verify(memoryRepository, never()).deleteBySessionId(TEST_SESSION_ID_2);
    }

    // ==================== 搜索测试 ====================

    /**
     * 测试关键词搜索功能
     *
     * <p>验证：
     * <ul>
     *   <li>能够找到包含关键词的消息</li>
     *   <li>返回的消息内容确实包含搜索词</li>
     * </ul>
     */
    @Test
    @DisplayName("应该能够按关键词搜索消息")
    void testSearchMessages() {
        // 准备测试数据
        String keyword = "coffee";
        List<MemoryRecord> searchResults = List.of(
                createMemoryRecord(1L, TEST_SESSION_ID, "user", "I love coffee", 0),
                createMemoryRecord(2L, TEST_SESSION_ID, "assistant", "Coffee is great!", 1)
        );

        // 配置模拟行为
        when(memoryRepository.searchByKeyword(TEST_SESSION_ID, keyword))
                .thenReturn(searchResults);

        // 执行搜索
        List<Msg> results = memoryService.searchMessages(TEST_SESSION_ID, keyword);

        // 验证搜索结果
        assertEquals(2, results.size());
        assertTrue(results.get(0).getTextContent().toLowerCase().contains("coffee"));
    }

    /**
     * 测试搜索无结果的情况
     *
     * <p>验证当关键词不存在时返回空列表。
     */
    @Test
    @DisplayName("搜索无结果时应该返回空列表")
    void testSearchNoResults() {
        // 配置模拟行为
        when(memoryRepository.searchByKeyword(TEST_SESSION_ID, "nonexistent"))
                .thenReturn(List.of());

        // 执行搜索
        List<Msg> results = memoryService.searchMessages(TEST_SESSION_ID, "nonexistent");

        // 验证返回空列表
        assertTrue(results.isEmpty());
    }

    /**
     * 测试跨会话搜索功能
     *
     * <p>验证能够在所有会话中搜索包含关键词的消息。
     */
    @Test
    @DisplayName("应该能够跨会话搜索")
    void testSearchAcrossSessions() {
        // 准备测试数据
        String keyword = "project";
        List<MemoryRecord> crossSessionResults = List.of(
                createMemoryRecord(1L, TEST_SESSION_ID, "user", "Working on project X", 0),
                createMemoryRecord(2L, TEST_SESSION_ID_2, "user", "Project deadline tomorrow", 0)
        );

        // 配置模拟行为
        when(memoryRepository.searchAcrossSessions(keyword))
                .thenReturn(crossSessionResults);

        // 执行跨会话搜索
        List<MemoryRecord> results = memoryService.searchAcrossSessions(keyword);

        // 验证结果包含来自不同会话的记录
        assertEquals(2, results.size());
    }

    // ==================== 最近消息测试 ====================

    /**
     * 测试获取最近消息功能
     *
     * <p>验证能够正确获取指定数量的最近消息。
     */
    @Test
    @DisplayName("应该能够获取限定数量的最近消息")
    void testGetRecentMessages() {
        // 准备测试数据
        List<MemoryRecord> recentRecords = List.of(
                createMemoryRecord(9L, TEST_SESSION_ID, "user", "Recent 1", 8),
                createMemoryRecord(10L, TEST_SESSION_ID, "user", "Recent 2", 9)
        );

        // 配置模拟行为
        when(memoryRepository.findRecentMessages(TEST_SESSION_ID, 5))
                .thenReturn(recentRecords);

        // 执行获取最近消息
        List<Msg> recent = memoryService.getRecentMessages(TEST_SESSION_ID, 5);

        // 验证结果
        assertEquals(2, recent.size());
    }

    // ==================== 统计测试 ====================

    /**
     * 测试记忆统计计算
     *
     * <p>验证统计信息的准确性：
     * <ul>
     *   <li>总消息数</li>
     *   <li>用户消息数</li>
     *   <li>助手消息数</li>
     * </ul>
     */
    @Test
    @DisplayName("应该正确计算记忆统计信息")
    void testMemoryStats() {
        // 准备测试数据：[总数, 用户数, 助手数]
        Object[] stats = new Object[]{10L, 5L, 5L};
        when(memoryRepository.getSessionStats(TEST_SESSION_ID)).thenReturn(stats);

        // 执行统计
        MemoryService.MemoryStats memoryStats = memoryService.getMemoryStats(TEST_SESSION_ID);

        // 验证统计结果
        assertEquals(10L, memoryStats.totalMessages());
        assertEquals(5L, memoryStats.userMessageCount());
        assertEquals(5L, memoryStats.assistantMessageCount());
    }

    /**
     * 测试空统计数据的处理
     *
     * <p>验证当统计查询返回 null 时能够正常处理，返回默认值 0。
     */
    @Test
    @DisplayName("应该优雅处理空统计数据")
    void testNullStats() {
        // 配置返回 null
        when(memoryRepository.getSessionStats(TEST_SESSION_ID)).thenReturn(null);

        // 执行统计
        MemoryService.MemoryStats stats = memoryService.getMemoryStats(TEST_SESSION_ID);

        // 验证返回默认值
        assertEquals(0L, stats.totalMessages());
        assertEquals(0L, stats.userMessageCount());
        assertEquals(0L, stats.assistantMessageCount());
    }

    // ==================== 会话 Agent 测试 ====================

    /**
     * 测试为新会话创建 Agent
     *
     * <p>验证：
     * <ul>
     *   <li>首次获取会创建新 Agent</li>
     *   <li>再次获取返回缓存的同一实例</li>
     *   <li>仓库只查询一次（创建时加载历史）</li>
     * </ul>
     */
    @Test
    @DisplayName("应该为新会话创建 Agent")
    void testCreateAgentForNewSession() {
        // 配置模拟行为
        when(memoryRepository.findBySessionIdOrderByMessageIndex(anyString()))
                .thenReturn(List.of());

        // 执行两次获取
        var agent1 = memoryService.getOrCreateAgent(TEST_SESSION_ID);
        var agent2 = memoryService.getOrCreateAgent(TEST_SESSION_ID);

        // 验证返回同一实例（缓存生效）
        assertSame(agent1, agent2);
        // 验证仓库只查询一次（创建时）
        verify(memoryRepository, times(1)).findBySessionIdOrderByMessageIndex(TEST_SESSION_ID);
    }

    /**
     * 测试创建 Agent 时加载历史消息
     *
     * <p>验证创建 Agent 时会从数据库加载该会话的历史消息到缓存。
     */
    @Test
    @DisplayName("创建 Agent 时应该加载已有消息")
    void testLoadExistingMessagesForAgent() {
        // 准备已有消息
        List<MemoryRecord> existingRecords = List.of(
                createMemoryRecord(1L, TEST_SESSION_ID, "user", "Old message", 0)
        );

        // 配置模拟行为
        when(memoryRepository.findBySessionIdOrderByMessageIndex(TEST_SESSION_ID))
                .thenReturn(existingRecords);

        // 执行创建 Agent
        memoryService.getOrCreateAgent(TEST_SESSION_ID);

        // 验证加载了历史消息
        verify(memoryRepository, times(1)).findBySessionIdOrderByMessageIndex(TEST_SESSION_ID);
        assertFalse(memoryService.getMemoryCache(TEST_SESSION_ID).isEmpty());
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建测试用的 Msg 对象
     *
     * @param content 消息内容
     * @param role 消息角色
     * @param name 发送者名称
     * @return 构建好的 Msg 对象
     */
    private Msg createTestMessage(String content, MsgRole role, String name) {
        return Msg.builder()
                .name(name)
                .role(role)
                .content(TextBlock.builder().text(content).build())
                .build();
    }

    /**
     * 创建测试用的 MemoryRecord 对象
     *
     * @param id 记录 ID
     * @param sessionId 会话 ID
     * @param role 角色
     * @param content 内容
     * @param index 消息索引
     * @return 构建好的 MemoryRecord 对象
     */
    private MemoryRecord createMemoryRecord(Long id, String sessionId, String role,
                                            String content, int index) {
        MemoryRecord record = new MemoryRecord(sessionId, role, content, "test", index);
        record.setId(id);
        record.setCreatedAt(LocalDateTime.now());
        return record;
    }
}
