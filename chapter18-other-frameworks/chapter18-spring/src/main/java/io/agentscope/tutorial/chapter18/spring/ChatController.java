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
package io.agentscope.tutorial.chapter18.spring;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST Controller for AgentScope chat endpoints in Spring Boot.
 *
 * <p>This controller demonstrates:
 * <ul>
 *   <li>Injecting ReActAgent into a REST controller</li>
 *   <li>Request/response DTOs with records</li>
 *   <li>Validation and error handling</li>
 *   <li>健康检查端点 via Actuator</li>
 * </ul>
 *
 * <p>Example requests:
 * <pre>
 * # Chat endpoint
 * curl -X POST http://localhost:8080/api/chat \
 *   -H "Content-Type: application/json" \
 *   -d '{"message":"Hello, who are you?"}'
 *
 * # Health endpoint
 * curl http://localhost:8080/api/health
 * </pre>
 *
 * @author AgentScope Tutorial
 */
@RestController
@RequestMapping("/api")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    /** The auto-configured ReActAgent. */
    private final ReActAgent agent;

    /**
     * Constructs ChatController with auto-configured ReActAgent.
     *
     * @param agent the auto-configured ReActAgent
     */
    public ChatController(ReActAgent agent) {
        this.agent = agent;
    }

    /**
     * Chat endpoint - sends a message to the agent and returns the response.
     *
     * @param request the chat request containing the user's message
     * @return the agent's response wrapped in ResponseEntity
     */
    @PostMapping("/chat")
    public ResponseEntity<?> chat(@RequestBody ChatRequest request) {
        if (request == null || request.message() == null || request.message().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Message cannot be empty"));
        }

        try {
            log.info("Received chat request: {}", request.message());

            Msg userMsg = Msg.builder()
                    .role(MsgRole.USER)
                    .content(TextBlock.builder().text(request.message()).build())
                    .build();

            Msg response = agent.call(List.of(userMsg)).block();

            String responseText = response != null ? response.getTextContent() : "No response";

            log.info("Agent response: {}", responseText);

            return ResponseEntity.ok(new ChatResponse(responseText));

        } catch (Exception e) {
            log.error("Error processing chat request", e);
            return ResponseEntity.internalServerError()
                    .body(new ErrorResponse("Internal error: " + e.getMessage()));
        }
    }

    /**
     * 健康检查端点 - returns agent status information.
     *
     * @return health status with agent name
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "AgentScope Agent is ready",
                "agentName", agent.getName()
        ));
    }

    /**
     * Multi-turn chat endpoint - processes a conversation with multiple messages.
     *
     * @param request the multi-turn chat request
     * @return the agent's response
     */
    @PostMapping("/chat/multi")
    public ResponseEntity<?> multiChat(@RequestBody MultiChatRequest request) {
        if (request == null || request.messages() == null || request.messages().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Messages cannot be empty"));
        }

        try {
            StringBuilder conversation = new StringBuilder();
            for (String msg : request.messages()) {
                conversation.append(msg).append("\n");
            }

            Msg userMsg = Msg.builder()
                    .role(MsgRole.USER)
                    .content(TextBlock.builder().text(conversation.toString()).build())
                    .build();

            Msg response = agent.call(List.of(userMsg)).block();
            String responseText = response != null ? response.getTextContent() : "No response";

            return ResponseEntity.ok(new ChatResponse(responseText));

        } catch (Exception e) {
            log.error("Error processing multi-turn chat", e);
            return ResponseEntity.internalServerError()
                    .body(new ErrorResponse("Internal error: " + e.getMessage()));
        }
    }

    // ==================== Request/Response DTOs ====================

    /**
     * Chat request DTO.
     *
     * @param message the user's message
     */
    public record ChatRequest(String message) {}

    /**
     * Multi-turn chat request DTO.
     *
     * @param messages list of messages to send as a conversation
     */
    public record MultiChatRequest(List<String> messages) {}

    /**
     * Chat response DTO.
     *
     * @param response the agent's response text
     */
    public record ChatResponse(String response) {}

    /**
     * Error response DTO.
     *
     * @param error the error message
     */
    public record ErrorResponse(String error) {}
}