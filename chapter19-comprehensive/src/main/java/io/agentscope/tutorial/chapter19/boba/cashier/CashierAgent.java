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
package io.agentscope.tutorial.chapter19.boba.cashier;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * 奶茶店收银员 Agent
 *
 * <p>负责：
 * <ul>
 *   <li>接收顾客点单</li>
 *   <li>确认饮品规格（甜度、冰度、加料）</li>
 *   <li>报价并处理付款</li>
 *   <li>将订单传递给制茶师</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 * @since 1.0
 */
@Component
public class CashierAgent {

    private static final Logger logger = LoggerFactory.getLogger(CashierAgent.class);

    private final ReActAgent cashierAgent;

    // 饮品菜单价格
    private static final String MENU = """
            饮品菜单：
            - 珍珠奶茶: 18元
            - 椰果奶茶: 18元
            - 波霸奶茶: 20元
            - 芋泥波波: 22元
            - 芝芝莓莓: 28元

            加料：
            - 珍珠: +3元
            - 椰果: +2元
            - 波霸: +3元
            - 芋圆: +3元
            """;

    public CashierAgent(
            @Qualifier("bobaCashierAgent") ReActAgent cashierAgent) {
        this.cashierAgent = cashierAgent;
        logger.info("CashierAgent 初始化完成");
    }

    /**
     * 接收订单并确认
     *
     * @param orderRequest 订单请求
     * @return 确认的订单信息
     */
    public String receiveOrder(String orderRequest) {
        logger.info("接收订单: {}", orderRequest);

        String prompt = String.format("""
                有一位顾客要点单，请处理：

                顾客需求：%s

                %s

                请：
                1. 确认饮品类型和数量
                2. 询问甜度和冰度（如果不明确）
                3. 询问是否需要加料
                4. 报出总价
                5. 确认付款方式
                """, orderRequest, MENU);

        Msg msg = Msg.builder()
                .name("customer")
                .role(MsgRole.USER)
                .content(TextBlock.builder().text(prompt).build())
                .build();

        try {
            Msg response = cashierAgent.call(msg).block();
            return extractText(response);
        } catch (Exception e) {
            logger.error("接收订单失败", e);
            return "接收订单失败：" + e.getMessage();
        }
    }

    /**
     * 处理付款
     *
     * @param amount 金额
     * @param paymentMethod 付款方式
     * @return 付款结果
     */
    public String processPayment(double amount, String paymentMethod) {
        logger.info("处理付款: {} via {}", amount, paymentMethod);

        String prompt = String.format("""
                请处理以下付款：

                金额：%.2f 元
                付款方式：%s

                请确认付款成功并提供找零信息（如果有）。
                """, amount, paymentMethod);

        Msg msg = Msg.builder()
                .name("customer")
                .role(MsgRole.USER)
                .content(TextBlock.builder().text(prompt).build())
                .build();

        try {
            Msg response = cashierAgent.call(msg).block();
            return extractText(response);
        } catch (Exception e) {
            logger.error("处理付款失败", e);
            return "付款处理失败：" + e.getMessage();
        }
    }

    /**
     * 生成订单号
     *
     * @param drinkName 饮品名称
     * @return 订单号
     */
    public String generateOrderId(String drinkName) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String orderId = "BOBA" + timestamp.substring(timestamp.length() - 8);
        logger.info("生成订单号: {} for {}", orderId, drinkName);
        return orderId;
    }

    /**
     * 获取菜单信息
     */
    public String getMenu() {
        return MENU;
    }

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
}