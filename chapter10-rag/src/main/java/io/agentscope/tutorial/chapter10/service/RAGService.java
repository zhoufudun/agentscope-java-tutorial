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
import io.agentscope.tutorial.chapter10.config.RAGConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Core RAG (Retrieval-Augmented Generation) service.
 *
 * <p>This service implements the RAG workflow:
 * <ol>
 *   <li>Receive user query</li>
 *   <li>Convert query to embedding vector</li>
 *   <li>检索相关文档 from vector store</li>
 *   <li>Build context from retrieved documents</li>
 *   <li>Generate response with retrieved context</li>
 * </ol>
 *
 * <p>This is a demonstration service. In production, you would integrate
 * with an LLM API for actual text generation.
 *
 * <p>Example usage:
 * <pre>{@code
 * // Perform RAG query
 * RAGResult result = ragService.query(
 *     "What is the main topic of the documents?",
 *     RAGConfig.builder()
 *         .topK(5)
 *         .scoreThreshold(0.6)
 *         .build()
 * ).join();
 *
 * System.out.println("Answer: " + result.getAnswer());
 * System.out.println("Sources: " + result.getSources());
 * }</pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RAGService {

    private final RAGConfig ragConfig;
    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;

    /**
     * Perform a RAG query and return results.
     *
     * @param query the user query
     * @return CompletableFuture containing the RAG result
     */
    public CompletableFuture<RAGResult> query(String query) {
        return query(query, new RAGConfig.RetrievalConfig());
    }

    /**
     * Perform a RAG query with custom retrieval configuration.
     *
     * @param query the user query
     * @param retrievalConfig the retrieval configuration
     * @return CompletableFuture containing the RAG result
     */
    public CompletableFuture<RAGResult> query(String query, RAGConfig.RetrievalConfig retrievalConfig) {
        log.info("Processing RAG query: '{}'", query);

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Step 1: Convert query to embedding
                double[] queryEmbedding = embeddingService.embedText(query).join();
                log.debug("Query embedding generated: {} dimensions", queryEmbedding.length);

                // Step 2: 检索相关文档
                int topK = retrievalConfig.getTopK() > 0
                        ? retrievalConfig.getTopK()
                        : ragConfig.getRetrieval().getTopK();
                double threshold = retrievalConfig.getScoreThreshold() > 0
                        ? retrievalConfig.getScoreThreshold()
                        : ragConfig.getRetrieval().getScoreThreshold();

                List<Document> retrievedDocs = vectorStoreService
                        .searchWithThreshold(query, queryEmbedding, topK, threshold)
                        .join();

                log.info("Retrieved {} documents above threshold", retrievedDocs.size());

                // Step 3: Build context from retrieved documents
                String context = buildContext(retrievedDocs);
                log.debug("Context built: {} characters", context.length());

                // Step 4: Generate answer (placeholder - in production, call LLM API)
                String answer = generateAnswer(query, context, retrievedDocs);
                log.info("Answer generated");

                // Step 5: Build and return result
                List<RAGSource> sources = retrievedDocs.stream()
                        .map(this::convertToSource)
                        .collect(Collectors.toList());

                return new RAGResult(query, answer, context, sources);
            } catch (Exception e) {
                log.error("RAG query failed", e);
                throw new RuntimeException("RAG query failed: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Perform pure retrieval without generation.
     *
     * @param query the search query
     * @return list of retrieved documents
     */
    public CompletableFuture<List<Document>> retrieve(String query) {
        return retrieve(query, ragConfig.getRetrieval().getTopK());
    }

    /**
     * Perform pure retrieval with custom topK.
     *
     * @param query the search query
     * @param topK the number of documents to retrieve
     * @return list of retrieved documents
     */
    public CompletableFuture<List<Document>> retrieve(String query, int topK) {
        log.info("Retrieving documents: query='{}', topK={}", query, topK);

        return CompletableFuture.supplyAsync(() -> {
            try {
                double[] queryEmbedding = embeddingService.embedText(query).join();
                return vectorStoreService.search(query, queryEmbedding, topK).join();
            } catch (Exception e) {
                log.error("Retrieval failed", e);
                throw new RuntimeException("Retrieval failed: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Perform hybrid search combining vector and keyword search.
     *
     * @param query the search query
     * @param topK the number of documents to retrieve
     * @return list of retrieved documents with combined scores
     */
    public CompletableFuture<List<Document>> hybridSearch(String query, int topK) {
        log.info("Performing hybrid search: query='{}', topK={}", query, topK);

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Get vector search results
                double[] queryEmbedding = embeddingService.embedText(query).join();
                List<Document> vectorResults = vectorStoreService
                        .search(query, queryEmbedding, topK * 2)
                        .join();

                // Keyword search (simple implementation - in production use BM25 or similar)
                List<Document> keywordResults = keywordSearch(query, topK * 2);

                // Combine results with weights
                return combineResults(vectorResults, keywordResults, topK);
            } catch (Exception e) {
                log.error("Hybrid search failed", e);
                throw new RuntimeException("Hybrid search failed: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Simple keyword search implementation.
     * In production, use BM25 or Elasticsearch for proper keyword search.
     *
     * @param query the search query
     * @param limit maximum results
     * @return list of matching documents (placeholder)
     */
    private List<Document> keywordSearch(String query, int limit) {
        // This is a simplified implementation
        // In production, you would use a proper keyword search engine
        log.debug("Performing keyword search for: {}", query);
        return new ArrayList<>();
    }

    /**
     * Combine vector and keyword search results.
     *
     * @param vectorResults results from vector search
     * @param keywordResults results from keyword search
     * @param topK final number of results
     * @return combined and ranked results
     */
    private List<Document> combineResults(
            List<Document> vectorResults,
            List<Document> keywordResults,
            int topK) {

        Map<String, Document> combinedMap = new HashMap<>();
        double vectorWeight = 1.0 - ragConfig.getRetrieval().getKeywordWeight();

        // Add vector results with vector weight
        for (Document doc : vectorResults) {
            doc.setScore(doc.getScore() * vectorWeight);
            combinedMap.put(doc.getId(), doc);
        }

        // Add keyword results with keyword weight
        for (Document doc : keywordResults) {
            Document existing = combinedMap.get(doc.getId());
            if (existing != null) {
                existing.setScore(existing.getScore() + doc.getScore() * ragConfig.getRetrieval().getKeywordWeight());
            } else {
                doc.setScore(doc.getScore() * ragConfig.getRetrieval().getKeywordWeight());
                combinedMap.put(doc.getId(), doc);
            }
        }

        // Sort by combined score and return topK
        return combinedMap.values().stream()
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .limit(topK)
                .collect(Collectors.toList());
    }

    /**
     * Build context string from retrieved documents.
     *
     * @param documents the retrieved documents
     * @return formatted context string
     */
    private String buildContext(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return "No relevant documents found.";
        }

        StringBuilder context = new StringBuilder();
        context.append("=== Relevant Documents ===\n\n");

        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);
            String content = doc.getMetadata().getContentText();
            Double score = doc.getScore();

            context.append(String.format("[Document %d] (relevance: %.2f)\n", i + 1, score != null ? score : 0.0));
            context.append(content).append("\n\n");
        }

        return context.toString();
    }

    /**
     * Generate answer from query and context.
     *
     * <p>This is a placeholder implementation. In production, you would
     * integrate with an LLM API (e.g., OpenAI, DashScope) to generate
     * the actual answer.
     *
     * @param query the user query
     * @param context the retrieved context
     * @param documents the source documents
     * @return generated answer
     */
    private String generateAnswer(String query, String context, List<Document> documents) {
        // Placeholder implementation
        // In production, this would call an LLM API

        if (documents.isEmpty()) {
            return "I couldn't find any relevant information to answer your question. " +
                   "Please try rephrasing your query or provide more context.";
        }

        StringBuilder answer = new StringBuilder();
        answer.append("Based on the retrieved documents, here is my answer:\n\n");

        // Extract key information from top documents
        for (int i = 0; i < Math.min(3, documents.size()); i++) {
            Document doc = documents.get(i);
            String content = doc.getMetadata().getContentText();
            // Take first 200 chars as summary
            String summary = content.length() > 200 ? content.substring(0, 200) + "..." : content;
            answer.append(String.format("According to document %d: %s\n\n", i + 1, summary));
        }

        answer.append("\nNote: This is a demonstration RAG response. ");
        answer.append("In production, you would integrate with an LLM API for proper text generation.");

        return answer.toString();
    }

    /**
     * Convert a Document to a RAGSource.
     *
     * @param document the source document
     * @return RAGSource with formatted metadata
     */
    private RAGSource convertToSource(Document document) {
        String content = document.getMetadata().getContentText();
        Map<String, Object> metadata = document.getMetadata().getPayload();

        String title = metadata != null && metadata.containsKey("title")
                ? metadata.get("title").toString()
                : "Unknown";

        Integer chunkIndex = metadata != null && metadata.containsKey("chunkIndex")
                ? (Integer) metadata.get("chunkIndex")
                : 0;

        return new RAGSource(
                document.getId(),
                title,
                content,
                document.getScore(),
                chunkIndex
        );
    }

    /**
     * Result of a RAG query.
     *
     * @param query the original user query
     * @param answer the generated answer
     * @param context the retrieved context
     * @param sources the source documents used
     */
    public record RAGResult(
            String query,
            String answer,
            String context,
            List<RAGSource> sources
    ) {
        /**
         * Get formatted sources as string.
         *
         * @return formatted source references
         */
        public String getFormattedSources() {
            if (sources == null || sources.isEmpty()) {
                return "No sources available.";
            }

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < sources.size(); i++) {
                RAGSource source = sources.get(i);
                sb.append(String.format("[%d] %s (score: %.2f)\n",
                        i + 1, source.title(), source.score()));
            }
            return sb.toString();
        }
    }

    /**
     * Source document reference.
     *
     * @param id the document ID
     * @param title the document title
     * @param content the document content excerpt
     * @param score the relevance score
     * @param chunkIndex the chunk index within the document
     */
    public record RAGSource(
            String id,
            String title,
            String content,
            Double score,
            Integer chunkIndex
    ) {}
}