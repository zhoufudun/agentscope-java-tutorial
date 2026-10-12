package io.agentscope.tutorial.chapter07.tool;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import io.agentscope.tutorial.chapter07.service.MemoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 记忆管理工具类
 *
 * <p>为 AI Agent 提供记忆管理工具。这些工具让 Agent 能够主动管理对话记忆：
 * 记住重要信息、搜索历史对话、获取最近记忆。
 *
 * <p><b>可用工具</b>：
 * <ul>
 *   <li>{@link #remember(String)} - 将重要信息存储到记忆</li>
 *   <li>{@link #recall(String)} - 搜索记忆中的信息</li>
 *   <li>{@link #getRecent(int)} - 获取最近 N 条记忆</li>
 *   <li>{@link #getMemoryStats()} - 获取记忆统计信息</li>
 * </ul>
 *
 * <p><b>Agent 使用示例</b>：
 * <pre>{@code
 * // Agent 记住用户偏好
 * remember("用户偏好深色主题")
 *
 * // Agent 搜索相关信息
 * recall("深色主题")
 *
 * // Agent 获取最近对话
 * getRecent(5)
 * }</pre>
 *
 * <p><b>与 AgentScope 的集成</b>：
 * 使用 {@link Tool} 和 {@link ToolParam} 注解定义工具元数据，
 * AgentScope 会自动将这些工具注册到 Agent 的 Toolkit 中。
 *
 * @author AgentScope Tutorial
 * @since 1.0
 */
public class MemoryManagementTool {

    private static final Logger log = LoggerFactory.getLogger(MemoryManagementTool.class);

    /** 记忆服务：提供记忆存储和检索功能 */
    private final MemoryService memoryService;

    /**
     * 当前会话上下文
     *
     * <p>通常由 Agent 运行时设置。所有工具操作都在此会话的记忆空间中进行。
     */
    private String currentSessionId = "default";

    /**
     * 构造函数
     *
     * @param memoryService 记忆服务
     */
    public MemoryManagementTool(MemoryService memoryService) {
        this.memoryService = memoryService;
    }

    /**
     * 设置当前会话上下文
     *
     * <p>后续的记忆操作都将在此会话的记忆空间中进行。
     *
     * @param sessionId 会话标识符
     */
    public void setSessionId(String sessionId) {
        this.currentSessionId = sessionId;
    }

    /**
     * 获取当前会话 ID
     *
     * @return 当前会话标识符
     */
    public String getSessionId() {
        return currentSessionId;
    }

    // ==================== 工具方法 ====================

    /**
     * 将重要信息存储到记忆
     *
     * <p>使用此工具记住用户偏好、重要事实或任何需要跨对话保留的信息。
     *
     * <p><b>适用场景</b>：
     * <ul>
     *   <li>用户偏好（主题、语言、姓名）</li>
     *   <li>对话中提到的重要事实</li>
     *   <li>达成的决定或协议</li>
     *   <li>需要长期记住的上下文</li>
     * </ul>
     *
     * <p><b>最佳实践</b>：
     * 内容应该具体、清晰，以便后续能够准确检索。
     *
     * @param content 要记住的信息（应具体清晰）
     * @return 确认消息，说明记住了什么
     */
    @Tool(
            name = "remember",
            description = """
                将重要信息存储到 Agent 的记忆中，以便跨对话持久访问。

                使用场景：
                - 用户分享姓名、偏好或个人详情时
                - 提到重要事实或决定时
                - 希望为未来交互记住上下文时
                - 用户明确要求记住某些内容时

                内容应清晰具体，以便后续能够轻松检索。

                示例：
                - "用户名叫张三，偏好深色主题"
                - "客户订单号是 12345，包含咖啡和松饼"
                - "重要：这个项目的截止日期是下周五"
                """
    )
    public String remember(
            @ToolParam(
                    name = "content",
                    description = "要记住的信息。应具体清晰，包含相关细节如姓名、日期、偏好等。",
                    required = true
            )
            String content
    ) {
        log.info("remember 工具被调用，内容: {}", content);

        try {
            // 创建一个系统消息来存储记忆内容
            // 使用 [记忆] 前缀标记这是一条主动记忆的信息
            io.agentscope.core.message.Msg memoryMsg = io.agentscope.core.message.Msg.builder()
                    .name("agent-memory")
                    .role(io.agentscope.core.message.MsgRole.SYSTEM)
                    .content(io.agentscope.core.message.TextBlock.builder()
                            .text("[记忆] " + content)
                            .build())
                    .build();

            // 添加到记忆服务
            memoryService.addMessage(currentSessionId, memoryMsg);

            return "已成功记住: " + content;

        } catch (Exception e) {
            log.error("remember 工具执行出错: {}", e.getMessage(), e);
            return "记忆失败: " + e.getMessage();
        }
    }

    /**
     * 搜索记忆中的信息
     *
     * <p>使用此工具查找之前存储在记忆中的信息。
     * 搜索不区分大小写，支持部分关键词匹配。
     *
     * <p><b>适用场景</b>：
     * <ul>
     *   <li>用户询问之前提到的内容</li>
     *   <li>需要回忆用户偏好</li>
     *   <li>检查是否讨论过某些信息</li>
     *   <li>验证过去对话中的事实或决定</li>
     * </ul>
     *
     * @param keyword 搜索关键词（不区分大小写）
     * @return 搜索结果，包含匹配的记忆；或"未找到"消息
     */
    @Tool(
            name = "recall",
            description = """
                使用关键词在 Agent 的记忆中搜索相关信息。

                使用场景：
                - 用户询问之前提到的内容
                - 需要回忆用户偏好或个人详情
                - 验证过去对话中的信息
                - 用户说"你还记得..."、"我之前告诉你的..."

                搜索不区分大小写，支持部分关键词匹配。
                返回所有匹配的记忆及其内容。
                """
    )
    public String recall(
            @ToolParam(
                    name = "keyword",
                    description = "在记忆中搜索的关键词。不区分大小写，部分匹配。",
                    required = true
            )
            String keyword
    ) {
        log.info("recall 工具被调用，关键词: {}", keyword);

        try {
            // 参数校验
            if (keyword == null || keyword.isBlank()) {
                return "搜索关键词不能为空";
            }

            // 执行搜索
            List<io.agentscope.core.message.Msg> results = memoryService.searchMessages(currentSessionId, keyword);

            if (results.isEmpty()) {
                return "没有找到与 '" + keyword + "' 相关的记忆";
            }

            // 格式化搜索结果
            String formattedResults = results.stream()
                    .map(m -> "- " + m.getTextContent())
                    .collect(Collectors.joining("\n"));

            return "找到 " + results.size() + " 条相关记忆:\n" + formattedResults;

        } catch (Exception e) {
            log.error("recall 工具执行出错: {}", e.getMessage(), e);
            return "搜索失败: " + e.getMessage();
        }
    }

    /**
     * 获取最近的 N 条记忆
     *
     * <p>使用此工具获取最近对话的上下文，用于了解刚刚讨论的内容。
     *
     * <p><b>适用场景</b>：
     * <ul>
     *   <li>新交互开始时获取上下文</li>
     *   <li>回顾最近的对话流程</li>
     *   <li>查找最近的用户偏好</li>
     *   <li>发生错误后检查最近历史进行恢复</li>
     * </ul>
     *
     * <p><b>推荐数量</b>：
     * <ul>
     *   <li>3-5：快速了解当前话题</li>
     *   <li>10：获取更广泛的对话历史</li>
     *   <li>20：全面回顾记忆</li>
     * </ul>
     *
     * @param count 要获取的最近记忆数量（建议 1-20）
     * @return 格式化的最近记忆列表，包含时间戳
     */
    @Tool(
            name = "getRecent",
            description = """
                获取对话历史中最近的 N 条记忆。

                使用场景：
                - 开始新话题时想回忆最近上下文
                - 需要回顾刚才讨论的内容
                - 用户问"我们刚才说了什么"等类似问题
                - 想基于最近历史提供个性化回复

                推荐数量：
                - 3-5：快速了解当前话题
                - 10：获取更广泛的对话历史
                - 20：全面回顾记忆
                """
    )
    public String getRecent(
            @ToolParam(
                    name = "count",
                    description = "要获取的最近记忆数量（推荐: 3-20）",
                    required = true
            )
            int count
    ) {
        log.info("getRecent 工具被调用，数量: {}", count);

        try {
            // 限制数量在合理范围内
            int effectiveCount = Math.max(1, Math.min(count, 20));

            List<io.agentscope.core.message.Msg> results =
                    memoryService.getRecentMessages(currentSessionId, effectiveCount);

            if (results.isEmpty()) {
                return "没有最近的记忆";
            }

            // 格式化结果（按时间顺序显示，最旧在前）
            StringBuilder sb = new StringBuilder();
            sb.append("最近 ").append(results.size()).append(" 条记忆:\n");

            // 反转列表以按时间顺序显示（便于阅读）
            List<io.agentscope.core.message.Msg> reversed = new java.util.ArrayList<>(results);
            java.util.Collections.reverse(reversed);

            for (int i = 0; i < reversed.size(); i++) {
                io.agentscope.core.message.Msg msg = reversed.get(i);
                sb.append(i + 1)
                        .append(". [")
                        .append(msg.getRole())
                        .append("] ")
                        .append(msg.getTextContent())
                        .append("\n");
            }

            return sb.toString();

        } catch (Exception e) {
            log.error("getRecent 工具执行出错: {}", e.getMessage(), e);
            return "获取失败: " + e.getMessage();
        }
    }

    /**
     * 获取当前会话的记忆统计信息
     *
     * <p>这是一个实用工具，用于检查记忆使用情况。
     * 可以帮助了解当前有多少上下文可用。
     *
     * @return 格式化的会话记忆统计信息
     */
    @Tool(
            name = "getMemoryStats",
            description = """
                获取当前会话的记忆使用统计信息。

                返回内容：
                - 存储的总消息数
                - 用户消息数
                - 助手消息数
                - 当前内存缓存大小

                适用于调试或想了解有多少上下文可用时使用。
                """
    )
    public String getMemoryStats() {
        log.info("getMemoryStats 工具被调用");

        try {
            MemoryService.MemoryStats stats = memoryService.getMemoryStats(currentSessionId);

            return String.format("""
                    记忆统计:
                    - 总消息数: %d
                    - 用户消息: %d
                    - 助手消息: %d
                    - 缓存大小: %d
                    """,
                    stats.totalMessages(),
                    stats.userMessageCount(),
                    stats.assistantMessageCount(),
                    stats.cachedCount());

        } catch (Exception e) {
            log.error("getMemoryStats 工具执行出错: {}", e.getMessage(), e);
            return "获取统计失败: " + e.getMessage();
        }
    }
}
