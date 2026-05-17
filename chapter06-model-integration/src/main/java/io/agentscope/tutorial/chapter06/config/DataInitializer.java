package io.agentscope.tutorial.chapter06.config;

import io.agentscope.tutorial.chapter06.entity.ChatConfig;
import io.agentscope.tutorial.chapter06.entity.ModelProvider;
import io.agentscope.tutorial.chapter06.repository.ChatConfigRepository;
import io.agentscope.tutorial.chapter06.repository.ModelProviderRepository;
import lombok.RequiredArgs构造函数;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Data Initializer
 * Initializes default model provider configuration on application startup
 */
@Component
@RequiredArgs构造函数
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final ModelProviderRepository providerRepository;
    private final ChatConfigRepository configRepository;

    @Override
    public void run(String... args) {
        initializeProviders();
        initialize默认Configs();
    }

    private void initializeProviders() {
        log.info("Initializing model provider configuration...");

        // DashScope
        if (!providerRepository.existsByName("dashscope")) {
            providerRepository.save(ModelProvider.builder()
                    .name("dashscope")
                    .displayName("阿里云百炼 (DashScope)")
                    .baseUrl("https://dashscope.aliyuncs.com/api/v1")
                    .enabled(true)
                    .priority(10)
                    .remark("通义千问系列模型，支持文本和视觉任务")
                    .build());
            log.info("Registered DashScope provider");
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
            log.info("Registered OpenAI provider");
        }

        // Ollama
        if (!providerRepository.existsByName("ollama")) {
            providerRepository.save(ModelProvider.builder()
                    .name("ollama")
                    .displayName("Ollama (本地)")
                    .baseUrl("http://localhost:11434")
                    .enabled(true)
                    .priority(30)
                    .remark("本地开源模型，隐私保护")
                    .build());
            log.info("Registered Ollama provider");
        }

        // Gemini
        if (!providerRepository.existsByName("gemini")) {
            providerRepository.save(ModelProvider.builder()
                    .name("gemini")
                    .displayName("Google Gemini")
                    .baseUrl("https://generativelanguage.googleapis.com")
                    .enabled(true)
                    .priority(40)
                    .remark("Google 多模态模型，支持图像和视频理解")
                    .build());
            log.info("Registered Gemini provider");
        }

        log.info("Model provider initialization completed");
    }

    private void initialize默认Configs() {
        log.info("Initializing default chat configurations...");

        // DashScope 默认 Config
        if (configRepository.findByName("DashScope 默认配置").isEmpty()) {
            configRepository.save(ChatConfig.builder()
                    .name("DashScope 默认配置")
                    .providerType("dashscope")
                    .modelName("qwen-plus")
                    .temperature(0.7)
                    .maxTokens(2000)
                    .systemPrompt("你是一个有帮助的AI助手。请用中文回答问题。")
                    .streamEnabled(true)
                    .is默认(true)
                    .build());
            log.info("Created DashScope default config");
        }

        // OpenAI 默认 Config
        if (configRepository.findByName("OpenAI 默认配置").isEmpty()) {
            configRepository.save(ChatConfig.builder()
                    .name("OpenAI 默认配置")
                    .providerType("openai")
                    .modelName("gpt-4o")
                    .temperature(0.7)
                    .maxTokens(2000)
                    .systemPrompt("You are a helpful AI assistant. Please answer in the same language as the user.")
                    .streamEnabled(true)
                    .is默认(false)
                    .build());
            log.info("Created OpenAI default config");
        }

        // Ollama 默认 Config
        if (configRepository.findByName("Ollama 默认配置").isEmpty()) {
            configRepository.save(ChatConfig.builder()
                    .name("Ollama 默认配置")
                    .providerType("ollama")
                    .modelName("llama3.2")
                    .temperature(0.7)
                    .maxTokens(2000)
                    .systemPrompt("You are a helpful AI assistant.")
                    .streamEnabled(true)
                    .is默认(false)
                    .build());
            log.info("Created Ollama default config");
        }

        log.info("默认 chat configuration initialization completed");
    }
}