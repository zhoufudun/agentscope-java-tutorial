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
 * {@link ChatResource} 的集成测试类。
 *
 * <h2>概述</h2>
 * <p>本测试类验证 ChatResource REST 端点的正确性，包括：
 * <ul>
 *   <li>成功的聊天请求处理</li>
 *   <li>错误处理（空消息、null 消息）</li>
 *   <li>响应格式验证</li>
 *   <li>健康检查端点</li>
 * </ul>
 *
 * <h2>Quarkus 测试特性</h2>
 * <ul>
 *   <li><b>@QuarkusTest</b> - 启动完整的 Quarkus 应用进行集成测试</li>
 *   <li><b>@TestProfile</b> - 使用自定义配置覆盖生产配置</li>
 *   <li><b>REST-assured</b> - 流式 API 进行 HTTP 测试</li>
 * </ul>
 *
 * <h2>测试配置</h2>
 * <p>使用 {@link TestAgentProfile} 覆盖配置，避免需要真实 API 凭据。
 *
 * @author AgentScope Tutorial
 * @see io.quarkus.test.junit.QuarkusTest
 * @see io.restassured.RestAssured
 */
@QuarkusTest  // Quarkus 集成测试注解，启动完整应用
@TestProfile(ChatResourceTest.TestAgentProfile.class)  // 使用测试配置
class ChatResourceTest {

    /**
     * 测试健康检查端点返回 Agent 状态。
     *
     * <h3>验证点</h3>
     * <ul>
     *   <li>HTTP 状态码 200</li>
     *   <li>响应类型为 JSON</li>
     *   <li>包含 status 字段</li>
     *   <li>agentName 为 "TestAgent"</li>
     *   <li>framework 为 "Quarkus"</li>
     * </ul>
     */
    @Test
    void testHealthEndpoint() {
        given().when()
                .get("/health")  // 发送 GET 请求到 /health
                .then()
                .statusCode(200)  // 验证状态码
                .contentType(MediaType.APPLICATION_JSON)  // 验证响应类型
                .body("status", notNullValue())  // 状态字段不为空
                .body("agentName", is("TestAgent"))  // Agent 名称匹配
                .body("framework", is("Quarkus"));  // 框架名称匹配
    }

    /**
     * 测试回显端点返回 Agent 信息。
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
     * 测试成功的聊天请求处理。
     *
     * <h3>验证点</h3>
     * <ul>
     *   <li>HTTP 200 响应</li>
     *   <li>JSON 格式响应</li>
     *   <li>包含 response 字段</li>
     * </ul>
     */
    @Test
    void testChatEndpointSuccess() {
        given().contentType(MediaType.APPLICATION_JSON)
                .body("{\"message\":\"Hello\"}")  // 发送有效消息
                .when()
                .post("/chat")
                .then()
                .statusCode(200)
                .contentType(MediaType.APPLICATION_JSON)
                .body("response", notNullValue());  // 响应不为空
    }

    /**
     * 测试聊天端点拒绝空消息。
     *
     * <h3>验证点</h3>
     * <ul>
     *   <li>HTTP 400 Bad Request</li>
     *   <li>错误信息为 "Message cannot be empty"</li>
     * </ul>
     */
    @Test
    void testChatEndpointEmptyMessage() {
        given().contentType(MediaType.APPLICATION_JSON)
                .body("{\"message\":\"\"}")  // 空消息
                .when()
                .post("/chat")
                .then()
                .statusCode(400)  // 期望 400 错误
                .contentType(MediaType.APPLICATION_JSON)
                .body("error", is("Message cannot be empty"));
    }

    /**
     * 测试聊天端点拒绝 null 消息（缺少字段）。
     */
    @Test
    void testChatEndpointNullMessage() {
        given().contentType(MediaType.APPLICATION_JSON)
                .body("{}")  // 缺少 message 字段
                .when()
                .post("/chat")
                .then()
                .statusCode(400)
                .contentType(MediaType.APPLICATION_JSON)
                .body("error", is("Message cannot be empty"));
    }

