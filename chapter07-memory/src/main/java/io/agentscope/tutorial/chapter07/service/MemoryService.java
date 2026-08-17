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
 * Memory Service
 *
 * <p>Central service for managing conversation memory across multiple sessions.
 * Provides database-backed persistent storage with support for:
 * <ul>
 *   <li>Multi-session isolation</li>
 *   <li>Memory compression and summarization</li>
 *   <li>Full-text search across conversations</li>
 *   <li>Session statistics and analytics</li>
 * </ul>
 *
 * <p>This service maintains a cache of session agents for performance and
 * delegates long-term storage to the database via MemoryRepository.
 *
 * @author AgentScope Tutorial
 */
@Service
public class MemoryService {

    private static final Logger log = LoggerFactory.getLogger(MemoryService.class);

    private final MemoryRepository memoryRepository;
    private final MemoryConfig memoryConfig;
    private final Model model;

    /**
     * 会话状态存储（2.x 记忆持久化后端）。
     *
     * <p>为每个会话的 Agent 提供 {@code AgentState} 的自动 load/save 能力，
     * 替代 1.x 中已被移除的 {@code Memory} + {@code StatePersistence}。
     */
    private final AgentStateStore agentStateStore;

    /**
     * Cache of session agents for quick access.
     * Key: sessionId, Value: Agent with session-specific memory
     */
    private final Map<String, ReActAgent> sessionAgents = new ConcurrentHashMap<>();

    /**
     * Cache of session memories (in-memory buffer before DB persistence).
     * Key: sessionId, Value: List of cached messages
     */
    private final Map<String, List<Msg>> sessionMemoryCache = new ConcurrentHashMap<>();

    public MemoryService(MemoryRepository memoryRepository,
                         MemoryConfig memoryConfig,
                         Model model,
                         AgentStateStore agentStateStore) {
        this.memoryRepository = memoryRepository;
        this.memoryConfig = memoryConfig;
        this.model = model;
        this.agentStateStore = agentStateStore;
    }

    // ==================== Session Agent Management ====================

    /**
     * Gets or creates an agent for the specified session.
     *
     * <p>If an agent already exists for this session, it returns the cached instance.
     * Otherwise, creates a new agent with session-specific memory.
     *
     * @param sessionId The session identifier
     * @return ReActAgent configured for this session
     */
    public ReActAgent getOrCreateAgent(String sessionId) {
        return sessionAgents.computeIfAbsent(sessionId, id -> {
            log.info("Creating new agent for session: {}", id);
            return createSessionAgent(id);
        });
    }

    /**
     * Creates a new session-specific agent.
     *
     * @param sessionId The session identifier
     * @return A new ReActAgent with database-backed memory
     */
    private ReActAgent createSessionAgent(String sessionId) {
        // Load existing messages from database into memory cache
        List<MemoryRecord> records = memoryRepository.findBySessionIdOrderByMessageIndex(sessionId);
        List<Msg> cachedMessages = records.stream()
                .map(MemoryRecord::toMsg)
                .collect(Collectors.toList());
        sessionMemoryCache.put(sessionId, cachedMessages);

        log.debug("Loaded {} messages from database for session: {}", records.size(), sessionId);

        // 2.x：会话历史由 AgentState 维护。通过 stateStore + defaultSessionId
        // 让每个会话的 Agent 自动按 sessionId 载入/保存上下文（AgentState），
        // 取代 1.x 已移除的 .memory(new InMemoryMemory()) 写法。
        return ReActAgent.builder()
                .name("MemoryAssistant-" + sessionId.substring(0, Math.min(8, sessionId.length())))
                .sysPrompt(buildSystemPrompt())
                .model(model)
                .toolkit(new io.agentscope.core.tool.Toolkit())
                .maxIters(10)
                .stateStore(agentStateStore)
                .defaultSessionId(sessionId)
                .build();
    }

    /**
     * Builds the system prompt for session agents.
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
     * Gets the current memory cache for a session.
     *
     * @param sessionId The session identifier
     * @return List of cached messages (may be empty but never null)
     */
    public List<Msg> getMemoryCache(String sessionId) {
        return sessionMemoryCache.computeIfAbsent(sessionId, id -> List.of());
    }

