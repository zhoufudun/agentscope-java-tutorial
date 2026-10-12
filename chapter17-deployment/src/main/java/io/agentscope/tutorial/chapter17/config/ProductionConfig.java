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
 * <p>本配置类为 AgentScope Java 应用提供生产环境所需的核心配置，
 * 是构建高可用、可观测、可扩展 AI 服务的基础。
 *
 * <h2>核心配置项</h2>
 * <ul>
 *   <li><b>线程池配置</b> - 为异步任务和 AI 推理提供独立的线程池，避免资源竞争</li>
 *   <li><b>健康检查端点</b> - 支持 Kubernetes 的 Liveness/Readiness 探针</li>
 *   <li><b>构建信息暴露</b> - 通过 BuildProperties 和 GitProperties 暴露版本信息</li>
 *   <li><b>JVM 参数验证</b> - 启动时记录关键 JVM 配置，便于问题排查</li>
 * </ul>
 *
 * <h2>生产环境最佳实践</h2>
 * <ol>
 *   <li>使用 {@code @Profile("prod")} 隔离环境特定配置</li>
 *   <li>通过 {@code @ConfigurationProperties} 外部化配置</li>
 *   <li>配置优雅关闭（Graceful Shutdown）确保请求完整处理</li>
 *   <li>启用监控指标（Micrometer + Prometheus）</li>
 * </ol>
 *
 * <h2>Kubernetes 部署示例</h2>
 * <pre>{@code
 * # 环境变量配置示例
 * env:
 *   - name: SPRING_PROFILES_ACTIVE
 *     value: "prod"
 *   - name: JAVA_OPTS
 *     value: "-Xms512m -Xmx2g -XX:+UseG1GC"
 * }</pre>
 *
 * @author AgentScope Team
 * @version 1.0.0
 * @see org.springframework.boot.context.properties.EnableConfigurationProperties
 * @see org.springframework.scheduling.annotation.EnableScheduling
 */
@Configuration  // 标记为 Spring 配置类，可定义 @Bean 方法
@EnableConfigurationProperties  // 启用 @ConfigurationProperties 支持
@EnableScheduling  // 启用定时任务支持（用于健康检查、指标收集等）
@Slf4j  // Lombok 注解，自动生成 log 字段
public class ProductionConfig {

