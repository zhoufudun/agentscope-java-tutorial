package io.agentscope.tutorial.chapter08.service;

import io.agentscope.core.model.Model;
import io.agentscope.tutorial.chapter08.agent.AddressUpdateAgent;
import io.agentscope.tutorial.chapter08.agent.OrderAgent;
import io.agentscope.tutorial.chapter08.agent.QueryAgent;
import io.agentscope.tutorial.chapter08.agent.RouterAgent;
import io.agentscope.tutorial.chapter08.agent.SynthesisAgent;
import io.agentscope.tutorial.chapter08.model.QueryClassification;
import io.agentscope.tutorial.chapter08.model.ServiceResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 多代理服务
 *
 * 整合路由代理和专业代理，提供完整的多代理协作服务。
 *
 * 服务流程：
 * 1. RouterAgent 分析用户查询意图
 * 2. 根据意图分发到对应的专业代理（OrderAgent / QueryAgent / AddressUpdateAgent）
 * 3. SynthesisAgent 合成最终回复
 *
 * 支持的模式：
 * - 路由模式：根据意图动态分发
 * - 并行模式：多个代理同时处理（当查询涉及多个方面时）
 * - Pipeline 模式：每个代理内部使用工具链处理
 */
@Service
public class MultiAgentService {

    private static final Logger log = LoggerFactory.getLogger(MultiAgentService.class);

    private final RouterAgent routerAgent;
    private final OrderAgent orderAgent;
    private final QueryAgent queryAgent;
    private final AddressUpdateAgent addressUpdateAgent;
    private final SynthesisAgent synthesisAgent;

    public MultiAgentService(Model model) {
        // 创建各个专业代理
        this.orderAgent = new OrderAgent(model);
        this.queryAgent = new QueryAgent(model);
        this.addressUpdateAgent = new AddressUpdateAgent(model);
        this.synthesisAgent = new SynthesisAgent(model);

        // 创建路由器代理（注入所有专业代理）
        this.routerAgent = new RouterAgent(model, orderAgent, queryAgent,
            addressUpdateAgent, synthesisAgent);

        log.info("多代理服务初始化完成");
    }

    /**
     * 处理用户查询（单代理处理）
     *
     * 使用路由模式：根据意图自动分发到专业代理
     *
     * @param query 用户查询
     * @return 服务结果
     */
    public ServiceResult process(String query) {
        log.info("处理用户查询：{}", query);

        try {
            // 调用路由器进行分析和分发
            RouterAgent.RoutingResult routingResult = routerAgent.route(query);

            // 转换为 ServiceResult
            String category = routingResult.category();
            QueryClassification classification = new QueryClassification(category,
                getReasonForCategory(category));

            List<String> agentsInvoked = new ArrayList<>();
            agentsInvoked.add(category);

            return new ServiceResult(
                query,
                classification,
                java.util.Map.of(category, routingResult.agentResult()),
                routingResult.finalResult(),
                agentsInvoked
            );

        } catch (Exception e) {
            log.error("处理用户查询失败", e);
            return ServiceResult.of(query,
                new QueryClassification(QueryClassification.UNKNOWN, "处理异常"),
                "抱歉，服务出现错误，请稍后重试。");
        }
    }

    /**
     * 处理订单查询（直接调用）
     */
    public ServiceResult processOrderQuery(String query) {
        log.info("直接处理订单查询：{}", query);

        try {
            String result = orderAgent.handle(query);
            String finalResult = synthesisAgent.synthesize(query, result, "ORDER_QUERY");

            return new ServiceResult(
                query,
                new QueryClassification(QueryClassification.ORDER_QUERY, "用户明确请求订单查询"),
                java.util.Map.of("order_query", result),
                finalResult,
                List.of("ORDER_QUERY")
            );

        } catch (Exception e) {
            log.error("订单查询失败", e);
            return ServiceResult.of(query,
                new QueryClassification(QueryClassification.ORDER_QUERY, "处理失败"),
                "抱歉，订单查询服务暂时不可用。");
        }
    }

    /**
     * 处理综合查询（直接调用）
     */
    public ServiceResult processGeneralQuery(String query) {
        log.info("直接处理综合查询：{}", query);

        try {
            String result = queryAgent.handle(query);
            String finalResult = synthesisAgent.synthesize(query, result, "GENERAL_QUERY");

            return new ServiceResult(
                query,
                new QueryClassification(QueryClassification.GENERAL_QUERY, "用户请求综合查询"),
                java.util.Map.of("general_query", result),
                finalResult,
                List.of("GENERAL_QUERY")
            );

        } catch (Exception e) {
            log.error("综合查询失败", e);
            return ServiceResult.of(query,
                new QueryClassification(QueryClassification.GENERAL_QUERY, "处理失败"),
                "抱歉，综合查询服务暂时不可用。");
        }
    }

    /**
     * 并行处理多个查询
     *
     * 当用户查询涉及多个方面时，可以并行调用多个代理
     *
     * @param queries 多个查询
     * @return 多个服务结果
     */
    public List<ServiceResult> processParallel(List<String> queries) {
        log.info("并行处理 {} 个查询", queries.size());

        List<ServiceResult> results = new ArrayList<>();

        for (String query : queries) {
            results.add(process(query));
        }

        return results;
    }

    /**
     * 获取路由代理（用于测试）
     */
    public RouterAgent getRouterAgent() {
        return routerAgent;
    }

    /**
     * 获取订单代理（用于测试）
     */
    public OrderAgent getOrderAgent() {
        return orderAgent;
    }

    /**
     * 获取查询代理（用于测试）
     */
    public QueryAgent getQueryAgent() {
        return queryAgent;
    }

    /**
     * 获取地址更新代理（用于测试）
     */
    public AddressUpdateAgent getAddressUpdateAgent() {
        return addressUpdateAgent;
    }

    /**
     * 获取合成代理（用于测试）
     */
    public SynthesisAgent getSynthesisAgent() {
        return synthesisAgent;
    }

    /**
     * 获取分类原因
     */
    private String getReasonForCategory(String category) {
        return switch (category) {
            case "ORDER_QUERY" -> "查询涉及订单状态或物流信息";
            case "ADDRESS_UPDATE" -> "查询涉及收货地址更新";
            case "GENERAL_QUERY" -> "查询涉及产品、优惠或账户信息";
            default -> "根据查询内容匹配";
        };
    }
}