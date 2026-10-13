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
package io.agentscope.tutorial.chapter18.micronaut;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.micronaut.context.ApplicationContext;

/**
 * AgentScope Java 教程 - 第18章：Micronaut 框架集成示例。
 *
 * <h2>概述</h2>
 * <p>本应用演示如何将 AgentScope 与 Micronaut 框架集成。
 * 使用 agentscope-micronaut-extension 实现编译时依赖注入，
 * 展示 Micronaut 框架的轻量级特性。
 *
 * <h2>Micronaut 核心特性</h2>
 * <ul>
 *   <li><b>编译时 DI</b> - 在编译期完成依赖注入，无运行时反射开销</li>
 *   <li><b>YAML 配置</b> - 通过 application.yml 配置 Agent</li>
 *   <li><b>低内存占用</b> - 无需维护反射元数据，内存占用更小</li>
 *   <li><b>快速启动</b> - 编译时处理使启动时间大幅缩短</li>
 *   <li><b>Netty HTTP</b> - 基于 Netty 的非阻塞 HTTP 服务器</li>
 * </ul>
 *
 * <h2>Micronaut vs Spring Boot 对比</h2>
 * <table border="1">
 *   <tr><th>特性</th><th>Micronaut</th><th>Spring Boot</th></tr>
 *   <tr><td>DI 时机</td><td>编译时</td><td>运行时</td></tr>
 *   <tr><td>启动时间</td><td>毫秒级</td><td>秒级</td></tr>
 *   <tr><td>内存占用</td><td>低</td><td>较高</td></tr>
 *   <tr><td>GraalVM 支持</td><td>原生支持</td><td>需额外配置</td></tr>
 * </table>
 *
 * <h2>运行方式</h2>
 * <pre>
 * # 设置 API Key 并运行
 * DASHSCOPE_API_KEY=your-key mvn compile exec:java
 * </pre>
 *
 * @author AgentScope Tutorial
 * @see io.micronaut.context.ApplicationContext
 */
public class Application {

    /**
     * 应用入口 - 启动 Micronaut 应用并演示 Agent 对话。
     *
     * <h3>执行流程</h3>
     * <ol>
     *   <li>启动 Micronaut ApplicationContext</li>
     *   <li>从 DI 容器获取自动配置的 ReActAgent</li>
     *   <li>执行多轮对话演示</li>
     *   <li>优雅关闭应用上下文</li>
     * </ol>
     *
     * @param args 命令行参数（本示例未使用）
     */
    public static void main(String[] args) {
        System.out.println("\n=== AgentScope Micronaut Example ===\n");
        System.out.println("Framework: Micronaut (compile-time DI)");
        System.out.println("Key advantage: Fast startup, low memory footprint\n");

        // 启动 Micronaut ApplicationContext
        // Micronaut 在编译时构建依赖注入图，因此启动速度更快、内存占用更低
        // try-with-resources 确保上下文在使用完毕后正确关闭
        try (ApplicationContext context = ApplicationContext.run()) {
            // 从 Micronaut DI 容器获取 ReActAgent
            // Agent 通过 agentscope-micronaut-extension 自动配置
            // 配置来源于 application.yml 文件
            ReActAgent agent = context.getBean(ReActAgent.class);

            // 输出 Agent 配置信息，验证自动配置成功
            System.out.println("✓ Agent created via Micronaut dependency injection");
            System.out.println("✓ Configuration loaded from application.yml");
            System.out.println("✓ Agent name: " + agent.getName());
            System.out.println("✓ Agent max iterations: " + agent.getMaxIters());
            System.out.println();

            // 演示与 Agent 的多轮对话
            String[] questions = {
                "Hello, who are you?",           // 第一轮：自我介绍
                "What are the advantages of Micronaut?",  // 第二轮：询问 Micronaut 优势
                "Thank you, goodbye!"            // 第三轮：结束对话
            };

            // 遍历问题列表，逐个与 Agent 对话
            for (String question : questions) {
                System.out.println("User: " + question);

                // 构建用户消息对象
                // Msg.builder() 提供流式 API 构建消息
                Msg userMsg = Msg.builder()
                        .role(MsgRole.USER)  // 消息角色：用户
                        .content(TextBlock.builder().text(question).build())  // 消息内容
                        .build();

                // 调用 Agent 并阻塞等待响应
                // block() 将响应式 Mono 转换为同步调用
                Msg response = agent.call(userMsg).block();

                // 输出 Agent 响应，处理 null 响应的边界情况
                System.out.println("Agent: " + (response != null ? response.getTextContent() : "No response"));
                System.out.println();
            }

            System.out.println("=== Example completed ===\n");

        } catch (Exception e) {
            // 错误处理：输出错误信息并以非零状态码退出
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}