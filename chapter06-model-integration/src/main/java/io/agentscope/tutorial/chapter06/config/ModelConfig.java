package io.agentscope.tutorial.chapter06.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Model Configuration Properties
 * Reads model configuration from application.yml
 */
@Configuration
@ConfigurationProperties(prefix = "model")
@Data
public class ModelConfig {

    private DashScopeConfig dashscope = new DashScopeConfig();
    private OpenAIConfig openai = new OpenAIConfig();
    private OllamaConfig ollama = new OllamaConfig();
    private GeminiConfig gemini = new GeminiConfig();

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