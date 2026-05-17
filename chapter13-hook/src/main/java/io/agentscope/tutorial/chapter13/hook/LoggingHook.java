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
package io.agentscope.tutorial.chapter13.hook;

import io.agentscope.core.hook.ActingChunkEvent;
import io.agentscope.core.hook.ErrorEvent;
import io.agentscope.core.hook.HookEvent;
import io.agentscope.core.hook.PostActingEvent;
import io.agentscope.core.hook.PostCallEvent;
import io.agentscope.core.hook.PostReasoningEvent;
import io.agentscope.core.hook.PostSummaryEvent;
import io.agentscope.core.hook.PreActingEvent;
import io.agentscope.core.hook.PreCallEvent;
import io.agentscope.core.hook.PreReasoningEvent;
import io.agentscope.core.hook.PreSummaryEvent;
import io.agentscope.core.hook.ReasoningChunkEvent;
import io.agentscope.core.hook.Hook;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.message.ToolUseBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 自定义日志钩子 - 记录 Agent 执行的关键生命周期事件.
 *
 * <p>功能特点：
 * <ul>
 *   <li>实现完整的生命周期事件监听</li>
 *   <li>使用 SLF4J 与 Spring Boot 日志框架集成</li>
 *   <li>可配置是否记录流式 chunks</li>
 *   <li>结构化日志输出，便于日志分析</li>
 * </ul>
 *
 * <p>事件处理：
 * <ul>
 *   <li>PreCallEvent - 记录 Agent 调用开始</li>
 *   <li>PostCallEvent - 记录 Agent 调用完成</li>
 *   <li>PreReasoningEvent - 记录推理开始</li>
 *   <li>PostReasoningEvent - 记录推理完成及 ToolCalls</li>
 *   <li>PreActingEvent - 记录工具执行开始</li>
 *   <li>PostActingEvent - 记录工具执行完成</li>
 *   <li>PreSummaryEvent - 记录摘要生成开始</li>
 *   <li>PostSummaryEvent - 记录摘要生成完成</li>
 *   <li>ErrorEvent - 记录执行错误</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 * @see io.agentscope.core.hook.Hook
 */
@SuppressWarnings("unchecked")
public class LoggingHook implements Hook {

    private static final Logger log = LoggerFactory.getLogger(LoggingHook.class);

    /** 是否记录流式 chunks（可能产生大量日志） */
    private final boolean logStreamingChunks;

    /**
     * 默认构造函数 - 不记录流式 chunks.
     */
    public LoggingHook() {
        this(false);
    }

    /**
     * 构造函数.
     *
     * @param logStreamingChunks 是否记录流式 chunks
     */
    public LoggingHook(boolean logStreamingChunks) {
        this.logStreamingChunks = logStreamingChunks;
    }

    /**
     * 钩子优先级：900（低优先级）.
     *
     * <p>确保在业务逻辑之后执行，专注于日志记录
     *
     * @return 优先级数值
     */
    @Override
    public int priority() {
        return 900;
    }

    /**
     * 统一事件处理入口.
     *
     * <p>使用 instanceof 分发到具体处理器
     *
     * @param event 钩子事件
     * @param <T> 事件具体类型
     * @return 包含潜在修改后事件的无状态 Mono
     */
    @Override
    public <T extends HookEvent> Mono<T> onEvent(T event) {
        if (event instanceof PreCallEvent e) {
            return handlePreCall(e);
        } else if (event instanceof PostCallEvent e) {
            return handlePostCall(e);
        } else if (event instanceof PreReasoningEvent e) {
            return handlePreReasoning(e);
        } else if (event instanceof PostReasoningEvent e) {
            return handlePostReasoning(e);
        } else if (event instanceof ReasoningChunkEvent e && logStreamingChunks) {
            return handleReasoningChunk(e);
        } else if (event instanceof PreActingEvent e) {
            return handlePreActing(e);
        } else if (event instanceof PostActingEvent e) {
            return handlePostActing(e);
        } else if (event instanceof ActingChunkEvent e && logStreamingChunks) {
            return handleActingChunk(e);
        } else if (event instanceof PreSummaryEvent e) {
            return handlePreSummary(e);
        } else if (event instanceof PostSummaryEvent e) {
            return handlePostSummary(e);
        } else if (event instanceof ErrorEvent e) {
            return handleError(e);
        }
        return Mono.just(event);
    }

    private <T extends HookEvent> Mono<T> handlePreCall(PreCallEvent e) {
        List<Msg> messages = e.getInputMessages();
        log.info("[{}] Agent 调用开始 | 输入消息数: {}",
                e.getAgent().getName(),
                messages.size());

        for (int i = 0; i < Math.min(3, messages.size()); i++) {
            Msg msg = messages.get(i);
            String preview = truncate(msg.getTextContent(), 50);
            log.debug("  消息[{}]: {} - {}",
                    i, msg.getRole(), preview);
        }

        return (Mono<T>) Mono.just(e);
    }

    private <T extends HookEvent> Mono<T> handlePostCall(PostCallEvent e) {
        Msg finalMsg = e.getFinalMessage();
        String content = finalMsg != null ? finalMsg.getTextContent() : "";
        log.info("[{}] Agent 调用完成 | 响应长度: {} 字符",
                e.getAgent().getName(),
                content.length());

        return (Mono<T>) Mono.just(e);
    }

