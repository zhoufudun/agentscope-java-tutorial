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
package io.agentscope.tutorial.chapter19.assistant.knowledge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 知识问答 Agent 服务 - 企业知识库的封装层
 *
 * <p>负责：
 * <ul>
 *   <li>FAQ 问答 - 回答常见问题</li>
 *   <li>文档检索 - 基于关键词的文档搜索</li>
 *   <li>知识库查询 - 标签和内容的模糊匹配</li>
 * </ul>
 *
 * <h2>知识库设计</h2>
 * <p>本示例使用内存 Map 模拟知识库，实际生产环境可替换为：
 * <ul>
 *   <li>向量数据库（如 Milvus、Pinecone）用于语义检索</li>
 *   <li>Elasticsearch 用于全文搜索</li>
 *   <li>图数据库（如 Neo4j）用于知识图谱</li>
 * </ul>
 *
 * <h2>检索策略</h2>
 * <p>当前实现使用简单的关键词匹配，优先级：
 * <ol>
 *   <li>标题匹配 - 最相关</li>
 *   <li>标签匹配 - 次相关</li>
 *   <li>内容匹配 - 可能相关</li>
 * </ol>
 *
 * <h2>与 Agent 的集成</h2>
 * <p>本服务被 EnterpriseAssistantTools 的 @Tool 方法调用，
 * 作为 ReActAgent 的工具能力之一。
 *
 * @author AgentScope Tutorial
 * @since 1.0
 */
@Service
public class KnowledgeAgent {

    private static final Logger logger = LoggerFactory.getLogger(KnowledgeAgent.class);

    // 模拟知识库数据 - 实际生产中应替换为持久化存储
    private final Map<String, KnowledgeItem> knowledgeBase = new HashMap<>();

    /**
     * 构造函数 - 初始化知识库
     */
    public KnowledgeAgent() {
        initializeKnowledgeBase();
    }

    /**
     * 初始化知识库
     *
     * <p>预置企业常用知识条目，包括：
     * <ul>
     *   <li>人力资源：请假、考勤、招聘</li>
     *   <li>财务：报销流程</li>
     *   <li>IT 服务：技术支持联系方式</li>
     *   <li>行政：会议室预约</li>
     *   <li>法务：合同管理</li>
     * </ul>
     *
     * <p>每个知识条目包含标题、内容、分类和标签，支持多维度检索。
     */
    private void initializeKnowledgeBase() {
        // 公司制度
        knowledgeBase.put("请假", new KnowledgeItem(
                "请假制度",
                "员工请假需提前在系统中提交申请。\n" +
                "审批流程：员工 -> 直属上级 -> HR\n" +
                "年假标准：入职满1年享有5天年假，此后每年增加1天，上限15天。",
                "人力资源", List.of("请假", "假期", "年假", "休假")));

        knowledgeBase.put("报销", new KnowledgeItem(
                "报销流程",
                "员工报销需在每月5日前提交上月票据。\n" +
                "审批周期：3个工作日\n" +
                "报销范围：差旅费、办公费、业务招待费等。\n" +
                "注意事项：票据必须真实有效，附详细说明。",
                "财务", List.of("报销", "费用", "票据", "差旅")));

        knowledgeBase.put("IT", new KnowledgeItem(
                "IT 支持服务",
                "IT 问题请联系：\n" +
                "- 邮箱：helpdesk@company.com\n" +
                "- 电话：8001\n" +
                "- 紧急故障：8002\n\n" +
                "服务时间：工作日 9:00-18:00\n" +
                "常见问题可访问内网 IT 知识库。",
                "IT服务", List.of("IT", "网络", "电脑", "打印机", "软件")));

        knowledgeBase.put("会议", new KnowledgeItem(
                "会议室预约",
                "会议室通过 OA 系统预约。\n" +
                "预约规则：提前2小时可预约使用。\n" +
                "取消规则：提前1小时可取消。\n" +
                "超时规则：超时使用将自动释放。",
                "行政", List.of("会议", "会议室", "预约", "投影")));

        knowledgeBase.put("合同", new KnowledgeItem(
                "合同管理制度",
                "合同签订流程：\n" +
                "1. 业务部门发起申请\n" +
                "2. 法务部门审核\n" +
                "3. 财务部门确认\n" +
                "4. 分管领导审批\n" +
                "5. CEO/董事长签批\n\n" +
                "合同模板可在 OA 系统下载。",
                "法务", List.of("合同", "协议", "签章", "法务")));

        knowledgeBase.put("招聘", new KnowledgeItem(
                "招聘流程",
                "招聘申请流程：\n" +
                "1. 用人部门提交招聘需求\n" +
                "2. HR 审核岗位必要性\n" +
                "3. 分管领导审批编制\n" +
                "4. 发布招聘信息\n" +
                "5. 简历筛选与面试\n" +
                "6. 发放 offer",
                "人力资源", List.of("招聘", "面试", "offer", "入职")));

        knowledgeBase.put("考勤", new KnowledgeItem(
                "考勤制度",
                "工作时间：9:00-18:00（午休1小时）\n" +
                "考勤方式：指纹/刷脸打卡\n" +
                "迟到处理：每月累计3次免罚，第4次起扣绩效\n" +
                "加班申请：需部门负责人审批\n" +
                "外出登记：需在系统中报备",
                "人力资源", List.of("考勤", "迟到", "加班", "打卡")));
    }

