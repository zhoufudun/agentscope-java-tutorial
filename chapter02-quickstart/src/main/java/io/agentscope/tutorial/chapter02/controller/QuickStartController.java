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
package io.agentscope.tutorial.chapter02.controller;

import io.agentscope.core.ReActAgent;
import io.agentscope.tutorial.chapter02.service.QuickStartService;
import lombok.RequiredArgs构造函数;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.Map;

/**
 * 快速开始控制器。
 *
 * <p>提供 REST API 端点，支持对话和流式响应。
 *
 * <p>API 端点：
 * <ul>
 *   <li>POST /chat - 发送对话消息（非流式）</li>
 *   <li>POST /chat/stream - 流式对话（SSE）</li>
 *   <li>GET /health - 健康检查</li>
 * </ul>
 *
 * @author AgentScope Team
 * @since 1.0
 */
@Slf4j
@RestController
@RequestMapping("/")
@RequiredArgs构造函数
public class QuickStartController {

    /**
     * 快速开始服务。
     */
    private final QuickStartService quickStartService;

    /**
     * Agent 实例（单例）。
     */
    private ReActAgent agent;

    /**
     * 获取或创建 Agent 实例。
     *
     * @return Agent 实例
     */
    private ReActAgent getAgent() {
        if (agent == null) {
            agent = quickStartService.createAgent();
            log.info("Agent 实例已创建: {}", agent);
        }
        return agent;
    }

    /**
     * 健康检查端点。
     *
     * @return 健康状态
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("service", "chapter02-quickstart");
        result.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(result);
    }

    /**
     * 发送对话消息（非流式）。
     *
     * <p>请求体：
     * <pre>{@code
     * {
     *   "message": "你好，请介绍一下你自己"
     * }
     * }</pre>
     *
     * <p>响应：
     * <pre>{@code
     * {
     *   "success": true,
     *   "response": "你好！我是基于通义千问大模型的AI助手..."
     * }
     * }</pre>
     *
     * @param request 请求体
     * @return 对话响应
     */
    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        if (message == null || message.trim().isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "消息不能为空");
            return ResponseEntity.badRequest().body(error);
        }

        try {
            ReActAgent agentInstance = getAgent();
            String response = quickStartService.sendMessage(agentInstance, message);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("response", response);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("处理对话请求失败", e);

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());

            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 发送对话消息（流式，SSE）。
     *
     * <p>使用 Server-Sent Events 进行流式响应。
     *
     * <p>请求体：
     * <pre>{@code
     * {
     *   "message": "你好，请介绍一下你自己"
     * }
     * }</pre>
     *
     * @param request 请求体
     * @return SSE Emitter
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        if (message == null || message.trim().isEmpty()) {
            // 返回错误 emitter
            SseEmitter emitter = new SseEmitter();
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data("{\"error\": \"消息不能为空\"}"));
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
            return emitter;
        }

        // 创建 SSE Emitter，超时时间 60 秒
        SseEmitter emitter = new SseEmitter(60_000L);

        try {
            ReActAgent agentInstance = getAgent();

            // 订阅流式响应
            quickStartService.streamMessage(agentInstance, message)
                    .subscribe(
                            // 处理每个响应片段
                            content -> {
                                try {
                                    emitter.send(SseEmitter.event()
                                            .name("message")
                                            .data(content));
                                } catch (Exception e) {
                                    log.warn("发送 SSE 事件失败", e);
                                    emitter.completeWithError(e);
                                }
                            },
                            // 处理错误
                            error -> {
                                log.error("流式响应错误", error);
                                try {
                                    emitter.send(SseEmitter.event()
                                            .name("error")
                                            .data("{\"error\": \"" + error.getMessage() + "\"}"));
                                } catch (Exception e) {
                                    // ignore
                                }
                                emitter.completeWithError(error);
                            },
                            // 完成
                            () -> {
                                try {
                                    emitter.send(SseEmitter.event()
                                            .name("end")
                                            .data(""));
                                    emitter.complete();
                                } catch (Exception e) {
                                    emitter.completeWithError(e);
                                }
                            });

        } catch (Exception e) {
            log.error("启动流式响应失败", e);
            emitter.completeWithError(e);
        }

        return emitter;
    }

    /**
     * 创建新 Agent 会话。
     *
     * <p>重置 Agent 实例，开始新的对话。
     *
     * @param name Agent 名称（可选）
     * @param sysPrompt 系统提示词（可选）
     * @return 创建结果
     */
    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> reset(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String sysPrompt) {

        // 重置 Agent
        agent = quickStartService.createAgent(name, sysPrompt);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Agent 已重置");

        return ResponseEntity.ok(result);
    }
}