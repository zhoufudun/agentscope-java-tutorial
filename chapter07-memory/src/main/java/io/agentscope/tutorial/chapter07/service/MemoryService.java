package io.agentscope.tutorial.chapter07.service;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.Model;
import io.agentscope.core.state.AgentStateStore;
import io.agentscope.tutorial.chapter07.config.MemoryConfig;
import io.agentscope.tutorial.chapter07.entity.MemoryRecord;
import io.agentscope.tutorial.chapter07.repository.MemoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 记忆服务
 *
 * <p>记忆系统的核心服务，管理多会话的对话记忆。提供数据库持久化存储，支持：
 * <ul>
 *   <li><b>多会话隔离</b> - 每个会话独立管理记忆</li>
 *   <li><b>记忆压缩</b> - 长对话自动摘要以节省上下文空间</li>
 *   <li><b>全文搜索</b> - 跨对话搜索历史内容</li>
 *   <li><b>会话统计</b> - 提供记忆使用情况分析</li>
 * </ul>
 *
 * <p><b>架构设计</b>：
 * <ul>
 *   <li>使用 {@link ConcurrentHashMap} 缓存会话 Agent 以提高性能</li>
 *   <li>通过 {@link MemoryRepository} 实现长期存储</li>
 *   <li>通过 {@link AgentStateStore} 实现 2.x 的状态持久化</li>
 * </ul>
 *
 * <p><b>2.x 迁移说明</b>：
 * AgentScope 2.x 移除了旧的 Memory 接口，会话历史改由 AgentState 维护。
 * 本服务同时维护数据库存储（用于搜索和持久化）和 AgentStateStore（用于 Agent 上下文）。
 *
 * @author AgentScope Tutorial
 * @since 1.0
 */
@Service
public class MemoryService {

    private static final Logger log = LoggerFactory.getLogger(MemoryService.class);

    /** 记忆记录仓库：提供数据库持久化 */
    private final MemoryRepository memoryRepository;

    /** 记忆配置：压缩阈值等参数 */
    private final MemoryConfig memoryConfig;

    /** AI 模型：用于生成记忆摘要 */
    private final Model model;

    /**
     * 会话状态存储（2.x 记忆持久化后端）
     *
     * <p>为每个会话的 Agent 提供 {@code AgentState} 的自动 load/save 能力，
     * 替代 1.x 中已被移除的 {@code Memory} + {@code StatePersistence}。
     */
    private final AgentStateStore agentStateStore;

    /**
     * 会话 Agent 缓存
     *
     * <p>使用 ConcurrentHashMap 实现线程安全的 Agent 缓存。
     * Key: sessionId, Value: 该会话专属的 ReActAgent
     */
    private final Map<String, ReActAgent> sessionAgents = new ConcurrentHashMap<>();

    /**
     * 会话记忆缓存
     *
     * <p>数据库持久化前的内存缓冲区。
     * Key: sessionId, Value: 缓存的消息列表
     */
    private final Map<String, List<Msg>> sessionMemoryCache = new ConcurrentHashMap<>();

    /**
     * 构造函数
     *
     * @param memoryRepository 记忆记录仓库
     * @param memoryConfig 记忆配置
     * @param model AI 模型（用于摘要生成）
     * @param agentStateStore 会话状态存储
     */
    public MemoryService(MemoryRepository memoryRepository,
                         MemoryConfig memoryConfig,
                         Model model,
                         AgentStateStore agentStateStore) {
        this.memoryRepository = memoryRepository;
        this.memoryConfig = memoryConfig;
        this.model = model;
        this.agentStateStore = agentStateStore;
    }

    // ==================== 会话 Agent 管理 ====================

    /**
     * 获取或创建会话 Agent
     *
     * <p>如果会话已存在 Agent，返回缓存的实例；否则创建新的 Agent 并加载历史记忆。
     * 使用 computeIfAbsent 保证线程安全和原子性。
     *
     * @param sessionId 会话标识符
     * @return 该会话的 ReActAgent 实例
     */
    public ReActAgent getOrCreateAgent(String sessionId) {
        return sessionAgents.computeIfAbsent(sessionId, id -> {
            log.info("为会话 {} 创建新 Agent", id);
            return createSessionAgent(id);
        });
    }

