package io.agentscope.tutorial.chapter08;

import io.agentscope.tutorial.chapter08.model.ServiceResult;
import io.agentscope.tutorial.chapter08.service.MultiAgentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 演示运行器
 *
 * 当配置文件中设置 customer-service.demo.enabled=true 时，
 * 应用启动后自动运行演示查询。
 */
@Component
@ConditionalOnProperty(name = "customer-service.demo.enabled", havingValue = "true")
public class Chapter08Demo implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(Chapter08Demo.class);

    private final MultiAgentService multiAgentService;

    public Chapter08Demo(MultiAgentService multiAgentService) {
        this.multiAgentService = multiAgentService;
    }

    @Override
    public void run(String... args) {
        log.info("===========================================");
        log.info("第八章案例演示：多代理架构模式");
        log.info("===========================================");

        try {
            // 演示1：订单查询
            demonstrateOrderQuery();

            // 演示2：综合查询
            demonstrateGeneralQuery();

            // 演示3：地址更新
            demonstrateAddressUpdate();

            log.info("===========================================");
            log.info("演示完成！如需测试更多功能，请访问：");
            log.info("  POST /api/chat/message - 通用聊天接口");
            log.info("  POST /api/chat/order - 订单查询接口");
            log.info("  POST /api/chat/query - 综合查询接口");
            log.info("  POST /api/chat/batch - 批量处理接口");
            log.info("===========================================");

        } catch (Exception e) {
            log.error("演示过程中出现错误：{}", e.getMessage());
            log.info("请确保已设置 AI_DASHSCOPE_API_KEY 环境变量");
        }
    }

    private void demonstrateOrderQuery() {
        log.info("\n--- 演示1：订单查询 ---");
        String query = "帮我查一下订单 ORD20240115001 到哪里了？";

        log.info("用户查询：{}", query);
        ServiceResult result = multiAgentService.processOrderQuery(query);

        log.info("分类结果：{}", result.classify().category());
        log.info("处理代理：{}", result.agentsInvoked());
        log.info("最终回复：\n{}", result.finalAnswer());
    }

    private void demonstrateGeneralQuery() {
        log.info("\n--- 演示2：综合查询 ---");
        String query = "我有多少积分？有什么优惠可以用？";

        log.info("用户查询：{}", query);
        ServiceResult result = multiAgentService.processGeneralQuery(query);

        log.info("分类结果：{}", result.classify().category());
        log.info("处理代理：{}", result.agentsInvoked());
        log.info("最终回复：\n{}", result.finalAnswer());
    }

    private void demonstrateAddressUpdate() {
        log.info("\n--- 演示3：地址更新 ---");
        String query = "帮我把收货地址改成上海市浦东新区世纪大道100号，收货人李四，手机号13900001111";

        log.info("用户查询：{}", query);
        ServiceResult result = multiAgentService.process(query);

        log.info("分类结果：{}", result.classify().category());
        log.info("处理代理：{}", result.agentsInvoked());
        log.info("最终回复：\n{}", result.finalAnswer());
    }
}