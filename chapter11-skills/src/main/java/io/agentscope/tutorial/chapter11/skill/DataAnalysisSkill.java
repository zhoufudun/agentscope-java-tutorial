package io.agentscope.tutorial.chapter11.skill;

import io.agentscope.core.tool.AgentTool;
import io.agentscope.core.tool.ToolCallParam;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * 数据分析技能
 *
 * <p>提供数据分析能力，包括：
 * <ul>
 *   <li>数据加载和清洗</li>
 *   <li>统计计算</li>
 *   <li>数据可视化生成</li>
 *   <li>分析报告创建</li>
 * </ul>
 *
 * <p>工具说明：
 * <ul>
 *   <li>analyze_data - 执行数据分析任务</li>
 *   <li>generate_report - 生成数据分析报告</li>
 *   <li>create_visualization - 创建数据可视化</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>{@code
 * // 分析数据
 * analyze_data("sales.csv", ["sum", "avg", "group_by"]);
 *
 * // 生成报告
 * generate_report("月度销售分析", "2024-01");
 *
 * // 创建可视化
 * create_visualization("sales", "bar", {"title": "销售趋势"});
 * }</pre>
 *
 * @author AgentScope Tutorial
 */
@Slf4j
public class DataAnalysisSkill {

    /**
     * 分析数据
     *
     * <p>对指定数据执行统计分析操作。
     *
     * @param dataSource 数据源（文件路径或数据内容）
     * @param operations 操作列表（sum, avg, count, min, max, group_by）
     * @param options 额外选项（可选）
     * @return 分析结果
     */
    @AgentTool(name = "analyze_data")
    public String analyzeData(
            @ToolCallParam(description = "数据源，可以是文件路径或 CSV 内容") String dataSource,
            @ToolCallParam(description = "要执行的统计操作，如 sum,avg,count,min,max,group_by") List<String> operations,
            @ToolCallParam(description = "额外选项，如分组字段、过滤条件等", required = false) Map<String, Object> options) {

        log.info("Analyzing data from: {}, operations: {}", dataSource, operations);

        StringBuilder result = new StringBuilder();
        result.append("# 数据分析结果\n\n");
        result.append("**数据源**: ").append(dataSource).append("\n\n");
        result.append("**执行操作**: ").append(String.join(", ", operations)).append("\n\n");

        // 模拟分析结果
        for (String op : operations) {
            result.append("## ").append(op.toUpperCase()).append("\n");
            switch (op.toLowerCase()) {
                case "sum" -> result.append("- 总和: 1234567.89\n");
                case "avg" -> result.append("- 平均值: 5432.10\n");
                case "count" -> result.append("- 记录数: 228\n");
                case "min" -> result.append("- 最小值: 100.00\n");
                case "max" -> result.append("- 最大值: 99999.99\n");
                case "group_by" -> result.append("- 分组数: 12\n");
                default -> result.append("- 未知操作: ").append(op).append("\n");
            }
            result.append("\n");
        }

        // 添加选项信息
        if (options != null && !options.isEmpty()) {
            result.append("**额外选项**:\n");
            options.forEach((k, v) -> result.append("- ").append(k).append(": ").append(v).append("\n"));
        }

        return result.toString();
    }

    /**
     * 生成数据分析报告
     *
     * @param title 报告标题
     * @param period 统计周期
     * @param dataSummary 数据摘要
     * @return 报告内容
     */
    @AgentTool(name = "generate_report")
    public String generateReport(
            @ToolCallParam(description = "报告标题") String title,
            @ToolCallParam(description = "统计周期，如 2024-01") String period,
            @ToolCallParam(description = "数据摘要内容", required = false) String dataSummary) {

        log.info("Generating report: {} for period: {}", title, period);

        StringBuilder report = new StringBuilder();
        report.append("# ").append(title).append("\n\n");
        report.append("**统计周期**: ").append(period).append("\n\n");
        report.append("**生成时间**: ").append(java.time.LocalDateTime.now()).append("\n\n");

        if (dataSummary != null && !dataSummary.isBlank()) {
            report.append("## 数据摘要\n\n").append(dataSummary).append("\n\n");
        }

        report.append("## 分析结论\n\n");
        report.append("1. 数据整体呈现").append(getTrend()).append("趋势\n");
        report.append("2. 关键指标表现").append(getPerformance()).append("\n");
        report.append("3. 建议关注点：").append(getRecommendations()).append("\n");

        return report.toString();
    }

    /**
     * 创建数据可视化
     *
     * @param chartType 图表类型（bar, line, pie, scatter）
     * @param dataSource 数据源
     * @param config 可视化配置
     * @return 可视化信息
     */
    @AgentTool(name = "create_visualization")
    public String createVisualization(
            @ToolCallParam(description = "图表类型: bar, line, pie, scatter") String chartType,
            @ToolCallParam(description = "数据源标识") String dataSource,
            @ToolCallParam(description = "可视化配置，如标题、颜色等") Map<String, Object> config) {

        log.info("Creating {} visualization for: {}", chartType, dataSource);

        StringBuilder info = new StringBuilder();
        info.append("# 可视化创建成功\n\n");
        info.append("**图表类型**: ").append(chartType).append("\n");
        info.append("**数据源**: ").append(dataSource).append("\n\n");

        if (config != null) {
            info.append("**配置信息**:\n");
            config.forEach((k, v) -> info.append("- ").append(k).append(": ").append(v).append("\n"));
        }

        info.append("\n**输出文件**: ");
        String fileName = dataSource + "_" + chartType + "_" + System.currentTimeMillis() + ".png";
        info.append("output/").append(fileName).append("\n");

        return info.toString();
    }

    private String getTrend() {
        return "增长";
    }

    private String getPerformance() {
        return "良好";
    }

    private String getRecommendations() {
        return "优化资源配置、提高运营效率";
    }
}