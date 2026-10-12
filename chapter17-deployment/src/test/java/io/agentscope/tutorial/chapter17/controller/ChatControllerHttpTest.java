/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.agentscope.tutorial.chapter17.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ChatController HTTP 接口测试.
 *
 * <p>使用 Spring MockMvc 对 ChatController 的 REST 端点进行集成测试。
 * 本测试类覆盖不依赖外部服务（DashScope / Redis / MySQL）的端点。
 *
 * <h2>测试范围</h2>
 * <table border="1">
 *   <tr><th>端点</th><th>测试内容</th></tr>
 *   <tr><td>{@code GET /api/v1/info}</td><td>构建信息返回</td></tr>
 *   <tr><td>{@code GET /api/v1/stats}</td><td>服务统计返回</td></tr>
 *   <tr><td>{@code DELETE /api/v1/session/{id}}</td><td>会话清除</td></tr>
 *   <tr><td>{@code POST /api/v1/chat}</td><td>请求体校验</td></tr>
 * </table>
 *
 * <h2>排除范围</h2>
 * <p>需要真实 AI 模型（DashScope）的端点不做正向断言，仅验证参数校验：
 * <ul>
 *   <li>{@code POST /api/v1/chat} - 正向调用需要 AI 模型</li>
 *   <li>{@code POST /api/v1/chat/async} - 异步处理需要 AI 模型</li>
 *   <li>{@code GET /api/v1/chat/stream/{sessionId}} - 流式需要 AI 模型</li>
 * </ul>
 *
 * <h2>测试策略</h2>
 * <ul>
 *   <li>使用 {@code @AutoConfigureMockMvc} 自动配置 MockMvc</li>
 *   <li>使用 H2 内存数据库替代真实数据库</li>
 *   <li>禁用 DashScope 避免外部依赖</li>
 * </ul>
 *
 * <h2>运行方式</h2>
 * <pre>{@code
 * # Maven
 * mvn test -Dtest=ChatControllerHttpTest
 *
 * # IDE
 * 右键点击测试类 -> Run
 * }</pre>
 *
 * @author AgentScope Team
 * @version 1.0.0
 * @see ChatController
 */
@SpringBootTest  // 加载完整的 Spring 应用上下文
@AutoConfigureMockMvc  // 自动配置 MockMvc，用于模拟 HTTP 请求
@ActiveProfiles("test")  // 使用 test profile
@TestPropertySource(properties = {
    // 使用 H2 内存数据库
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    // Redis 配置（测试中会被 Mock 或跳过）
    "spring.data.redis.host=localhost",
    "spring.data.redis.port=6379",
    // 禁用 DashScope，避免测试依赖外部 AI API
    "agentscope.model.provider=dashscope",
    "agentscope.dashscope.enabled=false"
})
class ChatControllerHttpTest {

    /**
     * MockMvc 实例，用于模拟 HTTP 请求.
     *
     * <p>通过 {@code @Autowired} 注入，{@code @AutoConfigureMockMvc} 已自动配置。
     * 可以模拟 GET/POST/PUT/DELETE 等所有 HTTP 方法。
     */
    @Autowired
    private MockMvc mockMvc;

    /**
     * 测试获取构建信息端点.
     *
     * <p>验证 {@code GET /api/v1/info} 返回正确的构建信息。
     *
     * <h3>预期响应</h3>
     * <pre>{@code
     * {
     *   "name": "chapter17-deployment",
     *   "version": "1.0.0",
     *   "buildTime": "2024-01-15T10:00:00Z",
     *   "gitCommit": "abc123",
     *   "gitBranch": "test"
     * }
     * }</pre>
     *
     * @throws Exception 如果 MockMvc 执行失败
     */
    @Test
    @DisplayName("GET /api/v1/info 返回构建信息，包含版本与 Git 分支")
    void getInfoReturnsBuildInfo() throws Exception {
        mockMvc.perform(get("/api/v1/info"))  // 执行 GET 请求
            .andExpect(status().isOk())  // 期望 HTTP 200
            .andExpect(jsonPath("$.version").value("1.0.0"))  // 验证版本号
            .andExpect(jsonPath("$.gitBranch").value("test"))  // 验证 Git 分支
            .andExpect(jsonPath("$.name").exists());  // 验证应用名存在
    }

