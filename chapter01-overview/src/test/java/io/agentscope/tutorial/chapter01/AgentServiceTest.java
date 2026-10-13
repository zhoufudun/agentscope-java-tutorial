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
package io.agentscope.tutorial.chapter01;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.test.MockModel;
import io.agentscope.core.agent.test.MockToolkit;
import io.agentscope.extensions.model.dashscope.DashScopeChatModel;
import io.agentscope.tutorial.chapter01.controller.ChatRequest;
import io.agentscope.tutorial.chapter01.controller.ChatResponse;
import io.agentscope.tutorial.chapter01.service.AgentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * AgentService 单元测试.
 *
 * <p>本测试类使用 Mock 来模拟 Agent 的行为，测试 AgentService 的各种功能。
 * 测试不需要真实的 API Key，可以在本地独立运行。
 *
 * <p>测试场景：
 * <ul>
 *   <li>正常聊天流程</li>
 *   <li>空消息处理</li>
 *   <li>异常处理</li>
 *   <li>消息内容提取</li>
 * </ul>
 */
class AgentServiceTest {

    /** Mock Agent 实例 */
    private ReActAgent mockAgent;

    /** Mock ObjectProvider */
    private ObjectProvider<ReActAgent> mockAgentProvider;

    /** AgentService 实例 */
    private AgentService agentService;

    /**
     * 测试前置准备.
     *
     * <p>创建一个 Mock Agent 并配置到 AgentService 中，
     * 这样测试可以在不调用真实 API 的情况下运行。
     */
    @BeforeEach
    void setUp() {
        // 创建 Mock Model，配置固定的回复
        MockModel mockModel = new MockModel("你好！我是 AgentScope Java 助手。有什么可以帮助你的吗？");

        // 创建 Mock Agent
        mockAgent = ReActAgent.builder()
                .name("TestAssistant")
                .sysPrompt("你是一个测试助手。")
                .model(mockModel)
                .toolkit(new MockToolkit())
                .maxIters(5)
                .build();

        // 创建 Mock ObjectProvider
        mockAgentProvider = mock(ObjectProvider.class);
        when(mockAgentProvider.getObject()).thenReturn(mockAgent);

        // 创建 AgentService
        agentService = new AgentService(mockAgentProvider);
    }

    /**
     * 测试正常聊天流程.
     *
     * <p>验证：
     * <ul>
     *   <li>能够成功发送消息</li>
     *   <li>返回的响应包含 Agent 回复</li>
     *   <li>响应状态为成功</li>
     * </ul>
     */
    @Test
    @DisplayName("测试正常聊天流程")
    void testChat_Success() {
        // 准备请求
        ChatRequest request = ChatRequest.builder()
                .message("你好")
                .build();

        // 执行
        ChatResponse response = agentService.chat(request);

        // 验证
        assertTrue(response.isSuccess(), "响应状态应为成功");
        assertNotNull(response.getMessage(), "响应消息不应为空");
        assertTrue(response.getMessage().contains("AgentScope"), "响应应包含 AgentScope 关键字");
        assertNotNull(response.getTimestamp(), "时间戳不应为空");
    }

    /**
     * 测试空消息处理.
     *
     * <p>验证：当发送空消息时，服务能够正确处理并返回错误信息。
     */
    @Test
    @DisplayName("测试空消息处理")
    void testChat_EmptyMessage() {
        // 准备请求 - 空消息
        ChatRequest request = ChatRequest.builder()
                .message("")
                .build();

        // 执行
        ChatResponse response = agentService.chat(request);

        // 验证
        assertFalse(response.isSuccess(), "空消息时响应状态应为失败");
        assertTrue(response.getMessage().contains("不能为空"), "错误信息应提示消息不能为空");
    }

