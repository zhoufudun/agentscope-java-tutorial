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

import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.Event;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.tutorial.chapter19.assistant.knowledge.KnowledgeAgent;
import io.agentscope.tutorial.chapter19.assistant.schedule.ScheduleAgent;
import io.agentscope.tutorial.chapter19.assistant.report.ReportAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.UUID;

/**
 * 企业智能助手 Agent
 *
 * <p>负责：
 * <ul>
 *   <li>接收用户请求并理解意图</li>
 *   <li>协调各专业子 Agent 处理任务</li>
 *   <li>聚合结果返回给用户</li>
 * </ul>
 *
 * <p>这是企业智能助手的核心入口，类似于奶茶店的监督者 Agent。
 *
 * @author AgentScope Tutorial
 * @since 1.0
 */
@Component
public class EnterpriseAssistantAgent {

    private static final Logger logger = LoggerFactory.getLogger(EnterpriseAssistantAgent.class);

    private final ReActAgent mainAgent;
    private final ReActAgent knowledgeAgent;
    private final ReActAgent scheduleAgent;
    private final ReActAgent reportAgent;

    private final KnowledgeAgent knowledgeAgentService;
    private final ScheduleAgent scheduleAgentService;
    private final ReportAgent reportAgentService;

    /**
     * 构造函数注入所有 Agent
     */
    @Autowired
    public EnterpriseAssistantAgent(
            @Qualifier("enterpriseAssistantAgent") ReActAgent mainAgent,
            @Qualifier("knowledgeAgent") ReActAgent knowledgeAgent,
            @Qualifier("scheduleAgent") ReActAgent scheduleAgent,
            @Qualifier("reportAgent") ReActAgent reportAgent,
            KnowledgeAgent knowledgeAgentService,
            ScheduleAgent scheduleAgentService,
            ReportAgent reportAgentService) {
        this.mainAgent = mainAgent;
        this.knowledgeAgent = knowledgeAgent;
        this.scheduleAgent = scheduleAgent;
        this.reportAgent = reportAgent;
        this.knowledgeAgentService = knowledgeAgentService;
        this.scheduleAgentService = scheduleAgentService;
        this.reportAgentService = reportAgentService;
        logger.info("EnterpriseAssistantAgent 初始化完成");
    }

    /**
     * 处理用户请求
     *
     * @param userMessage 用户的消息
     * @return 处理结果
     */
    public String processRequest(String userMessage) {
        logger.info("处理用户请求: {}", userMessage);
        String sessionId = UUID.randomUUID().toString();

        try {
            Msg userMsg = Msg.builder()
                    .name("user")
                    .role(MsgRole.USER)
                    .content(TextBlock.builder().text(userMessage).build())
                    .build();

            Msg response = mainAgent.call(userMsg).block();
            String result = extractText(response);

            logger.info("请求处理完成");
            return result;

        } catch (Exception e) {
            logger.error("处理请求失败", e);
            return "抱歉，处理请求时遇到问题：" + e.getMessage();
        }
    }

    /**
     * 流式处理用户请求
     *
     * @param userMessage 用户的消息
     * @return 事件流
     */
    public Flux<Event> processRequestStream(String userMessage) {
        logger.info("流式处理请求: {}", userMessage);

        Msg userMsg = Msg.builder()
                .name("user")
                .role(MsgRole.USER)
                .content(TextBlock.builder().text(userMessage).build())
                .build();

        return mainAgent.stream(userMsg);
    }

    /**
     * 知识库问答
     *
     * @param query 查询内容
     * @return 回答
     */
    public String answerKnowledge(String query) {
        logger.info("知识库问答: {}", query);
        try {
            return knowledgeAgentService.search(query);
        } catch (Exception e) {
            logger.error("知识库查询失败", e);
            return "知识库查询失败：" + e.getMessage();
        }
    }

    /**
     * 创建日程
     *
     * @param title 标题
     * @param description 描述
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 创建结果
     */
    public String createSchedule(String title, String description,
                                  String startTime, String endTime) {
        logger.info("创建日程: {}", title);
        try {
            return scheduleAgentService.createSchedule(title, description, startTime, endTime);
        } catch (Exception e) {
            logger.error("创建日程失败", e);
            return "创建日程失败：" + e.getMessage();
        }
    }

    /**
     * 查询日程
     *
     * @param userId 用户ID
     * @param date 日期
     * @return 日程列表
     */
    public String querySchedules(String userId, String date) {
        logger.info("查询日程: {} on {}", userId, date);
        try {
            return scheduleAgentService.querySchedules(userId, date);
        } catch (Exception e) {
            logger.error("查询日程失败", e);
            return "查询日程失败：" + e.getMessage();
        }
    }

    /**
     * 生成报表
     *
     * @param reportType 报表类型
     * @param dataRange 数据范围
     * @return 报表内容
     */
    public String generateReport(String reportType, String dataRange) {
        logger.info("生成报表: {} - {}", reportType, dataRange);
        try {
            return reportAgentService.generateReport(reportType, dataRange);
        } catch (Exception e) {
            logger.error("生成报表失败", e);
            return "生成报表失败：" + e.getMessage();
        }
    }

    /**
     * 获取助手状态
     */
    public String getStatus() {
        return String.format(
                "企业智能助手状态：\n" +
                "- 主助手: %s\n" +
                "- 知识问答: %s\n" +
                "- 日程管理: %s\n" +
                "- 报表生成: %s",
                mainAgent.getName(),
                knowledgeAgent.getName(),
                scheduleAgent.getName(),
                reportAgent.getName()
        );
    }

    private String extractText(Msg msg) {
        if (msg == null) {
            return "无响应";
        }
        return msg.getContent().stream()
                .filter(block -> block instanceof TextBlock)
                .map(block -> ((TextBlock) block).getText())
                .findFirst()
                .orElse("无法解析响应");
    }
}