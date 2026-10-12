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
package io.agentscope.tutorial.chapter17.controller;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.Memory;
import io.agentscope.tutorial.chapter17.config.ProductionConfig;
import io.agentscope.tutorial.chapter17.service.ChatService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 聊天控制器 - 生产环境版本.
 *
 * <p>提供 REST API 端点用于与 AI Agent 交互，是 AgentScope 应用的核心对外接口。
 * 本控制器展示了生产环境中构建 AI 聊天服务的最佳实践。
 *
 * <h2>API 端点一览</h2>
 * <table border="1">
 *   <tr><th>方法</th><th>路径</th><th>说明</th></tr>
 *   <tr><td>POST</td><td>/api/v1/chat</td><td>同步聊天（阻塞等待响应）</td></tr>
 *   <tr><td>POST</td><td>/api/v1/chat/async</td><td>异步聊天（立即返回任务ID）</td></tr>
 *   <tr><td>GET</td><td>/api/v1/chat/stream/{sessionId}</td><td>流式聊天（SSE 实时推送）</td></tr>
 *   <tr><td>GET</td><td>/api/v1/stats</td><td>获取服务统计</td></tr>
 *   <tr><td>GET</td><td>/api/v1/info</td><td>获取构建信息</td></tr>
 *   <tr><td>DELETE</td><td>/api/v1/session/{sessionId}</td><td>清除会话</td></tr>
 * </table>
 *
 * <h2>生产环境特性</h2>
 * <ul>
 *   <li><b>请求验证</b> - 使用 Jakarta Validation 进行参数校验</li>
 *   <li><b>限流保护</b> - 可集成 Resilience4j 或 Sentinel 实现</li>
 *   <li><b>多租户支持</b> - 通过 userId 和 sessionId 隔离用户数据</li>
 *   <li><b>健康检查集成</b> - 暴露服务状态供 Kubernetes 探针使用</li>
 *   <li><b>指标收集</b> - 集成 Micrometer 记录请求量、延迟等指标</li>
 * </ul>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * # 同步聊天
 * curl -X POST http://localhost:8080/api/v1/chat \
 *   -H "Content-Type: application/json" \
 *   -d '{"userId":"user1","sessionId":"sess1","message":"你好"}'
 *
 * # 流式聊天（SSE）
 * curl -N http://localhost:8080/api/v1/chat/stream/sess1?userId=user1&message=你好
 * }</pre>
 *
 * @author AgentScope Team
 * @version 1.0.0
 * @see ChatService
 * @see ChatRequest
 * @see ChatResponse
 */
@RestController  // 标记为 REST 控制器，返回值自动序列化为 JSON
@RequestMapping("/api/v1")  // 统一的 API 版本前缀
@RequiredArgsConstructor  // Lombok: 生成包含 final 字段的构造器（依赖注入）
@Slf4j  // Lombok: 自动生成 log 字段
public class ChatController {

    /** 聊天服务，处理核心业务逻辑 */
    private final ChatService chatService;
    /** 构建信息，包含版本号、构建时间等 */
    private final ProductionConfig.BuildInfo buildInfo;
    /** Git 信息，包含提交 ID、分支名等 */
    private final ProductionConfig.GitInfo gitInfo;

    /**
     * 会话存储（内存实现）.
     *
     * <p><b>注意：</b>生产环境应使用 Redis 或其他分布式缓存替代，
     * 以支持多实例部署和会话持久化。
     *
     * <p>使用 {@link ConcurrentHashMap} 确保线程安全，
     * 但在分布式环境下无法共享状态。
     */
    private final Map<String, SessionContext> sessions = new ConcurrentHashMap<>();

    /**
     * SSE 流式处理执行器.
     *
     * <p>使用独立的线程池处理 SSE 连接，避免阻塞 Tomcat 工作线程。
     * CachedThreadPool 适合短生命周期、数量不确定的任务。
     *
     * <p><b>生产环境建议：</b>使用有界线程池，防止连接数过多导致资源耗尽。
     */
    private final ExecutorService sseExecutor = Executors.newCachedThreadPool();

