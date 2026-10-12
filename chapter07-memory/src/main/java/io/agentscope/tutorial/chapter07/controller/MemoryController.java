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
 * 记忆管理控制器
 *
 * <p>提供记忆管理操作的 REST API 端点，用于查询、搜索和管理对话记忆。
 *
 * <p><b>核心功能</b>：
 * <ul>
 *   <li>查询对话历史</li>
 *   <li>全文搜索记忆内容</li>
 *   <li>获取最近消息</li>
 *   <li>清除会话记忆</li>
 *   <li>手动触发记忆压缩</li>
 * </ul>
 *
 * <p><b>API 端点</b>：
 * <ul>
 *   <li>GET /api/memory/history - 获取对话历史</li>
 *   <li>GET /api/memory/stats - 获取记忆统计</li>
 *   <li>GET /api/memory/search - 搜索记忆内容</li>
 *   <li>GET /api/memory/recent - 获取最近消息</li>
 *   <li>DELETE /api/memory/clear - 清除会话记忆</li>
 *   <li>GET /api/memory/sessions - 获取所有会话 ID</li>
 *   <li>POST /api/memory/compress - 触发记忆压缩</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 * @since 1.0
 */
@RestController
@RequestMapping("/api/memory")
public class MemoryController {

    /** 记忆服务 */
    private final MemoryService memoryService;

    /**
     * 构造函数
     *
     * @param memoryService 记忆服务
     */
    public MemoryController(MemoryService memoryService) {
        this.memoryService = memoryService;
    }

    /**
     * 获取会话的对话历史
     *
     * @param sessionId 会话标识符
     * @param limit 返回的最大消息数（可选）
     * @return 记忆记录列表
     */
    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> getHistory(
            @RequestParam(defaultValue = "default") String sessionId,
            @RequestParam(required = false) Integer limit) {

        List<MemoryRecord> records = memoryService.getAllRecords(sessionId);

        // 应用数量限制
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
     * 获取会话的记忆统计信息
     *
     * @param sessionId 会话标识符
     * @return 记忆统计数据
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
     * 搜索包含关键词的记忆
     *
     * <p>支持在单个会话内搜索，也支持跨会话搜索。
     *
     * @param sessionId 会话标识符
     * @param keyword 搜索关键词
     * @param acrossSessions 是否跨所有会话搜索（默认 false）
     * @return 搜索结果
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> search(
            @RequestParam(defaultValue = "default") String sessionId,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "false") boolean acrossSessions) {

        List<MemoryRecord> results;
        if (acrossSessions) {
            // 跨会话搜索
            results = memoryService.searchAcrossSessions(keyword);
        } else {
            // 仅搜索指定会话
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
     * 获取会话的最近消息
     *
     * @param sessionId 会话标识符
     * @param limit 获取的消息数量（默认 10）
     * @return 最近的记忆记录
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
     * 清除会话的所有记忆
     *
     * <p>删除指定会话的所有记忆记录，用于重新开始对话。
     *
     * @param sessionId 会话标识符
     * @return 确认消息
     */
    @DeleteMapping("/clear")
    public ResponseEntity<Map<String, String>> clearMemory(
            @RequestParam(defaultValue = "default") String sessionId) {

        memoryService.clearSessionMemory(sessionId);

        return ResponseEntity.ok(Map.of(
                "sessionId", sessionId,
                "message", "记忆已成功清除"
        ));
    }

    /**
     * 获取所有活跃的会话 ID
     *
     * @return 会话 ID 列表
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
     * 触发手动记忆压缩
     *
     * <p>对指定会话执行记忆压缩，将旧消息摘要化以节省上下文空间。
     *
     * @param sessionId 会话标识符
     * @return 压缩结果统计
     */
    @PostMapping("/compress")
    public ResponseEntity<Map<String, Object>> compressMemory(
            @RequestParam(defaultValue = "default") String sessionId) {

        // 记录压缩前的消息数
        long beforeCount = memoryService.getMemoryStats(sessionId).totalMessages();

        // 执行压缩
        memoryService.compressMemory(sessionId);

        // 记录压缩后的消息数
        long afterCount = memoryService.getMemoryStats(sessionId).totalMessages();

        return ResponseEntity.ok(Map.of(
                "sessionId", sessionId,
                "message", "记忆压缩完成",
                "messagesBefore", beforeCount,
                "messagesAfter", afterCount,
                "compressed", beforeCount - afterCount
        ));
    }

    /**
     * 根端点 - 返回服务信息
     *
     * @return 服务描述
     */
    @GetMapping
    public ResponseEntity<Map<String, String>> index() {
        return ResponseEntity.ok(Map.of(
                "service", "AgentScope Java - 记忆系统 API",
                "version", "1.0.0",
                "endpoints", "GET /api/memory/history, /api/memory/stats, /api/memory/search, " +
                        "/api/memory/recent, DELETE /api/memory/clear, GET /api/memory/sessions, " +
                        "POST /api/memory/compress"
        ));
    }

    // ==================== 辅助方法 ====================

    /**
     * 将 MemoryRecord 转换为 Map 以便 JSON 序列化
     *
     * @param record 记忆记录
     * @return Map 表示
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