package io.agentscope.tutorial.chapter08.config;

import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.model.Model;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * 模型配置
 *
 * 配置 AgentScope DashScope 模型，所有 Agent 共享同一个模型实例。
 * 在生产环境中，可以为不同 Agent 配置不同的模型（如 qwen-max 用于复杂任务，
 * qwen-turbo 用于简单查询）以优化成本和性能。
 */
@Configuration
public class MultiAgentConfig {

    @Value("${spring.ai.dashscope.api-key:}")
    private String apiKey;

    /**
     * 主模型 Bean：用于所有 Agent
     * 使用 qwen-plus 模型，平衡能力与成本
     */
    @Bean
    public Model primaryModel() {
        String key = StringUtils.hasText(apiKey)
            ? apiKey
            : System.getenv("AI_DASHSCOPE_API_KEY");

        return DashScopeChatModel.builder()
            .apiKey(key)
            .modelName("qwen-plus")
            .build();
    }
}