    /**
     * 异步任务执行器配置.
     *
     * <p>用于处理一般性的异步任务，如日志记录、通知发送等。
     * 与 agentExecutor 分离，避免 AI 推理任务阻塞其他异步操作。
     *
     * <h3>线程池参数说明</h3>
     * <ul>
     *   <li><b>corePoolSize=4</b> - 核心线程数，始终保持存活</li>
     *   <li><b>maxPoolSize=16</b> - 最大线程数，队列满时扩展</li>
     *   <li><b>queueCapacity=100</b> - 任务队列容量，超出触发拒绝策略</li>
     *   <li><b>keepAliveSeconds=60</b> - 非核心线程空闲存活时间</li>
     * </ul>
     *
     * <h3>生产环境调优建议</h3>
     * <ul>
     *   <li>根据服务器 CPU 核心数调整 corePoolSize（通常为核心数 * 2）</li>
     *   <li>监控队列长度，若频繁达到上限需增加 queueCapacity 或 maxPoolSize</li>
     *   <li>CallerRunsPolicy 拒绝策略可实现背压，防止系统过载</li>
     * </ul>
     *
     * @return 配置完成的线程池执行器
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数：即使空闲也保持的线程数量
        executor.setCorePoolSize(4);
        // 最大线程数：队列满时可扩展到的上限
        executor.setMaxPoolSize(16);
        // 队列容量：核心线程繁忙时，新任务进入队列等待
        executor.setQueueCapacity(100);
        // 线程名前缀：便于日志追踪和问题排查
        executor.setThreadNamePrefix("async-executor-");
        // 空闲线程存活时间：超过核心数的线程空闲多久后回收
        executor.setKeepAliveSeconds(60);
        // 拒绝策略：队列满且达到最大线程数时，由调用者线程执行任务（实现背压）
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 优雅关闭：等待正在执行的任务完成
        executor.setWaitForTasksToCompleteOnShutdown(true);
        // 关闭时最大等待时间
        executor.setAwaitTerminationSeconds(30);
        // 初始化线程池
        executor.initialize();
        return executor;
    }

    /**
     * Agent 任务执行器（专用于 AI 推理）.
     *
     * <p>为 AI 推理任务配置独立的线程池，隔离资源使用，
     * 避免耗时的 LLM 调用阻塞其他业务逻辑。
     *
     * <h3>设计考量</h3>
     * <ul>
     *   <li><b>独立隔离</b> - AI 推理通常耗时较长（数秒），需要独立线程池</li>
     *   <li><b>较小容量</b> - LLM API 通常有并发限制，过多并发反而降低吞吐</li>
     *   <li><b>较长存活</b> - 推理任务间隔可能较长，保持线程减少创建开销</li>
     * </ul>
     *
     * <h3>线程池参数说明</h3>
     * <ul>
     *   <li><b>corePoolSize=2</b> - 保守配置，避免占用过多 LLM API 并发配额</li>
     *   <li><b>maxPoolSize=8</b> - 峰值时可扩展，但受限于 API 并发限制</li>
     *   <li><b>queueCapacity=50</b> - 适度队列，超出时由调用者执行实现背压</li>
     *   <li><b>keepAliveSeconds=120</b> - 较长存活时间，减少线程创建销毁</li>
     * </ul>
     *
     * <p>标记为 {@link Primary}，作为默认的 {@link Executor}，
     * 以便在存在多个 Executor（taskExecutor / agentExecutor / taskScheduler）时，
     * 按类型注入（如 ChatService）能够无歧义地解析到该 Bean。
     *
     * @return 配置完成的 Agent 专用线程池执行器
     */
    @Bean(name = "agentExecutor")
    @Primary  // 标记为主要 Bean，当有多个 Executor 类型时优先注入此 Bean
    public Executor agentExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数：AI 推理并发数，需考虑 LLM API 的并发限制
        executor.setCorePoolSize(2);
        // 最大线程数：允许的最大并发推理数
        executor.setMaxPoolSize(8);
        // 队列容量：等待推理的请求数
        executor.setQueueCapacity(50);
        // 线程名前缀：agent-executor-1, agent-executor-2, ...
        executor.setThreadNamePrefix("agent-executor-");
        // 较长的存活时间：AI 推理间隔可能较长
        executor.setKeepAliveSeconds(120);
        // 拒绝策略：实现背压，防止请求堆积导致 OOM
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 优雅关闭：确保正在进行的 AI 推理能够完成
        executor.setWaitForTasksToCompleteOnShutdown(true);
        // AI 推理可能较慢，给予更长的等待时间
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    /**
     * 构建信息配置.
     *
     * <p>通过 Spring Boot Actuator 的 {@link BuildProperties} 获取构建信息，
     * 这些信息来自 Maven/Gradle 在构建时生成的 {@code META-INF/build-info.properties} 文件。
     *
     * <h3>生产环境用途</h3>
     * <ul>
     *   <li><b>版本追踪</b> - 快速确认部署的版本号</li>
     *   <li><b>问题排查</b> - 确认构建时间，定位代码变更</li>
     *   <li><b>监控集成</b> - 将版本信息暴露给 Prometheus/Grafana</li>
     * </ul>
     *
     * <p><b>Maven 配置示例：</b>
     * <pre>{@code
     * <plugin>
     *   <groupId>org.springframework.boot</groupId>
     *   <artifactId>spring-boot-maven-plugin</artifactId>
     *   <executions>
     *     <execution>
     *       <goals>
     *         <goal>build-info</goal>
     *       </goals>
     *     </execution>
     *   </executions>
     * </plugin>
     * }</pre>
     *
     * @param buildProperties Spring Boot 注入的构建属性
     * @return 封装后的构建信息对象
     */
    @Bean
    public BuildInfo buildInfo(BuildProperties buildProperties) {
        return new BuildInfo(
            buildProperties.getName(),      // 应用名称
            buildProperties.getVersion(),   // 版本号（如 1.0.0）
            buildProperties.getTime()       // 构建时间戳
        );
    }

