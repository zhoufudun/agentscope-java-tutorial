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
package io.agentscope.tutorial.chapter19;

import io.agentscope.tutorial.chapter19.assistant.knowledge.KnowledgeAgent;
import io.agentscope.tutorial.chapter19.assistant.report.ReportAgent;
import io.agentscope.tutorial.chapter19.assistant.schedule.ScheduleAgent;
import io.agentscope.tutorial.chapter19.boba.barista.BaristaAgent;
import io.agentscope.tutorial.chapter19.boba.cashier.CashierAgent;
import io.agentscope.tutorial.chapter19.boba.service.BobaOrderService;
import io.agentscope.tutorial.chapter19.boba.supervisor.OrderSupervisorAgent;
import io.agentscope.tutorial.chapter19.werewolf.game.GameState;
import io.agentscope.tutorial.chapter19.werewolf.game.Player;
import io.agentscope.tutorial.chapter19.werewolf.game.Role;
import io.agentscope.tutorial.chapter19.werewolf.game.WerewolfGame;
import io.agentscope.tutorial.chapter19.werewolf.role.ModeratorAgent;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.message.ToolResultState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 综合测试类 - 第十九章三大实战案例的集成测试
 *
 * <p>测试三个实战案例的核心功能：
 * <ul>
 *   <li>案例1：奶茶店多代理系统 - Supervisor 模式</li>
 *   <li>案例2：狼人杀游戏 - 多 Agent 博弈</li>
 *   <li>案例3：企业智能助手 - 能力分解协作</li>
 * </ul>
 *
 * <h2>测试策略</h2>
 * <ul>
 *   <li><b>单元测试</b>：测试各组件的独立功能（如价格计算、状态管理）</li>
 *   <li><b>集成测试</b>：测试 Agent 之间的协作（需要 Spring 容器）</li>
 *   <li><b>模拟测试</b>：使用 mock 对象避免真实 API 调用</li>
 * </ul>
 *
 * <h2>测试分类</h2>
 * <ul>
 *   <li><b>奶茶店测试</b>：菜单、价格、库存、订单号</li>
 *   <li><b>狼人杀测试</b>：游戏状态、胜负判定、玩家行为</li>
 *   <li><b>企业助手测试</b>：知识检索、日程管理、报表生成</li>
 *   <li><b>共享工具测试</b>：时间、计算、文本处理</li>
 * </ul>
 *
 * <h2>注意事项</h2>
 * <p>部分测试需要真实的 LLM API 调用，在 CI 环境中可能需要 mock 或跳过。
 * 建议将需要 API 调用的测试标记为 @Tag("integration")。
 *
 * @author AgentScope Tutorial
 * @since 1.0
 */
@SpringBootTest
class ComprehensiveTest {

    // ==================== 案例1：奶茶店测试 ====================
    // 测试 Supervisor 模式下各组件的独立功能
    // 注：真实的 Agent 调用测试需要 LLM API，这里主要测试业务逻辑

    @Autowired
    private OrderSupervisorAgent orderSupervisorAgent;

    @Autowired
    private CashierAgent cashierAgent;

    @Autowired
    private BaristaAgent baristaAgent;

    @Autowired
    private BobaOrderService bobaOrderService;

    // ==================== 案例2：狼人杀测试 ====================
    // 测试游戏状态管理和胜负判定逻辑
    // 使用 mock Player（agent 为 null）避免真实 Agent 调用

    @Autowired
    private ModeratorAgent moderatorAgent;

    // ==================== 案例3：企业助手测试 ====================
    // 测试各专业服务的独立功能

    @Autowired
    private KnowledgeAgent knowledgeAgent;

    @Autowired
    private ScheduleAgent scheduleAgent;

    @Autowired
    private ReportAgent reportAgent;

    // ==================== 共享工具测试 ====================
    // 测试跨案例共享的工具方法

    @Autowired
    private SharedTools sharedTools;

