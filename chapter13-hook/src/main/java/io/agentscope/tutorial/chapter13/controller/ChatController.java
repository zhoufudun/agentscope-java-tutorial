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
package io.agentscope.tutorial.chapter13.controller;

import io.agentscope.core.message.Msg;
import io.agentscope.tutorial.chapter13.hook.MetricsHook;
import io.agentscope.tutorial.chapter13.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Chat 控制器 - 提供 Agent 对话和指标查询接口.
 *
 * <p>提供以下 API 端点：
 * <ul>
 *   <li>POST /api/chat/call - 调用 Agent</li>
 *   <li>POST /api/chat/call-async - 异步调用 Agent</li>
 *   <li>GET /api/chat/metrics - 获取性能指标</li>
 *   <li>GET /api/chat/info - 获取 Agent 信息</li>
 *   <li>POST /api/chat/reset - 重置指标</li>
 *   <li>GET /api/chat/health - 健康检查</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * 同步调用 Agent.
     *
     * @param request 请求体，包含 "message" 字段
     * @return Agent 响应
     */
    @PostMapping("/call")
    public ResponseEntity<Map<String, Object>> callAgent(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        if (message == null || message.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "message is required"));
        }

        try {
            long startTime = System.currentTimeMillis();
            Msg response = chatService.call(message);
            long duration = System.currentTimeMillis() - startTime;

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "response", response.getTextContent(),
                    "agent", chatService.getAgentName(),
                    "durationMs", duration
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "success", false,
                            "error", e.getMessage()
                    ));
        }
    }

    /**
     * 异步调用 Agent.
     *
     * @param request 请求体，包含 "message" 字段
     * @return 202 Accepted，异步处理中
     */
    @PostMapping("/call-async")
    public ResponseEntity<Map<String, Object>> callAgentAsync(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        if (message == null || message.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "message is required"));
        }

        // 异步处理（实际应用中应该返回任务 ID，这里简化处理）
        chatService.callAsync(message)
                .subscribe(
                        response -> {
                            // 可以在这里添加异步完成后的处理逻辑
                            System.out.println("异步调用完成: " + response.getTextContent());
                        },
                        error -> {
                            System.err.println("异步调用失败: " + error.getMessage());
                        }
                );

        return ResponseEntity.accepted()
                .body(Map.of(
                        "message", "异步调用已接受",
                        "input", truncate(message, 100)
                ));
    }

    /**
     * 获取性能指标.
     */
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getMetrics() {
        MetricsHook metrics = chatService.getMetrics();

        return ResponseEntity.ok(Map.of(
                "totalCalls", metrics.getTotalCallCount(),
                "totalTimeMs", metrics.getTotalCallTimeMs(),
                "averageTimeMs", metrics.getAverageCallTimeMs(),
                "totalErrors", metrics.getTotalErrorCount(),
                "toolStats", metrics.getToolCallStats()
        ));
    }

    /**
     * 获取 Agent 信息.
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getAgentInfo() {
        return ResponseEntity.ok(Map.of(
                "name", chatService.getAgentName(),
                "healthy", chatService.isHealthy(),
                "hookInfo", chatService.getHookInfo()
        ));
    }

    /**
     * 重置性能指标.
     */
    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> resetMetrics() {
        chatService.resetMetrics();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "指标已重置"
        ));
    }

    /**
     * 健康检查端点.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        boolean healthy = chatService.isHealthy();
        return ResponseEntity.ok(Map.of(
                "status", healthy ? "UP" : "DOWN",
                "agent", chatService.getAgentName()
        ));
    }

    /**
     * 截断过长文本.
     */
    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }
}