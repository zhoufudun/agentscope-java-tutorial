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
package io.agentscope.tutorial.chapter18.quarkus;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * Agent bean for Quarkus - demonstrates CDI (Contexts and Dependency Injection).
 *
 * <p>该类 demonstrates Quarkus/Arc CDI features:
 * <ul>
 *   <li>@ApplicationScoped for application-wide singleton</li>
 *   <li>@Inject for dependency injection</li>
 *   <li>Build-time bean processing (ArC)</li>
 *   <li>构造函数 injection pattern</li>
 * </ul>
 *
 * <p>The ReActAgent is auto-configured by agentscope-quarkus-extension
 * and injected into this bean. This bean wraps the agent with additional
 * business logic and logging.
 *
 * @author AgentScope Tutorial
 */
@Slf4j
@ApplicationScoped
public class AgentBean {

    /** The auto-configured ReActAgent. */
    private final ReActAgent agent;

    /**
     * Constructs AgentBean with injected ReActAgent.
     *
     * <p>Quarkus ArC performs build-time analysis to optimize
     * constructor injection, eliminating reflection overhead.
     *
     * @param agent the auto-configured ReActAgent from AgentScope extension
     */
    @Inject
    public AgentBean(ReActAgent agent) {
        this.agent = agent;
        log.info("=== AgentScope Quarkus Integration ===");
        log.info("Agent initialized: {}", agent.getName());
        log.info("Agent max iterations: {}", agent.getMaxIterations());
    }

    /**
     * 返回 agent instance.
     *
     * @return the ReActAgent
     */
    public ReActAgent getAgent() {
        return agent;
    }

    /**
     * Sends a chat message to the agent and returns the response.
     *
     * @param userMessage the user's message
     * @return the agent's response text
     */
    public String chat(String userMessage) {
        log.info("User: {}", userMessage);

        Msg userMsg = Msg.builder()
                .role(MsgRole.USER)
                .content(TextBlock.builder().text(userMessage).build())
                .build();

        Msg response = agent.call(userMsg).block();
        String responseText = response != null ? response.getTextContent() : "No response";

        log.info("Agent: {}", responseText);
        return responseText;
    }

    /**
     * 返回 agent status information.
     *
     * @return status description
     */
    public String getStatus() {
        return "AgentScope Agent '" + agent.getName() + "' is ready";
    }
}