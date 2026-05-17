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
 * Memory Record Repository
 *
 * <p>Provides data access operations for memory records stored in the database.
 * Supports querying, searching, and managing conversation history.
 *
 * @author AgentScope Tutorial
 */
@Repository
public interface MemoryRepository extends JpaRepository<MemoryRecord, Long> {

    // ==================== Basic Query Methods ====================

    /**
     * Finds all records for a session, ordered by message index.
     *
     * @param sessionId The session identifier
     * @return List of memory records in order
     */
    List<MemoryRecord> findBySessionIdOrderByMessageIndex(String sessionId);

    /**
     * Finds all records for a session and role, ordered by message index.
     *
     * @param sessionId The session identifier
     * @param role The message role (user, assistant, system, tool)
     * @return List of memory records
     */
    List<MemoryRecord> findBySessionIdAndRoleOrderByMessageIndex(String sessionId, String role);

    /**
     * Counts the total number of records in a session.
     *
     * @param sessionId The session identifier
     * @return The number of records
     */
    long countBySessionId(String sessionId);

    /**
     * Counts records by session and role.
     *
     * @param sessionId The session identifier
     * @param role The message role
     * @return The number of records
     */
    long countBySessionIdAndRole(String sessionId, String role);

    /**
     * Deletes all records for a session.
     *
     * @param sessionId The session identifier
     */
    @Modifying
    void deleteBySessionId(String sessionId);

    /**
     * Finds the latest record in a session.
     *
     * @param sessionId The session identifier
     * @return Optional containing the latest record, or empty if none
     */
    Optional<MemoryRecord> findFirstBySessionIdOrderByMessageIndexDesc(String sessionId);

    // ==================== Recent Messages ====================

    /**
     * Finds the most recent N messages for a session.
     *
     * @param sessionId The session identifier
     * @param limit Maximum number of records to return
     * @return List of recent memory records
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
     * Finds messages within a time range.
     *
     * @param sessionId The session identifier
     * @param startTime Start of the time range
     * @param endTime End of the time range
     * @return List of memory records within the range
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
     * Finds messages before a specific index (older messages).
     *
     * @param sessionId The session identifier
     * @param beforeIndex The message index threshold
     * @return List of older memory records
     */
    @Query("SELECT m FROM MemoryRecord m " +
           "WHERE m.sessionId = :sessionId " +
           "AND m.messageIndex < :beforeIndex " +
           "ORDER BY m.messageIndex DESC")
    List<MemoryRecord> findMessagesBeforeIndex(
            @Param("sessionId") String sessionId,
            @Param("beforeIndex") int beforeIndex);

    // ==================== Search Operations ====================

    /**
     * Searches for messages containing a keyword (case-insensitive).
     *
     * @param sessionId The session identifier
     * @param keyword The search keyword
     * @return List of matching memory records
     */
    @Query("SELECT m FROM MemoryRecord m " +
           "WHERE m.sessionId = :sessionId " +
           "AND LOWER(m.content) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "ORDER BY m.messageIndex")
    List<MemoryRecord> searchByKeyword(
            @Param("sessionId") String sessionId,
            @Param("keyword") String keyword);

    /**
     * Searches across all sessions for a keyword.
     *
     * @param keyword The search keyword
     * @return List of matching memory records from all sessions
     */
    @Query("SELECT m FROM MemoryRecord m " +
           "WHERE LOWER(m.content) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "ORDER BY m.createdAt DESC")
    List<MemoryRecord> searchAcrossSessions(@Param("keyword") String keyword);

    /**
     * Full-text search using MySQL MATCH AGAINST (for production with MySQL).
     *
     * @param sessionId The session identifier
     * @param searchTerms Space-separated search terms
     * @return List of matching memory records
     */
    @Query(value = "SELECT * FROM memory_records " +
                   "WHERE session_id = :sessionId " +
                   "AND MATCH(content) AGAINST(:searchTerms IN NATURAL LANGUAGE MODE) " +
                   "ORDER BY message_index DESC",
           nativeQuery = true)
    List<MemoryRecord> fullTextSearch(
            @Param("sessionId") String sessionId,
            @Param("searchTerms") String searchTerms);

    // ==================== Memory Compression ====================

    /**
     * Finds all non-summarized records for compression processing.
     *
     * @param sessionId The session identifier
     * @param limit Maximum number of records to compress
     * @return List of records to be compressed
     */
    @Query("SELECT m FROM MemoryRecord m " +
           "WHERE m.sessionId = :sessionId " +
           "AND m.isSummarized = false " +
           "ORDER BY m.messageIndex ASC")
    List<MemoryRecord> findUncompressedMessages(
            @Param("sessionId") String sessionId,
            @Param("limit") int limit);

    /**
     * Marks records as summarized.
     *
     * @param sessionId The session identifier
     * @param fromIndex Start index (inclusive)
     * @param toIndex End index (inclusive)
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
     * Deletes summarized records older than a specific index.
     *
     * @param sessionId The session identifier
     * @param beforeIndex Records with index less than this will be deleted
     */
    @Modifying
    @Query("DELETE FROM MemoryRecord m " +
           "WHERE m.sessionId = :sessionId " +
           "AND m.messageIndex < :beforeIndex " +
           "AND m.isSummarized = true")
    void deleteSummarizedBeforeIndex(
            @Param("sessionId") String sessionId,
            @Param("beforeIndex") int beforeIndex);

    // ==================== Session Statistics ====================

    /**
     * Gets statistics for a session.
     *
     * @param sessionId The session identifier
     * @return Object array containing [totalCount, userCount, assistantCount]
     */
    @Query("SELECT COUNT(m), " +
           "SUM(CASE WHEN m.role = 'user' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN m.role = 'assistant' THEN 1 ELSE 0 END) " +
           "FROM MemoryRecord m WHERE m.sessionId = :sessionId")
    Object[] getSessionStats(@Param("sessionId") String sessionId);

    /**
     * Gets all distinct session IDs.
     *
     * @return List of unique session identifiers
     */
    @Query("SELECT DISTINCT m.sessionId FROM MemoryRecord m ORDER BY m.sessionId")
    List<String> findAllSessionIds();
}