    private <T extends HookEvent> Mono<T> handlePreReasoning(PreReasoningEvent e) {
        log.debug("[{}] 推理开始 | 模型: {} | 输入消息数: {}",
                e.getAgent().getName(),
                e.getModelName(),
                e.getInputMessages().size());

        return (Mono<T>) Mono.just(e);
    }

    private <T extends HookEvent> Mono<T> handlePostReasoning(PostReasoningEvent e) {
        Msg reasoningMsg = e.getReasoningMessage();

        if (reasoningMsg != null) {
            List<ToolUseBlock> toolCalls = reasoningMsg.getContentBlocks(ToolUseBlock.class);
            String reasoningPreview = truncate(reasoningMsg.getTextContent(), 100);

            log.info("[{}] 推理完成 | 推理内容预览: {}... | ToolCalls: {}",
                    e.getAgent().getName(),
                    reasoningPreview,
                    toolCalls.size());

            for (ToolUseBlock tool : toolCalls) {
                log.debug("  ToolCall: name={}, input={}",
                        tool.getName(),
                        truncate(tool.getInput().toString(), 80));
            }

            if (e.isStopRequested()) {
                log.info("[{}] 推理阶段请求停止 Agent", e.getAgent().getName());
            }

            if (e.isGotoReasoningRequested()) {
                log.info("[{}] 推理阶段请求返回推理", e.getAgent().getName());
            }
        } else {
            log.info("[{}] 推理完成 | 无推理消息（直接返回）",
                    e.getAgent().getName());
        }

        return (Mono<T>) Mono.just(e);
    }

    private <T extends HookEvent> Mono<T> handleReasoningChunk(ReasoningChunkEvent e) {
        Msg chunk = e.getIncrementalChunk();
        String text = chunk != null ? chunk.getTextContent() : "";

        log.trace("[{}] 推理 Chunk | 内容: {}",
                e.getAgent().getName(),
                truncate(text, 200));

        return (Mono<T>) Mono.just(e);
    }

    private <T extends HookEvent> Mono<T> handlePreActing(PreActingEvent e) {
        ToolUseBlock toolUse = e.getToolUse();

        log.info("[{}] 工具执行开始 | 工具: {} | 输入: {}",
                e.getAgent().getName(),
                toolUse.getName(),
                truncate(toolUse.getInput().toString(), 100));

        return (Mono<T>) Mono.just(e);
    }

    private <T extends HookEvent> Mono<T> handlePostActing(PostActingEvent e) {
        ToolUseBlock toolUse = e.getToolUse();
        ToolResultBlock result = e.getToolResult();

        String output = extractResultText(result);
        log.info("[{}] 工具执行完成 | 工具: {} | 结果: {}",
                e.getAgent().getName(),
                toolUse.getName(),
                truncate(output, 150));

        if (e.isStopRequested()) {
            log.info("[{}] 工具执行后请求停止 Agent", e.getAgent().getName());
        }

        return (Mono<T>) Mono.just(e);
    }

    private <T extends HookEvent> Mono<T> handleActingChunk(ActingChunkEvent e) {
        ToolResultBlock chunk = e.getChunk();
        String text = extractResultText(chunk);

        log.trace("[{}] 工具 Chunk | 工具: {} | 内容: {}",
                e.getAgent().getName(),
                e.getToolUse().getName(),
                truncate(text, 100));

        return (Mono<T>) Mono.just(e);
    }

    private <T extends HookEvent> Mono<T> handlePreSummary(PreSummaryEvent e) {
        log.info("[{}] 摘要生成开始 | 迭代次数: {}/{} | 模型: {}",
                e.getAgent().getName(),
                e.getCurrentIteration(),
                e.getMaxIterations(),
                e.getModelName());

        return (Mono<T>) Mono.just(e);
    }

    private <T extends HookEvent> Mono<T> handlePostSummary(PostSummaryEvent e) {
        Msg summaryMsg = e.getSummaryMessage();
        String content = summaryMsg != null ? summaryMsg.getTextContent() : "";

        log.info("[{}] 摘要生成完成 | 摘要长度: {} 字符",
                e.getAgent().getName(),
                content.length());

        return (Mono<T>) Mono.just(e);
    }

    private <T extends HookEvent> Mono<T> handleError(ErrorEvent e) {
        Throwable error = e.getError();

        log.error("[{}] 执行出错 | 类型: {} | 消息: {}",
                e.getAgent().getName(),
                error.getClass().getSimpleName(),
                error.getMessage());

        StackTraceElement[] stackTrace = error.getStackTrace();
        if (stackTrace.length > 0) {
            log.error("  发生在: {}:{}",
                    stackTrace[0].getFileName(),
                    stackTrace[0].getLineNumber());
        }

        return (Mono<T>) Mono.just(e);
    }

    private String extractResultText(ToolResultBlock result) {
        if (result == null || result.getOutput().isEmpty()) {
            return "(empty)";
        }
        return result.getOutput().get(0).toString();
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "(null)";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }
}