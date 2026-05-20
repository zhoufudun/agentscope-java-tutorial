package io.agentscope.tutorial.chapter08.agent;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.model.Model;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.tutorial.chapter08.tool.QueryTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 地址更新代理（Address Update Agent）
 *
 * 专门处理收货地址相关的操作：
 * - 查询当前收货地址
 * - 更新收货地址
 *
 * 使用 Pipeline 模式：获取当前地址 → 验证更新 → 执行更新
 */
public class AddressUpdateAgent {

    private static final Logger log = LoggerFactory.getLogger(AddressUpdateAgent.class);

    private final ReActAgent agent;

    public AddressUpdateAgent(Model model) {
        this.agent = createAddressUpdateAgent(model);
        log.info("地址更新代理初始化完成");
    }

    private ReActAgent createAddressUpdateAgent(Model model) {
        // 创建工具集
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(new QueryTools());

        return ReActAgent.builder()
            .name("address_update_agent")
            .description("地址更新代理：处理收货地址查询和更新")
            .model(model)
            .sysPrompt("""
                你是一个地址管理专家。

                你的职责是帮助用户查询和更新收货地址。

                可用工具：
                1. get_shipping_address：获取当前收货地址
                   - 参数：user_id（用户ID）
                   - 返回当前的默认收货地址

                2. update_shipping_address：更新收货地址
                   - 参数：
                     * user_id：用户ID
                     * province：省份（如：浙江省）
                     * city：城市（如：杭州市）
                     * district：区/县（如：西湖区）
                     * detail_address：详细地址
                     * recipient：收货人姓名
                     * phone：手机号

                处理规则：
                1. 首先调用 get_shipping_address 获取当前地址
                2. 确认用户要更新的新地址信息是否完整
                   - 必需信息：省份、城市、区县、详细地址、收货人、手机号
                   - 如果信息不完整，需要询问补充
                3. 调用 update_shipping_address 完成更新
                4. 返回更新确认信息

                重要提示：
                - 地址更新是敏感操作，需要谨慎处理
                - 确保所有必填信息都已提供
                - 用户ID固定使用：user_001（演示环境）

                输出要求：
                - 清晰展示更新前后的地址对比
                - 强调地址更新成功
                - 提醒用户确认收货信息
                """)
            .toolkit(toolkit)
            .memory(new InMemoryMemory())
            .maxIters(5)
            .build();
    }

    /**
     * 处理地址更新请求
     *
     * @param query 用户查询
     * @return 处理结果
     */
    public String handle(String query) {
        log.info("处理地址更新请求：{}", query);

        try {
            var response = agent.call(io.agentscope.core.message.Msg.builder()
                .role(io.agentscope.core.message.MsgRole.USER)
                .content(io.agentscope.core.message.TextBlock.builder().text(query).build())
                .build()).block();

            String result = response != null ? response.getTextContent() : "";
            log.info("地址更新完成，结果长度：{}", result.length());
            return result;

        } catch (Exception e) {
            log.error("地址更新失败", e);
            return "抱歉，地址更新服务暂时不可用，请稍后重试。";
        }
    }

    /**
     * 获取底层 Agent（用于测试）
     */
    public ReActAgent getAgent() {
        return agent;
    }
}