package io.agentscope.tutorial.chapter08.controller;

import io.agentscope.tutorial.chapter08.model.ServiceResult;
import io.agentscope.tutorial.chapter08.service.MultiAgentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 聊天控制器
 *
 * 提供 REST API 接口，用于与多代理系统交互
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final MultiAgentService multiAgentService;

    public ChatController(MultiAgentService multiAgentService) {
        this.multiAgentService = multiAgentService;
    }

    /**
     * 处理用户消息
     *
     * @param request 请求体，包含 message 字段
     * @return 服务结果
     */
    @PostMapping("/message")
    public Map<String, Object> handleMessage(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        if (message == null || message.isBlank()) {
            return Map.of(
                "success", false,
                "error", "消息不能为空"
            );
        }

        log.info("收到消息：{}", message);

        try {
            ServiceResult result = multiAgentService.process(message);

            return Map.of(
                "success", true,
                "query", result.originalQuery(),
                "category", result.classify().category(),
                "categoryReason", result.classify().reason(),
                "answer", result.finalAnswer(),
                "agentsInvoked", result.agentsInvoked()
            );

        } catch (Exception e) {
            log.error("处理消息失败", e);
            return Map.of(
                "success", false,
                "error", "服务暂时不可用，请稍后重试"
            );
        }
    }

    /**
     * 处理订单查询
     *
     * @param request 请求体，包含 message 字段
     * @return 服务结果
     */
    @PostMapping("/order")
    public Map<String, Object> handleOrderQuery(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        if (message == null || message.isBlank()) {
            return Map.of(
                "success", false,
                "error", "消息不能为空"
            );
        }

        log.info("收到订单查询：{}", message);

        try {
            ServiceResult result = multiAgentService.processOrderQuery(message);

            return Map.of(
                "success", true,
                "query", result.originalQuery(),
                "answer", result.finalAnswer(),
                "pipelineOutputs", result.pipelineOutputs()
            );

        } catch (Exception e) {
            log.error("处理订单查询失败", e);
            return Map.of(
                "success", false,
                "error", "订单查询服务暂时不可用"
            );
        }
    }

    /**
     * 处理综合查询
     *
     * @param request 请求体，包含 message 字段
     * @return 服务结果
     */
    @PostMapping("/query")
    public Map<String, Object> handleGeneralQuery(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        if (message == null || message.isBlank()) {
            return Map.of(
                "success", false,
                "error", "消息不能为空"
            );
        }

        log.info("收到综合查询：{}", message);

        try {
            ServiceResult result = multiAgentService.processGeneralQuery(message);

            return Map.of(
                "success", true,
                "query", result.originalQuery(),
                "answer", result.finalAnswer(),
                "pipelineOutputs", result.pipelineOutputs()
            );

        } catch (Exception e) {
            log.error("处理综合查询失败", e);
            return Map.of(
                "success", false,
                "error", "综合查询服务暂时不可用"
            );
        }
    }

    /**
     * 并行处理多个查询
     *
     * @param request 请求体，包含 queries 字段（List<String>）
     * @return 多个服务结果
     */
    @PostMapping("/batch")
    public Map<String, Object> handleBatch(@RequestBody Map<String, List<String>> request) {
        List<String> queries = request.get("queries");
        if (queries == null || queries.isEmpty()) {
            return Map.of(
                "success", false,
                "error", "查询列表不能为空"
            );
        }

        log.info("收到批量查询，数量：{}", queries.size());

        try {
            List<ServiceResult> results = multiAgentService.processParallel(queries);

            List<Map<String, Object>> resultList = results.stream()
                .map(r -> Map.<String, Object>of(
                    "query", r.originalQuery(),
                    "category", r.classify().category(),
                    "answer", r.finalAnswer()
                ))
                .toList();

            return Map.of(
                "success", true,
                "count", results.size(),
                "results", resultList
            );

        } catch (Exception e) {
            log.error("处理批量查询失败", e);
            return Map.of(
                "success", false,
                "error", "批量处理服务暂时不可用"
            );
        }
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
            "status", "ok",
            "service", "multi-agent-chat",
            "version", "1.0.0"
        );
    }
}