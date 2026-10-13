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
 * 第四章：ReAct Agent 详解 - Spring Boot 启动类.
 *
 * <p>本章节深入讲解 AgentScope Java 中最核心的 Agent 类型 —— ReActAgent。
 * ReAct（Reasoning + Acting）模式是一种强大的 Agent 设计范式，
 * 通过"思考-行动"循环实现复杂任务的自主完成。
 *
 * <h2>ReAct 模式核心概念</h2>
 * <ul>
 *   <li><b>Reasoning（推理）</b>：Agent 分析问题，决定下一步行动</li>
 *   <li><b>Acting（行动）</b>：Agent 调用工具执行具体操作</li>
 *   <li><b>Observation（观察）</b>：Agent 接收工具返回的结果</li>
 *   <li><b>Loop（循环）</b>：重复上述过程直到任务完成</li>
 * </ul>
 *
 * <h2>本章演示内容</h2>
 * <ul>
 *   <li>ReActAgent 的完整配置和使用</li>
 *   <li>自定义工具（Tool）的定义和注册</li>
 *   <li>Hook 生命周期钩子的实现</li>
 *   <li>流式响应处理</li>
 *   <li>Agent 状态管理（AgentState）</li>
 *   <li>中断和控制 Agent 执行</li>
 * </ul>
 *
 * <h2>包含的工具示例</h2>
 * <ul>
 *   <li>{@code WeatherTool} - 天气查询（当前天气、预报、生活指数）</li>
 *   <li>{@code CalculatorTool} - 数学计算和单位换算</li>
 *   <li>{@code DateTimeTool} - 日期时间查询和计算</li>
 * </ul>
 *
 * <h2>运行方式</h2>
 * <pre>{@code
 * # 设置 API Key
 * export DASHSCOPE_API_KEY=your-api-key
 *
 * # 运行应用
 * mvn spring-boot:run -pl chapter04-react-agent
 *
 * # 或者
 * java -jar target/chapter04-react-agent-1.0.0.jar
 * }</pre>
 *
 * <h2>API 端点</h2>
 * <ul>
 *   <li>POST /api/agent/chat - 发送消息（同步）</li>
 *   <li>POST /api/agent/chat/stream - 流式响应</li>
 *   <li>GET /api/agent/history - 获取对话历史</li>
 *   <li>DELETE /api/agent/history - 清除对话历史</li>
 *   <li>GET /api/agent/status - 获取 Agent 状态</li>
 *   <li>POST /api/agent/interrupt - 中断 Agent 执行</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 * @since 1.0
 * @see io.agentscope.core.ReActAgent
 * @see io.agentscope.core.tool.Tool
 * @see io.agentscope.core.hook.Hook
 */
@SpringBootApplication
@EnableAsync  // 启用异步支持，用于流式响应处理
public class Chapter04Application {

    /**
     * 应用启动入口.
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(Chapter04Application.class, args);
        // 打印启动信息
        System.out.println("===============================================");
        System.out.println("AgentScope Java 教程 - 第四章示例已启动");
        System.out.println("API 文档: http://localhost:8080/swagger-ui.html");
        System.out.println("===============================================");
    }
}