    /**
     * 处理同步聊天请求.
     *
     * <p>这是最基本的聊天端点，客户端发送请求后阻塞等待 AI 响应。
     * 适用于对实时性要求不高、响应时间可预测的场景。
     *
     * <h3>请求流程</h3>
     * <ol>
     *   <li>验证请求参数（{@code @Valid} 触发 Jakarta Validation）</li>
     *   <li>获取或创建会话上下文（包含对话历史）</li>
     *   <li>调用 ChatService 执行 AI 推理</li>
     *   <li>返回 AI 响应或错误信息</li>
     * </ol>
     *
     * <h3>生产环境注意事项</h3>
     * <ul>
     *   <li>AI 推理可能耗时数秒，需配置合适的请求超时</li>
     *   <li>建议配置限流，防止并发请求过多导致服务不可用</li>
     *   <li>监控请求延迟和错误率，设置告警阈值</li>
     * </ul>
     *
     * @param request 聊天请求，包含 userId、sessionId、message 等
     * @return 包含 AI 响应的 {@link ChatResponse}
     */
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        // 记录请求日志，包含关键参数但不包含消息内容（可能含敏感信息）
        log.info("Received chat request - userId: {}, sessionId: {}, hasAgent: {}",
            request.getUserId(), request.getSessionId(), request.getAgent() != null);

        // 使用 computeIfAbsent 原子操作获取或创建会话上下文
        // SessionContext 包含对话历史（Memory），用于多轮对话
        SessionContext context = sessions.computeIfAbsent(
            request.getSessionId(),
            k -> new SessionContext()  // 新会话自动创建 Memory
        );

