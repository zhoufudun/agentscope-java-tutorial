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

import io.agentscope.tutorial.chapter19.assistant.EnterpriseAssistantAgent;
import io.agentscope.tutorial.chapter19.assistant.knowledge.KnowledgeAgent;
import io.agentscope.tutorial.chapter19.assistant.report.ReportAgent;
import io.agentscope.tutorial.chapter19.assistant.schedule.ScheduleAgent;
import io.agentscope.tutorial.chapter19.boba.service.BobaOrderService;
import io.agentscope.tutorial.chapter19.boba.supervisor.OrderSupervisorAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 综合案例 REST 控制器
 *
 * <p>提供三个综合案例的统一 HTTP 接口：
 * <ul>
 *   <li>POST /api/boba/order - 奶茶店点单</li>
 *   <li>GET /api/boba/menu - 获取菜单</li>
 *   <li>POST /api/werewolf/start - 开始狼人杀游戏</li>
 *   <li>POST /api/assistant/chat - 企业助手聊天</li>
 *   <li>GET /api/assistant/knowledge - 知识库查询</li>
 *   <li>POST /api/assistant/schedule - 创建日程</li>
 *   <li>GET /api/assistant/report - 生成报表</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 * @since 1.0
 */
@RestController
@RequestMapping("/api")
public class Chapter19Controller {

    private static final Logger logger = LoggerFactory.getLogger(Chapter19Controller.class);

    // ==================== 奶茶店案例 ====================

    @Autowired
    private OrderSupervisorAgent orderSupervisorAgent;

    @Autowired
    private BobaOrderService bobaOrderService;

    // ==================== 狼人杀案例 ====================
    // 狼人杀相关的 HTTP 接口由专用的 GameController（@RequestMapping("/api/werewolf")）统一提供，
    // 包含 start/status/stream/rules 等完整生命周期，这里不再重复映射，避免 Ambiguous mapping。

    // ==================== 企业助手案例 ====================

    @Autowired
    private EnterpriseAssistantAgent enterpriseAssistantAgent;

    @Autowired
    private KnowledgeAgent knowledgeAgent;

    @Autowired
    private ScheduleAgent scheduleAgent;

    @Autowired
    private ReportAgent reportAgent;

    // ==================== 奶茶店 API ====================

    /**
     * 奶茶店点单
     */
    @PostMapping("/boba/order")
    public ResponseEntity<String> placeOrder(@RequestBody Map<String, String> request) {
        String drink = request.getOrDefault("drink", "");
        String sweetness = request.getOrDefault("sweetness", "正常糖");
        String ice = request.getOrDefault("ice", "正常冰");
        String topping = request.getOrDefault("topping", "");

        logger.info("收到奶茶订单: {} {} {} {}", drink, sweetness, ice, topping);

        String orderMessage = String.format("我要一杯%s，%s，%s%s",
                drink, sweetness, ice,
                topping.isEmpty() ? "" : "，" + topping);

        try {
            String result = orderSupervisorAgent.processOrder(orderMessage);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("处理订单失败", e);
            return ResponseEntity.internalServerError()
                    .body("处理订单失败：" + e.getMessage());
        }
    }

    /**
     * 获取奶茶菜单
     */
    @GetMapping("/boba/menu")
    public ResponseEntity<String> getMenu() {
        return ResponseEntity.ok(bobaOrderService.getMenu());
    }

    /**
     * 计算订单价格
     */
    @PostMapping("/boba/price")
    public ResponseEntity<String> calculatePrice(@RequestBody Map<String, Object> request) {
        String drink = (String) request.getOrDefault("drink", "");
        @SuppressWarnings("unchecked")
        java.util.List<String> toppings = (java.util.List<String>) request.getOrDefault("toppings", java.util.List.of());

        double price = bobaOrderService.calculatePrice(drink, toppings);
        return ResponseEntity.ok(String.format("%.2f 元", price));
    }

    // ==================== 狼人杀 API ====================
    // 狼人杀接口（POST /api/werewolf/start、GET /api/werewolf/rules 等）由 GameController 提供，
    // 详见 io.agentscope.tutorial.chapter19.werewolf.controller.GameController。

