package io.agentscope.tutorial.chapter07.config;

import io.agentscope.extensions.model.dashscope.formatter.DashScopeChatFormatter;
import io.agentscope.extensions.model.dashscope.DashScopeChatModel;
import io.agentscope.core.model.Model;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Model Configuration
 *
 * <p>Provides Model beans for the AI integration.
 * Supports DashScope (default), OpenAI, Ollama, and Gemini.
 *
 * <p>Configuration is read from application.yml under the "model" prefix.
 *
 * @author AgentScope Tutorial
 */
@Configuration
@ConfigurationProperties(prefix = "model")
@Data
public class ModelConfig {

    private DashScopeConfig dashscope = new DashScopeConfig();
    private OpenAIConfig openai = new OpenAIConfig();
    private OllamaConfig ollama = new OllamaConfig();
    private GeminiConfig gemini = new GeminiConfig();

    /**
     * Creates the default DashScope model.
     *
     * <p>This is the primary model used for the memory system demonstrations.
     * Configure via environment variable DASHSCOPE_API_KEY or application.yml.
     *
     * @return Configured DashScope Model instance
     */
    @Bean
    public Model dashscopeModel() {
        return DashScopeChatModel.builder()
                .apiKey(dashscope.getApiKey())
                .baseUrl(dashscope.getBaseUrl())
                .modelName(dashscope.getDefaultModel())
                .formatter(new DashScopeChatFormatter())
                .stream(true)
                .build();
    }

    @Data
    public static class DashScopeConfig {
        private String apiKey = "demo-key";
        private String baseUrl = "https://dashscope.aliyuncs.com/api/v1";
        private String defaultModel = "qwen-plus";
    }

    @Data
    public static class OpenAIConfig {
        private String apiKey = "demo-key";
        private String baseUrl = "https://api.openai.com/v1";
        private String defaultModel = "gpt-4o";
    }

    @Data
    public static class OllamaConfig {
        private String baseUrl = "http://localhost:11434";
        private String defaultModel = "llama3.2";
    }

    @Data
    public static class GeminiConfig {
        private String apiKey = "demo-key";
        private String defaultModel = "gemini-2.0-flash";
    }
}