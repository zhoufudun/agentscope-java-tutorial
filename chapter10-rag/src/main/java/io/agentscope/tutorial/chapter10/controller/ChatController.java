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
package io.agentscope.tutorial.chapter10.controller;

import io.agentscope.core.rag.model.Document;
import io.agentscope.tutorial.chapter10.config.RAGConfig;
import io.agentscope.tutorial.chapter10.service.RAGService;
import lombok.RequiredArgs构造函数;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * REST controller for RAG query operations.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>POST /api/chat - Query with RAG (retrieve + generate)</li>
 *   <li>GET /api/chat/retrieve - Pure retrieval without generation</li>
 *   <li>POST /api/chat/hybrid - Hybrid search (vector + keyword)</li>
 *   <li>GET /api/chat/stats - Get RAG system statistics</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * // Query with RAG
 * curl -X POST -H "Content-Type: application/json" \
 *   -d '{"query":"What is AgentScope?"}' \
 *   http://localhost:8080/api/chat
 *
 * // Pure retrieval
 * curl -G --data-urlencode "query=What is AgentScope?" http://localhost:8080/api/chat/retrieve
 * }</pre>
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgs构造函数
public class ChatController {

    private final RAGService ragService;
    private final RAGConfig ragConfig;

    /**
     * Query with RAG (retrieve + generate).
     *
     * @param request the chat request containing query and options
     * @return RAG result with answer and sources
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> query(@RequestBody ChatRequest request) {
        log.info("Received RAG query: '{}'", request.query());

        try {
            // Build retrieval config from request
            RAGConfig.RetrievalConfig retrievalConfig = buildRetrievalConfig(request);

            // Perform RAG query
            CompletableFuture<RAGService.RAGResult> future =
                    ragService.query(request.query(), retrievalConfig);

            RAGService.RAGResult result = future.join();

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("query", result.query());
            response.put("answer", result.answer());
            response.put("sources", result.sources());
            response.put("sourceCount", result.sources().size());
            response.put("formattedSources", result.getFormattedSources());

            log.info("RAG query completed: {} sources used", result.sources().size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("RAG query failed", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "error", "RAG query failed: " + e.getMessage(),
                            "success", false
                    ));
        }
    }

    /**
     * Pure retrieval without generation.
     *
     * @param query the search query
     * @param topK the number of documents to retrieve (optional)
     * @return list of retrieved documents
     */
    @GetMapping("/retrieve")
    public ResponseEntity<Map<String, Object>> retrieve(
            @Request参数 String query,
            @RequestParam(value = "topK", required = false) Integer topK) {

        log.info("Received retrieval request: query='{}', topK={}", query, topK);

        try {
            int k = topK != null && topK > 0
                    ? topK
                    : ragConfig.getRetrieval().getTopK();

            CompletableFuture<List<Document>> future = ragService.retrieve(query, k);
            List<Document> documents = future.join();

            // Build response
            List<Map<String, Object>> docList = documents.stream()
                    .map(this::documentToMap)
                    .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("query", query);
            response.put("documentCount", documents.size());
            response.put("documents", docList);

            log.info("Retrieval completed: {} documents", documents.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Retrieval failed", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "error", "Retrieval failed: " + e.getMessage(),
                            "success", false
                    ));
        }
    }

    /**
     * Hybrid search combining vector and keyword search.
     *
     * @param request the hybrid search request
     * @return list of retrieved documents with combined scores
     */
    @PostMapping("/hybrid")
    public ResponseEntity<Map<String, Object>> hybridSearch(@RequestBody HybridSearchRequest request) {
        log.info("Received hybrid search: query='{}', topK={}", request.query(), request.topK());

        try {
            int topK = request.topK() != null && request.topK() > 0
                    ? request.topK()
                    : ragConfig.getRetrieval().getTopK();

            CompletableFuture<List<Document>> future = ragService.hybridSearch(request.query(), topK);
            List<Document> documents = future.join();

            // Build response
            List<Map<String, Object>> docList = documents.stream()
                    .map(this::documentToMap)
                    .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("query", request.query());
            response.put("documentCount", documents.size());
            response.put("documents", docList);
            response.put("searchType", "hybrid");

            log.info("Hybrid search completed: {} documents", documents.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Hybrid search failed", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "error", "Hybrid search failed: " + e.getMessage(),
                            "success", false
                    ));
        }
    }

    /**
     * Get RAG system statistics and configuration.
     *
     * @return system stats
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("success", true);

            // Embedding config
            Map<String, Object> embedding = new HashMap<>();
            embedding.put("modelType", ragConfig.getEmbedding().getModelType());
            embedding.put("modelName", ragConfig.getEmbedding().getModelName());
            embedding.put("dimensions", ragConfig.getEmbedding().getDimensions());
            stats.put("embedding", embedding);

            // Chunking config
            Map<String, Object> chunking = new HashMap<>();
            chunking.put("strategy", ragConfig.getChunking().getStrategy());
            chunking.put("chunkSize", ragConfig.getChunking().getChunkSize());
            chunking.put("overlapSize", ragConfig.getChunking().getOverlapSize());
            stats.put("chunking", chunking);

            // Retrieval config
            Map<String, Object> retrieval = new HashMap<>();
            retrieval.put("topK", ragConfig.getRetrieval().getTopK());
            retrieval.put("scoreThreshold", ragConfig.getRetrieval().getScoreThreshold());
            retrieval.put("hybridSearch", ragConfig.getRetrieval().isHybridSearch());
            stats.put("retrieval", retrieval);

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            log.error("Failed to get stats", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get stats: " + e.getMessage()));
        }
    }

    /**
     * Build retrieval config from request.
     */
    private RAGConfig.RetrievalConfig buildRetrievalConfig(ChatRequest request) {
        RAGConfig.RetrievalConfig config = new RAGConfig.RetrievalConfig();
        config.setTopK(request.topK() != null ? request.topK() : ragConfig.getRetrieval().getTopK());
        config.setScoreThreshold(request.scoreThreshold() != null
                ? request.scoreThreshold()
                : ragConfig.getRetrieval().getScoreThreshold());
        config.setHybridSearch(request.hybridSearch() != null
                ? request.hybridSearch()
                : ragConfig.getRetrieval().isHybridSearch());
        return config;
    }

    /**
     * Convert Document to Map for JSON response.
     */
    private Map<String, Object> documentToMap(Document doc) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", doc.getId());
        map.put("score", doc.getScore());
        map.put("content", doc.getMetadata().getContentText());

        if (doc.getMetadata().getPayload() != null) {
            map.put("metadata", doc.getMetadata().getPayload());
        }

        return map;
    }

    /**
     * Chat request with RAG options.
     *
     * @param query the user query
     * @param topK number of documents to retrieve
     * @param scoreThreshold minimum similarity score (0-1)
     * @param hybridSearch enable hybrid search
     */
    public record ChatRequest(
            String query,
            Integer topK,
            Double scoreThreshold,
            Boolean hybridSearch
    ) {
        public ChatRequest {
            if (query == null || query.isBlank()) {
                throw new IllegalArgumentException("Query cannot be empty");
            }
        }
    }

    /**
     * Hybrid search request.
     *
     * @param query the search query
     * @param topK number of results to return
     */
    public record HybridSearchRequest(
            String query,
            Integer topK
    ) {
        public HybridSearchRequest {
            if (query == null || query.isBlank()) {
                throw new IllegalArgumentException("Query cannot be empty");
            }
        }
    }
}