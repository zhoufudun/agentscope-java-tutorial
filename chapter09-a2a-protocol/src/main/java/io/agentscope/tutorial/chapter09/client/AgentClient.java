package io.agentscope.tutorial.chapter09.client;

import io.agentscope.core.a2a.agent.A2aAgent;
import io.agentscope.core.a2a.agent.card.AgentCardResolver;
import io.agentscope.core.a2a.agent.card.WellKnownAgentCardResolver;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.UUID;

/**
 * A2A Client for communicating with remote agents.
 *
 * <p>This client demonstrates how to:
 * <ul>
 *   <li>Create an A2A client using AgentCard discovery</li>
 *   <li>Send messages to remote agents</li>
 *   <li>Handle streaming responses</li>
 *   <li>Process task events and updates</li>
 * </ul>
 *
 * <p>The A2A client uses the well-known URI pattern to discover
 * the remote agent's AgentCard, which contains its endpoint and
 * capabilities information.
 */
@Component
public class AgentClient {

    private static final Logger log = LoggerFactory.getLogger(AgentClient.class);

    /**
     * 默认 base URL for AgentCard discovery.
     */
    @Value("${a2a.client.base-url:http://localhost:8888}")
    private String baseUrl;

    /**
     * 默认 path for well-known AgentCard.
     */
    @Value("${a2a.client.agent-card-path:/.well-known/agent-card.json}")
    private String agentCardPath;

    /**
     * Remote agent name.
     */
    @Value("${a2a.client.agent-name:tutorial-a2a-agent}")
    private String agentName;

    /**
     * The A2A agent proxy for remote communication.
     */
    private A2aAgent a2aAgent;

    /**
     * Initialize the A2A client.
     *
     * <p>该方法 creates the A2aAgent instance configured to
     * communicate with the remote agent via A2A protocol.
     */
    @jakarta.annotation.PostConstruct
    public void init() {
        try {
            // Create AgentCard resolver using well-known URI
            AgentCardResolver agentCardResolver = WellKnownAgentCardResolver.builder()
                    .baseUrl(baseUrl)
                    .relativeCardPath(agentCardPath)
                    .authHeaders(Map.of())
                    .build();

            // Build A2A agent client
            this.a2aAgent = A2aAgent.builder()
                    .name(agentName)
                    .agentCardResolver(agentCardResolver)
                    .build();

            log.info("A2A Client initialized successfully");
            log.info("Target Agent: {}", agentName);
            log.info("AgentCard URL: {}{}", baseUrl, agentCardPath);

        } catch (Exception e) {
            log.warn("Failed to initialize A2A client: {}. "
                    + "This is expected if the remote agent is not available.", e.getMessage());
        }
    }

    /**
     * Send a chat message to the remote agent and get a response.
     *
     * <p>该方法 blocks until the response is received.
     * For streaming responses, use {@link #streamChat(Msg)}.
     *
     * @param message The message to send
     * @return The response message from the remote agent
     */
    public Msg chat(Msg message) {
        log.info("Sending message to remote agent: {}", message.getContent());
        return a2aAgent.call(message).block();
    }

    /**
     * Send a chat message to the remote agent with a context ID.
     *
     * @param content The message content
     * @param contextId The context ID for conversation tracking
     * @return The response message from the remote agent
     */
    public Msg chat(String content, String contextId) {
        Msg message = Msg.builder()
                .role(MsgRole.USER)
                .content(TextBlock.builder().text(content).build())
                .build();
        return chat(message);
    }

    /**
     * Stream chat responses from the remote agent.
     */
    public Flux<String> streamChat(Msg message) {
        log.info("Starting streaming chat with remote agent");

        return a2aAgent.stream(message)
                .map(event -> {
                    if (event.isLast()) {
                        return "";
                    }

                    Msg responseMessage = event.getMessage();
                    if (responseMessage == null) {
                        return "";
                    }

                    StringBuilder text = new StringBuilder();
                    responseMessage.getContent().stream()
                            .filter(block -> block instanceof TextBlock)
                            .map(block -> (TextBlock) block)
                            .forEach(block -> text.append(block.getText()));

                    return text.toString();
                });
    }

    /**
     * Stream chat responses with a context ID.
     *
     * @param content The message content
     * @param contextId The context ID for conversation tracking
     * @return A Flux of response text chunks
     */
    public Flux<String> streamChat(String content, String contextId) {
        Msg message = Msg.builder()
                .role(MsgRole.USER)
                .content(TextBlock.builder().text(content).build())
                .build();
        return streamChat(message);
    }

    /**
     * Check if the A2A client is ready.
     *
     * @return true if the client is initialized and ready
     */
    public boolean isReady() {
        return a2aAgent != null;
    }

    /**
     * Get the remote agent's name.
     *
     * @return The agent name
     */
    public String getAgentName() {
        return agentName;
    }

    /**
     * Get the configured base URL.
     *
     * @return The base URL
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * Create a new conversation context.
     *
     * @return A new context ID
     */
    public String createContextId() {
        return UUID.randomUUID().toString();
    }
}