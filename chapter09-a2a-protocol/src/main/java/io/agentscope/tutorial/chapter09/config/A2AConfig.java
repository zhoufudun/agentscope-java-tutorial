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

package io.agentscope.tutorial.chapter09.config;

import io.agentscope.core.a2a.server.card.ConfigurableAgentCard;
import io.agentscope.core.a2a.server.transport.DeploymentProperties;
import io.agentscope.core.a2a.server.transport.TransportProperties;
import io.agentscope.core.a2a.server.transport.jsonrpc.JsonRpcTransportMetadata;
import io.agentscope.tutorial.chapter09.service.A2AService;
import io.a2a.spec.AgentProvider;
import io.a2a.spec.AgentSkill;
import io.a2a.spec.TransportProtocol;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * A2A Protocol Configuration.
 *
 * <p>This configuration class demonstrates how to customize the A2A server
 * beyond the auto-configuration. It provides:
 * <ul>
 *   <li>Custom AgentCard configuration</li>
 *   <li>Deployment properties (host, port)</li>
 *   <li>Transport properties (JSON-RPC)</li>
 *   <li>Agent metadata (name, description, skills)</li>
 * </ul>
 *
 * <p><b>Key A2A Concepts:</b>
 * <ul>
 *   <li><b>AgentCard</b>: Metadata document for service discovery, served at
 *       {@code /.well-known/agent-card.json}</li>
 *   <li><b>JSON-RPC Transport</b>: 默认 transport protocol for A2A</li>
 *   <li><b>Skills</b>: Defines agent capabilities in a standardized way</li>
 * </ul>
 *
 * <p><b>Configuration:</b>
 * All settings can be configured via {@code application.yml}:
 * <pre>{@code
 * a2a:
 *   server:
 *     enabled: true
 *     agent-card:
 *       name: my-agent
 *       description: My custom agent
 * }</pre>
 */
@Configuration
public class A2AConfig {

    /**
     * Server deployment port.
     */
    @Value("${server.port:8888}")
    private int serverPort;

    /**
     * Server deployment host.
     */
    @Value("${server.address:localhost}")
    private String serverHost;

    /**
     * Context path for the application.
     */
    @Value("${server.servlet.context-path:}")
    private String contextPath;

    /**
     * Build custom deployment properties.
     *
     * <p>Deployment properties are used to generate the agent's URL
     * and endpoint information in the AgentCard.
     */
    @Bean
    public DeploymentProperties deploymentProperties() {
        return new DeploymentProperties.Builder()
                .host(serverHost)
                .port(serverPort)
                .path(normalizePath(contextPath))
                .build();
    }

    /**
     * Build custom transport properties for JSON-RPC.
     *
     * <p>The transport properties define how the A2A server exposes
     * its endpoints. JSON-RPC is the default and most common transport.
     */
    @Bean
    public TransportProperties jsonRpcTransportProperties() {
        return TransportProperties.builder(TransportProtocol.JSONRPC.asString())
                .host(serverHost)
                .port(serverPort)
                .path(normalizePath(contextPath))
                .metadata(new JsonRpcTransportMetadata.Builder()
                        .contentType("application/json")
                        .build())
                .build();
    }

    /**
     * Build custom AgentCard properties.
     *
     * <p>The AgentCard is the core metadata document for A2A service discovery.
     * It provides information about the agent's capabilities, skills, and
     * transport endpoints.
     *
     * <p><b>Example AgentCard JSON:</b>
     * <pre>{@code
     * {
     *   "name": "tutorial-a2a-agent",
     *   "description": "Tutorial agent for A2A protocol",
     *   "url": "http://localhost:8888",
     *   "version": "1.0.0",
     *   "provider": {
     *     "name": "AgentScope Team"
     *   },
     *   "skills": [
     *     {"id": "text-chat", "name": "Text Chat"}
     *   ]
     * }
     * }</pre>
     */
    @Bean
    public ConfigurableAgentCard configurableAgentCard() {
        return new ConfigurableAgentCard.Builder()
                .name("tutorial-a2a-agent")
                .description("Tutorial agent demonstrating A2A protocol communication")
                .url("http://" + serverHost + ":" + serverPort + normalizePath(contextPath))
                .version("1.0.0")
                .documentationUrl("http://" + serverHost + ":" + serverPort + normalizePath(contextPath) + "docs")
                .provider(new AgentProvider("AgentScope Team", "https://agentscope.io"))
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text", "streaming"))
                .skills(buildSkills())
                .iconUrl("https://agentscope.io/icon.png")
                .preferredTransport(TransportProtocol.JSONRPC.asString())
                .build();
    }

    /**
     * Build the list of skills this agent supports.
     *
     * <p>Skills define the capabilities of the agent in a standardized way,
     * allowing other agents to discover and utilize these capabilities.
     */
    private List<AgentSkill> buildSkills() {
        return List.of(
                AgentSkill.builder()
                        .id("text-chat")
                        .name("Text Chat")
                        .description("General text-based conversation with AI assistant")
                        .build(),
                AgentSkill.builder()
                        .id("calculation")
                        .name("Calculation")
                        .description("Mathematical calculations using the calculate tool")
                        .build(),
                AgentSkill.builder()
                        .id("weather")
                        .name("Weather Query")
                        .description("Get weather information for cities using get_weather tool")
                        .build(),
                AgentSkill.builder()
                        .id("time")
                        .name("Time Query")
                        .description("Get current date and time using get_current_time tool")
                        .build()
        );
    }

    /**
     * Normalize path to ensure it starts with / and doesn't end with /.
     */
    private String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return "/";
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        if (path.endsWith("/") && path.length() > 1) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }
}