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
package io.agentscope.tutorial.chapter10;

import io.agentscope.core.rag.model.Document;
import io.agentscope.core.rag.reader.SplitStrategy;
import io.agentscope.core.rag.reader.TextChunker;
import io.agentscope.tutorial.chapter10.config.RAGConfig;
import io.agentscope.tutorial.chapter10.service.DocumentService;
import io.agentscope.tutorial.chapter10.service.RAGService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RAG Service.
 *
 * <p>These tests cover:
 * <ul>
 *   <li>Text chunking strategies</li>
 *   <li>Document processing</li>
 *   <li>RAG query operations (may require external services)</li>
 * </ul>
 *
 * <p>Note: Some tests require Ollama and Qdrant to be running.
 * Set environment variables to enable integration tests:
 * <ul>
 *   <li>OLLAMA_URL=http://localhost:11434</li>
 *   <li>QDRANT_URL=http://localhost:6333</li>
 * </ul>
 */
@DisplayName("RAG Service Tests")
class RAGServiceTest {

    private static final Logger log = LoggerFactory.getLogger(RAGServiceTest.class);

    private DocumentService documentService;
    private RAGConfig ragConfig;

    @BeforeEach
    void setUp() {
        // Create RAG configuration
        ragConfig = new RAGConfig();
        ragConfig.setChunking(new RAGConfig.ChunkingConfig());
        ragConfig.getChunking().setStrategy("PARAGRAPH");
        ragConfig.getChunking().setChunkSize(500);
        ragConfig.getChunking().setOverlapSize(50);
        ragConfig.getChunking().setMinChunkLength(10);
        ragConfig.getChunking().setMaxChunkLength(2000);

        ragConfig.setRetrieval(new RAGConfig.RetrievalConfig());
        ragConfig.getRetrieval().setTopK(5);
        ragConfig.getRetrieval().setScoreThreshold(0.5);

        ragConfig.setEmbedding(new RAGConfig.EmbeddingConfig());
        ragConfig.getEmbedding().setModelType("OLLAMA");
        ragConfig.getEmbedding().setModelName("nomic-embed-text");
        ragConfig.getEmbedding().setDimensions(768);
        ragConfig.getEmbedding().setBaseUrl("http://localhost:11434");

        documentService = new DocumentService(ragConfig, null);
    }

    // =================================================================================
    // Text Chunking Tests
    // =================================================================================

    @Test
    @DisplayName("Should chunk text by paragraph strategy")
    void testChunkByParagraph() {
        String text = """
            This is the first paragraph. It contains some content.

            This is the second paragraph. It has more information.

            This is the third paragraph with additional details.
            """;

        List<String> chunks = TextChunker.chunkText(text, 100, SplitStrategy.PARAGRAPH, 10);

        assertNotNull(chunks);
        assertFalse(chunks.isEmpty());
        log.info("Text split into {} chunks by paragraph strategy", chunks.size());
        chunks.forEach(chunk -> log.debug("Chunk: '{}'", chunk));
    }

    @Test
    @DisplayName("Should chunk text by character strategy")
    void testChunkByCharacter() {
        String text = "This is a very long text that needs to be split into multiple chunks for processing in a RAG system. " +
                "Each chunk should be approximately the same size to ensure balanced processing. " +
                "The character-based chunking ensures consistent chunk sizes regardless of content structure.";

        List<String> chunks = TextChunker.chunkText(text, 50, SplitStrategy.CHARACTER, 10);

        assertNotNull(chunks);
        assertFalse(chunks.isEmpty());
        assertTrue(chunks.size() > 1, "Long text should be split into multiple chunks");
        log.info("Text split into {} chunks by character strategy", chunks.size());
    }

    @Test
    @DisplayName("Should chunk text by token approximation")
    void testChunkByToken() {
        String text = """
            Artificial intelligence is transforming the world. Machine learning enables computers
            to learn from data. Deep learning uses neural networks with multiple layers.
            Natural language processing helps computers understand human language.
            Computer vision allows machines to interpret visual information.
            Reinforcement learning trains agents through rewards and penalties.
            """;

        List<String> chunks = TextChunker.chunkText(text, 50, SplitStrategy.TOKEN, 10);

        assertNotNull(chunks);
        assertFalse(chunks.isEmpty());
        log.info("Text split into {} chunks by token strategy", chunks.size());
    }

    @Test
    @DisplayName("Should handle empty text gracefully")
    void testChunkEmptyText() {
        String text = "";

        List<String> chunks = TextChunker.chunkText(text, 100, SplitStrategy.PARAGRAPH, 10);

        assertNotNull(chunks);
        assertEquals(1, chunks.size());
        assertEquals("", chunks.get(0));
    }

    @Test
    @DisplayName("Should throw exception for invalid chunk size")
    void testChunkInvalidSize() {
        String text = "Some text content";

        assertThrows(IllegalArgumentException.class, () ->
                TextChunker.chunkText(text, 0, SplitStrategy.PARAGRAPH, 10)
        );
    }

    @Test
    @DisplayName("Should throw exception when overlap >= chunk size")
    void testChunkOverlapExceedsSize() {
        String text = "Some text content";

        assertThrows(IllegalArgumentException.class, () ->
                TextChunker.chunkText(text, 100, SplitStrategy.PARAGRAPH, 100)
        );
    }

