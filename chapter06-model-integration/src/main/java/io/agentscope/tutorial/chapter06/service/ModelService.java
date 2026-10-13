package io.agentscope.tutorial.chapter06.service;

import io.agentscope.core.formatter.Formatter;
import io.agentscope.extensions.model.dashscope.DashScopeChatModel;
import io.agentscope.extensions.model.dashscope.formatter.DashScopeChatFormatter;
import io.agentscope.extensions.model.gemini.GeminiChatModel;
import io.agentscope.extensions.model.gemini.formatter.GeminiChatFormatter;
import io.agentscope.extensions.model.ollama.OllamaChatModel;
import io.agentscope.extensions.model.ollama.formatter.OllamaChatFormatter;
import io.agentscope.extensions.model.openai.OpenAIChatModel;
import io.agentscope.extensions.model.openai.compat.deepseek.DeepSeekFormatter;
import io.agentscope.extensions.model.openai.compat.glm.GLMFormatter;
import io.agentscope.extensions.model.openai.formatter.OpenAIChatFormatter;
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
 * 模型服务 - 多模型客户端统一管理
 *
 * <p>本服务是模型集成章节的核心，演示了如何统一管理多种 AI 模型。
 * 支持运行时热切换和动态配置。
 *
 * <p><b>核心特性</b>：
 * <ul>
 *   <li><b>统一抽象</b> - 通过 {@code Model} 接口统一不同提供商的调用方式</li>
 *   <li><b>热切换</b> - 运行时动态切换模型，无需重启</li>
 *   <li><b>线程安全</b> - 使用 ConcurrentHashMap 保证并发安全</li>
 *   <li><b>按需注册</b> - 可动态注册新模型</li>
 * </ul>
 *
 * <p><b>支持的提供商</b>：
 * <ul>
 *   <li>DashScope（阿里云百炼）- 通义千问系列</li>
 *   <li>OpenAI - GPT 系列及兼容 API（DeepSeek、GLM 等）</li>
 *   <li>Ollama - 本地开源模型</li>
 *   <li>Gemini - Google 多模态模型</li>
 * </ul>
 *
 * <p><b>使用示例</b>：
 * <pre>{@code
 * // 获取已注册的模型
 * Model model = modelService.getModel("dashscope", "qwen-plus");
 *
 * // 动态注册新模型
 * modelService.registerDashScopeModel("qwen-max");
 *
 * // 获取提供商的所有模型
 * List<String> models = modelService.getModelNamesByProvider("dashscope");
 * }</pre>
 *
 * @author AgentScope Tutorial
 * @since 1.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ModelService {

    /** 模型提供商仓库 */
    private final ModelProviderRepository providerRepository;

    /** 聊天配置仓库 */
    private final ChatConfigRepository configRepository;

    /** 模型配置（从 application.yml 读取） */
    private final ModelConfig modelConfig;

    /**
     * 模型客户端缓存
     * <p>Key 格式：{provider}:{modelName}，如 "dashscope:qwen-plus"
     * <p>使用 ConcurrentHashMap 保证线程安全
     */
    private final Map<String, Model> modelCache = new ConcurrentHashMap<>();

    /**
     * 初始化默认模型客户端
     *
     * <p>应用启动时自动注册常用模型，确保系统可用。
     * 使用 {@code @PostConstruct} 注解在 Bean 初始化后执行。
     */
    @PostConstruct
    public void init() {
        log.info("正在初始化模型服务...");

        // 注册 DashScope 模型
        registerDashScopeModel("qwen-plus");
        registerDashScopeModel("qwen-max");
        registerDashScopeModel("qwen-turbo");
        registerDashScopeModel("qwq-32b");

        // 注册 OpenAI 兼容模型（包括第三方 API）
        registerOpenAIModel("gpt-4o", "https://api.openai.com/v1", new OpenAIChatFormatter());
        registerOpenAIModel("deepseek-chat", "https://api.deepseek.com/v1", new DeepSeekFormatter());
        registerOpenAIModel("glm-4", "https://open.bigmodel.cn/api/paas/v4", new GLMFormatter());

        // 注册 Ollama 本地模型
        registerOllamaModel("llama3.2");
        registerOllamaModel("qwen2.5:7b");
        registerOllamaModel("mistral");

        // 注册 Gemini 模型
        registerGeminiModel("gemini-2.0-flash");
        registerGeminiModel("gemini-1.5-pro");

        log.info("模型服务初始化完成，已注册 {} 个模型", modelCache.size());
    }

    // ==================== 模型注册方法 ====================

    /**
     * 注册 DashScope 模型
     *
     * <p>使用 DashScope API 创建模型客户端。支持通义千问系列的各种模型。
     *
     * @param modelName 模型名称（如 qwen-plus、qwen-max、qwen-turbo）
     */
    public void registerDashScopeModel(String modelName) {
        String cacheKey = "dashscope:" + modelName;
        Model model = DashScopeChatModel.builder()
                .apiKey(modelConfig.getDashscope().getApiKey())
                .baseUrl(modelConfig.getDashscope().getBaseUrl())
                .modelName(modelName)
                .stream(true)  // 启用流式输出
                .formatter(new DashScopeChatFormatter())
                .defaultOptions(GenerateOptions.builder()
                        .temperature(0.7)
                        .maxTokens(2000)
                        .build())
                .build();
        modelCache.put(cacheKey, model);
        log.info("已注册 DashScope 模型: {}", modelName);
    }

    /**
     * 注册 OpenAI 兼容模型
     *
     * <p>支持 OpenAI 官方 API 和兼容 API（如 DeepSeek、智谱 GLM）。
     * 不同提供商使用不同的 Formatter 处理请求/响应格式差异。
     *
     * @param modelName 模型名称
     * @param baseUrl API 端点 URL
     * @param formatter 格式化器（处理提供商特定的格式）
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
        log.info("已注册 OpenAI 兼容模型: {} -> {}", modelName, baseUrl);
    }

    /**
     * 注册 Ollama 模型
     *
     * <p>Ollama 是本地模型运行时，无需 API Key。
     * 确保本地已安装 Ollama 并拉取了对应模型。
     *
     * @param modelName 模型名称（如 llama3.2、qwen2.5:7b）
     */
    public void registerOllamaModel(String modelName) {
        String cacheKey = "ollama:" + modelName;
        Model model = OllamaChatModel.builder()
                .modelName(modelName)
                .baseUrl(modelConfig.getOllama().getBaseUrl())
                .build();
        modelCache.put(cacheKey, model);
        log.info("已注册 Ollama 模型: {}", modelName);
    }

    /**
     * 注册 Gemini 模型
     *
     * <p>Google Gemini 系列模型，支持多模态任务。
     *
     * @param modelName 模型名称（如 gemini-2.0-flash、gemini-1.5-pro）
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
        log.info("已注册 Gemini 模型: {}", modelName);
    }

    // ==================== 模型获取方法 ====================

    /**
     * 按提供商和模型名称获取模型客户端
     *
     * @param provider 提供商名称
     * @param modelName 模型名称
     * @return 模型客户端，不存在则返回 null
     */
    public Model getModel(String provider, String modelName) {
        String cacheKey = provider + ":" + modelName;
        return modelCache.get(cacheKey);
    }

    /**
     * 获取所有已注册的模型
     *
     * @return 模型映射表的副本
     */
    public Map<String, Model> getAllModels() {
        return new HashMap<>(modelCache);
    }

    /**
     * 按提供商获取模型
     *
     * @param provider 提供商名称
     * @return 该提供商的模型映射表
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
     * 获取提供商的模型名称列表
     *
     * @param provider 提供商名称
     * @return 模型名称列表
     */
    public List<String> getModelNamesByProvider(String provider) {
        return modelCache.keySet().stream()
                .filter(key -> key.startsWith(provider + ":"))
                .map(key -> key.substring(key.indexOf(':') + 1))
                .toList();
    }

    /**
     * 切换默认模型
     *
     * <p>验证模型是否已注册，然后切换默认模型。
     *
     * @param provider 提供商名称
     * @param modelName 模型名称
     * @throws IllegalArgumentException 如果模型不存在
     */
    public void switchDefaultModel(String provider, String modelName) {
        String cacheKey = provider + ":" + modelName;
        if (!modelCache.containsKey(cacheKey)) {
            throw new IllegalArgumentException("模型不存在: " + cacheKey);
        }
        log.info("切换默认模型: {} -> {}", provider, modelName);
    }

    /**
     * 移除模型
     *
     * @param provider 提供商名称
     * @param modelName 模型名称
     */
    public void removeModel(String provider, String modelName) {
        String cacheKey = provider + ":" + modelName;
        modelCache.remove(cacheKey);
        log.info("已移除模型: {}", cacheKey);
    }

    /**
     * 检查模型是否可用
     *
     * @param provider 提供商名称
     * @param modelName 模型名称
     * @return 是否可用
     */
    public boolean isModelAvailable(String provider, String modelName) {
        return modelCache.containsKey(provider + ":" + modelName);
    }

    /**
     * 获取所有支持的提供商
     *
     * @return 提供商名称列表
     */
    public List<String> getSupportedProviders() {
        return List.of("dashscope", "openai", "ollama", "gemini");
    }

    /**
     * 从数据库配置重新加载模型
     *
     * <p>根据数据库中的配置重新注册模型，用于配置变更后的热更新。
     *
     * @param providerId 提供商 ID
     */
    public void reloadModelFromDb(Long providerId) {
        ModelProvider provider = providerRepository.findById(providerId).orElse(null);
        if (provider == null) {
            log.warn("提供商不存在: {}", providerId);
            return;
        }

        // 根据配置重新注册该提供商的模型
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
        log.info("已重新加载提供商 {} 的模型配置", provider.getName());
    }
}