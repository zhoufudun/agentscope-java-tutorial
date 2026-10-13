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
package io.agentscope.tutorial.chapter19.boba.supervisor;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.Event;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.UUID;

/**
 * 奶茶店订单监督者 Agent - Supervisor 模式的典型实现
 *
 * <p>核心调度节点，负责：
 * <ul>
 *   <li>接收用户订单请求</li>
 *   <li>协调收银员和制茶师完成订单处理</li>
 *   <li>聚合子 Agent 返回的结果</li>
 *   <li>将最终结果返回给用户</li>
 * </ul>
 *
 * <h2>Supervisor 模式详解</h2>
 * <p>Supervisor（监督者）模式是多 Agent 系统的常见架构模式：
 * <pre>
 *                    ┌─────────────────┐
 *                    │   用户请求      │
 *                    └────────┬────────┘
 *                             ▼
 *                    ┌─────────────────┐
 *                    │  Supervisor     │  ← 理解意图，分发任务
 *                    │  (监督者 Agent) │
 *                    └────────┬────────┘
 *                             │
 *           ┌─────────────────┼─────────────────┐
 *           ▼                 ▼                 ▼
 *   ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
 *   │ 收银员 Agent │  │ 制茶师 Agent │  │ 其他 Agent   │
 *   └──────────────┘  └──────────────┘  └──────────────┘
 *           │                 │                 │
 *           └─────────────────┼─────────────────┘
 *                             ▼
 *                    ┌─────────────────┐
 *                    │  聚合结果       │
 *                    │  返回给用户     │
 *                    └─────────────────┘
 * </pre>
 *
 * <h2>与其他模式的对比</h2>
 * <ul>
 *   <li><b>Supervisor vs Pipeline</b>：Pipeline 是串行执行，Supervisor 可以并行或动态调度</li>
 *   <li><b>Supervisor vs Peer-to-Peer</b>：Supervisor 有中央节点，P2P 是去中心化</li>
 *   <li><b>Supervisor vs Hierarchical</b>：Hierarchical 有多层级，Supervisor 通常是两层</li>
 * </ul>
 *
 * <h2>适用场景</h2>
 * <ul>
 *   <li>客服系统：分发到不同业务线</li>
 *   <li>订单处理：协调支付、物流、库存</li>
 *   <li>智能办公：日程、邮件、报表多能力协作</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 * @since 1.0
 */
@Component
public class OrderSupervisorAgent {

    private static final Logger logger = LoggerFactory.getLogger(OrderSupervisorAgent.class);

    private final ReActAgent supervisorAgent;
    private final ReActAgent cashierAgent;
    private final ReActAgent baristaAgent;

    /**
     * 构造函数注入所有 Agent
     */
    @Autowired
    public OrderSupervisorAgent(
            @Qualifier("bobaSupervisorAgent") ReActAgent supervisorAgent,
            @Qualifier("bobaCashierAgent") ReActAgent cashierAgent,
            @Qualifier("bobaBaristaAgent") ReActAgent baristaAgent) {
        this.supervisorAgent = supervisorAgent;
        this.cashierAgent = cashierAgent;
        this.baristaAgent = baristaAgent;
        logger.info("OrderSupervisorAgent 初始化完成");
    }

    /**
     * 处理奶茶订单 - Supervisor 模式的核心方法
     *
     * <p><b>执行流程：</b>
     * <ol>
     *   <li>接收用户自然语言订单（如"我要一杯珍珠奶茶，少糖少冰"）</li>
     *   <li>Supervisor Agent 理解意图，调用收银员确认订单</li>
     *   <li>确认后调用制茶师制作饮品</li>
     *   <li>聚合结果，返回完整的订单处理信息</li>
     * </ol>
     *
     * <p><b>关键技术点：</b>
     * <ul>
     *   <li>使用 {@code block()} 实现同步调用（适合简单场景）</li>
     *   <li>UUID 用于会话追踪和日志关联</li>
     *   <li>异常处理保证用户体验</li>
     * </ul>
     *
     * @param userMessage 用户的消息（如"我要一杯珍珠奶茶，少糖少冰"）
     * @return 订单处理结果
     */
    public String processOrder(String userMessage) {
        logger.info("接收到订单请求: {}", userMessage);
        String sessionId = UUID.randomUUID().toString();

        try {
            // 构建用户消息
            Msg userMsg = Msg.builder()
                    .name("customer")
                    .role(MsgRole.USER)
                    .content(TextBlock.builder().text(userMessage).build())
                    .build();

            // 调用监督者 Agent 处理
            Msg response = supervisorAgent.call(userMsg).block();

            // 提取响应文本
            String result = extractText(response);
            logger.info("订单处理完成: {}", result);

            return result;

        } catch (Exception e) {
            logger.error("订单处理失败", e);
            return "抱歉，订单处理遇到问题：" + e.getMessage();
        }
    }

    /**
     * 流式处理订单 - 支持实时响应的处理方式
     *
     * <p><b>流式处理的优势：</b>
     * <ul>
     *   <li>用户可以实时看到处理进度</li>
     *   <li>支持长时间任务的中间反馈</li>
     *   <li>更好的用户体验</li>
     * </ul>
     *
     * <p><b>技术实现：</b>
     * <ul>
     *   <li>返回 {@code Flux<Event>} 响应式流</li>
     *   <li>可以与 SSE（Server-Sent Events）配合使用</li>
     *   <li>支持背压（backpressure）控制</li>
     * </ul>
     *
     * @param userMessage 用户的消息
     * @return 事件流，包含处理过程中的各种事件
     */
    public Flux<Event> processOrderStream(String userMessage) {
        logger.info("流式处理订单请求: {}", userMessage);

        Msg userMsg = Msg.builder()
                .name("customer")
                .role(MsgRole.USER)
                .content(TextBlock.builder().text(userMessage).build())
                .build();

        return supervisorAgent.stream(userMsg);
    }

    /**
     * 获取收银员 Agent 的回复（用于模拟协作）
     */
    public String consultCashier(String message) {
        Msg msg = Msg.builder()
                .name("supervisor")
                .role(MsgRole.USER)
                .content(TextBlock.builder().text(message).build())
                .build();

        try {
            Msg response = cashierAgent.call(msg).block();
            return extractText(response);
        } catch (Exception e) {
            logger.error("咨询收银员失败", e);
            return "收银员暂时无法响应";
        }
    }

    /**
     * 获取制茶师 Agent 的回复（用于模拟协作）
     */
    public String consultBarista(String message) {
        Msg msg = Msg.builder()
                .name("supervisor")
                .role(MsgRole.USER)
                .content(TextBlock.builder().text(message).build())
                .build();

        try {
            Msg response = baristaAgent.call(msg).block();
            return extractText(response);
        } catch (Exception e) {
            logger.error("咨询制茶师失败", e);
            return "制茶师暂时无法响应";
        }
    }

    /**
     * 提取消息中的文本内容
     */
    private String extractText(Msg msg) {
        if (msg == null) {
            return "无响应";
        }
        return msg.getContent().stream()
                .filter(block -> block instanceof TextBlock)
                .map(block -> ((TextBlock) block).getText())
                .findFirst()
                .orElse("无法解析响应");
    }

    /**
     * 获取监督者 Agent 状态信息
     */
    public String getStatus() {
        return String.format(
                "奶茶店多代理系统状态：\n" +
                "- 监督者: %s\n" +
                "- 收银员: %s\n" +
                "- 制茶师: %s",
                supervisorAgent.getName(),
                cashierAgent.getName(),
                baristaAgent.getName()
        );
    }
}