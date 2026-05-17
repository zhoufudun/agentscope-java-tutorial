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
package io.agentscope.tutorial.chapter18.quarkus;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST Controller for AgentScope chat endpoints in Quarkus.
 *
 * <p>This controller demonstrates:
 * <ul>
 *   <li>Injecting ReActAgent into a JAX-RS resource</li>
 *   <li>Injecting AgentBean for business logic wrapper</li>
 *   <li>Request/response DTOs with records (Java 16+)</li>
 *   <li>Validation and error handling</li>
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
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ChatResource {

    /** The auto-configured ReActAgent. */
    @Inject
    ReActAgent agent;

    /** The business logic wrapper bean. */
    @Inject
    AgentBean agentBean;

    /**
     * Chat endpoint - sends a message to the agent and returns the response.
     *
     * @param request the chat request containing the user's message
     * @return the agent's response wrapped in ResponseEntity
     */
    @POST
    @Path("/chat")
    public Response chat(ChatRequest request) {
        if (request == null || request.message() == null || request.message().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Message cannot be empty"))
                    .build();
        }

        try {
            Msg userMsg = Msg.builder()
                    .role(MsgRole.USER)
                    .content(TextBlock.builder().text(request.message()).build())
                    .build();

            Msg response = agent.call(userMsg).block();
            String responseText = response != null ? response.getTextContent() : "No response";

            return Response.ok(new ChatResponse(responseText)).build();

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Internal error: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * 健康检查端点 - returns agent status information.
     *
     * @return health status with agent name
     */
    @GET
    @Path("/health")
    public Response health() {
        return Response.ok(new HealthResponse(
                agentBean.getStatus(),
                agent.getName(),
                "Quarkus"
        )).build();
    }

    /**
     * Multi-turn chat endpoint - processes a conversation with multiple messages.
     *
     * @param request the multi-turn chat request
     * @return the agent's response
     */
    @POST
    @Path("/chat/multi")
    public Response multiChat(MultiChatRequest request) {
        if (request == null || request.messages() == null || request.messages().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Messages cannot be empty"))
                    .build();
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

            return Response.ok(new ChatResponse(responseText)).build();

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Internal error: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Echo endpoint - demonstrates using the agent bean.
     *
     * @return echo response with agent info
     */
    @GET
    @Path("/echo")
    public Response echo() {
        return Response.ok(new EchoResponse(
                agent.getName(),
                agentBean.getStatus(),
                "Quarkus"
        )).build();
    }

    // ==================== Request/Response DTOs ====================

    /** Chat request DTO. */
    public record ChatRequest(String message) {}

    /** Multi-turn chat request DTO. */
    public record MultiChatRequest(java.util.List<String> messages) {}

    /** Chat response DTO. */
    public record ChatResponse(String response) {}

    /** Error response DTO. */
    public record ErrorResponse(String error) {}

    /** Health response DTO. */
    public record HealthResponse(String status, String agentName, String framework) {}

    /** Echo response DTO. */
    public record EchoResponse(String agentName, String status, String framework) {}
}