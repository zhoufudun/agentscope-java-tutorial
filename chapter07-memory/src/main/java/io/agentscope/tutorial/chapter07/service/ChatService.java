package io.agentscope.tutorial.chapter07.service;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 聊天服务
 *
 * <p>处理用户与 AI Agent 之间的对话交互，集成记忆系统实现持久化对话历史。
 *
 * <p><b>核心功能</b>：
 * <ul>
 *   <li><b>消息路由</b> - 将用户消息路由到对应会话的 Agent</li>
 *   <li><b>记忆集成</b> - 自动将所有交互记录到持久化记忆</li>
 *   <li><b>上下文注入</b> - 支持在消息中注入额外系统上下文</li>
 *   <li><b>响应封装</b> - 返回结构化的聊天响应，包含元数据</li>
 * </ul>
 *
 * <p><b>使用示例</b>：
 * <pre>{@code
 * // 基本消息处理
 * ChatResponse response = chatService.processMessage("session-001", "你好");
 *
 * // 带上下文的消息处理
 * ChatResponse response = chatService.processMessageWithContext(
 *     "session-001",
 *     "推荐一些书",
 *     "用户喜欢科幻小说"
 * );
 * }</pre>
 *
 * @author AgentScope Tutorial
 * @since 1.0
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    /** 记忆服务：提供记忆存储和会话 Agent 管理 */
    private final MemoryService memoryService;

    /**
     * 构造函数
     *
     * @param memoryService 记忆服务
     */
    public ChatService(MemoryService memoryService) {
        this.memoryService = memoryService;
    }

    /**
     * 处理用户消息并返回 Agent 回复
     *
     * <p><b>处理流程</b>：
     * <ol>
     *   <li>创建或获取会话对应的 Agent</li>
     *   <li>将用户消息添加到记忆系统</li>
     *   <li>加载最近上下文以增强回复质量</li>
     *   <li>调用 Agent 生成回复</li>
     *   <li>将回复保存到记忆系统</li>
     *   <li>返回包含响应和元数据的结果</li>
     * </ol>
     *
     * <p><b>错误处理</b>：
     * 如果处理过程中发生异常，会返回包含错误信息的 ChatResponse，
     * 而不是抛出异常，以保证 API 的稳定性。
     *
     * @param sessionId 会话标识符
     * @param userMessage 用户消息文本
     * @return ChatResponse 包含 Agent 回复和元数据
     */
    public ChatResponse processMessage(String sessionId, String userMessage) {
        log.info("处理会话 {} 的消息，长度: {}", sessionId, userMessage.length());

        try {
            // 获取或创建会话 Agent
            // 每个会话有独立的 Agent 实例，维护独立的上下文
            ReActAgent agent = memoryService.getOrCreateAgent(sessionId);

            // 构建用户消息对象
            Msg userMsg = Msg.builder()
                    .name("user")
                    .role(MsgRole.USER)
                    .content(TextBlock.builder().text(userMessage).build())
                    .build();

            // 在处理前将用户消息记录到记忆
            // 这确保即使 Agent 调用失败，用户消息也不会丢失
            memoryService.addMessage(sessionId, userMsg);

            // 获取最近的上下文用于增强回复
            // 这些上下文会帮助 Agent 更好地理解对话背景
            List<Msg> recentContext = memoryService.getRecentMessages(sessionId, 10);
            if (!recentContext.isEmpty()) {
                log.debug("使用 {} 条最近消息作为上下文", recentContext.size());
            }

            // 调用 Agent 生成回复
            // 使用 block() 同步等待响应（也可以用响应式方式处理）
            Msg response = agent.call(userMsg).block();

            // 提取并处理响应
            String responseText = "";
            if (response != null) {
                responseText = response.getTextContent();

                // 将助手回复记录到记忆
                memoryService.addMessage(sessionId, response);

                log.info("为会话 {} 生成回复，长度: {}", sessionId, responseText.length());
            } else {
                log.warn("会话 {} 未生成回复", sessionId);
                responseText = "抱歉，我现在无法处理您的请求。请稍后再试。";
            }

            // 获取更新后的记忆统计
            MemoryService.MemoryStats stats = memoryService.getMemoryStats(sessionId);

            // 构建并返回响应
            return new ChatResponse(
                    sessionId,
                    userMessage,
                    responseText,
                    stats,
                    response != null ? response.getChatUsage() : null
            );

        } catch (Exception e) {
            log.error("处理会话 {} 的消息时发生错误: {}", sessionId, e.getMessage(), e);
            // 返回错误响应而非抛出异常，保证 API 稳定性
            return new ChatResponse(
                    sessionId,
                    userMessage,
                    "处理消息时发生错误: " + e.getMessage(),
                    null,
                    null
            );
        }
    }

    /**
     * 带额外上下文处理消息
     *
     * <p>允许在用户消息前注入额外的系统上下文，适用于需要动态添加背景信息的场景。
     *
     * <p><b>应用场景</b>：
     * <ul>
     *   <li>根据用户画像注入个性化偏好</li>
     *   <li>注入当前任务或场景的背景信息</li>
     *   <li>提供额外的约束或指导</li>
     * </ul>
     *
     * @param sessionId 会话标识符
     * @param userMessage 用户消息文本
     * @param systemContext 要注入的系统上下文（可为 null 或空）
     * @return ChatResponse 包含 Agent 回复
     */
    public ChatResponse processMessageWithContext(String sessionId, String userMessage, String systemContext) {
        log.info("处理会话 {} 的带上下文消息", sessionId);

        // 将系统上下文与用户消息合并
        // 上下文放在前面，用户消息标记在后面
        String enhancedMessage = systemContext != null && !systemContext.isBlank()
                ? systemContext + "\n\nUser: " + userMessage
                : userMessage;

        return processMessage(sessionId, enhancedMessage);
    }

    // ==================== 响应记录类 ====================

    /**
     * 聊天响应记录
     *
     * <p>封装 Agent 回复及相关元数据，包括：
     * <ul>
     *   <li>会话标识和原始消息</li>
     *   <li>Agent 生成的回复文本</li>
     *   <li>当前记忆统计信息</li>
     *   <li>模型 Token 使用情况</li>
     * </ul>
     *
     * @param sessionId 会话标识符
     * @param userMessage 原始用户消息
     * @param response Agent 回复文本
     * @param memoryStats 当前记忆统计
     * @param usage Token 使用信息
     */
    public record ChatResponse(
            /** 会话标识符 */
            String sessionId,

            /** 原始用户消息 */
            String userMessage,

            /** Agent 回复文本 */
            String response,

            /** 当前记忆统计信息 */
            MemoryService.MemoryStats memoryStats,

            /** 模型 Token 使用信息 */
            io.agentscope.core.model.ChatUsage usage
    ) {
        /**
         * 返回响应的字符串表示
         *
         * @return 格式化的字符串（响应内容会截断显示）
         */
        @Override
        public String toString() {
            return String.format("ChatResponse{sessionId='%s', response='%s...'}",
                    sessionId,
                    response != null && response.length() > 50
                            ? response.substring(0, 50) + "..."
                            : response);
        }
    }
}