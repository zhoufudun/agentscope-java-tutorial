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

package io.agentscope.tutorial.chapter09.controller;

import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import io.agentscope.tutorial.chapter09.client.AgentClient;
import io.agentscope.tutorial.chapter09.server.AgentServer;
import org.springframework.beans.factory.annotation.自动注入;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * REST Controller for A2A Chat interface.
 *
 * <p>This controller provides REST API endpoints for interacting
 * with the A2A agent, including:
 * <ul>
 *   <li>Synchronous chat requests</li>
 *   <li>Server-Sent Events (SSE) for streaming responses</li>
 *   <li>Agent status and capability queries</li>
 * </ul>
 *
 * <p>The controller bridges between REST/HTTP and the A2A protocol,
 * allowing clients to interact with the agent using standard HTTP.
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final AgentClient agentClient;

    private final AgentServer agentServer;

    /**
     * SSE timeout duration.
     */
    private static final long SSE_TIMEOUT = 30 * 60 * 1000L; // 30 minutes

    /**
     * 构造函数 injection.
     *
     * @param agentClient The A2A client for remote agent communication
     * @param agentServer The A2A server component
     */
    @Autowired
    public ChatController(AgentClient agentClient, AgentServer agentServer) {
        this.agentClient = agentClient;
        this.agentServer = agentServer;
    }

    /**
     * Send a chat message and get a synchronous response.
     *
     * <p><b>Request:</b>
     * <pre>{@code
     * POST /api/chat
     * Content-Type: application/json
     * {
     *   "message": "Hello, calculate 2 + 2",
     *   "contextId": "optional-context-id"
     * }
     * }</pre>
     *
     * <p><b>Response:</b>
     * <pre>{@code
     * {
     *   "contextId": "context-id",
     *   "response": "The result of 2 + 2 is 4",
     *   "success": true
     * }
     * }</pre>
     *
     * @param request The chat request
     * @return The chat response
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> chat(@RequestBody ChatRequest request) {
        try {
            // Generate context ID if not provided
            String contextId = request.getContextId() != null
                    ? request.getContextId()
                    : UUID.randomUUID().toString();

            // Create message
            Msg message = Msg.of(request.getMessage(), contextId);

            // Send to remote agent
            Msg response = agentClient.chat(message);

            // Extract response text
            String responseText = extractTextFromMessage(response);

            return ResponseEntity.ok(Map.of(
                    "contextId", contextId,
                    "response", responseText,
                    "success", true
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Stream chat responses using Server-Sent Events (SSE).
     *
     * <p><b>Request:</b>
     * <pre>{@code
     * GET /api/chat/stream?message=Hello
     * Accept: text/event-stream
     * }</pre>
     *
     * <p><b>Response (SSE format):</b>
     * <pre>{@code
     * data: Hello, I'm your assistant...
     * data: How can I help you today?
     * data: [DONE]
     * }</pre>
     *
     * @param message The message to send
     * @param contextId Optional context ID
     * @return SSE emitter for streaming
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(
            @Request参数 String message,
            @RequestParam(required = false) String contextId) {

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        // Generate context ID if not provided
        String sessionId = contextId != null ? contextId : UUID.randomUUID().toString();

        Flux<String> responseFlux = agentClient.streamChat(message, sessionId);

        responseFlux.subscribe(
                chunk -> {
                    if (!chunk.isEmpty()) {
                        try {
                            emitter.send(SseEmitter.event()
                                    .name("message")
                                    .data(chunk));
                        } catch (IOException e) {
                            emitter.completeWithError(e);
                        }
                    }
                },
                error -> {
                    emitter.completeWithError(error);
                },
                () -> {
                    try {
                        emitter.send(SseEmitter.event()
                                .name("done")
                                .data("[DONE]"));
                        emitter.complete();
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                }
        );

        return emitter;
    }

    /**
     * Get the agent's status and capabilities.
     *
     * <p><b>Response:</b>
     * <pre>{@code
     * {
     *   "name": "tutorial-a2a-agent",
     *   "status": "ready",
     *   "clientReady": true,
     *   "serverReady": true,
     *   "skills": ["text-chat", "calculation", "weather"]
     * }
     * }</pre>
     *
     * @return Agent status information
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(Map.of(
                "name", agentClient.getAgentName(),
                "status", "ready",
                "clientReady", agentClient.isReady(),
                "serverReady", agentServer.getA2aServer() != null,
                "baseUrl", agentClient.getBaseUrl()
        ));
    }

    /**
     * Extract text content from a message.
     *
     * @param message The message to extract text from
     * @return The extracted text
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

    /**
     * Chat request DTO.
     */
    public static class ChatRequest {
        private String message;
        private String contextId;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getContextId() {
            return contextId;
        }

        public void setContextId(String contextId) {
            this.contextId = contextId;
        }
    }
}