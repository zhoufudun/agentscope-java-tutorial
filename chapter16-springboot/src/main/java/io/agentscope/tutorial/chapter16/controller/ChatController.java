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
package io.agentscope.tutorial.chapter16.controller;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.tutorial.chapter16.dto.ChatRequest;
import io.agentscope.tutorial.chapter16.dto.ChatResponse;
import io.agentscope.tutorial.chapter16.model.entity.SessionMessage;
import io.agentscope.tutorial.chapter16.service.ChatService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 聊天 API 控制器.
 *
 * <p>提供对话相关的 HTTP 端点，包括：
 * <ul>
 *   <li>POST /api/v1/agent/chat - 发送对话请求（带会话管理）</li>
 *   <li>GET /api/v1/agent/tools - 获取可用工具列表</li>
 *   <li>GET /api/v1/agent/health - 健康检查</li>
 * </ul>
 *
 * <p>与 /v1/chat/completions 的区别：
 * <ul>
 *   <li>/api/v1/agent/chat - 支持会话管理，自动维护上下文</li>
 *   <li>/v1/chat/completions - 无状态 API，客户端维护上下文</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/agent")
@Slf4j
public class ChatController {

    /** Agent 实例提供器（原型作用域，每次获取新实例） */
    private final ObjectProvider<ReActAgent> agentProvider;

    /** 聊天服务 */
    private final ChatService chatService;

    public ChatController(
            ObjectProvider<ReActAgent> agentProvider,
            ChatService chatService) {
        this.agentProvider = agentProvider;
        this.chatService = chatService;
    }

    /**
     * 发送对话请求（带会话管理）
     *
     * <p>此端点自动维护会话历史，新消息会追加到现有会话中。
     * 适用于需要长期上下文的应用场景。
     *
     * @param sessionId 会话 ID（可选，为空则创建新会话）
     * @param request   聊天请求
     * @return 聊天响应（包含会话 ID 和 Agent 回复）
     */
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(
            @RequestParam(required = false) String sessionId,
            @Valid @RequestBody ChatRequest request) {

        log.info("收到聊天请求: sessionId={}, messageCount={}",
                sessionId, request.getMessages().size());

        try {
            // 获取或创建会话
            String currentSessionId = sessionId;
            if (currentSessionId == null || currentSessionId.isBlank()) {
                currentSessionId = chatService.createSession("默认会话");
                log.info("创建新会话: sessionId={}", currentSessionId);
            }

            // 保存用户消息到会话
            for (ChatRequest.Message msg : request.getMessages()) {
                if ("user".equalsIgnoreCase(msg.getRole())) {
                    chatService.addMessage(currentSessionId, msg.getRole(), msg.getContent());
                }
            }

            // 获取 Agent 实例（原型作用域）
            ReActAgent agent = agentProvider.getObject();

            // 构建消息列表（从会话历史中获取）
            List<SessionMessage> historyMessages = chatService.getMessages(currentSessionId);
            List<Msg> agentMessages = new ArrayList<>();

            // 转换历史消息
            for (SessionMessage msg : historyMessages) {
                MsgRole role = parseRole(msg.getRole());
                agentMessages.add(Msg.builder()
                        .role(role)
                        .content(TextBlock.builder().text(msg.getContent()).build())
                        .build());
            }

            // 添加请求中的新消息
            for (ChatRequest.Message msg : request.getMessages()) {
                MsgRole role = parseRole(msg.getRole());
                agentMessages.add(Msg.builder()
                        .role(role)
                        .content(TextBlock.builder().text(msg.getContent()).build())
                        .build());
            }

            // 调用 Agent
            long startTime = System.currentTimeMillis();
            Msg response = agent.call(agentMessages.toArray(new Msg[0])).block();
            long duration = System.currentTimeMillis() - startTime;

            // 提取回复内容
            String reply = extractTextFromMsg(response);

            log.info("Agent 响应完成: sessionId={}, duration={}ms, replyLength={}",
                    currentSessionId, duration, reply != null ? reply.length() : 0);

            // 保存 Agent 回复到会话
            if (reply != null && !reply.isEmpty()) {
                chatService.addMessage(currentSessionId, "assistant", reply);
            }

            // 更新会话时间
            chatService.updateSessionTime(currentSessionId);

            // 构建响应
            ChatResponse chatResponse = ChatResponse.success(currentSessionId, reply, duration);

            return ResponseEntity.ok(chatResponse);

        } catch (Exception e) {
            log.error("处理聊天请求出错: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ChatResponse.error("Agent 处理失败: " + e.getMessage()));
        }
    }

    /**
     * 获取可用工具列表
     *
     * @return 工具信息列表
     */
    @GetMapping("/tools")
    public ResponseEntity<List<Map<String, Object>>> getTools() {
        ReActAgent agent = agentProvider.getObject();

        List<Map<String, Object>> tools = agent.getToolkit().getToolSchemas().stream()
                .map(schema -> Map.<String, Object>of(
                        "name", schema.getName(),
                        "description", schema.getDescription(),
                        "parameters", schema.getParameters() != null ?
                                schema.getParameters() : Map.of()
                ))
                .toList();

        return ResponseEntity.ok(tools);
    }

    /**
     * 健康检查
     *
     * @return 健康状态
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "agentscope-agent-api",
                "version", "1.0.0"
        ));
    }

    /**
     * 解析角色字符串为 MsgRole 枚举
     */
    private MsgRole parseRole(String roleStr) {
        if (roleStr == null) {
            return MsgRole.USER;
        }
        return switch (roleStr.toLowerCase()) {
            case "system" -> MsgRole.SYSTEM;
            case "assistant" -> MsgRole.ASSISTANT;
            case "tool" -> MsgRole.TOOL;
            default -> MsgRole.USER;
        };
    }

    /**
     * 从消息中提取文本内容
     */
    private String extractTextFromMsg(Msg msg) {
        if (msg == null) {
            return "";
        }
        return msg.getContent().stream()
                .filter(block -> block instanceof TextBlock)
                .map(block -> ((TextBlock) block).getText())
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");
    }
}