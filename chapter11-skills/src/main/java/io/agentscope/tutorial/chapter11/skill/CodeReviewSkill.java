package io.agentscope.tutorial.chapter11.skill;

import io.agentscope.core.tool.AgentTool;
import io.agentscope.core.tool.ToolCallParam;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * 代码审查技能
 *
 * <p>提供代码审查能力，包括：
 * <ul>
 *   <li>代码风格检查</li>
 *   <li>潜在 Bug 检测</li>
 *   <li>性能问题识别</li>
 *   <li>安全漏洞扫描</li>
 *   <li>最佳实践建议</li>
 * </ul>
 *
 * <p>工具说明：
 * <ul>
 *   <li>review_code - 审查代码质量</li>
 *   <li>check_security - 安全检查</li>
 *   <li>analyze_performance - 性能分析</li>
 *   <li>generate_fix_suggestions - 生成修复建议</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>{@code
 * // 审查代码
 * review_code("src/main/java/MyClass.java", ["style", "bugs", "best_practices"]);
 *
 * // 安全检查
 * check_security("src", ["sql_injection", "xss", "csrf"]);
 *
 * // 性能分析
 * analyze_performance("src/Service.java", {"metrics": ["time", "memory"]});
 *
 * // 生成修复建议
 * generate_fix_suggestions("issues.json", {"language": "java"});
 * }</pre>
 *
 * @author AgentScope Tutorial
 */
@Slf4j
public class CodeReviewSkill {

    /**
     * 代码审查
     *
     * <p>对代码进行全面审查，包括风格、潜在问题、最佳实践等。
     *
     * @param filePath 文件路径
     * @param checkTypes 检查类型（style, bugs, best_practices, documentation）
     * @param options 额外选项
     * @return 审查结果
     */
    @AgentTool(name = "review_code")
    public String reviewCode(
            @ToolCallParam(description = "要审查的代码文件路径") String filePath,
            @ToolCallParam(description = "检查类型列表: style, bugs, best_practices, documentation") List<String> checkTypes,
            @ToolCallParam(description = "额外选项，如忽略规则、特定检查项等", required = false) Map<String, Object> options) {

        log.info("Reviewing code: {}, checks: {}", filePath, checkTypes);

        StringBuilder result = new StringBuilder();
        result.append("# 代码审查报告\n\n");
        result.append("**文件**: ").append(filePath).append("\n\n");
        result.append("**检查类型**: ").append(String.join(", ", checkTypes)).append("\n\n");

        // 模拟检查结果
        for (String checkType : checkTypes) {
            result.append("## ").append(formatCheckType(checkType)).append("\n\n");
            result.append(performCheck(checkType));
            result.append("\n\n");
        }

        // 添加总体评分
        result.append("## 总体评分\n\n");
        result.append("- 代码质量: **").append(getRandomScore()).append("/100**\n");
        result.append("- 可维护性: **").append(getRandomScore()).append("/100**\n");
        result.append("- 安全性: **").append(getRandomScore()).append("/100**\n");
        result.append("- 性能: **").append(getRandomScore()).append("/100**\n");

        return result.toString();
    }

    /**
     * 安全检查
     *
     * @param sourcePath 源代码路径
     * @param securityChecks 安全检查类型（sql_injection, xss, csrf, hardcode, etc）
     * @return 安全检查结果
     */
    @AgentTool(name = "check_security")
    public String checkSecurity(
            @ToolCallParam(description = "源代码目录或文件路径") String sourcePath,
            @ToolCallParam(description = "安全检查类型: sql_injection, xss, csrf, hardcode, weak_crypto") List<String> securityChecks) {

        log.info("Security check for: {}, checks: {}", sourcePath, securityChecks);

        StringBuilder result = new StringBuilder();
        result.append("# 安全检查报告\n\n");
        result.append("**目标**: ").append(sourcePath).append("\n\n");
        result.append("**检查项**: ").append(String.join(", ", securityChecks)).append("\n\n");

        // 模拟安全检查结果
        int totalIssues = 0;
        for (String check : securityChecks) {
            result.append("### ").append(formatSecurityCheck(check)).append("\n\n");

            int issueCount = new Random().nextInt(5);
            totalIssues += issueCount;

            if (issueCount == 0) {
                result.append("- 状态: **通过**\n");
                result.append("- 说明: 未发现明显安全问题\n\n");
            } else {
                result.append("- 状态: **发现问题**\n");
                result.append("- 问题数: ").append(issueCount).append("\n\n");

                for (int i = 0; i < issueCount; i++) {
                    result.append("#### 问题 ").append(i + 1).append("\n");
                    result.append("- 类型: ").append(check).append("\n");
                    result.append("- 严重程度: ").append(getSeverity()).append("\n");
                    result.append("- 描述: ").append(getSecurityIssue(check)).append("\n");
                    result.append("- 建议: ").append(getSecurityFix(check)).append("\n\n");
                }
            }
        }

        // 总结
        result.append("## 总结\n\n");
        result.append("- 总检查项: ").append(securityChecks.size()).append("\n");
        result.append("- 发现问题: ").append(totalIssues).append("\n");
        result.append("- 安全评级: ").append(getSecurityRating(totalIssues)).append("\n");

        return result.toString();
    }