    /**
     * Git 信息配置（如果有）.
     *
     * <p>通过 {@link GitProperties} 获取 Git 提交信息，
     * 这些信息来自构建时生成的 {@code git.properties} 文件。
     *
     * <h3>生产环境用途</h3>
     * <ul>
     *   <li><b>代码追溯</b> - 通过 commitId 精确定位代码版本</li>
     *   <li><b>分支确认</b> - 验证部署的代码来自正确的分支</li>
     *   <li><b>CI/CD 集成</b> - 与流水线联动，记录部署历史</li>
     * </ul>
     *
     * <p><b>Maven 配置示例：</b>
     * <pre>{@code
     * <plugin>
     *   <groupId>io.github.git-commit-id</groupId>
     *   <artifactId>git-commit-id-maven-plugin</artifactId>
     *   <executions>
     *     <execution>
     *       <goals>
     *         <goal>revision</goal>
     *       </goals>
     *     </execution>
     *   </executions>
     * </plugin>
     * }</pre>
     *
     * @param gitProperties Spring Boot 注入的 Git 属性
     * @return 封装后的 Git 信息对象
     */
    @Bean
    public GitInfo gitInfo(GitProperties gitProperties) {
        return new GitInfo(
            gitProperties.getCommitId(),  // Git 提交 SHA（短格式）
            gitProperties.getBranch()      // Git 分支名
        );
    }

    /**
     * 应用启动时记录关键信息.
     *
     * <p>使用 {@link PostConstruct} 注解确保在 Bean 初始化完成后执行，
     * 记录 JVM、操作系统、内存等关键信息，便于生产环境问题排查。
     *
     * <h3>记录内容</h3>
     * <ul>
     *   <li><b>JVM 版本</b> - 确认运行时环境</li>
     *   <li><b>JVM 参数</b> - 检查内存、GC 等配置是否正确</li>
     *   <li><b>操作系统</b> - 记录部署环境</li>
     *   <li><b>CPU 核心数</b> - 验证线程池配置是否合理</li>
     *   <li><b>堆内存配置</b> - 确认内存分配</li>
     * </ul>
     *
     * <h3>生产环境最佳实践</h3>
     * <ul>
     *   <li>确保日志级别设置为 INFO 或以上</li>
     *   <li>将启动日志发送到集中式日志系统（ELK/Loki）</li>
     *   <li>设置告警规则，监控启动失败或异常</li>
     * </ul>
     */
    @PostConstruct
    public void logStartupInfo() {
        log.info("========================================");
        log.info("AgentScope Java - Production Mode Starting");
        log.info("========================================");

        // JVM 信息 - 用于确认运行环境和参数配置
        var runtime = ManagementFactory.getRuntimeMXBean();
        log.info("JVM Version: {}", runtime.getVmName());
        // JVM 启动参数：-Xms, -Xmx, -XX:+UseG1GC 等
        log.info("JVM Arguments: {}", runtime.getInputArguments());
        log.info("Java Home: {}", System.getProperty("java.home"));

        // 系统信息 - 用于确认部署环境
        var os = ManagementFactory.getOperatingSystemMXBean();
        log.info("OS: {} {}",
            os.getName(),
            os.getVersion());
        // CPU 核心数：影响线程池配置
        log.info("Available Processors: {}", os.getAvailableProcessors());

        // 内存信息 - 用于验证 JVM 内存配置
        var memory = ManagementFactory.getMemoryMXBean();
        // 初始堆大小：-Xms 参数
        log.info("Heap Memory Init: {} MB",
            memory.getHeapMemoryUsage().getInit() / 1024 / 1024);
        // 最大堆大小：-Xmx 参数
        log.info("Heap Memory Max: {} MB",
            memory.getHeapMemoryUsage().getMax() / 1024 / 1024);

        log.info("========================================");
    }

    /**
     * 监听应用可用性变化事件.
     *
     * <p>Spring Boot 2.3+ 引入的可用性状态管理机制，
     * 支持 Kubernetes 的 Liveness/Readiness 探针。
     *
     * <h3>可用性状态</h3>
     * <ul>
     *   <li><b>LivenessState</b> - 应用是否存活（CORRECT/BROKEN）</li>
     *   <li><b>ReadinessState</b> - 应用是否就绪（ACCEPTING_TRAFFIC/REFUSING_TRAFFIC）</li>
     * </ul>
     *
     * <h3>Kubernetes 探针映射</h3>
     * <ul>
     *   <li>{@code /actuator/health/liveness} - livenessProbe</li>
     *   <li>{@code /actuator/health/readiness} - readinessProbe</li>
     * </ul>
     *
     * @param event 可用性变化事件，包含新的状态信息
     */
    @EventListener
    public void onAvailabilityChange(AvailabilityChangeEvent<?> event) {
        // 记录状态变化，便于追踪应用生命周期
        log.info("Application availability changed: {} -> {}",
            event.getState().getClass().getSimpleName(),
            event.getState());
    }

