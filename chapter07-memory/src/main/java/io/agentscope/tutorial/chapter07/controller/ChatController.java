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
 * 聊天控制器
 *
 * <p>提供与 AI Agent 对话的 REST API 接口，集成记忆系统实现持久化对话。
 *
 * <p><b>核心特性</b>：
 * <ul>
 *   <li>自动保存对话到记忆系统</li>
 *   <li>支持多会话隔离</li>
 *   <li>提供会话历史查询</li>
 *   <li>返回记忆统计信息</li>
 * </ul>
 *
 * <p><b>API 端点</b>：
 * <ul>
 *   <li>POST /api/chat/send - 发送消息获取回复</li>
 *   <li>POST /api/chat/send-with-context - 带额外上下文发送消息</li>
 *   <li>GET /api/chat/history - 获取对话历史</li>
 *   <li>GET /api/chat/stats - 获取记忆统计</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 * @since 1.0
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    /** 聊天服务 */
    private final ChatService chatService;

    /** 记忆服务 */
    private final MemoryService memoryService;

    /**
     * 构造函数
     *
     * @param chatService 聊天服务
     * @param memoryService 记忆服务
     */
    public ChatController(ChatService chatService, MemoryService memoryService) {
        this.chatService = chatService;
        this.memoryService = memoryService;
    }

    /**
     * 发送消息并获取 Agent 回复
     *
     * <p>处理流程：
     * <ol>
     *   <li>创建或获取会话 Agent</li>
     *   <li>将用户消息添加到记忆</li>
     *   <li>调用 Agent 生成回复</li>
     *   <li>将回复保存到记忆</li>
     *   <li>返回回复和元数据</li>
     * </ol>
     *
     * <p><b>请求体格式</b>：
     * <pre>{@code
     * {
     *   "message": "你好，请介绍一下自己"
     * }
     * }</pre>
     *
     * <p><b>响应内容</b>：
     * <ul>
     *   <li>sessionId - 会话标识</li>
     *   <li>response - Agent 回复文本</li>
     *   <li>memoryStats - 记忆使用统计</li>
     *   <li>usage - Token 使用信息</li>
     * </ul>
     *
     * @param sessionId 会话标识符（查询参数，默认 "default"）
     * @param request 请求体，包含消息内容
     * @return 包含 Agent 回复和元数据的响应
     */
    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendMessage(
            @RequestParam(defaultValue = "default") String sessionId,
            @RequestBody Map<String, String> request) {

        String userMessage = request.get("message");

        // 参数校验
        if (userMessage == null || userMessage.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "消息内容不能为空"));
        }

        log.info("收到会话 {} 的消息，长度: {}", sessionId, userMessage.length());

        try {
            // 处理消息
            ChatService.ChatResponse response = chatService.processMessage(sessionId, userMessage);

            // 构建响应
            Map<String, Object> result = new HashMap<>();
            result.put("sessionId", response.sessionId());
            result.put("userMessage", response.userMessage());
            result.put("response", response.response());

            // 添加记忆统计
            if (response.memoryStats() != null) {
                result.put("memoryStats", Map.of(
                        "totalMessages", response.memoryStats().totalMessages(),
                        "userMessages", response.memoryStats().userMessageCount(),
                        "assistantMessages", response.memoryStats().assistantMessageCount(),
                        "cachedCount", response.memoryStats().cachedCount()
                ));
            }

            // 添加 Token 使用统计
            if (response.usage() != null) {
                result.put("usage", Map.of(
                        "inputTokens", response.usage().getInputTokens(),
                        "outputTokens", response.usage().getOutputTokens(),
                        "totalTokens", response.usage().getTotalTokens()
                ));
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("处理会话 {} 的消息失败: {}", sessionId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "error", "消息处理失败",
                            "sessionId", sessionId,
                            "details", e.getMessage()
                    ));
        }
    }

    /**
     * 带额外上下文发送消息
     *
     * <p>允许在用户消息前注入额外的系统上下文，适用于需要动态添加背景信息的场景。
     *
     * @param sessionId 会话标识符
     * @param request 请求体，包含 message 和可选的 context
     * @return 聊天响应
     */
    @PostMapping("/send-with-context")
    public ResponseEntity<Map<String, Object>> sendMessageWithContext(
            @RequestParam(defaultValue = "default") String sessionId,
            @RequestBody Map<String, String> request) {

        String userMessage = request.get("message");
        String systemContext = request.get("context");

        if (userMessage == null || userMessage.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "消息内容不能为空"));
        }

        log.info("收到会话 {} 的带上下文消息", sessionId);

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
     * 获取会话的对话历史
     *
     * @param sessionId 会话标识符
     * @param limit 返回的最大消息数（可选）
     * @return 消息列表
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
     * 根端点 - 返回服务信息
     *
     * @return 服务描述
     */
    @GetMapping
    public ResponseEntity<Map<String, String>> index() {
        return ResponseEntity.ok(Map.of(
                "service", "AgentScope Java - 聊天 API",
                "version", "1.0.0",
                "endpoints", "POST /api/chat/send, POST /api/chat/send-with-context, " +
                        "GET /api/chat/history, GET /api/chat/stats"
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