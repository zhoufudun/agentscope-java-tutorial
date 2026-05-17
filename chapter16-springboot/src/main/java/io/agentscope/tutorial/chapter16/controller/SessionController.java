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
package io.agentscope.tutorial.chapter16.controller;

import io.agentscope.tutorial.chapter16.model.entity.SessionMessage;
import io.agentscope.tutorial.chapter16.service.ChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 会话管理控制器.
 *
 * <p>提供会话的 CRUD 操作：
 * <ul>
 *   <li>GET /api/v1/agent/sessions - 获取所有会话</li>
 *   <li>POST /api/v1/agent/sessions - 创建新会话</li>
 *   <li>GET /api/v1/agent/sessions/{id} - 获取会话详情</li>
 *   <li>DELETE /api/v1/agent/sessions/{id} - 删除会话</li>
 *   <li>POST /api/v1/agent/sessions/{id}/reset - 重置会话</li>
 *   <li>GET /api/v1/agent/sessions/{id}/messages - 获取会话消息历史</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/agent/sessions")
@Slf4j
public class SessionController {

    private final ChatService chatService;

    public SessionController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * 获取所有会话列表
     *
     * @return 会话列表
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllSessions() {
        List<Map<String, Object>> sessions = chatService.getAllSessions().stream()
                .map(info -> Map.<String, Object>of(
                        "id", info.id(),
                        "name", info.name(),
                        "createdAt", info.createdAt().toString(),
                        "updatedAt", info.updatedAt().toString()
                ))
                .toList();
        return ResponseEntity.ok(sessions);
    }

    /**
     * 创建新会话
     *
     * @param body 请求体，包含会话名称（可选）
     * @return 创建的会话信息
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createSession(@RequestBody(required = false) Map<String, String> body) {
        String name = (body != null && body.containsKey("name")) ? body.get("name") : "新会话";
        String sessionId = chatService.createSession(name);
        return ResponseEntity.ok(Map.of(
                "id", sessionId,
                "name", name,
                "message", "会话创建成功"
        ));
    }

    /**
     * 获取会话详情
     *
     * @param sessionId 会话 ID
     * @return 会话详情
     */
    @GetMapping("/{sessionId}")
    public ResponseEntity<Map<String, Object>> getSession(@PathVariable String sessionId) {
        return chatService.getSessionName(sessionId)
                .map(name -> ResponseEntity.ok(Map.<String, Object>of(
                        "id", sessionId,
                        "name", name,
                        "messageCount", chatService.getMessageCount(sessionId)
                )))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 删除会话
     *
     * @param sessionId 会话 ID
     * @return 删除结果
     */
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Map<String, Object>> deleteSession(@PathVariable String sessionId) {
        boolean deleted = chatService.deleteSession(sessionId);
        return ResponseEntity.ok(Map.of(
                "sessionId", sessionId,
                "deleted", deleted
        ));
    }

    /**
     * 重置会话（清除消息历史）
     *
     * @param sessionId 会话 ID
     * @return 重置结果
     */
    @PostMapping("/{sessionId}/reset")
    public ResponseEntity<Map<String, Object>> resetSession(@PathVariable String sessionId) {
        chatService.resetSession(sessionId);
        return ResponseEntity.ok(Map.of(
                "sessionId", sessionId,
                "messageCount", 0
        ));
    }

    /**
     * 获取会话消息历史
     *
     * @param sessionId 会话 ID
     * @return 消息列表
     */
    @GetMapping("/{sessionId}/messages")
    public ResponseEntity<List<Map<String, Object>>> getMessages(@PathVariable String sessionId) {
        List<SessionMessage> messages = chatService.getMessages(sessionId);
        List<Map<String, Object>> result = messages.stream()
                .map(msg -> Map.<String, Object>of(
                        "id", msg.getId(),
                        "role", msg.getRole(),
                        "content", msg.getContent(),
                        "toolCallId", msg.getToolCallId() != null ? msg.getToolCallId() : "",
                        "toolName", msg.getToolName() != null ? msg.getToolName() : "",
                        "createdAt", msg.getCreatedAt() != null ? msg.getCreatedAt().toString() : ""
                ))
                .toList();
        return ResponseEntity.ok(result);
    }
}