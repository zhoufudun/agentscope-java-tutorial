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
package io.agentscope.tutorial.chapter02;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AgentScope Java - 第二章：快速开始
 *
 * <p>应用启动类。
 *
 * <p>本示例演示：
 * <ul>
 *   <li>完整的 ReActAgent 配置</li>
 *   <li>多种模型客户端支持（DashScope、OpenAI、Ollama）</li>
 *   <li>简单的对话功能</li>
 *   <li>流式响应支持（SSE）</li>
 * </ul>
 *
 * <p>运行方式：
 * <pre>{@code
 * # 编译打包
 * mvn clean package
 *
 * # 运行应用
 * java -jar target/chapter02-quickstart-1.0.0.jar
 *
 * # 或使用 Maven 运行
 * mvn spring-boot:run
 * }</pre>
 *
 * <p>API 端点：
 * <ul>
 *   <li>POST /api/chat - 发送对话消息</li>
 *   <li>POST /api/chat/stream - 流式对话（SSE）</li>
 *   <li>GET /api/health - 健康检查</li>
 * </ul>
 *
 * @author AgentScope Team
 * @since 1.0
 */
@SpringBootApplication
public class Chapter02Application {

    /**
     * 应用启动入口。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(Chapter02Application.class, args);
    }
}