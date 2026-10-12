/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.agentscope.tutorial.chapter03.repository;

import io.agentscope.tutorial.chapter03.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 消息仓库接口 - JPA 数据访问层.
 *
 * <p>提供消息的 CRUD 操作，使用 Spring Data JPA 自动实现。
 * 继承自 {@link JpaRepository}，自动获得基础的增删改查能力。
 *
 * <h2>核心功能</h2>
 * <ul>
 *   <li>按会话ID查询消息列表（按序列号排序）</li>
 *   <li>统计会话消息数量</li>
 *   <li>获取会话的下一个序列号</li>
 *   <li>按内容关键词搜索消息</li>
 *   <li>批量删除会话消息</li>
 * </ul>
 *
 * <h2>命名查询规范</h2>
 * <p>Spring Data JPA 支持根据方法名自动生成查询：
 * <ul>
 *   <li>{@code findBy} 前缀 + 属性名：生成 WHERE 条件</li>
 *   <li>{@code OrderBy} + 属性名 + {@code Asc/Desc}：生成排序</li>
 *   <li>{@code countBy} 前缀：生成 COUNT 查询</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 * @since 1.0
 * @see ChatMessage
 * @see JpaRepository
 */
@Repository
public interface MessageRepository extends JpaRepository<ChatMessage, String> {

    /**
     * 根据会话ID查找所有消息，按序列号升序排列.
     *
     * <p>使用 JPQL 自定义查询，确保消息按时间顺序返回。
     *
     * @param sessionId 会话ID
     * @return 按序列号排序的消息列表
     */
    @Query("SELECT m FROM ChatMessage m WHERE m.sessionId = :sessionId ORDER BY m.sequenceNum ASC")
    List<ChatMessage> findBySessionIdOrderBySequenceNumAsc(@Param("sessionId") String sessionId);

    /**
     * 根据会话ID统计消息数量.
     *
     * <p>利用 Spring Data JPA 的方法名查询约定自动生成 COUNT 查询。
     *
     * @param sessionId 会话ID
     * @return 消息数量
     */
    long countBySessionId(String sessionId);

    /**
     * 根据会话ID查找最后一条消息.
     *
     * <p>按序列号降序排列，取第一条即为最新消息。
     * 返回 {@link Optional} 以处理会话为空的情况。
     *
     * @param sessionId 会话ID
     * @return 最后一条消息（如果存在）
     */
    Optional<ChatMessage> findFirstBySessionIdOrderBySequenceNumDesc(String sessionId);

    /**
     * 获取会话的下一个序列号.
     *
     * <p>使用 {@code COALESCE} 函数处理空会话的情况：
     * <ul>
     *   <li>如果会话有消息，返回 MAX(sequenceNum) + 1</li>
     *   <li>如果会话为空，返回 0（因为 COALESCE(null, -1) + 1 = 0）</li>
     * </ul>
     *
     * @param sessionId 会话ID
     * @return 下一个可用的序列号
     */
    @Query("SELECT COALESCE(MAX(m.sequenceNum), -1) + 1 FROM ChatMessage m WHERE m.sessionId = :sessionId")
    int getNextSequenceNum(@Param("sessionId") String sessionId);

    /**
     * 根据会话ID和角色查找消息.
     *
     * <p>可用于筛选特定角色的消息，如只获取用户消息或助手回复。
     *
     * @param sessionId 会话ID
     * @param role      消息角色（USER/ASSISTANT/SYSTEM/TOOL）
     * @return 符合条件的消息列表
     */
    List<ChatMessage> findBySessionIdAndRole(String sessionId, String role);

    /**
     * 根据会话ID和内容关键词搜索消息.
     *
     * <p>使用 LIKE 模糊匹配，在消息内容中搜索关键词。
     * 注意：大数据量时可能需要全文索引优化。
     *
     * @param sessionId 会话ID
     * @param keyword   搜索关键词
     * @return 匹配的消息列表
     */
    @Query("SELECT m FROM ChatMessage m WHERE m.sessionId = :sessionId AND m.content LIKE %:keyword%")
    List<ChatMessage> searchByContent(@Param("sessionId") String sessionId, @Param("keyword") String keyword);

    /**
     * 删除会话的所有消息.
     *
     * <p>通常在删除会话时调用，清理关联的消息数据。
     * 配合 {@link SessionRepository#deleteById} 使用。
     *
     * @param sessionId 会话ID
     */
    void deleteBySessionId(String sessionId);

    /**
     * 批量保存消息.
     *
     * <p>便捷方法，内部调用 {@link JpaRepository#saveAll}。
     *
     * @param messages 消息列表
     * @return 保存后的消息列表（包含持久化后的ID）
     */
    default List<ChatMessage> saveAllMessages(List<ChatMessage> messages) {
        return saveAll(messages);
    }
}