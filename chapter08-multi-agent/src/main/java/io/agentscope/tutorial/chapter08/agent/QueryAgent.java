package io.agentscope.tutorial.chapter08.agent;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.model.Model;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.tutorial.chapter08.tool.QueryTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 查询代理（Query Agent）
 *
 * 专门处理综合查询：
 * - 产品搜索和推荐
 * - 优惠活动查询
 * - 账户信息查询（积分、优惠券、会员等级）
 *
 * 内部使用 Pipeline 模式：分类查询类型 → 执行对应查询
 */
public class QueryAgent {

    private static final Logger log = LoggerFactory.getLogger(QueryAgent.class);

    private final ReActAgent agent;

    public QueryAgent(Model model) {
        this.agent = createQueryAgent(model);
        log.info("查询代理初始化完成");
    }

    private ReActAgent createQueryAgent(Model model) {
        // 创建工具集
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(new QueryTools());

        return ReActAgent.builder()
            .name("query_agent")
            .description("查询代理：处理产品搜索、优惠查询、账户信息")
            .model(model)
            .sysPrompt("""
                你是一个综合查询专家。

                你的职责是帮助用户查询产品、优惠活动和账户信息。

                可用工具：
                1. search_products：搜索商品
                   - 参数：keyword（搜索关键词）
                   - 适用于产品推荐和价格查询

                2. query_promotions：查询当前优惠活动
                   - 参数：user_id（用户ID）
                   - 返回当前可用的所有优惠

                3. query_account_info：查询账户信息
                   - 参数：user_id（用户ID）
                   - 返回会员等级、积分、优惠券等信息

                处理规则：
                1. 分析用户查询确定需要的工具：
                   - 询问产品/价格 → search_products
                   - 询问优惠/折扣 → query_promotions
                   - 询问积分/优惠券/会员 → query_account_info
                   - 多个问题可以调用多个工具

                2. 用户ID固定使用：user_001（演示环境）

                3. 查询结果需要用友好的方式呈现给用户

                输出要求：
                - 清晰展示查询结果
                - 突出重要信息（如优惠有效期、会员特权等）
                - 如有相关信息，给出行动建议
                """)
            .toolkit(toolkit)
            .memory(new InMemoryMemory())
            .maxIters(5)
            .build();
    }

    /**
     * 处理综合查询
     *
     * @param query 用户查询
     * @return 处理结果
     */
    public String handle(String query) {
        log.info("处理综合查询：{}", query);

        try {
            var response = agent.call(io.agentscope.core.message.Msg.builder()
                .role(io.agentscope.core.message.MsgRole.USER)
                .content(io.agentscope.core.message.TextBlock.builder().text(query).build())
                .build()).block();

            String result = response != null ? response.getTextContent() : "";
            log.info("综合查询完成，结果长度：{}", result.length());
            return result;

        } catch (Exception e) {
            log.error("综合查询失败", e);
            return "抱歉，综合查询服务暂时不可用，请稍后重试。";
        }
    }

    /**
     * 获取底层 Agent（用于测试）
     */
    public ReActAgent getAgent() {
        return agent;
    }
}