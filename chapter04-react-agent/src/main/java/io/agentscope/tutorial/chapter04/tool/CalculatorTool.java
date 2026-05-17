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
package io.agentscope.tutorial.chapter04.tool;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;
import java.util.Random;

/**
 * 计算器工具集
 *
 * 提供各种数学计算和单位换算功能：
 * - 数学表达式计算
 * - 单位换算（长度、重量、温度等）
 *
 * @author AgentScope Tutorial
 */
public class CalculatorTool {

    private static final Random RANDOM = new Random();

    /**
     * 执行数学计算
     *
     * @param expression 数学表达式（如：2+3*5, sqrt(16), pow(2,3)）
     * @return 计算结果
     */
    @Tool(
            name = "calculate",
            description = "执行数学计算。支持的运算：加(+)、减(-)、乘(*)、除(/)、幂(^)、" +
                    "平方根(sqrt)、绝对值(abs)、三角函数(sin/cos/tan)等。"
    )
    public Mono<String> calculate(
            @ToolParam(name = "expression", description = "数学表达式")
            String expression) {

        return Mono.fromCallable(() -> {
            simulateDelay(100, 300);

            try {
                double result = evaluateExpression(expression);
                return String.format("计算结果: %s = %.6f", expression, result);
            } catch (Exception e) {
                return String.format("计算错误: %s", e.getMessage());
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 单位换算工具
     *
     * @param value 要转换的数值
     * @param fromUnit 源单位
     * @param toUnit 目标单位
     * @return 转换结果
     */
    @Tool(
            name = "convert_unit",
            description = "进行单位换算。支持长度、重量、温度、面积、体积等单位转换。"
    )
    public Mono<String> convertUnit(
            @ToolParam(name = "value", description = "要转换的数值")
            double value,
            @ToolParam(name = "from_unit", description = "源单位")
            String fromUnit,
            @ToolParam(name = "to_unit", description = "目标单位")
            String toUnit) {

        return Mono.fromCallable(() -> {
            simulateDelay(200, 500);

            // 长度单位转换（转换为米的系数）
            Map<String, Double> lengthToMeter = Map.ofEntries(
                    Map.entry("km", 1000.0),
                    Map.entry("m", 1.0),
                    Map.entry("cm", 0.01),
                    Map.entry("mm", 0.001),
                    Map.entry("mi", 1609.344),
                    Map.entry("ft", 0.3048),
                    Map.entry("in", 0.0254),
                    Map.entry("yd", 0.9144)
            );

            // 重量单位转换（转换为千克的系数）
            Map<String, Double> weightToKg = Map.ofEntries(
                    Map.entry("kg", 1.0),
                    Map.entry("g", 0.001),
                    Map.entry("mg", 0.000001),
                    Map.entry("t", 1000.0),
                    Map.entry("lb", 0.453592),
                    Map.entry("oz", 0.0283495)
            );

            // 温度转换
            if (fromUnit.equalsIgnoreCase(toUnit)) {
                return String.format("%.4f %s = %.4f %s", value, fromUnit, value, toUnit);
            }

            // 尝试长度转换
            if (lengthToMeter.containsKey(fromUnit.toLowerCase())
                    && lengthToMeter.containsKey(toUnit.toLowerCase())) {
                double result = value * lengthToMeter.get(fromUnit.toLowerCase())
                        / lengthToMeter.get(toUnit.toLowerCase());
                return String.format("%.6f %s = %.6f %s", value, fromUnit, result, toUnit);
            }

            // 尝试重量转换
            if (weightToKg.containsKey(fromUnit.toLowerCase())
                    && weightToKg.containsKey(toUnit.toLowerCase())) {
                double result = value * weightToKg.get(fromUnit.toLowerCase())
                        / weightToKg.get(toUnit.toLowerCase());
                return String.format("%.6f %s = %.6f %s", value, fromUnit, result, toUnit);
            }

            // 温度转换
            double celsius = toCelsius(value, fromUnit);
            if (celsius != Double.MIN_VALUE) {
                double result = fromCelsius(celsius, toUnit);
                return String.format("%.4f %s = %.4f %s", value, fromUnit, result, toUnit);
            }

            return String.format("错误: 不支持的单位转换 '%s' -> '%s'", fromUnit, toUnit);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    // ========== 私有辅助方法 ==========

    private double toCelsius(double value, String unit) {
        return switch (unit.toLowerCase()) {
            case "c", "celsius" -> value;
            case "f", "fahrenheit" -> (value - 32) * 5 / 9;
            case "k", "kelvin" -> value - 273.15;
            default -> Double.MIN_VALUE;
        };
    }

    private double fromCelsius(double celsius, String unit) {
        return switch (unit.toLowerCase()) {
            case "c", "celsius" -> celsius;
            case "f", "fahrenheit" -> celsius * 9 / 5 + 32;
            case "k", "kelvin" -> celsius + 273.15;
            default -> Double.MIN_VALUE;
        };
    }

    private double evaluateExpression(String expr) {
        // 简单的表达式解析
        String expression = expr.replaceAll("\\s+", "").toLowerCase();

        try {
            // 尝试直接解析为数字
            if (expression.matches("-?\\d+(\\.\\d+)?")) {
                return Double.parseDouble(expression);
            }

            // 处理基本运算
            if (expression.contains("+")) {
                String[] parts = expression.split("\\+");
                return evaluateExpression(parts[0]) + evaluateExpression(parts[1]);
            }
            if (expression.contains("-") && expression.indexOf("-") > 0) {
                String[] parts = expression.split("-", 2);
                return evaluateExpression(parts[0]) - evaluateExpression(parts[1]);
            }
            if (expression.contains("*")) {
                String[] parts = expression.split("\\*");
                double result = 1;
                for (String part : parts) {
                    result *= evaluateExpression(part);
                }
                return result;
            }
            if (expression.contains("/")) {
                String[] parts = expression.split("/");
                double result = evaluateExpression(parts[0]);
                for (int i = 1; i < parts.length; i++) {
                    result /= evaluateExpression(parts[i]);
                }
                return result;
            }

            // 处理简单函数
            if (expression.startsWith("sqrt(") && expression.endsWith(")")) {
                double val = evaluateExpression(expression.substring(5, expression.length() - 1));
                return Math.sqrt(val);
            }
            if (expression.startsWith("abs(") && expression.endsWith(")")) {
                double val = evaluateExpression(expression.substring(4, expression.length() - 1));
                return Math.abs(val);
            }
            if (expression.startsWith("sin(") && expression.endsWith(")")) {
                double val = evaluateExpression(expression.substring(4, expression.length() - 1));
                return Math.sin(val);
            }
            if (expression.startsWith("cos(") && expression.endsWith(")")) {
                double val = evaluateExpression(expression.substring(4, expression.length() - 1));
                return Math.cos(val);
            }
            if (expression.startsWith("tan(") && expression.endsWith(")")) {
                double val = evaluateExpression(expression.substring(4, expression.length() - 1));
                return Math.tan(val);
            }

        } catch (Exception e) {
            throw new RuntimeException("无法解析表达式: " + expr);
        }

        throw new RuntimeException("无法解析表达式: " + expr);
    }

    private void simulateDelay(int minMs, int maxMs) {
        try {
            Thread.sleep(minMs + RANDOM.nextInt(maxMs - minMs));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}