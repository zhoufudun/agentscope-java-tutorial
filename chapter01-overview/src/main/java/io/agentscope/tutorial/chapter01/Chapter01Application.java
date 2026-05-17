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
package io.agentscope.tutorial.chapter01;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * 第一章：AgentScope Java 概述与快速入门 - 启动类.
 *
 * <p>本章节演示 AgentScope Java 的基本使用方法，包括：
 *
 * <ul>
 *   <li>配置 DashScope 模型客户端</li>
 *   <li>创建 ReActAgent</li>
 *   <li>提供 REST API 进行对话</li>
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
 * java -jar target/chapter01-overview-1.0.0.jar
 * }</pre>
 *
 * <p>API 调用示例：
 * <pre>{@code
 * curl -X POST http://localhost:8080/api/chat \
 *   -H "Content-Type: application/json" \
 *   -d '{"message": "你好，请介绍一下你自己"}'
 * }</pre>
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class Chapter01Application {

    public static void main(String[] args) {
        SpringApplication.run(Chapter01Application.class, args);
    }
}