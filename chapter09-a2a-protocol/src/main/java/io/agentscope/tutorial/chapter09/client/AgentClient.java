package io.agentscope.tutorial.chapter09.client;

import io.agentscope.core.a2a.agent.A2aAgent;
import io.agentscope.core.a2a.agent.card.AgentCardResolver;
import io.agentscope.core.a2a.agent.card.WellKnownAgentCardResolver;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.UUID;

/**
 * A2A 客户端，用于与远程 Agent 通信
 *
 * <p>本类演示如何创建 A2A 客户端来调用远程 Agent。
 * A2A 客户端通过 AgentCard 发现远程 Agent 的能力和端点信息。
 *
 * <h2>核心功能</h2>
 * <ul>
 *   <li><b>AgentCard 发现</b>：使用 well-known URI 自动发现远程 Agent</li>
 *   <li><b>消息发送</b>：向远程 Agent 发送消息并获取响应</li>
 *   <li><b>流式响应</b>：支持实时流式获取响应内容</li>
 *   <li><b>任务事件处理</b>：处理任务状态更新</li>
 * </ul>
 *
 * <h2>AgentScope 学习要点</h2>
 * <ul>
 *   <li><b>A2aAgent</b>：A2A 协议的客户端代理，封装了远程调用逻辑</li>
 *   <li><b>AgentCardResolver</b>：负责解析和获取 AgentCard</li>
 *   <li><b>WellKnownAgentCardResolver</b>：从标准 well-known URI 获取 AgentCard</li>
 *   <li><b>Flux</b>：使用 Reactor 的 Flux 处理流式响应</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 */
@Component
public class AgentClient {

    private static final Logger log = LoggerFactory.getLogger(AgentClient.class);

    /**
     * AgentCard 发现的基础 URL
     */
    @Value("${a2a.client.base-url:http://localhost:8888}")
    private String baseUrl;

    /**
     * AgentCard 的 well-known 路径
     */
    @Value("${a2a.client.agent-card-path:/.well-known/agent-card.json}")
    private String agentCardPath;

    /**
     * 远程 Agent 名称
     */
    @Value("${a2a.client.agent-name:tutorial-a2a-agent}")
    private String agentName;

    /**
     * 用于远程通信的 A2A Agent 代理
     */
    private A2aAgent a2aAgent;

    /**
     * 初始化 A2A 客户端
     *
     * <p>此方法在 Bean 构造后自动调用，创建配置好的 A2aAgent 实例，
     * 用于通过 A2A 协议与远程 Agent 通信。
     *
     * <p>初始化流程：
     * <ol>
     *   <li>创建 AgentCardResolver，配置 well-known URI</li>
     *   <li>使用 Builder 模式构建 A2aAgent</li>
     *   <li>Agent 会自动获取 AgentCard 并解析端点信息</li>
     * </ol>
     */
    @jakarta.annotation.PostConstruct
    public void init() {
        try {
            // 创建 AgentCard 解析器，使用 well-known URI 模式
            // well-known URI 是 A2A 协议的标准服务发现机制
            AgentCardResolver agentCardResolver = WellKnownAgentCardResolver.builder()
                    .baseUrl(baseUrl)                    // 服务基础 URL
                    .relativeCardPath(agentCardPath)     // AgentCard 相对路径
                    .authHeaders(Map.of())               // 认证头（如需要）
                    .build();

            // 构建 A2A Agent 客户端
            // A2aAgent 封装了所有 A2A 协议的通信细节
            this.a2aAgent = A2aAgent.builder()
                    .name(agentName)                     // Agent 名称
                    .agentCardResolver(agentCardResolver) // AgentCard 解析器
                    .build();

            log.info("A2A 客户端初始化成功");
            log.info("目标 Agent：{}", agentName);
            log.info("AgentCard URL：{}{}", baseUrl, agentCardPath);

        } catch (Exception e) {
            log.warn("A2A 客户端初始化失败：{}。如果远程 Agent 不可用，这是正常的。", e.getMessage());
        }
    }

    /**
     * 向远程 Agent 发送消息并获取同步响应
     *
     * <p>此方法会阻塞直到收到响应。如需流式响应，请使用 {@link #streamChat(Msg)}。
     *
     * @param message 要发送的消息
     * @return 远程 Agent 的响应消息
     */
    public Msg chat(Msg message) {
        log.info("向远程 Agent 发送消息：{}", message.getContent());
        // block() 会阻塞当前线程直到响应返回
        return a2aAgent.call(message).block();
    }

    /**
     * 向远程 Agent 发送消息（带上下文 ID）
     *
     * @param content 消息内容
     * @param contextId 会话跟踪的上下文 ID
     * @return 远程 Agent 的响应消息
     */
    public Msg chat(String content, String contextId) {
        // 构建消息对象
        Msg message = Msg.builder()
                .role(MsgRole.USER)  // 设置消息角色为用户
                .content(TextBlock.builder().text(content).build())  // 设置文本内容
                .build();
        return chat(message);
    }

    /**
     * 以流式方式从远程 Agent 获取响应
     *
     * <p>使用 Reactor 的 Flux 实现响应式流处理，适用于：
     * <ul>
     *   <li>长时间运行的任务</li>
     *   <li>需要实时显示进度的场景</li>
     *   <li>SSE（Server-Sent Events）推送</li>
     * </ul>
     *
     * @param message 要发送的消息
     * @return 响应文本块的 Flux 流
     */
    public Flux<String> streamChat(Msg message) {
        log.info("开始与远程 Agent 的流式对话");

        return a2aAgent.stream(message)
                .map(event -> {
                    // 检查是否是最后一个事件
                    if (event.isLast()) {
                        return "";
                    }

                    // 从事件中提取响应消息
                    Msg responseMessage = event.getMessage();
                    if (responseMessage == null) {
                        return "";
                    }

                    // 提取所有 TextBlock 的文本内容
                    StringBuilder text = new StringBuilder();
                    responseMessage.getContent().stream()
                            .filter(block -> block instanceof TextBlock)
                            .map(block -> (TextBlock) block)
                            .forEach(block -> text.append(block.getText()));

                    return text.toString();
                });
    }

    /**
     * 以流式方式发送消息（带上下文 ID）
     *
     * @param content 消息内容
     * @param contextId 会话跟踪的上下文 ID
     * @return 响应文本块的 Flux 流
     */
    public Flux<String> streamChat(String content, String contextId) {
        Msg message = Msg.builder()
                .role(MsgRole.USER)
                .content(TextBlock.builder().text(content).build())
                .build();
        return streamChat(message);
    }

    /**
     * 检查 A2A 客户端是否就绪
     *
     * @return 如果客户端已初始化且就绪则返回 true
     */
    public boolean isReady() {
        return a2aAgent != null;
    }

    /**
     * 获取远程 Agent 的名称
     *
     * @return Agent 名称
     */
    public String getAgentName() {
        return agentName;
    }

    /**
     * 获取配置的基础 URL
     *
     * @return 基础 URL
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * 创建新的会话上下文 ID
     *
     * @return 新的上下文 ID（UUID 格式）
     */
    public String createContextId() {
        return UUID.randomUUID().toString();
    }
}