    /**
     * 创建会话专属 Agent
     *
     * <p>创建新 Agent 时会：
     * <ol>
     *   <li>从数据库加载历史消息</li>
     *   <li>将历史消息缓存到内存</li>
     *   <li>配置 Agent 使用 AgentStateStore 持久化上下文</li>
     * </ol>
     *
     * @param sessionId 会话标识符
     * @return 配置好的 ReActAgent 实例
     */
    private ReActAgent createSessionAgent(String sessionId) {
        // 从数据库加载历史消息到内存缓存
        List<MemoryRecord> records = memoryRepository.findBySessionIdOrderByMessageIndex(sessionId);
        List<Msg> cachedMessages = records.stream()
                .map(MemoryRecord::toMsg)
                .collect(Collectors.toList());
        sessionMemoryCache.put(sessionId, cachedMessages);

        log.debug("从数据库加载 {} 条消息到会话 {}", records.size(), sessionId);

        // 2.x：会话历史由 AgentState 维护。通过 stateStore + defaultSessionId
        // 让每个会话的 Agent 自动按 sessionId 载入/保存上下文（AgentState），
        // 取代 1.x 已移除的 .memory(new InMemoryMemory()) 写法。
        return ReActAgent.builder()
                // Agent 名称包含会话 ID 前缀，便于日志追踪
                .name("MemoryAssistant-" + sessionId.substring(0, Math.min(8, sessionId.length())))
                .sysPrompt(buildSystemPrompt())
                .model(model)
                .toolkit(new io.agentscope.core.tool.Toolkit())
                .maxIters(10)
                // 指定状态存储后端
                .stateStore(agentStateStore)
                // 指定默认会话 ID，实现按会话隔离
                .defaultSessionId(sessionId)
                .build();
    }

    /**
     * 构建 Agent 系统提示词
     *
     * <p>定义 Agent 的能力和行为准则。
     *
     * @return 系统提示词文本
     */
    private String buildSystemPrompt() {
        return """
                You are an intelligent assistant with persistent memory capabilities.

                Your capabilities:
                1. You can remember important user preferences and facts across conversations
                2. You should use the remember tool to store important information
                3. You can search your memory using the recall tool when needed
                4. You provide personalized responses based on past interactions

                Guidelines:
                - Be helpful, friendly, and attentive to user details
                - Actively remember important information (names, preferences, facts)
                - When unsure, search your memory to find relevant context
                - Summarize long conversations when they become too long
                """;
    }

    /**
     * 获取会话的记忆缓存
     *
     * @param sessionId 会话标识符
     * @return 缓存的消息列表（可能为空但不会为 null）
     */
    public List<Msg> getMemoryCache(String sessionId) {
        return sessionMemoryCache.computeIfAbsent(sessionId, id -> List.of());
    }

    /**
     * 添加消息到会话记忆
     *
     * <p><b>处理流程</b>：
     * <ol>
     *   <li>将消息添加到内存缓存</li>
     *   <li>将消息持久化到数据库</li>
     *   <li>检查是否需要触发记忆压缩</li>
     * </ol>
     *
     * @param sessionId 会话标识符
     * @param msg 要添加的消息
     */
    @Transactional
    public void addMessage(String sessionId, Msg msg) {
        // 获取当前消息索引（基于数据库中的消息数）
        int messageIndex = (int) memoryRepository.countBySessionId(sessionId);

        // 创建并保存记忆记录
        MemoryRecord record = MemoryRecord.fromMsg(msg, sessionId, messageIndex);
        memoryRepository.save(record);

        // 更新内存缓存
        List<Msg> cache = sessionMemoryCache.computeIfAbsent(sessionId, id -> new java.util.ArrayList<>());
        cache.add(msg);

        log.debug("添加消息到会话 {}: index={}, role={}", sessionId, messageIndex, msg.getRole());

        // 检查是否需要触发压缩
        // 当缓存大小达到阈值时自动压缩
        if (memoryConfig.isCompressionEnabled() && cache.size() >= memoryConfig.getCompressionThresholdCount()) {
            log.info("记忆缓存大小 {} 达到阈值 {}，触发压缩",
                    cache.size(), memoryConfig.getCompressionThresholdCount());
            compressMemory(sessionId);
        }
    }

    // ==================== 记忆检索 ====================

    /**
     * 获取会话的所有记忆记录
     *
     * @param sessionId 会话标识符
     * @return 所有记忆记录列表
     */
    public List<MemoryRecord> getAllRecords(String sessionId) {
        return memoryRepository.findBySessionIdOrderByMessageIndex(sessionId);
    }

