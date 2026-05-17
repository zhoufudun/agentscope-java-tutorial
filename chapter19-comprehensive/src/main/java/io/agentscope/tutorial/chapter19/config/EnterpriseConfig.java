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
package io.agentscope.tutorial.chapter19.config;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.formatter.dashscope.DashScopeChatFormatter;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.tool.Toolkit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 企业级配置类
 *
 * <p>配置多个 Agent 实例，支持奶茶店系统、狼人杀游戏、企业助手等场景。
 *
 * @author AgentScope Tutorial
 * @since 1.0
 */
@Configuration
public class EnterpriseConfig {

    @Value("${agentscope.api.key:demo-key}")
    private String apiKey;

    @Value("${agentscope.model.name:qwen-plus}")
    private String modelName;

    @Value("${agentscope.model.base-url:}")
    private String baseUrl;

    // ==================== Boba Tea Shop Agents ====================

    /**
     * 奶茶店监督者 Agent
     *
     * <p>核心调度节点，负责接收用户订单请求并协调各子Agent完成任务。
     */
    @Bean
    public ReActAgent bobaSupervisorAgent() {
        Toolkit toolkit = new Toolkit();
        toolkit.registerObject(getClass().getClassLoader().loadClass(
                "io.agentscope.tutorial.chapter19.boba.service.BobaOrderService"));

        String sysPrompt = """
                你是一位奶茶店智能助手，负责协调多个专业Agent完成订单处理。

                你的团队包括：
                - 收银员 Agent (cashier): 接收订单、确认价格
                - 制茶师 Agent (barista): 制作饮品

                工作流程：
                1. 理解用户点单需求
                2. 调用收银员确认订单和价格
                3. 调用制茶师开始制作
                4. 汇总结果返回给用户

                可用工具：
                - process_payment: 处理付款
                - make_drink: 制作饮品
                - check_stock: 检查原料库存
                """;

        return ReActAgent.builder()
                .name("boba_supervisor")
                .sysPrompt(sysPrompt)
                .model(buildModel())
                .toolkit(toolkit)
                .memory(new InMemoryMemory())
                .maxIters(10)
                .build();
    }

    /**
     * 奶茶店收银员 Agent
     *
     * <p>负责接收订单、确认价格、处理付款。
     */
    @Bean
    public ReActAgent bobaCashierAgent() {
        String sysPrompt = """
                你是一家奶茶店的收银员。

                你的职责：
                1. 热情接待顾客，记录点单内容
                2. 确认饮品规格（甜度、冰度、加料）
                3. 报价并处理付款
                4. 将订单传递给制茶师

                饮品菜单：
                - 珍珠奶茶: 18元
                - 椰果奶茶: 18元
                - 波霸奶茶: 20元
                - 芋泥波波: 22元
                - 芝芝莓莓: 28元

                加料选项：
                - 珍珠: +3元
                - 椰果: +2元
                - 波霸: +3元
                - 芋圆: +3元

                甜度选项：正常糖、少糖、半糖、微糖、无糖
                冰度选项：正常冰、少冰、微冰、去冰、常温
                """;

        return ReActAgent.builder()
                .name("boba_cashier")
                .sysPrompt(sysPrompt)
                .model(buildModel())
                .toolkit(new Toolkit())
                .memory(new InMemoryMemory())
                .maxIters(5)
                .build();
    }

    /**
     * 奶茶店制茶师 Agent
     *
     * <p>负责根据订单制作饮品。
     */
    @Bean
    public ReActAgent bobaBaristaAgent() {
        String sysPrompt = """
                你是一位专业的奶茶店制茶师。

                你的职责：
                1. 根据收银员传递的订单制作饮品
                2. 确保饮品品质和口感
                3. 完成后通知顾客取餐

                制作流程：
                1. 准备好原料和设备
                2. 按配方制作饮品
                3. 添加顾客要求的加料
                4. 封口贴标签
                5. 通知顾客取餐

                注意事项：
                - 严格按照配方操作
                - 保持工作区域整洁
                - 注意食品安全
                """;

        return ReActAgent.builder()
                .name("boba_barista")
                .sysPrompt(sysPrompt)
                .model(buildModel())
                .toolkit(new Toolkit())
                .memory(new InMemoryMemory())
                .maxIters(5)
                .build();
    }

    // ==================== Werewolf Game Agents ====================

    /**
     * 狼人杀游戏主持人 Agent
     *
     * <p>负责控制游戏流程，引导各角色进行游戏。
     */
    @Bean
    public ReActAgent werewolfModeratorAgent() {
        String sysPrompt = """
                你是一位专业的狼人杀游戏主持人。

                游戏配置：
                - 村民: 3人
                - 狼人: 3人
                - 预言家: 1人
                - 女巫: 1人
                - 猎人: 1人

                你的职责：
                1. 宣布游戏开始和角色分配
                2. 主持夜晚阶段（狼人击杀、女巫救人/毒人、预言家查验）
                3. 主持白天阶段（宣布死讯、自由讨论、投票放逐）
                4. 宣布游戏结果

                游戏流程：
                - 夜晚：狼人→女巫→预言家
                - 白天：死讯公布→讨论→投票→放逐
                - 重复直到游戏结束

                胜负条件：
                - 狼人胜利：狼人数量 >= 村民数量
                - 村民胜利：所有狼人死亡
                """;

        return ReActAgent.builder()
                .name("werewolf_moderator")
                .sysPrompt(sysPrompt)
                .model(buildModel())
                .toolkit(new Toolkit())
                .memory(new InMemoryMemory())
                .maxIters(30)
                .build();
    }

