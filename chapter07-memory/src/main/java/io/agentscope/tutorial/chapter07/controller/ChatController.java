package io.agentscope.tutorial.chapter07.controller;

import io.agentscope.core.message.Msg;
import io.agentscope.tutorial.chapter07.entity.MemoryRecord;
import io.agentscope.tutorial.chapter07.service.ChatService;
import io.agentscope.tutorial.chapter07.service.MemoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Chat Controller
 *
 * <p>REST API endpoints for chat interactions with the AI agent.
 * Integrates with the memory system to provide persistent conversations.
 *
 * <p>Available endpoints:
 * <ul>
 *   <li>POST /api/chat/send - Send a message and get a response</li>
 *   <li>POST /api/chat/send-with-context - Send message with additional context</li>
 *   <li>GET /api/chat/history - Get conversation history</li>
 *   <li>GET /api/chat/stats - Get memory statistics</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ChatService chatService;
    private final MemoryService memoryService;

    public ChatController(ChatService chatService, MemoryService memoryService) {
        this.chatService = chatService;
        this.memoryService = memoryService;
    }

    /**
     * Sends a message and returns the agent's response.
     *
     * <p>Request body should contain:
     * <pre>{@code
     * {
     *   "message": "Hello, how are you?"
     * }
     * }</pre>
     *
     * <p>Response includes:
     * <ul>
     *   <li>sessionId - The session this conversation belongs to</li>
     *   <li>response - The agent's response text</li>
     *   <li>memoryStats - Current memory usage statistics</li>
     *   <li>usage - Token usage information (if available)</li>
     * </ul>
     *
     * @param sessionId Session identifier (query param)
     * @param request Request body with the message
     * @return Chat response with agent reply and metadata
     */
    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendMessage(
            @RequestParam(defaultValue = "default") String sessionId,
            @RequestBody Map<String, String> request) {

        String userMessage = request.get("message");

        if (userMessage == null || userMessage.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Message cannot be empty"));
        }

        log.info("Received message for session: {}, length: {}", sessionId, userMessage.length());

        try {
            ChatService.ChatResponse response = chatService.processMessage(sessionId, userMessage);

            Map<String, Object> result = new HashMap<>();
            result.put("sessionId", response.sessionId());
            result.put("userMessage", response.userMessage());
            result.put("response", response.response());

            if (response.memoryStats() != null) {
                result.put("memoryStats", Map.of(
                        "totalMessages", response.memoryStats().totalMessages(),
                        "userMessages", response.memoryStats().userMessageCount(),
                        "assistantMessages", response.memoryStats().assistantMessageCount(),
                        "cachedCount", response.memoryStats().cachedCount()
                ));
            }

            if (response.usage() != null) {
                result.put("usage", Map.of(
                        "inputTokens", response.usage().getInputTokens(),
                        "outputTokens", response.usage().getOutputTokens(),
                        "totalTokens", response.usage().getTotalTokens()
                ));
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error processing message for session {}: {}", sessionId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "error", "Failed to process message",
                            "sessionId", sessionId,
                            "details", e.getMessage()
                    ));
        }
    }

    /**
     * Sends a message with additional system context.
     *
     * <p>This endpoint allows prepending additional context to the user's message,
     * useful for scenarios where you want to inject external information.
     *
     * @param sessionId Session identifier
     * @param request Request body with message and optional context
     * @return Chat response
     */
    @PostMapping("/send-with-context")
    public ResponseEntity<Map<String, Object>> sendMessageWithContext(
            @RequestParam(defaultValue = "default") String sessionId,
            @RequestBody Map<String, String> request) {

        String userMessage = request.get("message");
        String systemContext = request.get("context");

        if (userMessage == null || userMessage.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Message cannot be empty"));
        }

        log.info("Received message with context for session: {}", sessionId);

        ChatService.ChatResponse response = chatService.processMessageWithContext(
                sessionId, userMessage, systemContext);

        Map<String, Object> result = new HashMap<>();
        result.put("sessionId", response.sessionId());
        result.put("userMessage", response.userMessage());
        result.put("response", response.response());

        if (response.memoryStats() != null) {
            result.put("memoryStats", Map.of(
                    "totalMessages", response.memoryStats().totalMessages(),
                    "userMessages", response.memoryStats().userMessageCount(),
                    "assistantMessages", response.memoryStats().assistantMessageCount()
            ));
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Gets the conversation history for a session.
     *
     * @param sessionId Session identifier
     * @param limit Maximum number of messages (optional)
     * @return List of messages
     */
    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> getHistory(
            @RequestParam(defaultValue = "default") String sessionId,
            @RequestParam(required = false) Integer limit) {

        List<MemoryRecord> records = memoryService.getAllRecords(sessionId);

        // Apply limit if specified
        if (limit != null && limit > 0 && limit < records.size()) {
            records = records.subList(records.size() - limit, records.size());
        }

        List<Map<String, Object>> historyData = records.stream()
                .map(this::recordToMap)
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
                "sessionId", sessionId,
                "count", records.size(),
                "history", historyData
        ));
    }

    /**
     * Gets memory statistics for a session.
     *
     * @param sessionId Session identifier
     * @return Memory statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats(
            @RequestParam(defaultValue = "default") String sessionId) {

        MemoryService.MemoryStats stats = memoryService.getMemoryStats(sessionId);

        return ResponseEntity.ok(Map.of(
                "sessionId", sessionId,
                "totalMessages", stats.totalMessages(),
                "userMessages", stats.userMessageCount(),
                "assistantMessages", stats.assistantMessageCount(),
                "cachedCount", stats.cachedCount()
        ));
    }

    /**
     * Root endpoint - returns service information.
     */
    @GetMapping
    public ResponseEntity<Map<String, String>> index() {
        return ResponseEntity.ok(Map.of(
                "service", "AgentScope Java - Chat API",
                "version", "1.0.0",
                "endpoints", "POST /api/chat/send, POST /api/chat/send-with-context, " +
                        "GET /api/chat/history, GET /api/chat/stats"
        ));
    }

    // ==================== Helper Methods ====================

    /**
     * Converts a MemoryRecord to a Map for JSON serialization.
     */
    private Map<String, Object> recordToMap(MemoryRecord record) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", record.getId());
        map.put("sessionId", record.getSessionId());
        map.put("role", record.getRole());
        map.put("name", record.getName());
        map.put("content", record.getContent());
        map.put("contentType", record.getContentType());
        map.put("messageIndex", record.getMessageIndex());
        map.put("isSummarized", record.getIsSummarized());
        map.put("createdAt", record.getCreatedAt() != null
                ? record.getCreatedAt().toString() : null);
        return map;
    }
}