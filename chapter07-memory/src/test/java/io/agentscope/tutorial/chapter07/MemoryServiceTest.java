package io.agentscope.tutorial.chapter07;

import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.tutorial.chapter07.entity.MemoryRecord;
import io.agentscope.tutorial.chapter07.repository.MemoryRepository;
import io.agentscope.tutorial.chapter07.service.MemoryService;
import io.agentscope.tutorial.chapter07.config.MemoryConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Memory Service Tests
 *
 * <p>Unit tests for the memory system functionality including:
 * <ul>
 *   <li>Memory record storage and retrieval</li>
 *   <li>Session isolation</li>
 *   <li>Memory search functionality</li>
 *   <li>Statistics aggregation</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 */
@ExtendWith(MockitoExtension.class)
class MemoryServiceTest {

    @Mock
    private MemoryRepository memoryRepository;

    @Mock
    private MemoryConfig memoryConfig;

    @Mock
    private io.agentscope.core.model.Model model;

    private MemoryService memoryService;

    private static final String TEST_SESSION_ID = "test-session-001";
    private static final String TEST_SESSION_ID_2 = "test-session-002";

    @BeforeEach
    void setUp() {
        memoryService = new MemoryService(memoryRepository, memoryConfig, model);
    }

    // ==================== Memory Record Tests ====================

    @Test
    @DisplayName("Should save and retrieve memory records")
    void testSaveAndRetrieve() {
        // Given
        Msg testMessage = createTestMessage("Hello, world!", MsgRole.USER, "user");

        when(memoryRepository.countBySessionId(TEST_SESSION_ID)).thenReturn(0L);
        when(memoryRepository.save(any(MemoryRecord.class))).thenAnswer(inv -> {
            MemoryRecord record = inv.getArgument(0);
            record.setId(1L);
            return record;
        });

        // When
        memoryService.addMessage(TEST_SESSION_ID, testMessage);

        // Then
        verify(memoryRepository, times(1)).save(any(MemoryRecord.class));
        verify(memoryRepository, times(1)).countBySessionId(TEST_SESSION_ID);
    }

    @Test
    @DisplayName("Should convert Msg to MemoryRecord correctly")
    void testMsgToMemoryRecordConversion() {
        // Given
        Msg msg = Msg.builder()
                .name("testUser")
                .role(MsgRole.USER)
                .content(TextBlock.builder().text("Test content").build())
                .build();

        // When
        MemoryRecord record = MemoryRecord.fromMsg(msg, TEST_SESSION_ID, 0);

        // Then
        assertEquals(TEST_SESSION_ID, record.getSessionId());
        assertEquals("user", record.getRole());
        assertEquals("testUser", record.getName());
        assertEquals("Test content", record.getContent());
        assertEquals(0, record.getMessageIndex());
    }

    @Test
    @DisplayName("Should convert MemoryRecord to Msg correctly")
    void testMemoryRecordToMsgConversion() {
        // Given
        MemoryRecord record = new MemoryRecord(TEST_SESSION_ID, "user", "Test content", "user", 0);
        record.setCreatedAt(LocalDateTime.now());

        // When
        Msg msg = record.toMsg();

        // Then
        assertEquals("user", msg.getRole().name().toLowerCase());
        assertEquals("user", msg.getName());
        assertEquals("Test content", msg.getTextContent());
    }

    // ==================== Session Isolation Tests ====================

    @Test
    @DisplayName("Should isolate memory between sessions")
    void testSessionIsolation() {
        // Given
        List<MemoryRecord> session1Records = List.of(
                createMemoryRecord(1L, TEST_SESSION_ID, "user", "Message 1", 0),
                createMemoryRecord(2L, TEST_SESSION_ID, "assistant", "Response 1", 1)
        );

        List<MemoryRecord> session2Records = List.of(
                createMemoryRecord(3L, TEST_SESSION_ID_2, "user", "Different message", 0)
        );

        when(memoryRepository.findBySessionIdOrderByMessageIndex(TEST_SESSION_ID))
                .thenReturn(session1Records);
        when(memoryRepository.findBySessionIdOrderByMessageIndex(TEST_SESSION_ID_2))
                .thenReturn(session2Records);

        // When
        List<MemoryRecord> records1 = memoryService.getAllRecords(TEST_SESSION_ID);
        List<MemoryRecord> records2 = memoryService.getAllRecords(TEST_SESSION_ID_2);

        // Then
        assertEquals(2, records1.size());
        assertEquals(1, records2.size());
        assertEquals("Message 1", records1.get(0).getContent());
        assertEquals("Different message", records2.get(0).getContent());
    }

    @Test
    @DisplayName("Should clear session memory independently")
    void testClearSessionMemory() {
        // When
        memoryService.clearSessionMemory(TEST_SESSION_ID);

        // Then
        verify(memoryRepository, times(1)).deleteBySessionId(TEST_SESSION_ID);
        verify(memoryRepository, never()).deleteBySessionId(TEST_SESSION_ID_2);
    }

    // ==================== Search Tests ====================

