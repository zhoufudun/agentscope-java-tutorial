package io.agentscope.tutorial.chapter05.tool;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;

/**
 * 计算器工具集
 *
 * <p>提供基础数学运算、科学计算和表达式求值功能。
 * 所有方法均支持参数验证，错误时返回友好的错误信息。
 *
 * <p>工具名称使用 snake_case 规范，便于模型理解。
 *
 * @author AgentScope Tutorial
 * @since 1.0
 */
public class CalculatorTool {

    // ==================== 基础运算 ====================

    /**
     * 加法运算
     *
     * @param a 第一个数
     * @param b 第二个数
     * @return 加法结果字符串
     */
    @Tool(
        name = "add",
        description = "计算两个数的加法。适用于简单的数值相加场景。"
    )
    public String add(
        @ToolParam(name = "a", description = "第一个加数", required = true) Double a,
        @ToolParam(name = "b", description = "第二个加数", required = true) Double b
    ) {
        validateNotNull(a, "a");
        validateNotNull(b, "b");
        return String.format("%.2f + %.2f = %.2f", a, b, a + b);
    }

    /**
     * 减法运算
     *
     * @param a 被减数
     * @param b 减数
     * @return 减法结果字符串
     */
    @Tool(
        name = "subtract",
        description = "计算两个数的减法"
    )
    public String subtract(
        @ToolParam(name = "a", description = "被减数", required = true) Double a,
        @ToolParam(name = "b", description = "减数", required = true) Double b
    ) {
        validateNotNull(a, "a");
        validateNotNull(b, "b");
        return String.format("%.2f - %.2f = %.2f", a, b, a - b);
    }

    /**
     * 乘法运算
     *
     * @param a 第一个因数
     * @param b 第二个因数
     * @return 乘法结果字符串
     */
    @Tool(
        name = "multiply",
        description = "计算两个数的乘法"
    )
    public String multiply(
        @ToolParam(name = "a", description = "第一个因数", required = true) Double a,
        @ToolParam(name = "b", description = "第二个因数", required = true) Double b
    ) {
        validateNotNull(a, "a");
        validateNotNull(b, "b");
        return String.format("%.2f * %.2f = %.2f", a, b, a * b);
    }

    /**
     * 除法运算
     *
     * @param a 被除数
     * @param b 除数（不能为零）
     * @return 除法结果字符串
     */
    @Tool(
        name = "divide",
        description = "计算两个数的除法。注意：除数不能为零"
    )
    public String divide(
        @ToolParam(name = "a", description = "被除数", required = true) Double a,
        @ToolParam(name = "b", description = "除数（不能为零）", required = true) Double b
    ) {
        validateNotNull(a, "a");
        validateNotNull(b, "b");
        if (b == 0) {
            return "Error: 除数不能为零";
        }
        return String.format("%.2f / %.2f = %.2f", a, b, a / b);
    }

    // ==================== 科学计算 ====================

    /**
     * 计算阶乘
     *
     * @param n 非负整数（最大支持 20! 以避免溢出）
     * @return 阶乘结果
     */
    @Tool(
        name = "factorial",
        description = "计算非负整数的阶乘。最大支持 20! 以避免整数溢出。"
    )
    public String factorial(
        @ToolParam(name = "n", description = "非负整数（0-20）", required = true) Integer n
    ) {
        validateNotNull(n, "n");

        if (n < 0) {
            return "Error: 阶乘不支持负数";
        }
        if (n > 20) {
            return "Error: 数字太大（最大支持 20!）";
        }

        long result = 1;
        for (int i = 2; i <= n; i++) {
            result *= i;
        }
        return String.format("%d! = %d", n, result);
    }

    /**
     * 判断质数
     *
     * @param n 待检测的正整数
     * @return 质数判断结果
     */
    @Tool(
        name = "is_prime",
        description = "判断一个正整数是否为质数。质数定义为大于1的自然数，除了1和它本身外不能被其他自然数整除。"
    )
    public String isPrime(
        @ToolParam(name = "n", description = "待检测的正整数（>= 2）", required = true) Integer n
    ) {
        validateNotNull(n, "n");

        if (n < 2) {
            return n + " 不是质数（质数必须 >= 2）";
        }

        for (int i = 2; i <= Math.sqrt(n); i++) {
            if (n % i == 0) {
                return n + " 不是质数（可以被 " + i + " 整除）";
            }
        }
        return n + " 是质数";
    }

    /**
     * 计算平方根
     *
     * @param x 被开方数（非负数）
     * @return 平方根结果
     */
    @Tool(
        name = "sqrt",
        description = "计算非负数的平方根"
    )
    public String sqrt(
        @ToolParam(name = "x", description = "被开方数（必须 >= 0）", required = true) Double x
    ) {
        validateNotNull(x, "x");

        if (x < 0) {
            return "Error: 平方根不支持负数";
        }
        return String.format("sqrt(%.2f) = %.4f", x, Math.sqrt(x));
    }

    /**
     * 计算幂运算
     *
     * @param base 底数
     * @param exponent 指数
     * @return 幂运算结果
     */
    @Tool(
        name = "power",
        description = "计算底数的指数次幂。如 2^10 = 1024"
    )
    public String power(
        @ToolParam(name = "base", description = "底数", required = true) Double base,
        @ToolParam(name = "exponent", description = "指数", required = true) Double exponent
    ) {
        validateNotNull(base, "base");
        validateNotNull(exponent, "exponent");

        double result = Math.pow(base, exponent);
        return String.format("%.2f ^ %.2f = %.4f", base, exponent, result);
    }

    // ==================== 统计运算 ====================

    /**
     * 计算平均值
     *
     * @param numbers 数字列表（JSON 数组格式，如 "[1, 2, 3, 4, 5]"）
     * @return 平均值
     */
    @Tool(
        name = "average",
        description = "计算一组数字的平均值。输入格式为 JSON 数组字符串，如 '[1, 2, 3, 4, 5]'"
    )
    public String average(
        @ToolParam(name = "numbers", description = "数字列表（JSON数组格式）", required = true) String numbers
    ) {
        try {
            // 解析 JSON 数组
            numbers = numbers.trim();
            if (numbers.startsWith("[")) {
                numbers = numbers.substring(1, numbers.length() - 1);
            }
            String[] parts = numbers.split(",");
            double sum = 0;
            int count = 0;
            for (String part : parts) {
                sum += Double.parseDouble(part.trim());
                count++;
            }
            if (count == 0) {
                return "Error: 数字列表为空";
            }
            return String.format("平均值 = %.2f（共 %d 个数字）", sum / count, count);
        } catch (Exception e) {
            return "Error: 无效的数字列表格式。请使用 JSON 数组格式，如 '[1, 2, 3, 4, 5]'";
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 验证参数是否为空
     */
    private void validateNotNull(Object value, String paramName) {
        if (value == null) {
            throw new IllegalArgumentException("参数 '" + paramName + "' 不能为空");
        }
    }
}