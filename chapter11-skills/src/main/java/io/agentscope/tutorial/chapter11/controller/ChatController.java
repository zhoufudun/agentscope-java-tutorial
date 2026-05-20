package io.agentscope.tutorial.chapter11.controller;

import io.agentscope.tutorial.chapter11.entity.SkillDefinition;
import io.agentscope.tutorial.chapter11.service.ChatService;
import io.agentscope.tutorial.chapter11.service.SkillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 聊天控制器
 *
 * <p>提供聊天和 Agent 交互的 REST API。
 * 核心功能：
 * <ul>
 *   <li>发送聊天消息</li>
 *   <li>获取聊天历史</li>
 *   <li>清除会话</li>
 *   <li>技能推荐</li>
 *   <li>技能使用统计</li>
 * </ul>
 *
 * <p>API 端点：
 * <ul>
 *   <li>POST /chat - 发送消息</li>
 *   <li>GET /chat/history/{sessionId} - 获取历史</li>
 *   <li>DELETE /chat/history/{sessionId} - 清除会话</li>
 *   <li>GET /chat/recommend-skills - 技能推荐</li>
 *   <li>GET /chat/stats - 聊天统计</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 * @see ChatService
 */
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    /**
     * 聊天服务
     */
    private final ChatService chatService;

    /**
     * 技能服务
     */
    private final SkillService skillService;

    /**
     * 发送聊天消息
     *
     * <p>处理用户消息，返回 AI 响应。
     * 响应中会自动包含技能推荐（如适用）。
     *
     * @param request 聊天请求
     * @return 聊天响应
     */
    @PostMapping
    public ResponseEntity<ChatResponse> sendMessage(@RequestBody ChatRequest request) {
        log.info("Chat request - session: {}, message length: {}",
                request.getSessionId(),
                request.getMessage() != null ? request.getMessage().length() : 0);

        ChatService.ChatResponseDto response = chatService.chat(
                ChatService.ChatRequest.builder()
                        .sessionId(request.getSessionId())
                        .message(request.getMessage())
                        .metadata(request.getMetadata())
                        .build()
        );

        // 构建响应
        ChatResponse chatResponse = ChatResponse.builder()
                .sessionId(response.getSessionId())
                .content(response.getContent())
                .toolCalls(response.getToolCalls())
                .success(response.isSuccess())
                .error(response.getError())
                .skillRecommendations(chatService.recommendSkills(request.getMessage()).stream()
                        .map(SkillDTO::fromEntity)
                        .toList())
                .build();

        return ResponseEntity.ok(chatResponse);
    }

    /**
     * 获取聊天历史
     *
     * @param sessionId 会话 ID
     * @return 历史消息列表
     */
    @GetMapping("/history/{sessionId}")
    public ResponseEntity<List<ChatMessageDTO>> getHistory(@PathVariable String sessionId) {
        List<ChatService.ChatMessage> history = chatService.getHistory(sessionId);

        List<ChatMessageDTO> dtos = history.stream()
                .map(msg -> ChatMessageDTO.builder()
                        .role(msg.getRole())
                        .content(msg.getContent())
                        .timestamp(System.currentTimeMillis())
                        .build())
                .toList();

        return ResponseEntity.ok(dtos);
    }

    /**
     * 清除会话
     *
     * @param sessionId 会话 ID
     * @return 清除结果
     */
    @DeleteMapping("/history/{sessionId}")
    public ResponseEntity<ApiResponse<Void>> clearSession(@PathVariable String sessionId) {
        chatService.clearSession(sessionId);
        return ResponseEntity.ok(ApiResponse.success("Session cleared"));
    }

    /**
     * 清除所有会话
     *
     * @return 清除结果
     */
    @DeleteMapping("/history")
    public ResponseEntity<ApiResponse<Void>> clearAllSessions() {
        chatService.clearAllSessions();
        return ResponseEntity.ok(ApiResponse.success("All sessions cleared"));
    }

    /**
     * 获取技能推荐
     *
     * <p>根据用户消息，推荐可能需要的技能。
     *
     * @param message 用户消息
     * @return 推荐的技能列表
     */
    @GetMapping("/recommend-skills")
    public ResponseEntity<List<SkillDTO>> recommendSkills(
            @RequestParam(required = false) String message) {

        List<SkillDefinition> recommendations = chatService.recommendSkills(message);

        List<SkillDTO> dtos = recommendations.stream()
                .map(SkillDTO::fromEntity)
                .toList();

        return ResponseEntity.ok(dtos);
    }

    /**
     * 获取聊天统计
     *
     * @return 统计信息
     */
    @GetMapping("/stats")
    public ResponseEntity<ChatStatsDTO> getStats() {
        return ResponseEntity.ok(ChatStatsDTO.builder()
                .activeSessions(chatService.getActiveSessionCount())
                .totalSkills(skillService.getStats().getTotalSkills())
                .activeSkills(skillService.getStats().getActiveSkills())
                .registeredSkillIds(skillService.getAllSkillIds())
                .build());
    }

    /**
     * 处理工具调用结果
     *
     * <p>当 Agent 返回工具调用时，调用此接口提交工具执行结果，
     * 并获取 Agent 的后续响应。
     *
     * @param request 工具结果请求
     * @return Agent 后续响应
     */
    @PostMapping("/tool-result")
    public ResponseEntity<ChatResponse> submitToolResult(@RequestBody ToolResultRequest request) {
        log.info("Tool result submitted - session: {}, tool count: {}",
                request.getSessionId(), request.getToolCalls() != null ? request.getToolCalls().size() : 0);

        // 工具执行已在客户端完成，这里处理工具结果的后续对话
        List<ChatService.ToolResult> toolResults = chatService.processToolCalls(
                request.getSessionId(),
                request.getToolCalls()
        );

        // 继续对话
        ChatService.ChatResponseDto response = chatService.chat(
                ChatService.ChatRequest.builder()
                        .sessionId(request.getSessionId())
                        .message("[TOOL_RESULT] " + formatToolResults(toolResults))
                        .build()
        );

        return ResponseEntity.ok(ChatResponse.builder()
                .sessionId(response.getSessionId())
                .content(response.getContent())
                .toolCalls(response.getToolCalls())
                .success(response.isSuccess())
                .error(response.getError())
                .build());
    }

    /**
     * 格式化工具结果
     */
    private String formatToolResults(List<ChatService.ToolResult> results) {
        if (results == null || results.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder("工具执行结果：\n");
        for (ChatService.ToolResult result : results) {
            sb.append("- ").append(result.getToolName())
                    .append(": ").append(result.isSuccess() ? "成功" : "失败")
                    .append("\n");
            if (result.isSuccess()) {
                sb.append("  结果: ").append(result.getResult()).append("\n");
            } else if (result.getError() != null) {
                sb.append("  错误: ").append(result.getError()).append("\n");
            }
        }
        return sb.toString();
    }

    // ==================== DTO 类 ====================

    /**
     * 聊天请求
     */
    @lombok.Data
    public static class ChatRequest {
        private String sessionId;
        private String message;
        private Map<String, Object> metadata;
    }

    /**
     * 聊天响应
     */
    @lombok.Data
    @lombok.Builder
    public static class ChatResponse {
        private String sessionId;
        private String content;
        private List<ChatService.ToolCall> toolCalls;
        private boolean success;
        private String error;
        private List<SkillDTO> skillRecommendations;
    }

    /**
     * 聊天消息 DTO
     */
    @lombok.Data
    @lombok.Builder
    public static class ChatMessageDTO {
        private String role;
        private String content;
        private long timestamp;
    }

    /**
     * 技能 DTO
     */
    @lombok.Data
    @lombok.Builder
    public static class SkillDTO {
        private String skillId;
        private String name;
        private String description;
        private String version;
        private String[] tags;
        private boolean active;

        public static SkillDTO fromEntity(SkillDefinition entity) {
            return SkillDTO.builder()
                    .skillId(entity.getSkillId())
                    .name(entity.getName())
                    .description(entity.getDescription())
                    .version(entity.getVersion())
                    .tags(entity.getTagsArray())
                    .active(entity.isActivated())
                    .build();
        }
    }

    /**
     * 聊天统计 DTO
     */
    @lombok.Data
    @lombok.Builder
    public static class ChatStatsDTO {
        private int activeSessions;
        private long totalSkills;
        private long activeSkills;
        private java.util.Set<String> registeredSkillIds;
    }

    /**
     * 工具结果请求
     */
    @lombok.Data
    public static class ToolResultRequest {
        private String sessionId;
        private List<ChatService.ToolCall> toolCalls;
    }

    /**
     * API 响应包装
     */
    @lombok.Data
    @lombok.Builder
    public static class ApiResponse<T> {
        private boolean success;
        private T data;
        private String error;
        private long timestamp;

        public static <T> ApiResponse<T> success(T data) {
            return ApiResponse.<T>builder()
                    .success(true)
                    .data(data)
                    .timestamp(System.currentTimeMillis())
                    .build();
        }

        public static <T> ApiResponse<T> success(String message) {
            return ApiResponse.<T>builder()
                    .success(true)
                    .data((T) message)
                    .timestamp(System.currentTimeMillis())
                    .build();
        }

        public static <T> ApiResponse<T> error(String message) {
            return ApiResponse.<T>builder()
                    .success(false)
                    .error(message)
                    .timestamp(System.currentTimeMillis())
                    .build();
        }
    }
}