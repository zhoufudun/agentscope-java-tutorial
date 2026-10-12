package io.agentscope.tutorial.chapter09.config;

import io.agentscope.core.a2a.server.card.ConfigurableAgentCard;
import io.agentscope.core.a2a.server.transport.TransportProperties;
import io.agentscope.tutorial.chapter09.service.A2AService;
import io.a2a.spec.AgentProvider;
import io.a2a.spec.AgentSkill;
import io.a2a.spec.TransportProtocol;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

/**
 * A2A 协议配置类
 *
 * <p>本配置类演示如何在自动配置之外自定义 A2A 服务器。
 *
 * <h2>提供的配置</h2>
 * <ul>
 *   <li><b>AgentCard 配置</b>：自定义 Agent 的元数据</li>
 *   <li><b>部署属性</b>：主机、端口配置</li>
 *   <li><b>传输属性</b>：JSON-RPC 传输配置</li>
 *   <li><b>Agent 元数据</b>：名称、描述、技能等</li>
 * </ul>
 *
 * <h2>A2A 核心概念</h2>
 * <ul>
 *   <li><b>AgentCard</b>：用于服务发现的元数据文档，
 *       在 {@code /.well-known/agent-card.json} 路径提供</li>
 *   <li><b>JSON-RPC 传输</b>：A2A 的默认传输协议</li>
 *   <li><b>Skills（技能）</b>：以标准化方式定义 Agent 的能力</li>
 * </ul>
 *
 * <h2>配置方式</h2>
 * <p>所有设置都可以通过 {@code application.yml} 配置：
 * <pre>{@code
 * a2a:
 *   server:
 *     enabled: true
 *     agent-card:
 *       name: my-agent
 *       description: 我的自定义 Agent
 * }</pre>
 *
 * @author AgentScope Tutorial
 */
@Configuration
public class A2AConfig {

    /**
     * 服务部署端口
     */
    @Value("${server.port:8888}")
    private int serverPort;

    /**
     * 服务部署主机
     */
    @Value("${server.address:localhost}")
    private String serverHost;

    /**
     * 应用上下文路径
     */
    @Value("${server.servlet.context-path:}")
    private String contextPath;

    /**
     * 构建 JSON-RPC 传输属性
     *
     * <p>传输属性定义了 A2A 服务器如何暴露其端点。
     * JSON-RPC 是默认且最常用的传输协议。
     *
     * <p>AgentScope 学习要点：
     * <ul>
     *   <li>TransportProperties 使用 Builder 模式构建</li>
     *   <li>supportTls 控制是否启用 TLS 加密</li>
     *   <li>extra 可以添加自定义元数据</li>
     * </ul>
     *
     * @return JSON-RPC 传输属性配置
     */
    @Bean
    public TransportProperties jsonRpcTransportProperties() {
        return TransportProperties.builder(TransportProtocol.JSONRPC.asString())
                .host(serverHost)
                .port(serverPort)
                .path(normalizePath(contextPath))
                .supportTls(false)  // 演示环境不启用 TLS
                .extra(Map.of("contentType", "application/json"))
                .build();
    }

    /**
     * 构建自定义 AgentCard 属性
     *
     * <p>AgentCard 是 A2A 服务发现的核心元数据文档。
     * 它提供有关 Agent 能力、技能和传输端点的信息。
     *
     * <p>AgentScope 学习要点：
     * <ul>
     *   <li>name：Agent 的唯一标识符</li>
     *   <li>description：描述 Agent 的功能</li>
     *   <li>skills：定义 Agent 支持的技能</li>
     *   <li>provider：提供者信息</li>
     *   <li>defaultInputModes/defaultOutputModes：支持的输入输出模式</li>
     * </ul>
     *
     * @return 可配置的 AgentCard
     */
    @Bean
    public ConfigurableAgentCard configurableAgentCard() {
        return new ConfigurableAgentCard.Builder()
                .name("tutorial-a2a-agent")
                .description("演示 A2A 协议通信的教程 Agent")
                .url("http://" + serverHost + ":" + serverPort + normalizePath(contextPath))
                .provider(new AgentProvider("AgentScope Team", "https://agentscope.io"))
                .version("1.0.0")
                .documentationUrl("http://" + serverHost + ":" + serverPort + normalizePath(contextPath) + "docs")
                .defaultInputModes(List.of("text"))           // 支持文本输入
                .defaultOutputModes(List.of("text", "streaming"))  // 支持文本和流式输出
                .skills(buildSkills())                        // Agent 技能列表
                .iconUrl("https://agentscope.io/icon.png")
                .preferredTransport(TransportProtocol.JSONRPC.asString())  // 首选传输协议
                .build();
    }

    /**
     * 构建此 Agent 支持的技能列表
     *
     * <p>技能以标准化方式定义 Agent 的能力，
     * 允许其他 Agent 发现并利用这些能力。
     *
     * <p>AgentScope 学习要点：
     * <ul>
     *   <li>每个 AgentSkill 包含 id、name、description</li>
     *   <li>技能帮助客户端了解 Agent 能做什么</li>
     *   <li>在服务发现时，客户端可以根据技能选择合适的 Agent</li>
     * </ul>
     *
     * @return 技能列表
     */
    private List<AgentSkill> buildSkills() {
        return List.of(
                new AgentSkill("text-chat", "文本对话", "与 AI 助手进行通用文本对话", null, null, null, null, null),
                new AgentSkill("calculation", "计算", "使用 calculate 工具进行数学计算", null, null, null, null, null),
                new AgentSkill("weather", "天气查询", "使用 get_weather 工具获取城市天气信息", null, null, null, null, null),
                new AgentSkill("time", "时间查询", "使用 get_current_time 工具获取当前日期和时间", null, null, null, null, null)
        );
    }

    /**
     * 规范化路径，确保以 / 开头且不以 / 结尾
     *
     * @param path 原始路径
     * @return 规范化后的路径
     */
    private String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return "/";
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        if (path.endsWith("/") && path.length() > 1) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }
}