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
package io.agentscope.tutorial.chapter18.spring;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Agent configuration component for Spring Boot integration.
 *
 * <p>该类 demonstrates Spring's dependency injection capabilities
 * with AgentScope. The ReActAgent is auto-configured by the
 * agentscope-spring-boot-starter, and we inject it here to showcase
 * different injection patterns.
 *
 * <p>Key integration points:
 * <ul>
 *   <li>@Component for Spring bean registration</li>
 *   <li>构造函数 injection of ReActAgent</li>
 *   <li>@PostConstruct for initialization logic</li>
 *   <li>构造函数 injection of ApplicationContext for late initialization</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 */
@Component
public class AgentConfig {

    private static final Logger log = LoggerFactory.getLogger(AgentConfig.class);

    /** The auto-configured ReActAgent instance. */
    private final ReActAgent agent;

    /** Spring ApplicationContext for advanced use cases. */
    private final org.springframework.context.ApplicationContext applicationContext;

    /**
     * Constructs AgentConfig with auto-configured ReActAgent and ApplicationContext.
     *
     * <p>构造函数 injection is preferred over field injection as it:
     * <ul>
     *   <li>Makes dependencies explicit</li>
     *   <li>Facilitates testing</li>
     *   <li>Avoids reflection-based field injection</li>
     * </ul>
     *
     * @param agent   the auto-configured ReActAgent from AgentScope starter
     * @param context the Spring ApplicationContext
     */
    public AgentConfig(ReActAgent agent, org.springframework.context.ApplicationContext context) {
        this.agent = agent;
        this.applicationContext = context;
    }

    /**
     * Initialize agent after bean construction.
     */
    @PostConstruct
    public void init() {
        log.info("=== AgentScope Spring Boot Integration ===");
        log.info("Agent initialized: {}", agent.getName());
        log.info("Agent max iterations: {}", agent.getMaxIters());
    }

    /**
     * 返回 configured agent instance.
     *
     * @return the ReActAgent
     */
    public ReActAgent getAgent() {
        return agent;
    }

    /**
     * Demonstrates a simple agent call via Spring-managed bean.
     *
     * @param userMessage the user's message
     * @return the agent's response
     */
    public Msg chat(String userMessage) {
        log.info("User: {}", userMessage);

        Msg userMsg = Msg.builder()
                .role(MsgRole.USER)
                .content(TextBlock.builder().text(userMessage).build())
                .build();

        Msg response = agent.call(List.of(userMsg)).block();
        log.info("Agent: {}", response != null ? response.getTextContent() : "null");

        return response;
    }

    /**
     * Demonstrates batch processing with the agent.
     *
     * @param messages array of user messages
     * @return array of agent responses
     */
    public Msg[] batchChat(String[] messages) {
        Msg[] responses = new Msg[messages.length];
        for (int i = 0; i < messages.length; i++) {
            responses[i] = chat(messages[i]);
        }
        return responses;
    }
}