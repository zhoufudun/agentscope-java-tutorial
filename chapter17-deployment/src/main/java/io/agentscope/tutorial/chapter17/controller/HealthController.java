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

import lombok.RequiredArgs构造函数;
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
 * <p>提供生产环境所需的健康检查和监控端点：
 * <ul>
 *   <li>Liveness Probe - 判断应用是否存活</li>
 *   <li>Readiness Probe - 判断应用是否就绪</li>
 *   <li>详细健康信息</li>
 *   <li>JVM 指标</li>
 * </ul>
 *
 * <p>这些端点被 Kubernetes 用于：
 * <ul>
 *   <li>livenessProbe - 检测应用是否需要重启</li>
 *   <li>readinessProbe - 检测应用是否应该接收流量</li>
 *   <li>startupProbe - 检测应用是否已完成启动</li>
 * </ul>
 *
 * @author AgentScope Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/actuator")
@RequiredArgs构造函数
@Slf4j
public class HealthController {

    private final ApplicationEventPublisher eventPublisher;

    @Value("${spring.application.name:chapter17-deployment}")
    private String applicationName;

    private Instant startTime;
    private LivenessState livenessState = LivenessState.CORRECT;
    private ReadinessState readinessState = ReadinessState.ACCEPTING_TRAFFIC;

    // 健康检查组件
    private final DatabaseHealthIndicator databaseHealthIndicator;
    private final RedisHealthIndicator redisHealthIndicator;

    @PostConstruct
    public void init() {
        startTime = Instant.now();
        log.info("HealthController initialized - application: {}", applicationName);
    }

    /**
     * Liveness Probe 端点.
     *
     * <p>Kubernetes 使用此端点判断应用是否存活。
     * 如果返回非 200，应用将被重启。
     */
    @GetMapping("/health/liveness")
    public ResponseEntity<Map<String, Object>> liveness() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("state", livenessState);

        if (livenessState == LivenessState.CORRECT) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(503).body(response);
        }
    }

    /**
     * Readiness Probe 端点.
     *
     * <p>Kubernetes 使用此端点判断应用是否应该接收流量。
     * 只有当所有依赖服务（数据库、Redis）就绪时返回 200。
     */
    @GetMapping("/health/readiness")
    public ResponseEntity<Map<String, Object>> readiness() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("state", readinessState);

        // 检查依赖服务状态
        Map<String, Boolean> components = new HashMap<>();
        components.put("database", databaseHealthIndicator.isHealthy());
        components.put("redis", redisHealthIndicator.isHealthy());
        response.put("components", components);

        boolean allHealthy = components.values().stream().allMatch(Boolean::booleanValue);

        if (allHealthy && readinessState == ReadinessState.ACCEPTING_TRAFFIC) {
            return ResponseEntity.ok(response);
        } else {
            response.put("status", "DOWN");
            return ResponseEntity.status(503).body(response);
        }
    }

    /**
     * 完整健康信息端点.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("application", applicationName);
        response.put("status", "UP");
        response.put("uptime", Duration.between(startTime, Instant.now()).toString());

        // JVM 信息
        Map<String, Object> jvm = new HashMap<>();
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryMXBean.getNonHeapMemoryUsage();

        jvm.put("memory", Map.of(
            "heap", Map.of(
                "used", heapUsage.getUsed() / 1024 / 1024 + " MB",
                "max", heapUsage.getMax() / 1024 / 1024 + " MB",
                "usage", String.format("%.2f%%", 100.0 * heapUsage.getUsed() / heapUsage.getMax())
            ),
            "nonHeap", Map.of(
                "used", nonHeapUsage.getUsed() / 1024 / 1024 + " MB",
                "max", nonHeapUsage.getMax() / 1024 / 1024 + " MB"
            )
        ));

        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        jvm.put("threads", Map.of(
            "count", threadMXBean.getThreadCount(),
            "peak", threadMXBean.getPeakThreadCount(),
            "daemon", threadMXBean.getDaemonThreadCount()
        ));

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
     */
    private Map<String, Object> getGcInfo() {
        var gcMxBeans = ManagementFactory.getGarbageCollectorMXBeans();
        Map<String, Object> gcInfo = new HashMap<>();

        for (var gc : gcMxBeans) {
            gcInfo.put(gc.getName(), Map.of(
                "count", gc.getCollectionCount(),
                "time", gc.getCollectionTime() + " ms"
            ));
        }

        return gcInfo;
    }

    /**
     * 应用就绪事件.
     */
    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
        log.info("Application is ready - switching to ACCEPTING_TRAFFIC state");
        readinessState = ReadinessState.ACCEPTING_TRAFFIC;
        eventPublisher.publishEvent(new AvailabilityChangeEvent<>(this, readinessState, ReadinessState.ACCEPTING_TRAFFIC));
    }

    /**
     * 应用启动事件.
     */
    @EventListener
    public void onApplicationStarted(org.springframework.boot.context.event.ApplicationStartedEvent event) {
        log.info("Application started - liveness check passed");
        livenessState = LivenessState.CORRECT;
        eventPublisher.publishEvent(new AvailabilityChangeEvent<>(this, livenessState, LivenessState.CORRECT));
    }

    /**
     * 数据库健康检查指示器.
     */
    @org.springframework.stereotype.Component
    @RequiredArgs构造函数
    public static class DatabaseHealthIndicator {
        private final javax.sql.DataSource dataSource;

        public boolean isHealthy() {
            try (var connection = dataSource.getConnection();
                 var statement = connection.createStatement();
                 var resultSet = statement.executeQuery("SELECT 1")) {
                return resultSet.next();
            } catch (Exception e) {
                log.warn("Database health check failed", e);
                return false;
            }
        }
    }

    /**
     * Redis 健康检查指示器.
     */
    @org.springframework.stereotype.Component
    @RequiredArgs构造函数
    public static class RedisHealthIndicator {
        private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

        public boolean isHealthy() {
            try {
                String result = redisTemplate.getConnectionFactory()
                    .getConnection()
                    .ping();
                return "PONG".equals(result);
            } catch (Exception e) {
                log.warn("Redis health check failed", e);
                return false;
            }
        }
    }
}