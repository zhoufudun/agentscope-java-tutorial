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
package io.agentscope.tutorial.chapter03;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 第三章：消息与会话管理 - Spring Boot 启动类.
 *
 * <p>本章节是 AgentScope Java 教程的核心章节之一，深入讲解消息（Msg）与会话（Session）
 * 的概念及其在实际应用中的使用方式。
 *
 * <h2>核心概念</h2>
 * <ul>
 *   <li><b>Msg（消息）</b>：AgentScope 中信息传递的基本单元，包含角色、内容、元数据等</li>
 *   <li><b>Session（会话）</b>：管理多轮对话的上下文，保持对话连贯性</li>
 *   <li><b>ContentBlock</b>：消息内容的结构化表示，支持文本、图片、工具调用等多种类型</li>
 *   <li><b>AgentStateStore</b>：2.x 新增的会话状态持久化接口，替代旧版 Session 类</li>
 * </ul>
 *
 * <h2>本章演示内容</h2>
 * <ul>
 *   <li>多轮对话的上下文管理</li>
 *   <li>H2 数据库实现会话持久化（JPA 方式）</li>
 *   <li>AgentStateStore 实现会话状态存储（AgentScope 原生方式）</li>
 *   <li>自定义消息格式器（JsonLogFormatter）</li>
 *   <li>消息实体与 AgentScope Msg 的双向转换</li>
 * </ul>
 *
 * <h2>运行方式</h2>
 * <pre>{@code
 * # 设置 API Key
 * export DASHSCOPE_API_KEY=your-api-key
 *
 * # 运行应用
 * mvn spring-boot:run -pl chapter03-message-session
 *
 * # 或者
 * java -jar target/chapter03-message-session-1.0.0.jar
 * }</pre>
 *
 * <h2>API 端点</h2>
 * <ul>
 *   <li>POST /api/chat - 发送消息</li>
 *   <li>POST /api/sessions - 创建会话</li>
 *   <li>GET /api/sessions - 获取会话列表</li>
 *   <li>GET /api/sessions/{id}/history - 获取会话历史</li>
 *   <li>DELETE /api/sessions/{id} - 删除会话</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 * @since 1.0
 * @see io.agentscope.core.message.Msg
 * @see io.agentscope.core.state.AgentStateStore
 */
@SpringBootApplication
public class Chapter03Application {

    /**
     * 应用启动入口.
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(Chapter03Application.class, args);
    }
}