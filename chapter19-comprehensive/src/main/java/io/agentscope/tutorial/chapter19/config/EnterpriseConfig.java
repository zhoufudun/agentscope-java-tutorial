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
import io.agentscope.extensions.model.dashscope.formatter.DashScopeChatFormatter;
import io.agentscope.extensions.model.dashscope.DashScopeChatModel;
import io.agentscope.core.tool.Toolkit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 企业级配置类 - 三大综合案例的 Agent 配置中心
 *
 * <p>本配置类是第十九章综合实战案例的核心配置，演示了如何在 Spring Boot 环境中
 * 配置和管理多个专业化 Agent。
 *
 * <h2>核心设计模式</h2>
 * <ul>
 *   <li><b>Supervisor 模式</b>：监督者 Agent 协调多个专业子 Agent（奶茶店案例）</li>
 *   <li><b>多角色协作模式</b>：不同角色 Agent 各司其职（狼人杀案例）</li>
 *   <li><b>能力分解模式</b>：将复杂任务分解为专业子能力（企业助手案例）</li>
 * </ul>
 *
 * <h2>配置的 Agent 清单</h2>
 * <ul>
 *   <li><b>奶茶店</b>：bobaSupervisorAgent（监督者）、bobaCashierAgent（收银员）、bobaBaristaAgent（制茶师）</li>
 *   <li><b>狼人杀</b>：werewolfModeratorAgent（主持人）、villagerReActAgent（村民）、werewolfReActAgent（狼人）</li>
 *   <li><b>企业助手</b>：enterpriseAssistantReActAgent（主助手）、knowledgeReActAgent、scheduleReActAgent、reportReActAgent</li>
 * </ul>
 *
 * <h2>关键配置点</h2>
 * <ul>
 *   <li>每个 Agent 都有独立的系统提示词（sysPrompt），定义其角色和行为</li>
 *   <li>通过 Toolkit 注入工具能力，实现 Agent 与外部系统的交互</li>
 *   <li>使用 @Qualifier 区分同类型的不同 Agent Bean</li>
 *   <li>maxIters 参数控制 ReAct 循环的最大迭代次数，防止无限循环</li>
 * </ul>
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

    // ==================== 案例一：奶茶店多代理系统 ====================
    //
    // 奶茶店案例演示了 Supervisor（监督者）模式：
    // - 监督者 Agent 作为中央协调节点，接收用户请求
    // - 根据任务类型，将子任务分发给专业 Agent（收银员、制茶师）
    // - 聚合各 Agent 的结果，返回给用户
    //
    // 这种模式适用于需要多角色协作的业务场景，如客服系统、订单处理等

    /**
     * 奶茶店监督者 Agent（Supervisor）
     *
     * <p>核心调度节点，负责接收用户订单请求并协调各子 Agent 完成任务。
     *
     * <p><b>Supervisor 模式要点：</b>
     * <ul>
     *   <li>理解用户意图，拆解任务</li>
     *   <li>选择合适的子 Agent 执行任务</li>
     *   <li>聚合结果，形成最终响应</li>
     * </ul>
     *
     * <p><b>系统提示词设计：</b>
     * <ul>
     *   <li>明确定义团队成员及其职责</li>
     *   <li>规定标准工作流程</li>
     *   <li>列出可用工具</li>
     * </ul>
     */
    @Bean
    public ReActAgent bobaSupervisorAgent() {
        Toolkit toolkit = new Toolkit();

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
                .maxIters(10)
                .build();
    }

    /**
     * 奶茶店收银员 Agent（专业子 Agent）
     *
     * <p>负责接收订单、确认价格、处理付款。
     *
     * <p><b>专业 Agent 设计要点：</b>
     * <ul>
     *   <li>系统提示词聚焦于具体职责</li>
     *   <li>包含业务知识（菜单、价格、规格选项）</li>
     *   <li>maxIters 较小，因为任务相对简单</li>
     * </ul>
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
                .maxIters(5)
                .build();
    }

    /**
     * 奶茶店制茶师 Agent（专业子 Agent）
     *
     * <p>负责根据订单制作饮品。
     *
     * <p><b>流程化任务的 Agent 设计：</b>
     * <ul>
     *   <li>系统提示词包含标准操作流程（SOP）</li>
     *   <li>强调质量和安全要求</li>
     *   <li>任务边界清晰，减少与其他 Agent 的职责重叠</li>
     * </ul>
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
                .maxIters(5)
                .build();
    }

    // ==================== 案例二：狼人杀多 Agent 博弈游戏 ====================
    //
    // 狼人杀案例演示了多 Agent 博弈场景：
    // - 不同阵营的 Agent 有不同的目标（狼人 vs 村民）
    // - Agent 需要推理、欺骗、合作等复杂行为
    // - 主持人 Agent 控制游戏流程，确保规则执行
    //
    // 这种模式适用于游戏 AI、模拟仿真、策略推演等场景

    /**
     * 狼人杀游戏主持人 Agent
     *
     * <p>负责控制游戏流程，引导各角色进行游戏。
     *
     * <p><b>游戏主持 Agent 设计要点：</b>
     * <ul>
     *   <li>完整掌握游戏规则和流程</li>
     *   <li>公正中立，不偏向任何阵营</li>
     *   <li>maxIters 较大（30），因为游戏回合数较多</li>
     * </ul>
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
                .maxIters(30)
                .build();
    }

    /**
     * 村民 Agent 工厂方法
     *
     * <p>Bean 名使用 {@code villagerReActAgent}，以避免与 {@code @Component} 标注的
     * {@link io.agentscope.tutorial.chapter19.werewolf.role.VillagerAgent}（默认 bean 名 villagerAgent）冲突。
     *
     * <p><b>好人阵营 Agent 设计要点：</b>
     * <ul>
     *   <li>目标明确：找出并放逐狼人</li>
     *   <li>强调逻辑推理和观察能力</li>
     *   <li>通过讨论和投票行为完成目标</li>
     * </ul>
     */
    @Bean
    public ReActAgent villagerReActAgent() {
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
                .maxIters(10)
                .build();
    }

    /**
     * 狼人 Agent 工厂方法
     *
     * <p>Bean 名使用 {@code werewolfReActAgent}，以避免与 {@code @Component} 标注的
     * {@link io.agentscope.tutorial.chapter19.werewolf.role.WerewolfAgent}（默认 bean 名 werewolfAgent）冲突。
     *
     * <p><b>对抗阵营 Agent 设计要点：</b>
     * <ul>
     *   <li>需要伪装能力：在讨论中假扮村民</li>
     *   <li>需要合作能力：与其他狼人配合行动</li>
     *   <li>需要策略能力：选择最优击杀目标</li>
     * </ul>
     *
     * <p><b>博弈 Agent 的挑战：</b>
     * <ul>
     *   <li>如何让 Agent 理解"欺骗"的概念</li>
     *   <li>如何平衡个人利益和团队利益</li>
     *   <li>如何应对不确定信息做出决策</li>
     * </ul>
     */
    @Bean
    public ReActAgent werewolfReActAgent() {
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
                .maxIters(10)
                .build();
    }

    // ==================== 案例三：企业智能助手 ====================
    //
    // 企业助手案例演示了能力分解与专业化协作：
    // - 主助手负责意图理解和任务路由
    // - 专业子 Agent 各自负责一个垂直领域（知识、日程、报表）
    // - 通过工具调用实现 Agent 之间的协作
    //
    // 这种模式适用于企业级智能助手、客服机器人、智能办公等场景

    /**
     * 企业助手主 Agent（意图路由中心）
     *
     * <p>负责接收用户请求并协调各专业 Agent 处理任务。
     *
     * <p><b>意图路由 Agent 设计要点：</b>
     * <ul>
     *   <li>具备强大的意图理解能力</li>
     *   <li>了解各子 Agent 的能力边界</li>
     *   <li>能够将复杂请求分解为子任务</li>
     *   <li>聚合多个子 Agent 的结果</li>
     * </ul>
     */
    @Bean
    public ReActAgent enterpriseAssistantReActAgent() {
        Toolkit toolkit = new Toolkit();

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
                .maxIters(10)
                .build();
    }

    /**
     * 知识问答 Agent（垂直领域专家）
     *
     * <p>负责回答用户关于企业知识库的问题。
     *
     * <p><b>知识型 Agent 设计要点：</b>
     * <ul>
     *   <li>强调基于事实回答，避免幻觉</li>
     *   <li>明确知识边界，不确定时诚实告知</li>
     *   <li>支持引用来源，增强可信度</li>
     * </ul>
     */
    @Bean
    public ReActAgent knowledgeReActAgent() {
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
                .maxIters(5)
                .build();
    }

    /**
     * 日程管理 Agent（任务执行型专家）
     *
     * <p>负责创建、查询、管理用户的日程安排。
     *
     * <p><b>任务执行型 Agent 设计要点：</b>
     * <ul>
     *   <li>支持 CRUD 操作（创建、查询、修改、删除）</li>
     *   <li>参数验证和错误处理</li>
     *   <li>与外部系统（日历 API）集成</li>
     * </ul>
     */
    @Bean
    public ReActAgent scheduleReActAgent() {
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
                .maxIters(5)
                .build();
    }

    /**
     * 报表生成 Agent（数据分析型专家）
     *
     * <p>负责生成各类数据报表和图表。
     *
     * <p><b>数据分析型 Agent 设计要点：</b>
     * <ul>
     *   <li>支持多种报表类型和图表格式</li>
     *   <li>数据聚合和可视化能力</li>
     *   <li>定时任务和历史查询支持</li>
     * </ul>
     */
    @Bean
    public ReActAgent reportReActAgent() {
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
                .maxIters(5)
                .build();
    }

    // ==================== 辅助方法 ====================

    /**
     * 构建模型客户端
     *
     * <p>使用 DashScope（阿里云通义千问）作为底层模型服务。
     *
     * <p><b>模型配置说明：</b>
     * <ul>
     *   <li>apiKey：模型服务的 API 密钥</li>
     *   <li>modelName：使用的模型名称（如 qwen-plus）</li>
     *   <li>formatter：消息格式化器，将 AgentScope 消息转换为模型 API 格式</li>
     *   <li>stream：启用流式输出，提升响应体验</li>
     *   <li>enableThinking：是否启用思考链（Chain-of-Thought）</li>
     * </ul>
     *
     * @return 配置好的 DashScope 模型客户端
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