package io.agentscope.tutorial.chapter07.controller;

import io.agentscope.tutorial.chapter07.entity.MemoryRecord;
import io.agentscope.tutorial.chapter07.service.MemoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Memory Controller
 *
 * <p>REST API endpoints for memory management operations.
 * Provides endpoints for querying, searching, and managing conversation memory.
 *
 * <p>Available endpoints:
 * <ul>
 *   <li>GET /api/memory/history - Get conversation history</li>
 *   <li>GET /api/memory/stats - Get memory statistics</li>
 *   <li>GET /api/memory/search - Search memory by keyword</li>
 *   <li>GET /api/memory/recent - Get recent messages</li>
 *   <li>DELETE /api/memory/clear - Clear session memory</li>
 *   <li>GET /api/memory/sessions - Get all session IDs</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 */
@RestController
@RequestMapping("/api/memory")
public class MemoryController {

    private final MemoryService memoryService;

    public MemoryController(MemoryService memoryService) {
        this.memoryService = memoryService;
    }

    /**
     * Gets the conversation history for a session.
     *
     * @param sessionId Session identifier
     * @param limit Maximum number of messages to return (optional)
     * @return List of memory records
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
     * Searches memory for messages containing a keyword.
     *
     * @param sessionId Session identifier
     * @param keyword Search keyword
     * @param acrossSessions Search across all sessions (default: false)
     * @return Search results
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> search(
            @RequestParam(defaultValue = "default") String sessionId,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "false") boolean acrossSessions) {

        List<MemoryRecord> results;
        if (acrossSessions) {
            results = memoryService.searchAcrossSessions(keyword);
        } else {
            results = memoryService.searchAcrossSessions(keyword).stream()
                    .filter(r -> r.getSessionId().equals(sessionId))
                    .collect(Collectors.toList());
        }

        List<Map<String, Object>> searchResults = results.stream()
                .map(this::recordToMap)
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
                "sessionId", sessionId,
                "keyword", keyword,
                "acrossSessions", acrossSessions,
                "count", results.size(),
                "results", searchResults
        ));
    }

    /**
     * Gets recent messages for a session.
     *
     * @param sessionId Session identifier
     * @param limit Number of recent messages to retrieve (default: 10)
     * @return Recent memory records
     */
    @GetMapping("/recent")
    public ResponseEntity<Map<String, Object>> getRecent(
            @RequestParam(defaultValue = "default") String sessionId,
            @RequestParam(defaultValue = "10") int limit) {

        List<MemoryRecord> records = memoryService.getAllRecords(sessionId);
        int effectiveLimit = Math.min(limit, records.size());
        List<MemoryRecord> recentRecords = records.subList(records.size() - effectiveLimit, records.size());

        List<Map<String, Object>> recentData = recentRecords.stream()
                .map(this::recordToMap)
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
                "sessionId", sessionId,
                "limit", limit,
                "count", recentRecords.size(),
                "recent", recentData
        ));
    }

    /**
     * Clears all memory for a session.
     *
     * @param sessionId Session identifier
     * @return Confirmation message
     */
    @DeleteMapping("/clear")
    public ResponseEntity<Map<String, String>> clearMemory(
            @RequestParam(defaultValue = "default") String sessionId) {

        memoryService.clearSessionMemory(sessionId);

        return ResponseEntity.ok(Map.of(
                "sessionId", sessionId,
                "message", "Memory cleared successfully"
        ));
    }

    /**
     * Gets all active session IDs.
     *
     * @return List of all session IDs
     */
    @GetMapping("/sessions")
    public ResponseEntity<Map<String, Object>> getAllSessions() {
        List<String> sessionIds = memoryService.getAllSessionIds();

        return ResponseEntity.ok(Map.of(
                "count", sessionIds.size(),
                "sessions", sessionIds
        ));
    }

    /**
     * Triggers manual memory compression for a session.
     *
     * @param sessionId Session identifier
     * @return Confirmation message
     */
    @PostMapping("/compress")
    public ResponseEntity<Map<String, Object>> compressMemory(
            @RequestParam(defaultValue = "default") String sessionId) {

        long beforeCount = memoryService.getMemoryStats(sessionId).totalMessages();

        memoryService.compressMemory(sessionId);

        long afterCount = memoryService.getMemoryStats(sessionId).totalMessages();

        return ResponseEntity.ok(Map.of(
                "sessionId", sessionId,
                "message", "Memory compression completed",
                "messagesBefore", beforeCount,
                "messagesAfter", afterCount,
                "compressed", beforeCount - afterCount
        ));
    }

    /**
     * Root endpoint - returns service information.
     */
    @GetMapping
    public ResponseEntity<Map<String, String>> index() {
        return ResponseEntity.ok(Map.of(
                "service", "AgentScope Java - Memory System API",
                "version", "1.0.0",
                "endpoints", "GET /api/memory/history, /api/memory/stats, /api/memory/search, " +
                        "/api/memory/recent, DELETE /api/memory/clear, GET /api/memory/sessions, " +
                        "POST /api/memory/compress"
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