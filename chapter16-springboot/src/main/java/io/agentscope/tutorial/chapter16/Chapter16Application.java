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
package io.agentscope.tutorial.chapter16;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * 第十六章：Spring Boot 深度集成 - 启动类.
 *
 * <p>本章节演示 AgentScope Java 与 Spring Boot 3 的深度集成，包括：
 *
 * <ul>
 *   <li>Spring Boot Starter 自动装配</li>
 *   <li>REST API 完整实现</li>
 *   <li>Chat Completions Web 接口</li>
 *   <li>H2 数据库会话管理</li>
 *   <li>多环境配置</li>
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
 * java -jar target/chapter16-springboot-1.0.0.jar
 * }</pre>
 *
 * <p>API 调用示例：
 * <pre>{@code
 * # 健康检查
 * curl http://localhost:8080/api/v1/agent/health
 *
 * # 发送聊天请求
 * curl -X POST http://localhost:8080/api/v1/agent/chat \
 *   -H "Content-Type: application/json" \
 *   -d '{"messages": [{"role": "user", "content": "你好"}]}'
 *
 * # 使用 Chat Completions API（OpenAI 兼容）
 * curl -X POST http://localhost:8080/v1/chat/completions \
 *   -H "Content-Type: application/json" \
 *   -d '{"model": "qwen-plus", "messages": [{"role": "user", "content": "你好"}]}'
 * }</pre>
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class Chapter16Application {

    public static void main(String[] args) {
        SpringApplication.run(Chapter16Application.class, args);
    }
}