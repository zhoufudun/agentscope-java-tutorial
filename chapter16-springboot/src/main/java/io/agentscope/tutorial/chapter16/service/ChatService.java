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

import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.tutorial.chapter16.model.entity.SessionMessage;
import io.agentscope.tutorial.chapter16.repository.SessionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 会话服务.
 *
 * <p>负责会话的创建、查询、管理和消息的持久化。
 * 使用 H2 数据库存储会话数据和消息历史。
 *
 * <p>提供的功能：
 * <ul>
 *   <li>创建新会话</li>
 *   <li>获取会话列表</li>
 *   <li>获取会话消息历史</li>
 *   <li>添加消息到会话</li>
 *   <li>删除会话</li>
 *   <li>重置会话（清除消息历史）</li>
 * </ul>
 */
@Service
@Slf4j
public class ChatService {

    private final SessionRepository sessionRepository;
    private final H2SessionManager h2SessionManager;

    public ChatService(SessionRepository sessionRepository, H2SessionManager h2SessionManager) {
        this.sessionRepository = sessionRepository;
        this.h2SessionManager = h2SessionManager;
    }

    /**
     * 创建新会话
     *
     * @param name 会话名称
     * @return 会话 ID
     */
    @Transactional
    public String createSession(String name) {
        String sessionId = UUID.randomUUID().toString();
        h2SessionManager.createSession(sessionId, name);
        log.info("创建会话: id={}, name={}", sessionId, name);
        return sessionId;
    }

    /**
     * 创建新会话（使用默认名称）
     *
     * @return 会话 ID
     */
    public String createSession() {
        return createSession("默认会话");
    }

    /**
     * 获取会话名称
     *
     * @param sessionId 会话 ID
     * @return 会话名称（如果不存在返回空）
     */
    public Optional<String> getSessionName(String sessionId) {
        return sessionRepository.findNameById(sessionId);
    }

    /**
     * 检查会话是否存在
     *
     * @param sessionId 会话 ID
     * @return 是否存在
     */
    public boolean sessionExists(String sessionId) {
        return h2SessionManager.sessionExists(sessionId);
    }

    /**
     * 获取所有会话
     *
     * @return 会话列表（按更新时间倒序）
     */
    public List<SessionInfo> getAllSessions() {
        return sessionRepository.findAllOrderByUpdatedAt().stream()
                .map(row -> new SessionInfo(
                        (String) row[0],  // id
                        (String) row[1],  // name
                        ((java.sql.Timestamp) row[2]).toInstant(),  // created_at
                        ((java.sql.Timestamp) row[3]).toInstant()  // updated_at
                ))
                .toList();
    }

    /**
     * 获取会话消息历史
     *
     * <p>返回的消息按时间正序排列，可直接用于 Agent 调用。
     *
     * @param sessionId 会话 ID
     * @return 消息列表
     */
    public List<SessionMessage> getMessages(String sessionId) {
        return sessionRepository.findMessagesBySessionId(sessionId).stream()
                .map(row -> SessionMessage.builder()
                        .id((Long) row[0])
                        .role((String) row[1])
                        .content((String) row[2])
                        .toolCallId((String) row[3])
                        .toolName((String) row[4])
                        .createdAt(row[5] != null ? ((java.sql.Timestamp) row[5]).toInstant() : null)
                        .build())
                .toList();
    }

    /**
     * 添加消息到会话
     *
     * @param sessionId 会话 ID
     * @param role      角色（system | user | assistant | tool）
     * @param content   消息内容
     * @return 自动生成的消息 ID
     */
    @Transactional
    public long addMessage(String sessionId, String role, String content) {
        long messageId = sessionRepository.insertMessage(
                sessionId, role, content, null, null);
        log.debug("添加消息: sessionId={}, role={}, contentLength={}",
                sessionId, role, content != null ? content.length() : 0);
        return messageId;
    }

    /**
     * 更新会话时间戳
     *
     * @param sessionId 会话 ID
     */
    @Transactional
    public void updateSessionTime(String sessionId) {
        sessionRepository.updateSessionTime(sessionId, Instant.now());
    }

    /**
     * 删除会话
     *
     * <p>会话删除后，所有关联消息也会被删除（级联删除）。
     *
     * @param sessionId 会话 ID
     * @return 是否成功删除
     */
    @Transactional
    public boolean deleteSession(String sessionId) {
        int deleted = sessionRepository.deleteById(sessionId);
        log.info("删除会话: sessionId={}, deleted={}", sessionId, deleted > 0);
        return deleted > 0;
    }

    /**
     * 重置会话（清除消息历史）
     *
     * @param sessionId 会话 ID
     */
    @Transactional
    public void resetSession(String sessionId) {
        sessionRepository.deleteMessagesBySessionId(sessionId);
        updateSessionTime(sessionId);
        log.info("重置会话: sessionId={}", sessionId);
    }

    /**
     * 获取会话消息数量
     *
     * @param sessionId 会话 ID
     * @return 消息数量
     */
    public int getMessageCount(String sessionId) {
        return sessionRepository.countMessagesBySessionId(sessionId);
    }

    /**
     * 会话信息记录
     *
     * @param id        会话 ID
     * @param name      会话名称
     * @param createdAt 创建时间
     * @param updatedAt 更新时间
     */
    public record SessionInfo(
            String id,
            String name,
            Instant createdAt,
            Instant updatedAt
    ) {}
}