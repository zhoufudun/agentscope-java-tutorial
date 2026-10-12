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

package io.agentscope.tutorial.chapter09.controller;

import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.tutorial.chapter09.client.AgentClient;
import io.agentscope.tutorial.chapter09.server.AgentServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * A2A 聊天接口的 REST 控制器
 *
 * <p>本控制器提供 REST API 端点，用于与 A2A Agent 交互。
 * 它在 REST/HTTP 和 A2A 协议之间架起桥梁，允许客户端使用标准 HTTP 与 Agent 通信。
 *
 * <h2>提供的功能</h2>
 * <ul>
 *   <li><b>同步聊天</b>：发送消息并等待完整响应</li>
 *   <li><b>流式响应</b>：使用 SSE（Server-Sent Events）实时获取响应</li>
 *   <li><b>状态查询</b>：获取 Agent 状态和能力信息</li>
 * </ul>
 *
 * <h2>AgentScope 学习要点</h2>
 * <ul>
 *   <li>如何将 A2A 协议集成到 Spring Boot REST 应用中</li>
 *   <li>如何处理 Msg 对象的创建和响应解析</li>
 *   <li>如何使用 SseEmitter 实现流式输出</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final AgentClient agentClient;

    private final AgentServer agentServer;

    /**
     * SSE 超时时间（30 分钟）
     */
    private static final long SSE_TIMEOUT = 30 * 60 * 1000L;

    /**
     * 构造函数注入依赖
     *
     * @param agentClient A2A 客户端，用于与远程 Agent 通信
     * @param agentServer A2A 服务器组件
     */
    @Autowired
    public ChatController(AgentClient agentClient, AgentServer agentServer) {
        this.agentClient = agentClient;
        this.agentServer = agentServer;
    }

    /**
     * 发送聊天消息并获取同步响应
     *
     * <p><b>请求示例：</b>
     * <pre>{@code
     * POST /api/chat
     * Content-Type: application/json
     * {
     *   "message": "你好，计算 2 + 2",
     *   "contextId": "可选的上下文ID"
     * }
     * }</pre>
     *
     * <p><b>响应示例：</b>
     * <pre>{@code
     * {
     *   "contextId": "context-id",
     *   "response": "2 + 2 的结果是 4",
     *   "success": true
     * }
     * }</pre>
     *
     * @param request 聊天请求
     * @return 聊天响应
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> chat(@RequestBody ChatRequest request) {
        try {
            // 如果未提供，生成上下文 ID
            String contextId = request.getContextId() != null
                    ? request.getContextId()
                    : UUID.randomUUID().toString();

            // 创建消息对象
            // Msg 是 AgentScope 中的标准消息格式
            Msg message = Msg.builder()
                    .role(MsgRole.USER)  // 设置角色为用户
                    .content(TextBlock.builder().text(request.getMessage()).build())
                    .build();

            // 发送到远程 Agent
            Msg response = agentClient.chat(message);

            // 提取响应文本
            String responseText = extractTextFromMessage(response);

            return ResponseEntity.ok(Map.of(
                    "contextId", contextId,
                    "response", responseText,
                    "success", true
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * 使用 Server-Sent Events (SSE) 流式获取聊天响应
     *
     * <p>SSE 是一种服务器推送技术，允许服务器向客户端实时推送数据。
     * 适用于需要实时显示 Agent 响应的场景。
     *
     * <p><b>请求示例：</b>
     * <pre>{@code
     * GET /api/chat/stream?message=你好
     * Accept: text/event-stream
     * }</pre>
     *
     * <p><b>响应（SSE 格式）：</b>
     * <pre>{@code
     * data: 你好，我是你的助手...
     * data: 有什么可以帮助你的吗？
     * data: [DONE]
     * }</pre>
     *
     * @param message 要发送的消息
     * @param contextId 可选的上下文 ID
     * @return SSE 发射器用于流式输出
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(
            @RequestParam String message,
            @RequestParam(required = false) String contextId) {

        // 创建 SSE 发射器，设置超时时间
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        // 如果未提供，生成上下文 ID
        String sessionId = contextId != null ? contextId : UUID.randomUUID().toString();

        // 获取流式响应的 Flux
        Flux<String> responseFlux = agentClient.streamChat(message, sessionId);

        // 订阅 Flux 并将数据推送到 SSE
        responseFlux.subscribe(
                chunk -> {
                    if (!chunk.isEmpty()) {
                        try {
                            // 发送 SSE 事件
                            emitter.send(SseEmitter.event()
                                    .name("message")
                                    .data(chunk));
                        } catch (IOException e) {
                            emitter.completeWithError(e);
                        }
                    }
                },
                error -> {
                    // 发生错误时完成 SSE
                    emitter.completeWithError(error);
                },
                () -> {
                    try {
                        // 流结束时发送完成事件
                        emitter.send(SseEmitter.event()
                                .name("done")
                                .data("[DONE]"));
                        emitter.complete();
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                }
        );

        return emitter;
    }

    /**
     * 获取 Agent 的状态和能力信息
     *
     * <p><b>响应示例：</b>
     * <pre>{@code
     * {
     *   "name": "tutorial-a2a-agent",
     *   "status": "ready",
     *   "clientReady": true,
     *   "serverReady": true,
     *   "skills": ["text-chat", "calculation", "weather"]
     * }
     * }</pre>
     *
     * @return Agent 状态信息
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(Map.of(
                "name", agentClient.getAgentName(),
                "status", "ready",
                "clientReady", agentClient.isReady(),
                "serverReady", agentServer.getA2aServer() != null,
                "baseUrl", agentClient.getBaseUrl()
        ));
    }

    /**
     * 从消息中提取文本内容
     *
     * <p>Msg 对象可能包含多个 ContentBlock，此方法遍历所有 TextBlock
     * 并拼接其文本内容。
     *
     * @param message 要提取文本的消息
     * @return 提取的文本内容
     */
    private String extractTextFromMessage(Msg message) {
        if (message == null || message.getContent() == null) {
            return "";
        }

        StringBuilder text = new StringBuilder();
        message.getContent().stream()
                .filter(block -> block instanceof TextBlock)
                .map(block -> (TextBlock) block)
                .forEach(block -> text.append(block.getText()));

        return text.toString();
    }

    /**
     * 聊天请求 DTO
     */
    public static class ChatRequest {
        /** 消息内容 */
        private String message;
        /** 可选的上下文 ID，用于会话跟踪 */
        private String contextId;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getContextId() {
            return contextId;
        }

        public void setContextId(String contextId) {
            this.contextId = contextId;
        }
    }
}