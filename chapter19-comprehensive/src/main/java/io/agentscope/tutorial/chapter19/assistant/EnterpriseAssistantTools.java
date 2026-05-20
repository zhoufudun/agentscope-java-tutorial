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
package io.agentscope.tutorial.chapter19.assistant;

import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import io.agentscope.tutorial.chapter19.assistant.knowledge.KnowledgeAgent;
import io.agentscope.tutorial.chapter19.assistant.report.ReportAgent;
import io.agentscope.tutorial.chapter19.assistant.schedule.ScheduleAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 企业助手工具类
 *
 * <p>提供企业助手 Agent 可调用的工具，包括：
 * <ul>
 *   <li>知识库搜索</li>
 *   <li>日程管理</li>
 *   <li>报表生成</li>
 *   <li>图表生成</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 * @since 1.0
 */
@Component
public class EnterpriseAssistantTools {

    private static final Logger logger = LoggerFactory.getLogger(EnterpriseAssistantTools.class);

    private final KnowledgeAgent knowledgeAgent;
    private final ScheduleAgent scheduleAgent;
    private final ReportAgent reportAgent;

    @Autowired
    public EnterpriseAssistantTools(
            KnowledgeAgent knowledgeAgent,
            ScheduleAgent scheduleAgent,
            ReportAgent reportAgent) {
        this.knowledgeAgent = knowledgeAgent;
        this.scheduleAgent = scheduleAgent;
        this.reportAgent = reportAgent;
    }

    /**
     * 搜索知识库
     */
    @Tool(name = "search_knowledge", description = "搜索企业知识库，回答用户关于公司制度、业务流程等问题")
    public ToolResultBlock searchKnowledge(
            @ToolParam(name = "query", description = "搜索关键词或问题") String query) {
        logger.info("搜索知识库: {}", query);
        try {
            String result = knowledgeAgent.search(query);
            return ToolResultBlock.text(result);
        } catch (Exception e) {
            logger.error("知识库搜索失败", e);
            return ToolResultBlock.error("知识库搜索失败：" + e.getMessage());
        }
    }

    /**
     * 获取 FAQ
     */
    @Tool(name = "get_faq", description = "获取常见问题 FAQ 列表")
    public ToolResultBlock getFaq() {
        logger.info("获取 FAQ");
        try {
            String result = knowledgeAgent.getFaq();
            return ToolResultBlock.text(result);
        } catch (Exception e) {
            logger.error("获取 FAQ 失败", e);
            return ToolResultBlock.error("获取 FAQ 失败：" + e.getMessage());
        }
    }

    /**
     * 创建日程
     */
    @Tool(name = "create_schedule", description = "创建新的日程安排")
    public ToolResultBlock createSchedule(
            @ToolParam(name = "title", description = "日程标题") String title,
            @ToolParam(name = "startTime", description = "开始时间（格式：yyyy-MM-dd HH:mm）") String startTime,
            @ToolParam(name = "endTime", description = "结束时间（格式：yyyy-MM-dd HH:mm）") String endTime,
            @ToolParam(name = "description", description = "日程描述（可选）") String description) {
        logger.info("创建日程: title={}, startTime={}, endTime={}", title, startTime, endTime);

        try {
            if (title == null || startTime == null || endTime == null) {
                return ToolResultBlock.error("缺少必要参数：title, startTime, endTime");
            }

            String result = scheduleAgent.createSchedule(title, description != null ? description : "", startTime, endTime);
            return ToolResultBlock.text(result);
        } catch (Exception e) {
            logger.error("创建日程失败", e);
            return ToolResultBlock.error("创建日程失败：" + e.getMessage());
        }
    }

    /**
     * 查询日程
     */
    @Tool(name = "query_schedules", description = "查询用户的日程安排")
    public ToolResultBlock querySchedules(
            @ToolParam(name = "date", description = "查询日期，格式为 yyyy-MM-dd（可选，不提供则返回近期所有日程）", required = false) String date) {
        logger.info("查询日程: {}", date);
        try {
            String result = scheduleAgent.querySchedules("current_user", date);
            return ToolResultBlock.text(result);
        } catch (Exception e) {
            logger.error("查询日程失败", e);
            return ToolResultBlock.error("查询日程失败：" + e.getMessage());
        }
    }

    /**
     * 生成报表
     */
    @Tool(name = "generate_report", description = "生成数据报表")
    public ToolResultBlock generateReport(
            @ToolParam(name = "type", description = "报表类型：sales（销售）/ user（用户）/ performance（绩效）") String type,
            @ToolParam(name = "dataRange", description = "数据范围：last_month（上月）/ last_week（上周）/ this_quarter（本季度）") String dataRange) {
        logger.info("生成报表: {} - {}", type, dataRange);
        try {
            String result = reportAgent.generateReport(type, dataRange);
            return ToolResultBlock.text(result);
        } catch (Exception e) {
            logger.error("生成报表失败", e);
            return ToolResultBlock.error("生成报表失败：" + e.getMessage());
        }
    }

    /**
     * 生成图表
     */
    @Tool(name = "generate_chart", description = "生成数据图表配置")
    public ToolResultBlock generateChart(
            @ToolParam(name = "chartType", description = "图表类型：bar（柱状图）/ line（折线图）/ pie（饼图）") String chartType,
            @ToolParam(name = "dataType", description = "数据类型：sales（销售）/ user（用户）", required = false) String dataType) {
        logger.info("生成图表: {} - {}", chartType, dataType);
        try {
            String result = reportAgent.generateChart(
                    chartType != null ? chartType : "bar",
                    dataType != null ? dataType : "sales");
            return ToolResultBlock.text(result);
        } catch (Exception e) {
            logger.error("生成图表失败", e);
            return ToolResultBlock.error("生成图表失败：" + e.getMessage());
        }
    }
}