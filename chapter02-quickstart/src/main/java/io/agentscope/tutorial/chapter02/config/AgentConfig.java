/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.agentscope.tutorial.chapter02.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Agent 配置类。
 *
 * <p>从 application.yml 读取 AgentScope 相关配置。
 *
 * <p>支持的配置项：
 * <ul>
 *   <li>dashscope - 阿里云百炼模型配置</li>
 *   <li>openai - OpenAI 兼容接口配置</li>
 *   <li>ollama - Ollama 本地模型配置</li>
 * </ul>
 *
 * @author AgentScope Team
 * @since 1.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "agentscope")
public class AgentConfig {

    /**
     * 模型配置。
     */
    private ModelConfig model = new ModelConfig();

    /**
     * 模型配置内部类。
     */
    @Data
    public static class ModelConfig {

        /**
         * DashScope（阿里云百炼）模型配置。
         */
        private DashScopeProperties dashscope = new DashScopeProperties();

        /**
         * OpenAI 兼容接口配置。
         */
        private OpenAIProperties openai = new OpenAIProperties();

        /**
         * Ollama 本地模型配置。
         */
        private OllamaProperties ollama = new OllamaProperties();
    }

    /**
     * DashScope 模型配置属性。
     */
    @Data
    public static class DashScopeProperties {

        /**
         * 是否启用 DashScope 模型。
         */
        private boolean enabled = true;

        /**
         * API Key。
         */
        private String apiKey;

        /**
         * 模型名称。
         * 可选值：qwen-plus（推荐）、qwen-turbo（快速）、qwen-max（最强）
         */
        private String modelName = "qwen-plus";

        /**
         * 是否启用流式输出。
         */
        private boolean stream = true;

        /**
         * 是否启用思考模式。
         */
        private boolean enableThinking = true;

        /**
         * 思考 token 预算。
         */
        private int thinkingBudget = 1024;

        /**
         * 温度参数（控制输出的随机性）。
         * 0 = 确定性强，1 = 创造性高。
         */
        private double temperature = 0.7;
    }

    /**
     * OpenAI 模型配置属性。
     */
    @Data
    public static class OpenAIProperties {

        /**
         * 是否启用 OpenAI 模型。
         */
        private boolean enabled = false;

        /**
         * API 基础地址。
         */
        private String baseUrl = "https://api.openai.com/v1";

        /**
         * API Key。
         */
        private String apiKey;

        /**
         * 模型名称。
         */
        private String modelName = "gpt-4o";
    }

    /**
     * Ollama 本地模型配置属性。
     */
    @Data
    public static class OllamaProperties {

        /**
         * 是否启用 Ollama 模型。
         */
        private boolean enabled = false;

        /**
         * Ollama 服务地址。
         */
        private String baseUrl = "http://localhost:11434";

        /**
         * 模型名称。
         */
        private String modelName = "llama3";
    }
}