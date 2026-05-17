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

import io.agentscope.core.hook.ErrorEvent;
import io.agentscope.core.hook.HookEvent;
import io.agentscope.core.hook.PostActingEvent;
import io.agentscope.core.hook.PostCallEvent;
import io.agentscope.core.hook.PostReasoningEvent;
import io.agentscope.core.hook.PreActingEvent;
import io.agentscope.core.hook.PreCallEvent;
import io.agentscope.core.hook.PreReasoningEvent;
import io.agentscope.core.hook.Hook;
import io.agentscope.core.message.ToolUseBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 自定义监控钩子 - 收集 Agent 执行性能指标.
 *
 * <p>收集的指标：
 * <ul>
 *   <li>调用次数和总耗时</li>
 *   <li>推理次数和耗时</li>
 *   <li>工具调用统计（按工具名称分类）</li>
 *   <li>错误统计</li>
 *   <li>推理/摘要切换次数</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 */
@SuppressWarnings("unchecked")
public class MetricsHook implements Hook {

    private static final Logger log = LoggerFactory.getLogger(MetricsHook.class);

    private final AtomicInteger callCount = new AtomicInteger(0);
    private final AtomicLong totalCallTimeMs = new AtomicLong(0);
    private final AtomicInteger reasoningCount = new AtomicInteger(0);
    private final Map<String, AtomicInteger> toolCallCounts = new ConcurrentHashMap<>();
    private final AtomicInteger errorCount = new AtomicInteger(0);
    private final AtomicInteger toolErrorCount = new AtomicInteger(0);

    private Instant currentCallStart;
    private Instant currentReasoningStart;
    private int currentIterationCount = 0;
    private final boolean detailed;

    public MetricsHook() {
        this(false);
    }

    public MetricsHook(boolean detailed) {
        this.detailed = detailed;
        log.info("MetricsHook initialized, detailed={}", detailed);
    }

    @Override
    public int priority() {
        return 950;
    }

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
        } else if (event instanceof PreActingEvent e) {
            return handlePreActing(e);
        } else if (event instanceof PostActingEvent e) {
            return handlePostActing(e);
        } else if (event instanceof ErrorEvent e) {
            return handleError(e);
        }
        return Mono.just(event);
    }

    private <T extends HookEvent> Mono<T> handlePreCall(PreCallEvent e) {
        currentCallStart = Instant.now();
        currentIterationCount = 0;
        callCount.incrementAndGet();

        if (detailed) {
            log.debug("[{}] 指标收集 - Agent 调用开始 #{}",
                    e.getAgent().getName(),
                    callCount.get());
        }

        return (Mono<T>) Mono.just(e);
    }

    private <T extends HookEvent> Mono<T> handlePostCall(PostCallEvent e) {
        if (currentCallStart != null) {
            long durationMs = Duration.between(currentCallStart, Instant.now()).toMillis();
            totalCallTimeMs.addAndGet(durationMs);

            log.info("=== Agent 调用指标 #{} ===", callCount.get());
            log.info("  总耗时: {} ms", durationMs);
            log.info("  迭代次数: {}", currentIterationCount);
            log.info("  推理次数: {}", reasoningCount.get());
            log.info("  错误数: {}", errorCount.get());

            if (!toolCallCounts.isEmpty()) {
                log.info("  工具调用统计:");
                toolCallCounts.forEach((tool, count) ->
                    log.info("    - {}: {} 次", tool, count.get()));
            }

            log.info("  平均耗时: {} ms", getAverageCallTimeMs());
            log.info("===================================");

            currentCallStart = null;
            reasoningCount.set(0);
            toolCallCounts.clear();
            errorCount.set(0);
        }

        return (Mono<T>) Mono.just(e);
    }

    private <T extends HookEvent> Mono<T> handlePreReasoning(PreReasoningEvent e) {
        currentReasoningStart = Instant.now();
        currentIterationCount++;
        reasoningCount.incrementAndGet();

        if (detailed) {
            log.debug("[{}] 指标收集 - 推理开始 #{} (迭代 #{})",
                    e.getAgent().getName(),
                    reasoningCount.get(),
                    currentIterationCount);
        }

        return (Mono<T>) Mono.just(e);
    }

    private <T extends HookEvent> Mono<T> handlePostReasoning(PostReasoningEvent e) {
        if (currentReasoningStart != null) {
            long durationMs = Duration.between(currentReasoningStart, Instant.now()).toMillis();

            if (e.getReasoningMessage() != null) {
                List<ToolUseBlock> toolCalls =
                    e.getReasoningMessage().getContentBlocks(ToolUseBlock.class);
                for (ToolUseBlock tool : toolCalls) {
                    toolCallCounts.computeIfAbsent(tool.getName(), k -> new AtomicInteger())
                            .incrementAndGet();
                }
            }

            if (detailed) {
                log.debug("[{}] 指标收集 - 推理完成 #{}, 耗时: {} ms",
                        e.getAgent().getName(),
                        reasoningCount.get(),
                        durationMs);
            }

            currentReasoningStart = null;
        }

        return (Mono<T>) Mono.just(e);
    }

    private <T extends HookEvent> Mono<T> handlePreActing(PreActingEvent e) {
        ToolUseBlock toolUse = e.getToolUse();

        if (detailed) {
            log.debug("[{}] 指标收集 - 工具执行: {}",
                    e.getAgent().getName(),
                    toolUse.getName());
        }

        return (Mono<T>) Mono.just(e);
    }

    private <T extends HookEvent> Mono<T> handlePostActing(PostActingEvent e) {
        return (Mono<T>) Mono.just(e);
    }

    private <T extends HookEvent> Mono<T> handleError(ErrorEvent e) {
        errorCount.incrementAndGet();

        log.warn("[{}] 指标收集 - 执行错误 #{}: {}",
                e.getAgent().getName(),
                errorCount.get(),
                e.getError().getMessage());

        return (Mono<T>) Mono.just(e);
    }

    public long getAverageCallTimeMs() {
        int count = callCount.get();
        if (count == 0) return 0;
        return totalCallTimeMs.get() / count;
    }

    public int getTotalCallCount() {
        return callCount.get();
    }

    public long getTotalCallTimeMs() {
        return totalCallTimeMs.get();
    }

    public int getTotalErrorCount() {
        return errorCount.get();
    }

    public Map<String, Integer> getToolCallStats() {
        return Map.copyOf(toolCallCounts.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().get()
                )));
    }

    public int getCurrentIterationCount() {
        return currentIterationCount;
    }

    public int getReasoningCount() {
        return reasoningCount.get();
    }

    public void reset() {
        callCount.set(0);
        totalCallTimeMs.set(0);
        reasoningCount.set(0);
        toolCallCounts.clear();
        errorCount.set(0);
        currentCallStart = null;
        currentReasoningStart = null;
        currentIterationCount = 0;

        log.info("指标已重置");
    }

    @Override
    public String toString() {
        return String.format(
                "MetricsHook{totalCalls=%d, avgTime=%dms, errors=%d, tools=%s}",
                callCount.get(),
                getAverageCallTimeMs(),
                errorCount.get(),
                toolCallCounts);
    }
}