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
package io.agentscope.tutorial.chapter04.hook;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.hook.ActingChunkEvent;
import io.agentscope.core.hook.Hook;
import io.agentscope.core.hook.HookEvent;
import io.agentscope.core.hook.PostActingEvent;
import io.agentscope.core.hook.PostCallEvent;
import io.agentscope.core.hook.PostReasoningEvent;
import io.agentscope.core.hook.PreActingEvent;
import io.agentscope.core.hook.PreCallEvent;
import io.agentscope.core.hook.PreReasoningEvent;
import io.agentscope.core.hook.ReasoningChunkEvent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ThinkingBlock;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.message.ToolUseBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 日志记录钩子
 *
 * 本钩子负责记录 Agent 执行过程中的关键事件，包括：
 * - 推理阶段开始/结束
 * - 工具调用开始/结束
 * - 最终响应完成
 * - 执行时间统计
 *
 * @author AgentScope Tutorial
 */
public class LoggingHook implements Hook {

    private static final Logger log = LoggerFactory.getLogger(LoggingHook.class);

    private Instant callStartTime = Instant.now();
    private final List<String> reasoningSteps = new ArrayList<>();
    private final List<String> toolExecutions = new ArrayList<>();

    @Override
    public <T extends HookEvent> Mono<T> onEvent(T event) {
        return Mono.fromCallable(() -> {
            if (event instanceof PreCallEvent preCall) {
                handlePreCall(preCall);
            } else if (event instanceof PreReasoningEvent preReasoning) {
                handlePreReasoning(preReasoning);
            } else if (event instanceof PostReasoningEvent postReasoning) {
                handlePostReasoning(postReasoning);
            } else if (event instanceof PreActingEvent preActing) {
                handlePreActing(preActing);
            } else if (event instanceof PostActingEvent postActing) {
                handlePostActing(postActing);
            } else if (event instanceof ReasoningChunkEvent reasoningChunk) {
                handleReasoningChunk(reasoningChunk);
            } else if (event instanceof PostCallEvent postCall) {
                handlePostCall(postCall);
            }
            return event;
        });
    }

    private void handlePreCall(PreCallEvent event) {
        callStartTime = Instant.now();
        reasoningSteps.clear();
        toolExecutions.clear();

        log.info("=".repeat(60));
        log.info("Agent 执行开始");
        log.info("Agent 名称: {}", event.getAgent().getName());
        log.info("输入消息数: {}", event.getInputMessages().size());
        if (event.getSystemMessage() != null) {
            String content = event.getSystemMessage().getContentAsString();
            log.info("系统提示词: {}",
                    content.length() > 100 ? content.substring(0, 100) + "..." : content);
        }
        log.info("-".repeat(60));
    }

    private void handlePreReasoning(PreReasoningEvent event) {
        log.info("[推理阶段] 开始 - 模型: {}", event.getModelName());
    }

    private void handlePostReasoning(PostReasoningEvent event) {
        Msg msg = event.getReasoningMessage();
        if (msg == null) {
            log.info("[推理阶段] 结束 - 无推理输出");
            return;
        }

        // 记录思考内容
        List<ThinkingBlock> thinkingBlocks = msg.getContentBlocks(ThinkingBlock.class);
        if (!thinkingBlocks.isEmpty()) {
            String thinking = thinkingBlocks.get(0).getThinking();
            reasoningSteps.add(truncate(thinking, 150));
            log.info("[推理阶段] 结束 - 思考长度: {} 字符", thinking.length());
        }

        // 检查是否有工具调用
        List<ToolUseBlock> toolCalls = msg.getContentBlocks(ToolUseBlock.class);
        if (!toolCalls.isEmpty()) {
            log.info("[推理阶段] 决定调用 {} 个工具:", toolCalls.size());
            for (ToolUseBlock tool : toolCalls) {
                log.info("  -> {} ({})", tool.getName(), tool.getId());
            }
        } else {
            log.info("[推理阶段] 结束 - 无工具调用，将返回最终响应");
        }
    }

    private void handlePreActing(PreActingEvent event) {
        ToolUseBlock toolUse = event.getToolUse();
        log.info("[工具执行] 开始 - 工具: {} ({})", toolUse.getName(), toolUse.getId());
    }

    private void handlePostActing(PostActingEvent event) {
        ToolResultBlock result = event.getToolResult();
        ToolUseBlock toolUse = event.getToolUse();

        String output = result.getOutputAsString();
        toolExecutions.add(String.format("%s: %s",
                toolUse.getName(),
                result.isError() ? "错误" : "成功"));

        if (result.isError()) {
            log.warn("[工具执行] 完成 - {}: 错误 - {}",
                    toolUse.getName(), truncate(output, 100));
        } else {
            log.info("[工具执行] 完成 - {}: 成功", toolUse.getName());
        }
    }

    private void handleReasoningChunk(ReasoningChunkEvent event) {
        Msg chunk = event.getIncrementalChunk();
        var content = chunk.getFirstContentBlock();

        if (content instanceof TextBlock textBlock) {
            // 流式文本输出
            System.out.print(textBlock.getText());
        } else if (content instanceof ThinkingBlock) {
            // 思考内容（不打印，只记录）
        } else if (content instanceof ToolUseBlock) {
            System.out.println();
            log.debug("[流式输出] 工具调用块");
        }
    }

    private void handlePostCall(PostCallEvent event) {
        Duration duration = Duration.between(callStartTime, Instant.now());

        log.info("-".repeat(60));
        log.info("Agent 执行完成");
        log.info("执行时长: {} ms", duration.toMillis());

        if (!reasoningSteps.isEmpty()) {
            log.info("推理步骤数: {}", reasoningSteps.size());
        }

        if (!toolExecutions.isEmpty()) {
            log.info("工具执行:");
            for (String exec : toolExecutions) {
                log.info("  -> {}", exec);
            }
        }

        Msg response = event.getFinalMessage();
        log.info("最终响应长度: {} 字符", response.getContentAsString().length());
        log.info("=".repeat(60));
    }

    @Override
    public int priority() {
        return 500;  // 低优先级，在其他钩子之后执行
    }

    @Override
    public String toString() {
        return "LoggingHook";
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }
}