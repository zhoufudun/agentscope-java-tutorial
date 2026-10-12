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

package io.agentscope.tutorial.chapter09.tools;

import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

/**
 * A2A 演示的教程工具类
 *
 * <p>这些工具演示如何创建可以通过 A2A 协议被 Agent 调用的工具函数。
 *
 * <h2>可用工具</h2>
 * <ul>
 *   <li>{@link #getWeather(String)} - 获取城市天气信息</li>
 *   <li>{@link #calculate(String)} - 执行算术计算</li>
 *   <li>{@link #getCurrentTime()} - 获取当前日期和时间</li>
 * </ul>
 *
 * <h2>AgentScope 学习要点</h2>
 * <ul>
 *   <li><b>@Tool 注解</b>：标记一个方法为可被 Agent 调用的工具</li>
 *   <li><b>@ToolParam 注解</b>：定义工具参数的名称和描述</li>
 *   <li><b>ToolResultBlock</b>：工具返回结果的标准格式，
 *       支持文本结果和错误结果</li>
 *   <li><b>description</b>：工具描述帮助 LLM 判断何时使用该工具</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 */
public class TutorialTools {

    /** 用于生成模拟数据的随机数生成器 */
    private final Random random = new Random();

    /**
     * 获取城市的天气信息
     *
     * <p>此工具演示如何通过 A2A 协议进行工具调用。
     * 在实际应用中，这里会调用真实的天气 API。
     *
     * @param city 要查询天气的城市名称（如"北京"、"上海"）
     * @return 包含天气状况、温度和湿度的天气信息
     */
    @Tool(name = "get_weather", description = "获取指定城市的当前天气信息")
    public ToolResultBlock getWeather(
            @ToolParam(name = "city", description = "城市名称（如'北京'、'上海'）")
                    String city) {
        // 模拟天气数据用于演示
        String[] conditions = {"晴朗", "多云", "局部多云", "小雨", "阴天"};
        String condition = conditions[random.nextInt(conditions.length)];
        int temperature = random.nextInt(35) + 5; // 5-40 摄氏度
        int humidity = random.nextInt(60) + 30; // 30-90%

        String result = String.format(
                "%s 的天气：\n" +
                "- 天气状况：%s\n" +
                "- 温度：%d°C\n" +
                "- 湿度：%d%%",
                city, condition, temperature, humidity);

        // 返回文本类型的工具结果
        return ToolResultBlock.text(result);
    }

    /**
     * 执行简单的算术计算
     *
     * <p>此工具演示如何通过 A2A 协议进行算术工具调用。
     * 支持基本的四则运算和括号。
     *
     * @param expression 要计算的数学表达式（如 "2 + 3 * 4"、"(10 + 5) / 3"）
     * @return 计算结果
     */
    @Tool(name = "calculate", description = "执行简单的算术计算（支持 +、-、*、/）")
    public ToolResultBlock calculate(
            @ToolParam(
                    name = "expression",
                    description = "要计算的算术表达式（如 '2 + 3 * 4'）"
            )
                    String expression) {
        try {
            double result = evaluateExpression(expression);
            return ToolResultBlock.text("计算结果：" + formatNumber(result));
        } catch (Exception e) {
            // 返回错误类型的工具结果
            return ToolResultBlock.error("计算失败：" + e.getMessage());
        }
    }

    /**
     * 获取当前日期和时间
     *
     * <p>此工具演示如何实现一个无参数的工具。
     *
     * @return 格式化的当前日期和时间（"yyyy-MM-dd HH:mm:ss" 格式）
     */
    @Tool(name = "get_current_time", description = "获取当前日期和时间")
    public ToolResultBlock getCurrentTime() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return ToolResultBlock.text("当前时间：" + now.format(formatter));
    }

    /**
     * 简单的表达式求值器
     *
     * <p>支持基本算术运算：+、-、*、/
     * 遵循运算符优先级，支持括号。
     *
     * @param expression 要计算的算术表达式
     * @return 计算结果
     * @throws IllegalArgumentException 如果表达式包含无效值
     */
    private double evaluateExpression(String expression) {
        // 移除所有空白字符
        expression = expression.replaceAll("\\s+", "");

        // 处理加法和减法（优先级较低，从右向左查找）
        int lastPlusOrMinus = -1;
        int parenDepth = 0;
        for (int i = expression.length() - 1; i >= 0; i--) {
            char c = expression.charAt(i);
            if (c == ')') parenDepth++;
            else if (c == '(') parenDepth--;
            else if (parenDepth == 0 && (c == '+' || c == '-') && i > 0) {
                lastPlusOrMinus = i;
                break;
            }
        }

        if (lastPlusOrMinus > 0) {
            String left = expression.substring(0, lastPlusOrMinus);
            String right = expression.substring(lastPlusOrMinus + 1);
            char op = expression.charAt(lastPlusOrMinus);
            if (op == '+') {
                return evaluateExpression(left) + evaluateExpression(right);
            } else {
                return evaluateExpression(left) - evaluateExpression(right);
            }
        }

        // 处理乘法和除法
        int lastMultOrDiv = -1;
        parenDepth = 0;
        for (int i = expression.length() - 1; i >= 0; i--) {
            char c = expression.charAt(i);
            if (c == ')') parenDepth++;
            else if (c == '(') parenDepth--;
            else if (parenDepth == 0 && (c == '*' || c == '/')) {
                lastMultOrDiv = i;
                break;
            }
        }

        if (lastMultOrDiv > 0) {
            String left = expression.substring(0, lastMultOrDiv);
            String right = expression.substring(lastMultOrDiv + 1);
            char op = expression.charAt(lastMultOrDiv);
            if (op == '*') {
                return evaluateExpression(left) * evaluateExpression(right);
            } else {
                return evaluateExpression(left) / evaluateExpression(right);
            }
        }

        // 处理括号
        if (expression.startsWith("(") && expression.endsWith(")")) {
            return evaluateExpression(expression.substring(1, expression.length() - 1));
        }

        return parseNumber(expression);
    }

    /**
     * 解析数字字符串
     *
     * @param expression 数字字符串
     * @return 解析后的数值
     * @throws IllegalArgumentException 如果字符串不是有效数字
     */
    private double parseNumber(String expression) {
        try {
            return Double.parseDouble(expression);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "无效的数值：'" + expression + "'", e);
        }
    }

    /**
     * 格式化数字输出
     *
     * <p>如果是整数则不显示小数点，否则保留 4 位小数。
     *
     * @param value 要格式化的数值
     * @return 格式化后的字符串
     */
    private String formatNumber(double value) {
        if (value == (long) value) {
            return String.valueOf((long) value);
        }
        return String.format("%.4f", value);
    }
}