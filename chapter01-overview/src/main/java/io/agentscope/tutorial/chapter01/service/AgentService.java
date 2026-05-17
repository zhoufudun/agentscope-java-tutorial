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
package io.agentscope.tutorial.chapter01.service;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.tutorial.chapter01.controller.ChatRequest;
import io.agentscope.tutorial.chapter01.controller.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Agent 服务类.
 *
 * <p>本服务封装了 Agent 的调用逻辑，提供：
 * <ul>
 *   <li>单轮对话（chat）- 适合简单问答场景</li>
 *   <li>流式对话（chatStream）- 适合需要实时响应的场景</li>
 * </ul>
 *
 * <p>注意：ReActAgent 是非线程安全的，每个会话应该使用独立的 Agent 实例。
 * 本服务使用 {@link ObjectProvider} 来获取 prototype scope 的 Agent 实例。
 *
 * @see ReActAgent
 * @see ChatRequest
 * @see ChatResponse
 */
@Service
@Slf4j
public class AgentService {

    /** Agent 实例提供器（prototype scope，每个请求获取新实例） */
    private final ObjectProvider<ReActAgent> agentProvider;

    /**
     * 构造函数.
     *
     * @param agentProvider Agent 实例提供器，由 Spring Boot 自动注入
     */
    public AgentService(ObjectProvider<ReActAgent> agentProvider) {
        this.agentProvider = agentProvider;
    }

    /**
     * 处理用户消息并返回 Agent 回复.
     *
     * <p>本方法执行以下步骤：
     * <ol>
     *   <li>获取一个新的 Agent 实例</li>
     *   <li>将用户消息封装成 Msg 对象</li>
     *   <li>调用 Agent 的 call 方法获取回复</li>
     *   <li>解析回复内容并返回</li>
     * </ol>
     *
     * @param request 聊天请求，包含用户消息
     * @return 聊天响应，包含 Agent 回复
     */
    public ChatResponse chat(ChatRequest request) {
        // 参数校验
        if (request == null || StringUtils.isBlank(request.getMessage())) {
            return ChatResponse.error("消息内容不能为空");
        }

        // 获取新的 Agent 实例（每个请求一个独立实例）
        ReActAgent agent = agentProvider.getObject();

        try {
            log.info("收到用户消息: {}", request.getMessage());

            // 构建用户消息
            Msg userMsg = Msg.builder()
                    .role(MsgRole.USER)
                    .content(TextBlock.builder().text(request.getMessage()).build())
                    .build();

            // 调用 Agent 获取回复
            Msg response = agent.call(userMsg).block();

            // 解析回复内容
            String reply = extractTextFromMsg(response);
            log.info("Agent 回复: {}", reply);

            return ChatResponse.success(reply);

        } catch (Exception e) {
            log.error("调用 Agent 出错: {}", e.getMessage(), e);
            return ChatResponse.error("Agent 处理失败: " + e.getMessage());
        }
    }

    /**
     * 流式处理用户消息.
     *
     * <p>本方法使用流式响应，适合需要实时展示 Agent 思考过程的场景。
     *
     * @param request 聊天请求
     * @return Agent 回复的 Mono 流
     */
    public Mono<String> chatStream(ChatRequest request) {
        if (request == null || StringUtils.isBlank(request.getMessage())) {
            return Mono.just("消息内容不能为空");
        }

        ReActAgent agent = agentProvider.getObject();

        Msg userMsg = Msg.builder()
                .role(MsgRole.USER)
                .content(TextBlock.builder().text(request.getMessage()).build())
                .build();

        return agent.stream(userMsg)
                .map(event -> {
                    // 这里可以处理流式事件，如思考过程、工具调用结果等
                    Msg msg = event.getMessage();
                    return extractTextFromMsg(msg);
                });
    }

    /**
     * 从消息中提取文本内容.
     *
     * @param msg 消息对象
     * @return 文本内容
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