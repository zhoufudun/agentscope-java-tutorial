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
package io.agentscope.tutorial.chapter04.controller;

import io.agentscope.tutorial.chapter04.service.AgentService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Agent REST API 控制器
 *
 * 提供以下 API 端点：
 * - POST /api/agent/chat - 发送消息并获取响应（同步）
 * - POST /api/agent/chat/stream - 流式响应
 * - GET /api/agent/history - 获取对话历史
 * - DELETE /api/agent/history - 清除对话历史
 * - GET /api/agent/status - 获取 Agent 状态
 * - POST /api/agent/interrupt - 中断 Agent 执行
 *
 * @author AgentScope Tutorial
 */
@RestController
@RequestMapping("/api/agent")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final AgentService agentService;

    public ChatController(AgentService agentService) {
        this.agentService = agentService;
    }

    /**
     * 发送消息并获取响应（同步）
     */
    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody ChatRequest request) {
        log.info("收到聊天请求: {}", truncate(request.message(), 50));

        String response = agentService.processMessage(request.message()).block();

        return ResponseEntity.ok(Map.of(
                "response", response != null ? response : "无响应",
                "agentName", agentService.getAgent().getName(),
                "messageCount", agentService.getAgent().getAgentState().getContext().size()
        ));
    }

    /**
     * 发送消息并获取流式响应
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Map<String, Object>> chatStream(@RequestBody ChatRequest request) {
        log.info("收到流式聊天请求: {}", truncate(request.message(), 50));

        return agentService.processStreamMessage(request.message());
    }

    /**
     * 获取对话历史
     */
    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> getHistory() {
        var messages = agentService.getHistory();

        return ResponseEntity.ok(Map.of(
                "messages", messages,
                "count", messages.size()
        ));
    }

    /**
     * 清除对话历史
     */
    @DeleteMapping("/history")
    public ResponseEntity<Map<String, String>> clearHistory() {
        agentService.clearHistory();

        return ResponseEntity.ok(Map.of(
                "message", "对话历史已清除"
        ));
    }

    /**
     * 获取 Agent 状态
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(agentService.getStatus());
    }

    /**
     * 中断 Agent 执行
     */
    @PostMapping("/interrupt")
    public ResponseEntity<Map<String, String>> interrupt() {
        log.info("收到中断请求");
        agentService.interrupt();
        return ResponseEntity.ok(Map.of(
                "message", "中断请求已发送"
        ));
    }

    // ========== 请求 DTO ==========

    public record ChatRequest(
            @NotBlank(message = "消息不能为空")
            @Size(max = 5000, message = "消息长度不能超过5000字符")
            String message
    ) {}

    // ========== 私有辅助方法 ==========

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }
}