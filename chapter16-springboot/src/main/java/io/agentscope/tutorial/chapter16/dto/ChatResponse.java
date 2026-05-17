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
package io.agentscope.tutorial.chapter16.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgs构造函数;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgs构造函数;

import java.time.Instant;

/**
 * 聊天响应 DTO.
 *
 * <p>包含 Agent 回复及元数据信息。
 */
@Data
@Builder
@NoArgs构造函数
@AllArgs构造函数
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatResponse {

    /** 会话 ID */
    private String sessionId;

    /** Agent 回复内容 */
    private String message;

    /** 使用的模型名称 */
    private String model;

    /** 估算的 token 数量 */
    private Integer tokenCount;

    /** 请求耗时（毫秒） */
    private Long duration;

    /** 完成原因：stop | length | content_filter | tool_calls */
    private String finishReason;

    /** 响应时间戳 */
    private Instant timestamp;

    /** 工具调用（如果有） */
    private Object toolCalls;

    /** 是否成功 */
    private boolean success;

    /** 错误信息（如果失败） */
    private String error;

    /**
     * 创建成功响应
     */
    public static ChatResponse success(String sessionId, String message, long duration) {
        return ChatResponse.builder()
                .sessionId(sessionId)
                .message(message)
                .tokenCount(estimateTokenCount(message))
                .duration(duration)
                .finishReason("stop")
                .timestamp(Instant.now())
                .success(true)
                .build();
    }

    /**
     * 创建错误响应
     */
    public static ChatResponse error(String errorMessage) {
        return ChatResponse.builder()
                .error(errorMessage)
                .success(false)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * 估算 token 数量（简化估算：中文 ~2字符/token，英文 ~4字符/token）
     */
    private static int estimateTokenCount(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return (int) (text.length() / 2.5);
    }
}