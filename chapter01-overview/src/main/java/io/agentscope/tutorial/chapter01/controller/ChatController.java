/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.agentscope.tutorial.chapter01.controller;

import io.agentscope.tutorial.chapter01.service.AgentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * REST API 控制器.
 *
 * <p>提供聊天相关的 REST API 接口：
 * <ul>
 *   <li>POST /api/chat - 发送消息并获取 Agent 回复</li>
 *   <li>GET /api/health - 健康检查接口</li>
 * </ul>
 *
 * <p>API 调用示例：
 * <pre>{@code
 * # 发送消息
 * curl -X POST http://localhost:8080/api/chat \
 *   -H "Content-Type: application/json" \
 *   -d '{"message": "你好，请介绍一下你自己"}'
 *
 * # 健康检查
 * curl http://localhost:8080/api/health
 * }</pre>
 *
 * @see AgentService
 * @see ChatRequest
 * @see ChatResponse
 */
@RestController
@RequestMapping("/api")
@Slf4j
public class ChatController {

    /** Agent 服务 */
    private final AgentService agentService;

    /**
     * 构造函数.
     *
     * @param agentService Agent 服务实例
     */
    public ChatController(AgentService agentService) {
        this.agentService = agentService;
    }

    /**
     * 发送消息并获取 Agent 回复.
     *
     * <p>请求方法：POST
     * <p>请求路径：/api/chat
     * <p>请求内容类型：application/json
     *
     * @param request 聊天请求，包含用户消息
     * @return Agent 的回复
     */
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@Validated @RequestBody ChatRequest request) {
        log.info("收到聊天请求: {}", request.getMessage());

        ChatResponse response = agentService.chat(request);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 流式聊天接口.
     *
     * <p>请求方法：POST
     * <p>请求路径：/api/chat/stream
     *
     * @param request 聊天请求
     * @return Agent 回复的流
     */
    @PostMapping("/chat/stream")
    public ResponseEntity<String> chatStream(@Validated @RequestBody ChatRequest request) {
        log.info("收到流式聊天请求: {}", request.getMessage());

        return ResponseEntity.ok()
                .header("Content-Type", "text/plain;charset=UTF-8")
                .body(agentService.chatStream(request).block());
    }

    /**
     * 健康检查接口.
     *
     * <p>请求方法：GET
     * <p>请求路径：/api/health
     *
     * @return 健康状态
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}