    /**
     * 获取会话的所有消息（AgentScope Msg 格式）
     *
     * @param sessionId 会话标识符
     * @return 消息列表
     */
    public List<Msg> getAllMessages(String sessionId) {
        return getMemoryCache(sessionId);
    }

    /**
     * 读取 AgentState 维护的会话上下文（2.x 记忆读取方式）
     *
     * <p>这是 1.x {@code agent.getMemory().getMessages()} 的等价写法：
     * {@code agent.getAgentState(userId, sessionId).getContext()}。上下文由 Agent
     * 在每次 {@code call()} 时自动写入，并通过 {@link AgentStateStore} 持久化。
     *
     * <p><b>注意</b>：2.x 已弃用无参的 {@code getAgentState()}，改用带 {@code (userId, sessionId)}
     * 的重载以显式指定会话身份（{@code userId} 传 {@code null} 走默认命名空间）。
     *
     * @param sessionId 会话标识符
     * @return 该会话 AgentState 中的消息列表
     */
    public List<Msg> getAgentStateContext(String sessionId) {
        return getOrCreateAgent(sessionId).getAgentState(null, sessionId).getContext();
    }

    /**
     * 获取最近的 N 条消息
     *
     * @param sessionId 会话标识符
     * @param limit 最大返回数量
     * @return 最近的消息列表
     */
    public List<Msg> getRecentMessages(String sessionId, int limit) {
        return memoryRepository.findRecentMessages(sessionId, limit).stream()
                .map(MemoryRecord::toMsg)
                .collect(Collectors.toList());
    }

    /**
     * 搜索包含关键词的消息
     *
     * @param sessionId 会话标识符
     * @param keyword 搜索关键词
     * @return 匹配的消息列表
     */
    public List<Msg> searchMessages(String sessionId, String keyword) {
        return memoryRepository.searchByKeyword(sessionId, keyword).stream()
                .map(MemoryRecord::toMsg)
                .collect(Collectors.toList());
    }

    /**
     * 跨所有会话搜索关键词
     *
     * @param keyword 搜索关键词
     * @return 所有会话中匹配的记忆记录
     */
    public List<MemoryRecord> searchAcrossSessions(String keyword) {
        return memoryRepository.searchAcrossSessions(keyword);
    }

    // ==================== 记忆压缩 ====================

    /**
     * 压缩会话记忆
     *
     * <p>将旧消息摘要化以节省上下文空间。
     *
     * <p><b>压缩流程</b>：
     * <ol>
     *   <li>获取未压缩的旧消息</li>
     *   <li>使用 AI 模型生成对话摘要</li>
     *   <li>将原始消息标记为已摘要</li>
     *   <li>保存摘要作为特殊记忆记录</li>
     *   <li>可选删除过旧的已摘要消息</li>
     * </ol>
     *
     * @param sessionId 会话标识符
     */
    @Transactional
    public void compressMemory(String sessionId) {
        int thresholdCount = memoryConfig.getCompressionThresholdCount();
        List<MemoryRecord> toCompress = memoryRepository.findUncompressedMessages(sessionId, thresholdCount);

        if (toCompress.isEmpty()) {
            log.debug("会话 {} 没有需要压缩的消息", sessionId);
            return;
        }

        log.info("压缩会话 {} 的 {} 条消息", sessionId, toCompress.size());

        // 构建摘要请求的上下文
        StringBuilder context = new StringBuilder();
        context.append("Please summarize the following conversation concisely. ");
        context.append("Focus on key facts, decisions, and important details:\n\n");

        for (MemoryRecord record : toCompress) {
            context.append(String.format("[%s] %s: %s\n",
                    record.getRole().toUpperCase(),
                    record.getName() != null ? record.getName() : "unknown",
                    record.getContent()));
        }

        context.append("\n---\nSummary:");

        // 构建摘要请求消息
        Msg summaryRequest = Msg.builder()
                .name("system")
                .role(MsgRole.USER)
                .content(TextBlock.builder().text(context.toString()).build())
                .build();

        try {
            // 使用流式 API 获取完整响应
            ChatResponse response = model.stream(List.of(summaryRequest), null, null)
                    .blockLast();
            String summary = "Summary generation failed";
            if (response != null && response.getContent() != null) {
                // 从响应中提取文本内容
                summary = response.getContent().stream()
                        .filter(TextBlock.class::isInstance)
                        .map(TextBlock.class::cast)
                        .map(TextBlock::getText)
                        .collect(Collectors.joining("\n"));
            }

            // 将原始消息标记为已摘要
            int fromIndex = toCompress.get(0).getMessageIndex();
            int toIndex = toCompress.get(toCompress.size() - 1).getMessageIndex();
            memoryRepository.markAsSummarized(sessionId, fromIndex, toIndex);

            // 将摘要作为特殊记忆记录保存
            MemoryRecord summaryRecord = new MemoryRecord(
                    sessionId,
                    "system",
                    "[记忆摘要] " + summary,
                    "memory-system",
                    toIndex + 1
            );
            summaryRecord.setIsSummarized(true);
            memoryRepository.save(summaryRecord);

            // 清理过旧的已摘要消息（保留最近 50 条）
            int keepFromIndex = Math.max(0, toIndex - 50);
            memoryRepository.deleteSummarizedBeforeIndex(sessionId, keepFromIndex);

            // 更新内存缓存
            List<Msg> cache = sessionMemoryCache.get(sessionId);
            if (cache != null) {
                // 只保留最近 N/2 条消息在缓存中
                int cacheSize = cache.size();
                int toRemove = cacheSize - memoryConfig.getMaxMessages() / 2;
                if (toRemove > 0) {
                    for (int i = 0; i < toRemove; i++) {
                        cache.remove(0);
                    }
                }
                // 将摘要添加到缓存
                cache.add(Msg.builder()
                        .name("memory-system")
                        .role(MsgRole.SYSTEM)
                        .content(TextBlock.builder().text(summary).build())
                        .build());
            }

            log.info("会话 {} 的记忆压缩完成", sessionId);

        } catch (Exception e) {
            log.error("压缩会话 {} 的记忆失败: {}", sessionId, e.getMessage(), e);
        }
    }

