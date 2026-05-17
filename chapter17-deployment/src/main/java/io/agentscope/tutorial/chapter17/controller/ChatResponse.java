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
package io.agentscope.tutorial.chapter17.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgs构造函数;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgs构造函数;

import java.time.Instant;

/**
 * 聊天响应 DTO.
 *
 * @author AgentScope Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgs构造函数
@AllArgs构造函数
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatResponse {

    /**
     * 是否成功.
     */
    private boolean success;

    /**
     * 响应消息.
     */
    private String message;

    /**
     * 会话 ID.
     */
    private String sessionId;

    /**
     * 处理时间（毫秒）.
     */
    private Long durationMs;

    /**
     * 时间戳.
     */
    private Instant timestamp;

    /**
     * 错误信息（如果有）.
     */
    private String error;

    /**
     * 创建成功响应.
     */
    public static ChatResponse success(String message) {
        return ChatResponse.builder()
            .success(true)
            .message(message)
            .timestamp(Instant.now())
            .build();
    }

    /**
     * 创建成功响应（包含会话 ID）.
     */
    public static ChatResponse success(String message, String sessionId, long durationMs) {
        return ChatResponse.builder()
            .success(true)
            .message(message)
            .sessionId(sessionId)
            .durationMs(durationMs)
            .timestamp(Instant.now())
            .build();
    }

    /**
     * 创建错误响应.
     */
    public static ChatResponse error(String errorMessage) {
        return ChatResponse.builder()
            .success(false)
            .error(errorMessage)
            .timestamp(Instant.now())
            .build();
    }
}