    // ==================== 企业助手 API ====================

    /**
     * 企业助手聊天
     */
    @PostMapping("/assistant/chat")
    public ResponseEntity<String> chat(@RequestBody Map<String, String> request) {
        String message = request.getOrDefault("message", "");

        if (message.isEmpty()) {
            return ResponseEntity.badRequest().body("消息不能为空");
        }

        logger.info("收到助手请求: {}", message);

        try {
            String result = enterpriseAssistantAgent.processRequest(message);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("处理请求失败", e);
            return ResponseEntity.internalServerError()
                    .body("处理请求失败：" + e.getMessage());
        }
    }

    /**
     * 知识库查询
     */
    @GetMapping("/assistant/knowledge")
    public ResponseEntity<String> queryKnowledge(
            @RequestParam(defaultValue = "") String query) {

        if (query.isEmpty()) {
            return ResponseEntity.ok(knowledgeAgent.getFaq());
        }

        return ResponseEntity.ok(knowledgeAgent.search(query));
    }

    /**
     * 创建日程
     */
    @PostMapping("/assistant/schedule")
    public ResponseEntity<String> createSchedule(@RequestBody Map<String, String> request) {
        String title = request.getOrDefault("title", "");
        String description = request.getOrDefault("description", "");
        String startTime = request.getOrDefault("startTime", "");
        String endTime = request.getOrDefault("endTime", "");

        if (title.isEmpty() || startTime.isEmpty() || endTime.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body("标题、开始时间、结束时间不能为空");
        }

        logger.info("创建日程: {}", title);

        try {
            String result = enterpriseAssistantAgent.createSchedule(
                    title, description, startTime, endTime);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("创建日程失败", e);
            return ResponseEntity.internalServerError()
                    .body("创建日程失败：" + e.getMessage());
        }
    }

    /**
     * 查询日程
     */
    @GetMapping("/assistant/schedule")
    public ResponseEntity<String> getSchedules(
            @RequestParam(defaultValue = "") String date) {
        return ResponseEntity.ok(scheduleAgent.querySchedules("current_user", date));
    }

    /**
     * 生成报表
     */
    @GetMapping("/assistant/report")
    public ResponseEntity<String> generateReport(
            @RequestParam(defaultValue = "sales") String type,
            @RequestParam(defaultValue = "last_month") String dataRange) {

        logger.info("生成报表: {} - {}", type, dataRange);

        try {
            String report = reportAgent.generateReport(type, dataRange);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            logger.error("生成报表失败", e);
            return ResponseEntity.internalServerError()
                    .body("生成报表失败：" + e.getMessage());
        }
    }

    /**
     * 生成图表
     */
    @GetMapping("/assistant/chart")
    public ResponseEntity<String> generateChart(
            @RequestParam(defaultValue = "bar") String chartType,
            @RequestParam(defaultValue = "sales") String dataType) {

        try {
            String chart = reportAgent.generateChart(chartType, dataType);
            return ResponseEntity.ok(chart);
        } catch (Exception e) {
            logger.error("生成图表失败", e);
            return ResponseEntity.internalServerError()
                    .body("生成图表失败：" + e.getMessage());
        }
    }

    /**
     * 获取助手状态
     */
    @GetMapping("/assistant/status")
    public ResponseEntity<String> getAssistantStatus() {
        StringBuilder status = new StringBuilder();
        status.append("===== 企业智能助手状态 =====\n\n");

        status.append("【主助手】\n");
        status.append(enterpriseAssistantAgent.getStatus()).append("\n\n");

        status.append("【知识库】\n");
        status.append("- 状态：正常\n");
        status.append("- FAQ：可用\n\n");

        status.append("【日程管理】\n");
        status.append("- 状态：正常\n");
        status.append(scheduleAgent.querySchedules("current_user", null)).append("\n\n");

        status.append("【报表生成】\n");
        status.append("- 状态：正常\n");
        status.append("- 支持类型：sales, user, performance\n");

        return ResponseEntity.ok(status.toString());
    }
}