    // ==================== Test Methods ====================

    @BeforeEach
    void setUp() {
        System.out.println("=".repeat(50));
        System.out.println("开始测试...");
        System.out.println("=".repeat(50));
    }

    // ==================== 奶茶店测试 ====================
    // 这些测试验证奶茶店案例的核心业务逻辑，不依赖 LLM API

    @Test
    @DisplayName("测试奶茶店菜单获取 - 验证菜单数据完整性")
    void testBobaGetMenu() {
        String menu = bobaOrderService.getMenu();
        assertNotNull(menu);
        assertTrue(menu.contains("珍珠奶茶"));
        assertTrue(menu.contains("18元"));
        System.out.println("奶茶店菜单获取成功");
    }

    @Test
    @DisplayName("测试奶茶店价格计算 - 验证饮品和加料的价格计算逻辑")
    void testBobaPriceCalculation() {
        // 测试基础饮品 + 单个加料
        double price1 = bobaOrderService.calculatePrice("珍珠奶茶", List.of("珍珠"));
        assertEquals(21.0, price1, 0.01); // 18 + 3 = 21
        System.out.println("珍珠奶茶(加珍珠)价格：21元 - 正确");

        // 测试高价饮品 + 多个加料
        double price2 = bobaOrderService.calculatePrice("芋泥波波", List.of("波霸", "珍珠"));
        assertEquals(28.0, price2, 0.01); // 22 + 3 + 3 = 28
        System.out.println("芋泥波波(加波霸+珍珠)价格：28元 - 正确");
    }

    @Test
    @DisplayName("测试奶茶店库存检查")
    void testBobaStockCheck() {
        var result = bobaOrderService.checkStock("珍珠");
        assertTrue(isSuccess(result));
        assertTrue(content(result).contains("库存"));
        System.out.println("库存检查成功：" + content(result));
    }

    @Test
    @DisplayName("测试奶茶店订单号生成")
    void testBobaOrderIdGeneration() {
        String orderId = cashierAgent.generateOrderId("珍珠奶茶");
        assertNotNull(orderId);
        assertTrue(orderId.startsWith("BOBA"));
        System.out.println("订单号生成成功：" + orderId);
    }

    // ==================== 狼人杀测试 ====================
    // 这些测试验证狼人杀游戏的状态管理和胜负判定逻辑
    // 使用 mock 玩家（Agent 为 null）避免真实 LLM 调用

    @Test
    @DisplayName("测试游戏状态初始化 - 验证角色分配的正确性")
    void testGameStateInitialization() {
        // 创建 9 名模拟玩家（3村民 + 3狼人 + 3神职）
        List<Player> players = createMockPlayers();
        GameState gameState = new GameState(players);

        // 验证玩家总数
        assertEquals(9, gameState.getAlivePlayers().size());
        // 验证狼人数量
        assertEquals(3, gameState.getAliveWerewolves().size());
        // 验证特殊角色存在
        assertNotNull(gameState.getSeer());
        assertNotNull(gameState.getWitch());
        assertNotNull(gameState.getHunter());

        System.out.println("游戏状态初始化正确：9名玩家，3狼3神3民");
    }

    @Test
    @DisplayName("测试狼人杀胜负判断 - 验证两种胜利条件")
    void testWerewolfWinCondition() {
        // ===== 测试狼人胜利条件 =====
        // 条件：狼人数量 >= 好人数量
        List<Player> players = createMockPlayers();
        GameState gameState = new GameState(players);

        // 模拟狼人杀光好人（只剩3狼）
        for (Player p : new ArrayList<>(gameState.getAliveVillagers())) {
            p.kill();
        }

        assertTrue(gameState.checkWerewolvesWin());
        System.out.println("狼人胜利条件判断正确");

        // ===== 测试好人胜利条件 =====
        // 条件：所有狼人死亡
        players = createMockPlayers();
        gameState = new GameState(players);

        // 模拟好人杀光狼人
        for (Player p : new ArrayList<>(gameState.getAliveWerewolves())) {
            p.kill();
        }

        assertTrue(gameState.checkVillagersWin());
        System.out.println("好人胜利条件判断正确");
    }

