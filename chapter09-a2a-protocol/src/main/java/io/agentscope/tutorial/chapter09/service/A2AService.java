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
 * A2A Service for managing the agent lifecycle.
 *
 * <p>This service is responsible for:
 * <ul>
 *   <li>Creating and configuring the ReActAgent</li>
 *   <li>Registering tools with the agent</li>
 *   <li>Providing the agent to the A2A Spring Boot auto-configuration</li>
 * </ul>
 *
 * <p>The A2A service wraps the underlying ReActAgent and provides
 * a Spring-managed lifecycle. The agent is automatically exposed via
 * the A2A protocol when a ReActAgent.Builder or ReActAgent bean is present.
 *
 * <p><b>Key Concepts:</b>
 * <ul>
 *   <li><b>AgentCard</b>: Metadata document for service discovery</li>
 *   <li><b>A2A Client</b>: Agent proxy for calling remote agents</li>
 *   <li><b>Streaming</b>: Real-time response streaming support</li>
 *   <li><b>Task Status</b>: Task state synchronization</li>
 * </ul>
 */
@Service
public class A2AService {

    private static final Logger log = LoggerFactory.getLogger(A2AService.class);

    /**
     * The configured ReActAgent.
     * This bean will be picked up by the A2A auto-configuration.
     */
    private final ReActAgent agent;

    /**
     * 构造函数 with toolkit and tools.
     *
     * @param toolkit The optional toolkit containing agent tools
     */
    @Autowired(required = false)
    public A2AService(Toolkit toolkit) {
        this.agent = createAgent(toolkit);
    }

    /**
     * Initialize the service after construction.
     */
    @PostConstruct
    public void init() {
        log.info("A2A Service initialized with agent: {}", agent.getName());
    }

    /**
     * Create and configure the ReActAgent.
     *
     * <p>The agent is configured with:
     * <ul>
     *   <li>A name for identification</li>
     *   <li>A system prompt defining its role and capabilities</li>
     *   <li>Registered tools from the toolkit</li>
     * </ul>
     *
     * @param toolkit The optional toolkit containing tools
     * @return Configured ReActAgent
     */
    private ReActAgent createAgent(Toolkit toolkit) {
        ReActAgent.Builder builder = ReActAgent.builder()
                .name("a2a-tutorial-agent")
                .sysPrompt(buildSystemPrompt());

        // Register tools if toolkit is available
        if (toolkit != null && !toolkit.getToolNames().isEmpty()) {
            log.info("Registering {} tools from toolkit", toolkit.getToolNames().size());
            builder.toolkit(toolkit);
        } else {
            // Register default tutorial tools
            log.info("Registering default tutorial tools");
            builder.toolkit(create默认Toolkit());
        }

        return builder.build();
    }

    /**
     * Build the system prompt for the tutorial agent.
     */
    private String buildSystemPrompt() {
        return """
                You are a helpful AI assistant powered by AgentScope Java.
                You can help users with various tasks including:

                1. **Text Chat**: General conversation and Q&A
                2. **Calculations**: Mathematical computations (use the calculate tool)
                3. **Weather**: Weather information queries (use the get_weather tool)
                4. **Time**: Current date and time (use the get_current_time tool)

                Guidelines:
                - Be helpful, concise, and accurate in your responses
                - Use tools when appropriate for calculations or data retrieval
                - Provide clear, well-structured responses
                - If you need to perform calculations, use the calculate tool
                """;
    }

    /**
     * Create the default toolkit with tutorial tools.
     */
    private Toolkit create默认Toolkit() {
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(new TutorialTools());
        return toolkit;
    }

    /**
     * Get the configured agent.
     *
     * @return The ReActAgent instance
     */
    public ReActAgent getAgent() {
        return agent;
    }

    /**
     * Get the agent's name.
     *
     * @return The agent name
     */
    public String getAgentName() {
        return agent.getName();
    }
}