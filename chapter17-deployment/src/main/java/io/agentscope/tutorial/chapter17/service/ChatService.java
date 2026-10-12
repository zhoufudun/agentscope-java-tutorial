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
 * <p>本服务是 AgentScope 应用的核心业务层，负责处理用户对话请求，
 * 使用 AgentScope 的 ReActAgent 执行 AI 推理。
 *
 * <h2>核心功能</h2>
 * <ul>
 *   <li><b>同步聊天</b> - {@link #chat} 阻塞等待 AI 响应</li>
 *   <li><b>异步聊天</b> - {@link #chatAsync} 非阻塞，返回 CompletableFuture</li>
 *   <li><b>流式聊天</b> - {@link #chatStream} 实时推送每个 token</li>
 * </ul>
 *
 * <h2>生产环境特性</h2>
 * <ul>
 *   <li><b>异步处理支持</b> - 使用独立线程池执行 AI 推理，不阻塞主线程</li>
 *   <li><b>多租户隔离</b> - 通过 userId 和 sessionId 隔离不同用户的对话</li>
 *   <li><b>请求限流</b> - 可集成 Resilience4j 实现限流和熔断</li>
 *   <li><b>指标收集</b> - 集成 Micrometer 记录请求量、延迟、错误等指标</li>
 * </ul>
 *
 * <h2>架构设计</h2>
 * <pre>{@code
 * Controller           Service              Agent
 *    |                    |                    |
 *    |--- chat() -------->|                    |
 *    |                    |--- call() -------->|
 *    |                    |<-- Msg ------------|
 *    |<-- String ---------|                    |
 * }</pre>
 *
 * <h2>指标监控</h2>
 * <p>本服务暴露以下 Micrometer 指标：
 * <ul>
 *   <li>{@code agentscope.chat.requests} - 请求计数器</li>
 *   <li>{@code agentscope.chat.duration} - 响应时间计时器</li>
 *   <li>{@code agentscope.chat.errors} - 错误计数器</li>
 *   <li>{@code agentscope.chat.stream.requests} - 流式请求计数器</li>
 * </ul>
 *
 * @author AgentScope Team
 * @version 1.0.0
 * @see io.agentscope.core.ReActAgent
 * @see io.agentscope.core.memory.Memory
 */
@Service  // 标记为 Spring 服务组件
@RequiredArgsConstructor  // Lombok: 生成包含 final 字段的构造器
@Slf4j  // Lombok: 自动生成 log 字段
public class ChatService {

    /**
     * Agent 执行器（线程池）.
     *
     * <p>由 {@link io.agentscope.tutorial.chapter17.config.ProductionConfig#agentExecutor()}
     * 配置，专用于 AI 推理任务。
     */
    private final Executor agentExecutor;

    /**
     * Micrometer 指标注册表.
     *
     * <p>用于记录应用指标，可与 Prometheus、Graphite 等监控系统集成。
     */
    private final io.micrometer.core.instrument.MeterRegistry meterRegistry;

    /**
     * 处理用户消息并返回 AI 响应（同步方式）.
     *
     * <p>该方法同步处理请求，调用线程将阻塞直到 AI 完成推理。
     * 适用于需要即时响应的场景，如 REST API 的同步端点。
     *
     * <h3>处理流程</h3>
     * <ol>
     *   <li>记录请求开始时间和指标</li>
     *   <li>构建用户消息（Msg）</li>
     *   <li>将消息添加到对话历史（Memory）</li>
     *   <li>调用 Agent 执行 AI 推理</li>
     *   <li>将 AI 响应添加到对话历史</li>
     *   <li>记录响应时间指标</li>
     *   <li>返回响应文本</li>
     * </ol>
     *
     * <h3>生产环境注意事项</h3>
     * <ul>
     *   <li>AI 推理可能耗时数秒，需配置请求超时</li>
     *   <li>监控 {@code agentscope.chat.duration} 指标设置告警</li>
     *   <li>考虑使用异步或流式方式减少等待时间</li>
     * </ul>
     *
     * @param userId    用户 ID，用于多租户隔离和指标标签
     * @param sessionId 会话 ID，用于关联对话历史
     * @param message   用户消息内容
     * @param agent     ReActAgent 实例，执行 AI 推理
     * @param memory    Memory 实例，存储对话历史
     * @return AI 响应文本
     * @throws RuntimeException 如果 AI 推理失败
     */
    public String chat(String userId, String sessionId, String message,
                       ReActAgent agent, Memory memory) {
        // 记录请求开始时间，用于计算响应延迟
        long startTime = System.currentTimeMillis();

        log.info("Processing chat request - userId: {}, sessionId: {}, messageLength: {}",
            userId, sessionId, message.length());

        try {
            // 记录请求指标 - 用于监控请求量和分布
            // 标签包含 userId 和 sessionId，支持按用户/会话维度分析
            meterRegistry.counter("agentscope.chat.requests",
                "userId", userId,
                "sessionId", sessionId
            ).increment();

            // 构建用户消息对象
            // Msg 是 AgentScope 的核心消息类型，包含角色、内容等信息
            Msg userMsg = Msg.builder()
                    .role(MsgRole.USER)  // 标记为用户消息
                    .content(TextBlock.builder().text(message).build())  // 文本内容
                    .build();

            // 添加用户消息到对话记忆
            // Memory 存储对话历史，支持多轮对话的上下文理解
            memory.addMessage(userMsg);

            // 执行 Agent 推理（核心 AI 调用）
            // block() 会阻塞当前线程直到推理完成
            // 生产环境可考虑设置超时：.block(Duration.ofSeconds(30))
            Msg response = agent.call(List.of(userMsg)).block();

            // 添加 Agent 响应到对话记忆
            // 这样后续对话可以引用之前的回答
            memory.addMessage(response);

            // 记录响应时间指标 - 用于监控 AI 推理性能
            long duration = System.currentTimeMillis() - startTime;
            meterRegistry.timer("agentscope.chat.duration",
                "userId", userId
            ).record(java.time.Duration.ofMillis(duration));

            // 从 Msg 中提取文本内容
            String responseText = extractTextFromMessage(response);

            log.info("Chat request completed - userId: {}, duration: {}ms, responseLength: {}",
                userId, duration, responseText.length());

            return responseText;

        } catch (Exception e) {
            // 记录错误指标 - 用于监控错误率和类型分布
            long duration = System.currentTimeMillis() - startTime;
            meterRegistry.counter("agentscope.chat.errors",
                "userId", userId,
                "errorType", e.getClass().getSimpleName()  // 按错误类型分类
            ).increment();

            log.error("Chat request failed - userId: {}, duration: {}ms, error: {}",
                userId, duration, e.getMessage(), e);

            // 重新抛出异常，由调用方处理
            throw new RuntimeException("Failed to process chat request", e);
        }
    }

    /**
     * 异步处理用户消息.
     *
     * <p>该方法异步处理请求，立即返回 {@link CompletableFuture}，
     * 调用线程不会被阻塞。适用于长对话或批量处理场景。
     *
     * <h3>异步模式优势</h3>
     * <ul>
     *   <li><b>非阻塞</b> - 调用线程可以继续处理其他请求</li>
     *   <li><b>资源优化</b> - 减少线程等待，提高系统吞吐量</li>
     *   <li><b>灵活处理</b> - 支持超时、重试、组合等高级操作</li>
     * </ul>
     *
     * <h3>使用示例</h3>
     * <pre>{@code
     * chatService.chatAsync(userId, sessionId, message, agent, memory)
     *     .thenAccept(response -> System.out.println("Response: " + response))
     *     .exceptionally(e -> { System.err.println("Error: " + e); return null; });
     * }</pre>
     *
     * <h3>生产环境注意事项</h3>
     * <ul>
     *   <li>设置合理的超时：{@code future.orTimeout(30, TimeUnit.SECONDS)}</li>
     *   <li>处理异常：{@code future.exceptionally(...)}</li>
     *   <li>监控任务队列长度，防止堆积</li>
     * </ul>
     *
     * @param userId    用户 ID
     * @param sessionId 会话 ID
     * @param message   用户消息
     * @param agent     ReActAgent 实例
     * @param memory    Memory 实例
     * @return 包含 AI 响应的 CompletableFuture
     */
    @Async("agentExecutor")  // 使用指定的线程池执行
    public CompletableFuture<String> chatAsync(String userId, String sessionId, String message,
                                                ReActAgent agent, Memory memory) {
        log.info("Async chat request started - userId: {}, sessionId: {}", userId, sessionId);

        // 使用 CompletableFuture.supplyAsync 在指定线程池中执行任务
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 复用同步方法的逻辑
                String response = chat(userId, sessionId, message, agent, memory);
                log.info("Async chat request completed - userId: {}, sessionId: {}", userId, sessionId);
                return response;
            } catch (Exception e) {
                log.error("Async chat request failed - userId: {}, sessionId: {}", userId, sessionId, e);
                // 返回友好的错误消息，而不是抛出异常
                return "抱歉，处理您的请求时出现错误。";
            }
        }, agentExecutor);  // 指定执行线程池
    }

    /**
     * 流式处理用户消息.
     *
     * <p>该方法支持流式响应，AI 生成的每个 token 通过回调实时推送。
     * 适用于需要实时显示 AI 输出的场景，如聊天界面的打字机效果。
     *
     * <h3>流式模式优势</h3>
     * <ul>
     *   <li><b>即时反馈</b> - 用户可以立即看到部分响应</li>
     *   <li><b>感知体验</b> - 减少等待焦虑，提升用户体验</li>
     *   <li><b>提前中断</b> - 用户可以在生成过程中取消</li>
     * </ul>
     *
     * <h3>回调方法</h3>
     * <ul>
     *   <li>{@code onNext(chunk)} - 接收到新的文本块</li>
     *   <li>{@code onComplete()} - 生成完成</li>
     *   <li>{@code onError(error)} - 发生错误</li>
     * </ul>
     *
     * <h3>生产环境注意事项</h3>
     * <ul>
     *   <li>监控活跃流的数量，防止资源耗尽</li>
     *   <li>设置流超时，防止连接长时间占用</li>
     *   <li>处理客户端断开连接的情况</li>
     * </ul>
     *
     * @param userId    用户 ID
     * @param sessionId 会话 ID
     * @param message   用户消息
     * @param agent     ReActAgent 实例
     * @param memory    Memory 实例
     * @param callback  流式回调，接收生成的文本块
     */
    public void chatStream(String userId, String sessionId, String message,
                           ReActAgent agent, Memory memory,
                           StreamCallback callback) {
        log.info("Stream chat request started - userId: {}, sessionId: {}", userId, sessionId);

        try {
            // 记录流式请求指标
            meterRegistry.counter("agentscope.chat.stream.requests",
                "userId", userId
            ).increment();

            // 构建用户消息
            Msg userMsg = Msg.builder()
                    .role(MsgRole.USER)
                    .content(TextBlock.builder().text(message).build())
                    .build();

            // 添加用户消息到对话记忆
            memory.addMessage(userMsg);

            // 配置流式选项
            // incremental=true 表示增量返回（每次只返回新生成的内容）
            StreamOptions options = StreamOptions.builder()
                    .incremental(true)
                    .build();

            // 使用响应式流处理 AI 推理结果
            // agent.stream() 返回 Flux<StreamEvent>，支持响应式操作
            agent.stream(List.of(userMsg), options)
                    // 过滤出包含消息的事件
                    .filter(event -> event.getMessage() != null)
                    // 从事件中提取文本块
                    .flatMap(event -> {
                        Msg msg = event.getMessage();
                        // 获取消息中的所有 TextBlock
                        return Flux.fromIterable(msg.getContentBlocks(TextBlock.class));
                    })
                    // 提取文本内容
                    .map(TextBlock::getText)
                    // 订阅并处理流
                    .subscribe(
                            callback::onNext,     // 每个文本块调用 onNext
                            callback::onError,    // 错误时调用 onError
                            callback::onComplete  // 完成时调用 onComplete
                    );

        } catch (Exception e) {
            log.error("Stream chat request failed - userId: {}, sessionId: {}", userId, sessionId, e);
            // 通过回调通知错误
            callback.onError(e);
        }
    }

    /**
     * 流式回调接口.
     *
     * <p>定义流式响应的回调方法，用于处理 AI 生成的增量内容。
     * 实现类可以是 SSE 发射器、WebSocket 处理器等。
     *
     * <h3>实现示例</h3>
     * <pre>{@code
     * new StreamCallback() {
     *     @Override
     *     public void onNext(String chunk) {
     *         System.out.print(chunk); // 打印每个文本块
     *     }
     *
     *     @Override
     *     public void onComplete() {
     *         System.out.println("\n[完成]");
     *     }
     *
     *     @Override
     *     public void onError(Throwable error) {
     *         System.err.println("错误: " + error.getMessage());
     *     }
     * }
     * }</pre>
     */
    public interface StreamCallback {
        /**
         * 接收到下一个数据块.
         *
         * <p>每当 AI 生成新的文本块时调用。
         * 实现应该尽快处理并返回，避免阻塞生成流程。
         *
         * @param chunk AI 生成的文本块
         */
        void onNext(String chunk);

        /**
         * 流式响应完成.
         *
         * <p>当 AI 完成所有文本生成后调用。
         * 实现应该执行清理操作，如关闭连接、发送结束标记等。
         */
        void onComplete();

        /**
         * 流式响应出错.
         *
         * <p>当生成过程中发生错误时调用。
         * 实现应该通知客户端错误并进行清理。
         *
         * @param error 发生的错误
         */
        void onError(Throwable error);
    }

    /**
     * 获取服务统计信息.
     *
     * <p>从 Micrometer 注册表读取指标数据，用于监控和运维。
     * 这些数据也可以通过 Prometheus 端点 {@code /actuator/prometheus} 获取。
     *
     * <h3>返回指标</h3>
     * <ul>
     *   <li><b>totalRequests</b> - 历史总请求数</li>
     *   <li><b>averageDurationMs</b> - 平均响应时间（毫秒）</li>
     *   <li><b>totalTokens</b> - 总请求计数（与 totalRequests 相同）</li>
     * </ul>
     *
     * @return 服务统计信息
     */
    public ServiceStats getStats() {
        // 查找响应时间计时器
        var timer = meterRegistry.find("agentscope.chat.duration").timer();
        // 查找请求计数器
        var counter = meterRegistry.find("agentscope.chat.requests").counter();

        return new ServiceStats(
            timer != null ? timer.count() : 0,                    // 请求数
            timer != null ? timer.mean(TimeUnit.MILLISECONDS) : 0.0,  // 平均响应时间
            counter != null ? (long) counter.count() : 0          // 总请求数
        );
    }

    /**
     * 服务统计信息.
     *
     * <p>使用 Java Record 定义不可变的统计数据对象。
     *
     * @param totalRequests      历史总请求数
     * @param averageDurationMs  平均响应时间（毫秒）
     * @param totalTokens        总 Token 消耗（此处为请求计数）
     */
    public record ServiceStats(
        long totalRequests,
        double averageDurationMs,
        long totalTokens
    ) {}

    /**
     * 从消息中提取文本内容.
     *
     * <p>AgentScope 的 {@link Msg} 可以包含多种类型的内容块，
     * 此方法从中提取所有 {@link TextBlock} 的文本并拼接。
     *
     * <h3>内容块类型</h3>
     * <ul>
     *   <li>{@link TextBlock} - 文本内容</li>
     *   <li>ImageBlock - 图片内容</li>
     *   <li>ToolUseBlock - 工具调用</li>
     *   <li>ToolResultBlock - 工具结果</li>
     * </ul>
     *
     * @param message AI 响应消息
     * @return 提取的文本内容，如果没有文本则返回空字符串
     */
    private String extractTextFromMessage(Msg message) {
        // 处理空消息
        if (message == null || message.getContent() == null) {
            return "";
        }

        // 使用 StringBuilder 拼接所有文本块
        StringBuilder text = new StringBuilder();
        message.getContent().stream()
                // 过滤出 TextBlock 类型的内容
                .filter(block -> block instanceof TextBlock)
                .map(block -> (TextBlock) block)
                // 拼接文本
                .forEach(block -> text.append(block.getText()));

        return text.toString();
    }
}