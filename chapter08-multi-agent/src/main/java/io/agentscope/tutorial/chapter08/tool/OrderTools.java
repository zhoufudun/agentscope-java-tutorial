package io.agentscope.tutorial.chapter08.tool;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Order query tools (simulated implementation)
 *
 * In production, this tool would call real order service APIs.
 * Here we use simulated data to demonstrate how tool invocation works.
 */
public class OrderTools {

    private static final Logger log = LoggerFactory.getLogger(OrderTools.class);

    public OrderTools() {}

    /**
     * Query order status by order ID
     *
     * @param orderId Order ID
     * @return Order status information (simulated data)
     */
    @Tool(
        name = "query_order_status",
        description = "Query order status and logistics information by order ID. Use when user asks about order progress or shipping status."
    )
    public static String queryOrderStatus(
            @ToolParam(name = "order_id", description = "Order ID, e.g.: ORD20240101001")
            String orderId) {

        log.info("Querying order status, order ID: {}", orderId);

        // Simulated order status response
        return """
            Order Information:
            - Order ID: %s
            - Status: In Transit
            - Order Time: 2024-01-15 10:30:00
            - Estimated Delivery: 2024-01-20 18:00:00
            - Logistics Company: SF Express
            - Tracking Number: SF1234567890
            - Current Status:
              1. [2024-01-15 10:30] Order Shipped
              2. [2024-01-16 08:00] Arrived at [Hangzhou Hub]
              3. [2024-01-16 14:00] Left [Hangzhou Hub] to [Shanghai Hub]
              4. [2024-01-17 09:00] Arrived at [Shanghai Hub]
            """.formatted(orderId);
    }

    /**
     * Query order list by date range
     *
     * @param startDate Start date (YYYY-MM-DD)
     * @param endDate End date (YYYY-MM-DD)
     * @return Order list summary
     */
    @Tool(
        name = "query_order_list",
        description = "Query user's order list by date range. Use when user asks about recent orders or historical purchases."
    )
    public static String queryOrderList(
            @ToolParam(name = "start_date", description = "Start date, format: YYYY-MM-DD")
            String startDate,
            @ToolParam(name = "end_date", description = "End date, format: YYYY-MM-DD")
            String endDate) {

        log.info("Querying order list, date range: {} to {}", startDate, endDate);

        // Simulated order list response
        return """
            Order List (%s to %s):
            1. ORD20240115001 - ¥299.00 - In Transit - 2024-01-15
            2. ORD20240110002 - ¥59.00 - Completed - 2024-01-10
            3. ORD20240105003 - ¥1299.00 - Completed - 2024-01-05
            4. ORD20240101004 - ¥89.00 - Completed - 2024-01-01
            Total: 4 orders, Amount: ¥1746.00
            """.formatted(startDate, endDate);
    }
}