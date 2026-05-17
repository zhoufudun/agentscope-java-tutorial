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
package io.agentscope.tutorial.chapter16.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;

/**
 * H2 会话管理器.
 *
 * <p>直接操作 H2 数据库，执行会话相关的 SQL 操作。
 * 封装了基础的 CRUD 操作，供 ChatService 调用。
 */
@Component
public class H2SessionManager {

    private static final Logger log = LoggerFactory.getLogger(H2SessionManager.class);

    private final JdbcTemplate jdbcTemplate;

    public H2SessionManager(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 创建会话记录
     *
     * @param id   会话 ID
     * @param name 会话名称
     */
    public void createSession(String id, String name) {
        jdbcTemplate.update(
            "INSERT INTO agent_sessions (id, name, created_at, updated_at) VALUES (?, ?, ?, ?)",
            id, name, Timestamp.from(Instant.now()), Timestamp.from(Instant.now())
        );
        log.debug("创建会话记录: id={}, name={}", id, name);
    }

    /**
     * 检查会话是否存在
     *
     * @param sessionId 会话 ID
     * @return 是否存在
     */
    public boolean sessionExists(String sessionId) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM agent_sessions WHERE id = ?",
            Integer.class,
            sessionId
        );
        return count != null && count > 0;
    }

    /**
     * 获取会话更新时间
     *
     * @param sessionId 会话 ID
     * @return 更新时间（如果不存在返回 null）
     */
    public Instant getSessionUpdatedAt(String sessionId) {
        return jdbcTemplate.queryForObject(
            "SELECT updated_at FROM agent_sessions WHERE id = ?",
            (rs, rowNum) -> rs.getTimestamp("updated_at").toInstant(),
            sessionId
        );
    }

    /**
     * 更新会话的更新时间
     *
     * @param sessionId 会话 ID
     * @param time      新的时间戳
     */
    public void updateSessionUpdatedAt(String sessionId, Instant time) {
        jdbcTemplate.update(
            "UPDATE agent_sessions SET updated_at = ? WHERE id = ?",
            Timestamp.from(time), sessionId
        );
    }

    /**
     * 删除会话及其所有消息
     *
     * @param sessionId 会话 ID
     */
    public void deleteSession(String sessionId) {
        // 由于外键约束，会自动级联删除消息
        int deleted = jdbcTemplate.update(
            "DELETE FROM agent_sessions WHERE id = ?",
            sessionId
        );
        log.debug("删除会话: id={}, rowsAffected={}", sessionId, deleted);
    }
}