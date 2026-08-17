package io.agentscope.tutorial.chapter11.config;

import io.agentscope.extensions.model.dashscope.DashScopeChatModel;
import io.agentscope.core.model.Model;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 模型配置
 *
 * <p>负责初始化 AgentScope 模型实例。
 * 支持 DashScope（阿里云百炼）等模型后端。
 *
 * <p>配置示例（application.yml）：
 * <pre>{@code
 * agentscope:
 *   model:
 *     type: dashscope
 *     api-key: ${DASHSCOPE_API_KEY}
 *     model-name: qwen-plus
 * }</pre>
 *
 * @author AgentScope Tutorial
 */
@Configuration
@Slf4j
public class ModelConfig {

    /**
     * DashScope API Key
     */
    @Value("${agentscope.model.api-key:}")
    private String apiKey;

    /**
     * 模型名称
     */
    @Value("${agentscope.model.model-name:qwen-plus}")
    private String modelName;

    /**
     * 创建 DashScope 模型实例
     *
     * <p>使用阿里云百炼作为模型后端，支持通义千问等模型。
     *
     * @return Model 实例
     */
    @Bean
    public Model dashscopeModel() {
        if (apiKey == null || apiKey.isBlank() || "your-api-key-here".equals(apiKey)) {
            log.warn("DashScope API key not configured, using placeholder model");
            return createPlaceholderModel();
        }

        try {
            DashScopeChatModel chatModel = DashScopeChatModel.builder()
                    .apiKey(apiKey)
                    .modelName(modelName)
                    .build();

            log.info("DashScope model initialized: {}", modelName);
            return chatModel;

        } catch (Exception e) {
            log.error("Failed to initialize DashScope model: {}", e.getMessage());
            return createPlaceholderModel();
        }
    }

    /**
     * 创建占位符模型（用于无 API Key 的测试场景）
     */
    private Model createPlaceholderModel() {
        return new Model() {
            @Override
            public String getModelName() {
                return modelName;
            }

            @Override
            public reactor.core.publisher.Flux<io.agentscope.core.model.ChatResponse> stream(
                    java.util.List<io.agentscope.core.message.Msg> messages,
                    java.util.List<io.agentscope.core.model.ToolSchema> tools,
                    io.agentscope.core.model.GenerateOptions options) {
                return reactor.core.publisher.Flux.empty();
            }
        };
    }
}