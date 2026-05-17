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
package io.agentscope.tutorial.chapter19.assistant.report;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 报表生成 Agent 服务
 *
 * <p>负责：
 * <ul>
 *   <li>数据查询和分析</li>
 *   <li>报表生成</li>
 *   <li>图表生成</li>
 *   <li>历史报表查询</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 * @since 1.0
 */
@Service
public class ReportAgent {

    private static final Logger logger = LoggerFactory.getLogger(ReportAgent.class);

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // 模拟报表存储
    private final Map<String, Report> reportStorage = new ConcurrentHashMap<>();

    // 生成ID计数器
    private long idCounter = 1;

    public ReportAgent() {
        initializeSampleData();
    }

    /**
     * 初始化示例数据
     */
    private void initializeSampleData() {
        LocalDateTime now = LocalDateTime.now();

        Report report1 = new Report(
                idCounter++,
                "user001",
                "销售月报",
                "sales",
                "本月销售额 100 万元，环比增长 15%。\n\n" +
                "【销售概况】\n" +
                "- 总订单数：1,234 单\n" +
                "- 总销售额：100 万元\n" +
                "- 客单价：810 元\n\n" +
                "【热销产品】\n" +
                "1. 珍珠奶茶 - 500 单\n" +
                "2. 波霸奶茶 - 300 单\n" +
                "3. 芋泥波波 - 200 单\n\n" +
                "【渠道分布】\n" +
                "- 线上：60%\n" +
                "- 线下：40%",
                "last_month",
                now.minusDays(5)
        );

        Report report2 = new Report(
                idCounter++,
                "user001",
                "用户增长周报",
                "user",
                "本周新增用户 500 人，环比增长 10%。\n\n" +
                "【用户概况】\n" +
                "- 新增用户：500 人\n" +
                "- 活跃用户：2,000 人\n" +
                "- 留存率：75%\n\n" +
                "【用户来源】\n" +
                "- 自然流量：40%\n" +
                "- 推广引流：35%\n" +
                "- 老带新：25%",
                "last_week",
                now.minusDays(2)
        );

        reportStorage.put("sales_last_month", report1);
        reportStorage.put("user_last_week", report2);
    }

    /**
     * 生成报表
     *
     * @param reportType 报表类型（sales/user/performance）
     * @param dataRange 数据范围（last_month/last_week/this_quarter）
     * @return 报表内容
     */
    public String generateReport(String reportType, String dataRange) {
        logger.info("生成报表: {} - {}", reportType, dataRange);

        String cacheKey = reportType + "_" + dataRange;
        if (reportStorage.containsKey(cacheKey)) {
            Report cached = reportStorage.get(cacheKey);
            return formatReport(cached);
        }

        // 生成新报表
        String content = switch (reportType.toLowerCase()) {
            case "sales" -> generateSalesReport(dataRange);
            case "user" -> generateUserReport(dataRange);
            case "performance" -> generatePerformanceReport(dataRange);
            default -> "不支持的报表类型：" + reportType;
        };

        String title = reportType.toUpperCase() + " Report - " + dataRange;
        Report report = new Report(
                idCounter++,
                "current_user",
                title,
                reportType,
                content,
                dataRange,
                LocalDateTime.now()
        );

        reportStorage.put(cacheKey, report);

        return formatReport(report);
    }

    /**
     * 生成销售报表
     */
    private String generateSalesReport(String dataRange) {
        return """
                ===== 销售报表 =====

                【销售概况】
                - 总订单数：1,234 单
                - 总销售额：100 万元
                - 客单价：810 元
                - 环比增长：15%

                【产品销售排行】
                1. 珍珠奶茶 - 500 单 (36%)
                2. 波霸奶茶 - 300 单 (22%)
                3. 芋泥波波 - 200 单 (14%)
                4. 椰果奶茶 - 150 单 (11%)
                5. 芝芝莓莓 - 84 单 (6%)

                【渠道分布】
                - 线上渠道：60%
                  - 小程序：45%
                  - 外卖平台：15%
                - 线下门店：40%

                【区域表现】
                - 华南区：35万元
                - 华东区：30万元
                - 华北区：20万元
                - 其他：15万元

                【趋势分析】
                本月销售呈上升趋势，周末销量明显高于工作日。
                下午茶时段（14:00-16:00）为销售高峰。
                """;
    }

    /**
     * 生成用户报表
     */
    private String generateUserReport(String dataRange) {
        return """
                ===== 用户报表 =====

                【用户概况】
                - 总用户数：10,000 人
                - 本期新增：500 人
                - 活跃用户：2,000 人
                - 留存率：75%

                【用户画像】
                - 18-25岁：45%
                - 26-35岁：35%
                - 36-45岁：15%
                - 45岁以上：5%

                【用户来源】
                - 自然流量：40%
                - 推广引流：35%
                - 老带新：25%

                【行为分析】
                - 平均月购买次数：3.5 次
                - 平均客单价：25 元
                - 复购率：60%

                【会员等级分布】
                - 普通会员：60%
                - 银卡会员：25%
                - 金卡会员：12%
                - 钻石会员：3%
                """;
    }

