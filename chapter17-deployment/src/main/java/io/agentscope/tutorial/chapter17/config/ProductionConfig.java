/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.agentscope.tutorial.chapter17.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.context.event.EventListener;

import jakarta.annotation.PostConstruct;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 生产环境配置类.
 *
 * <p>提供生产环境特有的配置，包括：
 * <ul>
 *   <li>线程池配置</li>
 *   <li>健康检查端点</li>
 *   <li>构建信息暴露</li>
 *   <li>JVM 优化参数验证</li>
 * </ul>
 *
 * @author AgentScope Team
 * @version 1.0.0
 */
@Configuration
@EnableConfigurationProperties
@EnableScheduling
@Slf4j
public class ProductionConfig {

    /**
     * 异步任务执行器配置.
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-executor-");
        executor.setKeepAliveSeconds(60);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    /**
     * Agent 任务执行器（用于 AI 推理）.
     *
     * <p>标记为 {@link Primary}，作为默认的 {@link Executor}，
     * 以便在存在多个 Executor（taskExecutor / agentExecutor / taskScheduler）时，
     * 按类型注入（如 ChatService）能够无歧义地解析到该 Bean。
     */
    @Bean(name = "agentExecutor")
    @Primary
    public Executor agentExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("agent-executor-");
        executor.setKeepAliveSeconds(120);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    /**
     * 构建信息配置.
     */
    @Bean
    public BuildInfo buildInfo(BuildProperties buildProperties) {
        return new BuildInfo(
            buildProperties.getName(),
            buildProperties.getVersion(),
            buildProperties.getTime()
        );
    }

    /**
     * Git 信息配置（如果有）.
     */
    @Bean
    public GitInfo gitInfo(GitProperties gitProperties) {
        return new GitInfo(
            gitProperties.getCommitId(),
            gitProperties.getBranch()
        );
    }

    /**
     * 应用启动时记录关键信息.
     */
    @PostConstruct
    public void logStartupInfo() {
        log.info("========================================");
        log.info("AgentScope Java - Production Mode Starting");
        log.info("========================================");

        // JVM 信息
        var runtime = ManagementFactory.getRuntimeMXBean();
        log.info("JVM Version: {}", runtime.getVmName());
        log.info("JVM Arguments: {}", runtime.getInputArguments());
        log.info("Java Home: {}", System.getProperty("java.home"));

        // 系统信息
        var os = ManagementFactory.getOperatingSystemMXBean();
        log.info("OS: {} {}",
            os.getName(),
            os.getVersion());
        log.info("Available Processors: {}", os.getAvailableProcessors());

        // 内存信息
        var memory = ManagementFactory.getMemoryMXBean();
        log.info("Heap Memory Init: {} MB",
            memory.getHeapMemoryUsage().getInit() / 1024 / 1024);
        log.info("Heap Memory Max: {} MB",
            memory.getHeapMemoryUsage().getMax() / 1024 / 1024);

        log.info("========================================");
    }

    /**
     * 监听应用可用性变化事件.
     */
    @EventListener
    public void onAvailabilityChange(AvailabilityChangeEvent<?> event) {
        log.info("Application availability changed: {} -> {}",
            event.getState().getClass().getSimpleName(),
            event.getState());
    }

    /**
     * 构建信息记录.
     */
    public record BuildInfo(
        String name,
        String version,
        java.time.Instant buildTime
    ) {}

    /**
     * Git 信息记录.
     */
    public record GitInfo(
        String commitId,
        String branch
    ) {}

    /**
     * 生产环境特定的配置属性.
     */
    @Data
    @Configuration
    @ConfigurationProperties(prefix = "production")
    @Profile("prod")
    public static class ProductionProperties {

        /**
         * 是否启用请求限流.
         */
        private boolean rateLimitingEnabled = true;

        /**
         * 最大并发请求数.
         */
        private int maxConcurrentRequests = 100;

        /**
         * 请求超时时间（毫秒）.
         */
        private int requestTimeoutMs = 30000;

        /**
         * 缓存配置.
         */
        private CacheConfig cache = new CacheConfig();

        /**
         * 缓存配置.
         */
        @Data
        public static class CacheConfig {
            private boolean enabled = true;
            private int maxSize = 1000;
            private int ttlSeconds = 3600;
        }
    }
}