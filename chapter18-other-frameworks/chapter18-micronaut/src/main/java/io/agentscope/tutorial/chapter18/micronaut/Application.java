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
import io.micronaut.context.ApplicationContext;

/**
 * AgentScope Java Tutorial - Chapter 18: Micronaut Integration Example.
 *
 * <p>This application demonstrates how to integrate AgentScope with Micronaut.
 * It uses the agentscope-micronaut-extension for compile-time dependency injection
 * and demonstrates the lightweight nature of Micronaut.
 *
 * <p>Key features:
 * <ul>
 *   <li>Compile-time DI (no runtime reflection overhead)</li>
 *   <li>YAML-based configuration</li>
 *   <li>Low memory footprint and fast startup</li>
 *   <li>Non-blocking HTTP server via Netty</li>
 * </ul>
 *
 * <p>Run with:
 * <pre>
 * DASHSCOPE_API_KEY=your-key mvn compile exec:java
 * </pre>
 *
 * @author AgentScope Tutorial
 */
public class Application {

    public static void main(String[] args) {
        System.out.println("\n=== AgentScope Micronaut Example ===\n");
        System.out.println("Framework: Micronaut (compile-time DI)");
        System.out.println("Key advantage: Fast startup, low memory footprint\n");

        // Start Micronaut ApplicationContext
        // Micronaut builds dependency injection at compile time,
        // resulting in faster startup and lower memory usage
        try (ApplicationContext context = ApplicationContext.run()) {
            // Get ReActAgent from Micronaut DI container
            // The agent is auto-configured via application.yml by agentscope-micronaut-extension
            ReActAgent agent = context.getBean(ReActAgent.class);

            System.out.println("✓ Agent created via Micronaut dependency injection");
            System.out.println("✓ Configuration loaded from application.yml");
            System.out.println("✓ Agent name: " + agent.getName());
            System.out.println("✓ Agent max iterations: " + agent.getMaxIterations());
            System.out.println();

            // Demonstrate chat with the agent
            String[] questions = {
                "Hello, who are you?",
                "What are the advantages of Micronaut?",
                "Thank you, goodbye!"
            };

            for (String question : questions) {
                System.out.println("User: " + question);

                // Build user message
                Msg userMsg = Msg.builder()
                        .role(MsgRole.USER)
                        .content(TextBlock.builder().text(question).build())
                        .build();

                // Call agent and get response
                Msg response = agent.call(userMsg).block();
                System.out.println("Agent: " + (response != null ? response.getTextContent() : "No response"));
                System.out.println();
            }

            System.out.println("=== Example completed ===\n");

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}