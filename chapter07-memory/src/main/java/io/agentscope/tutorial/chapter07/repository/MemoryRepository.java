package io.agentscope.tutorial.chapter07.repository;

import io.agentscope.tutorial.chapter07.entity.MemoryRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 记忆记录仓库接口
 *
 * <p>为记忆系统提供数据访问操作，支持对话历史的查询、搜索和管理。
 * 继承 {@link JpaRepository} 获得标准 CRUD 操作，并定义了记忆系统特有的查询方法。
 *
 * <p><b>功能分类</b>：
 * <ul>
 *   <li><b>基础查询</b> - 按会话/角色获取记录</li>
 *   <li><b>最近消息</b> - 获取最近 N 条消息、时间范围查询</li>
 *   <li><b>搜索操作</b> - 关键词搜索、全文搜索</li>
 *   <li><b>记忆压缩</b> - 标记和删除已压缩消息</li>
 *   <li><b>会话统计</b> - 消息计数和统计信息</li>
 * </ul>
 *
 * <p><b>数据库兼容性</b>：
 * <ul>
 *   <li>JPQL 查询 - 跨数据库兼容（H2、MySQL、PostgreSQL）</li>
 *   <li>原生 SQL 查询 - 需注意 MySQL/H2 语法差异</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 * @since 1.0
 */
@Repository
public interface MemoryRepository extends JpaRepository<MemoryRecord, Long> {

    // ==================== 基础查询方法 ====================

    /**
     * 按消息索引顺序获取会话的所有记录
     *
     * <p>这是最常用的查询方法，用于加载完整的对话历史。
     * 返回结果按 messageIndex 升序排列，保证消息的时间顺序。
     *
     * @param sessionId 会话标识符
     * @return 按消息索引排序的记录列表
     */
    List<MemoryRecord> findBySessionIdOrderByMessageIndex(String sessionId);

    /**
     * 按会话和角色获取记录
     *
     * <p>用于获取特定角色的消息，例如只获取用户消息或助手消息。
     *
     * @param sessionId 会话标识符
     * @param role 消息角色（user/assistant/system/tool）
     * @return 符合条件的记录列表
     */
    List<MemoryRecord> findBySessionIdAndRoleOrderByMessageIndex(String sessionId, String role);

    /**
     * 统计会话的总消息数
     *
     * @param sessionId 会话标识符
     * @return 消息总数
     */
    long countBySessionId(String sessionId);

    /**
     * 统计会话中特定角色的消息数
     *
     * @param sessionId 会话标识符
     * @param role 消息角色
     * @return 该角色的消息数
     */
    long countBySessionIdAndRole(String sessionId, String role);

    /**
     * 删除会话的所有记录
     *
     * <p>用于清除整个会话的记忆。需要配合 {@code @Transactional} 使用。
     *
     * @param sessionId 会话标识符
     */
    @Modifying
    void deleteBySessionId(String sessionId);

    /**
     * 获取会话的最新一条记录
     *
     * <p>用于获取对话的最后一条消息，例如检查最后一次交互。
     *
     * @param sessionId 会话标识符
     * @return 最新记录（如存在）
     */
    Optional<MemoryRecord> findFirstBySessionIdOrderByMessageIndexDesc(String sessionId);

    // ==================== 最近消息查询 ====================

    /**
     * 获取会话最近的 N 条消息
     *
     * <p>使用原生 SQL 的 LIMIT 子句。返回按 messageIndex 降序排列的记录，
     * 即最新的消息在前。
     *
     * <p><b>注意</b>：原生查询使用 MySQL/H2 的 LIMIT 语法，其他数据库可能需要调整。
     *
     * @param sessionId 会话标识符
     * @param limit 最大返回记录数
     * @return 最近的记录列表（最新在前）
     */
    @Query(value = "SELECT * FROM memory_records " +
                   "WHERE session_id = :sessionId " +
                   "ORDER BY message_index DESC " +
                   "LIMIT :limit",
           nativeQuery = true)
    List<MemoryRecord> findRecentMessages(
            @Param("sessionId") String sessionId,
            @Param("limit") int limit);

