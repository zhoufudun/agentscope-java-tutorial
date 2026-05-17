package io.agentscope.tutorial.chapter07.tool;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import io.agentscope.tutorial.chapter07.service.MemoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Memory Management Tool
 *
 * <p>Provides tools for the AI agent to manage conversation memory. These tools
 * allow the agent to remember important information, search past conversations,
 * and retrieve recent memories.
 *
 * <p>Available Tools:
 * <ul>
 *   <li>{@link #remember(String)} - Store important information in memory</li>
 *   <li>{@link #recall(String)} - Search for information in memory</li>
 *   <li>{@link #getRecent(int)} - Get the most recent N memories</li>
 * </ul>
 *
 * <p>Example usage by the agent:
 * <pre>{@code
 * // Remember a user's preference
 * remember("User prefers dark mode theme")

 * // Search for relevant information
 * recall("dark mode")

 * // Get recent conversations
 * getRecent(5)
 * }</pre>
 *
 * @author AgentScope Tutorial
 */
public class MemoryManagementTool {

    private static final Logger log = LoggerFactory.getLogger(MemoryManagementTool.class);

    private final MemoryService memoryService;

    /**
     * Current session context - typically set by the agent runtime.
     */
    private String currentSessionId = "default";

    public MemoryManagementTool(MemoryService memoryService) {
        this.memoryService = memoryService;
    }

    /**
     * 设置 current session context for memory operations.
     *
     * @param sessionId The session identifier
     */
    public void setSessionId(String sessionId) {
        this.currentSessionId = sessionId;
    }

    /**
     * Gets the current session ID.
     *
     * @return The current session identifier
     */
    public String getSessionId() {
        return currentSessionId;
    }

    // ==================== Tool Methods ====================

    /**
     * Stores important information in memory.
     *
     * <p>Use this tool to remember user preferences, important facts,
     * or any information that should persist across conversations.
     *
     * <p>This is especially useful for:
     * <ul>
     *   <li>User preferences (theme, language, name)</li>
     *   <li>Important facts mentioned during conversation</li>
     *   <li>Decisions or agreements made</li>
     *   <li>Context that should be remembered long-term</li>
     * </ul>
     *
     * @param content The information to remember (be specific and clear)
     * @return Confirmation message with what was remembered
     */
    @Tool(
            name = "remember",
            description = """
                Store important information in the agent's memory for persistent access across conversations.

                Use this when:
                - User shares their name, preferences, or personal details
                - Important facts or decisions are mentioned
                - You want to remember context for future interactions
                - User explicitly asks you to remember something

                The content should be clear and specific so it can be easily retrieved later.

                Examples:
                - "User's name is John and they prefer dark mode"
                - "Customer's order ID is 12345, items include coffee and muffin"
                - "Important: deadline for this project is next Friday"
                """
    )
    public String remember(
            @ToolParam(
                    name = "content",
                    description = "The information to remember. Be specific and clear. " +
                            "Include relevant details like names, dates, preferences, etc.",
                    required = true
            )
            String content
    ) {
        log.info("remember tool called with content: {}", content);

        try {
            // Create a system message with the memory content
            io.agentscope.core.message.Msg memoryMsg = io.agentscope.core.message.Msg.builder()
                    .name("agent-memory")
                    .role(io.agentscope.core.message.MsgRole.SYSTEM)
                    .content(io.agentscope.core.message.TextBlock.builder()
                            .text("[记忆] " + content)
                            .build())
                    .build();

            // Add to memory
            memoryService.addMessage(currentSessionId, memoryMsg);

            return "已成功记住: " + content;

        } catch (Exception e) {
            log.error("Error in remember tool: {}", e.getMessage(), e);
            return "记忆失败: " + e.getMessage();
        }
    }

    /**
     * Searches memory for relevant information.
     *
     * <p>Use this tool to find information previously stored in memory.
     * The search is case-insensitive and matches partial keywords.
     *
     * <p>This is useful when:
     * <ul>
     *   <li>User asks about something mentioned earlier</li>
     *   <li>You need to recall user preferences</li>
     *   <li>Checking if certain information was discussed</li>
     *   <li>Verifying facts or decisions from past conversations</li>
     * </ul>
     *
     * @param keyword The search keyword (case-insensitive)
     * @return Search results with matching memories, or a "not found" message
     */
    @Tool(
            name = "recall",
            description = """
                Search the agent's memory for relevant information using keywords.

                Use this when:
                - User asks about something mentioned earlier
                - You need to recall user preferences or personal details
                - Verifying information from past conversations
                - User says things like "do you remember...", "what did I tell you about..."
                - Checking if specific information was discussed

                The search is case-insensitive and matches partial keywords.
                返回 all matching memories with their content.
                """
    )
    public String recall(
            @ToolParam(
                    name = "keyword",
                    description = "The keyword to search for in memory. Case-insensitive partial match.",
                    required = true
            )
            String keyword
    ) {
        log.info("recall tool called with keyword: {}", keyword);

        try {
            if (keyword == null || keyword.isBlank()) {
                return "搜索关键词不能为空";
            }

            List<io.agentscope.core.message.Msg> results = memoryService.searchMessages(currentSessionId, keyword);

            if (results.isEmpty()) {
                return "没有找到与 '" + keyword + "' 相关的记忆";
            }

            // Format results
            String formattedResults = results.stream()
                    .map(m -> "- " + m.getTextContent())
                    .collect(Collectors.joining("\n"));

            return "找到 " + results.size() + " 条相关记忆:\n" + formattedResults;

        } catch (Exception e) {
            log.error("Error in recall tool: {}", e.getMessage(), e);
            return "搜索失败: " + e.getMessage();
        }
    }

    /**
     * Retrieves the most recent N memories.
     *
     * <p>Use this tool to get context from recent conversations when
     * you need to recall what was just discussed.
     *
     * <p>This is useful for:
     * <ul>
     *   <li>Getting context at the start of a new interaction</li>
     *   <li>Reviewing recent conversation flow</li>
     *   <li>Finding the most recent user preferences</li>
     *   <li>Recovering from errors by checking recent history</li>
     * </ul>
     *
     * @param count Number of recent memories to retrieve (1-20 recommended)
     * @return Formatted list of recent memories with timestamps
     */
    @Tool(
            name = "getRecent",
            description = """
                Retrieve the most recent N memories from the conversation history.

                Use this when:
                - Starting a new topic and want to recall recent context
                - Need to review what was just discussed
                - User asks "what did we talk about" or similar
                - You want to provide personalized responses based on recent history

                Recommended counts:
                - 3-5: For quick context on current topic
                - 10: For broader conversation history
                - 20: For comprehensive memory review
                """
    )
    public String getRecent(
            @ToolParam(
                    name = "count",
                    description = "Number of recent memories to retrieve (recommended: 3-20)",
                    required = true
            )
            int count
    ) {
        log.info("getRecent tool called with count: {}", count);

        try {
            // Clamp count to reasonable limits
            int effectiveCount = Math.max(1, Math.min(count, 20));

            List<io.agentscope.core.message.Msg> results =
                    memoryService.getRecentMessages(currentSessionId, effectiveCount);

            if (results.isEmpty()) {
                return "没有最近的记忆";
            }

            // Format results (newest first, reverse for display)
            StringBuilder sb = new StringBuilder();
            sb.append("最近 ").append(results.size()).append(" 条记忆:\n");

            // Display in chronological order (oldest first for readability)
            List<io.agentscope.core.message.Msg> reversed = new java.util.ArrayList<>(results);
            java.util.Collections.reverse(reversed);

            for (int i = 0; i < reversed.size(); i++) {
                io.agentscope.core.message.Msg msg = reversed.get(i);
                sb.append(i + 1)
                        .append(". [")
                        .append(msg.getRole())
                        .append("] ")
                        .append(msg.getTextContent())
                        .append("\n");
            }

            return sb.toString();

        } catch (Exception e) {
            log.error("Error in getRecent tool: {}", e.getMessage(), e);
            return "获取失败: " + e.getMessage();
        }
    }

    /**
     * Gets memory statistics for the current session.
     *
     * <p>This is a utility tool to check how much memory is being used.
     *
     * @return Formatted statistics about the current session's memory
     */
    @Tool(
            name = "getMemoryStats",
            description = """
                Get statistics about the current session's memory usage.

                Returns:
                - Total number of messages stored
                - Number of user messages
                - Number of assistant messages
                - Current memory cache size

                Useful for debugging or when you want to know how much context is available.
                """
    )
    public String getMemoryStats() {
        log.info("getMemoryStats tool called");

        try {
            MemoryService.MemoryStats stats = memoryService.getMemoryStats(currentSessionId);

            return String.format("""
                    记忆统计:
                    - 总消息数: %d
                    - 用户消息: %d
                    - 助手消息: %d
                    - 缓存大小: %d
                    """,
                    stats.totalMessages(),
                    stats.userMessageCount(),
                    stats.assistantMessageCount(),
                    stats.cachedCount());

        } catch (Exception e) {
            log.error("Error in getMemoryStats tool: {}", e.getMessage(), e);
            return "获取统计失败: " + e.getMessage();
        }
    }
}