/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.agentscope.tutorial.chapter04.config;

import io.agentscope.core.ReActAgent;
import io.agentscope.extensions.model.dashscope.formatter.DashScopeChatFormatter;
import io.agentscope.core.hook.Hook;
import io.agentscope.extensions.model.dashscope.DashScopeChatModel;
import io.agentscope.core.model.ExecutionConfig;
import io.agentscope.core.model.Model;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.tutorial.chapter04.hook.LoggingHook;
import io.agentscope.tutorial.chapter04.tool.CalculatorTool;
import io.agentscope.tutorial.chapter04.tool.DateTimeTool;
import io.agentscope.tutorial.chapter04.tool.WeatherTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

/**
 * Agent 配置类
 *
 * 配置 ReActAgent 实例，包括：
 * - 模型配置
 * - 工具包配置
 * - 钩子配置
 * - 记忆配置
 *
 * @author AgentScope Tutorial
 */
@Configuration
public class AgentConfig {

    private static final Logger log = LoggerFactory.getLogger(AgentConfig.class);

    @Value("${agentscope.model.api-key:EMPTY}")
    private String apiKey;

    @Value("${agentscope.model.model-name:qwen-plus}")
    private String modelName;

    @Value("${agentscope.model.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String baseUrl;

    /**
     * 创建 DashScope 聊天模型
     */
    @Bean
    public Model dashScopeModel() {
        log.info("初始化 DashScope 模型: {}, baseUrl: {}", modelName, baseUrl);

        return DashScopeChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .baseUrl(baseUrl)
                .stream(true)
                .enableThinking(true)
                .formatter(new DashScopeChatFormatter())
                .build();
    }

    /**
     * 创建工具包
     *
     * 配置 Agent 可用的所有工具
     */
    @Bean
    public Toolkit toolkit() {
        Toolkit toolkit = new Toolkit();

        // 注册工具类（自动发现所有 @Tool 注解方法）
        toolkit.registerTool(new WeatherTool());
        toolkit.registerTool(new CalculatorTool());
        toolkit.registerTool(new DateTimeTool());

        log.info("工具包已创建，注册了以下工具:");
        log.info("  - get_current_weather: 获取当前天气");
        log.info("  - get_weather_forecast: 获取天气预报");
        log.info("  - get_weather_index: 获取生活指数");
        log.info("  - calculate: 数学计算");
        log.info("  - convert_unit: 单位换算");
        log.info("  - get_current_time: 获取当前时间");
        log.info("  - calculate_days_between: 计算日期差");
        log.info("  - get_day_of_week: 获取星期几");

        return toolkit;
    }

    /**
     * 创建日志钩子
     */
    @Bean
    public Hook loggingHook() {
        return new LoggingHook();
    }

    /**
     * 创建 ReActAgent
     *
     * 配置完整的 ReActAgent 实例，包括：
     * - 名称和描述
     * - 系统提示词
     * - 模型
     * - 工具包
     * - 记忆存储
     * - 最大迭代次数
     * - 钩子列表
     */
    @Bean
    public ReActAgent reactAgent(Model model, Toolkit toolkit, Hook loggingHook) {
        String systemPrompt = """
                你是一个智能助手，名为小探。你有以下能力：

                1. 天气查询：你可以查询城市当前的天气、天气预报以及各种生活指数。
                   可用的天气工具包括：
                   - get_current_weather: 获取当前天气
                   - get_weather_forecast: 获取天气预报
                   - get_weather_index: 获取生活指数
                   - get_supported_index_types: 获取支持的指数类型

                2. 数学计算：你可以执行数学表达式的计算。
                   - calculate: 执行数学计算，支持加、减、乘、除、幂、平方根等

                3. 单位换算：你可以进行各种单位之间的转换。
                   - convert_unit: 单位换算
                   支持长度单位：km, m, cm, mm, mi, ft, in, yd
                   支持重量单位：kg, g, mg, t, lb, oz
                   支持温度单位：c/f/k

                4. 时间查询：你可以获取当前时间信息、计算日期差。
                   - get_current_time: 获取当前时间（支持时区）
                   - calculate_days_between: 计算两个日期之间的天数
                   - get_day_of_week: 获取指定日期是星期几

                请根据用户的问题，选择合适的工具来回答。
                如果用户的问题需要调用工具才能回答，请仔细分析并选择正确的工具。
                每次只调用一个工具，等待结果后再决定下一步。
                """;

        log.info("创建 ReActAgent: 小探");

        return ReActAgent.builder()
                .name("小探")
                .description("一个智能助手，可以查询天气、数学计算、单位换算、时间查询等")
                .sysPrompt(systemPrompt)
                .model(model)
                .toolkit(toolkit)
                // 2.x 起 ReActAgent 不再通过 builder 配置 memory，
                // 会话历史改由 AgentState 内部维护（可通过 agent.getAgentState().getContext() 访问），
                // 需要持久化时使用 .stateStore(store).defaultSessionId(id)。
                .maxIters(15)  // 最多15次推理-行动循环
                .hooks(List.of(loggingHook))
                .build();
    }
}