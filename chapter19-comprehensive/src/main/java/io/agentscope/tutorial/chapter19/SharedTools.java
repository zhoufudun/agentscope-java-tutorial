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
package io.agentscope.tutorial.chapter19;

import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 共享工具类 - 跨案例共用的通用工具集合
 *
 * <p>提供多个场景共用的工具，包括：
 * <ul>
 *   <li>时间查询 - 获取当前日期和时间</li>
 *   <li>计算器 - 数学表达式求值</li>
 *   <li>文本处理 - 大小写转换、首字母大写等</li>
 *   <li>随机选择 - 从列表中随机选择一个元素</li>
 * </ul>
 *
 * <h2>Tool 注解使用指南</h2>
 * <p>每个工具方法使用 @Tool 注解标记，包含：
 * <ul>
 *   <li><b>name</b> - 工具的唯一标识符，Agent 通过此名称调用工具</li>
 *   <li><b>description</b> - 工具的功能描述，帮助 Agent 理解何时使用此工具</li>
 * </ul>
 *
 * <h2>ToolParam 注解使用指南</h2>
 * <p>每个参数使用 @ToolParam 注解标记，包含：
 * <ul>
 *   <li><b>name</b> - 参数名称</li>
 *   <li><b>description</b> - 参数的说明，包括格式、取值范围等</li>
 *   <li><b>required</b> - 是否必填（默认 true）</li>
 * </ul>
 *
 * <h2>ToolResultBlock 返回值</h2>
 * <p>工具方法返回 ToolResultBlock，支持：
 * <ul>
 *   <li>{@code ToolResultBlock.text()} - 返回成功结果</li>
 *   <li>{@code ToolResultBlock.error()} - 返回错误信息</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 * @since 1.0
 */
@Component
public class SharedTools {