    @Test
    @DisplayName("测试玩家死亡和复活")
    void testPlayerKillAndResurrect() {
        List<Player> players = createMockPlayers();
        Player player = players.get(0);

        assertTrue(player.isAlive());
        player.kill();
        assertFalse(player.isAlive());

        player.resurrect();
        assertTrue(player.isAlive());
        System.out.println("玩家死亡和复活功能正常");
    }

    @Test
    @DisplayName("测试游戏日志")
    void testGameLog() {
        List<Player> players = createMockPlayers();
        GameState gameState = new GameState(players);

        assertNotNull(gameState.getGameLog());
        assertFalse(gameState.getGameLog().isEmpty());
        System.out.println("游戏日志功能正常");
    }

    // ==================== 企业助手测试 ====================
    // 这些测试验证企业助手各子能力的独立功能

    @Test
    @DisplayName("测试知识库搜索 - 请假相关问题")
    void testKnowledgeSearch请假() {
        String result = knowledgeAgent.search("请假");
        assertNotNull(result);
        assertTrue(result.contains("请假") || result.contains("假期"));
        System.out.println("知识库搜索'请假'成功");
    }

    @Test
    @DisplayName("测试知识库搜索 - IT")
    void testKnowledgeSearchIT() {
        String result = knowledgeAgent.search("电脑故障");
        assertNotNull(result);
        assertTrue(result.contains("IT") || result.contains("故障"));
        System.out.println("知识库搜索'电脑故障'成功");
    }

    @Test
    @DisplayName("测试知识库FAQ获取")
    void testKnowledgeFaq() {
        String faq = knowledgeAgent.getFaq();
        assertNotNull(faq);
        assertTrue(faq.contains("FAQ"));
        assertTrue(faq.contains("请假") || faq.contains("报销"));
        System.out.println("知识库FAQ获取成功");
    }

    @Test
    @DisplayName("测试日程创建")
    void testScheduleCreation() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        String startTime = LocalDateTime.now().plusDays(1)
                .withHour(10).withMinute(0).format(formatter);
        String endTime = LocalDateTime.now().plusDays(1)
                .withHour(11).withMinute(0).format(formatter);

        String result = scheduleAgent.createSchedule(
                "测试会议",
                "这是一个测试会议",
                startTime,
                endTime
        );