    /**
     * 测试 null 请求处理.
     *
     * <p>验证：当请求为 null 时，服务能够正确处理并返回错误信息。
     */
    @Test
    @DisplayName("测试 null 请求处理")
    void testChat_NullRequest() {
        // 执行 - null 请求
        ChatResponse response = agentService.chat(null);

        // 验证
        assertFalse(response.isSuccess(), "null 请求时响应状态应为失败");
        assertTrue(response.getMessage().contains("不能为空"), "错误信息应提示消息不能为空");
    }

    /**
     * 测试只有空白字符的消息.
     *
     * <p>验证：只包含空格、Tab 等空白字符的消息应被视为空消息。
     */
    @Test
    @DisplayName("测试空白字符消息处理")
    void testChat_BlankMessage() {
        // 准备请求 - 只有空白字符
        ChatRequest request = ChatRequest.builder()
                .message("   \t\n  ")
                .build();

        // 执行
        ChatResponse response = agentService.chat(request);

        // 验证
        assertFalse(response.isSuccess(), "空白消息时响应状态应为失败");
    }

    /**
     * 测试 Agent 异常处理.
     *
     * <p>验证：当 Agent 调用发生异常时，服务能够正确捕获并返回错误信息。
     */
    @Test
    @DisplayName("测试 Agent 异常处理")
    void testChat_AgentException() {
        // 创建一个会抛出异常的 Mock Agent
        MockModel errorModel = new MockModel("test");
        errorModel.withError("网络连接失败");

        ReActAgent errorAgent = ReActAgent.builder()
                .name("ErrorAgent")
                .sysPrompt("测试异常")
                .model(errorModel)
                .toolkit(new MockToolkit())
                .maxIters(5)
                .build();

        ObjectProvider<ReActAgent> errorProvider = mock(ObjectProvider.class);
        when(errorProvider.getObject()).thenReturn(errorAgent);

        AgentService errorService = new AgentService(errorProvider);

        ChatRequest request = ChatRequest.builder()
                .message("测试")
                .build();

        // 执行
        ChatResponse response = errorService.chat(request);

        // 验证
        assertFalse(response.isSuccess(), "异常时应返回失败状态");
        assertTrue(response.getMessage().contains("失败") || response.getMessage().contains("error"),
                "错误信息应包含失败相关描述");
    }

    /**
     * 测试 Agent 每次获取新实例.
     *
     * <p>验证：每次 chat 调用都会从 ObjectProvider 获取新的 Agent 实例。
     */
    @Test
    @DisplayName("测试每次获取新 Agent 实例")
    void testChat_NewAgentInstance() {
        // 准备多次请求
        ChatRequest request1 = ChatRequest.builder().message("第一次").build();
        ChatRequest request2 = ChatRequest.builder().message("第二次").build();

        // 执行
        agentService.chat(request1);
        agentService.chat(request2);

        // 验证：ObjectProvider.getObject() 被调用了两次
        verify(mockAgentProvider, times(2)).getObject();
    }

    /**
     * 测试流式聊天接口.
     *
     * <p>验证：流式聊天能够正常返回 Mono 流。
     */
    @Test
    @DisplayName("测试流式聊天接口")
    void testChatStream_Success() {
        // 准备请求
        ChatRequest request = ChatRequest.builder()
                .message("流式测试")
                .build();

        // 执行
        String result = agentService.chatStream(request).block();

        // 验证
        assertNotNull(result, "流式响应结果不应为空");
        assertTrue(result.contains("AgentScope") || !result.isEmpty(),
                "流式响应应包含内容");
    }

    /**
     * 测试流式聊天的空消息处理.
     *
     * <p>验证：空消息时流式接口能够正确处理。
     */
    @Test
    @DisplayName("测试流式聊天的空消息处理")
    void testChatStream_EmptyMessage() {
        // 准备请求 - 空消息
        ChatRequest request = ChatRequest.builder()
                .message("")
                .build();

        // 执行
        String result = agentService.chatStream(request).block();

        // 验证
        assertTrue(result.contains("不能为空"), "空消息时流式接口应返回错误提示");
    }
}