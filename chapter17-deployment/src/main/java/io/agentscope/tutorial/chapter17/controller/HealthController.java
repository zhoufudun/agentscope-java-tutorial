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
package io.agentscope.tutorial.chapter17.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.PostConstruct;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查控制器.
 *
 * <p>提供生产环境所需的健康检查和监控端点，是 Kubernetes 探针的核心实现。
 * 健康检查是云原生应用的基础能力，确保服务的高可用性和自愈能力。
 *
 * <h2>健康检查端点</h2>
 * <table border="1">
 *   <tr><th>端点</th><th>用途</th><th>Kubernetes 探针</th></tr>
 *   <tr><td>/actuator/health/liveness</td><td>应用是否存活</td><td>livenessProbe</td></tr>
 *   <tr><td>/actuator/health/readiness</td><td>应用是否就绪</td><td>readinessProbe</td></tr>
 *   <tr><td>/actuator/health</td><td>完整健康信息</td><td>startupProbe</td></tr>
 * </table>
 *
 * <h2>Kubernetes 探针说明</h2>
 * <ul>
 *   <li><b>livenessProbe</b> - 检测应用是否存活。如果失败，K8s 会重启 Pod</li>
 *   <li><b>readinessProbe</b> - 检测应用是否就绪。如果失败，K8s 不会将流量发送到该 Pod</li>
 *   <li><b>startupProbe</b> - 检测应用是否已完成启动。启动期间 liveness/readiness 探针会被禁用</li>
 * </ul>
 *
 * <h2>Kubernetes 配置示例</h2>
 * <pre>{@code
 * spec:
 *   containers:
 *   - name: agentscope
 *     livenessProbe:
 *       httpGet:
 *         path: /actuator/health/liveness
 *         port: 8080
 *       initialDelaySeconds: 10
 *       periodSeconds: 10
 *     readinessProbe:
 *       httpGet:
 *         path: /actuator/health/readiness
 *         port: 8080
 *       initialDelaySeconds: 5
 *       periodSeconds: 5
 *     startupProbe:
 *       httpGet:
 *         path: /actuator/health
 *         port: 8080
 *       failureThreshold: 30
 *       periodSeconds: 2
 * }</pre>
 *
 * <h2>设计原则</h2>
 * <ul>
 *   <li><b>快速响应</b> - 探针应在毫秒级返回，避免超时</li>
 *   <li><b>最小依赖</b> - liveness 不应检查外部依赖</li>
 *   <li><b>适度检查</b> - readiness 检查关键依赖，但不要过于严格</li>
 * </ul>
 *
 * @author AgentScope Team
 * @version 1.0.0
 * @see org.springframework.boot.availability.LivenessState
 * @see org.springframework.boot.availability.ReadinessState
 */
@RestController  // REST 控制器
@RequestMapping("/actuator")  // 遵循 Spring Boot Actuator 路径约定
@RequiredArgsConstructor  // Lombok: 构造器注入
@Slf4j  // Lombok: 日志
public class HealthController {

    /** 事件发布器，用于发布可用性状态变化事件 */
    private final ApplicationEventPublisher eventPublisher;

    /** 应用名称，从配置中读取 */
    @Value("${spring.application.name:chapter17-deployment}")
    private String applicationName;

    /** 应用启动时间，用于计算运行时长 */
    private Instant startTime;

    /**
     * 存活状态.
     *
     * <p>CORRECT 表示应用存活，BROKEN 表示应用需要重启。
     * 通常只有在检测到不可恢复的错误时才设置为 BROKEN。
     */
    private LivenessState livenessState = LivenessState.CORRECT;

    /**
     * 就绪状态.
     *
     * <p>ACCEPTING_TRAFFIC 表示应用可以接收流量，
     * REFUSING_TRAFFIC 表示应用暂时无法处理请求。
     */
    private ReadinessState readinessState = ReadinessState.ACCEPTING_TRAFFIC;

