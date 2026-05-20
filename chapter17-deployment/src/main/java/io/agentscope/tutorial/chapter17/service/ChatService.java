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
package io.agentscope.tutorial.chapter17.service;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.StreamOptions;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.memory.Memory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * 聊天服务 - 生产环境版本.
 *
 * <p>本服务负责处理用户对话请求，使用 AgentScope 的 ReActAgent 执行 AI 推理。
 * 生产环境版本包含以下特性：
 * <ul>
 *   <li>异步处理支持</li>
 *   <li>多租户隔离</li>
 *   <li>请求限流</li>
 *   <li>指标收集</li>
 * </ul>
 *
 * @author AgentScope Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final Executor agentExecutor;
    private final io.micrometer.core.instrument.MeterRegistry meterRegistry;

    /**
     * 处理用户消息并返回 AI 响应.
     *
     * <p>该方法同步处理请求，适用于需要即时响应的场景。
     *
     * @param userId    用户 ID
     * @param sessionId 会话 ID
     * @param message   用户消息
     * @param agent     ReActAgent 实例
     * @param memory    Memory 实例
     * @return AI 响应
     */
    public String chat(String userId, String sessionId, String message,
                       ReActAgent agent, Memory memory) {
        long startTime = System.currentTimeMillis();

        log.info("Processing chat request - userId: {}, sessionId: {}, messageLength: {}",
            userId, sessionId, message.length());

        try {
            // 记录请求指标
            meterRegistry.counter("agentscope.chat.requests",
                "userId", userId,
                "sessionId", sessionId
            ).increment();

            // 构建用户消息
            Msg userMsg = Msg.builder()
                    .role(MsgRole.USER)
                    .content(TextBlock.builder().text(message).build())
                    .build();

            // 添加用户消息到记忆
            memory.addMessage(userMsg);

            // 执行 Agent 推理
            Msg response = agent.call(List.of(userMsg)).block();

            // 添加 Agent 响应到记忆
            memory.addMessage(response);

            // 记录响应指标
            long duration = System.currentTimeMillis() - startTime;
            meterRegistry.timer("agentscope.chat.duration",
                "userId", userId
            ).record(java.time.Duration.ofMillis(duration));

            // Extract response text
            String responseText = extractTextFromMessage(response);

            log.info("Chat request completed - userId: {}, duration: {}ms, responseLength: {}",
                userId, duration, responseText.length());

            return responseText;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            meterRegistry.counter("agentscope.chat.errors",
                "userId", userId,
                "errorType", e.getClass().getSimpleName()
            ).increment();

            log.error("Chat request failed - userId: {}, duration: {}ms, error: {}",
                userId, duration, e.getMessage(), e);

            throw new RuntimeException("Failed to process chat request", e);
        }
    }

    /**
     * 异步处理用户消息.
     *
     * <p>该方法异步处理请求，适用于长对话或批量处理场景。
     *
     * @param userId    用户 ID
     * @param sessionId 会话 ID
     * @param message   用户消息
     * @param agent     ReActAgent 实例
     * @param memory    Memory 实例
     * @return 包含 AI 响应的 CompletableFuture
     */
    @Async("agentExecutor")
    public CompletableFuture<String> chatAsync(String userId, String sessionId, String message,
                                                ReActAgent agent, Memory memory) {
        log.info("Async chat request started - userId: {}, sessionId: {}", userId, sessionId);

        return CompletableFuture.supplyAsync(() -> {
            try {
                String response = chat(userId, sessionId, message, agent, memory);
                log.info("Async chat request completed - userId: {}, sessionId: {}", userId, sessionId);
                return response;
            } catch (Exception e) {
                log.error("Async chat request failed - userId: {}, sessionId: {}", userId, sessionId, e);
                return "抱歉，处理您的请求时出现错误。";
            }
        }, agentExecutor);
    }

    /**
     * 流式处理用户消息.
     *
     * <p>该方法支持流式响应，适用于需要实时显示 AI 输出场景。
     *
     * @param userId    用户 ID
     * @param sessionId 会话 ID
     * @param message   用户消息
     * @param agent     ReActAgent 实例
     * @param memory    Memory 实例
     * @param callback  流式回调
     */
    public void chatStream(String userId, String sessionId, String message,
                           ReActAgent agent, Memory memory,
                           StreamCallback callback) {
        log.info("Stream chat request started - userId: {}, sessionId: {}", userId, sessionId);

        try {
            meterRegistry.counter("agentscope.chat.stream.requests",
                "userId", userId
            ).increment();

            // 构建用户消息
            Msg userMsg = Msg.builder()
                    .role(MsgRole.USER)
                    .content(TextBlock.builder().text(message).build())
                    .build();

            // 添加用户消息到记忆
            memory.addMessage(userMsg);

            // 使用流式调用 - 获取文本块
            StreamOptions options = StreamOptions.builder()
                    .incremental(true)
                    .build();

            agent.stream(List.of(userMsg), options)
                    .filter(event -> event.getMessage() != null)
                    .flatMap(event -> {
                        Msg msg = event.getMessage();
                        return Flux.fromIterable(msg.getContentBlocks(TextBlock.class));
                    })
                    .map(TextBlock::getText)
                    .subscribe(
                            callback::onNext,
                            callback::onError,
                            callback::onComplete
                    );

        } catch (Exception e) {
            log.error("Stream chat request failed - userId: {}, sessionId: {}", userId, sessionId, e);
            callback.onError(e);
        }
    }

    /**
     * 流式回调接口.
     */
    public interface StreamCallback {
        /**
         * 接收到下一个数据块.
         */
        void onNext(String chunk);

        /**
         * 流式响应完成.
         */
        void onComplete();

        /**
         * 流式响应出错.
         */
        void onError(Throwable error);
    }

    /**
     * 获取服务统计信息.
     */
    public ServiceStats getStats() {
        var timer = meterRegistry.find("agentscope.chat.duration").timer();
        var counter = meterRegistry.find("agentscope.chat.requests").counter();

        return new ServiceStats(
            timer != null ? timer.count() : 0,
            timer != null ? timer.mean(TimeUnit.MILLISECONDS) : 0.0,
            counter != null ? (long) counter.count() : 0
        );
    }

    /**
     * 服务统计信息.
     */
    public record ServiceStats(
        long totalRequests,
        double averageDurationMs,
        long totalTokens
    ) {}

    /**
     * Extract text content from a message.
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
}