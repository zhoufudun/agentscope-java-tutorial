package io.agentscope.tutorial.chapter08;

import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.model.Model;
import io.agentscope.tutorial.chapter08.agent.AddressUpdateAgent;
import io.agentscope.tutorial.chapter08.agent.OrderAgent;
import io.agentscope.tutorial.chapter08.agent.QueryAgent;
import io.agentscope.tutorial.chapter08.agent.RouterAgent;
import io.agentscope.tutorial.chapter08.agent.SynthesisAgent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 多代理架构测试
 *
 * 测试各种多代理模式的协作：
 * - 路由模式：RouterAgent 分析意图并分发
 * - Pipeline 模式：各代理内部使用工具链处理
 * - 并行模式：多个代理同时处理
 * - 结果合成：SynthesisAgent 整合输出
 */
@Disabled("需要有效的 DashScope API Key 才能运行")
class MultiAgentTest {

    private static Model model;
    private static RouterAgent routerAgent;
    private static OrderAgent orderAgent;
    private static QueryAgent queryAgent;
    private static AddressUpdateAgent addressAgent;
    private static SynthesisAgent synthesisAgent;

    @BeforeAll
    static void setup() {
        // 从环境变量获取 API Key
        String apiKey = System.getenv("AI_DASHSCOPE_API_KEY");
        assertNotNull(apiKey, "需要设置 AI_DASHSCOPE_API_KEY 环境变量");

        model = DashScopeChatModel.builder()
            .apiKey(apiKey)
            .modelName("qwen-plus")
            .build();

        // 创建各专业代理
        orderAgent = new OrderAgent(model);
        queryAgent = new QueryAgent(model);
        addressAgent = new AddressUpdateAgent(model);
        synthesisAgent = new SynthesisAgent(model);

        // 创建路由器代理
        routerAgent = new RouterAgent(model, orderAgent, queryAgent, addressAgent, synthesisAgent);
    }

    @Test
    void testRouterAgentOrderQuery() {
        String query = "帮我查一下订单 ORD20240115001 到哪里了？";

        RouterAgent.RoutingResult result = routerAgent.route(query);

        assertNotNull(result);
        assertEquals("ORDER_QUERY", result.category());
        assertNotNull(result.agentResult());
        assertNotNull(result.finalResult());
        assertFalse(result.finalResult().isEmpty());
    }

    @Test
    void testRouterAgentGeneralQuery() {
        String query = "我有多少积分？有什么优惠可以用？";

        RouterAgent.RoutingResult result = routerAgent.route(query);

        assertNotNull(result);
        assertEquals("GENERAL_QUERY", result.category());
        assertNotNull(result.agentResult());
        assertNotNull(result.finalResult());
    }

    @Test
    void testRouterAgentAddressUpdate() {
        String query = "帮我把收货地址改成上海市浦东新区世纪大道100号，收货人李四，手机号13900001111";

        RouterAgent.RoutingResult result = routerAgent.route(query);

        assertNotNull(result);
        assertEquals("ADDRESS_UPDATE", result.category());
        assertNotNull(result.agentResult());
        assertNotNull(result.finalResult());
    }

    @Test
    void testOrderAgent() {
        String query = "查询订单 ORD20240115001 的物流状态";

        String result = orderAgent.handle(query);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        // 验证结果包含订单信息
        assertTrue(result.contains("ORD20240115001") || result.contains("订单"));
    }

    @Test
    void testQueryAgent() {
        String query = "我有多少积分？";

        String result = queryAgent.handle(query);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        // 验证结果包含账户信息
        assertTrue(result.contains("积分") || result.contains("账户") || result.contains("会员"));
    }

    @Test
    void testAddressAgent() {
        String query = "查看当前收货地址";

        String result = addressAgent.handle(query);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        // 验证结果包含地址信息
        assertTrue(result.contains("地址") || result.contains("收货"));
    }

    @Test
    void testSynthesisAgent() {
        String query = "查询订单状态";
        String agentResult = "订单 ORD20240115001 状态：运输中，预计送达：2024-01-20";
        String category = "ORDER_QUERY";

        String result = synthesisAgent.synthesize(query, agentResult, category);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.length() > agentResult.length()); // 合成后应该更丰富
    }

    @Test
    void testParallelProcessing() {
        // 测试并行处理多个查询
        String[] queries = {
            "查询订单 ORD20240115001 状态",
            "我有多少积分",
            "查看收货地址"
        };

        for (String query : queries) {
            RouterAgent.RoutingResult result = routerAgent.route(query);
            assertNotNull(result, "查询 " + query + " 应该返回结果");
            assertNotNull(result.category(), "应该有分类结果");
            assertFalse(result.finalResult().isEmpty(), "应该有最终回复");
        }
    }

    @Test
    void testErrorHandling() {
        // 测试服务降级
        String query = "这是一个无法理解的随机文本 abc123 xyz";

        RouterAgent.RoutingResult result = routerAgent.route(query);

        assertNotNull(result);
        // 即使无法分类，也应该返回一个合理的回复
        assertNotNull(result.finalResult());
        assertFalse(result.finalResult().isEmpty());
    }
}