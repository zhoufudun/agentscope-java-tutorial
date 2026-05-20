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
package io.agentscope.tutorial.chapter18.micronaut;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

/**
 * Agent factory for Micronaut - demonstrates factory pattern for complex bean creation.
 *
 * <p>While the agentscope-micronaut-extension provides auto-configuration for ReActAgent,
 * this factory demonstrates how to create custom agent beans with additional configuration.
 *
 * <p>Key Micronaut features demonstrated:
 * <ul>
 *   <li>@Factory for custom bean creation</li>
 *   <li>@Singleton scoped beans</li>
 *   <li>构造函数 injection</li>
 *   <li>PostConstruct-style initialization via method injection</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 */
@Slf4j
@Factory
public class AgentFactory {

    /**
     * Creates a custom configured ReActAgent bean.
     *
     * <p>This factory method demonstrates how to:
     * <ul>
     *   <li>Inject configuration properties</li>
     *   <li>Create agents with custom settings</li>
     *   <li>Add initialization logic</li>
     * </ul>
     *
     * <p>Note: In this example we delegate to the auto-configured agent
     * since agentscope-micronaut-extension already handles agent creation.
     * This factory pattern is useful when you need custom agent configurations
     * or multiple agent instances with different settings.
     *
     * @param autoConfiguredAgent the auto-configured agent from AgentScope extension
     * @return a ReActAgent ready to use
     */
    @Singleton
    public ReActAgent customAgent(ReActAgent autoConfiguredAgent) {
        log.info("Creating custom agent configuration");
        log.info("Base agent name: {}", autoConfiguredAgent.getName());
        log.info("Base agent max iterations: {}", autoConfiguredAgent.getMaxIters());

        // You can customize the agent here if needed
        // For example, wrap it with additional logic, add interceptors, etc.

        return autoConfiguredAgent;
    }

    /**
     * Creates a chat service that wraps the agent.
     *
     * <p>This demonstrates how to create service beans that depend on the agent,
     * providing a cleaner API for business logic.
     *
     * @param agent the injected ReActAgent
     * @return ChatService instance
     */
    @Singleton
    public ChatService chatService(ReActAgent agent) {
        return new ChatService(agent);
    }

    /**
     * Chat service wrapper around ReActAgent.
     *
     * <p>Provides a simpler interface for chat operations.
     */
    public static class ChatService {
        private final ReActAgent agent;

        public ChatService(ReActAgent agent) {
            this.agent = agent;
        }

        /**
         * Sends a message to the agent and returns the response.
         *
         * @param message the user's message
         * @return the agent's response text
         */
        public String chat(String message) {
            Msg userMsg = Msg.builder()
                    .role(MsgRole.USER)
                    .content(TextBlock.builder().text(message).build())
                    .build();

            Msg response = agent.call(userMsg).block();
            return response != null ? response.getTextContent() : "No response";
        }

        /**
         * 返回 agent name.
         *
         * @return agent name
         */
        public String getAgentName() {
            return agent.getName();
        }
    }
}