package io.agentscope.tutorial.chapter03.repository;

import io.agentscope.tutorial.chapter03.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 会话仓库 - JPA 数据访问层
 *
 * 提供会话的 CRUD 操作
 *
 * @author AgentScope Tutorial
 */
@Repository
public interface SessionRepository extends JpaRepository<ChatSession, String> {

    /**
     * 根据用户ID查找所有会话，按更新时间降序排列
     *
     * @param userId 用户ID
     * @return 会话列表
     */
    List<ChatSession> findByUserIdOrderByUpdatedAtDesc(String userId);

    /**
     * 根据用户ID查找会话数量
     *
     * @param userId 用户ID
     * @return 会话数量
     */
    long countByUserId(String userId);

    /**
     * 检查用户是否存在指定标题的会话
     *
     * @param userId 用户ID
     * @param title  会话标题
     * @return 是否存在
     */
    boolean existsByUserIdAndTitle(String userId, String title);

    /**
     * 查找最近更新的会话
     *
     * @param limit 返回数量
     * @return 会话列表
     */
    @Query("SELECT s FROM ChatSession s ORDER BY s.updatedAt DESC LIMIT ?1")
    List<ChatSession> findRecentSessions(int limit);

    /**
     * 删除用户的所有会话
     *
     * @param userId 用户ID
     */
    void deleteByUserId(String userId);
}