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

package io.agentscope.tutorial.chapter09.client;

import io.agentscope.core.a2a.agent.A2aAgent;
import io.agentscope.core.a2a.agent.card.AgentCardResolver;
import io.agentscope.core.a2a.agent.card.WellKnownAgentCardResolver;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.UUID;

/**
 * A2A Client for communicating with remote agents.
 *
 * <p>This client demonstrates how to:
 * <ul>
 *   <li>Create an A2A client using AgentCard discovery</li>
 *   <li>Send messages to remote agents</li>
 *   <li>Handle streaming responses</li>
 *   <li>Process task events and updates</li>
 * </ul>
 *
 * <p>The A2A client uses the well-known URI pattern to discover
 * the remote agent's AgentCard, which contains its endpoint and
 * capabilities information.
 *
 * <p><b>Usage Example:</b>
 * <pre>{@code
 * // Send a message
 * Msg message = Msg.of("Hello, calculate 2 + 2");
 * String response = agentClient.chat(message).block();
 *
 * // Stream responses
 * agentClient.streamChat(message)
 *     .doOnNext(chunk -> System.out.print(chunk))
 *     .then()
 *     .block();
 * }</pre>
 */
@Component
public class AgentClient {

    private static final Logger log = LoggerFactory.getLogger(AgentClient.class);

    /**
     * 默认 base URL for AgentCard discovery.
     */
    @Value("${a2a.client.base-url:http://localhost:8888}")
    private String baseUrl;

    /**
     * 默认 path for well-known AgentCard.
     */
    @Value("${a2a.client.agent-card-path:/.well-known/agent-card.json}")
    private String agentCardPath;

    /**
     * Remote agent name.
     */
    @Value("${a2a.client.agent-name:tutorial-a2a-agent}")
    private String agentName;

    /**
     * The A2A agent proxy for remote communication.
     */
    private A2aAgent a2aAgent;

    /**
     * Initialize the A2A client.
     *
     * <p>该方法 creates the A2aAgent instance configured to
     * communicate with the remote agent via A2A protocol.
     */
    @jakarta.annotation.PostConstruct
    public void init() {
        try {
            // Create AgentCard resolver using well-known URI
            AgentCardResolver agentCardResolver = WellKnownAgentCardResolver.builder()
                    .baseUrl(baseUrl)
                    .path(agentCardPath)
                    .headers(Map.of())
                    .build();

            // Build A2A agent client
            this.a2aAgent = A2aAgent.builder()
                    .name(agentName)
                    .agentCardResolver(agentCardResolver)
                    .build();

            log.info("A2A Client initialized successfully");
            log.info("Target Agent: {}", agentName);
            log.info("AgentCard URL: {}{}", baseUrl, agentCardPath);

        } catch (Exception e) {
            log.warn("Failed to initialize A2A client: {}. "
                    + "This is expected if the remote agent is not available.", e.getMessage());
        }
    }

    /**
     * Send a chat message to the remote agent and get a response.
     *
     * <p>该方法 blocks until the response is received.
     * For streaming responses, use {@link #streamChat(Msg)}.
     *
     * @param message The message to send
     * @return The response message from the remote agent
     */
    public Msg chat(Msg message) {
        log.info("Sending message to remote agent: {}", message.getContent());
        return a2aAgent.call(message);
    }

    /**
     * Send a chat message to the remote agent with a context ID.
     *
     * @param content The message content
     * @param contextId The context ID for conversation tracking
     * @return The response message from the remote agent
     */
    public Msg chat(String content, String contextId) {
        Msg message = Msg.builder()
                .role(MsgRole.USER)
                .content(TextBlock.builder().text(content).build())
                .contextId(contextId)
                .build();
        return chat(message);
    }

    /**
     * Stream chat responses from the remote agent.
     *
     * <p>该方法 returns a Flux of response chunks, allowing
     * for real-time streaming of the agent's response.
     *
     * <p><b>Usage:</b>
     * <pre>{@code
     * agentClient.streamChat(message)
     *     .doOnNext(chunk -> System.out.print(chunk))
     *     .doOnComplete(() -> System.out.println())
     *     .subscribe();
     * }</pre>
     *
     * @param message The message to send
     * @return A Flux of response text chunks
     */
    public Flux<String> streamChat(Msg message) {
        log.info("Starting streaming chat with remote agent");

        return a2aAgent.stream(message)
                .map(event -> {
                    // Process the event
                    if (event.isLast()) {
                        return "";
                    }

                    Msg responseMessage = event.getMessage();
                    if (responseMessage == null) {
                        return "";
                    }

                    // Extract text content from message
                    StringBuilder text = new StringBuilder();
                    responseMessage.getContent().stream()
                            .filter(block -> block instanceof TextBlock)
                            .map(block -> (TextBlock) block)
                            .forEach(block -> text.append(block.getText()));

                    return text.toString();
                });
    }

    /**
     * Stream chat responses with a context ID.
     *
     * @param content The message content
     * @param contextId The context ID for conversation tracking
     * @return A Flux of response text chunks
     */
    public Flux<String> streamChat(String content, String contextId) {
        Msg message = Msg.builder()
                .role(MsgRole.USER)
                .content(TextBlock.builder().text(content).build())
                .contextId(contextId)
                .build();
        return streamChat(message);
    }

    /**
     * Send a message and get streaming events.
     *
     * <p>This provides access to the full event stream, including
     * task status updates and other metadata.
     *
     * @param message The message to send
     * @return A Flux of task events
     */
    public Flux<io.agentscope.core.a2a.agent.event.ClientEventContext> streamEvents(Msg message) {
        return a2aAgent.stream(message)
                .map(event -> event);
    }

    /**
     * Check if the A2A client is ready.
     *
     * @return true if the client is initialized and ready
     */
    public boolean isReady() {
        return a2aAgent != null;
    }

    /**
     * Get the remote agent's name.
     *
     * @return The agent name
     */
    public String getAgentName() {
        return agentName;
    }

    /**
     * Get the configured base URL.
     *
     * @return The base URL
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * Create a new conversation context.
     *
     * @return A new context ID
     */
    public String createContextId() {
        return UUID.randomUUID().toString();
    }
}