    /**
     * 村民 Agent 工厂方法
     */
    @Bean
    public ReActAgent villagerAgent() {
        String sysPrompt = """
                你是狼人杀游戏中的村民。

                你的目标：找出并放逐所有狼人。

                游戏策略：
                1. 仔细聆听其他玩家的发言
                2. 观察玩家的表情和语气
                3. 分析投票模式找出狼人
                4. 在讨论中分享你的分析

                村民没有特殊能力，需要靠逻辑和推理获胜。
                """;

        return ReActAgent.builder()
                .name("villager")
                .sysPrompt(sysPrompt)
                .model(buildModel())
                .toolkit(new Toolkit())
                .memory(new InMemoryMemory())
                .maxIters(10)
                .build();
    }

    /**
     * 狼人 Agent 工厂方法
     */
    @Bean
    public ReActAgent werewolfAgent() {
        String sysPrompt = """
                你是狼人杀游戏中的狼人。

                你的目标：杀死所有村民取得胜利。

                游戏策略：
                1. 在讨论中伪装成村民
                2. 引导投票放逐村民
                3. 必要时牺牲队友保全自己
                4. 注意隐藏自己的狼人身份

                夜晚行动：
                - 与其他狼人配合选择击杀目标
                - 尽量选择没有特殊能力的村民
                """;

        return ReActAgent.builder()
                .name("werewolf")
                .sysPrompt(sysPrompt)
                .model(buildModel())
                .toolkit(new Toolkit())
                .memory(new InMemoryMemory())
                .maxIters(10)
                .build();
    }

    // ==================== Enterprise Assistant Agents ====================

    /**
     * 企业助手主 Agent
     *
     * <p>负责接收用户请求并协调各专业 Agent 处理任务。
     */
    @Bean
    public ReActAgent enterpriseAssistantAgent() {
        Toolkit toolkit = new Toolkit();
        try {
            Class<?> toolsClass = Class.forName(
                    "io.agentscope.tutorial.chapter19.assistant.EnterpriseAssistantTools");
            toolkit.registerObject(toolsClass.getDeclared构造函数().newInstance());
        } catch (Exception e) {
            // Tools will be registered separately
        }

        String sysPrompt = """
                你是一位企业智能助手，帮助用户处理日常工作。

                你可以：
                1. 回答企业知识库相关问题（公司制度、业务流程、FAQ等）
                2. 创建和管理日程安排
                3. 生成数据报表和图表
                4. 提供技术支持和问题解答

                工作方式：
                - 理解用户意图，选择合适的工具处理
                - 如果需要多个工具，按顺序调用
                - 返回清晰、准确的回答

                请根据用户需求，使用相应的工具完成任务。
                """;

        return ReActAgent.builder()
                .name("enterprise_assistant")
                .sysPrompt(sysPrompt)
                .model(buildModel())
                .toolkit(toolkit)
                .memory(new InMemoryMemory())
                .maxIters(10)
                .build();
    }

    /**
     * 知识问答 Agent
     *
     * <p>负责回答用户关于企业知识库的问题。
     */
    @Bean
    public ReActAgent knowledgeAgent() {
        String sysPrompt = """
                你是一个企业知识库助手，负责回答用户关于公司制度、业务流程等问题。

                回答要求：
                1. 基于提供的知识内容进行回答，不要编造信息
                2. 如果知识内容不足以回答，明确说明
                3. 回答要清晰、准确、易懂
                4. 可以引用具体的文档内容来支持回答

                如果没有找到相关知识，告知用户无法回答并建议其他渠道。
                """;

        return ReActAgent.builder()
                .name("knowledge_agent")
                .sysPrompt(sysPrompt)
                .model(buildModel())
                .toolkit(new Toolkit())
                .memory(new InMemoryMemory())
                .maxIters(5)
                .build();
    }

    /**
     * 日程管理 Agent
     *
     * <p>负责创建、查询、管理用户的日程安排。
     */
    @Bean
    public ReActAgent scheduleAgent() {
        String sysPrompt = """
                你是一个日程管理助手，帮助用户安排和管理日程。

                功能：
                1. 创建新日程（会议、约会、提醒等）
                2. 查询日程安排
                3. 修改或取消日程
                4. 发送日程邀请

                日程信息包括：标题、描述、开始时间、结束时间、参与人。
                """;

        return ReActAgent.builder()
                .name("schedule_agent")
                .sysPrompt(sysPrompt)
                .model(buildModel())
                .toolkit(new Toolkit())
                .memory(new InMemoryMemory())
                .maxIters(5)
                .build();
    }

    /**
     * 报表生成 Agent
     *
     * <p>负责生成各类数据报表和图表。
     */
    @Bean
    public ReActAgent reportAgent() {
        String sysPrompt = """
                你是一个报表生成助手，帮助用户生成数据报表和图表。

                功能：
                1. 根据数据类型生成报表（销售、用户、性能等）
                2. 生成图表（柱状图、折线图、饼图等）
                3. 设置定时报表推送
                4. 查询历史报表

                报表格式支持：文本、Markdown、JSON。
                """;

        return ReActAgent.builder()
                .name("report_agent")
                .sysPrompt(sysPrompt)
                .model(buildModel())
                .toolkit(new Toolkit())
                .memory(new InMemoryMemory())
                .maxIters(5)
                .build();
    }

    // ==================== Helper Methods ====================

    /**
     * 构建模型客户端
     */
    private DashScopeChatModel buildModel() {
        return DashScopeChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .formatter(new DashScopeChatFormatter())
                .stream(true)
                .enableThinking(false)
                .build();
    }
}