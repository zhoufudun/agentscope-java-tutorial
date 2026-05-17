package io.agentscope.tutorial.chapter08.tool;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 订单查询工具（模拟实现）
 *
 * 在实际生产环境中，此工具会调用真实的订单服务 API。
 * 这里使用模拟数据演示工具调用的工作原理。
 */
public class OrderTools {

    private static final Logger log = LoggerFactory.getLogger(OrderTools.class);

    private OrderTools() {}

    /**
     * 根据订单号查询订单状态
     *
     * @param orderId 订单号
     * @return 订单状态信息（模拟数据）
     */
    @Tool(
        name = "query_order_status",
        description = "根据订单号查询订单状态和物流信息。" +
                      "适用于用户询问"我的订单到哪了"或"订单什么时候发货"等问题。"
    )
    public static String queryOrderStatus(
            @ToolParam(name = "order_id", description = "订单号，例如：ORD20240101001")
            String orderId) {

        log.info("查询订单状态，订单号：{}", orderId);

        // 模拟返回订单状态
        return """
            订单信息：
            - 订单号：%s
            - 订单状态：运输中
            - 下单时间：2024-01-15 10:30:00
            - 预计送达：2024-01-20 18:00:00
            - 物流公司：顺丰速运
            - 物流单号：SF1234567890
            - 当前物流状态：
              1. [2024-01-15 10:30] 订单已发货
              2. [2024-01-16 08:00] 快件到达【杭州中转场】
              3. [2024-01-16 14:00] 快件离开【杭州中转场】，发往【上海中转场】
              4. [2024-01-17 09:00] 快件到达【上海中转场】
            """.formatted(orderId);
    }

    /**
     * 根据时间范围查询订单列表
     *
     * @param startDate 开始日期（YYYY-MM-DD）
     * @param endDate 结束日期（YYYY-MM-DD）
     * @return 订单列表摘要
     */
    @Tool(
        name = "query_order_list",
        description = "根据时间范围查询用户的订单列表。" +
                      "适用于用户询问"最近有哪些订单"或"这个月买了什么"等问题。"
    )
    public static String queryOrderList(
            @ToolParam(name = "start_date", description = "开始日期，格式：YYYY-MM-DD")
            String startDate,
            @ToolParam(name = "end_date", description = "结束日期，格式：YYYY-MM-DD")
            String endDate) {

        log.info("查询订单列表，时间范围：{} 至 {}", startDate, endDate);

        // 模拟返回订单列表
        return """
            订单列表（%s 至 %s）：
            1. ORD20240115001 - ¥299.00 - 运输中 - 2024-01-15
            2. ORD20240110002 - ¥59.00 - 已完成 - 2024-01-10
            3. ORD20240105003 - ¥1299.00 - 已完成 - 2024-01-05
            4. ORD20240101004 - ¥89.00 - 已完成 - 2024-01-01
            合计：4 个订单，总金额：¥1746.00
            """.formatted(startDate, endDate);
    }
}