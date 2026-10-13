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

package io.agentscope.tutorial.chapter09.service;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.tutorial.chapter09.tools.TutorialTools;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * A2A 服务，管理 Agent 的生命周期
 *
 * <p>本服务负责创建、配置和管理 ReActAgent，并将其提供给
 * A2A Spring Boot 自动配置使用。
 *
 * <h2>核心职责</h2>
 * <ul>
 *   <li>创建和配置 ReActAgent</li>
 *   <li>向 Agent 注册工具</li>
 *   <li>提供 Agent 供 A2A 自动配置使用</li>
 * </ul>
 *
 * <h2>AgentScope 学习要点</h2>
 * <ul>
 *   <li><b>ReActAgent</b>：AgentScope 的核心 Agent 类型，
 *       实现了 ReAct（Reasoning and Acting）模式</li>
 *   <li><b>Toolkit</b>：工具集合，Agent 可以调用其中的工具</li>
 *   <li><b>sysPrompt</b>：系统提示词，定义 Agent 的角色和行为</li>
 *   <li><b>A2A 自动配置</b>：当存在 ReActAgent Bean 时，
 *       自动配置会将其暴露为 A2A 服务</li>
 * </ul>
 *
 * <h2>A2A 核心概念</h2>
 * <ul>
 *   <li><b>AgentCard</b>：用于服务发现的元数据文档</li>
 *   <li><b>A2A Client</b>：调用远程 Agent 的代理</li>
 *   <li><b>Streaming</b>：实时响应流支持</li>
 *   <li><b>Task Status</b>：任务状态同步</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 */
@Service
public class A2AService {

    private static final Logger log = LoggerFactory.getLogger(A2AService.class);

    /**
     * 配置好的 ReActAgent
     *
     * <p>此 Bean 会被 A2A 自动配置获取并暴露为 A2A 服务。
     */
    private final ReActAgent agent;

    /**
     * 构造函数，接收工具集
     *
     * @param toolkit 包含 Agent 工具的可选工具集
     */
    @Autowired(required = false)
    public A2AService(Toolkit toolkit) {
        this.agent = createAgent(toolkit);
    }

    /**
     * Bean 构造后的初始化方法
     */
    @PostConstruct
    public void init() {
        log.info("A2A 服务已初始化，Agent 名称：{}", agent.getName());
    }

    /**
     * 创建和配置 ReActAgent
     *
     * <p>Agent 的配置包括：
     * <ul>
     *   <li>名称：用于标识 Agent</li>
     *   <li>系统提示词：定义 Agent 的角色和能力</li>
     *   <li>工具集：Agent 可以调用的工具</li>
     * </ul>
     *
     * <p>AgentScope 学习要点：
     * <ul>
     *   <li>使用 Builder 模式构建 Agent</li>
     *   <li>系统提示词决定了 Agent 的行为特征</li>
     *   <li>工具扩展了 Agent 的能力边界</li>
     * </ul>
     *
     * @param toolkit 可选的包含工具的工具集
     * @return 配置好的 ReActAgent
     */
    private ReActAgent createAgent(Toolkit toolkit) {
        ReActAgent.Builder builder = ReActAgent.builder()
                .name("a2a-tutorial-agent")
                .sysPrompt(buildSystemPrompt());

        // 如果提供了工具集则注册
        if (toolkit != null && !toolkit.getToolNames().isEmpty()) {
            log.info("从工具集注册 {} 个工具", toolkit.getToolNames().size());
            builder.toolkit(toolkit);
        } else {
            // 注册默认的教程工具
            log.info("注册默认教程工具");
            builder.toolkit(createDefaultToolkit());
        }

        return builder.build();
    }

    /**
     * 构建教程 Agent 的系统提示词
     *
     * <p>系统提示词定义了 Agent 的：
     * <ul>
     *   <li>角色定位</li>
     *   <li>能力范围</li>
     *   <li>行为准则</li>
     * </ul>
     *
     * @return 系统提示词
     */
    private String buildSystemPrompt() {
        return """
                你是一个由 AgentScope Java 驱动的智能 AI 助手。
                你可以帮助用户完成各种任务，包括：

                1. **文本对话**：通用的问答和对话
                2. **计算**：数学计算（使用 calculate 工具）
                3. **天气**：天气信息查询（使用 get_weather 工具）
                4. **时间**：当前日期和时间（使用 get_current_time 工具）

                使用指南：
                - 回复要有帮助、简洁且准确
                - 在适当的时候使用工具进行计算或数据检索
                - 提供清晰、结构化的响应
                - 如果需要进行计算，使用 calculate 工具
                """;
    }

    /**
     * 创建包含教程工具的默认工具集
     *
     * @return 默认工具集
     */
    private Toolkit createDefaultToolkit() {
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(new TutorialTools());
        return toolkit;
    }

    /**
     * 获取配置好的 Agent
     *
     * @return ReActAgent 实例
     */
    public ReActAgent getAgent() {
        return agent;
    }

    /**
     * 获取 Agent 的名称
     *
     * @return Agent 名称
     */
    public String getAgentName() {
        return agent.getName();
    }
}