        assertNotNull(result);
        assertTrue(result.contains("成功") || result.contains("创建"));
        System.out.println("日程创建成功");
    }

    @Test
    @DisplayName("测试日程查询")
    void testScheduleQuery() {
        String result = scheduleAgent.querySchedules("user001", null);
        assertNotNull(result);
        System.out.println("日程查询成功，共 " +
                (result.contains("日程安排") ? "有日程" : "无日程"));
    }

    @Test
    @DisplayName("测试报表生成 - 销售报表")
    void testReportGenerationSales() {
        String report = reportAgent.generateReport("sales", "last_month");
        assertNotNull(report);
        assertTrue(report.contains("销售") || report.contains("报表"));
        System.out.println("销售报表生成成功");
    }

    @Test
    @DisplayName("测试报表生成 - 用户报表")
    void testReportGenerationUser() {
        String report = reportAgent.generateReport("user", "last_week");
        assertNotNull(report);
        assertTrue(report.contains("用户") || report.contains("报表"));
        System.out.println("用户报表生成成功");
    }

    @Test
    @DisplayName("测试图表生成 - 柱状图")
    void testChartGenerationBar() {
        String chart = reportAgent.generateChart("bar", "sales");
        assertNotNull(chart);
        assertTrue(chart.contains("bar") || chart.contains("chartType"));
        System.out.println("柱状图配置生成成功");
    }

    @Test
    @DisplayName("测试图表生成 - 饼图")
    void testChartGenerationPie() {
        String chart = reportAgent.generateChart("pie", "sales");
        assertNotNull(chart);
        assertTrue(chart.contains("pie") || chart.contains("chartType"));
        System.out.println("饼图配置生成成功");
    }

    // ==================== 共享工具测试 ====================
    // 测试跨案例共享的工具方法，验证 Tool 注解和 ToolResultBlock 的使用

    @Test
    @DisplayName("测试获取当前时间 - 验证时间工具的正确性")
    void testGetCurrentTime() {
        var result = sharedTools.getCurrentTime();
        assertTrue(isSuccess(result));
        assertTrue(content(result).contains("当前时间"));
        System.out.println("获取当前时间成功");
    }

    @Test
    @DisplayName("测试数学计算 - 验证表达式解析器的运算符优先级")
    void testCalculate() {
        // 测试运算符优先级：乘法优先于加法
        var result = sharedTools.calculate("2+3*4");
        assertTrue(isSuccess(result));
        assertTrue(content(result).contains("14")); // 2 + (3*4) = 14，而非 (2+3)*4 = 20
        System.out.println("数学计算 2+3*4 = 14 - 正确");
    }

    @Test
    @DisplayName("测试文本格式化")
    void testFormatText() {
        var result = sharedTools.formatText("hello world", "upper");
        assertTrue(isSuccess(result));
        assertTrue(content(result).contains("HELLO"));
        System.out.println("文本格式化成功");
    }

    @Test
    @DisplayName("测试随机选择")
    void testRandomChoice() {
        var result = sharedTools.randomChoice(List.of("A", "B", "C"));
        assertTrue(isSuccess(result));
        assertTrue(content(result).contains("A") ||
                   content(result).contains("B") ||
                   content(result).contains("C"));
        System.out.println("随机选择成功");
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建模拟玩家列表
     *
     * <p>用于测试游戏逻辑，不涉及真实 Agent 调用。
     * 玩家 Agent 设置为 null，因为我们只测试 GameState 的状态管理逻辑。
     *
     * @return 包含 9 名玩家的列表（3村民 + 3狼人 + 3神职）
     */
    private List<Player> createMockPlayers() {
        List<Player> players = new ArrayList<>();
        List<String> names = List.of(
                "小红", "小明", "小华", "小丽", "小强",
                "小芳", "小刚", "小美", "小亮"
        );

        List<Role> roles = List.of(
                Role.VILLAGER, Role.VILLAGER, Role.VILLAGER,
                Role.WEREWOLF, Role.WEREWOLF, Role.WEREWOLF,
                Role.SEER, Role.WITCH, Role.HUNTER
        );

        for (int i = 0; i < 9; i++) {
            // 使用 null 作为 Agent，因为我们只测试游戏逻辑
            players.add(new Player(names.get(i), roles.get(i), null));
        }

        return players;
    }

    /**
     * 判断工具结果是否成功。
     *
     * <p>2.x 移除了 ToolResultBlock.isSuccess()，改用 ToolResultState 表示状态，
     * 这里将非 ERROR 状态视为成功（工具方法成功时返回 text 结果，失败时返回 error 结果）。
     */
    private static boolean isSuccess(ToolResultBlock result) {
        return result.getState() != ToolResultState.ERROR;
    }

    /**
     * 提取工具结果的文本内容。
     *
     * <p>2.x 移除了 ToolResultBlock.getContent()，内容改为通过 getOutput() 返回的
     * ContentBlock 列表承载，这里拼接其中所有 TextBlock 的文本。
     */
    private static String content(ToolResultBlock result) {
        StringBuilder sb = new StringBuilder();
        for (ContentBlock block : result.getOutput()) {
            if (block instanceof TextBlock textBlock) {
                sb.append(textBlock.getText());
            }
        }
        return sb.toString();
    }
}