    /**
     * 获取时间范围内的消息
     *
     * <p>用于按时间段检索对话历史，例如"今天的对话"或"过去一周的对话"。
     *
     * @param sessionId 会话标识符
     * @param startTime 开始时间（包含）
     * @param endTime 结束时间（包含）
     * @return 时间范围内的记录列表
     */
    @Query("SELECT m FROM MemoryRecord m " +
           "WHERE m.sessionId = :sessionId " +
           "AND m.createdAt >= :startTime " +
           "AND m.createdAt <= :endTime " +
           "ORDER BY m.messageIndex")
    List<MemoryRecord> findMessagesInTimeRange(
            @Param("sessionId") String sessionId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 获取指定索引之前的消息
     *
     * <p>用于获取更早的历史消息，支持分页加载更多历史。
     *
     * @param sessionId 会话标识符
     * @param beforeIndex 索引阈值（不包含）
     * @return 较早的记录列表（按索引降序）
     */
    @Query("SELECT m FROM MemoryRecord m " +
           "WHERE m.sessionId = :sessionId " +
           "AND m.messageIndex < :beforeIndex " +
           "ORDER BY m.messageIndex DESC")
    List<MemoryRecord> findMessagesBeforeIndex(
            @Param("sessionId") String sessionId,
            @Param("beforeIndex") int beforeIndex);

    // ==================== 搜索操作 ====================

    /**
     * 在会话内搜索包含关键词的消息
     *
     * <p>使用 LIKE 进行模糊匹配，不区分大小写。
     * 适用于简单的关键词搜索场景。
     *
     * @param sessionId 会话标识符
     * @param keyword 搜索关键词
     * @return 匹配的记录列表
     */
    @Query("SELECT m FROM MemoryRecord m " +
           "WHERE m.sessionId = :sessionId " +
           "AND LOWER(m.content) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "ORDER BY m.messageIndex")
    List<MemoryRecord> searchByKeyword(
            @Param("sessionId") String sessionId,
            @Param("keyword") String keyword);

    /**
     * 跨所有会话搜索关键词
     *
     * <p>在所有会话中搜索包含关键词的消息，返回按时间倒序排列的结果。
     * 适用于全局搜索场景。
     *
     * @param keyword 搜索关键词
     * @return 所有会话中匹配的记录列表
     */
    @Query("SELECT m FROM MemoryRecord m " +
           "WHERE LOWER(m.content) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "ORDER BY m.createdAt DESC")
    List<MemoryRecord> searchAcrossSessions(@Param("keyword") String keyword);

    /**
     * MySQL 全文搜索（仅生产环境 MySQL 可用）
     *
     * <p>使用 MySQL 的 MATCH AGAINST 语法进行全文搜索，
     * 支持自然语言模式，比 LIKE 更高效且支持相关性排序。
     *
     * <p><b>前提条件</b>：content 字段需要创建 FULLTEXT 索引。
     *
     * <p><b>注意</b>：此查询仅在 MySQL 环境下可用，H2 不支持。
     *
     * @param sessionId 会话标识符
     * @param searchTerms 空格分隔的搜索词
     * @return 匹配的记录列表（按相关性排序）
     */
    @Query(value = "SELECT * FROM memory_records " +
                   "WHERE session_id = :sessionId " +
                   "AND MATCH(content) AGAINST(:searchTerms IN NATURAL LANGUAGE MODE) " +
                   "ORDER BY message_index DESC",
           nativeQuery = true)
    List<MemoryRecord> fullTextSearch(
            @Param("sessionId") String sessionId,
            @Param("searchTerms") String searchTerms);

    // ==================== 记忆压缩 ====================

    /**
     * 获取未压缩的消息用于压缩处理
     *
     * <p>返回会话中尚未被摘要的消息，按消息索引升序排列。
     * 用于记忆压缩功能，将旧消息生成摘要。
     *
     * @param sessionId 会话标识符
     * @param limit 最大返回记录数（实际由调用方控制使用数量）
     * @return 待压缩的记录列表
     */
    @Query("SELECT m FROM MemoryRecord m " +
           "WHERE m.sessionId = :sessionId " +
           "AND m.isSummarized = false " +
           "ORDER BY m.messageIndex ASC")
    List<MemoryRecord> findUncompressedMessages(
            @Param("sessionId") String sessionId,
            @Param("limit") int limit);

    /**
     * 将指定范围的消息标记为已摘要
     *
     * <p>在生成摘要后，将原始消息标记为已处理，
     * 后续可以选择删除这些消息以节省存储空间。
     *
     * @param sessionId 会话标识符
     * @param fromIndex 起始索引（包含）
     * @param toIndex 结束索引（包含）
     */
    @Modifying
    @Query("UPDATE MemoryRecord m SET m.isSummarized = true " +
           "WHERE m.sessionId = :sessionId " +
           "AND m.messageIndex >= :fromIndex " +
           "AND m.messageIndex <= :toIndex")
    void markAsSummarized(
            @Param("sessionId") String sessionId,
            @Param("fromIndex") int fromIndex,
            @Param("toIndex") int toIndex);

    /**
     * 删除指定索引之前的已摘要消息
     *
     * <p>清理已被摘要压缩的旧消息，释放存储空间。
     * 只删除 isSummarized 为 true 的记录，保护未处理的消息。
     *
     * @param sessionId 会话标识符
     * @param beforeIndex 索引阈值（删除小于此值的记录）
     */
    @Modifying
    @Query("DELETE FROM MemoryRecord m " +
           "WHERE m.sessionId = :sessionId " +
           "AND m.messageIndex < :beforeIndex " +
           "AND m.isSummarized = true")
    void deleteSummarizedBeforeIndex(
            @Param("sessionId") String sessionId,
            @Param("beforeIndex") int beforeIndex);

    // ==================== 会话统计 ====================

    /**
     * 获取会话的统计信息
     *
     * <p>返回单次查询获取的聚合统计数据：
     * <ul>
     *   <li>[0] - 总消息数</li>
     *   <li>[1] - 用户消息数</li>
     *   <li>[2] - 助手消息数</li>
     * </ul>
     *
     * @param sessionId 会话标识符
     * @return 包含统计数据的对象数组 [总数, 用户数, 助手数]
     */
    @Query("SELECT COUNT(m), " +
           "SUM(CASE WHEN m.role = 'user' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN m.role = 'assistant' THEN 1 ELSE 0 END) " +
           "FROM MemoryRecord m WHERE m.sessionId = :sessionId")
    Object[] getSessionStats(@Param("sessionId") String sessionId);

    /**
     * 获取所有会话 ID
     *
     * <p>返回系统中所有存在记忆记录的会话 ID 列表。
     * 用于会话管理和跨会话操作。
     *
     * @return 去重排序后的会话 ID 列表
     */
    @Query("SELECT DISTINCT m.sessionId FROM MemoryRecord m ORDER BY m.sessionId")
    List<String> findAllSessionIds();
}