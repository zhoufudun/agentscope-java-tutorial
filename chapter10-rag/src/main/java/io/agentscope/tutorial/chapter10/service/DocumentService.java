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
package io.agentscope.tutorial.chapter10.service;

import io.agentscope.core.message.TextBlock;
import io.agentscope.core.rag.model.Document;
import io.agentscope.core.rag.reader.SplitStrategy;
import io.agentscope.core.rag.reader.TextChunker;
import io.agentscope.tutorial.chapter10.config.RAGConfig;
import io.agentscope.tutorial.chapter10.model.DocumentStatus;
import lombok.RequiredArgs构造函数;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 文档处理服务 operations.
 *
 * <p>This service handles document upload, text chunking, and preparation
 * of chunks for embedding and storage in the vector database.
 *
 * <p>Chunking strategies:
 * <ul>
 *   <li>CHARACTER: Split by fixed character count</li>
 *   <li>PARAGRAPH: Split by paragraph boundaries (recommended for most documents)</li>
 *   <li>TOKEN: Split by approximate token count</li>
 *   <li>SEMANTIC: Split by semantic boundaries (sentence/clause level)</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * // Upload and process a document
 * List<DocumentChunk> chunks = documentService.processDocument(
 *     "Document title",
 *     "Full document text content...",
 *     Map.of("author", "John Doe")
 * ).join();
 *
 * // Chunk text without full processing
 * List<String> textChunks = documentService.chunkText(content);
 * }</pre>
 */
@Slf4j
@Service
@RequiredArgs构造函数
public class DocumentService {

    private final RAGConfig ragConfig;
    private final EmbeddingService embeddingService;

    /**
     * Process a document: chunk text and prepare for embedding.
     *
     * @param title the document title
     * @param content the full text content
     * @param metadata additional metadata
     * @return list of processed chunks ready for embedding
     */
    public CompletableFuture<List<ChunkInfo>> processDocument(
            String title,
            String content,
            Map<String, String> metadata) {

        String docId = UUID.randomUUID().toString();
        log.info("Processing document: title='{}', contentLength={}", title, content.length());

        return CompletableFuture.supplyAsync(() -> {
            // Step 1: Split text into chunks
            List<String> textChunks = chunkText(content);
            log.info("Document split into {} chunks", textChunks.size());

            // Step 2: Create chunk info with metadata
            List<ChunkInfo> chunks = new ArrayList<>();
            for (int i = 0; i < textChunks.size(); i++) {
                String chunkContent = textChunks.get(i);

                // Skip empty or too short chunks
                if (chunkContent.trim().length() < ragConfig.getChunking().getMinChunkLength()) {
                    continue;
                }

                Map<String, Object> chunkMetadata = new HashMap<>();
                chunkMetadata.put("title", title);
                chunkMetadata.put("chunkIndex", i);
                chunkMetadata.put("totalChunks", textChunks.size());
                if (metadata != null) {
                    chunkMetadata.putAll(metadata);
                }

                chunks.add(new ChunkInfo(
                        chunkContent,
                        docId,
                        String.valueOf(i),
                        chunkMetadata
                ));
            }

            log.info("Created {} valid chunks from document", chunks.size());
            return chunks;
        });
    }

    /**
     * Chunk text based on configured strategy.
     *
     * @param content the text to chunk
     * @return list of text chunks
     */
    public List<String> chunkText(String content) {
        String strategy = ragConfig.getChunking().getStrategy();
        int chunkSize = ragConfig.getChunking().getChunkSize();
        int overlapSize = ragConfig.getChunking().getOverlapSize();

        SplitStrategy splitStrategy = parseSplitStrategy(strategy);

        log.debug("Chunking text: strategy={}, chunkSize={}, overlap={}",
                strategy, chunkSize, overlapSize);

        return TextChunker.chunkText(content, chunkSize, splitStrategy, overlapSize);
    }

    /**
     * Parse split strategy from string.
     *
     * @param strategy the strategy name
     * @return SplitStrategy enum value
     */
    private SplitStrategy parseSplitStrategy(String strategy) {
        try {
            return SplitStrategy.valueOf(strategy.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown split strategy '{}', using PARAGRAPH", strategy);
            return SplitStrategy.PARAGRAPH;
        }
    }

    /**
     * Create document chunks with embeddings.
     *
     * @param chunks the chunk information
     * @return list of Documents with embeddings
     */
    public CompletableFuture<List<Document>> createChunksWithEmbeddings(List<ChunkInfo> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return CompletableFuture.completedFuture(List.of());
        }

        log.info("Creating {} document chunks with embeddings", chunks.size());

        return CompletableFuture.supplyAsync(() -> {
            List<Document> documents = new ArrayList<>();

            for (ChunkInfo chunk : chunks) {
                try {
                    // Generate embedding for chunk
                    double[] embedding = embeddingService.embedText(chunk.content()).join();

                    // Create Document with metadata
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("title", chunk.metadata().get("title"));
                    payload.put("chunkIndex", chunk.chunkIndex());
                    payload.put("totalChunks", chunk.metadata().get("totalChunks"));

                    io.agentscope.core.rag.model.DocumentMetadata metadata =
                            new io.agentscope.core.rag.model.DocumentMetadata(
                                    TextBlock.builder().text(chunk.content()).build(),
                                    chunk.docId(),
                                    chunk.chunkId(),
                                    payload
                            );

                    Document doc = Document.builder()
                            .id(UUID.randomUUID().toString())
                            .metadata(metadata)
                            .embedding(embedding)
                            .build();

                    documents.add(doc);
                } catch (Exception e) {
                    log.error("Failed to create embedding for chunk {}", chunk.chunkId(), e);
                }
            }

            log.info("Created {} documents with embeddings", documents.size());
            return documents;
        });
    }

    /**
     * Validate document content.
     *
     * @param content the content to validate
     * @return true if content is valid for processing
     */
    public boolean validateContent(String content) {
        if (content == null || content.isBlank()) {
            return false;
        }

        int maxLength = ragConfig.getChunking().getMaxChunkLength() * 100; // Rough estimate
        if (content.length() > maxLength) {
            log.warn("Content length {} exceeds maximum recommended {}", content.length(), maxLength);
        }

        return true;
    }

    /**
     * Get statistics about a document.
     *
     * @param content the document content
     * @return statistics map
     */
    public Map<String, Object> getDocumentStats(String content) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalLength", content.length());
        stats.put("wordCount", content.split("\\s+").length);
        stats.put("estimatedTokens", content.length() / 4);
        stats.put("chunkStrategy", ragConfig.getChunking().getStrategy());
        stats.put("chunkSize", ragConfig.getChunking().getChunkSize());

        List<String> chunks = chunkText(content);
        stats.put("estimatedChunks", chunks.size());

        return stats;
    }

    /**
     * Information about a document chunk.
     *
     * @param content the chunk text content
     * @param docId the parent document ID
     * @param chunkId the chunk unique identifier
     * @param metadata additional chunk metadata
     */
    public record ChunkInfo(
            String content,
            String docId,
            String chunkId,
            Map<String, Object> metadata
    ) {}

    /**
     * Document processing status enum.
     */
    public enum DocumentStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED
    }
}