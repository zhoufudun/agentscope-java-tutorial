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
package io.agentscope.tutorial.chapter04;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.hook.Hook;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.ExecutionConfig;
import io.agentscope.core.model.Model;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.tutorial.chapter04.hook.LoggingHook;
import io.agentscope.tutorial.chapter04.tool.CalculatorTool;
import io.agentscope.tutorial.chapter04.tool.DateTimeTool;
import io.agentscope.tutorial.chapter04.tool.WeatherTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ReAct Agent 测试类
 *
 * 测试 ReActAgent 的核心功能，包括：
 * - Agent 创建和配置
 * - 工具调用
 * - Hook 生命周期
 * - 流式响应
 *
 * @author AgentScope Tutorial
 */
@DisplayName("ReActAgent 测试")
@DisabledIfEnvironmentVariable(named = "SKIP_INTEGRATION_TESTS", matches = "true")
class ReActAgentTest {

    private ReActAgent agent;
    private Model model;

    @BeforeEach
    void setUp() {
        // 从环境变量获取 API Key，如果没有则跳过测试
        String apiKey = System.getenv("DASHSCOPE_API_KEY");
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("EMPTY")) {
            apiKey = System.getenv("QWEN_API_KEY");
        }

        // 如果没有有效的 API Key，跳过集成测试
        if (apiKey == null || apiKey.isEmpty()) {
            // 创建一个 mock agent 用于基本测试
            Toolkit toolkit = createToolkit();
            agent = createMockAgent(toolkit);
            return;
        }

        // 创建真实的 Agent
        model = createModel(apiKey);
        Toolkit toolkit = createToolkit();
        Hook loggingHook = new LoggingHook();

