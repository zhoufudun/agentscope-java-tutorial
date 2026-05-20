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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 异步聊天响应 DTO.
 *
 * @author AgentScope Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AsyncChatResponse {

    /**
     * 任务 ID.
     */
    private String taskId;

    /**
     * 任务状态.
     */
    private String status;

    /**
     * 状态消息.
     */
    private String message;

    /**
     * 预计完成时间（秒）.
     */
    private Integer estimatedCompletionSeconds;

    /**
     * 轮询 URL.
     */
    private String pollUrl;

    /**
     * 时间戳.
     */
    private Instant timestamp;

    /**
     * 创建已接受响应.
     */
    public static AsyncChatResponse accepted(String taskId) {
        return AsyncChatResponse.builder()
            .taskId(taskId)
            .status("ACCEPTED")
            .message("请求已接受，正在处理中")
            .pollUrl("/api/v1/chat/async/status/" + taskId)
            .estimatedCompletionSeconds(30)
            .timestamp(Instant.now())
            .build();
    }

    /**
     * 创建处理中响应.
     */
    public static AsyncChatResponse processing(String taskId) {
        return AsyncChatResponse.builder()
            .taskId(taskId)
            .status("PROCESSING")
            .message("请求正在处理中")
            .timestamp(Instant.now())
            .build();
    }

    /**
     * 创建完成响应.
     */
    public static AsyncChatResponse completed(String taskId, String result) {
        return AsyncChatResponse.builder()
            .taskId(taskId)
            .status("COMPLETED")
            .message(result)
            .timestamp(Instant.now())
            .build();
    }

    /**
     * 创建失败响应.
     */
    public static AsyncChatResponse failed(String taskId, String error) {
        return AsyncChatResponse.builder()
            .taskId(taskId)
            .status("FAILED")
            .message(error)
            .timestamp(Instant.now())
            .build();
    }
}