    /**
     * Adds a message to the session memory.
     *
     * <p>该方法:
     * <ol>
     *   <li>Adds the message to the in-memory cache</li>
     *   <li>Persists the message to the database</li>
     *   <li>Triggers compression check if threshold is reached</li>
     * </ol>
     *
     * @param sessionId The session identifier
     * @param msg The message to add
     */
    @Transactional
    public void addMessage(String sessionId, Msg msg) {
        // Get current message index
        int messageIndex = (int) memoryRepository.countBySessionId(sessionId);

        // Create and save the memory record
        MemoryRecord record = MemoryRecord.fromMsg(msg, sessionId, messageIndex);
        memoryRepository.save(record);

        // Update in-memory cache
        List<Msg> cache = sessionMemoryCache.computeIfAbsent(sessionId, id -> new java.util.ArrayList<>());
        cache.add(msg);

        log.debug("Added message to session {}: index={}, role={}", sessionId, messageIndex, msg.getRole());

        // Check if compression should be triggered
        if (memoryConfig.isCompressionEnabled() && cache.size() >= memoryConfig.getCompressionThresholdCount()) {
            log.info("Memory cache size {} reached threshold {}, triggering compression",
                    cache.size(), memoryConfig.getCompressionThresholdCount());
            compressMemory(sessionId);
        }
    }

    // ==================== Memory Retrieval ====================

    /**
     * Retrieves all messages for a session from the database.
     *
     * @param sessionId The session identifier
     * @return List of all memory records for the session
     */
    public List<MemoryRecord> getAllRecords(String sessionId) {
        return memoryRepository.findBySessionIdOrderByMessageIndex(sessionId);
    }

    /**
     * Retrieves all messages as AgentScope Msg objects.
     *
     * @param sessionId The session identifier
     * @return List of all messages for the session
     */
    public List<Msg> getAllMessages(String sessionId) {
        return getMemoryCache(sessionId);
    }

    /**
     * 读取由 AgentState 维护的会话上下文（2.x 记忆读取方式）。
     *
     * <p>这是 1.x {@code agent.getMemory().getMessages()} 的等价写法：
     * {@code agent.getAgentState(userId, sessionId).getContext()}。上下文由 Agent
     * 在每次 {@code call()} 时自动写入，并通过 {@link AgentStateStore} 持久化。
     *
     * <p>注意：2.x 已弃用无参的 {@code getAgentState()}，改用带 {@code (userId, sessionId)}
     * 的重载以显式指定会话身份（{@code userId} 传 {@code null} 走默认命名空间）。
     *
     * @param sessionId The session identifier
     * @return 该会话 AgentState 中的消息列表
     */
    public List<Msg> getAgentStateContext(String sessionId) {
        return getOrCreateAgent(sessionId).getAgentState(null, sessionId).getContext();
    }

    /**
     * Retrieves the most recent N messages.
     *
     * @param sessionId The session identifier
     * @param limit Maximum number of messages to retrieve
     * @return List of recent messages
     */
    public List<Msg> getRecentMessages(String sessionId, int limit) {
        return memoryRepository.findRecentMessages(sessionId, limit).stream()
                .map(MemoryRecord::toMsg)
                .collect(Collectors.toList());
    }

    /**
     * Searches for messages containing a keyword.
     *
     * @param sessionId The session identifier
     * @param keyword The search keyword
     * @return List of matching messages
     */
    public List<Msg> searchMessages(String sessionId, String keyword) {
        return memoryRepository.searchByKeyword(sessionId, keyword).stream()
                .map(MemoryRecord::toMsg)
                .collect(Collectors.toList());
    }

    /**
     * Searches across all sessions for a keyword.
     *
     * @param keyword The search keyword
     * @return List of matching memory records
     */
    public List<MemoryRecord> searchAcrossSessions(String keyword) {
        return memoryRepository.searchAcrossSessions(keyword);
    }

    // ==================== Memory Compression ====================

