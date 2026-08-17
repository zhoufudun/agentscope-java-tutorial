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

import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.agentscope.core.a2a.agent.A2aAgent;
import io.agentscope.core.a2a.agent.card.FixedAgentCardResolver;
import io.agentscope.core.a2a.agent.card.WellKnownAgentCardResolver;
import io.agentscope.core.a2a.server.AgentScopeA2aServer;
import io.agentscope.core.a2a.server.card.ConfigurableAgentCard;
import io.agentscope.core.a2a.server.transport.DeploymentProperties;
import io.agentscope.core.a2a.server.transport.TransportProperties;
import io.agentscope.core.a2a.server.transport.jsonrpc.JsonRpcTransportMetadata;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import io.a2a.spec.AgentProvider;
import io.a2a.spec.AgentSkill;
import io.a2a.spec.TransportProtocol;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for A2A Protocol functionality.
 *
 * <p>These tests demonstrate and verify the core concepts of the A2A protocol:
 * <ul>
 *   <li>AgentCard creation and configuration</li>
 *   <li>AgentCardResolver usage (Fixed and WellKnown)</li>
 *   <li>A2A message format and conversion</li>
 *   <li>Service discovery mechanism</li>
 * </ul>
 */
public class A2ATest {

    /**
     * Test AgentCard creation and configuration.
     *
     * <p>The AgentCard is the core metadata document for A2A service discovery.
     */
    @Test
    public void testAgentCardCreation() {
        // Create a configurable agent card
        ConfigurableAgentCard card = new ConfigurableAgentCard.Builder()
                .name("test-agent")
                .description("A test agent for A2A protocol")
                .url("http://localhost:8888")
                .version("1.0.0")
                .provider(new AgentProvider("Test Provider", "https://test.com"))
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text", "streaming"))
                .skills(buildTestSkills())
                .iconUrl("https://test.com/icon.png")
                .preferredTransport(TransportProtocol.JSONRPC.asString())
                .build();

        // Verify card properties
        assertEquals("test-agent", card.getName());
        assertEquals("A test agent for A2A protocol", card.getDescription());
        assertEquals("http://localhost:8888", card.getUrl());
        assertEquals("1.0.0", card.getVersion());
        assertNotNull(card.getProvider());
        assertEquals("Test Provider", card.getProvider().organization());
        assertNotNull(card.getSkills());
        assertEquals(2, card.getSkills().size());
    }

    /**
     * Test FixedAgentCardResolver usage.
     *
     * <p>The FixedAgentCardResolver is used when you have a pre-configured
     * AgentCard that doesn't need dynamic discovery.
     */
    @Test
    public void testFixedAgentCardResolver() {
        // Create a mock agent card.
        // In a2a-java-sdk 0.3.x the AgentCard record has 18 components, so we use its
        // Builder to set only the fields relevant to this test and leave the rest at
        // their builder defaults.
        AgentCard mockCard = new AgentCard.Builder()
                .name("fixed-agent")
                .description("An agent with fixed card")
                .url("http://localhost:8888")
                .provider(new AgentProvider("Fixed Provider", "https://fixed.com"))
                .version("1.0.0")
                .capabilities(new AgentCapabilities(false, false, false, List.of()))
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .skills(List.of())
                .build();

        // Create fixed resolver
        FixedAgentCardResolver resolver = FixedAgentCardResolver.builder()
                .agentCard(mockCard)
                .build();

        // Get agent card from resolver
        AgentCard resolvedCard = resolver.getAgentCard("fixed-agent");

        assertNotNull(resolvedCard);
        assertEquals("fixed-agent", resolvedCard.name());
        assertEquals("An agent with fixed card", resolvedCard.description());
    }

    /**
     * Test WellKnownAgentCardResolver configuration.
     *
     * <p>The WellKnownAgentCardResolver discovers the AgentCard from a
     * well-known URI using HTTP.
     */
    @Test
    public void testWellKnownAgentCardResolver() {
        // Create well-known resolver
        WellKnownAgentCardResolver resolver = WellKnownAgentCardResolver.builder()
                .baseUrl("http://localhost:8888")
                .relativeCardPath("/.well-known/agent-card.json")
                .authHeaders(Map.of())
                .build();

        assertNotNull(resolver);

        // Note: This test doesn't actually connect to the server
        // In real tests, you would mock the HTTP response or use a test server
    }

    /**
     * Test message creation and formatting.
     *
     * <p>A2A messages have a specific format with roles, content blocks,
     * and metadata.
     */
    @Test
    public void testMessageCreation() {
        // Create a user message.
        // Msg has no dedicated context id field; conversational context is carried
        // through the message metadata map (Map<String, Object>).
        Msg message = Msg.builder()
                .role(io.agentscope.core.message.MsgRole.USER)
                .content(TextBlock.builder().text("Hello, calculate 2 + 2").build())
                .metadata(Map.of("contextId", "test-context-123"))
                .build();

        assertNotNull(message);
        assertEquals(io.agentscope.core.message.MsgRole.USER, message.getRole());
        assertNotNull(message.getContent());
        assertEquals("test-context-123", message.getMetadata().get("contextId"));
    }

