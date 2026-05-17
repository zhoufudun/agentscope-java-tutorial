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
package io.agentscope.tutorial.chapter18.micronaut;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.micronaut.serde.annotation.Serdeable;

/**
 * REST Controller for AgentScope chat endpoints in Micronaut.
 *
 * <p>This controller demonstrates:
 * <ul>
 *   <li>Injecting ReActAgent into a Micronaut controller</li>
 *   <li>Serde-compatible DTOs with @Serdeable</li>
 *   <li>Non-blocking HTTP handling via Netty</li>
 *   <li>健康检查端点</li>
 * </ul>
 *
 * <p>Example requests:
 * <pre>
 * # Chat endpoint
 * curl -X POST http://localhost:8080/chat \
 *   -H "Content-Type: application/json" \
 *   -d '{"message":"Hello, who are you?"}'
 *
 * # Health endpoint
 * curl http://localhost:8080/health
 * </pre>
 *
 * @author AgentScope Tutorial
 */
@Controller
public class ChatResource {

    /** The auto-configured ReActAgent. */
    private final ReActAgent agent;

    /** The chat service from AgentFactory. */
    private final AgentFactory.ChatService chatService;

    /**
     * Constructs ChatResource with injected dependencies.
     *
     * <p>Micronaut uses constructor injection, which is resolved at compile time
     * for optimal performance.
     *
     * @param agent       the auto-configured ReActAgent
     * @param chatService the chat service from AgentFactory
     */
    public ChatResource(ReActAgent agent, AgentFactory.ChatService chatService) {
        this.agent = agent;
        this.chatService = chatService;
    }

    /**
     * Chat endpoint - sends a message to the agent and returns the response.
     *
     * @param request the chat request containing the user's message
     * @return the agent's response
     */
    @Post("/chat")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public HttpResponse<?> chat(@Body ChatRequest request) {
        if (request == null || request.message() == null || request.message().isBlank()) {
            return HttpResponse.badRequest(new ErrorResponse("Message cannot be empty"));
        }

        try {
            Msg userMsg = Msg.builder()
                    .role(MsgRole.USER)
                    .content(TextBlock.builder().text(request.message()).build())
                    .build();

            Msg response = agent.call(userMsg).block();
            String responseText = response != null ? response.getTextContent() : "No response";

            return HttpResponse.ok(new ChatResponse(responseText));

        } catch (Exception e) {
            return HttpResponse.serverError(new ErrorResponse("Internal error: " + e.getMessage()));
        }
    }

    /**
     * 健康检查端点 - returns agent status information.
     *
     * @return health status with agent name
     */
    @Get("/health")
    @Produces(MediaType.APPLICATION_JSON)
    public HttpResponse<HealthResponse> health() {
        return HttpResponse.ok(new HealthResponse(
                "AgentScope Agent is ready",
                agent.getName(),
                "Micronaut"
        ));
    }

    /**
     * Multi-turn chat endpoint - processes a conversation with multiple messages.
     *
     * @param request the multi-turn chat request
     * @return the agent's response
     */
    @Post("/chat/multi")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public HttpResponse<?> multiChat(@Body MultiChatRequest request) {
        if (request == null || request.messages() == null || request.messages().isEmpty()) {
            return HttpResponse.badRequest(new ErrorResponse("Messages cannot be empty"));
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

            Msg response = agent.call(userMsg).block();
            String responseText = response != null ? response.getTextContent() : "No response";

            return HttpResponse.ok(new ChatResponse(responseText));

        } catch (Exception e) {
            return HttpResponse.serverError(new ErrorResponse("Internal error: " + e.getMessage()));
        }
    }

    /**
     * Echo endpoint - demonstrates using the chat service.
     *
     * @param message the message to echo
     * @return the echo response
     */
    @Get("/echo/{message}")
    @Produces(MediaType.APPLICATION_JSON)
    public HttpResponse<EchoResponse> echo(@PathVariable String message) {
        return HttpResponse.ok(new EchoResponse(
                chatService.getAgentName(),
                message,
                "Micronaut"
        ));
    }

    // ==================== Request/Response DTOs ====================

    /** Chat request DTO. */
    @Serdeable
    public record ChatRequest(String message) {}

    /** Multi-turn chat request DTO. */
    @Serdeable
    public record MultiChatRequest(java.util.List<String> messages) {}

    /** Chat response DTO. */
    @Serdeable
    public record ChatResponse(String response) {}

    /** Error response DTO. */
    @Serdeable
    public record ErrorResponse(String error) {}

    /** Health response DTO. */
    @Serdeable
    public record HealthResponse(String status, String agentName, String framework) {}

    /** Echo response DTO. */
    @Serdeable
    public record EchoResponse(String agentName, String message, String framework) {}
}