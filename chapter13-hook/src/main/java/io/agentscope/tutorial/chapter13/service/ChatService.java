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
package io.agentscope.tutorial.chapter13.service;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.core.tool.ToolParam;
import io.agentscope.tutorial.chapter13.hook.AuditHook;
import io.agentscope.tutorial.chapter13.hook.LoggingHook;
import io.agentscope.tutorial.chapter13.hook.MetricsHook;
import io.agentscope.tutorial.chapter13.hook.TimingHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Chat 服务层 - 封装 Agent 的调用逻辑.
 *
 * <p>提供：
 * <ul>
 *   <li>同步调用接口</li>
 *   <li>异步调用接口（使用 Project Reactor）</li>
 *   <li>Agent 状态查询</li>
 *   <li>钩子链管理</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    /** 注入的 Agent 实例 */
    private final ReActAgent agent;

    /** 注入的日志钩子 */
    private final LoggingHook loggingHook;

    /** 注入的监控钩子 */
    private final MetricsHook metricsHook;

    /** 注入的审计钩子 */
    private final AuditHook auditHook;

    /** 注入的性能钩子 */
    private final TimingHook timingHook;

    public ChatService(
            ReActAgent agent,
            LoggingHook loggingHook,
            MetricsHook metricsHook,
            AuditHook auditHook,
            TimingHook timingHook) {
        this.agent = agent;
        this.loggingHook = loggingHook;
        this.metricsHook = metricsHook;
        this.auditHook = auditHook;
        this.timingHook = timingHook;
    }

    /**
     * 同步调用 Agent.
     *
     * @param userInput 用户输入
     * @return Agent 响应消息
     */
    public Msg call(String userInput) {
        log.info("调用 Agent，输入: {}", truncate(userInput, 100));

        try {
            Msg response = agent.call(
                    Msg.builder()
                            .name("user")
                            .role(MsgRole.USER)
                            .content(TextBlock.builder().text(userInput).build())
                            .build()
            ).block();

            log.info("Agent 响应: {}", truncate(response.getTextContent(), 200));
            return response;

        } catch (Exception e) {
            log.error("Agent 调用失败: {}", e.getMessage(), e);
            throw new RuntimeException("Agent 调用失败", e);
        }
    }

    /**
     * 异步调用 Agent（返回 Mono）.
     *
     * @param userInput 用户输入
     * @return 包含响应消息的 Mono
     */
    public Mono<Msg> callAsync(String userInput) {
        log.info("异步调用 Agent，输入: {}", truncate(userInput, 100));

        return Mono.fromCallable(() -> {
                    Msg response = agent.call(
                            Msg.builder()
                                    .name("user")
                                    .role(MsgRole.USER)
                                    .content(TextBlock.builder().text(userInput).build())
                                    .build()
                    ).block();
                    log.info("异步调用完成: {}", truncate(response.getTextContent(), 200));
                    return response;
                })
                .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
                .doOnError(e -> log.error("异步 Agent 调用失败: {}", e.getMessage(), e));
    }

    /**
     * 获取性能指标.
     */
    public MetricsHook getMetrics() {
        return metricsHook;
    }

    /**
     * 获取工具审计记录.
     */
    public AuditHook getAuditHook() {
        return auditHook;
    }

    /**
     * 重置性能指标.
     */
    public void resetMetrics() {
        metricsHook.reset();
        log.info("性能指标已重置");
    }

    /**
     * 清空审计记录.
     */
    public void clearAuditRecords() {
        auditHook.clearRecords();
        auditHook.clearDatabaseRecords();
    }

    /**
     * 获取 Agent 名称.
     */
    public String getAgentName() {
        return agent.getName();
    }

    /**
     * 检查 Agent 是否健康.
     */
    public boolean isHealthy() {
        try {
            // 简单的健康检查：执行一个空调用
            agent.call(Msg.builder()
                    .name("user")
                    .role(MsgRole.USER)
                    .content(TextBlock.builder().text("health check").build())
                    .build()).block();
            return true;
        } catch (Exception e) {
            log.warn("Agent 健康检查失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取已注册的钩子列表信息.
     */
    public String getHookInfo() {
        return String.format("""
                Hook 链信息:
                - AuditHook: priority=%d, records=%d
                - TimingHook: priority=%d
                - LoggingHook: priority=%d
                - MetricsHook: priority=%d, totalCalls=%d, avgTime=%dms
                """,
                auditHook.priority(),
                auditHook.getRecordCount(),
                timingHook.priority(),
                loggingHook.priority(),
                metricsHook.priority(),
                metricsHook.getTotalCallCount(),
                metricsHook.getAverageCallTimeMs());
    }

    /**
     * 截断过长文本.
     */
    private String truncate(String text, int maxLength) {
        if (text == null) return "(null)";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }

    /**
     * 示例工具类 - 演示如何在 Hook 中观察工具调用.
     */
    public static class DemoTools {

        @Tool(name = "calculate", description = "Perform a mathematical calculation")
        public String calculate(
                @ToolParam(name = "expression", description = "Mathematical expression to evaluate")
                        String expression) {
            try {
                // 安全评估：仅支持基本数学运算
                double result = evaluateExpression(expression);
                return String.format("%.2f", result);
            } catch (Exception e) {
                return "Error: " + e.getMessage();
            }
        }

        @Tool(name = "get_weather", description = "Get weather for a city")
        public String getWeather(
                @ToolParam(name = "city", description = "City name")
                        String city) {
            // 模拟天气查询
            return String.format("Weather in %s: Sunny, 25°C", city);
        }

        @Tool(name = "search_info", description = "Search information online")
        public String searchInfo(
                @ToolParam(name = "query", description = "Search query")
                        String query) {
            // 模拟搜索
            return String.format("Search results for '%s': Found 100 results", query);
        }

        /**
         * 安全评估数学表达式（仅用于演示，生产环境请使用专业表达式求值库）.
         */
        private double evaluateExpression(String expr) {
            // 移除空格
            expr = expr.replaceAll("\\s+", "");
            // 使用简单的正则表达式验证安全性
            if (!expr.matches("^[0-9+\\-*/().]+$")) {
                throw new IllegalArgumentException("Invalid expression");
            }
            // 使用 ScriptEngine 进行计算
            try {
                return new javax.script.ScriptEngineManager()
                        .getEngineByName("JavaScript")
                        .eval(expr)
                        .toString()
                        .matches("-?\\d+(\\.\\d+)?")
                        ? Double.parseDouble(new javax.script.ScriptEngineManager()
                                .getEngineByName("JavaScript")
                                .eval(expr).toString())
                        : 0;
            } catch (Exception e) {
                throw new RuntimeException("Calculation error: " + e.getMessage());
            }
        }
    }
}