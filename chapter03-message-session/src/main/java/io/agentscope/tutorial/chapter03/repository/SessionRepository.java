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

import io.agentscope.tutorial.chapter03.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 会话仓库接口 - JPA 数据访问层.
 *
 * <p>提供会话的 CRUD 操作，使用 Spring Data JPA 自动实现。
 * 继承自 {@link JpaRepository}，自动获得基础的增删改查能力。
 *
 * <h2>核心功能</h2>
 * <ul>
 *   <li>按用户ID查询会话列表（按更新时间排序）</li>
 *   <li>统计用户的会话数量</li>
 *   <li>检查会话标题是否重复</li>
 *   <li>获取最近活跃的会话</li>
 *   <li>批量删除用户的所有会话</li>
 * </ul>
 *
 * <h2>与 AgentStateStore 的关系</h2>
 * <p>本仓库管理会话的元数据（标题、创建时间等），而 AgentStateStore
 * 管理会话的运行时状态（对话上下文、Agent 状态等）。两者协同工作：
 * <ul>
 *   <li>创建会话时：先在数据库创建记录，再初始化 AgentState</li>
 *   <li>删除会话时：先清理 AgentState，再删除数据库记录</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 * @since 1.0
 * @see ChatSession
 * @see JpaRepository
 */
@Repository
public interface SessionRepository extends JpaRepository<ChatSession, String> {

    /**
     * 根据用户ID查找所有会话，按更新时间降序排列.
     *
     * <p>最近活跃的会话排在前面，适合在会话列表中显示。
     * 利用 Spring Data JPA 的方法名查询约定自动生成查询。
     *
     * @param userId 用户ID
     * @return 按更新时间降序排列的会话列表
     */
    List<ChatSession> findByUserIdOrderByUpdatedAtDesc(String userId);

    /**
     * 根据用户ID统计会话数量.
     *
     * <p>可用于显示用户的会话总数，或限制用户的会话数量。
     *
     * @param userId 用户ID
     * @return 会话数量
     */
    long countByUserId(String userId);

    /**
     * 检查用户是否存在指定标题的会话.
     *
     * <p>可用于避免创建重复标题的会话，提升用户体验。
     *
     * @param userId 用户ID
     * @param title  会话标题
     * @return 如果存在相同标题的会话返回 true，否则返回 false
     */
    boolean existsByUserIdAndTitle(String userId, String title);

    /**
     * 查找最近更新的会话（跨用户）.
     *
     * <p>使用 JPQL 的 LIMIT 子句限制返回数量。
     * 适合管理员查看系统中最近活跃的会话。
     *
     * @param limit 返回数量限制
     * @return 最近更新的会话列表
     */
    @Query("SELECT s FROM ChatSession s ORDER BY s.updatedAt DESC LIMIT ?1")
    List<ChatSession> findRecentSessions(int limit);

    /**
     * 删除用户的所有会话.
     *
     * <p>注意：删除前应先调用 {@link MessageRepository#deleteBySessionId}
     * 清理相关的消息数据，避免外键约束冲突。
     *
     * @param userId 用户ID
     */
    void deleteByUserId(String userId);
}