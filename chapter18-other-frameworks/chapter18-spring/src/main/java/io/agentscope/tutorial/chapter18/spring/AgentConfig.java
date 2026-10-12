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
package io.agentscope.tutorial.chapter18.spring;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Spring Boot Agent 配置组件 - 演示 Spring 依赖注入与 AgentScope 集成。
 *
 * <h2>概述</h2>
 * <p>本类演示 Spring Boot 的依赖注入能力与 AgentScope 的集成。
 * ReActAgent 由 agentscope-spring-boot-starter 自动配置，
 * 我们在此注入它以展示不同的注入模式。
 *
 * <h2>Spring Boot 集成要点</h2>
 * <ul>
 *   <li><b>@Component</b> - 标记为 Spring Bean，自动扫描注册</li>
 *   <li><b>构造函数注入</b> - 推荐的依赖注入方式</li>
 *   <li><b>@PostConstruct</b> - Bean 构造后的初始化逻辑</li>
 *   <li><b>ApplicationContext</b> - 获取 Spring 上下文进行高级操作</li>
 * </ul>
 *
 * <h2>Spring Boot vs 其他框架</h2>
 * <table border="1">
 *   <tr><th>特性</th><th>Spring Boot</th><th>Quarkus</th><th>Micronaut</th></tr>
 *   <tr><td>DI 时机</td><td>运行时</td><td>构建时</td><td>编译时</td></tr>
 *   <tr><td>生态系统</td><td>最丰富</td><td>丰富</td><td>中等</td></tr>
 *   <tr><td>学习曲线</td><td>平缓</td><td>较陡</td><td>较陡</td></tr>
 *   <tr><td>企业采用</td><td>最广泛</td><td>增长中</td><td>小众</td></tr>
 * </table>
 *
 * @author AgentScope Tutorial
 * @see org.springframework.stereotype.Component
 * @see jakarta.annotation.PostConstruct
 */
@Component  // Spring 组件注解，标记为 Spring 管理的 Bean
public class AgentConfig {

    private static final Logger log = LoggerFactory.getLogger(AgentConfig.class);

    /** 自动配置的 ReActAgent 实例 */
    private final ReActAgent agent;

    /** Spring ApplicationContext，用于高级用例 */
    private final org.springframework.context.ApplicationContext applicationContext;

    /**
     * 构造 AgentConfig，注入自动配置的 ReActAgent 和 ApplicationContext。
     *
     * <h3>构造函数注入优势</h3>
     * <p>相比字段注入（@Autowired 字段），构造函数注入有以下优势：
     * <ul>
     *   <li><b>依赖明确</b> - 所有依赖在构造函数参数中可见</li>
     *   <li><b>便于测试</b> - 可通过构造函数传入 Mock 对象</li>
     *   <li><b>不可变性</b> - 依赖设为 final，防止运行时修改</li>
     *   <li><b>避免反射</b> - 不依赖反射注入字段</li>
     * </ul>
     *
     * @param agent   agentscope-spring-boot-starter 自动配置的 ReActAgent
     * @param context Spring ApplicationContext，用于获取其他 Bean 或环境信息
     */
    public AgentConfig(ReActAgent agent, org.springframework.context.ApplicationContext context) {
        this.agent = agent;
        this.applicationContext = context;
    }

    /**
     * Bean 构造后初始化 - 在所有依赖注入完成后执行。
     *
     * <h3>@PostConstruct 生命周期</h3>
     * <ol>
     *   <li>Bean 实例化（构造函数调用）</li>
     *   <li>依赖注入完成</li>
     *   <li>@PostConstruct 方法调用</li>
     *   <li>Bean 可用于其他组件</li>
     * </ol>
     *
     * <h3>适用场景</h3>
     * <ul>
     *   <li>日志输出初始化信息</li>
     *   <li>验证配置正确性</li>
     *   <li>预热缓存或连接池</li>
     *   <li>注册回调或监听器</li>
     * </ul>
     */
    @PostConstruct  // Bean 初始化后回调
    public void init() {
        // 输出 Agent 初始化信息，便于调试和验证配置
        log.info("=== AgentScope Spring Boot Integration ===");
        log.info("Agent initialized: {}", agent.getName());
        log.info("Agent max iterations: {}", agent.getMaxIters());
    }

    /**
     * 获取配置的 Agent 实例。
     *
     * @return ReActAgent 实例
     */
    public ReActAgent getAgent() {
        return agent;
    }

    /**
     * 演示通过 Spring 管理的 Bean 调用 Agent。
     *
     * <h3>处理流程</h3>
     * <ol>
     *   <li>记录用户消息（日志审计）</li>
     *   <li>构建 Msg 对象 - 使用 List.of() 创建消息列表</li>
     *   <li>调用 Agent - 阻塞等待响应</li>
     *   <li>记录 Agent 响应</li>
     * </ol>
     *
     * @param userMessage 用户输入的消息文本
     * @return Agent 的响应 Msg 对象
     */
    public Msg chat(String userMessage) {
        // 记录用户消息
        log.info("User: {}", userMessage);

        // 构建用户消息对象
        Msg userMsg = Msg.builder()
                .role(MsgRole.USER)  // 消息角色：用户
                .content(TextBlock.builder().text(userMessage).build())  // 消息内容
                .build();

        // 调用 Agent 并阻塞等待响应
        // 注意：这里使用 List.of(userMsg) 传递消息列表
        Msg response = agent.call(List.of(userMsg)).block();

        // 记录响应
        log.info("Agent: {}", response != null ? response.getTextContent() : "null");

        return response;
    }

    /**
     * 演示批量处理多个消息。
     *
     * <h3>使用场景</h3>
     * <ul>
     *   <li>批量处理用户查询</li>
     *   <li>自动化测试场景</li>
     *   <li>数据处理管道</li>
     * </ul>
     *
     * @param messages 用户消息数组
     * @return Agent 响应数组
     */
    public Msg[] batchChat(String[] messages) {
        Msg[] responses = new Msg[messages.length];
        // 逐个处理消息
        for (int i = 0; i < messages.length; i++) {
            responses[i] = chat(messages[i]);
        }
        return responses;
    }
}