    /**
     * Compresses memory by summarizing older messages.
     *
     * <p>该方法:
     * <ol>
     *   <li>Finds uncompressed messages older than the threshold</li>
     *   <li>Generates a summary using the AI model</li>
     *   <li>Marks old messages as summarized</li>
     *   <li>Optionally deletes very old summarized messages</li>
     * </ol>
     *
     * @param sessionId The session identifier
     */
    @Transactional
    public void compressMemory(String sessionId) {
        int thresholdCount = memoryConfig.getCompressionThresholdCount();
        List<MemoryRecord> toCompress = memoryRepository.findUncompressedMessages(sessionId, thresholdCount);

        if (toCompress.isEmpty()) {
            log.debug("No messages to compress for session: {}", sessionId);
            return;
        }

        log.info("Compressing {} messages for session: {}", toCompress.size(), sessionId);

        // Build the conversation context for summarization
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

        // Generate summary using the model
        Msg summaryRequest = Msg.builder()
                .name("system")
                .role(MsgRole.USER)
                .content(TextBlock.builder().text(context.toString()).build())
                .build();

        try {
            // Use stream API to get the full response
            ChatResponse response = model.stream(List.of(summaryRequest), null, null)
                    .blockLast();
            String summary = "Summary generation failed";
            if (response != null && response.getContent() != null) {
                summary = response.getContent().stream()
                        .filter(TextBlock.class::isInstance)
                        .map(TextBlock.class::cast)
                        .map(TextBlock::getText)
                        .collect(Collectors.joining("\n"));
            }

            // Mark old messages as summarized
            int fromIndex = toCompress.get(0).getMessageIndex();
            int toIndex = toCompress.get(toCompress.size() - 1).getMessageIndex();
            memoryRepository.markAsSummarized(sessionId, fromIndex, toIndex);

            // Add summary as a special memory record
            MemoryRecord summaryRecord = new MemoryRecord(
                    sessionId,
                    "system",
                    "[记忆摘要] " + summary,
                    "memory-system",
                    toIndex + 1
            );
            summaryRecord.setIsSummarized(true);
            memoryRepository.save(summaryRecord);

            // Clean up very old summarized messages (keep last 50)
            int keepFromIndex = Math.max(0, toIndex - 50);
            memoryRepository.deleteSummarizedBeforeIndex(sessionId, keepFromIndex);

            // Update in-memory cache
            List<Msg> cache = sessionMemoryCache.get(sessionId);
            if (cache != null) {
                // Keep only the last N messages in cache
                int cacheSize = cache.size();
                int toRemove = cacheSize - memoryConfig.getMaxMessages() / 2;
                if (toRemove > 0) {
                    for (int i = 0; i < toRemove; i++) {
                        cache.remove(0);
                    }
                }
                cache.add(Msg.builder()
                        .name("memory-system")
                        .role(MsgRole.SYSTEM)
                        .content(TextBlock.builder().text(summary).build())
                        .build());
            }

            log.info("Memory compression completed for session: {}", sessionId);

        } catch (Exception e) {
            log.error("Failed to compress memory for session {}: {}", sessionId, e.getMessage(), e);
        }
    }

    // ==================== Session Management ====================

    /**
     * Clears all memory for a session.
     *
     * @param sessionId The session identifier
     */
    @Transactional
    public void clearSessionMemory(String sessionId) {
        memoryRepository.deleteBySessionId(sessionId);
        sessionAgents.remove(sessionId);
        sessionMemoryCache.remove(sessionId);
        // 2.x：同步清除 AgentStateStore 中该会话持久化的 AgentState
        // （userId 传 null 表示单租户/默认命名空间）。
        agentStateStore.delete(null, sessionId);
        log.info("Cleared all memory for session: {}", sessionId);
    }

    /**
     * Gets memory statistics for a session.
     *
     * @param sessionId The session identifier
     * @return MemoryStats object with session statistics
     */
    public MemoryStats getMemoryStats(String sessionId) {
        Object[] stats = memoryRepository.getSessionStats(sessionId);
        long total = stats != null && stats[0] != null ? ((Number) stats[0]).longValue() : 0;
        long userCount = stats != null && stats[1] != null ? ((Number) stats[1]).longValue() : 0;
        long assistantCount = stats != null && stats[2] != null ? ((Number) stats[2]).longValue() : 0;
        int cachedCount = getMemoryCache(sessionId).size();

        return new MemoryStats(total, userCount, assistantCount, cachedCount);
    }

    /**
     * Gets all active session IDs.
     *
     * @return List of all session IDs with memory records
     */
    public List<String> getAllSessionIds() {
        return memoryRepository.findAllSessionIds();
    }

    // ==================== Inner Classes ====================

    /**
     * Memory statistics record.
     */
    public record MemoryStats(
            long totalMessages,
            long userMessageCount,
            long assistantMessageCount,
            int cachedCount
    ) {
        @Override
        public String toString() {
            return String.format("MemoryStats{total=%d, user=%d, assistant=%d, cached=%d}",
                    totalMessages, userMessageCount, assistantMessageCount, cachedCount);
        }
    }
}