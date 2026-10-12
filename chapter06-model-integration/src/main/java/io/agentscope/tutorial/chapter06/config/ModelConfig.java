package io.agentscope.tutorial.chapter06.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 模型配置属性类
 *
 * <p>从 application.yml 的 {@code model} 前缀下读取各模型提供商的配置。
 * 支持多提供商配置，每个提供商有独立的 API Key、Base URL 和默认模型。
 *
 * <p><b>配置示例</b>（application.yml）：
 * <pre>{@code
 * model:
 *   dashscope:
 *     api-key: ${DASHSCOPE_API_KEY:demo-key}
 *     base-url: https://dashscope.aliyuncs.com/api/v1
 *     default-model: qwen-plus
 *   openai:
 *     api-key: ${OPENAI_API_KEY:demo-key}
 *     base-url: https://api.openai.com/v1
 *     default-model: gpt-4o
 *   ollama:
 *     base-url: http://localhost:11434
 *     default-model: llama3.2
 *   gemini:
 *     api-key: ${GEMINI_API_KEY:demo-key}
 *     default-model: gemini-2.0-flash
 * }</pre>
 *
 * <p><b>安全建议</b>：
 * 生产环境中请使用环境变量或密钥管理服务存储 API Key，避免硬编码。
 *
 * @author AgentScope Tutorial
 * @since 1.0
 */
@Configuration
@ConfigurationProperties(prefix = "model")
@Data
public class ModelConfig {

    /** DashScope（阿里云百炼）配置 */
    private DashScopeConfig dashscope = new DashScopeConfig();

    /** OpenAI 配置 */
    private OpenAIConfig openai = new OpenAIConfig();

    /** Ollama（本地部署）配置 */
    private OllamaConfig ollama = new OllamaConfig();

    /** Gemini（Google）配置 */
    private GeminiConfig gemini = new GeminiConfig();

    /**
     * DashScope 配置类
     *
     * <p>阿里云百炼平台配置，用于访问通义千问系列模型。
     * 获取 API Key：<a href="https://dashscope.console.aliyun.com/">阿里云百炼控制台</a>
     */
    @Data
    public static class DashScopeConfig {
        /** API 密钥（从阿里云百炼控制台获取） */
        private String apiKey = "demo-key";
        /** API 基础 URL */
        private String baseUrl = "https://dashscope.aliyuncs.com/api/v1";
        /** 默认模型名称（如 qwen-plus、qwen-max、qwen-turbo） */
        private String defaultModel = "qwen-plus";
    }

    /**
     * OpenAI 配置类
     *
     * <p>OpenAI 平台配置，用于访问 GPT 系列模型。
     * 获取 API Key：<a href="https://platform.openai.com/api-keys">OpenAI Platform</a>
     */
    @Data
    public static class OpenAIConfig {
        /** API 密钥 */
        private String apiKey = "demo-key";
        /** API 基础 URL（可用于配置代理或兼容 API） */
        private String baseUrl = "https://api.openai.com/v1";
        /** 默认模型名称（如 gpt-4o、gpt-4-turbo） */
        private String defaultModel = "gpt-4o";
    }

    /**
     * Ollama 配置类
     *
     * <p>本地 Ollama 服务配置，用于访问本地部署的开源模型。
     * Ollama 无需 API Key，默认运行在 localhost:11434。
     *
     * <p>安装 Ollama：<a href="https://ollama.ai/">Ollama 官网</a>
     */
    @Data
    public static class OllamaConfig {
        /** Ollama 服务地址 */
        private String baseUrl = "http://localhost:11434";
        /** 默认模型名称（如 llama3.2、qwen2.5、mistral） */
        private String defaultModel = "llama3.2";
    }

    /**
     * Gemini 配置类
     *
     * <p>Google Gemini 平台配置，用于访问 Gemini 系列多模态模型。
     * 获取 API Key：<a href="https://aistudio.google.com/app/apikey">Google AI Studio</a>
     */
    @Data
    public static class GeminiConfig {
        /** API 密钥 */
        private String apiKey = "demo-key";
        /** 默认模型名称（如 gemini-2.0-flash、gemini-1.5-pro） */
        private String defaultModel = "gemini-2.0-flash";
    }
}