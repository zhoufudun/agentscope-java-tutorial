package io.agentscope.tutorial.chapter03.repository;

import io.agentscope.tutorial.chapter03.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 消息仓库 - JPA 数据访问层
 *
 * 提供消息的 CRUD 操作
 *
 * @author AgentScope Tutorial
 */
@Repository
public interface MessageRepository extends JpaRepository<ChatMessage, String> {

    /**
     * 根据会话ID查找所有消息，按序列号升序排列
     *
     * @param sessionId 会话ID
     * @return 消息列表
     */
    @Query("SELECT m FROM ChatMessage m WHERE m.sessionId = :sessionId ORDER BY m.sequenceNum ASC")
    List<ChatMessage> findBySessionIdOrderBySequenceNumAsc(@Param("sessionId") String sessionId);

    /**
     * 根据会话ID统计消息数量
     *
     * @param sessionId 会话ID
     * @return 消息数量
     */
    long countBySessionId(String sessionId);

    /**
     * 根据会话ID查找最后一条消息
     *
     * @param sessionId 会话ID
     * @return 最后一条消息
     */
    Optional<ChatMessage> findFirstBySessionIdOrderBySequenceNumDesc(String sessionId);

    /**
     * 获取会话的下一个序列号
     *
     * @param sessionId 会话ID
     * @return 下一个序列号
     */
    @Query("SELECT COALESCE(MAX(m.sequenceNum), -1) + 1 FROM ChatMessage m WHERE m.sessionId = :sessionId")
    int getNextSequenceNum(@Param("sessionId") String sessionId);

    /**
     * 根据会话ID和角色查找消息
     *
     * @param sessionId 会话ID
     * @param role      消息角色
     * @return 消息列表
     */
    List<ChatMessage> findBySessionIdAndRole(String sessionId, String role);

    /**
     * 根据会话ID和内容关键词搜索消息
     *
     * @param sessionId 会话ID
     * @param keyword   关键词
     * @return 匹配的消息列表
     */
    @Query("SELECT m FROM ChatMessage m WHERE m.sessionId = :sessionId AND m.content LIKE %:keyword%")
    List<ChatMessage> searchByContent(@Param("sessionId") String sessionId, @Param("keyword") String keyword);

    /**
     * 删除会话的所有消息
     *
     * @param sessionId 会话ID
     */
    void deleteBySessionId(String sessionId);

    /**
     * 批量保存消息
     *
     * @param messages 消息列表
     * @return 保存后的消息列表
     */
    default List<ChatMessage> saveAllMessages(List<ChatMessage> messages) {
        return saveAll(messages);
    }
}