    /**
     * 性能分析
     *
     * @param filePath 要分析的文件路径
     * @param metrics 分析指标（time_complexity, space_complexity, resource_usage）
     * @param options 额外选项
     * @return 性能分析结果
     */
    @AgentTool(name = "analyze_performance")
    public String analyzePerformance(
            @ToolCallParam(description = "要分析的代码文件路径") String filePath,
            @ToolCallParam(description = "分析指标: time_complexity, space_complexity, resource_usage") List<String> metrics,
            @ToolCallParam(description = "额外选项，如分析深度、阈值等", required = false) Map<String, Object> options) {

        log.info("Performance analysis for: {}, metrics: {}", filePath, metrics);

        StringBuilder result = new StringBuilder();
        result.append("# 性能分析报告\n\n");
        result.append("**文件**: ").append(filePath).append("\n\n");
        result.append("**分析指标**: ").append(String.join(", ", metrics)).append("\n\n");

        for (String metric : metrics) {
            result.append("## ").append(formatMetric(metric)).append("\n\n");
            result.append(analyzeMetric(metric));
            result.append("\n\n");
        }

        // 性能建议
        result.append("## 优化建议\n\n");
        result.append("1. 避免在循环中进行重复计算\n");
        result.append("2. 使用适当的数据结构提高查询效率\n");
        result.append("3. 考虑使用缓存减少重复计算\n");
        result.append("4. 对于大数据集，考虑分批处理\n");

        return result.toString();
    }

    /**
     * 生成修复建议
     *
     * @param issuesJson 问题列表（JSON 格式）
     * @param language 编程语言
     * @return 修复建议
     */
    @AgentTool(name = "generate_fix_suggestions")
    public String generateFixSuggestions(
            @ToolCallParam(description = "问题列表，JSON 格式") String issuesJson,
            @ToolCallParam(description = "编程语言: java, python, javascript, etc") String language) {

        log.info("Generating fix suggestions for {} issues in {}", issuesJson, language);

        StringBuilder result = new StringBuilder();
        result.append("# 修复建议报告\n\n");
        result.append("**编程语言**: ").append(language).append("\n\n");

        // 模拟生成修复建议
        result.append("## 代码修复建议\n\n");

        result.append("### 1. 命名规范问题\n");
        result.append("**问题**: 变量命名不符合规范\n");
        result.append("**当前代码**:\n```").append(language).append("\n");
        result.append("int a = 10;\n");
        result.append("String n = \"test\";\n");
        result.append("```\n");
        result.append("**建议修改**:\n```").append(language).append("\n");
        result.append("int itemCount = 10;\n");
        result.append("String userName = \"test\";\n");
        result.append("```\n\n");

        result.append("### 2. 空指针检查\n");
        result.append("**问题**: 未进行空指针检查\n");
        result.append("**当前代码**:\n```").append(language).append("\n");
        result.append("String result = obj.getValue().toString();\n");
        result.append("```\n");
        result.append("**建议修改**:\n```").append(language).append("\n");
        result.append("String result = Optional.ofNullable(obj)\n");
        result.append("    .map(Object::getValue)\n");
        result.append("    .map(Object::toString)\n");
        result.append("    .orElse(\"\");\n");
        result.append("```\n\n");

        result.append("### 3. 资源管理\n");
        result.append("**问题**: 资源未正确关闭\n");
        result.append("**建议**: 使用 try-with-resources 或 finally 确保资源关闭\n\n");

        return result.toString();
    }

