package io.agentscope.tutorial.chapter06.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Agent 配置属性类
 *
 * <p>从 application.yml 的 {@code agent} 前缀下读取 Agent 默认配置参数。
 * 这些参数用于初始化 Agent 的默认行为。
 *
 * <p><b>配置示例</b>（application.yml）：
 * <pre>{@code
 * agent:
 *   default-system-prompt: "你是一个有帮助的AI助手。"
 *   default-temperature: 0.7
 *   default-max-tokens: 2000
 *   default-stream-enabled: true
 * }</pre>
 *
 * <p><b>参数说明</b>：
 * <ul>
 *   <li>{@code defaultSystemPrompt} - 系统提示词，定义 Agent 的角色和行为</li>
 *   <li>{@code defaultTemperature} - 生成温度，0-1 范围，越高越随机</li>
 *   <li>{@code defaultMaxTokens} - 最大生成 token 数</li>
 *   <li>{@code defaultStreamEnabled} - 是否启用流式输出</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 * @since 1.0
 */
@Configuration
@ConfigurationProperties(prefix = "agent")
@Data
public class AgentConfig {

    /**
     * 默认系统提示词
     * <p>定义 Agent 的角色、能力和回复风格
     */
    private String defaultSystemPrompt = "You are a helpful AI assistant.";

    /**
     * 默认生成温度
     * <p>取值范围 0.0-1.0：
     * <ul>
     *   <li>0.0 - 完全确定性，输出最可能的结果</li>
     *   <li>0.7 - 平衡创造性和准确性（推荐）</li>
     *   <li>1.0 - 最大随机性，输出更多样化</li>
     * </ul>
     */
    private Double defaultTemperature = 0.7;

    /**
     * 默认最大生成 token 数
     * <p>限制单次回复的长度。不同模型有不同的上限，
     * 如 GPT-4 支持更长的上下文窗口
     */
    private Integer defaultMaxTokens = 2000;

    /**
     * 默认是否启用流式输出
     * <p>启用后可实现打字机效果，提升用户体验
     */
    private Boolean defaultStreamEnabled = true;
}