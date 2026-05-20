package io.agentscope.tutorial.chapter06.service;

import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.tutorial.chapter06.config.AgentConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Chat Service - Handles chat interactions with models
 *
 * <p>Provides high-level chat operations including streaming and non-streaming chat.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ChatService {

    private final ModelService modelService;
    private final AgentConfig agentConfig;

    // Conversation history cache (per session)
    private final Map<String, List<Msg>> conversationHistory = new ConcurrentHashMap<>();

    /**
     * Send a chat message with a specific model
     */
    public ChatResponse chat(String provider, String modelName, String userMessage) {
        return chat(provider, modelName, userMessage, null);
    }

    /**
     * Send a chat message with custom options
     */
    public ChatResponse chat(String provider, String modelName, String userMessage, GenerateOptions options) {
        Model model = modelService.getModel(provider, modelName);
        if (model == null) {
            throw new IllegalArgumentException("Model not found: " + provider + ":" + modelName);
        }

        List<Msg> messages = List.of(
                Msg.builder()
                        .role(MsgRole.SYSTEM)
                        .content(TextBlock.builder().text(agentConfig.getDefaultSystemPrompt()).build())
                        .build(),
                Msg.builder()
                        .role(MsgRole.USER)
                        .content(TextBlock.builder().text(userMessage).build())
                        .build()
        );

        GenerateOptions effectiveOptions = options != null ? options : GenerateOptions.builder()
                .temperature(agentConfig.getDefaultTemperature())
                .maxTokens(agentConfig.getDefaultMaxTokens())
                .build();

        try {
            return model.stream(messages, null, effectiveOptions)
                    .blockLast(Duration.ofSeconds(60));
        } catch (Exception e) {
            log.error("Chat request failed for {}:{}", provider, modelName, e);
            throw new RuntimeException("Chat request failed: " + e.getMessage(), e);
        }
    }

    /**
     * Send a chat message with conversation history
     */
    public ChatResponse chatWithHistory(String sessionId, String provider, String modelName,
                                        String userMessage, boolean includeSystemPrompt) {
        Model model = modelService.getModel(provider, modelName);
        if (model == null) {
            throw new IllegalArgumentException("Model not found: " + provider + ":" + modelName);
        }

        List<Msg> history = conversationHistory.getOrDefault(sessionId, List.of());
        List<Msg> messages;

        if (includeSystemPrompt) {
            messages = List.of(
                    Msg.builder()
                            .role(MsgRole.SYSTEM)
                            .content(TextBlock.builder().text(agentConfig.getDefaultSystemPrompt()).build())
                            .build()
            );
        } else {
            messages = List.of();
        }

        // Add history
        messages = new java.util.ArrayList<>(messages);
        messages.addAll(history);

        // Add user message
        messages.add(Msg.builder()
                .role(MsgRole.USER)
                .content(TextBlock.builder().text(userMessage).build())
                .build());

        GenerateOptions options = GenerateOptions.builder()
                .temperature(agentConfig.getDefaultTemperature())
                .maxTokens(agentConfig.getDefaultMaxTokens())
                .build();

        try {
            ChatResponse response = model.stream(messages, null, options)
                    .blockLast(Duration.ofSeconds(60));

            // Save to history
            if (response != null) {
                saveToHistory(sessionId,
                        Msg.builder()
                                .role(MsgRole.USER)
                                .content(TextBlock.builder().text(userMessage).build())
                                .build()
                );
                saveToHistory(sessionId,
                        Msg.builder()
                                .role(MsgRole.ASSISTANT)
                                .content(TextBlock.builder().text(extractText(response)).build())
                                .build()
                );
            }

            return response;
        } catch (Exception e) {
            log.error("Chat with history failed for session: {}", sessionId, e);
            throw new RuntimeException("Chat request failed: " + e.getMessage(), e);
        }
    }

    /**
     * Stream chat response
     */
    public Flux<ChatResponse> streamChat(String provider, String modelName, String userMessage) {
        return streamChat(provider, modelName, userMessage, null);
    }

    /**
     * Stream chat response with options
     */
    public Flux<ChatResponse> streamChat(String provider, String modelName,
                                         String userMessage, GenerateOptions options) {
        Model model = modelService.getModel(provider, modelName);
        if (model == null) {
            throw new IllegalArgumentException("Model not found: " + provider + ":" + modelName);
        }

        List<Msg> messages = List.of(
                Msg.builder()
                        .role(MsgRole.SYSTEM)
                        .content(TextBlock.builder().text(agentConfig.getDefaultSystemPrompt()).build())
                        .build(),
                Msg.builder()
                        .role(MsgRole.USER)
                        .content(TextBlock.builder().text(userMessage).build())
                        .build()
        );

        GenerateOptions effectiveOptions = options != null ? options : GenerateOptions.builder()
                .temperature(agentConfig.getDefaultTemperature())
                .maxTokens(agentConfig.getDefaultMaxTokens())
                .build();

        return model.stream(messages, null, effectiveOptions);
    }

    /**
     * Save a message to conversation history
     */
    private void saveToHistory(String sessionId, Msg message) {
        conversationHistory.computeIfAbsent(sessionId, k -> new java.util.ArrayList<>());
        conversationHistory.get(sessionId).add(message);
    }

    /**
     * Clear conversation history
     */
    public void clearHistory(String sessionId) {
        conversationHistory.remove(sessionId);
        log.info("Cleared conversation history for session: {}", sessionId);
    }

    /**
     * Get conversation history
     */
    public List<Msg> getHistory(String sessionId) {
        return conversationHistory.getOrDefault(sessionId, List.of());
    }

    /**
     * Extract text content from ChatResponse
     */
    private String extractText(ChatResponse response) {
        if (response == null || response.getContent() == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (io.agentscope.core.message.ContentBlock block : response.getContent()) {
            if (block instanceof io.agentscope.core.message.TextBlock textBlock) {
                sb.append(textBlock.getText());
            }
        }
        return sb.toString();
    }
}