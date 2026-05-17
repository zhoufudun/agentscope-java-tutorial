package io.agentscope.tutorial.chapter03.service;

import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.tutorial.chapter03.entity.ChatSession;
import io.agentscope.tutorial.chapter03.formatter.JsonLogFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 对话服务 - 整合 Session 和 Agent 的核心服务
 *
 * 核心功能：
 * - 处理用户聊天请求
 * - 管理多轮对话上下文
 * - 调用 AI 模型生成回复
 * - 消息持久化
 *
 * @author AgentScope Tutorial
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final SessionService sessionService;
    private final JsonLogFormatter logFormatter;

    @Value("${agentscope.model.api-key}")
    private String apiKey;

    @Value("${agentscope.model.model}")
    private String modelName;

    @Value("${agentscope.model.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String baseUrl;

    @Value("${agentscope.session.max-history-size:50}")
    private int maxHistorySize;

    /** DashScope 模型实例 */
    private DashScopeChatModel chatModel;

    /** 活跃的对话历史（会话级） */
    private final Map<String, List<Msg>> conversationHistories = new ConcurrentHashMap<>();

    /** 默认系统提示词 */
    private static final String DEFAULT_SYSTEM_PROMPT =
            "你是一个有帮助的 AI 助手。请用简洁专业的语言回答用户问题。";

    public ChatService(SessionService sessionService) {
        this.sessionService = sessionService;
        this.logFormatter = new JsonLogFormatter(false, false);
    }

    @PostConstruct
    public void init() {
        // 初始化 DashScope 模型
        this.chatModel = DashScopeChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .baseUrl(baseUrl)
                .stream(false)
                .defaultOptions(GenerateOptions.builder()
                        .temperature(0.7)
                        .maxTokens(2048)
                        .build())
                .build();

        log.info("ChatService 初始化完成");
        log.info("模型配置: name={}, baseUrl={}", modelName, baseUrl);
    }

    /**
     * 处理聊天请求
     *
     * @param sessionId    会话ID
     * @param userMessage  用户消息
     * @param systemPrompt 系统提示词（可选）
     * @return 对话响应
     */
    public ChatResponseDTO chat(String sessionId, String userMessage, String systemPrompt) {
        long startTime = System.currentTimeMillis();

        try {
            // 1. 确保会话存在
            ChatSession session = sessionService.getOrCreateSession(sessionId, "default_user");

            // 2. 获取对话历史
            List<Msg> history = getOrCreateHistory(sessionId, systemPrompt);

            // 3. 添加用户消息
            Msg userMsg = Msg.builder()
                    .role(MsgRole.USER)
                    .textContent(userMessage)
                    .build();
            history.add(userMsg);

            // 4. 调用模型生成回复
            Msg assistantMsg = callModel(history);

            // 5. 添加助手回复到历史
            history.add(assistantMsg);

            // 6. 修整过长的历史
            trimHistory(sessionId);

            // 7. 保存消息到数据库
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
     * 调用 AI 模型生成回复
     */
    private Msg callModel(List<Msg> conversationHistory) {
        try {
            // 调用模型 (使用 stream().blockFirst() 获取单个响应)
            ChatResponse response = chatModel.stream(conversationHistory, null, null)
                    .blockFirst(60, TimeUnit.SECONDS);

            if (response != null && response.getContent() != null && !response.getContent().isEmpty()) {
                // 从 ContentBlock 列表中提取文本内容
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
     * 从 ContentBlock 列表中提取文本内容
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
     * 获取或创建对话历史
     */
    private List<Msg> getOrCreateHistory(String sessionId, String systemPrompt) {
        return conversationHistories.computeIfAbsent(sessionId, k -> {
            List<Msg> history = new ArrayList<>();

            // 加载数据库中的历史消息
            List<Msg> dbHistory = sessionService.loadMessages(sessionId);
            if (!dbHistory.isEmpty()) {
                history.addAll(dbHistory);
            }

            // 添加系统消息
            String effectivePrompt = (systemPrompt != null && !systemPrompt.isEmpty())
                    ? systemPrompt
                    : DEFAULT_SYSTEM_PROMPT;
            history.add(0, Msg.builder()
                    .role(MsgRole.SYSTEM)
                    .textContent(effectivePrompt)
                    .build());

            return history;
        });
    }

    /**
     * 修整过长的对话历史
     */
    private void trimHistory(String sessionId) {
        List<Msg> history = conversationHistories.get(sessionId);
        if (history != null && history.size() > maxHistorySize + 1) {
            // 保留系统消息 + 最近的 maxHistorySize 条消息
            int systemMessages = 0;
            for (Msg msg : history) {
                if (msg.getRole() == MsgRole.SYSTEM) {
                    systemMessages++;
                } else {
                    break;
                }
            }

            int keepCount = systemMessages + maxHistorySize;
            while (history.size() > keepCount) {
                // 移除最早的 非系统消息
                for (int i = systemMessages; i < history.size(); i++) {
                    if (history.get(i).getRole() != MsgRole.SYSTEM) {
                        history.remove(i);
                        break;
                    }
                }
            }
            log.debug("对话历史已修整: sessionId={}, size={}", sessionId, history.size());
        }
    }

    /**
     * 构建响应对象
     */
    private ChatResponseDTO buildResponse(ChatSession session, Msg userMsg,
                                          Msg assistantMsg, long durationMs) {
        List<ChatResponseDTO.MessageInfo> history = conversationHistories.getOr默认(
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
     * 构建错误响应
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
     * 将 Msg 转换为 MessageInfo
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
     * 创建错误消息
     */
    private Msg createErrorMessage(String errorText) {
        return Msg.builder()
                .role(MsgRole.ASSISTANT)
                .textContent("抱歉，发生了错误: " + errorText)
                .build();
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取会话历史
     */
    public List<Msg> getSessionHistory(String sessionId) {
        return sessionService.loadMessages(sessionId);
    }

    /**
     * 清空会话
     */
    public void clearSession(String sessionId) {
        sessionService.clearMessages(sessionId);
        conversationHistories.remove(sessionId);
        log.info("会话已清空: sessionId={}", sessionId);
    }

    /**
     * 删除会话
     */
    public void deleteSession(String sessionId) {
        sessionService.deleteSession(sessionId);
        conversationHistories.remove(sessionId);
        log.info("会话已删除: sessionId={}", sessionId);
    }

    /**
     * 创建新会话
     */
    public ChatSession createNewSession(String userId, String title) {
        return sessionService.createSession(userId, title);
    }

    /**
     * 重置会话对话历史（内存）
     */
    public void resetConversationHistory(String sessionId) {
        conversationHistories.remove(sessionId);
        log.info("对话历史已重置: sessionId={}", sessionId);
    }

    /**
     * 导出对话日志（用于调试）
     */
    public String exportConversationLog(String sessionId) {
        List<Msg> history = sessionService.loadMessages(sessionId);
        return logFormatter.formatMessagesToString(history);
    }

    /**
     * 获取当前内存中的对话历史
     */
    public List<Msg> getCurrentHistory(String sessionId) {
        return new ArrayList<>(conversationHistories.getOr默认(sessionId, new ArrayList<>()));
    }
}