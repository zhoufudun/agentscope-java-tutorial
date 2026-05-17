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
import io.agentscope.core.hook.Hook;
import io.agentscope.core.hook.PostActingEvent;
import io.agentscope.core.hook.PostCallEvent;
import io.agentscope.core.hook.PostReasoningEvent;
import io.agentscope.core.hook.PostSummaryEvent;
import io.agentscope.core.hook.PreActingEvent;
import io.agentscope.core.hook.PreCallEvent;
import io.agentscope.core.hook.PreReasoningEvent;
import io.agentscope.core.hook.PreSummaryEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

/**
 * 性能钩子 - 测量 Agent 执行各阶段的耗时.
 *
 * <p>功能特点：
 * <ul>
 *   <li>测量 Agent 调用的总耗时</li>
 *   <li>测量推理阶段耗时</li>
 *   <li>测量工具执行耗时</li>
 *   <li>测量摘要生成耗时</li>
 *   <li>识别性能瓶颈</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 */
@SuppressWarnings("unchecked")
public class TimingHook implements Hook {

    private static final Logger log = LoggerFactory.getLogger(TimingHook.class);

    private Instant callStartTime;
    private Instant reasoningStartTime;
    private Instant toolStartTime;
    private Instant summaryStartTime;
    private String currentToolName;
    private int currentIteration = 0;
    private final boolean verbose;

    public TimingHook() {
        this(true);
    }

    public TimingHook(boolean verbose) {
        this.verbose = verbose;
        log.info("TimingHook initialized, verbose={}", verbose);
    }

    @Override
    public int priority() {
        return 100;
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
        callStartTime = Instant.now();
        currentIteration = 0;

        if (verbose) {
            log.info("[TIMING] Agent '{}' 调用开始", e.getAgent().getName());
        }

        return (Mono<T>) Mono.just(e);
    }

    private <T extends HookEvent> Mono<T> handlePostCall(PostCallEvent e) {
        if (callStartTime != null) {
            long totalDuration = Duration.between(callStartTime, Instant.now()).toMillis();

            log.info("[TIMING] Agent '{}' 调用完成", e.getAgent().getName());
            log.info("[TIMING]   总耗时: {} ms", totalDuration);

            callStartTime = null;
        }

        return (Mono<T>) Mono.just(e);
    }

    private <T extends HookEvent> Mono<T> handlePreReasoning(PreReasoningEvent e) {
        reasoningStartTime = Instant.now();
        currentIteration++;

        if (verbose) {
            log.info("[TIMING] 推理开始 (迭代 #{}) - 模型: {}",
                    currentIteration, e.getModelName());
        }

        return (Mono<T>) Mono.just(e);
    }

    private <T extends HookEvent> Mono<T> handlePostReasoning(PostReasoningEvent e) {
        if (reasoningStartTime != null) {
            long reasoningDuration = Duration.between(reasoningStartTime, Instant.now()).toMillis();

            if (verbose) {
                log.info("[TIMING] 推理完成 (迭代 #{}) - 耗时: {} ms",
                        currentIteration, reasoningDuration);
            }

            reasoningStartTime = null;
        }

        return (Mono<T>) Mono.just(e);
    }

    private <T extends HookEvent> Mono<T> handlePreActing(PreActingEvent e) {
        toolStartTime = Instant.now();
        currentToolName = e.getToolUse().getName();

        if (verbose) {
            log.info("[TIMING] 工具 '{}' 执行开始", currentToolName);
        }

        return (Mono<T>) Mono.just(e);
    }

    private <T extends HookEvent> Mono<T> handlePostActing(PostActingEvent e) {
        if (toolStartTime != null) {
            long toolDuration = Duration.between(toolStartTime, Instant.now()).toMillis();

            if (verbose) {
                log.info("[TIMING] 工具 '{}' 执行完成 - 耗时: {} ms",
                        currentToolName, toolDuration);
            }

            if (toolDuration > 5000) {
                log.warn("[TIMING] 警告: 工具 '{}' 执行耗时过长 ({} ms)",
                        currentToolName, toolDuration);
            }

            toolStartTime = null;
            currentToolName = null;
        }

        return (Mono<T>) Mono.just(e);
    }

    private <T extends HookEvent> Mono<T> handlePreSummary(PreSummaryEvent e) {
        summaryStartTime = Instant.now();

        if (verbose) {
            log.info("[TIMING] 摘要生成开始 - 迭代: {}/{}",
                    e.getCurrentIteration(), e.getMaxIterations());
        }

        return (Mono<T>) Mono.just(e);
    }

    private <T extends HookEvent> Mono<T> handlePostSummary(PostSummaryEvent e) {
        if (summaryStartTime != null) {
            long summaryDuration = Duration.between(summaryStartTime, Instant.now()).toMillis();

            if (verbose) {
                log.info("[TIMING] 摘要生成完成 - 耗时: {} ms", summaryDuration);
            }

            summaryStartTime = null;
        }

        return (Mono<T>) Mono.just(e);
    }

    private <T extends HookEvent> Mono<T> handleError(ErrorEvent e) {
        log.error("[TIMING] 执行出错 - Agent: '{}', 错误: {}",
                e.getAgent().getName(),
                e.getError().getMessage());

        if (callStartTime != null) {
            long elapsed = Duration.between(callStartTime, Instant.now()).toMillis();
            log.error("[TIMING]   出错时已耗时: {} ms", elapsed);
        }

        return (Mono<T>) Mono.just(e);
    }

    public long getCurrentCallElapsed() {
        if (callStartTime == null) return 0;
        return Duration.between(callStartTime, Instant.now()).toMillis();
    }

    public int getCurrentIteration() {
        return currentIteration;
    }
}