package io.agentscope.tutorial.chapter07.config;

import io.agentscope.extensions.model.dashscope.formatter.DashScopeChatFormatter;
import io.agentscope.extensions.model.dashscope.DashScopeChatModel;
import io.agentscope.core.model.Model;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 模型配置类
 *
 * <p>为记忆系统演示提供 AI 模型配置。
 * 从 application.yml 的 {@code model} 前缀下读取配置。
 *
 * <p><b>支持的模型提供商</b>：
 * <ul>
 *   <li>DashScope（默认）- 阿里云百炼，通义千问系列</li>
 *   <li>OpenAI - GPT 系列</li>
 *   <li>Ollama - 本地开源模型</li>
 *   <li>Gemini - Google 多模态模型</li>
 * </ul>
 *
 * <p><b>配置示例</b>（application.yml）：
 * <pre>{@code
 * model:
 *   dashscope:
 *     api-key: ${DASHSCOPE_API_KEY:demo-key}
 *     base-url: https://dashscope.aliyuncs.com/api/v1
 *     default-model: qwen-plus
 * }</pre>
 *
 * @author AgentScope Tutorial
 * @since 1.0
 */
@Configuration
@ConfigurationProperties(prefix = "model")
@Data
public class ModelConfig {

    /** DashScope 配置 */
    private DashScopeConfig dashscope = new DashScopeConfig();

    /** OpenAI 配置 */
    private OpenAIConfig openai = new OpenAIConfig();

    /** Ollama 配置 */
    private OllamaConfig ollama = new OllamaConfig();

    /** Gemini 配置 */
    private GeminiConfig gemini = new GeminiConfig();

    /**
     * 创建默认的 DashScope 模型 Bean
     *
     * <p>这是记忆系统演示使用的主要模型。
     * 通过环境变量 DASHSCOPE_API_KEY 或 application.yml 配置 API Key。
     *
     * <p><b>使用说明</b>：
     * <ol>
     *   <li>获取 API Key：访问 <a href="https://dashscope.console.aliyun.com/">阿里云百炼控制台</a></li>
     *   <li>设置环境变量：{@code export DASHSCOPE_API_KEY=your-key}</li>
     *   <li>或在 application.yml 中配置</li>
     * </ol>
     *
     * @return 配置好的 DashScope 模型实例
     */
    @Bean
    public Model dashscopeModel() {
        return DashScopeChatModel.builder()
                .apiKey(dashscope.getApiKey())
                .baseUrl(dashscope.getBaseUrl())
                .modelName(dashscope.getDefaultModel())
                .formatter(new DashScopeChatFormatter())
                .stream(true)  // 启用流式输出
                .build();
    }

    /**
     * DashScope 配置类
     *
     * <p>阿里云百炼平台配置，提供通义千问系列模型访问。
     */
    @Data
    public static class DashScopeConfig {
        /** API 密钥 */
        private String apiKey = "demo-key";
        /** API 端点 */
        private String baseUrl = "https://dashscope.aliyuncs.com/api/v1";
        /** 默认模型 */
        private String defaultModel = "qwen-plus";
    }

    /**
     * OpenAI 配置类
     */
    @Data
    public static class OpenAIConfig {
        /** API 密钥 */
        private String apiKey = "demo-key";
        /** API 端点 */
        private String baseUrl = "https://api.openai.com/v1";
        /** 默认模型 */
        private String defaultModel = "gpt-4o";
    }

    /**
     * Ollama 配置类
     *
     * <p>本地 Ollama 服务配置，无需 API Key。
     */
    @Data
    public static class OllamaConfig {
        /** 服务地址 */
        private String baseUrl = "http://localhost:11434";
        /** 默认模型 */
        private String defaultModel = "llama3.2";
    }

    /**
     * Gemini 配置类
     */
    @Data
    public static class GeminiConfig {
        /** API 密钥 */
        private String apiKey = "demo-key";
        /** 默认模型 */
        private String defaultModel = "gemini-2.0-flash";
    }
}