    // ==================== 辅助方法 ====================

    private String formatCheckType(String checkType) {
        return switch (checkType.toLowerCase()) {
            case "style" -> "代码风格检查";
            case "bugs" -> "潜在 Bug 检测";
            case "best_practices" -> "最佳实践检查";
            case "documentation" -> "文档检查";
            default -> checkType;
        };
    }

    private String performCheck(String checkType) {
        return switch (checkType.toLowerCase()) {
            case "style" -> "- 缩进: 正常\n- 命名: 发现 2 处不规范\n- 注释: 覆盖率 45%\n- 通过率: 85%";
            case "bugs" -> "- 空指针: 发现 1 处风险\n- 资源泄露: 未发现\n- 并发问题: 未发现\n- 通过率: 92%";
            case "best_practices" -> "- 异常处理: 可优化\n- 日志记录: 正常\n- 错误处理: 可优化\n- 通过率: 78%";
            case "documentation" -> "- JavaDoc: 覆盖率 60%\n- 注释清晰度: 良好\n- API 文档: 存在\n- 通过率: 75%";
            default -> "- 检查类型: " + checkType + "\n- 通过率: 80%";
        };
    }

    private String formatSecurityCheck(String check) {
        return switch (check.toLowerCase()) {
            case "sql_injection" -> "SQL 注入检查";
            case "xss" -> "XSS 跨站脚本检查";
            case "csrf" -> "CSRF 跨站请求伪造检查";
            case "hardcode" -> "硬编码敏感信息检查";
            case "weak_crypto" -> "弱加密算法检查";
            default -> check;
        };
    }

    private String getSeverity() {
        String[] severities = {"低", "中", "高", "严重"};
        return severities[new Random().nextInt(severities.length)];
    }

    private String getSecurityIssue(String check) {
        return switch (check.toLowerCase()) {
            case "sql_injection" -> "发现动态拼接 SQL 语句，存在 SQL 注入风险";
            case "xss" -> "用户输入未进行 HTML 转义，可能导致 XSS 攻击";
            case "hardcode" -> "代码中发现硬编码的 API 密钥或密码";
            default -> "存在潜在安全问题";
        };
    }

    private String getSecurityFix(String check) {
        return switch (check.toLowerCase()) {
            case "sql_injection" -> "使用参数化查询或 ORM 框架";
            case "xss" -> "对用户输入进行 HTML 转义";
            case "hardcode" -> "将敏感信息移至配置文件或环境变量";
            default -> "根据具体情况实施安全修复";
        };
    }

    private String getSecurityRating(int issues) {
        return switch {
            case 0 -> "优秀 (A)";
            case 1 -> "良好 (B)";
            case 2 -> "一般 (C)";
            case 3 -> "较差 (D)";
            default -> "危险 (F)";
        };
    }

    private String formatMetric(String metric) {
        return switch (metric.toLowerCase()) {
            case "time_complexity" -> "时间复杂度分析";
            case "space_complexity" -> "空间复杂度分析";
            case "resource_usage" -> "资源使用分析";
            default -> metric;
        };
    }

    private String analyzeMetric(String metric) {
        return switch (metric.toLowerCase()) {
            case "time_complexity" -> "- 平均复杂度: O(n log n)\n- 最坏情况: O(n^2)\n- 建议优化: 考虑使用更高效的算法";
            case "space_complexity" -> "- 平均空间: O(n)\n- 峰值空间: O(n^2)\n- 建议优化: 释放不需要的对象引用";
            case "resource_usage" -> "- CPU 使用: 中等\n- 内存占用: 合理\n- IO 操作: 可优化\n- 建议: 使用缓存减少 IO";
            default -> "- 指标: " + metric + "\n- 状态: 可接受";
        };
    }

    private String getRandomScore() {
        return String.valueOf(70 + new Random().nextInt(30));
    }
}