    /**
     * 测试聊天端点处理仅包含空白字符的消息。
     */
    @Test
    void testChatEndpointWhitespaceMessage() {
        given().contentType(MediaType.APPLICATION_JSON)
                .body("{\"message\":\"   \"}")  // 仅空白字符
                .when()
                .post("/chat")
                .then()
                .statusCode(400)
                .contentType(MediaType.APPLICATION_JSON)
                .body("error", is("Message cannot be empty"));
    }

    /**
     * 测试聊天端点处理长消息。
     *
     * <h3>目的</h3>
     * <p>验证端点能正确处理较长的消息文本，不会因消息长度而失败。
     */
    @Test
    void testChatEndpointLongMessage() {
        String longMessage = "a".repeat(1000);  // 1000 字符的长消息
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
     * 测试聊天端点处理特殊字符。
     *
     * <h3>测试内容</h3>
     * <ul>
     *   <li>标点符号</li>
     *   <li>特殊符号</li>
     *   <li>中文字符</li>
     *   <li>Emoji 表情</li>
     * </ul>
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
     * 测试聊天端点处理多行消息。
     */
    @Test
    void testChatEndpointMultilineMessage() {
        given().contentType(MediaType.APPLICATION_JSON)
                .body("{\"message\":\"Line 1\\nLine 2\\nLine 3\"}")  // 包含换行符
                .when()
                .post("/chat")
                .then()
                .statusCode(200)
                .contentType(MediaType.APPLICATION_JSON)
                .body("response", notNullValue());
    }

    /**
     * 测试多轮聊天端点成功处理有效消息列表。
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
     * 测试多轮聊天端点拒绝空消息列表。
     */
    @Test
    void testMultiChatEndpointEmptyMessages() {
        given().contentType(MediaType.APPLICATION_JSON)
                .body("{\"messages\":[]}")  // 空列表
                .when()
                .post("/chat/multi")
                .then()
                .statusCode(400)
                .contentType(MediaType.APPLICATION_JSON)
                .body("error", is("Messages cannot be empty"));
    }

    /**
     * 测试多轮聊天端点拒绝 null 消息列表。
     */
    @Test
    void testMultiChatEndpointNullMessages() {
        given().contentType(MediaType.APPLICATION_JSON)
                .body("{}")  // 缺少 messages 字段
                .when()
                .post("/chat/multi")
                .then()
                .statusCode(400)
                .contentType(MediaType.APPLICATION_JSON)
                .body("error", is("Messages cannot be empty"));
    }

    /**
     * 测试响应的 Content-Type 头。
     */
    @Test
    void testContentTypeHeader() {
        given().contentType(MediaType.APPLICATION_JSON)
                .body("{\"message\":\"Test\"}")
                .when()
                .post("/chat")
                .then()
                .statusCode(200)
                .contentType(MediaType.APPLICATION_JSON);  // 验证 JSON Content-Type
    }

    /**
     * 测试配置 Profile - 为测试提供最小化的 Agent 配置。
     *
     * <h3>设计目的</h3>
     * <p>本 Profile 覆盖 AgentScope 配置，使用测试 API Key 和最小化配置，
     * 允许测试在没有真实 API 凭据的情况下运行。
     *
     * <h3>Quarkus TestProfile 特性</h3>
     * <ul>
     *   <li>覆盖任意配置属性</li>
     *   <li>支持多个 Profile 组合</li>
     *   <li>可禁用特定扩展</li>
     * </ul>
     */
    public static class TestAgentProfile implements QuarkusTestProfile {
        /**
         * 返回配置覆盖映射。
         *
         * @return 配置属性键值对映射
         */
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "agentscope.model.provider", "dashscope",  // 模型提供商
                    "agentscope.dashscope.api-key", "test-api-key",  // 测试 API Key
                    "agentscope.dashscope.model-name", "qwen-plus",  // 模型名称
                    "agentscope.agent.name", "TestAgent",  // Agent 名称
                    "agentscope.agent.sys-prompt", "You are a test assistant. Keep responses brief."
            );
        }
    }
}