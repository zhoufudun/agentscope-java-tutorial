package io.agentscope.tutorial.chapter06.service;

import io.agentscope.core.formatter.Formatter;
import io.agentscope.core.formatter.dashscope.DashScopeChatFormatter;
import io.agentscope.core.formatter.gemini.GeminiChatFormatter;
import io.agentscope.core.formatter.ollama.OllamaChatFormatter;
import io.agentscope.core.formatter.openai.DeepSeekFormatter;
import io.agentscope.core.formatter.openai.GLMFormatter;
import io.agentscope.core.formatter.openai.OpenAIChatFormatter;
import io.agentscope.core.model.*;
import io.agentscope.tutorial.chapter06.config.ModelConfig;
import io.agentscope.tutorial.chapter06.entity.ChatConfig;
import io.agentscope.tutorial.chapter06.entity.ModelProvider;
import io.agentscope.tutorial.chapter06.repository.ChatConfigRepository;
import io.agentscope.tutorial.chapter06.repository.ModelProviderRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Model Service - Unified management of multiple model clients
 *
 * <p>Supports hot-swapping and dynamic configuration of models.
 * Thread-safe implementation using ConcurrentHashMap for model cache.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ModelService {

    private final ModelProviderRepository providerRepository;
    private final ChatConfigRepository configRepository;
    private final ModelConfig modelConfig;

    // Model client cache (thread-safe)
    private final Map<String, Model> modelCache = new ConcurrentHashMap<>();

    /**
     * Initialize default model clients
     */
    @PostConstruct
    public void init() {
        log.info("Initializing model service...");

        // Initialize DashScope models
        registerDashScopeModel("qwen-plus");
        registerDashScopeModel("qwen-max");
        registerDashScopeModel("qwen-turbo");
        registerDashScopeModel("qwq-32b");

        // Initialize OpenAI compatible models
        registerOpenAIModel("gpt-4o", "https://api.openai.com/v1", new OpenAIChatFormatter());
        registerOpenAIModel("deepseek-chat", "https://api.deepseek.com/v1", new DeepSeekFormatter());
        registerOpenAIModel("glm-4", "https://open.bigmodel.cn/api/paas/v4", new GLMFormatter());

        // Initialize Ollama models
        registerOllamaModel("llama3.2");
        registerOllamaModel("qwen2.5:7b");
        registerOllamaModel("mistral");

        // Initialize Gemini models
        registerGeminiModel("gemini-2.0-flash");
        registerGeminiModel("gemini-1.5-pro");

        log.info("Model service initialized, registered {} models", modelCache.size());
    }

    // ==================== Model Registration Methods ====================

    /**
     * Register a DashScope model
     */
    public void registerDashScopeModel(String modelName) {
        String cacheKey = "dashscope:" + modelName;
        Model model = DashScopeChatModel.builder()
                .apiKey(modelConfig.getDashscope().getApiKey())
                .baseUrl(modelConfig.getDashscope().getBaseUrl())
                .modelName(modelName)
                .stream(true)
                .formatter(new DashScopeChatFormatter())
                .defaultOptions(GenerateOptions.builder()
                        .temperature(0.7)
                        .maxTokens(2000)
                        .build())
                .build();
        modelCache.put(cacheKey, model);
        log.info("Registered DashScope model: {}", modelName);
    }

    /**
     * Register an OpenAI compatible model
     */
    public void registerOpenAIModel(String modelName, String baseUrl, Formatter formatter) {
        String cacheKey = "openai:" + modelName;
        Model model = OpenAIChatModel.builder()
                .apiKey(modelConfig.getOpenai().getApiKey())
                .baseUrl(baseUrl)
                .modelName(modelName)
                .stream(true)
                .formatter(formatter)
                .build();
        modelCache.put(cacheKey, model);
        log.info("Registered OpenAI compatible model: {} -> {}", modelName, baseUrl);
    }

    /**
     * Register an Ollama model
     */
    public void registerOllamaModel(String modelName) {
        String cacheKey = "ollama:" + modelName;
        Model model = OllamaChatModel.builder()
                .modelName(modelName)
                .baseUrl(modelConfig.getOllama().getBaseUrl())
                .build();
        modelCache.put(cacheKey, model);
        log.info("Registered Ollama model: {}", modelName);
    }

    /**
     * Register a Gemini model
     */
    public void registerGeminiModel(String modelName) {
        String cacheKey = "gemini:" + modelName;
        Model model = GeminiChatModel.builder()
                .apiKey(modelConfig.getGemini().getApiKey())
                .modelName(modelName)
                .streamEnabled(true)
                .formatter(new GeminiChatFormatter())
                .defaultOptions(GenerateOptions.builder()
                        .temperature(0.7)
                        .maxTokens(2000)
                        .build())
                .build();
        modelCache.put(cacheKey, model);
        log.info("Registered Gemini model: {}", modelName);
    }

    // ==================== Model Retrieval Methods ====================

    /**
     * Get model client by provider and model name
     */
    public Model getModel(String provider, String modelName) {
        String cacheKey = provider + ":" + modelName;
        return modelCache.get(cacheKey);
    }

    /**
     * Get all registered models
     */
    public Map<String, Model> getAllModels() {
        return new HashMap<>(modelCache);
    }

    /**
     * Get models by provider
     */
    public Map<String, Model> getModelsByProvider(String provider) {
        Map<String, Model> result = new HashMap<>();
        modelCache.forEach((key, model) -> {
            if (key.startsWith(provider + ":")) {
                result.put(key, model);
            }
        });
        return result;
    }

    /**
     * Get list of model names for a provider
     */
    public List<String> getModelNamesByProvider(String provider) {
        return modelCache.keySet().stream()
                .filter(key -> key.startsWith(provider + ":"))
                .map(key -> key.substring(key.indexOf(':') + 1))
                .toList();
    }

    /**
     * Switch default model
     */
    public void switchDefaultModel(String provider, String modelName) {
        String cacheKey = provider + ":" + modelName;
        if (!modelCache.containsKey(cacheKey)) {
            throw new IllegalArgumentException("Model does not exist: " + cacheKey);
        }
        log.info("Switching default model: {} -> {}", provider, modelName);
    }

    /**
     * Remove a model
     */
    public void removeModel(String provider, String modelName) {
        String cacheKey = provider + ":" + modelName;
        modelCache.remove(cacheKey);
        log.info("Removed model: {}", cacheKey);
    }

    /**
     * Check if model is available
     */
    public boolean isModelAvailable(String provider, String modelName) {
        return modelCache.containsKey(provider + ":" + modelName);
    }

    /**
     * Get all supported providers
     */
    public List<String> getSupportedProviders() {
        return List.of("dashscope", "openai", "ollama", "gemini");
    }

    /**
     * Reload model from database configuration
     */
    public void reloadModelFromDb(Long providerId) {
        ModelProvider provider = providerRepository.findById(providerId).orElse(null);
        if (provider == null) {
            log.warn("Provider not found: {}", providerId);
            return;
        }

        // Re-register models for this provider
        List<ChatConfig> configs = configRepository.findByProviderType(provider.getName());
        for (ChatConfig config : configs) {
            switch (provider.getName()) {
                case "dashscope" -> registerDashScopeModel(config.getModelName());
                case "ollama" -> registerOllamaModel(config.getModelName());
                case "gemini" -> registerGeminiModel(config.getModelName());
                case "openai" -> registerOpenAIModel(
                        config.getModelName(),
                        provider.getBaseUrl(),
                        new OpenAIChatFormatter()
                );
            }
        }
        log.info("Reloaded models for provider: {}", provider.getName());
    }
}