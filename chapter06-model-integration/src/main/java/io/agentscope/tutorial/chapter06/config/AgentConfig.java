package io.agentscope.tutorial.chapter06.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Agent Configuration Properties
 */
@Configuration
@ConfigurationProperties(prefix = "agent")
@Data
public class AgentConfig {

    private String defaultSystemPrompt = "You are a helpful AI assistant.";
    private Double defaultTemperature = 0.7;
    private Integer defaultMaxTokens = 2000;
    private Boolean defaultStreamEnabled = true;
}