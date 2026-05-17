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
package io.agentscope.tutorial.chapter16.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 会话数据访问层.
 *
 * <p>使用 Spring JDBC 访问 H2 数据库。
 * 相比 JPA/JDBC 模板，JDBC Template 更轻量且性能更好。
 */
@Repository
public class SessionRepository {

    private final JdbcTemplate jdbcTemplate;

    public SessionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ==================== 会话操作 ====================

    /**
     * 查询会话名称
     *
     * @param sessionId 会话 ID
     * @return 会话名称
     */
    public Optional<String> findNameById(String sessionId) {
        String name = jdbcTemplate.queryForObject(
            "SELECT name FROM agent_sessions WHERE id = ?",
            String.class,
            sessionId
        );
        return Optional.ofNullable(name);
    }

    /**
     * 查询所有会话（按更新时间倒序）
     *
     * @return 会话列表
     */
    public List<Object[]> findAllOrderByUpdatedAt() {
        return jdbcTemplate.query(
            "SELECT id, name, created_at, updated_at FROM agent_sessions ORDER BY updated_at DESC",
            (rs, rowNum) -> new Object[]{
                rs.getString("id"),
                rs.getString("name"),
                rs.getTimestamp("created_at"),
                rs.getTimestamp("updated_at")
            }
        );
    }

    /**
     * 更新会话时间
     *
     * @param sessionId 会话 ID
     * @param time     新的时间戳
     */
    public void updateSessionTime(String sessionId, Instant time) {
        jdbcTemplate.update(
            "UPDATE agent_sessions SET updated_at = ? WHERE id = ?",
            Timestamp.from(time), sessionId
        );
    }

    /**
     * 删除会话
     *
     * @param sessionId 会话 ID
     * @return 影响的行数
     */
    public int deleteById(String sessionId) {
        return jdbcTemplate.update(
            "DELETE FROM agent_sessions WHERE id = ?",
            sessionId
        );
    }

    // ==================== 消息操作 ====================

    /**
     * 查询会话的所有消息（按时间正序）
     *
     * @param sessionId 会话 ID
     * @return 消息列表
     */
    public List<Object[]> findMessagesBySessionId(String sessionId) {
        return jdbcTemplate.query(
            """
            SELECT id, role, content, tool_call_id, tool_name, created_at
            FROM session_messages
            WHERE session_id = ?
            ORDER BY created_at ASC
            """,
            (rs, rowNum) -> new Object[]{
                rs.getLong("id"),
                rs.getString("role"),
                rs.getString("content"),
                rs.getString("tool_call_id"),
                rs.getString("tool_name"),
                rs.getTimestamp("created_at")
            },
            sessionId
        );
    }

    /**
     * 插入消息
     *
     * @param sessionId  会话 ID
     * @param role       角色
     * @param content    内容
     * @param toolCallId 工具调用 ID（可选）
     * @param toolName   工具名称（可选）
     * @return 插入行的 ID
     */
    public long insertMessage(String sessionId, String role, String content,
                              String toolCallId, String toolName) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                """
                INSERT INTO session_messages (session_id, role, content, tool_call_id, tool_name, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                new String[]{"id"}
            );
            ps.setString(1, sessionId);
            ps.setString(2, role);
            ps.setString(3, content);
            if (toolCallId != null) {
                ps.setString(4, toolCallId);
            } else {
                ps.setNull(4, Types.VARCHAR);
            }
            if (toolName != null) {
                ps.setString(5, toolName);
            } else {
                ps.setNull(5, Types.VARCHAR);
            }
            ps.setTimestamp(6, Timestamp.from(Instant.now()));
            return ps;
        }, keyHolder);

        return keyHolder.getKey().longValue();
    }

    /**
     * 统计会话消息数量
     *
     * @param sessionId 会话 ID
     * @return 消息数量
     */
    public int countMessagesBySessionId(String sessionId) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM session_messages WHERE session_id = ?",
            Integer.class,
            sessionId
        );
        return count != null ? count : 0;
    }

    /**
     * 删除会话的所有消息
     *
     * @param sessionId 会话 ID
     * @return 影响的行数
     */
    public int deleteMessagesBySessionId(String sessionId) {
        return jdbcTemplate.update(
            "DELETE FROM session_messages WHERE session_id = ?",
            sessionId
        );
    }

    /**
     * 查询会话最新的一条消息
     *
     * @param sessionId 会话 ID
     * @return 最新消息
     */
    public Optional<Object[]> findLatestMessage(String sessionId) {
        List<Object[]> results = jdbcTemplate.query(
            """
            SELECT id, role, content, tool_call_id, tool_name, created_at
            FROM session_messages
            WHERE session_id = ?
            ORDER BY created_at DESC
            LIMIT 1
            """,
            (rs, rowNum) -> new Object[]{
                rs.getLong("id"),
                rs.getString("role"),
                rs.getString("content"),
                rs.getString("tool_call_id"),
                rs.getString("tool_name"),
                rs.getTimestamp("created_at")
            },
            sessionId
        );
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
}