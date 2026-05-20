package io.agentscope.tutorial.chapter06.controller;

import io.agentscope.core.message.Msg;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.tutorial.chapter06.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Chat Controller
 * Provides high-level chat APIs with session support
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;

    /**
     * Send a chat message
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> chat(@RequestBody Map<String, Object> request) {
        String provider = (String) request.get("provider");
        String modelName = (String) request.get("modelName");
        String message = (String) request.get("message");
        String sessionId = (String) request.getOrDefault("sessionId", "default");
        Boolean includeSystemPrompt = (Boolean) request.getOrDefault("includeSystemPrompt", true);

        Double temperature = request.containsKey("temperature")
                ? ((Number) request.get("temperature")).doubleValue() : null;
        Integer maxTokens = request.containsKey("maxTokens")
                ? ((Number) request.get("maxTokens")).intValue() : null;

        GenerateOptions options = null;
        if (temperature != null || maxTokens != null) {
            GenerateOptions.Builder builder = GenerateOptions.builder();
            if (temperature != null) builder.temperature(temperature);
            if (maxTokens != null) builder.maxTokens(maxTokens);
            options = builder.build();
        }

        try {
            ChatResponse response;
            if (sessionId != null && !sessionId.equals("default")) {
                response = chatService.chatWithHistory(sessionId, provider, modelName, message, includeSystemPrompt);
            } else {
                response = chatService.chat(provider, modelName, message, options);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("response", extractText(response));
            if (response.getUsage() != null) {
                result.put("usage", response.getUsage());
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Chat failed", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Stream chat response (SSE)
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Map<String, Object>> streamChat(@RequestBody Map<String, Object> request) {
        String provider = (String) request.get("provider");
        String modelName = (String) request.get("modelName");
        String message = (String) request.get("message");

        Double temperature = request.containsKey("temperature")
                ? ((Number) request.get("temperature")).doubleValue() : null;

        GenerateOptions options = null;
        if (temperature != null) {
            options = GenerateOptions.builder().temperature(temperature).build();
        }

        return chatService.streamChat(provider, modelName, message, options)
                .map(response -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("content", extractText(response));
                    result.put("done", isResponseDone(response));
                    if (response.getUsage() != null) {
                        result.put("usage", response.getUsage());
                    }
                    return result;
                })
                .timeout(Duration.ofSeconds(120))
                .onErrorResume(e -> {
                    log.error("Stream chat failed", e);
                    return Flux.just(Map.of(
                            "error", true,
                            "message", e.getMessage()
                    ));
                });
    }

    /**
     * Get conversation history
     */
    @GetMapping("/history/{sessionId}")
    public ResponseEntity<List<Msg>> getHistory(@PathVariable String sessionId) {
        return ResponseEntity.ok(chatService.getHistory(sessionId));
    }

    /**
     * Clear conversation history
     */
    @DeleteMapping("/history/{sessionId}")
    public ResponseEntity<Map<String, Object>> clearHistory(@PathVariable String sessionId) {
        chatService.clearHistory(sessionId);
        return ResponseEntity.ok(Map.of("success", true));
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

    /**
     * Check if response is done based on content
     */
    private boolean isResponseDone(ChatResponse response) {
        if (response == null || response.getContent() == null) {
            return false;
        }
        // Consider done if there's text content and it's the final response
        return response.getContent().stream()
                .anyMatch(block -> block instanceof io.agentscope.core.message.TextBlock);
    }
}