    /**
     * 生成绩效报表
     */
    private String generatePerformanceReport(String dataRange) {
        return """
                ===== 绩效报表 =====

                【团队绩效】
                - 总销售额：100 万元
                - 目标完成率：105%
                - 人均产值：10 万元

                【员工排行】
                1. 张三 - 15万元 (150%)
                2. 李四 - 12万元 (120%)
                3. 王五 - 10万元 (100%)

                【KPI 完成情况】
                - 销售额：105%
                - 客户满意度：98%
                - 订单准时率：99%

                【改进建议】
                1. 加强下午茶时段的人员配置
                2. 推广会员积分制度提高复购率
                3. 优化线上点餐体验
                """;
    }

    /**
     * 生成图表配置
     *
     * @param chartType 图表类型
     * @param dataType 数据类型
     * @return 图表配置（JSON格式）
     */
    public String generateChart(String chartType, String dataType) {
        logger.info("生成图表: {} - {}", chartType, dataType);

        return switch (chartType.toLowerCase()) {
            case "bar" -> generateBarChartConfig(dataType);
            case "line" -> generateLineChartConfig(dataType);
            case "pie" -> generatePieChartConfig(dataType);
            default -> "{\"error\": \"不支持的图表类型: " + chartType + "\"}";
        };
    }

    /**
     * 生成柱状图配置
     */
    private String generateBarChartConfig(String dataType) {
        return """
                {
                    "chartType": "bar",
                    "title": "销售额统计",
                    "xAxis": ["周一", "周二", "周三", "周四", "周五", "周六", "周日"],
                    "series": [{
                        "name": "销售额(万元)",
                        "data": [12, 15, 14, 18, 20, 25, 22]
                    }],
                    "options": {
                        "color": "#5470c6",
                        "showLabel": true
                    }
                }""";
    }

    /**
     * 生成折线图配置
     */
    private String generateLineChartConfig(String dataType) {
        return """
                {
                    "chartType": "line",
                    "title": "用户增长趋势",
                    "xAxis": ["1月", "2月", "3月", "4月", "5月", "6月"],
                    "series": [
                        {
                            "name": "新增用户",
                            "data": [200, 250, 300, 350, 400, 500]
                        },
                        {
                            "name": "活跃用户",
                            "data": [1000, 1200, 1500, 1700, 1900, 2000]
                        }
                    ],
                    "options": {
                        "smooth": true,
                        "showArea": true
                    }
                }""";
    }

    /**
     * 生成饼图配置
     */
    private String generatePieChartConfig(String dataType) {
        return """
                {
                    "chartType": "pie",
                    "title": "产品销售占比",
                    "data": [
                        {"name": "珍珠奶茶", "value": 36},
                        {"name": "波霸奶茶", "value": 22},
                        {"name": "芋泥波波", "value": 14},
                        {"name": "椰果奶茶", "value": 11},
                        {"name": "芝芝莓莓", "value": 6},
                        {"name": "其他", "value": 11}
                    ],
                    "options": {
                        "radius": ["40%", "70%"],
                        "showLabel": true
                    }
                }""";
    }

    /**
     * 查询历史报表
     *
     * @param userId 用户ID
     * @param limit 返回数量限制
     * @return 历史报表列表
     */
    public String queryHistoryReports(String userId, int limit) {
        logger.info("查询历史报表: {} limit {}", userId, limit);

        List<Report> reports = reportStorage.values().stream()
                .filter(r -> r.userId.equals(userId))
                .sorted(Comparator.comparing(r -> r.createdAt).reversed())
                .limit(limit)
                .toList();

        if (reports.isEmpty()) {
            return "暂无历史报表。";
        }

        StringBuilder result = new StringBuilder();
        result.append("===== 历史报表 =====\n\n");

        for (Report report : reports) {
            result.append(String.format("- %s (%s)\n",
                    report.title,
                    report.createdAt.format(FORMATTER)));
        }

        return result.toString();
    }

    /**
     * 格式化报表输出
     */
    private String formatReport(Report report) {
        StringBuilder sb = new StringBuilder();
        sb.append("=".repeat(50)).append("\n");
        sb.append(report.title).append("\n");
        sb.append("=".repeat(50)).append("\n\n");
        sb.append(report.content).append("\n\n");
        sb.append("-".repeat(50)).append("\n");
        sb.append(String.format("生成时间：%s\n", report.createdAt.format(FORMATTER)));
        sb.append(String.format("数据范围：%s", report.dataRange));
        return sb.toString();
    }

    /**
     * 报表数据类
     */
    private static class Report {
        long id;
        String userId;
        String title;
        String type;
        String content;
        String dataRange;
        LocalDateTime createdAt;

        Report(long id, String userId, String title, String type,
               String content, String dataRange, LocalDateTime createdAt) {
            this.id = id;
            this.userId = userId;
            this.title = title;
            this.type = type;
            this.content = content;
            this.dataRange = dataRange;
            this.createdAt = createdAt;
        }
    }
}