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
 * <p>提供 REST API 端点用于与 AI Agent 交互。
 * 生产环境版本包含以下特性：
 * <ul>
 *   <li>请求验证</li>
 *   <li>限流保护</li>
 *   <li>多租户支持</li>
 *   <li>健康检查集成</li>
 * </ul>
 *
 * @author AgentScope Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;
    private final ProductionConfig.BuildInfo buildInfo;
    private final ProductionConfig.GitInfo gitInfo;

    // 简单的会话存储（生产环境应使用 Redis）
    private final Map<String, SessionContext> sessions = new ConcurrentHashMap<>();

    // SSE 流式处理执行器
    private final ExecutorService sseExecutor = Executors.newCachedThreadPool();

    /**
     * 处理聊天请求.
     */
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        log.info("Received chat request - userId: {}, sessionId: {}, hasAgent: {}",
            request.getUserId(), request.getSessionId(), request.getAgent() != null);

        // 获取或创建会话上下文
        SessionContext context = sessions.computeIfAbsent(
            request.getSessionId(),
            k -> new SessionContext()
        );

        try {
            String response = chatService.chat(
                request.getUserId(),
                request.getSessionId(),
                request.getMessage(),
                request.getAgent(),
                context.getMemory()
            );

            return ResponseEntity.ok(ChatResponse.success(response));

        } catch (Exception e) {
            log.error("Chat processing failed - sessionId: {}", request.getSessionId(), e);
            return ResponseEntity.internalServerError()
                .body(ChatResponse.error(e.getMessage()));
        }
    }

    /**
     * 处理异步聊天请求.
     */
    @PostMapping("/chat/async")
    public ResponseEntity<AsyncChatResponse> chatAsync(@Valid @RequestBody ChatRequest request) {
        log.info("Received async chat request - userId: {}, sessionId: {}",
            request.getUserId(), request.getSessionId());

        String taskId = java.util.UUID.randomUUID().toString();

        // 异步处理
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
            log.info("Async chat completed - taskId: {}, sessionId: {}", taskId, request.getSessionId());
            // 实际生产中应该通过 WebSocket 或轮询推送结果
        });

        return ResponseEntity.accepted()
            .body(AsyncChatResponse.accepted(taskId));
    }

    /**
     * 处理流式聊天请求 (Server-Sent Events).
     */
    @GetMapping(value = "/chat/stream/{sessionId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(
            @PathVariable String sessionId,
            @RequestParam @NotBlank String userId,
            @RequestParam @NotBlank String message,
            ReActAgent agent) {

        log.info("Received stream chat request - userId: {}, sessionId: {}", userId, sessionId);

        SseEmitter emitter = new SseEmitter(300_000L); // 5 分钟超时

        // 获取会话上下文
        SessionContext context = sessions.computeIfAbsent(
            sessionId,
            k -> new SessionContext()
        );

        // 异步处理流式响应
        sseExecutor.execute(() -> {
            try {
                chatService.chatStream(userId, sessionId, message, agent, context.getMemory(),
                    new StreamCallbackHandler(emitter));
            } catch (Exception e) {
                log.error("Stream chat execution failed - sessionId: {}", sessionId, e);
                try {
                    emitter.send(SseEmitter.event()
                        .name("error")
                        .data("处理请求时发生错误"));
                    emitter.completeWithError(e);
                } catch (java.io.IOException ioe) {
                    log.warn("Failed to send error to SSE client", ioe);
                }
            }
        });

        // 处理超时和完成
        emitter.onCompletion(() -> log.debug("SSE stream completed - sessionId: {}", sessionId));
        emitter.onTimeout(() -> log.debug("SSE stream timeout - sessionId: {}", sessionId));
        emitter.onError(e -> log.warn("SSE stream error - sessionId: {}, error: {}", sessionId, e.getMessage()));

        return emitter;
    }

    /**
     * 流式回调处理器.
     */
    private static class StreamCallbackHandler implements ChatService.StreamCallback {
        private final SseEmitter emitter;

        public StreamCallbackHandler(SseEmitter emitter) {
            this.emitter = emitter;
        }

        @Override
        public void onNext(String chunk) {
            try {
                emitter.send(SseEmitter.event()
                    .name("message")
                    .data(chunk));
            } catch (java.io.IOException e) {
                throw new RuntimeException("Failed to send SSE event", e);
            }
        }

        @Override
        public void onComplete() {
            try {
                emitter.send(SseEmitter.event()
                    .name("done")
                    .data("[DONE]"));
                emitter.complete();
            } catch (java.io.IOException e) {
                emitter.completeWithError(e);
            }
        }

        @Override
        public void onError(Throwable error) {
            emitter.completeWithError(error);
        }
    }

    /**
     * 获取服务统计信息.
     */
    @GetMapping("/stats")
    public ResponseEntity<ChatService.ServiceStats> getStats() {
        return ResponseEntity.ok(chatService.getStats());
    }

    /**
     * 获取构建信息.
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
     */
    @DeleteMapping("/session/{sessionId}")
    public ResponseEntity<Void> clearSession(@PathVariable String sessionId) {
        sessions.remove(sessionId);
        log.info("Session cleared - sessionId: {}", sessionId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 会话上下文.
     */
    private static class SessionContext {
        private final Memory memory;

        public SessionContext() {
            this.memory = new io.agentscope.core.memory.InMemoryMemory();
        }

        public Memory getMemory() {
            return memory;
        }
    }

    /**
     * 构建信息响应.
     */
    public record BuildInfoResponse(
        String name,
        String version,
        String buildTime,
        String gitCommit,
        String gitBranch
    ) {}
}