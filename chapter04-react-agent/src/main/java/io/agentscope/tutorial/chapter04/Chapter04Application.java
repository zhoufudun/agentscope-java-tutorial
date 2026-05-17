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
package io.agentscope.tutorial.chapter04;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 第四章教程示例应用启动类
 *
 * 本示例展示如何在 Spring Boot 环境中配置和使用 ReActAgent，
 * 包括工具定义、生命周期钩子、状态管理等核心功能。
 *
 * @author AgentScope Tutorial
 */
@SpringBootApplication
@EnableAsync
public class Chapter04Application {

    public static void main(String[] args) {
        SpringApplication.run(Chapter04Application.class, args);
        System.out.println("===============================================");
        System.out.println("AgentScope Java 教程 - 第四章示例已启动");
        System.out.println("API 文档: http://localhost:8080/swagger-ui.html");
        System.out.println("===============================================");
    }
}