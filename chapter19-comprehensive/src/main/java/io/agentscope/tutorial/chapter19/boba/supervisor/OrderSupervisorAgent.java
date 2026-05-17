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
import org.springframework.beans.factory.annotation.自动注入;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.UUID;

/**
 * 奶茶店订单监督者 Agent
 *
 * <p>核心调度节点，负责：
 * <ul>
 *   <li>接收用户订单请求</li>
 *   <li>协调收银员和制茶师完成订单处理</li>
 *   <li>聚合子 Agent 返回的结果</li>
 *   <li>将最终结果返回给用户</li>
 * </ul>
 *
 * <p>这是一个 Supervisor Agent 模式的应用示例，
 * 通过监督者协调多个专业子 Agent 完成复杂任务。
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
     * 处理奶茶订单
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
     * 流式处理订单
     *
     * @param userMessage 用户的消息
     * @return 事件流
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