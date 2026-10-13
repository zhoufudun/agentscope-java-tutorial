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
package io.agentscope.tutorial.chapter17;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 第十七章：部署与生产环境 - 启动类.
 *
 * <p>本章节演示 AgentScope Java 应用的生产环境部署，包括：
 *
 * <ul>
 *   <li>Docker 容器化部署</li>
 *   <li>Kubernetes 集群部署</li>
 *   <li>监控和可观测性配置</li>
 *   <li>高可用与自动扩缩容</li>
 *   <li>优雅关闭与滚动更新</li>
 * </ul>
 *
 * <p>构建 Docker 镜像：
 * <pre>{@code
 * # 构建镜像
 * docker build -t agentscope/chapter17-deployment:1.0.0 .
 *
 * # 使用 Docker Compose 启动
 * docker-compose up -d
 *
 * # 查看日志
 * docker-compose logs -f app
 * }</pre>
 *
 * <p>Kubernetes 部署：
 * <pre>{@code
 * # 创建命名空间和资源
 * kubectl apply -f k8s-common.yaml
 * kubectl apply -f k8s-deployment.yaml
 * kubectl apply -f k8s-hpa.yaml
 *
 * # 查看部署状态
 * kubectl get pods -n agentscope
 * kubectl get svc -n agentscope
 *
 * # 查看日志
 * kubectl logs -f deployment/agentscope-deployment -n agentscope
 * }</pre>
 *
 * <p>Helm 部署：
 * <pre>{@code
 * # 安装 Chart
 * helm install agentscope ./helm/agentscope -n agentscope --create-namespace
 *
 * # 升级
 * helm upgrade agentscope ./helm/agentscope -n agentscope
 *
 * # 查看状态
 * helm status agentscope -n agentscope
 * }</pre>
 */
/**
 * {@code @SpringBootApplication} 组合注解，包含：
 * <ul>
 *   <li>{@code @Configuration} - 标记为配置类</li>
 *   <li>{@code @EnableAutoConfiguration} - 启用 Spring Boot 自动配置</li>
 *   <li>{@code @ComponentScan} - 扫描当前包及子包下的组件</li>
 * </ul>
 *
 * <p><b>生产环境部署要点：</b>
 * <ul>
 *   <li>使用 {@code spring.profiles.active=prod} 激活生产配置</li>
 *   <li>通过环境变量注入敏感配置（API 密钥、数据库密码等）</li>
 *   <li>配置合适的 JVM 参数（堆大小、GC 策略等）</li>
 * </ul>
 */
@SpringBootApplication
/**
 * {@code @EnableAsync} 启用 Spring 异步任务支持.
 *
 * <p>启用后可以使用 {@code @Async} 注解标记异步方法，
 * 配合自定义线程池实现非阻塞的 AI 推理调用。
 *
 * <p><b>生产环境注意事项：</b>
 * <ul>
 *   <li>必须配置自定义线程池，避免使用默认的 SimpleAsyncTaskExecutor</li>
 *   <li>合理设置线程池大小，通常为 CPU 核心数 * 2 到 4</li>
 *   <li>设置队列容量和拒绝策略，防止内存溢出</li>
 * </ul>
 */
@EnableAsync
public class Chapter17Application {

    /**
     * 应用程序入口方法.
     *
     * <p>Spring Boot 应用启动流程：
     * <ol>
     *   <li>加载 Spring 上下文和自动配置</li>
     *   <li>初始化嵌入式 Web 服务器（Tomcat/Netty）</li>
     *   <li>执行 CommandLineRunner 和 ApplicationRunner</li>
     *   <li>发布 ApplicationReadyEvent 事件</li>
     * </ol>
     *
     * <p><b>生产环境启动命令示例：</b>
     * <pre>{@code
     * # 直接启动（推荐使用环境变量注入配置）
     * java -jar app.jar --spring.profiles.active=prod
     *
     * # Docker 容器启动
     * docker run -e SPRING_PROFILES_ACTIVE=prod \
     *            -e DASHSCOPE_API_KEY=sk-xxx \
     *            -p 8080:8080 agentscope/chapter17:latest
     * }</pre>
     *
     * @param args 命令行参数，可传递 Spring 配置覆盖
     */
    public static void main(String[] args) {
        // SpringApplication.run() 是 Spring Boot 应用的核心启动方法
        // 它会创建并配置 ApplicationContext，启动嵌入式服务器
        SpringApplication.run(Chapter17Application.class, args);
    }
}