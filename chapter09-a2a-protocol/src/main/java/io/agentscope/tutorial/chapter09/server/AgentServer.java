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

package io.agentscope.tutorial.chapter09.server;

import io.agentscope.core.a2a.server.AgentScopeA2aServer;
import io.agentscope.core.a2a.server.transport.TransportWrapper;
import io.agentscope.core.a2a.server.transport.jsonrpc.JsonRpcTransportWrapper;
import io.a2a.spec.AgentCard;
import io.a2a.spec.TransportProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.自动注入;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

/**
 * A2A Server component for exposing the agent via A2A protocol.
 *
 * <p>This server component:
 * <ul>
 *   <li>Initializes the AgentScopeA2aServer from Spring Boot auto-configuration</li>
 *   <li>Provides access to transport wrappers for request handling</li>
 *   <li>Exposes the AgentCard for service discovery</li>
 * </ul>
 *
 * <p><b>Key Responsibilities:</b>
 * <ul>
 *   <li>Bridge between Spring Boot and A2A protocol layer</li>
 *   <li>Provide transport wrapper access for JSON-RPC handling</li>
 *   <li>Expose AgentCard for client discovery</li>
 * </ul>
 */
@Component
public class AgentServer {

    private static final Logger log = LoggerFactory.getLogger(AgentServer.class);

    private static final String JSONRPC_TRANSPORT = TransportProtocol.JSONRPC.asString();

    private final AgentScopeA2aServer a2aServer;

    private JsonRpcTransportWrapper jsonRpcTransportWrapper;

    /**
     * 构造函数 injection of the A2A server.
     *
     * <p>The AgentScopeA2aServer bean is automatically created by the
     * agentscope-a2a-spring-boot-starter when a ReActAgent or its Builder
     * is present in the Spring context.
     *
     * @param a2aServer The auto-configured A2A server
     */
    @Autowired
    public AgentServer(AgentScopeA2aServer a2aServer) {
        this.a2aServer = a2aServer;
    }

    /**
     * Initialize transport wrapper on context refresh.
     */
    @org.springframework.context.event.EventListener(ContextRefreshedEvent.class)
    public void init() {
        try {
            // Get the JSON-RPC transport wrapper
            this.jsonRpcTransportWrapper = a2aServer.getTransportWrapper(
                    JSONRPC_TRANSPORT, JsonRpcTransportWrapper.class);

            // Log agent card information
            AgentCard agentCard = a2aServer.getAgentCard();
            log.info("=== A2A Server Initialized ===");
            log.info("Agent Name: {}", agentCard.name());
            log.info("Agent URL: {}", agentCard.url());
            log.info("Agent Version: {}", agentCard.version());
            log.info("Transport: {}", JSONRPC_TRANSPORT);
            log.info("Skills: {}", agentCard.skills().size());
            log.info("================================");

        } catch (Exception e) {
            log.error("Failed to initialize A2A server transport wrapper", e);
        }
    }

    /**
     * Get the A2A server instance.
     *
     * @return The AgentScopeA2aServer
     */
    public AgentScopeA2aServer getA2aServer() {
        return a2aServer;
    }

    /**
     * Get the JSON-RPC transport wrapper.
     *
     * <p>This wrapper handles JSON-RPC request processing and can be
     * used by the A2aJsonRpcController.
     *
     * @return The JsonRpcTransportWrapper or null if not initialized
     */
    public JsonRpcTransportWrapper getJsonRpcTransportWrapper() {
        return jsonRpcTransportWrapper;
    }

    /**
     * Get the agent card.
     *
     * <p>The AgentCard contains all metadata about this agent,
     * including its capabilities and endpoints.
     *
     * @return The AgentCard instance
     */
    public AgentCard getAgentCard() {
        return a2aServer.getAgentCard();
    }

    /**
     * Get a transport wrapper by type.
     *
     * <p>Use this method to access other transport wrappers if configured.
     *
     * @param transportType The type of transport (e.g., "JSONRPC")
     * @return The TransportWrapper instance
     * @throws IllegalArgumentException if transport not found
     */
    public TransportWrapper<?, ?> getTransportWrapper(String transportType) {
        return a2aServer.getTransportWrapper(transportType);
    }
}