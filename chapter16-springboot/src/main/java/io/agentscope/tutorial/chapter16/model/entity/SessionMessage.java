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
package io.agentscope.tutorial.chapter16.model.entity;

import lombok.AllArgs构造函数;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgs构造函数;

import java.time.Instant;

/**
 * 会话消息实体.
 *
 * <p>与数据库表 session_messages 对应。
 * 注意：这里不使用 JPA 实体，而是通过 JdbcTemplate 手动操作 SQL，
 * 因此此类仅作为数据传输对象（DTO）。
 */
@Data
@Builder
@NoArgs构造函数
@AllArgs构造函数
public class SessionMessage {

    /** 消息 ID（自增） */
    private Long id;

    /** 所属会话 ID */
    private String sessionId;

    /** 消息角色：system | user | assistant | tool */
    private String role;

    /** 消息内容 */
    private String content;

    /** 工具调用 ID（仅 tool 角色） */
    private String toolCallId;

    /** 工具名称（仅 tool 角色） */
    private String toolName;

    /** 创建时间 */
    private Instant createdAt;

    /**
     * 判断是否为用户消息
     */
    public boolean isUserMessage() {
        return "user".equalsIgnoreCase(role);
    }

    /**
     * 判断是否为助手消息
     */
    public boolean isAssistantMessage() {
        return "assistant".equalsIgnoreCase(role);
    }

    /**
     * 判断是否为工具调用消息
     */
    public boolean isToolMessage() {
        return "tool".equalsIgnoreCase(role);
    }
}