    /**
     * 构建信息记录.
     *
     * <p>使用 Java 16+ 的 Record 特性定义不可变数据类，
     * 自动生成 equals、hashCode、toString 等方法。
     *
     * <h3>字段说明</h3>
     * <ul>
     *   <li><b>name</b> - 应用名称（来自 pom.xml 的 artifactId）</li>
     *   <li><b>version</b> - 版本号（来自 pom.xml 的 version）</li>
     *   <li><b>buildTime</b> - 构建时间戳（UTC 时间）</li>
     * </ul>
     *
     * @param name      应用名称
     * @param version   版本号
     * @param buildTime 构建时间
     */
    public record BuildInfo(
        String name,
        String version,
        java.time.Instant buildTime
    ) {}

    /**
     * Git 信息记录.
     *
     * <p>记录 Git 提交信息，用于代码版本追溯。
     *
     * <h3>字段说明</h3>
     * <ul>
     *   <li><b>commitId</b> - Git 提交 SHA（短格式，7 位）</li>
     *   <li><b>branch</b> - Git 分支名（如 main、develop）</li>
     * </ul>
     *
     * @param commitId Git 提交 ID
     * @param branch   Git 分支名
     */
    public record GitInfo(
        String commitId,
        String branch
    ) {}

    /**
     * 生产环境特定的配置属性.
     *
     * <p>使用 {@link ConfigurationProperties} 从配置文件绑定属性，
     * 支持类型安全的配置管理和 IDE 自动补全。
     *
     * <h3>配置示例（application-prod.yml）</h3>
     * <pre>{@code
     * production:
     *   rate-limiting-enabled: true
     *   max-concurrent-requests: 100
     *   request-timeout-ms: 30000
     *   cache:
     *     enabled: true
     *     max-size: 1000
     *     ttl-seconds: 3600
     * }</pre>
     *
     * <h3>环境变量覆盖</h3>
     * <pre>{@code
     * # Kubernetes ConfigMap 或环境变量
     * PRODUCTION_RATE_LIMITING_ENABLED=true
     * PRODUCTION_MAX_CONCURRENT_REQUESTS=200
     * }</pre>
     *
     * <p>使用 {@link Profile} 注解确保只在 prod 环境加载，
     * 避免开发环境受到生产配置影响。
     */
    @Data  // Lombok: 生成 getter/setter/toString/equals/hashCode
    @Configuration  // 标记为配置类，允许作为 Bean 注入
    @ConfigurationProperties(prefix = "production")  // 绑定 production.* 配置
    @Profile("prod")  // 仅在 prod profile 激活时加载
    public static class ProductionProperties {

        /**
         * 是否启用请求限流.
         *
         * <p>生产环境建议开启，防止 API 被过度调用导致服务不可用。
         * 限流策略可与 Resilience4j 或 Sentinel 集成。
         */
        private boolean rateLimitingEnabled = true;

        /**
         * 最大并发请求数.
         *
         * <p>控制同时处理的请求数量，超出时新请求将被拒绝或排队。
         * 需根据服务器资源和 LLM API 并发限制调整。
         */
        private int maxConcurrentRequests = 100;

        /**
         * 请求超时时间（毫秒）.
         *
         * <p>AI 推理可能耗时较长，建议设置 30-60 秒。
         * 超时后请求将被取消，避免资源长时间占用。
         */
        private int requestTimeoutMs = 30000;

        /**
         * 缓存配置.
         *
         * <p>对相同问题的 AI 响应进行缓存，
         * 减少 LLM API 调用次数和响应延迟。
         */
        private CacheConfig cache = new CacheConfig();

        /**
         * 缓存配置.
         *
         * <p>生产环境缓存策略配置，用于优化重复请求的处理。
         *
         * <h3>缓存策略</h3>
         * <ul>
         *   <li><b>enabled</b> - 是否启用缓存</li>
         *   <li><b>maxSize</b> - 最大缓存条目数（LRU 淘汰）</li>
         *   <li><b>ttlSeconds</b> - 缓存过期时间（秒）</li>
         * </ul>
         *
         * <h3>最佳实践</h3>
         * <ul>
         *   <li>对确定性问题（FAQ）启用缓存</li>
         *   <li>对需要实时性的问题禁用缓存</li>
         *   <li>根据内存大小调整 maxSize</li>
         * </ul>
         */
        @Data
        public static class CacheConfig {
            /** 是否启用缓存 */
            private boolean enabled = true;
            /** 最大缓存条目数 */
            private int maxSize = 1000;
            /** 缓存过期时间（秒） */
            private int ttlSeconds = 3600;
        }
    }
}