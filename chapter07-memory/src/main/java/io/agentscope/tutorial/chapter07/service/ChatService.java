package io.agentscope.tutorial.chapter07.service;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Chat Service
 *
 * <p>Handles chat interactions between users and agents, integrating with the
 * memory system for persistent conversation history.
 *
 * <p>This service:
 * <ul>
 *   <li>Routes user messages to appropriate session agents</li>
 *   <li>Records all interactions to persistent memory</li>
 *   <li>返回 structured chat responses</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final MemoryService memoryService;

    public ChatService(MemoryService memoryService) {
        this.memoryService = memoryService;
    }

    /**
     * Processes a chat message and returns the agent's response.
     *
     * <p>该方法:
     * <ol>
     *   <li>Creates or retrieves the session agent</li>
     *   <li>Adds the user message to memory</li>
     *   <li>Invokes the agent to generate a response</li>
     *   <li>Records the response to memory</li>
     *   <li>返回 response to the caller</li>
     * </ol>
     *
     * @param sessionId The session identifier
     * @param userMessage The user's message text
     * @return ChatResponse containing the agent's response and metadata
     */
    public ChatResponse processMessage(String sessionId, String userMessage) {
        log.info("Processing message for session: {}, message length: {}",
                sessionId, userMessage.length());

        try {
            // Get or create session agent
            ReActAgent agent = memoryService.getOrCreateAgent(sessionId);

            // Build user message
            Msg userMsg = Msg.builder()
                    .name("user")
                    .role(MsgRole.USER)
                    .content(TextBlock.builder().text(userMessage).build())
                    .build();

            // Record user message to memory before processing
            memoryService.addMessage(sessionId, userMsg);

            // Get recent context from memory for better responses
            List<Msg> recentContext = memoryService.getRecentMessages(sessionId, 10);
            if (!recentContext.isEmpty()) {
                log.debug("Using {} recent messages as context", recentContext.size());
            }

            // Invoke the agent
            Msg response = agent.call(userMsg).block();

            // Extract response text
            String responseText = "";
            if (response != null) {
                responseText = response.getTextContent();

                // Record assistant response to memory
                memoryService.addMessage(sessionId, response);

                log.info("Generated response for session: {}, response length: {}",
                        sessionId, responseText.length());
            } else {
                log.warn("No response generated for session: {}", sessionId);
                responseText = "抱歉，我现在无法处理您的请求。请稍后再试。";
            }

            // Get updated memory stats
            MemoryService.MemoryStats stats = memoryService.getMemoryStats(sessionId);

            return new ChatResponse(
                    sessionId,
                    userMessage,
                    responseText,
                    stats,
                    response != null ? response.getChatUsage() : null
            );

        } catch (Exception e) {
            log.error("Error processing message for session {}: {}", sessionId, e.getMessage(), e);
            return new ChatResponse(
                    sessionId,
                    userMessage,
                    "处理消息时发生错误: " + e.getMessage(),
                    null,
                    null
            );
        }
    }

    /**
     * Processes a message with additional context.
     *
     * @param sessionId The session identifier
     * @param userMessage The user's message text
     * @param systemContext Additional system context to prepend
     * @return ChatResponse containing the agent's response
     */
    public ChatResponse processMessageWithContext(String sessionId, String userMessage, String systemContext) {
        log.info("Processing message with context for session: {}", sessionId);

        // Prepend system context to user message
        String enhancedMessage = systemContext != null && !systemContext.isBlank()
                ? systemContext + "\n\nUser: " + userMessage
                : userMessage;

        return processMessage(sessionId, enhancedMessage);
    }

    // ==================== Response Record ====================

    /**
     * Chat response containing the agent's reply and metadata.
     */
    public record ChatResponse(
            /** The session this response belongs to */
            String sessionId,

            /** The original user message */
            String userMessage,

            /** The agent's response text */
            String response,

            /** Current memory statistics */
            MemoryService.MemoryStats memoryStats,

            /** Token usage information from the model */
            io.agentscope.core.model.ChatUsage usage
    ) {
        @Override
        public String toString() {
            return String.format("ChatResponse{sessionId='%s', response='%s...'}",
                    sessionId,
                    response != null && response.length() > 50
                            ? response.substring(0, 50) + "..."
                            : response);
        }
    }
}