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
 * <p>覆盖不依赖外部服务（DashScope / Redis / MySQL）的 REST 端点：
 *
 * <ul>
 *   <li>{@code GET  /api/v1/info} —— 构建信息</li>
 *   <li>{@code GET  /api/v1/stats} —— 服务统计</li>
 *   <li>{@code DELETE /api/v1/session/{sessionId}} —— 清除会话</li>
 *   <li>{@code POST /api/v1/chat} —— 请求体校验（非法入参返回 400）</li>
 * </ul>
 *
 * <p>需要真实 AI 模型（DashScope）的 {@code /chat} 正向调用、{@code /chat/async}、
 * {@code /chat/stream} 在此不做正向断言，仅通过请求体校验验证参数约束，
 * 避免引入外部依赖导致的不稳定。
 *
 * @author AgentScope Team
 * @version 1.0.0
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.data.redis.host=localhost",
    "spring.data.redis.port=6379",
    "agentscope.model.provider=dashscope",
    "agentscope.dashscope.enabled=false"
})
class ChatControllerHttpTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/v1/info 返回构建信息，包含版本与 Git 分支")
    void getInfoReturnsBuildInfo() throws Exception {
        mockMvc.perform(get("/api/v1/info"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.version").value("1.0.0"))
            .andExpect(jsonPath("$.gitBranch").value("test"))
            .andExpect(jsonPath("$.name").exists());
    }

    @Test
    @DisplayName("GET /api/v1/stats 返回初始服务统计（计数为 0）")
    void getStatsReturnsInitialStats() throws Exception {
        mockMvc.perform(get("/api/v1/stats"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalRequests").value(0))
            .andExpect(jsonPath("$.totalTokens").value(0));
    }

    @Test
    @DisplayName("DELETE /api/v1/session/{id} 清除会话返回 204")
    void deleteSessionReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/session/{sessionId}", "test-session-1"))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /api/v1/chat 空请求体触发参数校验，返回 400")
    void postChatWithBlankBodyReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/chat 缺少 message 字段触发参数校验，返回 400")
    void postChatWithMissingMessageReturnsBadRequest() throws Exception {
        String body = "{\"userId\":\"u1\",\"sessionId\":\"s1\"}";
        mockMvc.perform(post("/api/v1/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());
    }
}