    /**
     * 获取当前时间工具
     *
     * <p><b>使用场景：</b>
     * <ul>
     *   <li>用户询问当前时间、日期、星期几</li>
     *   <li>需要时间戳的业务逻辑</li>
     *   <li>日程安排的时间参考</li>
     * </ul>
     *
     * @return 包含当前日期时间、星期的格式化文本
     */
    @Tool(name = "get_current_time", description = "获取当前日期和时间")
    public ToolResultBlock getCurrentTime() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.format.DateTimeFormatter formatter =
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        String result = String.format(
                "当前时间：%s\n" +
                "日期：%s\n" +
                "星期：%s",
                now.format(formatter),
                now.toLocalDate(),
                now.getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.CHINESE)
        );

        return ToolResultBlock.text(result);
    }

    /**
     * 数学计算工具
     *
     * <p><b>使用场景：</b>
     * <ul>
     *   <li>用户要求计算数学表达式</li>
     *   <li>订单金额计算</li>
     *   <li>数据分析中的简单计算</li>
     * </ul>
     *
     * <p><b>支持的运算：</b>
     * <ul>
     *   <li>四则运算：+ - * /</li>
     *   <li>取模：%</li>
     *   <li>幂运算：^</li>
     *   <li>括号：()</li>
     * </ul>
     *
     * <p><b>安全性：</b>使用自实现的表达式解析器，
     * 而非 JavaScript 引擎，避免代码注入风险。
     *
     * @param expression 数学表达式，如 "2+3*4"
     * @return 计算结果
     */
    @Tool(name = "calculate", description = "执行数学计算")
    public ToolResultBlock calculate(
            @ToolParam(name = "expression", description = "数学表达式（如 2+3*4）") String expression) {
        try {
            // 安全的数学计算（仅支持基本运算）
            String sanitized = expression.replaceAll("[^0-9+\\-*/().%^]", "");
            double result = evaluateExpression(sanitized);

            String resultStr = String.format(
                    "表达式：%s\n计算结果：%s",
                    expression,
                    result % 1 == 0 ? (long) result : result
            );

            return ToolResultBlock.text(resultStr);
        } catch (Exception e) {
            return ToolResultBlock.error("计算失败：" + e.getMessage());
        }
    }

    /**
     * 简单的表达式求值。
     *
     * <p>使用自实现的递归下降解析器，支持 {@code + - * / % ^}、括号与一元正负号，
     * 并正确处理运算符优先级（如 {@code 2+3*4 = 14}）。
     *
     * <p>注意：早期实现依赖 {@code ScriptEngineManager.getEngineByName("JavaScript")}
     * （Nashorn 引擎），而 Nashorn 已在 JDK 15+ 中被移除，会导致引擎为 {@code null} 并抛出异常，
     * 因此改为纯 Java 实现，避免对运行环境的隐式依赖。
     */
    private double evaluateExpression(String expression) {
        try {
            return new ExpressionParser(expression).parse();
        } catch (RuntimeException e) {
            throw new RuntimeException("无效的表达式");
        }
    }

    /**
     * 轻量级四则运算解析器（递归下降）。
     *
     * <p>文法（优先级由低到高）：
     * <pre>
     *   expression := term (('+' | '-') term)*
     *   term       := factor (('*' | '/' | '%') factor)*
     *   factor     := ('+' | '-') factor | base ('^' factor)?
     *   base       := number | '(' expression ')'
     * </pre>
     */
    private static final class ExpressionParser {

        private final String expr;
        private int pos = -1;
        private int ch;

        ExpressionParser(String expr) {
            this.expr = expr == null ? "" : expr;
        }

        double parse() {
            nextChar();
            double x = parseExpression();
            if (pos < expr.length()) {
                throw new IllegalArgumentException("非预期字符: " + (char) ch);
            }
            return x;
        }

        private void nextChar() {
            ch = (++pos < expr.length()) ? expr.charAt(pos) : -1;
        }

        private boolean eat(int charToEat) {
            while (ch == ' ') {
                nextChar();
            }
            if (ch == charToEat) {
                nextChar();
                return true;
            }
            return false;
        }

        private double parseExpression() {
            double x = parseTerm();
            for (; ; ) {
                if (eat('+')) {
                    x += parseTerm();
                } else if (eat('-')) {
                    x -= parseTerm();
                } else {
                    return x;
                }
            }
        }

        private double parseTerm() {
            double x = parseFactor();
            for (; ; ) {
                if (eat('*')) {
                    x *= parseFactor();
                } else if (eat('/')) {
                    x /= parseFactor();
                } else if (eat('%')) {
                    x %= parseFactor();
                } else {
                    return x;
                }
            }
        }

        private double parseFactor() {
            if (eat('+')) {
                return parseFactor();
            }
            if (eat('-')) {
                return -parseFactor();
            }

            double x;
            int startPos = pos;
            if (eat('(')) {
                x = parseExpression();
                if (!eat(')')) {
                    throw new IllegalArgumentException("缺少 ')'");
                }
            } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                while ((ch >= '0' && ch <= '9') || ch == '.') {
                    nextChar();
                }
                x = Double.parseDouble(expr.substring(startPos, pos));
            } else {
                throw new IllegalArgumentException("非预期字符: " + (char) ch);
            }

            if (eat('^')) {
                x = Math.pow(x, parseFactor());
            }
            return x;
        }
    }

    /**
     * 格式化文本
     */
    @Tool(name = "format_text", description = "格式化文本")
    public ToolResultBlock formatText(
            @ToolParam(name = "text", description = "要格式化的文本") String text,
            @ToolParam(name = "format", description = "格式类型（upper/lower/title/first）") String format) {
        String result = switch (format.toLowerCase()) {
            case "upper" -> text.toUpperCase();
            case "lower" -> text.toLowerCase();
            case "title" -> {
                StringBuilder sb = new StringBuilder();
                boolean nextUpper = true;
                for (char c : text.toCharArray()) {
                    if (Character.isWhitespace(c) || c == '.' || c == '!' || c == '?') {
                        nextUpper = true;
                        sb.append(c);
                    } else if (nextUpper) {
                        sb.append(Character.toUpperCase(c));
                        nextUpper = false;
                    } else {
                        sb.append(Character.toLowerCase(c));
                    }
                }
                yield sb.toString();
            }
            case "first" -> text.isEmpty() ? "" : text.substring(0, 1).toUpperCase() +
                    (text.length() > 1 ? text.substring(1).toLowerCase() : "");
            default -> text;
        };

        return ToolResultBlock.text(String.format("原文：%s\n格式：%s\n结果：%s", text, format, result));
    }

    /**
     * 随机选择
     */
    @Tool(name = "random_choice", description = "从列表中随机选择一个")
    public ToolResultBlock randomChoice(
            @ToolParam(name = "options", description = "选项列表") List<String> options) {
        if (options == null || options.isEmpty()) {
            return ToolResultBlock.error("选项列表不能为空");
        }

        int index = (int) (Math.random() * options.size());
        String selected = options.get(index);

        return ToolResultBlock.text(String.format("所有选项：%s\n随机选择：%s（索引：%d/%d）",
                String.join(", ", options),
                selected,
                index + 1,
                options.size()));
    }
}