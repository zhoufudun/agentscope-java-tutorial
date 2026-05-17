package io.agentscope.tutorial.chapter11.service;

import com.alibaba.cloud.ai.model.ChatCompletion;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import io.agentscope.core.model.Model;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.tutorial.chapter11.entity.SkillDefinition;
import lombok.RequiredArgs构造函数;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 聊天服务
 *
 * <p>提供 Agent 聊天能力，支持技能集成。
 * 核心功能：
 * <ul>
 *   <li>多轮对话管理</li>
 *   <li>技能系统集成（自动加载技能提示词）</li>
 *   <li>会话历史管理</li>
 * </ul>
 *
 * <p>技能集成流程：
 * <pre>
 * 1. 用户发送消息
 * 2. 获取技能提示词（SkillService.getSkillPrompt()）
 * 3. 构建系统提示词（包含技能信息）
 * 4. 调用模型
 * 5. 返回响应
 * </pre>
 *
 * @author AgentScope Tutorial
 * @see SkillService
 */
@Service
@RequiredArgs构造函数
@Slf4j
public class ChatService {

    /**
     * 技能服务
     */
    private final SkillService skillService;

    /**
     * 模型
     */
    private final Model model;

    /**
     * 工具箱
     */
    private final Toolkit toolkit;

    /**
     * 默认系统提示词模板
     */
    @Value("${agentscope.chat.system-prompt:你是一个有用的AI助手。}")
    private String systemPromptTemplate;

    /**
     * 技能提示词前缀
     */
    @Value("${agentscope.chat.skill-prompt-prefix:你可以使用以下技能：}")
    private String skillPromptPrefix;

    /**
     * 最大对话历史长度
     */
    @Value("${agentscope.chat.max-history:20}")
    private int maxHistorySize;

    /**
     * 会话存储（sessionId -> 消息列表）
     */
    private final Map<String, List<ChatMessage>> sessions = new ConcurrentHashMap<>();

    /**
     * 发送聊天消息
     *
     * <p>处理用户消息，返回 AI 响应。
     * 自动集成技能系统，在系统提示词中包含技能信息。
     *
     * @param request 聊天请求
     * @return 聊天响应
     */
    public ChatResponse chat(ChatRequest request) {
        try {
            // 获取或创建会话
            String sessionId = request.getSessionId() != null
                    ? request.getSessionId()
                    : UUID.randomUUID().toString();

            // 添加用户消息到历史
            List<ChatMessage> history = sessions.computeIfAbsent(sessionId, k -> new ArrayList<>());
            history.add(new ChatMessage("user", request.getMessage()));

            // 构建消息列表
            List<Message> messages = buildMessages(sessionId, request.getMessage());

            // 调用模型
            ChatCompletion response = model.chatComplete(messages);

            // 解析响应
            String content = parseContent(response);
            List<ToolCall> toolCalls = parseToolCalls(response);

            // 添加助手消息到历史
            history.add(new ChatMessage("assistant", content));

            // 限制历史长度
            while (history.size() > maxHistorySize * 2) {
                history.remove(1);
            }

            return ChatResponse.builder()
                    .sessionId(sessionId)
                    .content(content)
                    .toolCalls(toolCalls)
                    .success(true)
                    .build();

        } catch (Exception e) {
            log.error("Chat error: {}", e.getMessage(), e);
            return ChatResponse.builder()
                    .sessionId(request.getSessionId())
                    .content("抱歉，发生了错误：" + e.getMessage())
                    .success(false)
                    .error(e.getMessage())
                    .build();
        }
    }

    /**
     * 解析模型响应内容
     */
    private String parseContent(ChatCompletion response) {
        if (response == null) {
            return "模型返回为空";
        }
        try {
            var choices = response.getChoices();
            if (choices != null && !choices.isEmpty()) {
                var first = choices.get(0);
                var message = first.getMessage();
                if (message != null) {
                    var content = message.getContent();
                    if (content != null && !content.isEmpty()) {
                        return content;
                    }
                }
            }
            return "模型未返回有效内容";
        } catch (Exception e) {
            log.warn("Failed to parse content: {}", e.getMessage());
            return "解析响应失败";
        }
    }

    /**
     * 解析工具调用
     */
    private List<ToolCall> parseToolCalls(ChatCompletion response) {
        // 简化实现，实际需要根据具体响应格式解析
        return Collections.emptyList();
    }

    /**
     * 构建消息列表
     *
     * <p>包含系统消息（带技能信息）、历史消息和当前用户消息。
     */
    private List<Message> buildMessages(String sessionId, String userMessage) {
        List<Message> messages = new ArrayList<>();

        // 1. 系统消息（包含技能提示词）
        String systemContent = buildSystemPrompt();
        messages.add(Message.builder()
                .role(Role.SYSTEM.getValue())
                .content(systemContent)
                .build());

        // 2. 会话历史
        List<ChatMessage> history = sessions.getOr默认(sessionId, Collections.emptyList());
        for (int i = 0; i < history.size() - 1; i++) { // 排除最后一条用户消息
            ChatMessage msg = history.get(i);
            messages.add(Message.builder()
                    .role("user".equals(msg.getRole()) ? Role.USER.getValue() : Role.ASSISTANT.getValue())
                    .content(msg.getContent())
                    .build());
        }

        // 3. 当前用户消息
        messages.add(Message.builder()
                .role(Role.USER.getValue())
                .content(userMessage)
                .build());

        return messages;
    }

