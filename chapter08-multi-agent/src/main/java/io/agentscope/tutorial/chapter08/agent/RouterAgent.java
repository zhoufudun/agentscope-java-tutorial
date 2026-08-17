package io.agentscope.tutorial.chapter08.agent;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 路由器代理（Router Agent）
 *
 * 负责接收用户查询并进行意图分析，然后将任务分发到对应的专业代理。
 *
 * 核心职责：
 * 1. 分析用户查询的意图（订单查询、地址更新、综合查询）
 * 2. 根据意图决定调用哪个专业代理
 * 3. 返回分类结果和路由决策
 */
public class RouterAgent {

    private static final Logger log = LoggerFactory.getLogger(RouterAgent.class);

    private final ReActAgent agent;
    private final OrderAgent orderAgent;
    private final QueryAgent queryAgent;
    private final AddressUpdateAgent addressUpdateAgent;
    private final SynthesisAgent synthesisAgent;

    public RouterAgent(Model model, OrderAgent orderAgent, QueryAgent queryAgent,
                       AddressUpdateAgent addressUpdateAgent, SynthesisAgent synthesisAgent) {
        this.orderAgent = orderAgent;
        this.queryAgent = queryAgent;
        this.addressUpdateAgent = addressUpdateAgent;
        this.synthesisAgent = synthesisAgent;
        this.agent = createRouterAgent(model);
        log.info("路由器代理初始化完成");
    }

    private ReActAgent createRouterAgent(Model model) {
        return ReActAgent.builder()
            .name("router_agent")
            .description("路由器代理：分析用户查询意图并分发到对应专业代理")
            .model(model)
            .sysPrompt("""
                你是一个智能客户服务中心路由器。

                你的职责是分析用户的查询意图，并将其分类到以下类别之一：

                1. order_query（订单查询）：
                   - 查询订单状态、物流信息
                   - 查询历史订单列表
                   - 示例："我的订单到哪了"、"查一下订单"

                2. address_update（地址更新）：
                   - 修改收货地址
                   - 更新配送信息
                   - 示例："帮我改地址"、"换个收货人"

                3. general_query（综合查询）：
                   - 产品搜索和推荐
                   - 优惠活动查询
                   - 账户信息查询（积分、优惠券、会员等级）
                   - 示例："有什么优惠"、"我的积分是多少"

                输出格式：
                请只输出分类结果，使用以下格式之一：
                - [ORDER_QUERY] 用户查询内容
                - [ADDRESS_UPDATE] 用户查询内容
                - [GENERAL_QUERY] 用户查询内容

                不要输出其他解释文字。
                """)
            .build();
    }

    /**
     * 处理用户查询
     *
     * @param query 用户查询
     * @return 路由结果，包含分类和对应的处理结果
     */
    public RoutingResult route(String query) {
        log.info("开始路由分析：{}", query);

        try {
            // 调用路由代理分析意图
            var response = agent.call(io.agentscope.core.message.Msg.builder()
                .role(io.agentscope.core.message.MsgRole.USER)
                .content(io.agentscope.core.message.TextBlock.builder().text(query).build())
                .build()).block();

            String responseText = response != null ? response.getTextContent() : "";

            // 解析分类结果
            String category = parseCategory(responseText);
            log.info("分类结果：{}", category);

            // 根据分类调用对应的专业代理
            String result = switch (category) {
                case "ORDER_QUERY" -> orderAgent.handle(query);
                case "ADDRESS_UPDATE" -> addressUpdateAgent.handle(query);
                case "GENERAL_QUERY" -> queryAgent.handle(query);
                default -> "抱歉，我无法理解您的请求，请重新描述。";
            };

            // 进行结果合成
            String finalResult = synthesisAgent.synthesize(query, result, category);

            return new RoutingResult(category, result, finalResult);

        } catch (Exception e) {
            log.error("路由处理失败", e);
            return new RoutingResult("UNKNOWN", "服务暂时不可用，请稍后重试。",
                "抱歉，服务出现异常，请稍后重试。");
        }
    }

    /**
     * 解析分类结果
     */
    private String parseCategory(String response) {
        String upper = response.toUpperCase();
        if (upper.contains("[ORDER_QUERY]")) {
            return "ORDER_QUERY";
        } else if (upper.contains("[ADDRESS_UPDATE]")) {
            return "ADDRESS_UPDATE";
        } else if (upper.contains("[GENERAL_QUERY]")) {
            return "GENERAL_QUERY";
        }
        return "UNKNOWN";
    }

    /**
     * 获取底层 Agent（用于测试）
     */
    public ReActAgent getAgent() {
        return agent;
    }

    /**
     * 路由结果记录
     */
    public record RoutingResult(
        String category,    // 分类：ORDER_QUERY / ADDRESS_UPDATE / GENERAL_QUERY / UNKNOWN
        String agentResult, // 专业代理的处理结果
        String finalResult  // 最终合成后的结果
    ) {}
}