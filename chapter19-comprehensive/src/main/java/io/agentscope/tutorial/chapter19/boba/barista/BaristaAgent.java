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
package io.agentscope.tutorial.chapter19.boba.barista;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * 奶茶店制茶师 Agent
 *
 * <p>负责根据订单制作饮品，包括：
 * <ul>
 *   <li>准备原料和设备</li>
 *   <li>按配方制作饮品</li>
 *   <li>添加顾客要求的加料</li>
 *   <li>封口贴标签</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 * @since 1.0
 */
@Component
public class BaristaAgent {

    private static final Logger logger = LoggerFactory.getLogger(BaristaAgent.class);

    private final ReActAgent baristaAgent;

    public BaristaAgent(
            @Qualifier("bobaBaristaAgent") ReActAgent baristaAgent) {
        this.baristaAgent = baristaAgent;
        logger.info("BaristaAgent 初始化完成");
    }

    /**
     * 开始制作饮品
     *
     * @param orderDetails 订单详情
     * @return 制作结果
     */
    public String makeDrink(String orderDetails) {
        logger.info("开始制作饮品: {}", orderDetails);

        String prompt = String.format("""
                请根据以下订单制作饮品：

                订单详情：%s

                请按照标准流程制作：
                1. 确认订单内容
                2. 准备原料和设备
                3. 按配方制作
                4. 添加加料
                5. 封口贴标签

                完成后请告知饮品已准备好。
                """, orderDetails);

        Msg msg = Msg.builder()
                .name("cashier")
                .role(MsgRole.USER)
                .content(TextBlock.builder().text(prompt).build())
                .build();

        try {
            Msg response = baristaAgent.call(msg).block();
            return extractText(response);
        } catch (Exception e) {
            logger.error("制作饮品失败", e);
            return "制作饮品失败：" + e.getMessage();
        }
    }

    /**
     * 确认饮品制作完成
     *
     * @param drinkName 饮品名称
     * @return 完成消息
     */
    public String confirmReady(String drinkName) {
        logger.info("确认饮品完成: {}", drinkName);
        return String.format("%s 已制作完成，请顾客取餐。", drinkName);
    }

    /**
     * 检查库存状态
     *
     * @param ingredient 原料名称
     * @return 库存状态
     */
    public String checkStock(String ingredient) {
        // 模拟库存检查
        return String.format("原料 '%s' 库存充足，可以正常使用。", ingredient);
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