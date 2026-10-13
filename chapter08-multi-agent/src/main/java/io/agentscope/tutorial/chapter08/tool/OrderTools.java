package io.agentscope.tutorial.chapter08.tool;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 订单查询工具类（模拟实现）
 *
 * <p>在生产环境中，此工具类会调用真实的订单服务 API。
 * 这里使用模拟数据来演示 AgentScope 中工具调用的工作原理。
 *
 * <h2>AgentScope 学习要点</h2>
 * <ul>
 *   <li><b>@Tool 注解</b>：标记一个方法作为可被 Agent 调用的工具</li>
 *   <li><b>@ToolParam 注解</b>：定义工具参数的名称和描述，帮助 LLM 理解如何使用</li>
 *   <li><b>静态方法</b>：工具方法可以是静态的，便于注册和调用</li>
 *   <li><b>工具描述</b>：description 字段帮助 LLM 判断何时使用该工具</li>
 * </ul>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 注册工具到 Toolkit
 * Toolkit toolkit = new Toolkit();
 * toolkit.registerTool(new OrderTools());
 *
 * // 将 Toolkit 绑定到 Agent
 * ReActAgent agent = ReActAgent.builder()
 *     .toolkit(toolkit)
 *     .build();
 * }</pre>
 *
 * @author AgentScope Tutorial
 * @see io.agentscope.core.tool.Tool
 * @see io.agentscope.core.tool.Toolkit
 */
public class OrderTools {

    private static final Logger log = LoggerFactory.getLogger(OrderTools.class);

    /**
     * 默认构造函数
     */
    public OrderTools() {}

    /**
     * 根据订单号查询订单状态
     *
     * <p>此工具展示了如何定义一个带参数的工具。
     * LLM 会根据用户的查询自动提取订单号并调用此方法。
     *
     * @param orderId 订单号，例如：ORD20240101001
     * @return 订单状态信息（模拟数据）
     */
    @Tool(
        name = "query_order_status",
        description = "根据订单号查询订单状态和物流信息。当用户询问订单进度或物流状态时使用此工具。"
    )
    public static String queryOrderStatus(
            @ToolParam(name = "order_id", description = "订单号，例如：ORD20240101001")
            String orderId) {

        log.info("查询订单状态，订单号：{}", orderId);

        // 模拟订单状态响应
        return """
            订单信息：
            - 订单号：%s
            - 状态：运输中
            - 下单时间：2024-01-15 10:30:00
            - 预计送达：2024-01-20 18:00:00
            - 物流公司：顺丰速运
            - 运单号：SF1234567890
            - 物流轨迹：
              1. [2024-01-15 10:30] 订单已发货
              2. [2024-01-16 08:00] 到达【杭州转运中心】
              3. [2024-01-16 14:00] 离开【杭州转运中心】，发往【上海转运中心】
              4. [2024-01-17 09:00] 到达【上海转运中心】
            """.formatted(orderId);
    }

    /**
     * 根据日期范围查询订单列表
     *
     * <p>此工具展示了如何定义带多个参数的工具。
     * LLM 会根据用户描述（如"最近的订单"）推断日期范围。
     *
     * @param startDate 开始日期，格式：YYYY-MM-DD
     * @param endDate 结束日期，格式：YYYY-MM-DD
     * @return 订单列表摘要
     */
    @Tool(
        name = "query_order_list",
        description = "根据日期范围查询用户的订单列表。当用户询问最近订单或历史购买记录时使用此工具。"
    )
    public static String queryOrderList(
            @ToolParam(name = "start_date", description = "开始日期，格式：YYYY-MM-DD")
            String startDate,
            @ToolParam(name = "end_date", description = "结束日期，格式：YYYY-MM-DD")
            String endDate) {

        log.info("查询订单列表，日期范围：{} 至 {}", startDate, endDate);

        // 模拟订单列表响应
        return """
            订单列表（%s 至 %s）：
            1. ORD20240115001 - ¥299.00 - 运输中 - 2024-01-15
            2. ORD20240110002 - ¥59.00 - 已完成 - 2024-01-10
            3. ORD20240105003 - ¥1299.00 - 已完成 - 2024-01-05
            4. ORD20240101004 - ¥89.00 - 已完成 - 2024-01-01
            合计：4 笔订单，总金额：¥1746.00
            """.formatted(startDate, endDate);
    }
}