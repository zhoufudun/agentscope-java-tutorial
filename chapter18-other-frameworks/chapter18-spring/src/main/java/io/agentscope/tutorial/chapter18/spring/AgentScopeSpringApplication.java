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
package io.agentscope.tutorial.chapter18.spring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AgentScope Java 教程 - 第18章：Spring Boot 框架集成示例。
 *
 * <h2>概述</h2>
 * <p>本应用演示如何将 AgentScope 与 Spring Boot 框架集成。
 * 使用 agentscope-spring-boot-starter 实现自动配置，
 * 并提供 REST 端点与 Agent 交互。
 *
 * <h2>Spring Boot 核心特性</h2>
 * <ul>
 *   <li><b>自动配置</b> - 根据类路径自动配置 Bean</li>
 *   <li><b>起步依赖</b> - starter 简化依赖管理</li>
 *   <li><b>Actuator</b> - 生产就绪的监控端点</li>
 *   <li><b>外部化配置</b> - application.yml/properties 配置</li>
 * </ul>
 *
 * <h2>访问端点</h2>
 * <ul>
 *   <li><b>API 端点</b>: http://localhost:8080/api/chat</li>
 *   <li><b>Actuator 健康检查</b>: http://localhost:8080/actuator/health</li>
 *   <li><b>H2 控制台</b>: http://localhost:8080/h2-console (dev profile)</li>
 * </ul>
 *
 * <h2>运行方式</h2>
 * <pre>
 * # 使用 Maven
 * DASHSCOPE_API_KEY=your-key mvn spring-boot:run
 *
 * # 使用 JAR
 * DASHSCOPE_API_KEY=your-key java -jar target/chapter18-spring-1.0.0.jar
 * </pre>
 *
 * <h2>Spring Boot 自动配置原理</h2>
 * <ol>
 *   <li>@SpringBootApplication 启用组件扫描和自动配置</li>
 *   <li>spring.factories 中注册自动配置类</li>
 *   <li>@Conditional 注解控制 Bean 创建条件</li>
 *   <li>application.yml 属性绑定到配置类</li>
 * </ol>
 *
 * @author AgentScope Tutorial
 * @see org.springframework.boot.autoconfigure.SpringBootApplication
 */
@SpringBootApplication  // 组合注解：@Configuration + @EnableAutoConfiguration + @ComponentScan
public class AgentScopeSpringApplication {

    /**
     * 应用入口 - 启动 Spring Boot 应用。
     *
     * <h3>启动流程</h3>
     * <ol>
     *   <li>创建 SpringApplication 实例</li>
     *   <li>推断应用类型（Web/非Web）</li>
     *   <li>加载 ApplicationContextInitializer</li>
     *   <li>加载 ApplicationListener</li>
     *   <li>推断主配置类</li>
     *   <li>运行 SpringApplication</li>
     * </ol>
     *
     * <h3>Spring Boot 启动事件</h3>
     * <ul>
     *   <li>ApplicationStartingEvent - 开始启动</li>
     *   <li>ApplicationEnvironmentPreparedEvent - 环境准备完成</li>
     *   <li>ApplicationContextInitializedEvent - 上下文初始化</li>
     *   <li>ApplicationPreparedEvent - 上下文准备完成</li>
     *   <li>ApplicationStartedEvent - 应用启动完成</li>
     *   <li>ApplicationReadyEvent - 应用就绪</li>
     * </ul>
     *
     * @param args 命令行参数，可传递 --server.port=8081 等覆盖配置
     */
    public static void main(String[] args) {
        // SpringApplication.run() 启动 Spring Boot 应用
        // 返回 ConfigurableApplicationContext，可用于程序化访问 Bean
        SpringApplication.run(AgentScopeSpringApplication.class, args);
    }
}