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

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.Test;
import java.util.Map;

/**
 * Integration tests for {@link ChatResource}.
 *
 * <p>These tests verify the REST endpoints including:
 * <ul>
 *   <li>Successful chat requests</li>
 *   <li>Error handling (empty message, null message)</li>
 *   <li>Response formatting</li>
 *   <li>健康检查端点</li>
 * </ul>
 *
 * <p>Uses REST-assured for HTTP testing and QuarkusTestProfile
 * to override configuration for testing.
 *
 * @author AgentScope Tutorial
 */
@QuarkusTest
@TestProfile(ChatResourceTest.TestAgentProfile.class)
class ChatResourceTest {

    /**
     * Test health endpoint returns agent status.
     */
    @Test
    void testHealthEndpoint() {
        given().when()
                .get("/health")
                .then()
                .statusCode(200)
                .contentType(MediaType.APPLICATION_JSON)
                .body("status", notNullValue())
                .body("agentName", is("TestAgent"))
                .body("framework", is("Quarkus"));
    }

    /**
     * Test echo endpoint returns agent information.
     */
    @Test
    void testEchoEndpoint() {
        given().when()
                .get("/echo")
                .then()
                .statusCode(200)
                .contentType(MediaType.APPLICATION_JSON)
                .body("agentName", is("TestAgent"))
                .body("framework", is("Quarkus"));
    }

    /**
     * Test successful chat with valid message.
     */
    @Test
    void testChatEndpointSuccess() {
        given().contentType(MediaType.APPLICATION_JSON)
                .body("{\"message\":\"Hello\"}")
                .when()
                .post("/chat")
                .then()
                .statusCode(200)
                .contentType(MediaType.APPLICATION_JSON)
                .body("response", notNullValue());
    }

    /**
     * Test chat endpoint rejects empty message.
     */
    @Test
    void testChatEndpointEmptyMessage() {
        given().contentType(MediaType.APPLICATION_JSON)
                .body("{\"message\":\"\"}")
                .when()
                .post("/chat")
                .then()
                .statusCode(400)
                .contentType(MediaType.APPLICATION_JSON)
                .body("error", is("Message cannot be empty"));
    }

    /**
     * Test chat endpoint rejects null message (missing field).
     */
    @Test
    void testChatEndpointNullMessage() {
        given().contentType(MediaType.APPLICATION_JSON)
                .body("{}")
                .when()
                .post("/chat")
                .then()
                .statusCode(400)
                .contentType(MediaType.APPLICATION_JSON)
                .body("error", is("Message cannot be empty"));
    }

    /**
     * Test chat endpoint handles whitespace-only message.
     */
    @Test
    void testChatEndpointWhitespaceMessage() {
        given().contentType(MediaType.APPLICATION_JSON)
                .body("{\"message\":\"   \"}")
                .when()
                .post("/chat")
                .then()
                .statusCode(400)
                .contentType(MediaType.APPLICATION_JSON)
                .body("error", is("Message cannot be empty"));
    }

    /**
     * Test chat endpoint with long message.
     */
    @Test
    void testChatEndpointLongMessage() {
        String longMessage = "a".repeat(1000);
        given().contentType(MediaType.APPLICATION_JSON)
                .body("{\"message\":\"" + longMessage + "\"}")
                .when()
                .post("/chat")
                .then()
                .statusCode(200)
                .contentType(MediaType.APPLICATION_JSON)
                .body("response", notNullValue());
    }

    /**
     * Test chat endpoint with special characters in message.
     */
    @Test
    void testChatEndpointSpecialCharacters() {
        given().contentType(MediaType.APPLICATION_JSON)
                .body("{\"message\":\"Hello! @#$%^&*() 测试 🚀\"}")
                .when()
                .post("/chat")
                .then()
                .statusCode(200)
                .contentType(MediaType.APPLICATION_JSON)
                .body("response", notNullValue());
    }

    /**
     * Test chat endpoint with newlines in message.
     */
    @Test
    void testChatEndpointMultilineMessage() {
        given().contentType(MediaType.APPLICATION_JSON)
                .body("{\"message\":\"Line 1\\nLine 2\\nLine 3\"}")
                .when()
                .post("/chat")
                .then()
                .statusCode(200)
                .contentType(MediaType.APPLICATION_JSON)
                .body("response", notNullValue());
    }

    /**
     * Test multi-turn chat endpoint with valid messages.
     */
    @Test
    void testMultiChatEndpointSuccess() {
        given().contentType(MediaType.APPLICATION_JSON)
                .body("{\"messages\":[\"Hello\", \"How are you?\"]}")
                .when()
                .post("/chat/multi")
                .then()
                .statusCode(200)
                .contentType(MediaType.APPLICATION_JSON)
                .body("response", notNullValue());
    }

    /**
     * Test multi-turn chat endpoint rejects empty messages.
     */
    @Test
    void testMultiChatEndpointEmptyMessages() {
        given().contentType(MediaType.APPLICATION_JSON)
                .body("{\"messages\":[]}")
                .when()
                .post("/chat/multi")
                .then()
                .statusCode(400)
                .contentType(MediaType.APPLICATION_JSON)
                .body("error", is("Messages cannot be empty"));
    }

    /**
     * Test multi-turn chat endpoint rejects null messages.
     */
    @Test
    void testMultiChatEndpointNullMessages() {
        given().contentType(MediaType.APPLICATION_JSON)
                .body("{}")
                .when()
                .post("/chat/multi")
                .then()
                .statusCode(400)
                .contentType(MediaType.APPLICATION_JSON)
                .body("error", is("Messages cannot be empty"));
    }

    /**
     * Test content type header in response.
     */
    @Test
    void testContentTypeHeader() {
        given().contentType(MediaType.APPLICATION_JSON)
                .body("{\"message\":\"Test\"}")
                .when()
                .post("/chat")
                .then()
                .statusCode(200)
                .contentType(MediaType.APPLICATION_JSON);
    }

    /**
     * 测试环境 with minimal agent configuration for testing.
     *
     * <p>This profile overrides the AgentScope configuration to use
     * a test API key and minimal agent settings, allowing tests to
     * run without requiring real API credentials.
     */
    public static class TestAgentProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "agentscope.model.provider", "dashscope",
                    "agentscope.dashscope.api-key", "test-api-key",
                    "agentscope.dashscope.model-name", "qwen-plus",
                    "agentscope.agent.name", "TestAgent",
                    "agentscope.agent.sys-prompt", "You are a test assistant. Keep responses brief."
            );
        }
    }
}