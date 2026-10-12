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

package io.agentscope.tutorial.chapter09;

import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.agentscope.core.a2a.agent.A2aAgent;
import io.agentscope.core.a2a.agent.card.FixedAgentCardResolver;
import io.agentscope.core.a2a.agent.card.WellKnownAgentCardResolver;
import io.agentscope.core.a2a.server.AgentScopeA2aServer;
import io.agentscope.core.a2a.server.card.ConfigurableAgentCard;
import io.agentscope.core.a2a.server.transport.DeploymentProperties;
import io.agentscope.core.a2a.server.transport.TransportProperties;
import io.agentscope.core.a2a.server.transport.jsonrpc.JsonRpcTransportMetadata;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import io.a2a.spec.AgentProvider;
import io.a2a.spec.AgentSkill;
import io.a2a.spec.TransportProtocol;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A2A 协议功能的单元测试
 *
 * <p>这些测试演示并验证 A2A 协议的核心概念：
 * <ul>
 *   <li>AgentCard 创建和配置</li>
 *   <li>AgentCardResolver 使用（Fixed 和 WellKnown）</li>
 *   <li>A2A 消息格式和转换</li>
 *   <li>服务发现机制</li>
 * </ul>
 *
 * <h2>AgentScope 学习要点</h2>
 * <ul>
 *   <li>如何创建和配置 AgentCard</li>
 *   <li>如何使用不同的 AgentCardResolver</li>
 *   <li>如何构建 A2A 消息</li>
 *   <li>如何配置传输属性</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 */
public class A2ATest {

    /**
     * 测试 AgentCard 的创建和配置
     *
     * <p>AgentCard 是 A2A 服务发现的核心元数据文档。
     * 本测试演示如何使用 Builder 模式创建一个完整配置的 AgentCard。
     */
    @Test
    public void testAgentCardCreation() {
        // 使用 Builder 模式创建可配置的 AgentCard
        ConfigurableAgentCard card = new ConfigurableAgentCard.Builder()
                .name("test-agent")                          // Agent 名称
                .description("用于 A2A 协议测试的 Agent")      // 描述
                .url("http://localhost:8888")                // 服务 URL
                .version("1.0.0")                            // 版本号
                .provider(new AgentProvider("Test Provider", "https://test.com"))  // 提供者信息
                .defaultInputModes(List.of("text"))          // 支持的输入模式
                .defaultOutputModes(List.of("text", "streaming"))  // 支持的输出模式
                .skills(buildTestSkills())                   // 技能列表
                .iconUrl("https://test.com/icon.png")        // 图标 URL
                .preferredTransport(TransportProtocol.JSONRPC.asString())  // 首选传输协议
                .build();

        // 验证 AgentCard 属性
        assertEquals("test-agent", card.getName());
        assertEquals("用于 A2A 协议测试的 Agent", card.getDescription());
        assertEquals("http://localhost:8888", card.getUrl());
        assertEquals("1.0.0", card.getVersion());
        assertNotNull(card.getProvider());
        assertEquals("Test Provider", card.getProvider().organization());
        assertNotNull(card.getSkills());
        assertEquals(2, card.getSkills().size());
    }

    /**
     * 测试 FixedAgentCardResolver 的使用
     *
     * <p>FixedAgentCardResolver 用于已有预配置的 AgentCard，
     * 不需要动态发现的场景。适用于测试或已知配置的情况。
     */
    @Test
    public void testFixedAgentCardResolver() {
        // 创建一个模拟的 AgentCard
        // 在 a2a-java-sdk 0.3.x 中，AgentCard record 有 18 个组件，
        // 所以我们使用 Builder 只设置本测试相关的字段
        AgentCard mockCard = new AgentCard.Builder()
                .name("fixed-agent")
                .description("带有固定 Card 的 Agent")
                .url("http://localhost:8888")
                .provider(new AgentProvider("Fixed Provider", "https://fixed.com"))
                .version("1.0.0")
                .capabilities(new AgentCapabilities(false, false, false, List.of()))
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .skills(List.of())
                .build();

        // 创建 Fixed 解析器
        // FixedAgentCardResolver 直接返回预设的 AgentCard
        FixedAgentCardResolver resolver = FixedAgentCardResolver.builder()
                .agentCard(mockCard)
                .build();

        // 从解析器获取 AgentCard
        AgentCard resolvedCard = resolver.getAgentCard("fixed-agent");

        assertNotNull(resolvedCard);
        assertEquals("fixed-agent", resolvedCard.name());
        assertEquals("带有固定 Card 的 Agent", resolvedCard.description());
    }

    /**
     * 测试 WellKnownAgentCardResolver 的配置
     *
     * <p>WellKnownAgentCardResolver 通过 HTTP 从 well-known URI 发现 AgentCard。
     * 这是 A2A 协议的标准服务发现方式。
     */
    @Test
    public void testWellKnownAgentCardResolver() {
        // 创建 well-known 解析器
        // well-known URI 模式：{baseUrl}/.well-known/agent-card.json
        WellKnownAgentCardResolver resolver = WellKnownAgentCardResolver.builder()
                .baseUrl("http://localhost:8888")
                .relativeCardPath("/.well-known/agent-card.json")
                .authHeaders(Map.of())  // 认证头（如需要）
                .build();

        assertNotNull(resolver);

        // 注意：此测试不会实际连接服务器
        // 在真实测试中，你需要 mock HTTP 响应或使用测试服务器
    }

