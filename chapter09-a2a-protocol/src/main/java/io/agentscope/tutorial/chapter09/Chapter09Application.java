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

package io.agentscope.tutorial.chapter09;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 第九章：A2A（Agent to Agent）协议 - Spring Boot 启动类
 *
 * <p>本应用演示 AgentScope Java 中 A2A 通信协议的实现。
 * A2A 协议是一种标准化的 Agent 间通信协议，允许不同系统中的 Agent 相互发现和调用。
 *
 * <h2>本章演示内容</h2>
 * <ul>
 *   <li><b>A2A Server 配置</b>：如何配置和部署 A2A 服务端</li>
 *   <li><b>AgentCard</b>：用于服务发现的元数据文档</li>
 *   <li><b>A2A Client</b>：如何创建客户端调用远程 Agent</li>
 *   <li><b>流式响应</b>：支持实时流式输出</li>
 *   <li><b>任务状态同步</b>：跨 Agent 的任务状态管理</li>
 * </ul>
 *
 * <h2>AgentScope 学习要点</h2>
 * <ul>
 *   <li><b>AgentCard</b>：A2A 协议的核心，描述 Agent 的能力、端点和元数据</li>
 *   <li><b>JSON-RPC</b>：A2A 默认的传输协议</li>
 *   <li><b>Well-Known URI</b>：标准的服务发现路径 {@code /.well-known/agent-card.json}</li>
 *   <li><b>技能（Skills）</b>：标准化的 Agent 能力描述</li>
 * </ul>
 *
 * <h2>使用方式</h2>
 * <ol>
 *   <li>启动应用：{@code mvn spring-boot:run}</li>
 *   <li>访问 AgentCard：{@code http://localhost:8888/.well-known/agent-card.json}</li>
 *   <li>通过 A2A 协议端点发送请求</li>
 *   <li>在控制台查看流式响应</li>
 * </ol>
 *
 * <h2>关键端点</h2>
 * <ul>
 *   <li>{@code GET /.well-known/agent-card.json} - AgentCard 服务发现端点</li>
 *   <li>{@code POST /} - A2A JSON-RPC 端点</li>
 *   <li>{@code GET /api/chat} - REST API 聊天接口</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 */
@SpringBootApplication
public class Chapter09Application {

    public static void main(String[] args) {
        SpringApplication.run(Chapter09Application.class, args);
    }
}