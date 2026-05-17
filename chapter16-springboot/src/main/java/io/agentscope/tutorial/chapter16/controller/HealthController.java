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
package io.agentscope.tutorial.chapter16.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.自动注入;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 健康检查控制器.
 *
 * <p>提供系统级的健康检查端点：
 * <ul>
 *   <li>GET /api/v1/health - 简单健康状态</li>
 *   <li>GET /api/v1/health/detailed - 详细健康信息</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/health")
@Slf4j
public class HealthController {

    @Autowired
    private DataSource dataSource;

    /**
     * 简单健康检查
     *
     * @return 健康状态
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "timestamp", Instant.now().toString()
        ));
    }

    /**
     * 详细健康检查
     *
     * <p>包含数据库连接状态、应用信息等
     *
     * @return 详细健康信息
     */
    @GetMapping("/detailed")
    public ResponseEntity<Map<String, Object>> detailedHealth() {
        Map<String, Object> healthInfo = new LinkedHashMap<>();
        healthInfo.put("status", "UP");
        healthInfo.put("timestamp", Instant.now().toString());
        healthInfo.put("service", "agentscope-springboot-tutorial");
        healthInfo.put("version", "1.0.0");

        // 数据库健康检查
        Map<String, Object> dbHealth = checkDatabaseHealth();
        healthInfo.put("database", dbHealth);

        return ResponseEntity.ok(healthInfo);
    }

    /**
     * 检查数据库连接状态
     */
    private Map<String, Object> checkDatabaseHealth() {
        Map<String, Object> dbInfo = new LinkedHashMap<>();
        try (Connection connection = dataSource.getConnection()) {
            dbInfo.put("status", "UP");
            dbInfo.put("url", connection.getMetaData().getURL());
            dbInfo.put("driver", connection.getMetaData().getDriverName());
        } catch (Exception e) {
            log.error("数据库连接检查失败: {}", e.getMessage());
            dbInfo.put("status", "DOWN");
            dbInfo.put("error", e.getMessage());
        }
        return dbInfo;
    }
}