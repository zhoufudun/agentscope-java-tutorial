package io.agentscope.tutorial.chapter06.config;

import io.agentscope.tutorial.chapter06.entity.ChatConfig;
import io.agentscope.tutorial.chapter06.entity.ModelProvider;
import io.agentscope.tutorial.chapter06.repository.ChatConfigRepository;
import io.agentscope.tutorial.chapter06.repository.ModelProviderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 数据初始化器
 *
 * <p>应用启动时自动初始化默认的模型提供商配置和聊天配置。
 * 实现 {@link CommandLineRunner} 接口，在 Spring Boot 完成所有 Bean 初始化后执行。
 *
 * <p><b>初始化内容</b>：
 * <ol>
 *   <li>模型提供商（DashScope、OpenAI、Ollama、Gemini）</li>
 *   <li>各提供商的默认聊天配置（模型名称、温度、系统提示词等）</li>
 * </ol>
 *
 * <p><b>幂等性保证</b>：
 * 使用 {@code existsByName} 检查避免重复创建，确保多次启动不会产生重复数据。
 *
 * @author AgentScope Tutorial
 * @since 1.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    /** 模型提供商数据访问层 */
    private final ModelProviderRepository providerRepository;

    /** 聊天配置数据访问层 */
    private final ChatConfigRepository configRepository;

    /**
     * 应用启动后的回调入口
     *
     * @param args 命令行参数（未使用）
     */
    @Override
    public void run(String... args) {
        initializeProviders();
        initializeDefaultConfigs();
    }

    /**
     * 初始化模型提供商配置
     *
     * <p>注册所有支持的模型提供商到数据库，包括：
     * <ul>
     *   <li>DashScope - 阿里云百炼平台，提供通义千问系列</li>
     *   <li>OpenAI - GPT 系列模型</li>
     *   <li>Ollama - 本地部署的开源模型</li>
     *   <li>Gemini - Google 多模态模型</li>
     * </ul>
     */
    private void initializeProviders() {
        log.info("正在初始化模型提供商配置...");

        // DashScope（阿里云百炼）
        if (!providerRepository.existsByName("dashscope")) {
            providerRepository.save(ModelProvider.builder()
                    .name("dashscope")
                    .displayName("阿里云百炼 (DashScope)")
                    .baseUrl("https://dashscope.aliyuncs.com/api/v1")
                    .enabled(true)
                    .priority(10)  // 优先级越小越靠前
                    .remark("通义千问系列模型，支持文本和视觉任务")
                    .build());
            log.info("已注册 DashScope 提供商");
        }

        // OpenAI
        if (!providerRepository.existsByName("openai")) {
            providerRepository.save(ModelProvider.builder()
                    .name("openai")
                    .displayName("OpenAI")
                    .baseUrl("https://api.openai.com/v1")
                    .enabled(true)
                    .priority(20)
                    .remark("GPT 系列模型，强大的推理和生成能力")
                    .build());
            log.info("已注册 OpenAI 提供商");
        }

        // Ollama（本地部署）
        if (!providerRepository.existsByName("ollama")) {
            providerRepository.save(ModelProvider.builder()
                    .name("ollama")
                    .displayName("Ollama (本地)")
                    .baseUrl("http://localhost:11434")
                    .enabled(true)
                    .priority(30)
                    .remark("本地开源模型，隐私保护")
                    .build());
            log.info("已注册 Ollama 提供商");
        }

        // Gemini（Google）
        if (!providerRepository.existsByName("gemini")) {
            providerRepository.save(ModelProvider.builder()
                    .name("gemini")
                    .displayName("Google Gemini")
                    .baseUrl("https://generativelanguage.googleapis.com")
                    .enabled(true)
                    .priority(40)
                    .remark("Google 多模态模型，支持图像和视频理解")
                    .build());
            log.info("已注册 Gemini 提供商");
        }

        log.info("模型提供商初始化完成");
    }

    /**
     * 初始化默认聊天配置
     *
     * <p>为每个提供商创建默认的聊天配置，包含推荐的模型和参数设置。
     * 用户可以在此基础上创建自定义配置。
     */
    private void initializeDefaultConfigs() {
        log.info("正在初始化默认聊天配置...");

        // DashScope 默认配置
        if (configRepository.findByName("DashScope 默认配置").isEmpty()) {
            configRepository.save(ChatConfig.builder()
                    .name("DashScope 默认配置")
                    .providerType("dashscope")
                    .modelName("qwen-plus")
                    .temperature(0.7)
                    .maxTokens(2000)
                    .systemPrompt("你是一个有帮助的AI助手。请用中文回答问题。")
                    .streamEnabled(true)
                    .isDefault(true)  // 设为默认配置
                    .build());
            log.info("已创建 DashScope 默认配置");
        }

        // OpenAI 默认配置
        if (configRepository.findByName("OpenAI 默认配置").isEmpty()) {
            configRepository.save(ChatConfig.builder()
                    .name("OpenAI 默认配置")
                    .providerType("openai")
                    .modelName("gpt-4o")
                    .temperature(0.7)
                    .maxTokens(2000)
                    .systemPrompt("You are a helpful AI assistant. Please answer in the same language as the user.")
                    .streamEnabled(true)
                    .isDefault(false)
                    .build());
            log.info("已创建 OpenAI 默认配置");
        }

        // Ollama 默认配置
        if (configRepository.findByName("Ollama 默认配置").isEmpty()) {
            configRepository.save(ChatConfig.builder()
                    .name("Ollama 默认配置")
                    .providerType("ollama")
                    .modelName("llama3.2")
                    .temperature(0.7)
                    .maxTokens(2000)
                    .systemPrompt("You are a helpful AI assistant.")
                    .streamEnabled(true)
                    .isDefault(false)
                    .build());
            log.info("已创建 Ollama 默认配置");
        }

        log.info("默认聊天配置初始化完成");
    }
}