    // ==================== 会话管理 ====================

    /**
     * 清除会话的所有记忆
     *
     * <p>同时清除：
     * <ul>
     *   <li>数据库中的记忆记录</li>
     *   <li>缓存的 Agent 实例</li>
     *   <li>内存中的消息缓存</li>
     *   <li>AgentStateStore 中的状态</li>
     * </ul>
     *
     * @param sessionId 会话标识符
     */
    @Transactional
    public void clearSessionMemory(String sessionId) {
        // 清除数据库记录
        memoryRepository.deleteBySessionId(sessionId);
        // 清除 Agent 缓存
        sessionAgents.remove(sessionId);
        // 清除内存缓存
        sessionMemoryCache.remove(sessionId);
        // 2.x：同步清除 AgentStateStore 中该会话持久化的 AgentState
        // （userId 传 null 表示单租户/默认命名空间）
        agentStateStore.delete(null, sessionId);
        log.info("已清除会话 {} 的所有记忆", sessionId);
    }

    /**
     * 获取会话的记忆统计信息
     *
     * @param sessionId 会话标识符
     * @return 记忆统计对象
     */
    public MemoryStats getMemoryStats(String sessionId) {
        Object[] stats = memoryRepository.getSessionStats(sessionId);
        // 安全地解析统计数据，处理 null 情况
        long total = stats != null && stats[0] != null ? ((Number) stats[0]).longValue() : 0;
        long userCount = stats != null && stats[1] != null ? ((Number) stats[1]).longValue() : 0;
        long assistantCount = stats != null && stats[2] != null ? ((Number) stats[2]).longValue() : 0;
        int cachedCount = getMemoryCache(sessionId).size();

        return new MemoryStats(total, userCount, assistantCount, cachedCount);
    }

    /**
     * 获取所有活跃会话的 ID
     *
     * @return 会话 ID 列表
     */
    public List<String> getAllSessionIds() {
        return memoryRepository.findAllSessionIds();
    }

    // ==================== 内部类 ====================

    /**
     * 记忆统计记录
     *
     * <p>封装会话的记忆使用统计信息。
     *
     * @param totalMessages 总消息数
     * @param userMessageCount 用户消息数
     * @param assistantMessageCount 助手消息数
     * @param cachedCount 内存缓存中的消息数
     */
    public record MemoryStats(
            long totalMessages,
            long userMessageCount,
            long assistantMessageCount,
            int cachedCount
    ) {
        /**
         * 返回统计信息的字符串表示
         *
         * @return 格式化的统计字符串
         */
        @Override
        public String toString() {
            return String.format("MemoryStats{total=%d, user=%d, assistant=%d, cached=%d}",
                    totalMessages, userMessageCount, assistantMessageCount, cachedCount);
        }
    }
}