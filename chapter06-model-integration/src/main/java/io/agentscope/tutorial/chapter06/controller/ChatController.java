package io.agentscope.tutorial.chapter06.controller;

import io.agentscope.core.message.Msg;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.tutorial.chapter06.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 聊天控制器
 *
 * <p>提供高级聊天 API，支持会话管理和历史记录。
 * 与 {@code ModelController} 的区别在于本控制器提供更高层的抽象，
 * 封装了会话状态管理和系统提示词注入。
 *
 * <p><b>API 端点</b>：
 * <ul>
 *   <li>POST /api/chat - 发送消息（支持会话上下文）</li>
 *   <li>POST /api/chat/stream - 流式聊天（SSE）</li>
 *   <li>GET /api/chat/history/{sessionId} - 获取会话历史</li>
 *   <li>DELETE /api/chat/history/{sessionId} - 清除会话历史</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 * @since 1.0
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    /** 聊天服务 */
    private final ChatService chatService;

    /**
     * 发送聊天消息
     *
     * <p>支持两种模式：
     * <ul>
     *   <li>无会话模式（sessionId="default"）：每次请求独立，无上下文</li>
     *   <li>会话模式（指定 sessionId）：保持对话上下文，支持多轮对话</li>
     * </ul>
     *
     * <p><b>请求示例</b>：
     * <pre>{@code
     * POST /api/chat
     * {
     *   "provider": "dashscope",
     *   "modelName": "qwen-plus",
     *   "message": "你好",
     *   "sessionId": "user-001",
     *   "includeSystemPrompt": true,
     *   "temperature": 0.7,
     *   "maxTokens": 2000
     * }
     * }</pre>
     *
     * @param request 请求体，包含消息内容和配置参数
     * @return 包含回复内容和使用统计的响应
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> chat(@RequestBody Map<String, Object> request) {
        // 解析请求参数
        String provider = (String) request.get("provider");
        String modelName = (String) request.get("modelName");
        String message = (String) request.get("message");
        String sessionId = (String) request.getOrDefault("sessionId", "default");
        Boolean includeSystemPrompt = (Boolean) request.getOrDefault("includeSystemPrompt", true);

        // 解析可选的生成参数
        Double temperature = request.containsKey("temperature")
                ? ((Number) request.get("temperature")).doubleValue() : null;
        Integer maxTokens = request.containsKey("maxTokens")
                ? ((Number) request.get("maxTokens")).intValue() : null;

        // 构建生成选项（如果有自定义参数）
        GenerateOptions options = null;
        if (temperature != null || maxTokens != null) {
            GenerateOptions.Builder builder = GenerateOptions.builder();
            if (temperature != null) builder.temperature(temperature);
            if (maxTokens != null) builder.maxTokens(maxTokens);
            options = builder.build();
        }

        try {
            ChatResponse response;
            // 根据会话 ID 决定是否使用历史上下文
            if (sessionId != null && !sessionId.equals("default")) {
                response = chatService.chatWithHistory(sessionId, provider, modelName, message, includeSystemPrompt);
            } else {
                response = chatService.chat(provider, modelName, message, options);
            }

            // 构建响应
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("response", extractText(response));
            if (response.getUsage() != null) {
                result.put("usage", response.getUsage());
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("聊天请求失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 流式聊天响应（SSE）
     *
     * <p>使用 Server-Sent Events 实现实时流式输出，
     * 客户端可以逐步接收响应内容，实现打字机效果。
     *
     * <p><b>客户端使用示例</b>（JavaScript）：
     * <pre>{@code
     * const eventSource = new EventSource('/api/chat/stream');
     * eventSource.onmessage = (event) => {
     *   const data = JSON.parse(event.data);
     *   console.log(data.content);
     *   if (data.done) {
     *     eventSource.close();
     *   }
     * };
     * }</pre>
     *
     * @param request 请求体
     * @return SSE 事件流
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Map<String, Object>> streamChat(@RequestBody Map<String, Object> request) {
        String provider = (String) request.get("provider");
        String modelName = (String) request.get("modelName");
        String message = (String) request.get("message");

        Double temperature = request.containsKey("temperature")
                ? ((Number) request.get("temperature")).doubleValue() : null;

        GenerateOptions options = null;
        if (temperature != null) {
            options = GenerateOptions.builder().temperature(temperature).build();
        }

        // 返回流式响应
        return chatService.streamChat(provider, modelName, message, options)
                .map(response -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("content", extractText(response));
                    result.put("done", isResponseDone(response));
                    if (response.getUsage() != null) {
                        result.put("usage", response.getUsage());
                    }
                    return result;
                })
                .timeout(Duration.ofSeconds(120))  // 2分钟超时
                .onErrorResume(e -> {
                    log.error("流式聊天失败", e);
                    return Flux.just(Map.of(
                            "error", true,
                            "message", e.getMessage()
                    ));
                });
    }

    /**
     * 获取会话历史记录
     *
     * @param sessionId 会话标识符
     * @return 该会话的所有消息列表
     */
    @GetMapping("/history/{sessionId}")
    public ResponseEntity<List<Msg>> getHistory(@PathVariable String sessionId) {
        return ResponseEntity.ok(chatService.getHistory(sessionId));
    }

    /**
     * 清除会话历史记录
     *
     * <p>删除指定会话的所有历史消息，用于重新开始对话。
     *
     * @param sessionId 会话标识符
     * @return 操作结果
     */
    @DeleteMapping("/history/{sessionId}")
    public ResponseEntity<Map<String, Object>> clearHistory(@PathVariable String sessionId) {
        chatService.clearHistory(sessionId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // ==================== 辅助方法 ====================

    /**
     * 从 ChatResponse 中提取文本内容
     *
     * <p>遍历响应中的所有内容块，提取并拼接文本内容。
     *
     * @param response 聊天响应
     * @return 提取的文本内容
     */
    private String extractText(ChatResponse response) {
        if (response == null || response.getContent() == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (io.agentscope.core.message.ContentBlock block : response.getContent()) {
            if (block instanceof io.agentscope.core.message.TextBlock textBlock) {
                sb.append(textBlock.getText());
            }
        }
        return sb.toString();
    }

    /**
     * 判断响应是否完成
     *
     * <p>检查响应中是否包含文本内容块，作为完成标志。
     *
     * @param response 聊天响应
     * @return 是否完成
     */
    private boolean isResponseDone(ChatResponse response) {
        if (response == null || response.getContent() == null) {
            return false;
        }
        // 如果存在文本内容块，认为是最终响应
        return response.getContent().stream()
                .anyMatch(block -> block instanceof io.agentscope.core.message.TextBlock);
    }
}