        try {
            // 调用服务层执行 AI 推理
            String response = chatService.chat(
                request.getUserId(),
                request.getSessionId(),
                request.getMessage(),
                request.getAgent(),
                context.getMemory()  // 传递对话历史，支持上下文理解
            );

            // 返回成功响应（HTTP 200）
            return ResponseEntity.ok(ChatResponse.success(response));

        } catch (Exception e) {
            // 记录错误详情，便于问题排查
            log.error("Chat processing failed - sessionId: {}", request.getSessionId(), e);
            // 返回错误响应（HTTP 500）
            // 生产环境应返回友好的错误消息，不暴露内部细节
            return ResponseEntity.internalServerError()
                .body(ChatResponse.error(e.getMessage()));
        }
    }

    /**
     * 处理异步聊天请求.
     *
     * <p>客户端发送请求后立即返回任务 ID，无需等待 AI 响应。
     * 适用于长时间推理或批量处理场景。
     *
     * <h3>异步模式优势</h3>
     * <ul>
     *   <li><b>快速响应</b> - 客户端无需长时间等待</li>
     *   <li><b>资源优化</b> - 释放 HTTP 连接，支持更多并发</li>
     *   <li><b>弹性处理</b> - 可实现任务队列、重试等机制</li>
     * </ul>
     *
     * <h3>生产环境实现建议</h3>
     * <ul>
     *   <li>使用消息队列（Kafka/RabbitMQ）存储待处理任务</li>
     *   <li>结果通过 WebSocket 推送或提供轮询端点</li>
     *   <li>记录任务状态到 Redis，支持查询和重试</li>
     * </ul>
     *
     * @param request 聊天请求
     * @return HTTP 202 (Accepted) 和任务 ID
     */
    @PostMapping("/chat/async")
    public ResponseEntity<AsyncChatResponse> chatAsync(@Valid @RequestBody ChatRequest request) {
        log.info("Received async chat request - userId: {}, sessionId: {}",
            request.getUserId(), request.getSessionId());

        // 生成唯一任务 ID，用于后续查询结果
        String taskId = java.util.UUID.randomUUID().toString();

        // 异步执行 AI 推理（非阻塞）
        chatService.chatAsync(
            request.getUserId(),
            request.getSessionId(),
            request.getMessage(),
            request.getAgent(),
            sessions.computeIfAbsent(
                request.getSessionId(),
                k -> new SessionContext()
            ).getMemory()
        ).thenAccept(response -> {
            // 推理完成后的回调
            log.info("Async chat completed - taskId: {}, sessionId: {}", taskId, request.getSessionId());
            // 实际生产中应该通过 WebSocket 或轮询推送结果到客户端
            // 或将结果存储到 Redis/数据库供客户端查询
        });

        // 立即返回 HTTP 202 (Accepted)，表示请求已接受但尚未完成
        return ResponseEntity.accepted()
            .body(AsyncChatResponse.accepted(taskId));
    }

    /**
     * 处理流式聊天请求 (Server-Sent Events).
     *
     * <p>使用 SSE（Server-Sent Events）实现实时流式响应，
     * AI 生成的每个 token 立即推送到客户端，提供打字机效果。
     *
     * <h3>SSE 特点</h3>
     * <ul>
     *   <li><b>单向通信</b> - 服务器推送到客户端</li>
     *   <li><b>自动重连</b> - 浏览器内置断线重连机制</li>
     *   <li><b>文本协议</b> - 基于 HTTP，易于调试</li>
     *   <li><b>兼容性好</b> - 大多数浏览器原生支持</li>
     * </ul>
     *
     * <h3>生产环境配置</h3>
     * <ul>
     *   <li>配置 Nginx 禁用响应缓冲：{@code proxy_buffering off}</li>
     *   <li>设置合适的超时时间，防止连接长时间占用</li>
     *   <li>监控活跃 SSE 连接数，设置上限</li>
     * </ul>
     *
     * @param sessionId 会话 ID（路径参数）
     * @param userId    用户 ID（查询参数）
     * @param message   用户消息（查询参数）
     * @param agent     ReActAgent 实例（Spring 注入）
     * @return SSE 事件流发射器
     */
    @GetMapping(value = "/chat/stream/{sessionId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(
            @PathVariable String sessionId,
            @RequestParam @NotBlank String userId,
            @RequestParam @NotBlank String message,
            ReActAgent agent) {

        log.info("Received stream chat request - userId: {}, sessionId: {}", userId, sessionId);

        // 创建 SSE 发射器，设置 5 分钟超时
        // 超时后连接自动关闭，客户端可重新连接
        SseEmitter emitter = new SseEmitter(300_000L);

        // 获取会话上下文
        SessionContext context = sessions.computeIfAbsent(
            sessionId,
            k -> new SessionContext()
        );

        // 在独立线程中执行流式推理，避免阻塞 HTTP 线程
        sseExecutor.execute(() -> {
            try {
                // 调用服务层的流式方法，传入回调处理器
                chatService.chatStream(userId, sessionId, message, agent, context.getMemory(),
                    new StreamCallbackHandler(emitter));
            } catch (Exception e) {
                log.error("Stream chat execution failed - sessionId: {}", sessionId, e);
                try {
                    // 发送错误事件到客户端
                    emitter.send(SseEmitter.event()
                        .name("error")  // 事件名称，客户端可监听 onerror
                        .data("处理请求时发生错误"));
                    emitter.completeWithError(e);
                } catch (java.io.IOException ioe) {
                    log.warn("Failed to send error to SSE client", ioe);
                }
            }
        });

        // 设置生命周期回调，用于监控和资源清理
        emitter.onCompletion(() -> log.debug("SSE stream completed - sessionId: {}", sessionId));
        emitter.onTimeout(() -> log.debug("SSE stream timeout - sessionId: {}", sessionId));
        emitter.onError(e -> log.warn("SSE stream error - sessionId: {}, error: {}", sessionId, e.getMessage()));

        return emitter;
    }

    /**
     * 流式回调处理器.
     *
     * <p>实现 {@link ChatService.StreamCallback} 接口，
     * 将 AI 生成的每个文本块转发到 SSE 客户端。
     *
     * <h3>SSE 事件格式</h3>
     * <ul>
     *   <li>{@code event: message} + {@code data: <文本块>} - 正常文本</li>
     *   <li>{@code event: done} + {@code data: [DONE]} - 流结束标记</li>
     *   <li>{@code event: error} + {@code data: <错误信息>} - 错误事件</li>
     * </ul>
     */
    private static class StreamCallbackHandler implements ChatService.StreamCallback {
        /** SSE 发射器，用于向客户端推送事件 */
        private final SseEmitter emitter;

        /**
         * 构造流式回调处理器.
         *
         * @param emitter SSE 发射器
         */
        public StreamCallbackHandler(SseEmitter emitter) {
            this.emitter = emitter;
        }

        /**
         * 接收到下一个文本块时调用.
         *
         * <p>将 AI 生成的每个 token/文本块立即推送到客户端。
         *
         * @param chunk AI 生成的文本块
         */
        @Override
        public void onNext(String chunk) {
            try {
                // 发送 SSE 事件：event: message, data: <chunk>
                emitter.send(SseEmitter.event()
                    .name("message")  // 事件类型
                    .data(chunk));     // 事件数据
            } catch (java.io.IOException e) {
                // 客户端断开连接时会抛出 IOException
                throw new RuntimeException("Failed to send SSE event", e);
            }
        }

        /**
         * 流式响应完成时调用.
         *
         * <p>发送完成标记并关闭 SSE 连接。
         */
        @Override
        public void onComplete() {
            try {
                // 发送完成事件，遵循 OpenAI 流式 API 的 [DONE] 约定
                emitter.send(SseEmitter.event()
                    .name("done")
                    .data("[DONE]"));
                // 正常关闭 SSE 连接
                emitter.complete();
            } catch (java.io.IOException e) {
                emitter.completeWithError(e);
            }
        }

        /**
         * 流式响应出错时调用.
         *
         * @param error 发生的错误
         */
        @Override
        public void onError(Throwable error) {
            // 以错误状态关闭 SSE 连接
            emitter.completeWithError(error);
        }
    }

    /**
     * 获取服务统计信息.
     *
     * <p>返回聊天服务的运行时统计数据，用于监控和运维。
     *
     * <h3>统计指标</h3>
     * <ul>
     *   <li><b>totalRequests</b> - 总请求数</li>
     *   <li><b>averageDurationMs</b> - 平均响应时间</li>
     *   <li><b>totalTokens</b> - 总 Token 消耗（估算）</li>
     * </ul>
     *
     * <h3>生产环境用途</h3>
     * <ul>
     *   <li>接入 Prometheus + Grafana 可视化监控</li>
     *   <li>设置告警规则（如响应时间 > 5s）</li>
     *   <li>分析服务负载，指导扩缩容决策</li>
     * </ul>
     *
     * @return 服务统计信息
     */
    @GetMapping("/stats")
    public ResponseEntity<ChatService.ServiceStats> getStats() {
        return ResponseEntity.ok(chatService.getStats());
    }

    /**
     * 获取构建信息.
     *
     * <p>返回应用的构建和版本信息，用于运维和问题排查。
     *
     * <h3>返回字段</h3>
     * <ul>
     *   <li><b>name</b> - 应用名称</li>
     *   <li><b>version</b> - 版本号（如 1.0.0）</li>
     *   <li><b>buildTime</b> - 构建时间</li>
     *   <li><b>gitCommit</b> - Git 提交 ID</li>
     *   <li><b>gitBranch</b> - Git 分支名</li>
     * </ul>
     *
     * <h3>典型用途</h3>
     * <ul>
     *   <li>确认部署的版本是否正确</li>
     *   <li>通过 commitId 追溯代码变更</li>
     *   <li>集成到健康检查页面</li>
     * </ul>
     *
     * @return 构建信息响应
     */
    @GetMapping("/info")
    public ResponseEntity<BuildInfoResponse> getInfo() {
        return ResponseEntity.ok(new BuildInfoResponse(
            buildInfo.name(),
            buildInfo.version(),
            buildInfo.buildTime() != null ? buildInfo.buildTime().toString() : "unknown",
            gitInfo != null ? gitInfo.commitId() : "unknown",
            gitInfo != null ? gitInfo.branch() : "unknown"
        ));
    }

    /**
     * 清除会话.
     *
     * <p>删除指定会话的上下文数据（对话历史），
     * 用于实现"开始新对话"功能或会话超时清理。
     *
     * <h3>生产环境注意事项</h3>
     * <ul>
     *   <li>应实现身份验证，防止用户删除他人会话</li>
     *   <li>考虑软删除，保留数据用于审计</li>
     *   <li>分布式环境需清除 Redis 中的会话数据</li>
     * </ul>
     *
     * @param sessionId 要清除的会话 ID
     * @return HTTP 204 No Content
     */
    @DeleteMapping("/session/{sessionId}")
    public ResponseEntity<Void> clearSession(@PathVariable String sessionId) {
        // 从内存中移除会话上下文
        sessions.remove(sessionId);
        log.info("Session cleared - sessionId: {}", sessionId);
        // 返回 204 No Content，表示操作成功但无返回内容
        return ResponseEntity.noContent().build();
    }

    /**
     * 会话上下文.
     *
     * <p>封装单个会话的状态信息，包括对话历史（Memory）。
     * 在多轮对话中保持上下文，使 AI 能够理解对话历史。
     *
     * <h3>内存管理</h3>
     * <ul>
     *   <li>默认使用 InMemoryMemory，所有历史存储在内存中</li>
     *   <li>生产环境可替换为 RedisMemory 实现持久化</li>
     *   <li>应设置消息数量上限，防止内存溢出</li>
     * </ul>
     */
    private static class SessionContext {
        /** 对话历史存储 */
        private final Memory memory;

        /**
         * 创建新的会话上下文.
         *
         * <p>初始化 InMemoryMemory 用于存储对话历史。
         */
        public SessionContext() {
            // 使用 AgentScope 提供的内存实现
            this.memory = new io.agentscope.core.memory.InMemoryMemory();
        }

        /**
         * 获取对话历史.
         *
         * @return Memory 实例
         */
        public Memory getMemory() {
            return memory;
        }
    }

    /**
     * 构建信息响应.
     *
     * <p>使用 Java Record 定义不可变的响应对象，
     * Jackson 会自动将其序列化为 JSON。
     *
     * @param name      应用名称
     * @param version   版本号
     * @param buildTime 构建时间（ISO 8601 格式）
     * @param gitCommit Git 提交 ID
     * @param gitBranch Git 分支名
     */
    public record BuildInfoResponse(
        String name,
        String version,
        String buildTime,
        String gitCommit,
        String gitBranch
    ) {}
}