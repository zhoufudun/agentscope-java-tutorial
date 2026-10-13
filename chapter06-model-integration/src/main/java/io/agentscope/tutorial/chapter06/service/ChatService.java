package io.agentscope.tutorial.chapter06.service;

import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.tutorial.chapter06.config.AgentConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 聊天服务
 *
 * <p>处理与模型的聊天交互，提供高级聊天操作，包括流式和非流式聊天。
 * 封装了消息构建、模型调用和响应处理的完整流程。
 *
 * <p><b>核心功能</b>：
 * <ul>
 *   <li>单轮对话（无上下文）</li>
 *   <li>多轮对话（带会话历史）</li>
 *   <li>流式响应输出</li>
 * </ul>
 *
 * <p><b>使用示例</b>：
 * <pre>{@code
 * // 简单聊天
 * ChatResponse response = chatService.chat("dashscope", "qwen-plus", "你好");
 *
 * // 带会话历史的聊天
 * ChatResponse response = chatService.chatWithHistory(
 *     "session-001", "dashscope", "qwen-plus", "继续刚才的话题", true);
 *
 * // 流式聊天
 * Flux<ChatResponse> stream = chatService.streamChat("dashscope", "qwen-plus", "讲个故事");
 * }</pre>
 *
 * @author AgentScope Tutorial
 * @since 1.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ChatService {

    /** 模型服务 */
    private final ModelService modelService;

    /** Agent 配置 */
    private final AgentConfig agentConfig;

    /**
     * 会话历史缓存
     * <p>Key: sessionId, Value: 该会话的消息列表
     * <p>使用 ConcurrentHashMap 保证线程安全
     */
    private final Map<String, List<Msg>> conversationHistory = new ConcurrentHashMap<>();

    /**
     * 发送聊天消息（使用默认选项）
     *
     * @param provider 模型提供商
     * @param modelName 模型名称
     * @param userMessage 用户消息
     * @return 聊天响应
     */
    public ChatResponse chat(String provider, String modelName, String userMessage) {
        return chat(provider, modelName, userMessage, null);
    }

    /**
     * 发送聊天消息（使用自定义选项）
     *
     * <p>构建包含系统提示词和用户消息的请求，调用指定模型获取响应。
     *
     * @param provider 模型提供商（dashscope/openai/ollama/gemini）
     * @param modelName 模型名称
     * @param userMessage 用户消息内容
     * @param options 生成选项（温度、最大 token 等），可为 null 使用默认值
     * @return 聊天响应
     * @throws IllegalArgumentException 如果模型不存在
     * @throws RuntimeException 如果请求失败
     */
    public ChatResponse chat(String provider, String modelName, String userMessage, GenerateOptions options) {
        // 获取模型实例
        Model model = modelService.getModel(provider, modelName);
        if (model == null) {
            throw new IllegalArgumentException("未找到模型: " + provider + ":" + modelName);
        }

        // 构建消息列表（系统提示 + 用户消息）
        List<Msg> messages = List.of(
                Msg.builder()
                        .role(MsgRole.SYSTEM)
                        .content(TextBlock.builder().text(agentConfig.getDefaultSystemPrompt()).build())
                        .build(),
                Msg.builder()
                        .role(MsgRole.USER)
                        .content(TextBlock.builder().text(userMessage).build())
                        .build()
        );

        // 使用默认选项（如果未提供）
        GenerateOptions effectiveOptions = options != null ? options : GenerateOptions.builder()
                .temperature(agentConfig.getDefaultTemperature())
                .maxTokens(agentConfig.getDefaultMaxTokens())
                .build();

        try {
            // 调用模型（使用流式 API 但阻塞等待完整结果）
            return model.stream(messages, null, effectiveOptions)
                    .blockLast(Duration.ofSeconds(60));
        } catch (Exception e) {
            log.error("聊天请求失败 {}:{}", provider, modelName, e);
            throw new RuntimeException("聊天请求失败: " + e.getMessage(), e);
        }
    }

    /**
     * 发送聊天消息（带会话历史）
     *
     * <p>维护会话上下文，支持多轮对话。每次调用会将消息添加到会话历史。
     *
     * @param sessionId 会话标识符
     * @param provider 模型提供商
     * @param modelName 模型名称
     * @param userMessage 用户消息
     * @param includeSystemPrompt 是否包含系统提示词
     * @return 聊天响应
     */
    public ChatResponse chatWithHistory(String sessionId, String provider, String modelName,
                                        String userMessage, boolean includeSystemPrompt) {
        // 获取模型实例
        Model model = modelService.getModel(provider, modelName);
        if (model == null) {
            throw new IllegalArgumentException("未找到模型: " + provider + ":" + modelName);
        }

        // 获取会话历史
        List<Msg> history = conversationHistory.getOrDefault(sessionId, List.of());
        List<Msg> messages;

        // 可选地添加系统提示词
        if (includeSystemPrompt) {
            messages = List.of(
                    Msg.builder()
                            .role(MsgRole.SYSTEM)
                            .content(TextBlock.builder().text(agentConfig.getDefaultSystemPrompt()).build())
                            .build()
            );
        } else {
            messages = List.of();
        }

        // 合并历史消息
        messages = new java.util.ArrayList<>(messages);
        messages.addAll(history);

        // 添加当前用户消息
        messages.add(Msg.builder()
                .role(MsgRole.USER)
                .content(TextBlock.builder().text(userMessage).build())
                .build());

        // 构建生成选项
        GenerateOptions options = GenerateOptions.builder()
                .temperature(agentConfig.getDefaultTemperature())
                .maxTokens(agentConfig.getDefaultMaxTokens())
                .build();

        try {
            // 调用模型
            ChatResponse response = model.stream(messages, null, options)
                    .blockLast(Duration.ofSeconds(60));

            // 保存到会话历史
            if (response != null) {
                // 保存用户消息
                saveToHistory(sessionId,
                        Msg.builder()
                                .role(MsgRole.USER)
                                .content(TextBlock.builder().text(userMessage).build())
                                .build()
                );
                // 保存助手响应
                saveToHistory(sessionId,
                        Msg.builder()
                                .role(MsgRole.ASSISTANT)
                                .content(TextBlock.builder().text(extractText(response)).build())
                                .build()
                );
            }

            return response;
        } catch (Exception e) {
            log.error("会话 {} 聊天失败", sessionId, e);
            throw new RuntimeException("聊天请求失败: " + e.getMessage(), e);
        }
    }

    /**
     * 流式聊天响应（使用默认选项）
     *
     * @param provider 模型提供商
     * @param modelName 模型名称
     * @param userMessage 用户消息
     * @return 响应流
     */
    public Flux<ChatResponse> streamChat(String provider, String modelName, String userMessage) {
        return streamChat(provider, modelName, userMessage, null);
    }

    /**
     * 流式聊天响应
     *
     * <p>返回响应式流，客户端可以逐步接收响应内容。
     * 适用于需要即时反馈的交互场景，如打字机效果。
     *
     * @param provider 模型提供商
     * @param modelName 模型名称
     * @param userMessage 用户消息
     * @param options 生成选项
     * @return 响应流
     */
    public Flux<ChatResponse> streamChat(String provider, String modelName,
                                         String userMessage, GenerateOptions options) {
        // 获取模型实例
        Model model = modelService.getModel(provider, modelName);
        if (model == null) {
            throw new IllegalArgumentException("未找到模型: " + provider + ":" + modelName);
        }

        // 构建消息列表
        List<Msg> messages = List.of(
                Msg.builder()
                        .role(MsgRole.SYSTEM)
                        .content(TextBlock.builder().text(agentConfig.getDefaultSystemPrompt()).build())
                        .build(),
                Msg.builder()
                        .role(MsgRole.USER)
                        .content(TextBlock.builder().text(userMessage).build())
                        .build()
        );

        // 使用默认选项（如果未提供）
        GenerateOptions effectiveOptions = options != null ? options : GenerateOptions.builder()
                .temperature(agentConfig.getDefaultTemperature())
                .maxTokens(agentConfig.getDefaultMaxTokens())
                .build();

        // 返回流式响应
        return model.stream(messages, null, effectiveOptions);
    }

    /**
     * 保存消息到会话历史
     *
     * @param sessionId 会话标识符
     * @param message 要保存的消息
     */
    private void saveToHistory(String sessionId, Msg message) {
        conversationHistory.computeIfAbsent(sessionId, k -> new java.util.ArrayList<>());
        conversationHistory.get(sessionId).add(message);
    }

    /**
     * 清除会话历史
     *
     * @param sessionId 会话标识符
     */
    public void clearHistory(String sessionId) {
        conversationHistory.remove(sessionId);
        log.info("已清除会话 {} 的历史记录", sessionId);
    }

    /**
     * 获取会话历史
     *
     * @param sessionId 会话标识符
     * @return 会话消息列表
     */
    public List<Msg> getHistory(String sessionId) {
        return conversationHistory.getOrDefault(sessionId, List.of());
    }

    /**
     * 从 ChatResponse 中提取文本内容
     *
     * @param response 聊天响应
     * @return 提取的文本
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
}