    /**
     * 搜索知识
     *
     * <p><b>搜索算法：</b>
     * <ol>
     *   <li>遍历所有知识条目</li>
     *   <li>依次检查标题、标签、内容是否包含查询词</li>
     *   <li>匹配的条目加入结果集</li>
     *   <li>格式化输出，包含分类和内容</li>
     * </ol>
     *
     * <p><b>优化方向：</b>
     * <ul>
     *   <li>使用向量嵌入实现语义搜索</li>
     *   <li>支持同义词扩展</li>
     *   <li>添加搜索结果排序和分页</li>
     * </ul>
     *
     * @param query 查询关键词
     * @return 搜索结果，格式化的文本
     */
    public String search(String query) {
        logger.info("搜索知识: {}", query);

        List<KnowledgeItem> results = new ArrayList<>();
        String lowerQuery = query.toLowerCase();

        for (KnowledgeItem item : knowledgeBase.values()) {
            // 匹配标题
            if (item.title.toLowerCase().contains(lowerQuery)) {
                results.add(item);
                continue;
            }

            // 匹配标签
            for (String tag : item.tags) {
                if (tag.toLowerCase().contains(lowerQuery)) {
                    results.add(item);
                    break;
                }
            }

            // 匹配内容
            if (item.content.toLowerCase().contains(lowerQuery)) {
                if (!results.contains(item)) {
                    results.add(item);
                }
            }
        }

        if (results.isEmpty()) {
            return String.format("抱歉，未找到与 '%s' 相关的知识内容。\n" +
                    "建议：请尝试使用其他关键词，或联系IT支持（8001）获取帮助。", query);
        }

        StringBuilder response = new StringBuilder();
        response.append("找到以下相关内容：\n\n");

        for (int i = 0; i < results.size(); i++) {
            KnowledgeItem item = results.get(i);
            response.append(String.format("%d. 【%s】%s\n", i + 1, item.category, item.title));
            response.append(item.content).append("\n\n");
        }

        return response.toString();
    }

    /**
     * 获取 FAQ
     */
    public String getFaq() {
        StringBuilder faq = new StringBuilder();
        faq.append("===== 常见问题 FAQ =====\n\n");

        faq.append("【请假相关】\n");
        faq.append("Q: 如何申请请假？\n");
        faq.append("A: 请在OA系统提交请假申请，审批通过后生效。\n\n");

        faq.append("【报销相关】\n");
        faq.append("Q: 报销需要哪些票据？\n");
        faq.append("A: 需要正规发票，注明费用明细。\n\n");

        faq.append("【IT相关】\n");
        faq.append("Q: 电脑故障怎么办？\n");
        faq.append("A: 联系IT部门（8001）或发送邮件至 helpdesk@company.com\n\n");

        faq.append("【会议相关】\n");
        faq.append("Q: 如何预约会议室？\n");
        faq.append("A: 通过OA系统预约，建议提前2小时预约。\n");

        return faq.toString();
    }

    /**
     * 知识条目 - 知识库的基本存储单元
     *
     * <p>包含四个核心字段：
     * <ul>
     *   <li>title - 条目标题，用于快速识别</li>
     *   <li>content - 详细内容，包含完整信息</li>
     *   <li>category - 所属分类（如人力资源、财务等）</li>
     *   <li>tags - 标签列表，支持多关键词检索</li>
     * </ul>
     */
    private static class KnowledgeItem {
        String title;
        String content;
        String category;
        List<String> tags;

        KnowledgeItem(String title, String content, String category, List<String> tags) {
            this.title = title;
            this.content = content;
            this.category = category;
            this.tags = tags;
        }
    }
}