    /**
     * 测试消息的创建和格式化
     *
     * <p>A2A 消息有特定的格式，包括角色、内容块和元数据。
     * 本测试演示如何构建符合 A2A 协议的消息。
     */
    @Test
    public void testMessageCreation() {
        // 创建一个用户消息
        // Msg 没有专门的 context id 字段；会话上下文通过 metadata map 传递
        Msg message = Msg.builder()
                .role(io.agentscope.core.message.MsgRole.USER)  // 设置消息角色
                .content(TextBlock.builder().text("你好，计算 2 + 2").build())  // 设置内容
                .metadata(Map.of("contextId", "test-context-123"))  // 设置元数据
                .build();

        assertNotNull(message);
        assertEquals(io.agentscope.core.message.MsgRole.USER, message.getRole());
        assertNotNull(message.getContent());
        assertEquals("test-context-123", message.getMetadata().get("contextId"));
    }

    /**
     * 测试传输属性的配置
     *
     * <p>传输属性定义了 A2A 服务器如何暴露其端点。
     * JSON-RPC 是默认的传输协议。
     */
    @Test
    public void testTransportProperties() {
        // 构建 JSON-RPC 传输属性
        TransportProperties properties = TransportProperties.builder(
                        TransportProtocol.JSONRPC.asString())
                .host("localhost")
                .port(8888)
                .path("/")
                .extra("metadata", new JsonRpcTransportMetadata())  // 额外元数据
                .build();

        assertEquals("JSONRPC", properties.transportType());
        assertEquals("localhost", properties.host());
        assertEquals(8888, properties.port());
        assertEquals("/", properties.path());
    }

    /**
     * 测试部署属性的配置
     *
     * <p>部署属性用于在 AgentCard 中生成默认的传输接口信息。
     */
    @Test
    public void testDeploymentProperties() {
        // 构建部署属性
        DeploymentProperties properties = new DeploymentProperties.Builder()
                .host("0.0.0.0")    // 监听所有网络接口
                .port(8888)         // 端口号
                .path("/a2a")       // 路径前缀
                .build();

        assertEquals("0.0.0.0", properties.host());
        assertEquals(8888, properties.port());
        assertEquals("/a2a", properties.path());
    }

    /**
     * 测试 AgentCard 技能配置
     *
     * <p>技能以标准化方式定义 Agent 的能力，
     * 允许其他 Agent 发现并利用这些能力。
     */
    @Test
    public void testAgentSkills() {
        List<AgentSkill> skills = buildTestSkills();

        assertNotNull(skills);
        assertEquals(2, skills.size());

        // 验证技能属性
        AgentSkill firstSkill = skills.get(0);
        assertEquals("calculation", firstSkill.id());
        assertEquals("计算", firstSkill.name());
        assertNotNull(firstSkill.description());
    }

    /**
     * 测试 AgentScopeA2aServer Builder 配置
     *
     * <p>服务器 Builder 演示如何配置具有自定义传输和 AgentCard 设置的 A2A 服务器。
     */
    @Test
    public void testServerBuilder() {
        // 构建简单的服务器配置（不含实际 Agent）
        // 此测试验证 Builder 模式是否正常工作

        ConfigurableAgentCard agentCard = new ConfigurableAgentCard.Builder()
                .name("builder-test-agent")
                .description("测试 Server Builder 模式")
                .url("http://localhost:8888")
                .version("1.0.0")
                .build();

        DeploymentProperties deploymentProperties = new DeploymentProperties.Builder()
                .port(8888)
                .build();

        // 验证配置正确
        assertNotNull(agentCard);
        assertEquals("builder-test-agent", agentCard.getName());
        assertNotNull(deploymentProperties);
    }

    /**
     * 测试 A2A 消息元数据处理
     *
     * <p>消息可以包含用于跟踪、认证等目的的元数据。
     */
    @Test
    public void testMessageMetadata() {
        // 创建包含多个元数据字段的消息
        Map<String, Object> metadata = Map.of(
                "userId", "user-123",
                "sessionId", "session-456",
                "traceId", "trace-789"
        );

        Msg message = Msg.builder()
                .role(io.agentscope.core.message.MsgRole.USER)
                .content(TextBlock.builder().text("测试消息").build())
                .metadata(metadata)
                .build();

        assertNotNull(message.getMetadata());
        assertEquals("user-123", message.getMetadata().get("userId"));
        assertEquals("session-456", message.getMetadata().get("sessionId"));
    }

    /**
     * 测试流式响应处理
     *
     * <p>A2A 协议支持使用 Project Reactor 的 Flux 进行流式响应。
     * 适用于需要实时显示响应的场景。
     */
    @Test
    public void testStreamingResponse() {
        // 创建模拟的 Flux 流（模拟流式响应）
        Flux<String> mockStream = Flux.just("第一部分", "第二部分", "第三部分", "[DONE]");

        // 收集所有部分
        StringBuilder collected = new StringBuilder();
        mockStream.subscribe(
                collected::append,
                error -> fail("流错误：" + error.getMessage()),
                () -> assertTrue(collected.toString().contains("[DONE]"))
        );
    }

    /**
     * 构建用于单元测试的测试技能
     *
     * @return 技能列表
     */
    private List<AgentSkill> buildTestSkills() {
        // 在 a2a-java-sdk 0.3.x 中，AgentSkill 通过嵌套 Builder 构造
        // （通过 new AgentSkill.Builder()）；没有静态的 AgentSkill.builder() 工厂方法
        return List.of(
                new AgentSkill.Builder()
                        .id("calculation")
                        .name("计算")
                        .description("数学计算")
                        .tags(List.of("math"))
                        .build(),
                new AgentSkill.Builder()
                        .id("text-chat")
                        .name("文本对话")
                        .description("通用文本对话")
                        .tags(List.of("chat"))
                        .build()
        );
    }
}