    /** 数据库健康检查指示器 */
    private final DatabaseHealthIndicator databaseHealthIndicator;
    /** Redis 健康检查指示器 */
    private final RedisHealthIndicator redisHealthIndicator;

    /**
     * 初始化健康检查控制器.
     *
     * <p>使用 {@link PostConstruct} 确保在依赖注入完成后执行。
     * 记录应用启动时间，用于后续计算运行时长。
     */
    @PostConstruct
    public void init() {
        startTime = Instant.now();
        log.info("HealthController initialized - application: {}", applicationName);
    }

    /**
     * Liveness Probe 端点.
     *
     * <p>Kubernetes 使用此端点判断应用是否存活。
     * 如果返回非 200，Kubernetes 会重启 Pod。
     *
     * <h3>设计原则</h3>
     * <ul>
     *   <li><b>不检查外部依赖</b> - 数据库/Redis 不可用不应导致 Pod 重启</li>
     *   <li><b>快速响应</b> - 应在毫秒级返回</li>
     *   <li><b>保守判断</b> - 只有确定应用"死亡"时才返回失败</li>
     * </ul>
     *
     * <h3>何时返回失败</h3>
     * <ul>
     *   <li>检测到死锁</li>
     *   <li>OOM 导致的不可恢复状态</li>
     *   <li>核心线程池完全阻塞</li>
     * </ul>
     *
     * @return HTTP 200 (UP) 或 HTTP 503 (DOWN)
     */
    @GetMapping("/health/liveness")
    public ResponseEntity<Map<String, Object>> liveness() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("state", livenessState);

