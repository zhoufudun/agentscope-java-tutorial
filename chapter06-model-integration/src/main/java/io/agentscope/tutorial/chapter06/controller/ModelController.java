package io.agentscope.tutorial.chapter06.controller;

import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.tutorial.chapter06.entity.ChatConfig;
import io.agentscope.tutorial.chapter06.entity.ModelProvider;
import io.agentscope.tutorial.chapter06.repository.ChatConfigRepository;
import io.agentscope.tutorial.chapter06.repository.ModelProviderRepository;
import io.agentscope.tutorial.chapter06.service.ModelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.*;

/**
 * Model Controller
 * Provides REST API for model management, switching, and chat
 */
@RestController
@RequestMapping("/api/models")
@RequiredArgsConstructor
@Slf4j
public class ModelController {

    private final ModelService modelService;
    private final ModelProviderRepository providerRepository;
    private final ChatConfigRepository configRepository;

    // ==================== Model Management APIs ====================

    /**
     * Get all supported model providers
     */
    @GetMapping("/providers")
    public ResponseEntity<List<Map<String, Object>>> getProviders() {
        List<ModelProvider> providers = providerRepository.findByEnabledTrueOrderByPriorityAsc();
        List<Map<String, Object>> result = new ArrayList<>();

        for (ModelProvider provider : providers) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", provider.getId());
            map.put("name", provider.getName());
            map.put("displayName", provider.getDisplayName());
            map.put("enabled", provider.getEnabled());
            map.put("priority", provider.getPriority());
            map.put("baseUrl", provider.getBaseUrl());
            map.put("remark", provider.getRemark());

            // Get all models for this provider
            List<String> modelNames = modelService.getModelNamesByProvider(provider.getName());
            map.put("models", modelNames);

            result.add(map);
        }
        return ResponseEntity.ok(result);
    }

    /**
     * Get all models for a specific provider
     */
    @GetMapping("/providers/{provider}/models")
    public ResponseEntity<List<String>> getModelsByProvider(@PathVariable String provider) {
        List<String> modelNames = modelService.getModelNamesByProvider(provider);
        return ResponseEntity.ok(modelNames);
    }

    /**
     * Register a new model
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registerModel(
            @RequestBody Map<String, String> request) {
        String provider = request.get("provider");
        String modelName = request.get("modelName");

        if (provider == null || modelName == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "provider and modelName are required"
            ));
        }

        try {
            switch (provider.toLowerCase()) {
                case "dashscope" -> modelService.registerDashScopeModel(modelName);
                case "ollama" -> modelService.registerOllamaModel(modelName);
                case "gemini" -> modelService.registerGeminiModel(modelName);
                case "openai" -> modelService.registerOpenAIModel(
                        modelName,
                        "https://api.openai.com/v1",
                        new io.agentscope.extensions.model.openai.formatter.OpenAIChatFormatter()
                );
                default -> {
                    return ResponseEntity.badRequest().body(Map.of(
                            "success", false,
                            "message", "Unsupported provider: " + provider
                    ));
                }
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Model registered successfully",
                    "modelKey", provider + ":" + modelName
            ));
        } catch (Exception e) {
            log.error("Failed to register model", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Registration failed: " + e.getMessage()
            ));
        }
    }

    /**
     * Remove a model
     */
    @DeleteMapping("/remove")
    public ResponseEntity<Map<String, Object>> removeModel(
            @RequestBody Map<String, String> request) {
        String provider = request.get("provider");
        String modelName = request.get("modelName");

        if (provider == null || modelName == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "provider and modelName are required"
            ));
        }

        modelService.removeModel(provider, modelName);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Model removed successfully"
        ));
    }

    // ==================== Chat APIs ====================

    /**
     * Single model chat (non-streaming)
     */
    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(
            @RequestBody Map<String, Object> request) {
        String provider = (String) request.get("provider");
        String modelName = (String) request.get("modelName");
        String message = (String) request.get("message");
        Double temperature = request.containsKey("temperature")
                ? ((Number) request.get("temperature")).doubleValue() : 0.7;
        Integer maxTokens = request.containsKey("maxTokens")
                ? ((Number) request.get("maxTokens")).intValue() : 2000;

        if (provider == null || modelName == null || message == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "provider, modelName, and message are required"
            ));
        }

        Model model = modelService.getModel(provider, modelName);
        if (model == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Model not found: " + provider + ":" + modelName
            ));
        }

        try {
            Msg userMsg = Msg.builder()
                    .role(MsgRole.USER)
                    .content(TextBlock.builder().text(message).build())
                    .build();

            GenerateOptions options = GenerateOptions.builder()
                    .temperature(temperature)
                    .maxTokens(maxTokens)
                    .build();

            // Execute chat
            ChatResponse response = model.stream(List.of(userMsg), null, options)
                    .blockLast(java.time.Duration.ofSeconds(60));

            if (response != null) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "model", modelName,
                        "provider", provider,
                        "response", extractText(response),
                        "usage", response.getUsage()
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "No response received"
                ));
            }
        } catch (Exception e) {
            log.error("Chat request failed", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Request failed: " + e.getMessage()
            ));
        }
    }

    /**
     * Streaming chat
     */
    @PostMapping("/chat/stream")
    public Flux<Map<String, Object>> streamChat(
            @RequestBody Map<String, Object> request) {
        String provider = (String) request.get("provider");
        String modelName = (String) request.get("modelName");
        String message = (String) request.get("message");
        Double temperature = request.containsKey("temperature")
                ? ((Number) request.get("temperature")).doubleValue() : 0.7;

        Model model = modelService.getModel(provider, modelName);
        if (model == null) {
            return Flux.just(Map.of(
                    "error", true,
                    "message", "Model not found"
            ));
        }

        Msg userMsg = Msg.builder()
                .role(MsgRole.USER)
                .content(TextBlock.builder().text(message).build())
                .build();

        GenerateOptions options = GenerateOptions.builder()
                .temperature(temperature)
                .build();

        return model.stream(List.of(userMsg), null, options)
                .map(response -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("content", extractText(response));
                    result.put("done", isResponseDone(response));
                    if (response.getUsage() != null) {
                        result.put("usage", response.getUsage());
                    }
                    return result;
                });
    }

    // ==================== Model Comparison APIs ====================

    /**
     * Compare multiple models (send same question to multiple models)
     */
    @PostMapping("/compare")
    public ResponseEntity<Map<String, Object>> compareModels(
            @RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<Map<String, String>> modelList = (List<Map<String, String>>) request.get("models");
        String message = (String) request.get("message");
        String systemPrompt = (String) request.getOrDefault("systemPrompt",
                "You are a helpful AI assistant.");

        if (modelList == null || modelList.isEmpty() || message == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "models and message are required"
            ));
        }

        List<Map<String, Object>> results = new ArrayList<>();

        for (Map<String, String> modelConfig : modelList) {
            String provider = modelConfig.get("provider");
            String modelName = modelConfig.get("modelName");

            Map<String, Object> result = new HashMap<>();
            result.put("provider", provider);
            result.put("modelName", modelName);

            try {
                long startTime = System.currentTimeMillis();

                Model model = modelService.getModel(provider, modelName);
                if (model == null) {
                    result.put("success", false);
                    result.put("error", "Model not found");
                    results.add(result);
                    continue;
                }

                List<Msg> messages = List.of(
                        Msg.builder()
                                .role(MsgRole.SYSTEM)
                                .content(TextBlock.builder().text(systemPrompt).build())
                                .build(),
                        Msg.builder()
                                .role(MsgRole.USER)
                                .content(TextBlock.builder().text(message).build())
                                .build()
                );

                GenerateOptions options = GenerateOptions.builder()
                        .temperature(0.7)
                        .maxTokens(1000)
                        .build();

                ChatResponse response = model.stream(messages, null, options)
                        .blockLast(java.time.Duration.ofSeconds(60));

                long duration = System.currentTimeMillis() - startTime;

                if (response != null) {
                    result.put("success", true);
                    result.put("response", extractText(response));
                    result.put("duration", duration);
                    if (response.getUsage() != null) {
                        result.put("usage", response.getUsage());
                    }
                } else {
                    result.put("success", false);
                    result.put("error", "No response received");
                }
            } catch (Exception e) {
                result.put("success", false);
                result.put("error", e.getMessage());
            }

            results.add(result);
        }

        return ResponseEntity.ok(Map.of(
                "question", message,
                "results", results
        ));
    }

    // ==================== Configuration Management APIs ====================

    /**
     * Save chat configuration
     */
    @PostMapping("/config/save")
    public ResponseEntity<Map<String, Object>> saveConfig(
            @RequestBody ChatConfig config) {
        try {
            if (config.getId() == null) {
                config.setCreateTime(java.time.LocalDateTime.now());
            }
            config.setUpdateTime(java.time.LocalDateTime.now());

            ChatConfig saved = configRepository.save(config);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", saved
            ));
        } catch (Exception e) {
            log.error("Failed to save config", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Get all chat configurations
     */
    @GetMapping("/config/list")
    public ResponseEntity<List<ChatConfig>> listConfigs() {
        return ResponseEntity.ok(configRepository.findAll());
    }

    /**
     * Get default configuration
     */
    @GetMapping("/config/default")
    public ResponseEntity<Optional<ChatConfig>> getDefaultConfig() {
        return ResponseEntity.ok(configRepository.findByIsDefaultTrue());
    }

    /**
     * Set default configuration
     */
    @PostMapping("/config/set-default/{id}")
    public ResponseEntity<Map<String, Object>> setDefaultConfig(@PathVariable Long id) {
        // Clear current default
        configRepository.findByIsDefaultTrue().ifPresent(config -> {
            config.setIsDefault(false);
            configRepository.save(config);
        });

        // Set new default
        configRepository.findById(id).ifPresent(config -> {
            config.setIsDefault(true);
            configRepository.save(config);
        });

        return ResponseEntity.ok(Map.of("success", true));
    }

    /**
     * Delete a configuration
     */
    @DeleteMapping("/config/{id}")
    public ResponseEntity<Map<String, Object>> deleteConfig(@PathVariable Long id) {
        configRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // ==================== Helper Methods ====================

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
        return response.getContent().stream()
                .anyMatch(block -> block instanceof io.agentscope.core.message.TextBlock);
    }
}