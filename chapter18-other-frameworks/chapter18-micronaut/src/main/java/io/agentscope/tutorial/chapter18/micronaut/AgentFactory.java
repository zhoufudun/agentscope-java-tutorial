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
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

/**
 * Micronaut 框架的 Agent 工厂类 - 演示使用工厂模式创建复杂 Bean。
 *
 * <h2>核心概念</h2>
 * <p>虽然 agentscope-micronaut-extension 已提供 ReActAgent 的自动配置，
 * 但本工厂类演示了如何通过工厂模式创建具有额外配置的自定义 Agent Bean。
 *
 * <h2>Micronaut 特性演示</h2>
 * <ul>
 *   <li>{@code @Factory} - 标记工厂类，用于创建复杂 Bean</li>
 *   <li>{@code @Singleton} - 单例作用域 Bean</li>
 *   <li>构造函数注入 - Micronaut 推荐的依赖注入方式</li>
 *   <li>方法注入初始化 - 类似 @PostConstruct 的初始化逻辑</li>
 * </ul>
 *
 * <h2>Micronaut 工厂模式优势</h2>
 * <ul>
 *   <li>编译时 DI - 无运行时反射开销，启动更快</li>
 *   <li>AOT（提前编译）处理 - 适合 GraalVM 原生镜像</li>
 *   <li>低内存占用 - 无需维护反射元数据</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 * @see io.micronaut.context.annotation.Factory
 * @see jakarta.inject.Singleton
 */
@Slf4j
@Factory  // Micronaut @Factory 注解标记此类为 Bean 工厂，可生产多个 Bean
public class AgentFactory {

    /**
     * 创建自定义配置的 ReActAgent Bean。
     *
     * <h3>工厂方法设计</h3>
     * <p>本方法演示了如何：
     * <ul>
     *   <li>注入配置属性 - 从 application.yml 读取配置</li>
     *   <li>创建自定义 Agent - 可添加额外的初始化逻辑</li>
     *   <li>包装或增强 Agent - 添加拦截器、日志、监控等</li>
     * </ul>
     *
     * <h3>实现说明</h3>
     * <p>本示例委托给自动配置的 Agent，因为 agentscope-micronaut-extension
     * 已处理了 Agent 创建。工厂模式在以下场景更有价值：
     * <ul>
     *   <li>需要自定义 Agent 配置时</li>
     *   <li>需要创建多个不同配置的 Agent 实例时</li>
     *   <li>需要添加初始化或清理逻辑时</li>
     * </ul>
     *
     * @param autoConfiguredAgent AgentScope 扩展自动配置的 Agent 实例
     * @return 可直接使用的 ReActAgent 实例
     */
    @Singleton  // @Singleton 确保整个应用只有一个实例
    public ReActAgent customAgent(ReActAgent autoConfiguredAgent) {
        // 记录 Agent 配置信息，便于调试和监控
        log.info("Creating custom agent configuration");
        log.info("Base agent name: {}", autoConfiguredAgent.getName());
        log.info("Base agent max iterations: {}", autoConfiguredAgent.getMaxIters());

        // 可以在此处自定义 Agent，例如：
        // - 包装 Agent 添加额外逻辑
        // - 添加拦截器用于日志、监控、限流等
        // - 配置自定义的消息处理器

        return autoConfiguredAgent;
    }

    /**
     * 创建封装 Agent 的聊天服务 Bean。
     *
     * <h3>服务层设计</h3>
     * <p>演示如何创建依赖 Agent 的服务层 Bean，提供更清晰的业务逻辑 API。
     * 这是典型的分层架构设计：
     * <ul>
     *   <li>Controller 层 - 处理 HTTP 请求</li>
     *   <li>Service 层 - 封装业务逻辑（本类）</li>
     *   <li>Agent 层 - AI 能力提供者</li>
     * </ul>
     *
     * @param agent 注入的 ReActAgent 实例
     * @return ChatService 聊天服务实例
     */
    @Singleton
    public ChatService chatService(ReActAgent agent) {
        return new ChatService(agent);
    }

    /**
     * 聊天服务包装类 - 对 ReActAgent 的业务层封装。
     *
     * <h3>设计目的</h3>
     * <p>提供更简洁的聊天接口，隐藏底层 Agent API 的复杂性：
     * <ul>
     *   <li>简化消息构建 - 不需要手动创建 Msg 对象</li>
     *   <li>统一响应处理 - 处理 null 响应等边界情况</li>
     *   <li>便于扩展 - 可添加日志、监控、缓存等横切关注点</li>
     * </ul>
     */
    public static class ChatService {
        /** 底层 ReActAgent 实例 */
        private final ReActAgent agent;

        /**
         * 构造聊天服务。
         *
         * @param agent ReActAgent 实例
         */
        public ChatService(ReActAgent agent) {
            this.agent = agent;
        }

        /**
         * 发送消息给 Agent 并返回响应文本。
         *
         * <h3>处理流程</h3>
         * <ol>
         *   <li>构建用户消息 - 使用 Msg.builder() 创建消息对象</li>
         *   <li>调用 Agent - 阻塞等待响应（适用于同步场景）</li>
         *   <li>提取响应文本 - 处理 null 响应的边界情况</li>
         * </ol>
         *
         * @param message 用户输入的消息文本
         * @return Agent 的响应文本，如无响应则返回 "No response"
         */
        public String chat(String message) {
            // 构建用户消息对象
            Msg userMsg = Msg.builder()
                    .role(MsgRole.USER)  // 设置消息角色为用户
                    .content(TextBlock.builder().text(message).build())  // 设置文本内容
                    .build();

            // 调用 Agent 并阻塞等待响应
            // block() 将响应式流转换为同步调用
            Msg response = agent.call(userMsg).block();

            // 安全地提取响应文本
            return response != null ? response.getTextContent() : "No response";
        }

        /**
         * 获取 Agent 名称。
         *
         * @return Agent 名称，用于标识和日志记录
         */
        public String getAgentName() {
            return agent.getName();
        }
    }
}