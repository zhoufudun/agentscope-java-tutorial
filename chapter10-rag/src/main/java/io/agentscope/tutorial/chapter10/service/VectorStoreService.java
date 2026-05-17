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

import io.agentscope.core.rag.model.Document;
import io.agentscope.core.rag.model.DocumentMetadata;
import io.agentscope.core.rag.store.QdrantStore;
import io.agentscope.core.rag.store.VDBStoreBase;
import io.agentscope.core.rag.store.dto.SearchDocumentDto;
import io.agentscope.tutorial.chapter10.config.QdrantConfig;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgs构造函数;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service for managing vector storage operations.
 *
 * <p>This service provides an interface to Qdrant vector database for storing
 * document embeddings and performing similarity searches.
 *
 * <p>Example usage:
 * <pre>{@code
 * // Add document chunks to vector store
 * List<Document> chunks = createChunks(content, docId);
 * vectorStoreService.addDocuments(chunks).join();
 *
 * // Search for similar documents
 * List<Document> results = vectorStoreService.search("query text", queryEmbedding, 5);
 * }</pre>
 */
@Slf4j
@Service
@RequiredArgs构造函数
public class VectorStoreService {

    private final QdrantConfig qdrantConfig;
    private final EmbeddingService embeddingService;
    private VDBStoreBase vectorStore;

    /**
     * Initialize the vector store after bean construction.
     */
    @PostConstruct
    public void init() {
        log.info("Initializing vector store: location={}, collection={}",
                qdrantConfig.getLocation(), qdrantConfig.getCollectionName());

        this.vectorStore = createVectorStore();
        log.info("Vector store initialized successfully");
    }

    /**
     * Creates the vector store based on configuration.
     *
     * @return configured VDBStoreBase (QdrantStore)
     */
    private VDBStoreBase createVectorStore() {
        try {
            QdrantStore store = QdrantStore.builder()
                    .location(qdrantConfig.getLocation())
                    .collectionName(qdrantConfig.getCollectionName())
                    .dimensions(qdrantConfig.getDimensions())
                    .apiKey(qdrantConfig.getApiKey())
                    .useTransportLayerSecurity(qdrantConfig.isUseTls())
                    .checkCompatibility(qdrantConfig.isCheckCompatibility())
                    .build();
            log.debug("QdrantStore created successfully");
            return store;
        } catch (Exception e) {
            log.error("Failed to create QdrantStore", e);
            throw new RuntimeException("Failed to initialize vector store", e);
        }
    }

    /**
     * Add document chunks to the vector store.
     *
     * @param documents the documents with embeddings to store
     * @return CompletableFuture that completes when done
     */
    public CompletableFuture<Void> addDocuments(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        log.info("Adding {} documents to vector store", documents.size());
        return vectorStore.add(documents)
                .doOnSuccess(v -> log.info("Successfully added {} documents", documents.size()))
                .doOnError(e -> log.error("Failed to add documents", e))
                .toFuture();
    }

    /**
     * Search for similar documents in the vector store.
     *
     * @param query the query text
     * @param queryEmbedding the pre-computed query embedding vector
     * @param topK the maximum number of results to return
     * @return list of matching documents with similarity scores
     */
    public CompletableFuture<List<Document>> search(String query, double[] queryEmbedding, int topK) {
        log.debug("Searching for similar documents: query='{}', topK={}", query, topK);

        SearchDocumentDto searchDto = SearchDocumentDto.builder()
                .queryEmbedding(queryEmbedding)
                .limit(topK)
                .build();

        return vectorStore.search(searchDto)
                .doOnSuccess(results -> log.debug("Found {} matching documents", results.size()))
                .doOnError(e -> log.error("Search failed", e))
                .toFuture();
    }

    /**
     * Search for similar documents with a score threshold.
     *
     * @param query the query text
     * @param queryEmbedding the pre-computed query embedding vector
     * @param topK the maximum number of results to return
     * @param scoreThreshold minimum similarity score (0-1)
     * @return list of matching documents with similarity scores above threshold
     */
    public CompletableFuture<List<Document>> searchWithThreshold(
            String query, double[] queryEmbedding, int topK, double scoreThreshold) {
        log.debug("Searching with threshold: query='{}', topK={}, threshold={}",
                query, topK, scoreThreshold);

        SearchDocumentDto searchDto = SearchDocumentDto.builder()
                .queryEmbedding(queryEmbedding)
                .limit(topK)
                .scoreThreshold(scoreThreshold)
                .build();

        return vectorStore.search(searchDto)
                .doOnSuccess(results -> log.debug("Found {} documents above threshold", results.size()))
                .toFuture();
    }

    /**
     * Delete a document from the vector store.
     *
     * @param documentId the document ID to delete
     * @return CompletableFuture that completes with true if deletion succeeded
     */
    public CompletableFuture<Boolean> deleteDocument(String documentId) {
        log.info("Deleting document: {}", documentId);
        return vectorStore.delete(documentId)
                .doOnSuccess(result -> log.info("Document {} deleted: {}", documentId, result))
                .doOnError(e -> log.error("Failed to delete document {}", documentId, e))
                .toFuture();
    }

    /**
     * Create a Document with metadata and embedding.
     *
     * @param content the text content
     * @param docId the document ID
     * @param chunkId the chunk ID
     * @param embedding the embedding vector
     * @param additionalMetadata additional metadata key-value pairs
     * @return configured Document ready for storage
     */
    public io.agentscope.core.rag.model.Document createDocument(
            String content,
            String docId,
            String chunkId,
            double[] embedding,
            Map<String, String> additionalMetadata) {

        Map<String, Object> payload = new HashMap<>();
        if (additionalMetadata != null) {
            payload.putAll(additionalMetadata);
        }

        DocumentMetadata metadata = new DocumentMetadata(
                io.agentscope.core.message.TextBlock.builder().text(content).build(),
                docId,
                chunkId,
                payload
        );

        return io.agentscope.core.rag.model.Document.builder()
                .id(UUID.randomUUID().toString())
                .metadata(metadata)
                .embedding(embedding)
                .build();
    }

    /**
     * Get the underlying vector store.
     *
     * @return the VDBStoreBase
     */
    public VDBStoreBase getVectorStore() {
        return vectorStore;
    }

    /**
     * Get the collection name.
     *
     * @return the collection name
     */
    public String getCollectionName() {
        return qdrantConfig.getCollectionName();
    }

    /**
     * Clean up resources on shutdown.
     */
    @PreDestroy
    public void destroy() {
        log.info("Closing vector store connection");
        if (vectorStore instanceof AutoCloseable closeable) {
            try {
                closeable.close();
            } catch (Exception e) {
                log.warn("Error closing vector store", e);
            }
        }
    }
}