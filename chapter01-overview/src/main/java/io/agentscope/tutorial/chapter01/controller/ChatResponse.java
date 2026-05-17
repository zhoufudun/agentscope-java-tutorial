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

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgs构造函数;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgs构造函数;

/**
 * 聊天响应类.
 *
 * <p>用于返回 Agent 的回复给客户端。
 *
 * <p>成功响应示例：
 * <pre>{@code
 * {
 *   "success": true,
 *   "message": "你好！我是 AgentScope Java 助手..."
 * }
 * }</pre>
 *
 * <p>错误响应示例：
 * <pre>{@code
 * {
 *   "success": false,
 *   "message": "Agent 处理失败: ..."
 * }
 * }</pre>
 *
 * @see ChatRequest
 */
@Data
@Builder
@NoArgs构造函数
@AllArgs构造函数
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatResponse {

    /**
     * 请求是否成功.
     */
    private boolean success;

    /**
     * 响应消息（成功时为 Agent 回复，失败时为错误信息）.
     */
    private String message;

    /**
     * 时间戳（毫秒）.
     */
    private Long timestamp;

    /**
     * 创建一个成功的响应.
     *
     * @param message Agent 回复内容
     * @return 成功的响应对象
     */
    public static ChatResponse success(String message) {
        return ChatResponse.builder()
                .success(true)
                .message(message)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建一个失败的响应.
     *
     * @param message 错误信息
     * @return 失败的响应对象
     */
    public static ChatResponse error(String message) {
        return ChatResponse.builder()
                .success(false)
                .message(message)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}