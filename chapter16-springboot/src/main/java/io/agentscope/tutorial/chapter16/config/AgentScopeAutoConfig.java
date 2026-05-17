/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.agentscope.tutorial.chapter16.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * AgentScope 自动配置类.
 *
 * <p>通过 agentscope-spring-boot-starter 的自动装配机制，
 * 以下组件会被自动创建：
 * <ul>
 *   <li>AgentscopeProperties - 配置属性绑定</li>
 *   <li>Model - 根据配置的 provider 自动选择 DashScope/OpenAI/Gemini/Claude 模型客户端</li>
 *   <li>Memory - 原型作用域的内存存储</li>
 *   <li>ReActAgent - 原型作用域的 Agent 实例</li>
 *   <li>Toolkit - 工具集（可通过 ToolConfig 类扩展）</li>
 * </ul>
 *
 * <p>配置项说明：
 * <ul>
 *   <li>agentscope.model.provider - 模型提供者（dashscope/openai/gemini/anthropic）</li>
 *   <li>agentscope.dashscope.api-key - DashScope API Key</li>
 *   <li>agentscope.dashscope.model-name - 模型名称</li>
 *   <li>agentscope.agent.name - Agent 名称</li>
 *   <li>agentscope.agent.sys-prompt - 系统提示词</li>
 *   <li>agentscope.agent.max-iters - 最大迭代次数</li>
 * </ul>
 *
 * @see ToolConfig
 */
@Configuration
@Slf4j
public class AgentScopeAutoConfig {
    // 无需手动创建 Bean，由 Starter 自动装配
}