    // =================================================================================
    // Document Service Tests
    // =================================================================================

    @Test
    @DisplayName("Should validate content correctly")
    void testValidateContent() {
        // Valid content
        assertTrue(documentService.validateContent("This is valid content"));

        // Invalid - null
        assertFalse(documentService.validateContent(null));

        // Invalid - empty
        assertFalse(documentService.validateContent(""));

        // Invalid - whitespace only
        assertFalse(documentService.validateContent("   "));
    }

    @Test
    @DisplayName("Should get document statistics")
    void testGetDocumentStats() {
        String content = """
            This is a test document. It contains multiple paragraphs.
            Each paragraph provides some context about the topic.

            This is the second paragraph. It continues the discussion.
            More content here to ensure sufficient length for testing.
            """;

        Map<String, Object> stats = documentService.getDocumentStats(content);

        assertNotNull(stats);
        assertTrue(stats.containsKey("totalLength"));
        assertTrue(stats.containsKey("wordCount"));
        assertTrue(stats.containsKey("estimatedTokens"));
        assertTrue(stats.containsKey("estimatedChunks"));

        assertEquals(content.length(), stats.get("totalLength"));
        log.info("Document stats: {}", stats);
    }

    @Test
    @DisplayName("Should process document and create chunks")
    void testProcessDocument() throws Exception {
        // 2.x 的 TextChunker 是“按 chunkSize 打包”的：PARAGRAPH 策略会把多个小段落
        // 合并进同一个 chunk 直到接近 chunkSize。默认 chunkSize=500 时，本用例这段
        // 约 355 字的短文本会被合并成 1 个 chunk。为验证“多段落被切分成多个块”的意图，
        // 这里将 chunkSize 调小到 100（与 testChunkByParagraph 一致），使每个段落各成一块。
        ragConfig.getChunking().setChunkSize(100);

        String title = "Test Document";
        String content = """
            This is a test document for RAG processing.
            It contains multiple paragraphs to test the chunking functionality.

            This second paragraph provides additional context.
            The system should be able to split this content appropriately.

            A third paragraph ensures we have enough content for meaningful testing.
            Multiple sentences help verify the chunking behavior.
            """;

        Map<String, String> metadata = Map.of("author", "Test Author", "category", "test");

        CompletableFuture<List<DocumentService.ChunkInfo>> future =
                documentService.processDocument(title, content, metadata);

        List<DocumentService.ChunkInfo> chunks = future.get();

        assertNotNull(chunks);
        assertFalse(chunks.isEmpty());
        assertTrue(chunks.size() >= 3, "Should have at least 3 chunks");

        // Verify chunk structure
        for (DocumentService.ChunkInfo chunk : chunks) {
            assertNotNull(chunk.content());
            assertNotNull(chunk.docId());
            assertNotNull(chunk.chunkId());
            assertNotNull(chunk.metadata());

            // Verify metadata
            assertEquals(title, chunk.metadata().get("title"));
        }

        log.info("Document processed into {} chunks", chunks.size());
        chunks.forEach(chunk -> log.debug("Chunk [{}]: '{}'", chunk.chunkId(), chunk.content().substring(0, Math.min(50, chunk.content().length()))));
    }

    // =================================================================================
    // Integration Tests (Require External Services)
    // =================================================================================

    @Test
    @Disabled("Requires Ollama and Qdrant to be running")
    @DisplayName("Should perform full RAG query flow")
    void testRAGQueryFlow() throws Exception {
        // This test requires:
        // 1. Ollama running at localhost:11434 with nomic-embed-text model
        // 2. Qdrant running at localhost:6333

        // For a full integration test, you would:
        // 1. Create EmbeddingService with actual configuration
        // 2. Create VectorStoreService
        // 3. Create RAGService
        // 4. Add test documents
        // 5. Perform RAG query
        // 6. Verify results

        log.info("Skipping full RAG test - requires external services");
    }

    // =================================================================================
    // Helper Methods
    // =================================================================================

    /**
     * Generates sample document content for testing.
     */
    private String generateSampleContent(int paragraphs) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= paragraphs; i++) {
            sb.append(String.format("""
                    This is paragraph %d. It contains information about the topic being discussed.
                    Additional sentences provide more context and detail for the RAG system to process.
                    The embedding model will convert this text into a vector representation.
                    """, i));
            if (i < paragraphs) {
                sb.append("\n\n");
            }
        }
        return sb.toString();
    }

    /**
     * Verifies that chunks have appropriate size distribution.
     */
    private void verifyChunkSizeDistribution(List<String> chunks, int expectedAvgSize, int tolerance) {
        int totalSize = chunks.stream().mapToInt(String::length).sum();
        int avgSize = totalSize / chunks.size();

        assertTrue(Math.abs(avgSize - expectedAvgSize) <= tolerance,
                String.format("Average chunk size %d differs too much from expected %d", avgSize, expectedAvgSize));

        log.info("Chunk size distribution verified: avg={}, min={}, max={}",
                avgSize,
                chunks.stream().mapToInt(String::length).min().orElse(0),
                chunks.stream().mapToInt(String::length).max().orElse(0));
    }
}