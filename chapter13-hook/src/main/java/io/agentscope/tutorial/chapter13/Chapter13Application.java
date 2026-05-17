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
package io.agentscope.tutorial.chapter13;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * AgentScope Hook 系统教程 - Spring Boot 启动类.
 *
 * <p>本章节演示如何通过 Spring Boot 3 + Java 21 集成 AgentScope Java 的 Hook 机制，
 * 实现日志记录、性能监控和工具调用审计。
 *
 * <p>主要功能：
 * <ul>
 *   <li>配置多种内置和自定义 Hook</li>
 *   <li>实现 LoggingHook 日志钩子</li>
 *   <li>实现 MetricsHook 监控钩子</li>
 *   <li>实现 AuditHook 审计钩子</li>
 *   <li>配置钩子优先级链</li>
 * </ul>
 *
 * <p>启动方式：
 * <pre>{@code
 * # 设置 DashScope API Key
 * export DASHSCOPE_API_KEY=your-api-key
 *
 * # 启动应用
 * mvn spring-boot:run
 *
 * # 或者运行主类
 * java -jar target/chapter13-hook-1.0.0.jar
 * }</pre>
 *
 * <p>API 调用示例：
 * <pre>{@code
 * # 调用 Agent
 * curl -X POST http://localhost:8080/api/chat/call \
 *   -H "Content-Type: application/json" \
 *   -d '{"message": "Calculate 123 + 456"}'
 *
 * # 查看性能指标
 * curl http://localhost:8080/api/chat/metrics
 *
 * # 查看工具审计记录
 * curl http://localhost:8080/api/audit/records
 *
 * # 健康检查
 * curl http://localhost:8080/api/chat/health
 * }</pre>
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class Chapter13Application {

    public static void main(String[] args) {
        SpringApplication.run(Chapter13Application.class, args);
    }
}