        agent = ReActAgent.builder()
                .name("测试助手")
                .sysPrompt("你是一个测试助手。请简洁地回答问题。")
                .model(model)
                .toolkit(toolkit)
                .memory(new InMemoryMemory())
                .maxIters(5)
                .hooks(List.of(loggingHook))
                .build();
    }

    @Test
    @DisplayName("测试 Agent 创建")
    void testAgentCreation() {
        assertNotNull(agent, "Agent 不应该为空");
        assertEquals("测试助手", agent.getName(), "Agent 名称应该正确");
        assertEquals(5, agent.getMaxIters(), "最大迭代次数应该正确");
        assertNotNull(agent.getMemory(), "Memory 不应该为空");
        assertNotNull(agent.getModel(), "Model 不应该为空");
    }

    @Test
    @DisplayName("测试简单对话（无工具调用）")
    void testSimpleConversation() {
        // 这个测试需要有效的 API Key
        if (model == null) {
            System.out.println("跳过需要 API Key 的测试");
            return;
        }

        Msg userMessage = Msg.builder()
                .name("user")
                .role(MsgRole.USER)
                .content(TextBlock.builder().text("你好，请简单介绍一下自己。").build())
                .build();

        // 执行调用
        Msg response = agent.call(userMessage).block(Duration.ofSeconds(60));

        assertNotNull(response, "响应不应该为空");
        String content = response.getContentAsString();
        assertNotNull(content, "响应内容不应该为空");
        assertFalse(content.isEmpty(), "响应内容不应该为空");
        System.out.println("简单对话响应: " + content);
    }

    @Test
    @DisplayName("测试工具调用 - 计算器")
    void testCalculatorTool() {
        if (model == null) {
            System.out.println("跳过需要 API Key 的测试");
            return;
        }

        Msg userMessage = Msg.builder()
                .name("user")
                .role(MsgRole.USER)
                .content(TextBlock.builder().text("请帮我计算 25 * 4 + 10 等于多少？").build())
                .build();

        Msg response = agent.call(userMessage).block(Duration.ofSeconds(120));

        assertNotNull(response, "响应不应该为空");
        String content = response.getContentAsString();
        assertNotNull(content, "响应内容不应该为空");
        System.out.println("计算器响应: " + content);

        // 响应中应该包含计算结果
        assertTrue(content.contains("110") || content.contains("计算"),
                "响应应该包含计算结果");
    }

    @Test
    @DisplayName("测试工具调用 - 天气查询")
    void testWeatherTool() {
        if (model == null) {
            System.out.println("跳过需要 API Key 的测试");
            return;
        }

        Msg userMessage = Msg.builder()
                .name("user")
                .role(MsgRole.USER)
                .content(TextBlock.builder().text("北京现在的天气怎么样？").build())
                .build();

        Msg response = agent.call(userMessage).block(Duration.ofSeconds(120));

        assertNotNull(response, "响应不应该为空");
        String content = response.getContentAsString();
        assertNotNull(content, "响应内容不应该为空");
        System.out.println("天气查询响应: " + content);

        // 响应中应该包含天气相关信息
        assertTrue(content.contains("北京") || content.contains("天气") || content.contains("温度"),
                "响应应该包含天气相关信息");
    }

    @Test
    @DisplayName("测试工具调用 - 时间查询")
    void testDateTimeTool() {
        if (model == null) {
            System.out.println("跳过需要 API Key 的测试");
            return;
        }

        Msg userMessage = Msg.builder()
                .name("user")
                .role(MsgRole.USER)
                .content(TextBlock.builder().text("现在是什么时间？请用 Asia/Shanghai 时区告诉我。").build())
                .build();

        Msg response = agent.call(userMessage).block(Duration.ofSeconds(120));

        assertNotNull(response, "响应不应该为空");
        String content = response.getContentAsString();
        assertNotNull(content, "响应内容不应该为空");
        System.out.println("时间查询响应: " + content);
    }

    @Test
    @DisplayName("测试工具调用 - 单位换算")
    void testUnitConversion() {
        if (model == null) {
            System.out.println("跳过需要 API Key 的测试");
            return;
        }

        Msg userMessage = Msg.builder()
                .name("user")
                .role(MsgRole.USER)
                .content(TextBlock.builder().text("请帮我把 100 华氏度转换成摄氏度。").build())
                .build();

        Msg response = agent.call(userMessage).block(Duration.ofSeconds(120));

        assertNotNull(response, "响应不应该为空");
        String content = response.getContentAsString();
        assertNotNull(content, "响应内容不应该为空");
        System.out.println("单位换算响应: " + content);
    }

    @Test
    @DisplayName("测试对话历史管理")
    void testMemoryManagement() {
        if (model == null) {
            System.out.println("跳过需要 API Key 的测试");
            return;
        }

        // 第一轮对话
        Msg message1 = Msg.builder()
                .name("user")
                .role(MsgRole.USER)
                .content(TextBlock.builder().text("请告诉我当前时间。").build())
                .build();

        agent.call(message1).block(Duration.ofSeconds(120));

        int messageCountAfterFirst = agent.getMemory().getMessages().size();
        assertTrue(messageCountAfterFirst > 0, "第一轮对话后应该有消息");

        System.out.println("第一轮对话后消息数: " + messageCountAfterFirst);

        // 第二轮对话
        Msg message2 = Msg.builder()
                .name("user")
                .role(MsgRole.USER)
                .content(TextBlock.builder().text("请计算 5 + 3 等于多少？").build())
                .build();

        agent.call(message2).block(Duration.ofSeconds(120));

        int messageCountAfterSecond = agent.getMemory().getMessages().size();
        assertTrue(messageCountAfterSecond > messageCountAfterFirst, "第二轮对话后消息数应该增加");

        System.out.println("第二轮对话后消息数: " + messageCountAfterSecond);
    }

    @Test
    @DisplayName("测试最大迭代次数限制")
    void testMaxIterations() {
        if (model == null) {
            System.out.println("跳过需要 API Key 的测试");
            return;
        }

        // 创建一个最大迭代次数为 2 的 Agent
        ReActAgent limitedAgent = ReActAgent.builder()
                .name("限制助手")
                .sysPrompt("当你被问到计算问题时，必须连续调用 3 次计算器工具。")
                .model(model)
                .toolkit(createToolkit())
                .memory(new InMemoryMemory())
                .maxIters(2)  // 设置很小的迭代次数
                .build();

        Msg userMessage = Msg.builder()
                .name("user")
                .role(MsgRole.USER)
                .content(TextBlock.builder().text("请帮我计算 1 + 1, 然后再 + 2, 最后 + 3。").build())
                .build();

        // 即使问题需要多步计算，也应该在 2 次迭代后结束
        Msg response = limitedAgent.call(userMessage).block(Duration.ofSeconds(120));

        assertNotNull(response, "响应不应该为空");
        String content = response.getContentAsString();
        System.out.println("限制迭代次数响应: " + content);
    }

    // ========== 辅助方法 ==========

    private Model createModel(String apiKey) {
        return io.agentscope.core.model.DashScopeChatModel.builder()
                .apiKey(apiKey)
                .modelName(System.getenv("MODEL_NAME") != null ?
                        System.getenv("MODEL_NAME") : "qwen-plus")
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                .stream(true)
                .build();
    }

    private Toolkit createToolkit() {
        Toolkit toolkit = new Toolkit();
        toolkit.registerObject(new WeatherTool());
        toolkit.registerObject(new CalculatorTool());
        toolkit.registerObject(new DateTimeTool());
        toolkit.setDefaultExecutionConfig(
                ExecutionConfig.builder()
                        .timeout(Duration.ofSeconds(30))
                        .maxRetries(1)
                        .build()
        );
        return toolkit;
    }

    private ReActAgent createMockAgent(Toolkit toolkit) {
        // 创建一个简单的 mock Agent 用于基本测试
        return ReActAgent.builder()
                .name("MockAgent")
                .sysPrompt("Mock agent for testing")
                .memory(new InMemoryMemory())
                .maxIters(5)
                .build();
    }
}