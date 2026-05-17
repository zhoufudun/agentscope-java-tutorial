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
package io.agentscope.tutorial.chapter01.config;

import io.agentscope.spring.boot.properties.AgentscopeProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Agent 配置类.
 *
 * <p>本类负责从 application.yml 中读取 AgentScope 相关配置，
 * 并创建 Agent 所需的各种组件（Model、Memory、Toolkit）。
 *
 * <p>配置项说明：
 * <ul>
 *   <li>agentscope.model.provider - 模型提供者，默认 dashscope</li>
 *   <li>agentscope.dashscope.api-key - DashScope API Key</li>
 *   <li>agentscope.dashscope.model-name - 模型名称</li>
 *   <li>agentscope.agent.name - Agent 名称</li>
 *   <li>agentscope.agent.sys-prompt - 系统提示词</li>
 * </ul>
 *
 * @see AgentscopeProperties
 */
@Configuration
@Slf4j
public class AgentConfig {

    /**
     * 注册 AgentScope 配置属性类.
     *
     * <p>通过 {@link ConfigurationPropertiesBinding} 将 YAML 配置绑定到
     * {@link AgentscopeProperties} 对象。
     *
     * @return AgentScope 配置属性
     */
    @Bean
    @ConfigurationPropertiesBinding
    public AgentscopeProperties agentscopeProperties() {
        return new AgentscopeProperties();
    }
}