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
package io.agentscope.tutorial.chapter02.service;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.EventType;
import io.agentscope.core.agent.StreamOptions;
import io.agentscope.core.formatter.dashscope.DashScopeChatFormatter;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ThinkingBlock;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.tutorial.chapter02.config.AgentConfig;
import lombok.RequiredArgs构造函数;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 快速开始服务类。
 *
 * <p>提供 Agent 的创建、配置和执行能力。
 *
 * <p>支持的模型类型：
 * <ul>
 *   <li>DashScope（阿里云百炼）- 默认</li>
 *   <li>OpenAI 兼容接口</li>
 *   <li>Ollama 本地模型</li>
 * </ul>
 *
 * @author AgentScope Team
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgs构造函数
public class QuickStartService {

    /**
     * Agent 配置。
     */
    private final AgentConfig agentConfig;

    /**
     * 默认系统提示词。
     */
    private static final String DEFAULT_SYS_PROMPT =
            "你是一个专业、耐心的AI助手。你的任务是：\n"
                    + "1. 准确理解用户的问题\n"
                    + "2. 提供清晰、有条理的回答\n"
                    + "3. 如果不确定，诚实告知用户\n"
                    + "4. 适当使用思考过程来分析问题";

    /**
     * 最大迭代次数。
     */
    private static final int DEFAULT_MAX_ITERATIONS = 10;

    /**
     * 创建 DashScope 模型客户端。
     *
     * <p>根据配置创建并配置 DashScope 模型实例。
     *
     * @return 配置好的 DashScopeChatModel 实例
     */
    private DashScopeChatModel createDashScopeModel() {
        AgentConfig.DashScopeProperties dashscope = agentConfig.getModel().getDashscope();

        // 获取 API Key
        String apiKey = dashscope.getApiKey();
        if (apiKey == null || apiKey.isEmpty() || "your-api-key-here".equals(apiKey)) {
            apiKey = System.getenv("DASHSCOPE_API_KEY");
            if (apiKey == null || apiKey.isEmpty()) {
                log.warn("未配置 DASHSCOPE_API_KEY 环境变量，使用占位符");
                apiKey = "your-api-key-here";
            }
        }

        return DashScopeChatModel.builder()
                .apiKey(apiKey)
                .modelName(dashscope.getModelName())
                .stream(dashscope.isStream())
                .enableThinking(dashscope.isEnableThinking())
                .formatter(new DashScopeChatFormatter())
                .defaultOptions(
                        GenerateOptions.builder()
                                .thinkingBudget(dashscope.getThinkingBudget())
                                .temperature(dashscope.getTemperature())
                                .maxTokens(2048)
                                .build())
                .build();
    }

    /**
     * 创建 ReActAgent 实例。
     *
     * <p>使用配置中的模型和默认设置创建 Agent。
     *
     * @return 配置好的 ReActAgent 实例
     */
    public ReActAgent createAgent() {
        return createAgent("QuickStartAgent", DEFAULT_SYS_PROMPT);
    }

    /**
     * 创建自定义配置的 ReActAgent。
     *
     * @param name      Agent 名称
     * @param sysPrompt 系统提示词
     * @return 配置好的 ReActAgent 实例
     */
    public ReActAgent createAgent(String name, String sysPrompt) {
        // 创建内存组件
        InMemoryMemory memory = new InMemoryMemory();

        // 创建工具包
        Toolkit toolkit = new Toolkit();

        // 创建模型客户端
        var model = createDashScopeModel();

        // 构建 Agent
        return ReActAgent.builder()
                .name(name)
                .sysPrompt(sysPrompt)
                .model(model)
                .memory(memory)
                .toolkit(toolkit)
                .maxIterations(DEFAULT_MAX_ITERATIONS)
                .build();
    }

    /**
     * 发送消息（非流式调用）。
     *
     * @param agent Agent 实例
     * @param message 用户消息
     * @return Agent 响应
     */
    public String sendMessage(Agent agent, String message) {
        log.info("发送消息: {}", message);

        // 构建用户消息
        Msg userMsg = Msg.builder()
                .role(MsgRole.USER)
                .content(TextBlock.builder().text(message).build())
                .build();

        try {
            // 调用 Agent
            Msg response = agent.call(userMsg).block();
            if (response != null) {
                return extractTextContent(response);
            }
            return "无响应";
        } catch (Exception e) {
            log.error("调用 Agent 失败", e);
            return "调用失败: " + e.getMessage();
        }
    }

    /**
     * 发送消息（流式调用）。
     *
     * <p>返回流式响应，支持 SSE。
     *
     * @param agent Agent 实例
     * @param message 用户消息
     * @return 流式响应事件
     */
    public Flux<String> streamMessage(Agent agent, String message) {
        log.info("发送流式消息: {}", message);

        // 构建用户消息
        Msg userMsg = Msg.builder()
                .role(MsgRole.USER)
                .content(TextBlock.builder().text(message).build())
                .build();

        // 配置流式输出选项
        StreamOptions streamOptions = StreamOptions.builder()
                .eventTypes(EventType.REASONING, EventType.TOOL_RESULT, EventType.FINAL)
                .incremental(true)
                .includeReasoningResult(true)
                .build();

        return agent.stream(userMsg, streamOptions)
                .map(event -> {
                    Msg msg = event.getMessage();
                    StringBuilder sb = new StringBuilder();

                    for (ContentBlock block : msg.getContent()) {
                        if (block instanceof ThinkingBlock) {
                            sb.append("> 思考: ").append(((ThinkingBlock) block).getThinking()).append("\n");
                        } else if (block instanceof TextBlock) {
                            sb.append("回答: ").append(((TextBlock) block).getText());
                        }
                    }

                    return sb.toString();
                });
    }

    /**
     * 从消息中提取文本内容。
     *
     * @param msg 消息对象
     * @return 提取的文本内容
     */
    private String extractTextContent(Msg msg) {
        List<String> thinkingList = msg.getContent().stream()
                .filter(block -> block instanceof ThinkingBlock)
                .map(block -> ((ThinkingBlock) block).getThinking())
                .collect(Collectors.toList());

        List<String> textList = msg.getContent().stream()
                .filter(block -> block instanceof TextBlock)
                .map(block -> ((TextBlock) block).getText())
                .collect(Collectors.toList());

        StringBuilder result = new StringBuilder();
        if (!thinkingList.isEmpty()) {
            result.append("> 思考:\n").append(String.join("\n", thinkingList)).append("\n\n");
        }
        if (!textList.isEmpty()) {
            result.append("回答: ").append(String.join("\n", textList));
        }

        return result.length() > 0 ? result.toString() : "[无响应]";
    }
}