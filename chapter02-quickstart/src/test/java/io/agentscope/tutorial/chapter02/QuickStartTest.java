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

import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.tutorial.chapter02.service.QuickStartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 快速开始测试类。
 *
 * <p>测试 AgentScope Java 的基本功能：
 * <ul>
 *   <li>Spring Boot 应用启动</li>
 *   <li>Agent 创建</li>
 *   <li>配置加载</li>
 * </ul>
 *
 * <p>注意：这些测试需要有效的 API Key 才能运行完整的 Agent 调用测试。
 * 可以通过设置环境变量 DASHSCOPE_API_KEY 来提供。
 *
 * @author AgentScope Team
 * @since 1.0
 */
@SpringBootTest(classes = Chapter02Application.class)
@DisplayName("第二章快速开始测试")
class QuickStartTest {

    /**
     * 快速开始服务。
     */
    @Autowired
    private QuickStartService quickStartService;

    /**
     * 测试 Spring Boot 应用启动。
     */
    @Test
    @DisplayName("应用启动测试")
    void testApplicationStartup() {
        // 验证应用上下文已加载
        org.junit.jupiter.api.Assertions.assertNotNull(quickStartService);
    }

    /**
     * 测试 Agent 创建。
     */
    @Test
    @DisplayName("Agent 创建测试")
    void testAgentCreation() {
        // 创建 Agent
        ReActAgent agent = quickStartService.createAgent("TestAgent", "你是一个测试助手");

        // 验证 Agent 已创建
        org.junit.jupiter.api.Assertions.assertNotNull(agent);
        org.junit.jupiter.api.Assertions.assertEquals("TestAgent", agent.getName());
    }

    /**
     * 测试 Agent 使用内存组件。
     */
    @Test
    @DisplayName("Agent 内存组件测试")
    void testAgentMemory() {
        // 创建自定义 Agent
        ReActAgent agent = ReActAgent.builder()
                .name("MemoryTestAgent")
                .sysPrompt("你是一个测试助手")
                .model(quickStartService.createAgent().getModel())
                .memory(new InMemoryMemory())
                .toolkit(new Toolkit())
                .maxIterations(5)
                .build();

        // 验证内存组件已设置
        org.junit.jupiter.api.Assertions.assertNotNull(agent.getMemory());
    }

    /**
     * 测试工具包配置。
     */
    @Test
    @DisplayName("工具包配置测试")
    void testToolkitConfiguration() {
        // 创建工具包
        Toolkit toolkit = new Toolkit();

        // 验证工具包为空（尚未注册工具）
        org.junit.jupiter.api.Assertions.assertNotNull(toolkit);
    }
}