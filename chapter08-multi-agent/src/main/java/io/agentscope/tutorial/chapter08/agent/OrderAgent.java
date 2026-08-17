package io.agentscope.tutorial.chapter08.agent;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.model.Model;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.tutorial.chapter08.tool.OrderTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 订单代理（Order Agent）
 *
 * 专门处理订单相关的查询：
 * - 订单状态查询
 * - 物流信息查询
 * - 历史订单列表查询
 *
 * 内部使用 Pipeline 模式：提取订单信息 → 查询订单详情
 */
public class OrderAgent {

    private static final Logger log = LoggerFactory.getLogger(OrderAgent.class);

    private final ReActAgent agent;

    public OrderAgent(Model model) {
        this.agent = createOrderAgent(model);
        log.info("订单代理初始化完成");
    }

    private ReActAgent createOrderAgent(Model model) {
        // 创建工具集
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(new OrderTools());

        return ReActAgent.builder()
            .name("order_agent")
            .description("订单代理：处理订单状态、物流信息查询")
            .model(model)
            .sysPrompt("""
                你是一个订单查询专家。

                你的职责是帮助用户查询订单信息和物流状态。

                可用工具：
                1. query_order_status：查询指定订单的状态和物流信息
                   - 参数：order_id（订单号）
                   - 示例：ORD20240115001

                2. query_order_list：查询某个时间段的订单列表
                   - 参数：start_date, end_date（格式：YYYY-MM-DD）
                   - 适用于查询历史订单

                处理规则：
                1. 如果用户提供了订单号，直接使用 query_order_status 查询
                2. 如果用户询问"最近订单"或"所有订单"，使用 query_order_list
                   - 开始日期：可以设为 30 天前
                   - 结束日期：今天
                3. 查询结果需要用友好的方式呈现给用户

                输出要求：
                - 清晰展示订单状态和关键信息
                - 对于运输中的订单，提醒预计送达时间
                - 如有异常情况，给出适当的建议
                """)
            .toolkit(toolkit)
            .maxIters(5)
            .build();
    }

    /**
     * 处理订单查询
     *
     * @param query 用户查询
     * @return 处理结果
     */
    public String handle(String query) {
        log.info("处理订单查询：{}", query);

        try {
            var response = agent.call(io.agentscope.core.message.Msg.builder()
                .role(io.agentscope.core.message.MsgRole.USER)
                .content(io.agentscope.core.message.TextBlock.builder().text(query).build())
                .build()).block();

            String result = response != null ? response.getTextContent() : "";
            log.info("订单查询完成，结果长度：{}", result.length());
            return result;

        } catch (Exception e) {
            log.error("订单查询失败", e);
            return "抱歉，订单查询服务暂时不可用，请稍后重试。";
        }
    }

    /**
     * 获取底层 Agent（用于测试）
     */
    public ReActAgent getAgent() {
        return agent;
    }
}