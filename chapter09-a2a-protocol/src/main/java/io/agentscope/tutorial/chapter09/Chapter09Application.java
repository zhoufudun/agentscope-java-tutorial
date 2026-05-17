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

package io.agentscope.tutorial.chapter09;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Chapter 09 Application - A2A (Agent to Agent) Protocol.
 *
 * <p>This application demonstrates the Agent to Agent (A2A) communication protocol
 * implementation in AgentScope Java. It showcases:
 *
 * <ul>
 *   <li>A2A Server configuration and deployment</li>
 *   <li>AgentCard for service discovery</li>
 *   <li>A2A Client for remote agent communication</li>
 *   <li>Streaming response support</li>
 *   <li>Task status synchronization</li>
 * </ul>
 *
 * <p><b>Usage:</b>
 * <ol>
 *   <li>Start the application with: {@code mvn spring-boot:run}</li>
 *   <li>Access the AgentCard at: {@code http://localhost:8888/.well-known/agent-card.json}</li>
 *   <li>Send requests via the A2A protocol endpoint</li>
 *   <li>View streaming responses in the console</li>
 * </ol>
 *
 * <p><b>Key Endpoints:</b>
 * <ul>
 *   <li>{@code GET /.well-known/agent-card.json} - AgentCard for service discovery</li>
 *   <li>{@code POST /} - A2A JSON-RPC endpoint</li>
 *   <li>{@code GET /api/chat} - REST API chat interface</li>
 * </ul>
 */
@SpringBootApplication
public class Chapter09Application {

    public static void main(String[] args) {
        SpringApplication.run(Chapter09Application.class, args);
    }
}