    /**
     * 构建系统提示词
     *
     * <p>包含基础提示词和技能信息。
     */
    private String buildSystemPrompt() {
        StringBuilder prompt = new StringBuilder();
        prompt.append(systemPromptTemplate).append("\n\n");

        // 添加技能信息
        String skillPrompt = skillService.getSkillPrompt();
        if (skillPrompt != null && !skillPrompt.isEmpty()) {
            prompt.append(skillPromptPrefix).append("\n");
            prompt.append(skillPrompt).append("\n");
        }

        return prompt.toString();
    }

    /**
     * 处理带工具调用的响应
     *
     * <p>当模型返回工具调用时，执行工具并返回结果。
     *
     * @param sessionId 会话 ID
     * @param toolCalls 工具调用列表
     * @return 工具结果
     */
    public List<ToolResult> processToolCalls(String sessionId, List<ToolCall> toolCalls) {
        if (toolCalls == null || toolCalls.isEmpty()) {
            return Collections.emptyList();
        }

        List<ToolResult> results = new ArrayList<>();
        List<ChatMessage> history = sessions.get(sessionId);

        for (ToolCall toolCall : toolCalls) {
            try {
                // 执行工具
                String result = executeTool(toolCall);

                // 添加工具结果到历史
                if (history != null) {
                    history.add(new ChatMessage("tool", result));
                }

                results.add(ToolResult.builder()
                        .toolCallId(toolCall.getId())
                        .toolName(toolCall.getName())
                        .result(result)
                        .success(true)
                        .build());

            } catch (Exception e) {
                log.error("Tool execution error: {} - {}", toolCall.getName(), e.getMessage());

                results.add(ToolResult.builder()
                        .toolCallId(toolCall.getId())
                        .toolName(toolCall.getName())
                        .result("工具执行失败：" + e.getMessage())
                        .success(false)
                        .error(e.getMessage())
                        .build());
            }
        }

        return results;
    }

    /**
     * 执行工具
     */
    private String executeTool(ToolCall toolCall) {
        // 工具执行逻辑
        return "Tool executed: " + toolCall.getName();
    }

    /**
     * 获取会话历史
     *
     * @param sessionId 会话 ID
     * @return 消息历史
     */
    public List<ChatMessage> getHistory(String sessionId) {
        return new ArrayList<>(sessions.getOr默认(sessionId, Collections.emptyList()));
    }

    /**
     * 清除会话
     *
     * @param sessionId 会话 ID
     */
    public void clearSession(String sessionId) {
        sessions.remove(sessionId);
        log.info("Session cleared: {}", sessionId);
    }

    /**
     * 清除所有会话
     */
    public void clearAllSessions() {
        sessions.clear();
        log.info("All sessions cleared");
    }

    /**
     * 获取活跃会话数
     */
    public int getActiveSessionCount() {
        return sessions.size();
    }

    /**
     * 获取技能推荐
     *
     * <p>根据用户消息，推荐可能需要的技能。
     *
     * @param message 用户消息
     * @return 推荐的技能列表
     */
    public List<SkillDefinition> recommendSkills(String message) {
        if (message == null || message.isBlank()) {
            return Collections.emptyList();
        }

        // 简单的关键词匹配
        List<SkillDefinition> allSkills = skillService.getActiveSkills();
        String lowerMessage = message.toLowerCase();

        return allSkills.stream()
                .filter(skill -> {
                    String name = skill.getName().toLowerCase();
                    String desc = (skill.getDescription() != null ? skill.getDescription() : "").toLowerCase();
                    String tags = (skill.getTags() != null ? skill.getTags() : "").toLowerCase();

                    return lowerMessage.contains(name) ||
                           name.contains(lowerMessage) ||
                           desc.contains(lowerMessage) ||
                           tags.contains(lowerMessage);
                })
                .limit(3)
                .collect(Collectors.toList());
    }

    // ==================== DTO 类 ====================

    /**
     * 聊天请求
     */
    @lombok.Data
    @lombok.Builder
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
        private List<ToolCall> toolCalls;
        private boolean success;
        private String error;
    }

    /**
     * 聊天消息
     */
    @lombok.Data
    @lombok.AllArgs构造函数
    public static class ChatMessage {
        private String role;
        private String content;
    }

    /**
     * 工具调用
     */
    @lombok.Data
    @lombok.Builder
    public static class ToolCall {
        private String id;
        private String name;
        private Map<String, Object> arguments;
    }

    /**
     * 工具结果
     */
    @lombok.Data
    @lombok.Builder
    public static class ToolResult {
        private String toolCallId;
        private String toolName;
        private String result;
        private boolean success;
        private String error;
    }
}