    @Test
    @DisplayName("Should search messages by keyword")
    void testSearchMessages() {
        // Given
        String keyword = "coffee";
        List<MemoryRecord> searchResults = List.of(
                createMemoryRecord(1L, TEST_SESSION_ID, "user", "I love coffee", 0),
                createMemoryRecord(2L, TEST_SESSION_ID, "assistant", "Coffee is great!", 1)
        );

        when(memoryRepository.searchByKeyword(TEST_SESSION_ID, keyword))
                .thenReturn(searchResults);

        // When
        List<Msg> results = memoryService.searchMessages(TEST_SESSION_ID, keyword);

        // Then
        assertEquals(2, results.size());
        assertTrue(results.get(0).getTextContent().toLowerCase().contains("coffee"));
    }

    @Test
    @DisplayName("Should return empty list when no search results")
    void testSearchNoResults() {
        // Given
        when(memoryRepository.searchByKeyword(TEST_SESSION_ID, "nonexistent"))
                .thenReturn(List.of());

        // When
        List<Msg> results = memoryService.searchMessages(TEST_SESSION_ID, "nonexistent");

        // Then
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Should search across all sessions")
    void testSearchAcrossSessions() {
        // Given
        String keyword = "project";
        List<MemoryRecord> crossSessionResults = List.of(
                createMemoryRecord(1L, TEST_SESSION_ID, "user", "Working on project X", 0),
                createMemoryRecord(2L, TEST_SESSION_ID_2, "user", "Project deadline tomorrow", 0)
        );

        when(memoryRepository.searchAcrossSessions(keyword))
                .thenReturn(crossSessionResults);

        // When
        List<MemoryRecord> results = memoryService.searchAcrossSessions(keyword);

        // Then
        assertEquals(2, results.size());
    }

    // ==================== Recent Messages Tests ====================

    @Test
    @DisplayName("Should get recent messages with limit")
    void testGetRecentMessages() {
        // Given
        List<MemoryRecord> recentRecords = List.of(
                createMemoryRecord(9L, TEST_SESSION_ID, "user", "Recent 1", 8),
                createMemoryRecord(10L, TEST_SESSION_ID, "user", "Recent 2", 9)
        );

        when(memoryRepository.findRecentMessages(TEST_SESSION_ID, 5))
                .thenReturn(recentRecords);

        // When
        List<Msg> recent = memoryService.getRecentMessages(TEST_SESSION_ID, 5);

        // Then
        assertEquals(2, recent.size());
    }

    // ==================== Statistics Tests ====================

    @Test
    @DisplayName("Should calculate memory statistics correctly")
    void testMemoryStats() {
        // Given
        Object[] stats = new Object[]{10L, 5L, 5L}; // total, user, assistant
        when(memoryRepository.getSessionStats(TEST_SESSION_ID)).thenReturn(stats);

        // When
        MemoryService.MemoryStats memoryStats = memoryService.getMemoryStats(TEST_SESSION_ID);

        // Then
        assertEquals(10L, memoryStats.totalMessages());
        assertEquals(5L, memoryStats.userMessageCount());
        assertEquals(5L, memoryStats.assistantMessageCount());
    }

    @Test
    @DisplayName("Should handle null statistics gracefully")
    void testNullStats() {
        // Given
        when(memoryRepository.getSessionStats(TEST_SESSION_ID)).thenReturn(null);

        // When
        MemoryService.MemoryStats stats = memoryService.getMemoryStats(TEST_SESSION_ID);

        // Then
        assertEquals(0L, stats.totalMessages());
        assertEquals(0L, stats.userMessageCount());
        assertEquals(0L, stats.assistantMessageCount());
    }

    // ==================== Session Agent Tests ====================

    @Test
    @DisplayName("Should create new agent for new session")
    void testCreateAgentForNewSession() {
        // Given
        when(memoryRepository.findBySessionIdOrderByMessageIndex(anyString()))
                .thenReturn(List.of());

        // When
        var agent1 = memoryService.getOrCreateAgent(TEST_SESSION_ID);
        var agent2 = memoryService.getOrCreateAgent(TEST_SESSION_ID);

        // Then
        assertSame(agent1, agent2); // Same instance for same session
        // First call creates agent, second returns cached
        verify(memoryRepository, times(1)).findBySessionIdOrderByMessageIndex(TEST_SESSION_ID);
    }

    @Test
    @DisplayName("Should load existing messages when creating agent")
    void testLoadExistingMessagesForAgent() {
        // Given
        List<MemoryRecord> existingRecords = List.of(
                createMemoryRecord(1L, TEST_SESSION_ID, "user", "Old message", 0)
        );

        when(memoryRepository.findBySessionIdOrderByMessageIndex(TEST_SESSION_ID))
                .thenReturn(existingRecords);

        // When
        memoryService.getOrCreateAgent(TEST_SESSION_ID);

        // Then
        verify(memoryRepository, times(1)).findBySessionIdOrderByMessageIndex(TEST_SESSION_ID);
        assertFalse(memoryService.getMemoryCache(TEST_SESSION_ID).isEmpty());
    }

    // ==================== Helper Methods ====================

    private Msg createTestMessage(String content, MsgRole role, String name) {
        return Msg.builder()
                .name(name)
                .role(role)
                .content(TextBlock.builder().text(content).build())
                .build();
    }

    private MemoryRecord createMemoryRecord(Long id, String sessionId, String role,
                                            String content, int index) {
        MemoryRecord record = new MemoryRecord(sessionId, role, content, "test", index);
        record.setId(id);
        record.setCreatedAt(LocalDateTime.now());
        return record;
    }
}