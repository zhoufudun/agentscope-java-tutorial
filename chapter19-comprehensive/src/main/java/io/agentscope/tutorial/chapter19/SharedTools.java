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

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolResult;
import io.agentscope.core.tool.ToolSpec;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 共享工具类
 *
 * <p>提供多个场景共用的工具，包括：
 * <ul>
 *   <li>时间查询</li>
 *   <li>计算器</li>
 *   <li>文本处理</li>
 *   <li>随机选择</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 * @since 1.0
 */
@Component
public class SharedTools {

    /**
     * 获取工具规格列表
     */
    public static List<ToolSpec> getToolSpecs() {
        return List.of(
                ToolSpec.builder()
                        .name("get_current_time")
                        .description("获取当前日期和时间")
                        .paramSpec("""
                                {
                                    "type": "object",
                                    "properties": {}
                                }
                                """)
                        .build(),

                ToolSpec.builder()
                        .name("calculate")
                        .description("执行数学计算")
                        .paramSpec("""
                                {
                                    "type": "object",
                                    "properties": {
                                        "expression": {
                                            "type": "string",
                                            "description": "数学表达式（如 2+3*4）"
                                        }
                                    },
                                    "required": ["expression"]
                                }
                                """)
                        .build(),

                ToolSpec.builder()
                        .name("format_text")
                        .description("格式化文本")
                        .paramSpec("""
                                {
                                    "type": "object",
                                    "properties": {
                                        "text": {
                                            "type": "string",
                                            "description": "要格式化的文本"
                                        },
                                        "format": {
                                            "type": "string",
                                            "description": "格式类型（upper/lower/title/first）"
                                        }
                                    },
                                    "required": ["text", "format"]
                                }
                                """)
                        .build(),

                ToolSpec.builder()
                        .name("random_choice")
                        .description("从列表中随机选择一个")
                        .paramSpec("""
                                {
                                    "type": "object",
                                    "properties": {
                                        "options": {
                                            "type": "array",
                                            "items": {"type": "string"},
                                            "description": "选项列表"
                                        }
                                    },
                                    "required": ["options"]
                                }
                                """)
                        .build()
        );
    }

    /**
     * 获取当前时间
     */
    public ToolResult getCurrentTime() {
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

        return ToolResult.success("当前时间获取成功", result);
    }

    /**
     * 数学计算
     */
    public ToolResult calculate(String expression) {
        try {
            // 安全的数学计算（仅支持基本运算）
            String sanitized = expression.replaceAll("[^0-9+\\-*/().%^]", "");
            double result = evaluateExpression(sanitized);

            String resultStr = String.format(
                    "表达式：%s\n计算结果：%s",
                    expression,
                    result % 1 == 0 ? (long) result : result
            );

            return ToolResult.success("计算完成", resultStr);
        } catch (Exception e) {
            return ToolResult.failure("计算失败：" + e.getMessage());
        }
    }

    /**
     * 简单的表达式求值
     */
    private double evaluateExpression(String expression) {
        // 使用 ScriptEngine 进行安全的计算
        try {
            javax.script.ScriptEngineManager m = new javax.script.ScriptEngineManager();
            javax.script.ScriptEngine engine = m.getEngineByName("JavaScript");
            Object result = engine.eval(expression);
            return Double.parseDouble(result.toString());
        } catch (Exception e) {
            throw new RuntimeException("无效的表达式");
        }
    }

    /**
     * 格式化文本
     */
    public ToolResult formatText(String text, String format) {
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

        return ToolResult.success("格式化完成",
                String.format("原文：%s\n格式：%s\n结果：%s", text, format, result));
    }

    /**
     * 随机选择
     */
    public ToolResult randomChoice(List<String> options) {
        if (options == null || options.isEmpty()) {
            return ToolResult.failure("选项列表不能为空");
        }

        int index = (int) (Math.random() * options.size());
        String selected = options.get(index);

        return ToolResult.success("随机选择完成",
                String.format("所有选项：%s\n随机选择：%s（索引：%d/%d）",
                        String.join(", ", options),
                        selected,
                        index + 1,
                        options.size()));
    }
}