    /**
     * Test transport properties configuration.
     *
     * <p>Transport properties define how the A2A server exposes its
     * endpoints. JSON-RPC is the default transport.
     */
    @Test
    public void testTransportProperties() {
        TransportProperties properties = TransportProperties.builder(
                        TransportProtocol.JSONRPC.asString())
                .host("localhost")
                .port(8888)
                .path("/")
                .extra("metadata", new JsonRpcTransportMetadata())
                .build();

        assertEquals("JSONRPC", properties.transportType());
        assertEquals("localhost", properties.host());
        assertEquals(8888, properties.port());
        assertEquals("/", properties.path());
    }

    /**
     * Test deployment properties configuration.
     *
     * <p>Deployment properties are used to generate the default transport
     * interface information in the AgentCard.
     */
    @Test
    public void testDeploymentProperties() {
        DeploymentProperties properties = new DeploymentProperties.Builder()
                .host("0.0.0.0")
                .port(8888)
                .path("/a2a")
                .build();

        assertEquals("0.0.0.0", properties.host());
        assertEquals(8888, properties.port());
        assertEquals("/a2a", properties.path());
    }

    /**
     * Test AgentCard skills configuration.
     *
     * <p>Skills define the capabilities of the agent in a standardized way,
     * allowing other agents to discover and utilize these capabilities.
     */
    @Test
    public void testAgentSkills() {
        List<AgentSkill> skills = buildTestSkills();

        assertNotNull(skills);
        assertEquals(2, skills.size());

        // Verify skill properties
        AgentSkill firstSkill = skills.get(0);
        assertEquals("calculation", firstSkill.id());
        assertEquals("Calculation", firstSkill.name());
        assertNotNull(firstSkill.description());
    }

    /**
     * Test AgentScopeA2aServer builder configuration.
     *
     * <p>The server builder demonstrates how to configure an A2A server
     * with custom transport and agent card settings.
     */
    @Test
    public void testServerBuilder() {
        // Build a simple server configuration (without actual agent)
        // This test verifies the builder pattern works correctly

        ConfigurableAgentCard agentCard = new ConfigurableAgentCard.Builder()
                .name("builder-test-agent")
                .description("Testing server builder pattern")
                .url("http://localhost:8888")
                .version("1.0.0")
                .build();

        DeploymentProperties deploymentProperties = new DeploymentProperties.Builder()
                .port(8888)
                .build();

        // Verify configurations are correct
        assertNotNull(agentCard);
        assertEquals("builder-test-agent", agentCard.getName());
        assertNotNull(deploymentProperties);
    }

    /**
     * Test A2A message metadata handling.
     *
     * <p>Messages can include metadata for tracking, authentication,
     * and other purposes.
     */
    @Test
    public void testMessageMetadata() {
        Map<String, Object> metadata = Map.of(
                "userId", "user-123",
                "sessionId", "session-456",
                "traceId", "trace-789"
        );

        Msg message = Msg.builder()
                .role(io.agentscope.core.message.MsgRole.USER)
                .content(TextBlock.builder().text("Test message").build())
                .metadata(metadata)
                .build();

        assertNotNull(message.getMetadata());
        assertEquals("user-123", message.getMetadata().get("userId"));
        assertEquals("session-456", message.getMetadata().get("sessionId"));
    }

    /**
     * Test streaming response handling.
     *
     * <p>The A2A protocol supports streaming responses using Flux
     * from Project Reactor.
     */
    @Test
    public void testStreamingResponse() {
        // Create a mock Flux stream (simulating streaming response)
        Flux<String> mockStream = Flux.just("Part 1", "Part 2", "Part 3", "[DONE]");

        // Collect all parts
        StringBuilder collected = new StringBuilder();
        mockStream.subscribe(
                collected::append,
                error -> fail("Stream error: " + error.getMessage()),
                () -> assertTrue(collected.toString().contains("[DONE]"))
        );
    }

    /**
     * Build test skills for unit tests.
     */
    private List<AgentSkill> buildTestSkills() {
        // AgentSkill in a2a-java-sdk 0.3.x exposes a nested Builder (constructed via
        // new AgentSkill.Builder()); there is no static AgentSkill.builder() factory.
        return List.of(
                new AgentSkill.Builder()
                        .id("calculation")
                        .name("Calculation")
                        .description("Mathematical calculations")
                        .tags(List.of("math"))
                        .build(),
                new AgentSkill.Builder()
                        .id("text-chat")
                        .name("Text Chat")
                        .description("General text conversation")
                        .tags(List.of("chat"))
                        .build()
        );
    }
}