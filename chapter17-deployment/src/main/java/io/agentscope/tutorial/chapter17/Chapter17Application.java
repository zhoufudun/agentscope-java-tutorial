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
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

import io.agentscope.tutorial.chapter17.config.ProductionConfig;

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
@SpringBootApplication
@EnableConfigurationProperties(ProductionConfig.class)
@EnableAsync
public class Chapter17Application {

    public static void main(String[] args) {
        SpringApplication.run(Chapter17Application.class, args);
    }
}