        // 只有当应用处于正常状态时返回 200
        if (livenessState == LivenessState.CORRECT) {
            return ResponseEntity.ok(response);
        } else {
            // 返回 503 表示应用需要重启
            return ResponseEntity.status(503).body(response);
        }
    }

    /**
     * Readiness Probe 端点.
     *
     * <p>Kubernetes 使用此端点判断应用是否应该接收流量。
     * 只有当所有依赖服务（数据库、Redis）就绪时才返回 200。
     *
     * <h3>设计原则</h3>
     * <ul>
     *   <li><b>检查关键依赖</b> - 数据库和缓存必须可用才能处理请求</li>
     *   <li><b>快速恢复</b> - 依赖恢复后应立即返回就绪状态</li>
     *   <li><b>适度严格</b> - 不要检查非关键依赖</li>
     * </ul>
     *
     * <h3>何时返回失败</h3>
     * <ul>
     *   <li>数据库连接不可用</li>
     *   <li>Redis 连接不可用</li>
     *   <li>应用正在启动/关闭中</li>
     * </ul>
     *
     * <p>失败时 Kubernetes 会将该 Pod 从 Service 端点移除，
     * 不再向其发送流量，但不会重启 Pod。
     *
     * @return HTTP 200 (ACCEPTING_TRAFFIC) 或 HTTP 503 (REFUSING_TRAFFIC)
     */
    @GetMapping("/health/readiness")
    public ResponseEntity<Map<String, Object>> readiness() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("state", readinessState);

        // 检查所有关键依赖服务的状态
        Map<String, Boolean> components = new HashMap<>();
        components.put("database", databaseHealthIndicator.isHealthy());  // 数据库健康
        components.put("redis", redisHealthIndicator.isHealthy());        // Redis 健康
        response.put("components", components);

        // 只有所有组件都健康，且应用处于接收流量状态时返回 200
        boolean allHealthy = components.values().stream().allMatch(Boolean::booleanValue);

        if (allHealthy && readinessState == ReadinessState.ACCEPTING_TRAFFIC) {
            return ResponseEntity.ok(response);
        } else {
            response.put("status", "DOWN");
            // 返回 503 表示应用不应接收流量
            return ResponseEntity.status(503).body(response);
        }
    }

    /**
     * 完整健康信息端点.
     *
     * <p>返回应用的详细健康信息，包括 JVM 指标、组件状态等。
     * 可用于 Kubernetes 的 startupProbe 或运维监控。
     *
     * <h3>返回信息</h3>
     * <ul>
     *   <li><b>application</b> - 应用名称</li>
     *   <li><b>uptime</b> - 运行时长</li>
     *   <li><b>jvm</b> - JVM 内存、线程、GC 信息</li>
     *   <li><b>components</b> - 各组件健康状态</li>
     * </ul>
     *
     * <h3>生产环境用途</h3>
     * <ul>
     *   <li>接入监控系统（Prometheus/Grafana）</li>
     *   <li>问题排查时查看 JVM 状态</li>
     *   <li>作为 startupProbe 判断启动是否完成</li>
     * </ul>
     *
     * @return 完整的健康信息
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("application", applicationName);
        response.put("status", "UP");
        // 计算应用运行时长（ISO 8601 持续时间格式）
        response.put("uptime", Duration.between(startTime, Instant.now()).toString());

        // JVM 信息 - 用于监控和问题排查
        Map<String, Object> jvm = new HashMap<>();
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();       // 堆内存
        MemoryUsage nonHeapUsage = memoryMXBean.getNonHeapMemoryUsage(); // 非堆内存（Metaspace）

        // 内存使用情况
        jvm.put("memory", Map.of(
            "heap", Map.of(
                "used", heapUsage.getUsed() / 1024 / 1024 + " MB",       // 已使用
                "max", heapUsage.getMax() / 1024 / 1024 + " MB",         // 最大可用
                "usage", String.format("%.2f%%", 100.0 * heapUsage.getUsed() / heapUsage.getMax())  // 使用率
            ),
            "nonHeap", Map.of(
                "used", nonHeapUsage.getUsed() / 1024 / 1024 + " MB",
                "max", nonHeapUsage.getMax() / 1024 / 1024 + " MB"
            )
        ));

        // 线程信息
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        jvm.put("threads", Map.of(
            "count", threadMXBean.getThreadCount(),       // 当前线程数
            "peak", threadMXBean.getPeakThreadCount(),    // 峰值线程数
            "daemon", threadMXBean.getDaemonThreadCount() // 守护线程数
        ));

        // GC 信息
        jvm.put("gc", getGcInfo());
        response.put("jvm", jvm);

        // 组件状态
        Map<String, Object> components = new HashMap<>();
        components.put("database", databaseHealthIndicator.isHealthy() ? "UP" : "DOWN");
        components.put("redis", redisHealthIndicator.isHealthy() ? "UP" : "DOWN");
        response.put("components", components);

        return ResponseEntity.ok(response);
    }

    /**
     * 获取 GC 信息.
     *
     * <p>收集所有垃圾收集器的统计信息，包括 GC 次数和总耗时。
     * 这些信息对于诊断 GC 问题和调优非常重要。
     *
     * <h3>常见 GC 类型</h3>
     * <ul>
     *   <li><b>G1 Young Generation</b> - G1 新生代 GC</li>
     *   <li><b>G1 Old Generation</b> - G1 老年代 GC</li>
     *   <li><b>ZGC</b> - 低延迟 GC（Java 15+）</li>
     * </ul>
     *
     * @return GC 统计信息 Map
     */
    private Map<String, Object> getGcInfo() {
        var gcMxBeans = ManagementFactory.getGarbageCollectorMXBeans();
        Map<String, Object> gcInfo = new HashMap<>();

        for (var gc : gcMxBeans) {
            gcInfo.put(gc.getName(), Map.of(
                "count", gc.getCollectionCount(),          // GC 执行次数
                "time", gc.getCollectionTime() + " ms"     // GC 累计耗时
            ));
        }

        return gcInfo;
    }

    /**
     * 应用就绪事件处理.
     *
     * <p>监听 {@link ApplicationReadyEvent}，在应用完全启动后触发。
     * 此时将 readiness 状态设置为 ACCEPTING_TRAFFIC，
     * 允许 Kubernetes 将流量发送到此 Pod。
     *
     * @param event 应用就绪事件
     */
    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
        log.info("Application is ready - switching to ACCEPTING_TRAFFIC state");
        readinessState = ReadinessState.ACCEPTING_TRAFFIC;
        // 发布状态变化事件，Spring Boot Actuator 会自动更新端点响应
        AvailabilityChangeEvent.publish(eventPublisher, this, ReadinessState.ACCEPTING_TRAFFIC);
    }

    /**
     * 应用启动事件处理.
     *
     * <p>监听 {@link org.springframework.boot.context.event.ApplicationStartedEvent}，
     * 在 Spring 上下文刷新完成后触发（早于 ApplicationReadyEvent）。
     * 此时将 liveness 状态设置为 CORRECT，表示应用存活。
     *
     * @param event 应用启动事件
     */
    @EventListener
    public void onApplicationStarted(org.springframework.boot.context.event.ApplicationStartedEvent event) {
        log.info("Application started - liveness check passed");
        livenessState = LivenessState.CORRECT;
        AvailabilityChangeEvent.publish(eventPublisher, this, LivenessState.CORRECT);
    }

    /**
     * 数据库健康检查指示器.
     *
     * <p>通过执行简单的 SQL 查询验证数据库连接是否正常。
     * 这是最常用的数据库健康检查方式。
     *
     * <h3>检查策略</h3>
     * <ul>
     *   <li>执行 "SELECT 1" 验证连接可用</li>
     *   <li>使用 try-with-resources 确保连接释放</li>
     *   <li>任何异常都视为不健康</li>
     * </ul>
     *
     * <h3>生产环境建议</h3>
     * <ul>
     *   <li>设置查询超时，避免健康检查阻塞</li>
     *   <li>使用只读连接，减少资源消耗</li>
     *   <li>考虑连接池健康而非单次查询</li>
     * </ul>
     */
    @org.springframework.stereotype.Component
    @RequiredArgsConstructor
    public static class DatabaseHealthIndicator {
        /** 数据源，由 Spring Boot 自动配置注入 */
        private final javax.sql.DataSource dataSource;

        /**
         * 检查数据库是否健康.
         *
         * @return true 如果数据库可用，false 如果不可用
         */
        public boolean isHealthy() {
            // 使用 try-with-resources 确保连接、语句、结果集都被正确关闭
            try (var connection = dataSource.getConnection();
                 var statement = connection.createStatement();
                 var resultSet = statement.executeQuery("SELECT 1")) {
                // 能够执行查询并返回结果即为健康
                return resultSet.next();
            } catch (Exception e) {
                // 任何异常都视为数据库不健康
                log.warn("Database health check failed", e);
                return false;
            }
        }
    }

    /**
     * Redis 健康检查指示器.
     *
     * <p>通过 PING 命令验证 Redis 连接是否正常。
     * PING 是 Redis 最轻量的健康检查方式。
     *
     * <h3>检查策略</h3>
     * <ul>
     *   <li>执行 PING 命令，期望返回 "PONG"</li>
     *   <li>任何异常或非 PONG 响应都视为不健康</li>
     * </ul>
     *
     * <h3>生产环境建议</h3>
     * <ul>
     *   <li>对于 Redis 集群，检查所有节点或使用集群健康 API</li>
     *   <li>设置命令超时，避免健康检查阻塞</li>
     *   <li>考虑使用 Lettuce 的异步健康检查</li>
     * </ul>
     */
    @org.springframework.stereotype.Component
    @RequiredArgsConstructor
    public static class RedisHealthIndicator {
        /** Redis 模板，由 Spring Boot 自动配置注入 */
        private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

        /**
         * 检查 Redis 是否健康.
         *
         * @return true 如果 Redis 可用，false 如果不可用
         */
        public boolean isHealthy() {
            try {
                // 执行 PING 命令
                String result = redisTemplate.getConnectionFactory()
                    .getConnection()
                    .ping();
                // 期望返回 "PONG"
                return "PONG".equals(result);
            } catch (Exception e) {
                // 连接异常视为 Redis 不健康
                log.warn("Redis health check failed", e);
                return false;
            }
        }
    }
}