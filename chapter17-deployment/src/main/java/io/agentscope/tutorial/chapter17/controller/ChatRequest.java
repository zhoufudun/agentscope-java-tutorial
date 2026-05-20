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

import io.agentscope.core.ReActAgent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 聊天请求 DTO.
 *
 * @author AgentScope Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    /**
     * 用户 ID.
     */
    @NotBlank(message = "userId 不能为空")
    private String userId;

    /**
     * 会话 ID.
     */
    @NotBlank(message = "sessionId 不能为空")
    private String sessionId;

    /**
     * 用户消息.
     */
    @NotBlank(message = "message 不能为空")
    @Size(max = 10000, message = "message 长度不能超过 10000 字符")
    private String message;

    /**
     * ReActAgent 实例（由框架自动注入）.
     */
    private ReActAgent agent;

    /**
     * 额外的配置选项.
     */
    private ChatOptions options;

    /**
     * 聊天选项.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatOptions {
        /**
         * 最大 token 数.
         */
        private Integer maxTokens;

        /**
         * 温度参数.
         */
        private Double temperature;

        /**
         * 是否启用流式输出.
         */
        private Boolean stream;
    }
}