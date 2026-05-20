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
package io.agentscope.tutorial.chapter19.boba.service;

import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import io.agentscope.tutorial.chapter19.boba.barista.BaristaAgent;
import io.agentscope.tutorial.chapter19.boba.cashier.CashierAgent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 奶茶店订单服务
 *
 * <p>提供订单处理的核心服务，包括：
 * <ul>
 *   <li>处理付款</li>
 *   <li>制作饮品</li>
 *   <li>检查库存</li>
 *   <li>订单状态查询</li>
 * </ul>
 *
 * <p>实现为 Tool 接口，供 Agent 调用。
 *
 * @author AgentScope Tutorial
 * @since 1.0
 */
@Service
public class BobaOrderService {

    private final CashierAgent cashierAgent;
    private final BaristaAgent baristaAgent;

    // 库存状态（模拟）
    private final Map<String, Integer> stock = Map.of(
            "珍珠", 100,
            "椰果", 80,
            "波霸", 90,
            "芋圆", 70,
            "茶底", 200,
            "奶盖", 150
    );

    // 价格表
    private static final Map<String, Double> PRICES = Map.of(
            "珍珠奶茶", 18.0,
            "椰果奶茶", 18.0,
            "波霸奶茶", 20.0,
            "芋泥波波", 22.0,
            "芝芝莓莓", 28.0
    );

    private static final Map<String, Double> TOPPING_PRICES = Map.of(
            "珍珠", 3.0,
            "椰果", 2.0,
            "波霸", 3.0,
            "芋圆", 3.0
    );

    @Autowired
    public BobaOrderService(CashierAgent cashierAgent, BaristaAgent baristaAgent) {
        this.cashierAgent = cashierAgent;
        this.baristaAgent = baristaAgent;
    }

    /**
     * 处理付款工具方法
     */
    @Tool(name = "process_payment", description = "处理顾客付款，确认订单完成")
    public ToolResultBlock processPayment(
            @ToolParam(name = "amount", description = "付款金额") double amount,
            @ToolParam(name = "payment_method", description = "付款方式（wechat/alipay/cash）") String paymentMethod) {
        try {
            String result = cashierAgent.processPayment(amount, paymentMethod);
            if (result.contains("成功") || result.contains("完成")) {
                return ToolResultBlock.text(result);
            } else {
                return ToolResultBlock.error("付款处理失败：" + result);
            }
        } catch (Exception e) {
            return ToolResultBlock.error("付款处理异常：" + e.getMessage());
        }
    }

    /**
     * 制作饮品工具方法
     */
    @Tool(name = "make_drink", description = "根据订单制作饮品")
    public ToolResultBlock makeDrink(
            @ToolParam(name = "order_details", description = "订单详情，包含饮品名称、甜度、冰度、加料等") String orderDetails) {
        try {
            String result = baristaAgent.makeDrink(orderDetails);
            if (result.contains("完成") || result.contains("好")) {
                return ToolResultBlock.text(result);
            } else {
                return ToolResultBlock.text("饮品制作中: " + result);
            }
        } catch (Exception e) {
            return ToolResultBlock.error("制作饮品异常：" + e.getMessage());
        }
    }

    /**
     * 检查库存工具方法
     */
    @Tool(name = "check_stock", description = "检查原料库存")
    public ToolResultBlock checkStock(
            @ToolParam(name = "ingredient", description = "原料名称（珍珠/椰果/波霸/芋圆/茶底/奶盖）") String ingredient) {
        Integer quantity = stock.get(ingredient);
        if (quantity != null) {
            if (quantity > 20) {
                return ToolResultBlock.text(String.format("原料 '%s' 库存：%d 单位，库存充足", ingredient, quantity));
            } else if (quantity > 0) {
                return ToolResultBlock.text(String.format("原料 '%s' 库存：%d 单位，库存不足，建议补货", ingredient, quantity));
            } else {
                return ToolResultBlock.text(String.format("原料 '%s' 已缺货，请立即补货", ingredient));
            }
        } else {
            return ToolResultBlock.error(String.format("原料 '%s' 不在库存列表中", ingredient));
        }
    }

    /**
     * 生成订单号工具方法
     */
    @Tool(name = "generate_order_id", description = "生成唯一订单号")
    public ToolResultBlock generateOrderId(
            @ToolParam(name = "drink_name", description = "饮品名称") String drinkName) {
        try {
            String orderId = cashierAgent.generateOrderId(drinkName);
            return ToolResultBlock.text(String.format("订单号：%s，饮品：%s", orderId, drinkName));
        } catch (Exception e) {
            return ToolResultBlock.error("生成订单号失败：" + e.getMessage());
        }
    }

    /**
     * 计算订单价格
     *
     * @param drinkName 饮品名称
     * @param toppings 加料列表
     * @return 总价
     */
    public double calculatePrice(String drinkName, List<String> toppings) {
        double price = PRICES.getOrDefault(drinkName, 0.0);
        if (toppings != null) {
            for (String topping : toppings) {
                price += TOPPING_PRICES.getOrDefault(topping, 0.0);
            }
        }
        return price;
    }

    /**
     * 获取菜单信息
     */
    public String getMenu() {
        StringBuilder menu = new StringBuilder();
        menu.append("===== 奶茶店菜单 =====\n\n");
        menu.append("【饮品】\n");
        PRICES.forEach((name, price) ->
                menu.append(String.format("  %s: %.0f元\n", name, price)));
        menu.append("\n【加料】\n");
        TOPPING_PRICES.forEach((name, price) ->
                menu.append(String.format("  %s: +%.0f元\n", name, price)));
        menu.append("\n【规格】\n");
        menu.append("甜度：正常糖、少糖、半糖、微糖、无糖\n");
        menu.append("冰度：正常冰、少冰、微冰、去冰、常温\n");
        return menu.toString();
    }
}