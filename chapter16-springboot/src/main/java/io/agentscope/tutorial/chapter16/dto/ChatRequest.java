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

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgs构造函数;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgs构造函数;

import java.util.List;

/**
 * 聊天请求 DTO.
 *
 * <p>用于 /api/v1/agent/chat 端点。
 * 支持传入消息历史以进行多轮对话。
 *
 * <p>示例请求：
 * <pre>{@code
 * {
 *   "messages": [
 *     {"role": "system", "content": "你是一个有帮助的助手"},
 *     {"role": "user", "content": "你好，请介绍一下自己"}
 *   ]
 * }
 * }</pre>
 */
@Data
@Builder
@NoArgs构造函数
@AllArgs构造函数
public class ChatRequest {

    /** 消息列表（包含角色和内容） */
    @NotEmpty(message = "消息列表不能为空")
    private List<Message> messages;

    /** 模型名称（可选，用于覆盖默认配置） */
    private String model;

    /** 温度参数（控制随机性，0-2） */
    private Double temperature;

    /** 最大生成 token 数 */
    private Integer maxTokens;

    /**
     * 消息结构
     */
    @Data
    @Builder
    @NoArgs构造函数
    @AllArgs构造函数
    public static class Message {
        /** 角色：system | user | assistant | tool */
        private String role;

        /** 消息内容 */
        private String content;

        /** 工具调用 ID（仅 tool 角色使用） */
        private String toolCallId;

        /** 工具名称（仅 tool 角色使用） */
        private String toolName;
    }
}