/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.agentscope.tutorial.chapter03.service;

import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.extensions.model.dashscope.DashScopeChatModel;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.tutorial.chapter03.entity.ChatSession;
import io.agentscope.tutorial.chapter03.formatter.JsonLogFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 对话服务 - 整合 Session 和 AI 模型的核心服务.
 *
 * <p>本服务是第三章的核心业务逻辑类，负责处理用户聊天请求，
 * 管理多轮对话上下文，并调用 AI 模型生成回复。
 *
 * <h2>核心功能</h2>
 * <ul>
 *   <li>处理用户聊天请求</li>
 *   <li>管理多轮对话上下文（内存 + 数据库双层缓存）</li>
 *   <li>调用 DashScope 模型生成回复</li>
 *   <li>消息持久化到数据库</li>
 *   <li>对话历史的修剪和导出</li>
 * </ul>
 *
 * <h2>架构说明</h2>
 * <p>对话上下文采用两层缓存策略：
 * <ul>
 *   <li><b>内存层</b>：使用 ConcurrentHashMap 缓存活跃会话，提供低延迟访问</li>
 *   <li><b>持久层</b>：通过 SessionService 将消息保存到 H2 数据库</li>
 * </ul>
 *
 * <h2>配置项</h2>
 * <ul>
 *   <li>{@code agentscope.model.api-key}：DashScope API Key</li>
 *   <li>{@code agentscope.model.model}：模型名称（如 qwen-plus）</li>
 *   <li>{@code agentscope.model.base-url}：API 基础地址</li>
 *   <li>{@code agentscope.session.max-history-size}：最大历史消息数</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 * @since 1.0
 * @see SessionService
 * @see DashScopeChatModel
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    /**
     * 会话服务，处理会话和消息的持久化.
     */
    private final SessionService sessionService;

    /**
     * JSON 日志格式器，用于导出对话记录.
     */
    private final JsonLogFormatter logFormatter;

    /**
     * DashScope API Key.
     */
    @Value("${agentscope.model.api-key}")
    private String apiKey;

    /**
     * 模型名称.
     */
    @Value("${agentscope.model.model}")
    private String modelName;

    /**
     * API 基础地址.
     */
    @Value("${agentscope.model.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String baseUrl;

    /**
     * 最大历史消息数.
     */
    @Value("${agentscope.session.max-history-size:50}")
    private int maxHistorySize;

    /**
     * DashScope 聊天模型实例.
     */
    private DashScopeChatModel chatModel;

    /**
     * 活跃会话的对话历史缓存.
     *
     * <p>Key: sessionId, Value: 该会话的消息列表
     */
    private final Map<String, List<Msg>> conversationHistories = new ConcurrentHashMap<>();

    /**
     * 默认系统提示词.
     */
    private static final String DEFAULT_SYSTEM_PROMPT =
            "你是一个有帮助的 AI 助手。请用简洁专业的语言回答用户问题。";

    /**
     * 构造函数.
     *
     * @param sessionService 会话服务
     */
    public ChatService(SessionService sessionService) {
        this.sessionService = sessionService;
        this.logFormatter = new JsonLogFormatter(false, false);
    }

    /**
     * 初始化方法 - 创建 DashScope 模型实例.
     *
     * <p>在 Spring 容器启动后，依赖注入完成后执行。
     */
    @PostConstruct
    public void init() {
        // 初始化 DashScope 模型
        this.chatModel = DashScopeChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .baseUrl(baseUrl)
                .stream(false)  // 本章使用同步调用
                .defaultOptions(GenerateOptions.builder()
                        .temperature(0.7)  // 控制输出随机性
                        .maxTokens(2048)   // 最大输出 token 数
                        .build())
                .build();

        log.info("ChatService 初始化完成");
        log.info("模型配置: name={}, baseUrl={}", modelName, baseUrl);
    }

    /**
     * 处理聊天请求.
     *
     * <p>这是对话的主入口方法，完整流程如下：
     * <ol>
     *   <li>确保会话存在（不存在则创建）</li>
     *   <li>获取或初始化对话历史</li>
     *   <li>添加用户消息到历史</li>
     *   <li>调用 AI 模型生成回复</li>
     *   <li>添加助手回复到历史</li>
     *   <li>修剪过长的历史</li>
     *   <li>持久化消息到数据库</li>
     *   <li>构建并返回响应</li>
     * </ol>
     *
     * @param sessionId    会话ID
     * @param userMessage  用户消息内容
     * @param systemPrompt 系统提示词（可选，为空时使用默认值）
     * @return 包含 Agent 回复和会话信息的响应对象
     */
    public ChatResponseDTO chat(String sessionId, String userMessage, String systemPrompt) {
        long startTime = System.currentTimeMillis();

        try {
            // 1. 确保会话存在（不存在则自动创建）
            ChatSession session = sessionService.getOrCreateSession(sessionId, "default_user");

            // 2. 获取对话历史（从内存缓存或数据库加载）
            List<Msg> history = getOrCreateHistory(sessionId, systemPrompt);

            // 3. 构建用户消息并添加到历史
            Msg userMsg = Msg.builder()
                    .role(MsgRole.USER)
                    .textContent(userMessage)
                    .build();
            history.add(userMsg);

            // 4. 调用 AI 模型生成回复
            Msg assistantMsg = callModel(history);

            // 5. 添加助手回复到历史
            history.add(assistantMsg);

            // 6. 修剪过长的历史（保持上下文窗口在限制内）
            trimHistory(sessionId);

            // 7. 持久化消息到数据库
            sessionService.saveMessage(sessionId, userMsg);
            sessionService.saveMessage(sessionId, assistantMsg);

            // 8. 构建响应
            long duration = System.currentTimeMillis() - startTime;
            return buildResponse(session, userMsg, assistantMsg, duration);

        } catch (Exception e) {
            log.error("处理聊天请求失败: sessionId={}, error={}", sessionId, e.getMessage(), e);
            return buildErrorResponse(sessionId, e.getMessage(), startTime);
        }
    }

    /**
     * 调用 AI 模型生成回复.
     *
     * <p>使用 DashScope 模型的流式接口，通过 blockLast 获取完整响应。
     * 超时时间为 60 秒。
     *
     * @param conversationHistory 完整的对话历史（包含系统消息）
     * @return Agent 生成的回复消息
     */
    private Msg callModel(List<Msg> conversationHistory) {
        try {
            // 调用模型，使用 blockLast() 等待完整响应
            ChatResponse response = chatModel.stream(conversationHistory, null, null)
                    .blockLast(Duration.ofSeconds(60));

            if (response != null && response.getContent() != null && !response.getContent().isEmpty()) {
                // 从响应中提取文本内容
                String textContent = extractTextFromContent(response.getContent());
                return Msg.builder()
                        .role(MsgRole.ASSISTANT)
                        .textContent(textContent)
                        .build();
            } else {
                log.warn("模型返回空响应");
                return createErrorMessage("模型返回空响应");
            }
        } catch (Exception e) {
            log.error("调用模型失败: {}", e.getMessage());
            return createErrorMessage("调用失败: " + e.getMessage());
        }
    }

    /**
     * 从 ContentBlock 列表中提取文本内容.
     *
     * <p>遍历所有内容块，筛选出 TextBlock 并拼接文本。
     *
     * @param contentBlocks 内容块列表
     * @return 拼接后的文本内容
     */
    private String extractTextFromContent(List<ContentBlock> contentBlocks) {
        if (contentBlocks == null || contentBlocks.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (ContentBlock block : contentBlocks) {
            if (block instanceof TextBlock textBlock) {
                if (sb.length() > 0) {
                    sb.append("\n");
                }
                sb.append(textBlock.getText());
            }
        }
        return sb.toString();
    }

    /**
     * 获取或创建对话历史.
     *
     * <p>首先检查内存缓存，如果不存在则从数据库加载历史消息，
     * 并添加系统提示词作为第一条消息。
     *
     * @param sessionId 会话ID
     * @param systemPrompt 系统提示词（可选）
     * @return 对话历史列表
     */
    private List<Msg> getOrCreateHistory(String sessionId, String systemPrompt) {
        return conversationHistories.computeIfAbsent(sessionId, k -> {
            List<Msg> history = new ArrayList<>();

            // 从数据库加载历史消息
            List<Msg> dbHistory = sessionService.loadMessages(sessionId);
            if (!dbHistory.isEmpty()) {
                history.addAll(dbHistory);
            }

            // 添加系统消息（如果历史为空或没有系统消息）
            String effectivePrompt = (systemPrompt != null && !systemPrompt.isEmpty())
                    ? systemPrompt
                    : DEFAULT_SYSTEM_PROMPT;
            // 系统消息始终放在第一位
            history.add(0, Msg.builder()
                    .role(MsgRole.SYSTEM)
                    .textContent(effectivePrompt)
                    .build());

            return history;
        });
    }

    /**
     * 修剪过长的对话历史.
     *
     * <p>保留系统消息和最近的 maxHistorySize 条消息，
     * 删除较早的用户/助手消息以控制上下文长度。
     *
     * @param sessionId 会话ID
     */
    private void trimHistory(String sessionId) {
        List<Msg> history = conversationHistories.get(sessionId);
        if (history != null && history.size() > maxHistorySize + 1) {
            // 计算系统消息数量
            int systemMessages = 0;
            for (Msg msg : history) {
                if (msg.getRole() == MsgRole.SYSTEM) {
                    systemMessages++;
                } else {
                    break;  // 系统消息总是在最前面
                }
            }

            // 保留系统消息 + 最近的 maxHistorySize 条消息
            int keepCount = systemMessages + maxHistorySize;
            while (history.size() > keepCount) {
                // 移除最早的非系统消息
                for (int i = systemMessages; i < history.size(); i++) {
                    if (history.get(i).getRole() != MsgRole.SYSTEM) {
                        history.remove(i);
                        break;
                    }
                }
            }
            log.debug("对话历史已修剪: sessionId={}, size={}", sessionId, history.size());
        }
    }

    /**
     * 构建成功响应对象.
     *
     * @param session 会话实体
     * @param userMsg 用户消息
     * @param assistantMsg 助手回复
     * @param durationMs 处理耗时
     * @return 响应 DTO
     */
    private ChatResponseDTO buildResponse(ChatSession session, Msg userMsg,
                                          Msg assistantMsg, long durationMs) {
        // 获取对话历史（过滤掉系统消息）
        List<ChatResponseDTO.MessageInfo> history = conversationHistories.getOrDefault(
                session.getSessionId(), new ArrayList<>()).stream()
                .filter(m -> m.getRole() != MsgRole.SYSTEM)
                .map(this::toMessageInfo)
                .toList();

        return ChatResponseDTO.builder()
                .sessionId(session.getSessionId())
                .messageId(assistantMsg.getId())
                .content(assistantMsg.getTextContent())
                .role(assistantMsg.getRole().name())
                .durationMs(durationMs)
                .history(history)
                .build();
    }

    /**
     * 构建错误响应对象.
     *
     * @param sessionId 会话ID
     * @param errorMessage 错误信息
     * @param startTime 请求开始时间
     * @return 响应 DTO
     */
    private ChatResponseDTO buildErrorResponse(String sessionId, String errorMessage, long startTime) {
        return ChatResponseDTO.builder()
                .sessionId(sessionId)
                .content("处理请求时发生错误: " + errorMessage)
                .role("ASSISTANT")
                .durationMs(System.currentTimeMillis() - startTime)
                .build();
    }

    /**
     * 将 Msg 转换为 MessageInfo DTO.
     *
     * @param msg 消息对象
     * @return MessageInfo DTO
     */
    private ChatResponseDTO.MessageInfo toMessageInfo(Msg msg) {
        return new ChatResponseDTO.MessageInfo(
                msg.getId(),
                msg.getRole().name(),
                msg.getTextContent(),
                msg.getTimestamp()
        );
    }

    /**
     * 创建错误消息.
     *
     * @param errorText 错误描述
     * @return 包含错误信息的助手消息
     */
    private Msg createErrorMessage(String errorText) {
        return Msg.builder()
                .role(MsgRole.ASSISTANT)
                .textContent("抱歉，发生了错误: " + errorText)
                .build();
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取会话的消息历史（从数据库）.
     *
     * @param sessionId 会话ID
     * @return 消息列表
     */
    public List<Msg> getSessionHistory(String sessionId) {
        return sessionService.loadMessages(sessionId);
    }

    /**
     * 清空会话（删除消息但保留会话）.
     *
     * @param sessionId 会话ID
     */
    public void clearSession(String sessionId) {
        sessionService.clearMessages(sessionId);
        conversationHistories.remove(sessionId);  // 同时清除内存缓存
        log.info("会话已清空: sessionId={}", sessionId);
    }

    /**
     * 删除会话（包括会话记录和所有消息）.
     *
     * @param sessionId 会话ID
     */
    public void deleteSession(String sessionId) {
        sessionService.deleteSession(sessionId);
        conversationHistories.remove(sessionId);  // 同时清除内存缓存
        log.info("会话已删除: sessionId={}", sessionId);
    }

    /**
     * 创建新会话.
     *
     * @param userId 用户ID
     * @param title 会话标题
     * @return 创建的会话实体
     */
    public ChatSession createNewSession(String userId, String title) {
        return sessionService.createSession(userId, title);
    }

    /**
     * 重置会话对话历史（仅清除内存缓存）.
     *
     * <p>数据库中的消息保留，下次对话时会重新从数据库加载。
     *
     * @param sessionId 会话ID
     */
    public void resetConversationHistory(String sessionId) {
        conversationHistories.remove(sessionId);
        log.info("对话历史已重置: sessionId={}", sessionId);
    }

    /**
     * 导出对话日志（用于调试）.
     *
     * @param sessionId 会话ID
     * @return 格式化的对话日志字符串
     */
    public String exportConversationLog(String sessionId) {
        List<Msg> history = sessionService.loadMessages(sessionId);
        return logFormatter.formatMessagesToString(history);
    }

    /**
     * 获取当前内存中的对话历史.
     *
     * @param sessionId 会话ID
     * @return 对话历史副本
     */
    public List<Msg> getCurrentHistory(String sessionId) {
        return new ArrayList<>(conversationHistories.getOrDefault(sessionId, new ArrayList<>()));
    }
}