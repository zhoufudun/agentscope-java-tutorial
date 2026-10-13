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
package io.agentscope.tutorial.chapter18.quarkus;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import lombok.extern.slf4j.Slf4j;

/**
 * AgentScope Java 教程 - 第18章：Quarkus 框架集成示例。
 *
 * <h2>概述</h2>
 * <p>本应用演示如何将 AgentScope 与 Quarkus 框架集成。
 * 使用 agentscope-quarkus-extension 实现构建时优化，
 * 支持 GraalVM 原生镜像编译。
 *
 * <h2>Quarkus 核心特性</h2>
 * <ul>
 *   <li><b>构建时 CDI (ArC)</b> - 在构建时处理 CDI，消除运行时反射</li>
 *   <li><b>开发模式热重载</b> - 代码修改后自动重新编译和部署</li>
 *   <li><b>GraalVM 原生支持</b> - 直接编译为原生可执行文件</li>
 *   <li><b>超快启动</b> - 毫秒级启动时间（原生镜像更快）</li>
 *   <li><b>低内存占用</b> - 适合容器化和 Serverless 部署</li>
 * </ul>
 *
 * <h2>运行方式</h2>
 * <pre>
 * # 开发模式（支持热重载）
 * DASHSCOPE_API_KEY=your-key mvn quarkus:dev
 *
 * # JAR 包运行
 * DASHSCOPE_API_KEY=your-key mvn package
 * java -jar target/quarkus-app/quarkus-run.jar
 *
 * # 编译原生镜像（需要 GraalVM）
 * mvn package -Pnative
 * ./target/chapter18-quarkus-1.0.0-runner
 * </pre>
 *
 * <h2>Quarkus vs 传统 Java 框架</h2>
 * <table border="1">
 *   <tr><th>指标</th><th>Quarkus 原生</th><th>Quarkus JVM</th><th>传统 Spring</th></tr>
 *   <tr><td>启动时间</td><td>~10ms</td><td>~1s</td><td>~5s</td></tr>
 *   <tr><td>内存占用</td><td>~10MB</td><td>~70MB</td><td>~200MB</td></tr>
 *   <tr><td>首请求延迟</td><td>低</td><td>中</td><td>高（JIT预热）</td></tr>
 * </table>
 *
 * @author AgentScope Tutorial
 * @see io.quarkus.runtime.annotations.QuarkusMain
 * @see io.quarkus.runtime.QuarkusApplication
 */
@Slf4j
@QuarkusMain  // 标记为 Quarkus 应用入口，允许自定义 main 方法逻辑
public class Application implements QuarkusApplication {

    /**
     * 应用入口 - 启动 Quarkus 应用。
     *
     * <h3>启动流程</h3>
     * <ol>
     *   <li>输出框架信息</li>
     *   <li>调用 Quarkus.run() 启动应用</li>
     *   <li>Quarkus 初始化 CDI 容器和所有扩展</li>
     *   <li>调用 run() 方法执行应用逻辑</li>
     * </ol>
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        System.out.println("\n=== AgentScope Quarkus Example ===\n");
        System.out.println("Framework: Quarkus (cloud-native Java)");
        System.out.println("Key advantage: Ultra-fast startup, GraalVM native support\n");

        // 启动 Quarkus 应用
        // Quarkus.run() 初始化 CDI 容器并调用 QuarkusApplication.run()
        Quarkus.run(Application.class, args);
    }

    /**
     * Quarkus 应用运行逻辑。
     *
     * <h3>实现说明</h3>
     * <p>实现 QuarkusApplication 接口允许自定义应用启动后的行为。
     * 本示例启动 HTTP 服务器并等待退出信号。
     *
     * <h3>端点信息</h3>
     * <ul>
     *   <li>POST /chat - 与 Agent 对话</li>
     *   <li>GET /health - 健康检查</li>
     *   <li>POST /chat/multi - 多轮对话</li>
     *   <li>GET /echo - 回显测试</li>
     * </ul>
     *
     * @param args 命令行参数
     * @return 退出码，0 表示正常退出
     * @throws Exception 如果应用运行中发生错误
     */
    @Override
    public int run(String... args) throws Exception {
        // 输出启动成功信息和可用端点
        System.out.println("Quarkus application started!");
        System.out.println("Access endpoints at: http://localhost:8080");
        System.out.println("  - POST /chat - Chat with the agent");
        System.out.println("  - GET  /health - Health check\n");

        // 等待退出信号（Ctrl+C 或 SIGTERM）
        // 这使应用保持运行状态，持续处理 HTTP 请求
        Quarkus.waitForExit();
        return 0;  // 返回 0 表示正常退出
    }
}