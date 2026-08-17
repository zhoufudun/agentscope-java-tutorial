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
 * 综合测试类
 *
 * <p>测试三个实战案例的核心功能：
 * <ul>
 *   <li>案例1：奶茶店多代理系统</li>
 *   <li>案例2：狼人杀游戏</li>
 *   <li>案例3：企业智能助手</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 * @since 1.0
 */
@SpringBootTest
class ComprehensiveTest {

    // ==================== 案例1：奶茶店测试 ====================

    @Autowired
    private OrderSupervisorAgent orderSupervisorAgent;

    @Autowired
    private CashierAgent cashierAgent;

    @Autowired
    private BaristaAgent baristaAgent;

    @Autowired
    private BobaOrderService bobaOrderService;

    // ==================== 案例2：狼人杀测试 ====================

    @Autowired
    private ModeratorAgent moderatorAgent;

    // ==================== 案例3：企业助手测试 ====================

    @Autowired
    private KnowledgeAgent knowledgeAgent;

    @Autowired
    private ScheduleAgent scheduleAgent;

    @Autowired
    private ReportAgent reportAgent;

    // ==================== 共享工具测试 ====================

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

    @Test
    @DisplayName("测试奶茶店菜单获取")
    void testBobaGetMenu() {
        String menu = bobaOrderService.getMenu();
        assertNotNull(menu);
        assertTrue(menu.contains("珍珠奶茶"));
        assertTrue(menu.contains("18元"));
        System.out.println("奶茶店菜单获取成功");
    }

    @Test
    @DisplayName("测试奶茶店价格计算")
    void testBobaPriceCalculation() {
        double price1 = bobaOrderService.calculatePrice("珍珠奶茶", List.of("珍珠"));
        assertEquals(21.0, price1, 0.01);
        System.out.println("珍珠奶茶(加珍珠)价格：21元 - 正确");

        double price2 = bobaOrderService.calculatePrice("芋泥波波", List.of("波霸", "珍珠"));
        assertEquals(28.0, price2, 0.01);
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

    @Test
    @DisplayName("测试游戏状态初始化")
    void testGameStateInitialization() {
        List<Player> players = createMockPlayers();
        GameState gameState = new GameState(players);

        assertEquals(9, gameState.getAlivePlayers().size());
        assertEquals(3, gameState.getAliveWerewolves().size());
        assertNotNull(gameState.getSeer());
        assertNotNull(gameState.getWitch());
        assertNotNull(gameState.getHunter());

        System.out.println("游戏状态初始化正确：9名玩家，3狼3神3民");
    }

    @Test
    @DisplayName("测试狼人杀胜负判断")
    void testWerewolfWinCondition() {
        List<Player> players = createMockPlayers();
        GameState gameState = new GameState(players);

        // 模拟狼人杀光好人（只剩3狼）
        for (Player p : new ArrayList<>(gameState.getAliveVillagers())) {
            p.kill();
        }

        assertTrue(gameState.checkWerewolvesWin());
        System.out.println("狼人胜利条件判断正确");

        // 重置测试
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

    @Test
    @DisplayName("测试知识库搜索 - 请假")
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

    @Test
    @DisplayName("测试获取当前时间")
    void testGetCurrentTime() {
        var result = sharedTools.getCurrentTime();
        assertTrue(isSuccess(result));
        assertTrue(content(result).contains("当前时间"));
        System.out.println("获取当前时间成功");
    }

    @Test
    @DisplayName("测试数学计算")
    void testCalculate() {
        var result = sharedTools.calculate("2+3*4");
        assertTrue(isSuccess(result));
        assertTrue(content(result).contains("14"));
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

    // ==================== Helper Methods ====================

    /**
     * 创建模拟玩家列表
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