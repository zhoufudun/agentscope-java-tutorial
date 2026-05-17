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

import io.agentscope.core.tool.ToolResult;
import io.agentscope.core.tool.ToolSpec;
import io.agentscope.tutorial.chapter19.assistant.knowledge.KnowledgeAgent;
import io.agentscope.tutorial.chapter19.assistant.report.ReportAgent;
import io.agentscope.tutorial.chapter19.assistant.schedule.ScheduleAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.自动注入;
import org.springframework.stereotype.Component;

import java.util.List;
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
     * 获取工具规格列表
     */
    public static List<ToolSpec> getToolSpecs() {
        return List.of(
                ToolSpec.builder()
                        .name("search_knowledge")
                        .description("搜索企业知识库，回答用户关于公司制度、业务流程等问题")
                        .paramSpec("""
                                {
                                    "type": "object",
                                    "properties": {
                                        "query": {
                                            "type": "string",
                                            "description": "搜索关键词或问题"
                                        }
                                    },
                                    "required": ["query"]
                                }
                                """)
                        .build(),

                ToolSpec.builder()
                        .name("get_faq")
                        .description("获取常见问题 FAQ 列表")
                        .paramSpec("""
                                {
                                    "type": "object",
                                    "properties": {}
                                }
                                """)
                        .build(),

                ToolSpec.builder()
                        .name("create_schedule")
                        .description("创建新的日程安排")
                        .paramSpec("""
                                {
                                    "type": "object",
                                    "properties": {
                                        "title": {
                                            "type": "string",
                                            "description": "日程标题"
                                        },
                                        "description": {
                                            "type": "string",
                                            "description": "日程描述（可选）"
                                        },
                                        "startTime": {
                                            "type": "string",
                                            "description": "开始时间（格式：yyyy-MM-dd HH:mm）"
                                        },
                                        "endTime": {
                                            "type": "string",
                                            "description": "结束时间（格式：yyyy-MM-dd HH:mm）"
                                        }
                                    },
                                    "required": ["title", "startTime", "endTime"]
                                }
                                """)
                        .build(),

                ToolSpec.builder()
                        .name("query_schedules")
                        .description("查询用户的日程安排")
                        .paramSpec("""
                                {
                                    "type": "object",
                                    "properties": {
                                        "date": {
                                            "type": "string",
                                            "description": "查询日期，格式为 yyyy-MM-dd（可选，不提供则返回近期所有日程）"
                                        }
                                    }
                                }
                                """)
                        .build(),

                ToolSpec.builder()
                        .name("generate_report")
                        .description("生成数据报表")
                        .paramSpec("""
                                {
                                    "type": "object",
                                    "properties": {
                                        "type": {
                                            "type": "string",
                                            "description": "报表类型：sales（销售）/ user（用户）/ performance（绩效）"
                                        },
                                        "dataRange": {
                                            "type": "string",
                                            "description": "数据范围：last_month（上月）/ last_week（上周）/ this_quarter（本季度）"
                                        }
                                    },
                                    "required": ["type", "dataRange"]
                                }
                                """)
                        .build(),

                ToolSpec.builder()
                        .name("generate_chart")
                        .description("生成数据图表配置")
                        .paramSpec("""
                                {
                                    "type": "object",
                                    "properties": {
                                        "chartType": {
                                            "type": "string",
                                            "description": "图表类型：bar（柱状图）/ line（折线图）/ pie（饼图）"
                                        },
                                        "dataType": {
                                            "type": "string",
                                            "description": "数据类型：sales（销售）/ user（用户）"
                                        }
                                    },
                                    "required": ["chartType"]
                                }
                                """)
                        .build()
        );
    }

    /**
     * 搜索知识库
     */
    public ToolResult searchKnowledge(String query) {
        logger.info("搜索知识库: {}", query);
        try {
            String result = knowledgeAgent.search(query);
            return ToolResult.success("知识检索完成", result);
        } catch (Exception e) {
            logger.error("知识库搜索失败", e);
            return ToolResult.failure("知识库搜索失败：" + e.getMessage());
        }
    }

    /**
     * 获取 FAQ
     */
    public ToolResult getFaq() {
        logger.info("获取 FAQ");
        try {
            String result = knowledgeAgent.getFaq();
            return ToolResult.success("FAQ 获取成功", result);
        } catch (Exception e) {
            logger.error("获取 FAQ 失败", e);
            return ToolResult.failure("获取 FAQ 失败：" + e.getMessage());
        }
    }

    /**
     * 创建日程
     */
    @SuppressWarnings("unchecked")
    public ToolResult createSchedule(Map<String, Object> params) {
        logger.info("创建日程: {}", params);

        try {
            String title = (String) params.get("title");
            String description = (String) params.getOr默认("description", "");
            String startTime = (String) params.get("startTime");
            String endTime = (String) params.get("endTime");

            if (title == null || startTime == null || endTime == null) {
                return ToolResult.failure("缺少必要参数：title, startTime, endTime");
            }

            String result = scheduleAgent.createSchedule(title, description, startTime, endTime);
            return ToolResult.success("日程创建成功", result);
        } catch (Exception e) {
            logger.error("创建日程失败", e);
            return ToolResult.failure("创建日程失败：" + e.getMessage());
        }
    }

    /**
     * 查询日程
     */
    public ToolResult querySchedules(String date) {
        logger.info("查询日程: {}", date);
        try {
            String result = scheduleAgent.querySchedules("current_user", date);
            return ToolResult.success("日程查询完成", result);
        } catch (Exception e) {
            logger.error("查询日程失败", e);
            return ToolResult.failure("查询日程失败：" + e.getMessage());
        }
    }

    /**
     * 生成报表
     */
    public ToolResult generateReport(String type, String dataRange) {
        logger.info("生成报表: {} - {}", type, dataRange);
        try {
            String result = reportAgent.generateReport(type, dataRange);
            return ToolResult.success("报表生成完成", result);
        } catch (Exception e) {
            logger.error("生成报表失败", e);
            return ToolResult.failure("生成报表失败：" + e.getMessage());
        }
    }

    /**
     * 生成图表
     */
    public ToolResult generateChart(String chartType, String dataType) {
        logger.info("生成图表: {} - {}", chartType, dataType);
        try {
            String result = reportAgent.generateChart(
                    chartType != null ? chartType : "bar",
                    dataType != null ? dataType : "sales");
            return ToolResult.success("图表生成完成", result);
        } catch (Exception e) {
            logger.error("生成图表失败", e);
            return ToolResult.failure("生成图表失败：" + e.getMessage());
        }
    }
}