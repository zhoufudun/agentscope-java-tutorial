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

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * Quarkus 框架的 Agent Bean - 演示 CDI (Contexts and Dependency Injection) 集成。
 *
 * <h2>概述</h2>
 * <p>本类演示如何在 Quarkus 框架中使用 CDI 管理 AgentScope Agent。
 * ReActAgent 由 agentscope-quarkus-extension 自动配置，本 Bean 对其进行业务层封装。
 *
 * <h2>Quarkus CDI (ArC) 特性演示</h2>
 * <ul>
 *   <li><b>@ApplicationScoped</b> - 应用级单例作用域，整个应用生命周期只有一个实例</li>
 *   <li><b>@Inject</b> - CDI 依赖注入注解</li>
 *   <li><b>构建时 Bean 处理</b> - ArC 在构建时分析和优化 Bean，无运行时反射</li>
 *   <li><b>构造函数注入</b> - 推荐的依赖注入方式</li>
 * </ul>
 *
 * <h2>Quarkus ArC vs 传统 CDI</h2>
 * <table border="1">
 *   <tr><th>特性</th><th>Quarkus ArC</th><th>传统 CDI (Weld)</th></tr>
 *   <tr><td>Bean 发现</td><td>构建时</td><td>运行时</td></tr>
 *   <tr><td>代理生成</td><td>构建时</td><td>运行时</td></tr>
 *   <tr><td>启动时间</td><td>毫秒级</td><td>秒级</td></tr>
 *   <tr><td>原生镜像</td><td>直接支持</td><td>需额外配置</td></tr>
 * </table>
 *
 * @author AgentScope Tutorial
 * @see jakarta.enterprise.context.ApplicationScoped
 * @see jakarta.inject.Inject
 */
@Slf4j
@ApplicationScoped  // CDI 应用作用域：整个应用只有一个实例，类似 Spring 的 @Singleton
public class AgentBean {

    /** 自动配置的 ReActAgent 实例 */
    private final ReActAgent agent;

    /**
     * 构造 AgentBean，注入自动配置的 ReActAgent。
     *
     * <h3>Quarkus ArC 构建时优化</h3>
     * <p>Quarkus ArC 在构建时执行依赖分析，相比传统 CDI 的运行时反射：
     * <ul>
     *   <li>消除反射开销 - 依赖关系在编译时确定</li>
     *   <li>更快启动 - 无需运行时扫描类路径</li>
     *   <li>更小内存 - 不需要保存反射元数据</li>
     *   <li>原生友好 - 直接支持 GraalVM 原生镜像编译</li>
     * </ul>
     *
     * @param agent agentscope-quarkus-extension 自动配置的 ReActAgent
     */
    @Inject  // CDI 注入注解，Quarkus ArC 在构建时处理
    public AgentBean(ReActAgent agent) {
        this.agent = agent;
        // 记录 Agent 初始化信息
        log.info("=== AgentScope Quarkus Integration ===");
        log.info("Agent initialized: {}", agent.getName());
        log.info("Agent max iterations: {}", agent.getMaxIters());
    }

    /**
     * 获取 Agent 实例。
     *
     * @return ReActAgent 实例
     */
    public ReActAgent getAgent() {
        return agent;
    }

    /**
     * 发送聊天消息给 Agent 并返回响应文本。
     *
     * <h3>处理流程</h3>
     * <ol>
     *   <li>记录用户消息（用于调试和审计）</li>
     *   <li>构建 Msg 对象 - 使用 AgentScope 消息构建器</li>
     *   <li>调用 Agent - 阻塞等待响应</li>
     *   <li>提取并记录响应文本</li>
     * </ol>
     *
     * @param userMessage 用户输入的消息文本
     * @return Agent 的响应文本，如无响应则返回 "No response"
     */
    public String chat(String userMessage) {
        // 记录用户消息
        log.info("User: {}", userMessage);

        // 构建用户消息对象
        Msg userMsg = Msg.builder()
                .role(MsgRole.USER)  // 消息角色：用户
                .content(TextBlock.builder().text(userMessage).build())  // 消息内容
                .build();

        // 调用 Agent 并阻塞等待响应
        // block() 将响应式 Mono 转换为同步调用
        Msg response = agent.call(userMsg).block();

        // 安全提取响应文本
        String responseText = response != null ? response.getTextContent() : "No response";

        // 记录 Agent 响应
        log.info("Agent: {}", responseText);
        return responseText;
    }

    /**
     * 获取 Agent 状态描述。
     *
     * <h3>用途</h3>
     * <ul>
     *   <li>健康检查端点的状态信息</li>
     *   <li>监控系统的状态采集</li>
     *   <li>日志和调试输出</li>
     * </ul>
     *
     * @return Agent 状态描述字符串
     */
    public String getStatus() {
        return "AgentScope Agent '" + agent.getName() + "' is ready";
    }
}