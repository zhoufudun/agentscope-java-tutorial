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
package io.agentscope.tutorial.chapter04.service;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.Event;
import io.agentscope.core.agent.EventType;
import io.agentscope.core.agent.StreamOptions;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.tool.ToolUseBlock;
import io.github.pivistechaopeng.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Agent 服务层
 *
 * 提供 Agent 相关的业务逻辑，包括：
 * - 同步消息处理
 * - 流式消息处理
 * - 对话历史管理
 *
 * @author AgentScope Tutorial
 */
@Service
public class AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentService.class);

    private final ReActAgent agent;

    public AgentService(ReActAgent agent) {
        this.agent = agent;
    }

    /**
     * 处理同步消息
     *
     * @param userMessage 用户消息
     * @return Agent 的响应
     */
    public Mono<String> processMessage(String userMessage) {
        log.info("处理消息: {}", StringUtils.abbreviate(userMessage, 50));

        Msg userMsg = Msg.builder()
                .name("user")
                .role(MsgRole.USER)
                .content(TextBlock.builder().text(userMessage).build())
                .build();

        return agent.call(userMsg)
                .map(response -> {
                    String content = response.getContentAsString();
                    log.info("Agent 响应长度: {} 字符", content.length());
                    return content;
                })
                .onErrorResume(e -> {
                    log.error("处理消息时发生错误", e);
                    return Mono.just("处理消息时发生错误: " + e.getMessage());
                });
    }

    /**
     * 处理流式消息
     *
     * @param userMessage 用户消息
     * @return 流式事件
     */
    public Flux<Map<String, Object>> processStreamMessage(String userMessage) {
        log.info("处理流式消息: {}", StringUtils.abbreviate(userMessage, 50));

        Msg userMsg = Msg.builder()
                .name("user")
                .role(MsgRole.USER)
                .content(TextBlock.builder().text(userMessage).build())
                .build();

        StreamOptions options = StreamOptions.builder()
                .stream(EventType.REASONING)
                .stream(EventType.TOOL_RESULT)
                .stream(EventType.AGENT_RESULT)
                .build();

        return agent.stream(userMsg, options)
                .map(event -> Map.of(
                        "type", event.getType().name(),
                        "content", formatEventContent(event),
                        "timestamp", java.time.Instant.now().toString()
                ))
                .doOnComplete(() -> log.info("流式响应完成"))
                .doOnError(e -> log.error("流式响应出错", e));
    }

    /**
     * 获取对话历史
     */
    public List<Map<String, String>> getHistory() {
        List<Msg> messages = agent.getMemory().getMessages();

        return messages.stream()
                .map(msg -> Map.of(
                        "role", msg.getRole().name(),
                        "name", msg.getName() != null ? msg.getName() : "",
                        "content", msg.getContentAsString()
                ))
                .toList();
    }

    /**
     * 清除对话历史
     */
    public void clearHistory() {
        log.info("清除对话历史，当前消息数: {}", agent.getMemory().getMessages().size());
        // 注意：InMemoryMemory 不支持直接清除，需要重新创建
        // 这里通过创建一个新的 memory 实例来"清除"
        agent.getMemory().clear();
    }

    /**
     * 获取 Agent 状态信息
     */
    public Map<String, Object> getStatus() {
        return Map.of(
                "agentId", agent.getAgentId(),
                "name", agent.getName(),
                "description", agent.getDescription(),
                "maxIterations", agent.getMaxIters(),
                "messageCount", agent.getMemory().getMessages().size(),
                "sysPrompt", StringUtils.abbreviate(agent.getSysPrompt(), 200)
        );
    }

    /**
     * 中断 Agent 执行
     */
    public void interrupt() {
        log.info("请求中断 Agent 执行");
        agent.interrupt();
    }

    /**
     * 获取 Agent 实例
     */
    public ReActAgent getAgent() {
        return agent;
    }

    // ========== 私有辅助方法 ==========

    private String formatEventContent(Event event) {
        Msg msg = event.getMessage();
        if (msg == null) return "";

        return switch (event.getType()) {
            case REASONING -> msg.getContentAsString();
            case TOOL_RESULT -> {
                List<ToolResultBlock> toolResults = msg.getContentBlocks(ToolResultBlock.class);
                if (!toolResults.isEmpty()) {
                    yield "工具 [" + toolResults.get(0).getName() + "] 执行完成";
                }
                yield "";
            }
            case HINT -> msg.getContentAsString();
            case AGENT_RESULT -> msg.getContentAsString();
            case SUMMARY -> msg.getContentAsString();
            default -> "";
        };
    }
}