package io.agentscope.tutorial.chapter05.config;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.formatter.dashscope.DashScopeChatFormatter;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.tutorial.chapter05.service.ToolService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AgentScope 配置类
 *
 * <p>配置 ReActAgent 实例，集成工具系统和模型客户端。
 *
 * @author AgentScope Tutorial
 * @since 1.0
 */
@Configuration
public class ToolConfig {

    @Value("${agentscope.api.key:demo-key}")
    private String apiKey;

    @Value("${agentscope.model.name:qwen-max}")
    private String modelName;

    @Value("${agentscope.model.base-url:}")
    private String baseUrl;

    /**
     * 创建 ReActAgent 实例
     */
    @Bean
    public ReActAgent reactAgent(ToolService toolService) {
        // 获取已配置的 Toolkit
        Toolkit toolkit = toolService.getToolkit();

        // 构建 Agent
        return ReActAgent.builder()
            .name("TutorialAgent")
            .sysPrompt(
                "你是一个智能助手，名为 TutorialAgent。\n" +
                "你可以使用各种工具来回答问题和完成任务。\n" +
                "可用工具包括：\n" +
                "  - 计算器：数学运算、阶乘、质数判断等\n" +
                "  - 天气查询：查询城市天气、预报、空气质量\n" +
                "  - 日期时间：获取当前时间、日期计算、星期查询\n" +
                "  - 搜索工具：网络搜索功能\n" +
                "  - HTTP 工具：发送 HTTP 请求\n\n" +
                "当用户需要使用工具时，请清楚地说明你正在使用什么工具。\n" +
                "如果某个工具分组未激活，你可以要求用户激活它。"
            )
            .model(
                DashScopeChatModel.builder()
                    .apiKey(apiKey)
                    .modelName(modelName)
                    .stream(true)
                    .enableThinking(false)
                    .formatter(new DashScopeChatFormatter())
                    .build()
            )
            .toolkit(toolkit)
            .memory(new InMemoryMemory())
            .enableMetaTool(true)  // 启用元工具，允许 Agent 动态管理工具分组
            .build();
    }
}