    /**
     * 测试获取服务统计端点.
     *
     * <p>验证 {@code GET /api/v1/stats} 返回初始统计数据。
     * 由于是新启动的测试环境，计数应为 0。
     *
     * <h3>预期响应</h3>
     * <pre>{@code
     * {
     *   "totalRequests": 0,
     *   "averageDurationMs": 0.0,
     *   "totalTokens": 0
     * }
     * }</pre>
     *
     * @throws Exception 如果 MockMvc 执行失败
     */
    @Test
    @DisplayName("GET /api/v1/stats 返回初始服务统计（计数为 0）")
    void getStatsReturnsInitialStats() throws Exception {
        mockMvc.perform(get("/api/v1/stats"))  // 执行 GET 请求
            .andExpect(status().isOk())  // 期望 HTTP 200
            .andExpect(jsonPath("$.totalRequests").value(0))  // 初始请求数为 0
            .andExpect(jsonPath("$.totalTokens").value(0));  // 初始 Token 数为 0
    }

    /**
     * 测试清除会话端点.
     *
     * <p>验证 {@code DELETE /api/v1/session/{sessionId}} 正确清除会话。
     * 成功时返回 HTTP 204 No Content。
     *
     * <p><b>注意：</b>即使会话不存在，也返回 204（幂等性）。
     *
     * @throws Exception 如果 MockMvc 执行失败
     */
    @Test
    @DisplayName("DELETE /api/v1/session/{id} 清除会话返回 204")
    void deleteSessionReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/session/{sessionId}", "test-session-1"))
            .andExpect(status().isNoContent());  // 期望 HTTP 204
    }

    /**
     * 测试空请求体的参数校验.
     *
     * <p>验证 {@code POST /api/v1/chat} 在收到空 JSON 时返回 400 错误。
     * 这测试了 Jakarta Validation 的 {@code @NotBlank} 约束。
     *
     * <h3>请求</h3>
     * <pre>{@code
     * POST /api/v1/chat
     * Content-Type: application/json
     * Body: {}
     * }</pre>
     *
     * <h3>预期响应</h3>
     * <p>HTTP 400 Bad Request，包含验证错误信息。
     *
     * @throws Exception 如果 MockMvc 执行失败
     */
    @Test
    @DisplayName("POST /api/v1/chat 空请求体触发参数校验，返回 400")
    void postChatWithBlankBodyReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/chat")
                .contentType(MediaType.APPLICATION_JSON)  // 设置 Content-Type
                .content("{}"))  // 空 JSON 对象
            .andExpect(status().isBadRequest());  // 期望 HTTP 400
    }

    /**
     * 测试缺少必填字段的参数校验.
     *
     * <p>验证 {@code POST /api/v1/chat} 在缺少 message 字段时返回 400。
     * 这测试了 {@code @NotBlank} 对必填字段的验证。
     *
     * <h3>请求</h3>
     * <pre>{@code
     * POST /api/v1/chat
     * Content-Type: application/json
     * Body: {"userId": "u1", "sessionId": "s1"}
     * }</pre>
     *
     * <h3>预期响应</h3>
     * <p>HTTP 400 Bad Request，错误信息指出 message 不能为空。
     *
     * @throws Exception 如果 MockMvc 执行失败
     */
    @Test
    @DisplayName("POST /api/v1/chat 缺少 message 字段触发参数校验，返回 400")
    void postChatWithMissingMessageReturnsBadRequest() throws Exception {
        // 构造缺少 message 字段的请求体
        String body = "{\"userId\":\"u1\",\"sessionId\":\"s1\"}";
        mockMvc.perform(post("/api/v1/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());  // 期望 HTTP 400
    }
}
