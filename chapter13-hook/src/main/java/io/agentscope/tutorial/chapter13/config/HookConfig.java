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
package io.agentscope.tutorial.chapter13.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Hook 配置属性类.
 *
 * <p>从 application.yml 中读取 Hook 相关配置，包括：
 * <ul>
 *   <li>日志钩子配置</li>
 *   <li>监控钩子配置</li>
 *   <li>审计钩子配置</li>
 * </ul>
 *
 * @see io.agentscope.tutorial.chapter13.hook.LoggingHook
 * @see io.agentscope.tutorial.chapter13.hook.MetricsHook
 * @see io.agentscope.tutorial.chapter13.hook.AuditHook
 */
@Data
@Component
@ConfigurationProperties(prefix = "tutorial.hook")
public class HookConfig {

    /** 日志钩子配置 */
    private LoggingConfig logging = new LoggingConfig();

    /** 监控钩子配置 */
    private MetricsConfig metrics = new MetricsConfig();

    /** 审计钩子配置 */
    private AuditConfig audit = new AuditConfig();

    /**
     * 日志钩子配置
     */
    @Data
    public static class LoggingConfig {
        /**
         * 是否记录流式 chunks（可能产生大量日志）
         */
        private boolean logStreamingChunks = false;
    }

    /**
     * 监控钩子配置
     */
    @Data
    public static class MetricsConfig {
        /**
         * 是否启用详细指标收集
         */
        private boolean detailed = true;
    }

    /**
     * 审计钩子配置
     */
    @Data
    public static class AuditConfig {
        /**
         * 敏感工具列表
         */
        private List<String> sensitiveTools = new ArrayList<>();

        /**
         * 是否启用严格模式（阻止未授权工具）
         */
        private boolean strictMode = false;

        /**
         * 允许的工具集合（空表示全部允许）
         */
        private List<String> allowedTools = new ArrayList<>();
    }
}