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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * RAG 查询操作的 REST 控制器
 *
 * <h2>API 端点</h2>
 * <ul>
 *   <li>POST /api/chat - RAG 查询（检索 + 生成）</li>
 *   <li>GET /api/chat/retrieve - 纯检索（不生成）</li>
 *   <li>POST /api/chat/hybrid - 混合搜索（向量 + 关键词）</li>
 *   <li>GET /api/chat/stats - 获取 RAG 系统统计</li>
 * </ul>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * // RAG 查询
 * curl -X POST -H "Content-Type: application/json" \
 *   -d '{"query":"什么是 AgentScope？"}' \
 *   http://localhost:8080/api/chat
 *
 * // 纯检索
 * curl -G --data-urlencode "query=什么是 AgentScope？" http://localhost:8080/api/chat/retrieve
 * }</pre>
 *
 * <h2>AgentScope 学习要点</h2>
 * <ul>
 *   <li>RAG 查询流程：查询嵌入 -> 向量检索 -> 上下文构建 -> 回答生成</li>
 *   <li>Document 对象包含内容、元数据和相似度分数</li>
 *   <li>混合搜索结合语义理解和精确匹配</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final RAGService ragService;
    private final RAGConfig ragConfig;

    /**
     * RAG 查询（检索 + 生成）
     *
     * <p>完整的 RAG 流程：
     * <ol>
     *   <li>将查询转换为嵌入向量</li>
     *   <li>从向量数据库检索相关文档</li>
     *   <li>基于检索结果构建上下文</li>
     *   <li>生成回答</li>
     * </ol>
     *
     * @param request 聊天请求，包含查询和选项
     * @return RAG 结果，包含回答和来源
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> query(@RequestBody ChatRequest request) {
        log.info("收到 RAG 查询：'{}'", request.query());

        try {
            // 从请求构建检索配置
            RAGConfig.RetrievalConfig retrievalConfig = buildRetrievalConfig(request);

            // 执行 RAG 查询
            CompletableFuture<RAGService.RAGResult> future =
                    ragService.query(request.query(), retrievalConfig);

            RAGService.RAGResult result = future.join();

            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("query", result.query());
            response.put("answer", result.answer());
            response.put("sources", result.sources());
            response.put("sourceCount", result.sources().size());
            response.put("formattedSources", result.getFormattedSources());

            log.info("RAG 查询完成：使用了 {} 个来源", result.sources().size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("RAG 查询失败", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "error", "RAG 查询失败：" + e.getMessage(),
                            "success", false
                    ));
        }
    }

    /**
     * 纯检索（不生成回答）
     *
     * <p>仅执行向量检索，返回相关文档，不调用 LLM 生成回答。
     * 适用于需要查看原始检索结果的场景。
     *
     * @param query 搜索查询
     * @param topK 要检索的文档数量（可选）
     * @return 检索到的文档列表
     */
    @GetMapping("/retrieve")
    public ResponseEntity<Map<String, Object>> retrieve(
            @RequestParam String query,
            @RequestParam(value = "topK", required = false) Integer topK) {

        log.info("收到检索请求：query='{}'，topK={}", query, topK);

        try {
            int k = topK != null && topK > 0
                    ? topK
                    : ragConfig.getRetrieval().getTopK();

            CompletableFuture<List<Document>> future = ragService.retrieve(query, k);
            List<Document> documents = future.join();

            // 构建响应
            List<Map<String, Object>> docList = documents.stream()
                    .map(this::documentToMap)
                    .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("query", query);
            response.put("documentCount", documents.size());
            response.put("documents", docList);

            log.info("检索完成：{} 个文档", documents.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("检索失败", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "error", "检索失败：" + e.getMessage(),
                            "success", false
                    ));
        }
    }

    /**
     * 混合搜索（结合向量搜索和关键词搜索）
     *
     * <p>混合搜索的优势：
     * <ul>
     *   <li>向量搜索：理解语义相似性</li>
     *   <li>关键词搜索：精确匹配专有名词</li>
     * </ul>
     *
     * @param request 混合搜索请求
     * @return 检索到的文档列表（带组合分数）
     */
    @PostMapping("/hybrid")
    public ResponseEntity<Map<String, Object>> hybridSearch(@RequestBody HybridSearchRequest request) {
        log.info("收到混合搜索：query='{}'，topK={}", request.query(), request.topK());

        try {
            int topK = request.topK() != null && request.topK() > 0
                    ? request.topK()
                    : ragConfig.getRetrieval().getTopK();

            CompletableFuture<List<Document>> future = ragService.hybridSearch(request.query(), topK);
            List<Document> documents = future.join();

            // 构建响应
            List<Map<String, Object>> docList = documents.stream()
                    .map(this::documentToMap)
                    .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("query", request.query());
            response.put("documentCount", documents.size());
            response.put("documents", docList);
            response.put("searchType", "hybrid");

            log.info("混合搜索完成：{} 个文档", documents.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("混合搜索失败", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "error", "混合搜索失败：" + e.getMessage(),
                            "success", false
                    ));
        }
    }

    /**
     * 获取 RAG 系统统计信息和配置
     *
     * @return 系统统计信息
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("success", true);

            // 嵌入配置
            Map<String, Object> embedding = new HashMap<>();
            embedding.put("modelType", ragConfig.getEmbedding().getModelType());
            embedding.put("modelName", ragConfig.getEmbedding().getModelName());
            embedding.put("dimensions", ragConfig.getEmbedding().getDimensions());
            stats.put("embedding", embedding);

            // 分块配置
            Map<String, Object> chunking = new HashMap<>();
            chunking.put("strategy", ragConfig.getChunking().getStrategy());
            chunking.put("chunkSize", ragConfig.getChunking().getChunkSize());
            chunking.put("overlapSize", ragConfig.getChunking().getOverlapSize());
            stats.put("chunking", chunking);

            // 检索配置
            Map<String, Object> retrieval = new HashMap<>();
            retrieval.put("topK", ragConfig.getRetrieval().getTopK());
            retrieval.put("scoreThreshold", ragConfig.getRetrieval().getScoreThreshold());
            retrieval.put("hybridSearch", ragConfig.getRetrieval().isHybridSearch());
            stats.put("retrieval", retrieval);

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            log.error("获取统计信息失败", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "获取统计信息失败：" + e.getMessage()));
        }
    }

    /**
     * 从请求构建检索配置
     *
     * @param request 聊天请求
     * @return 检索配置
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
     * 将 Document 转换为 Map 用于 JSON 响应
     *
     * @param doc 文档对象
     * @return Map 表示
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
     * 聊天请求（带 RAG 选项）
     *
     * @param query 用户查询
     * @param topK 要检索的文档数量
     * @param scoreThreshold 最小相似度分数（0-1）
     * @param hybridSearch 是否启用混合搜索
     */
    public record ChatRequest(
            String query,
            Integer topK,
            Double scoreThreshold,
            Boolean hybridSearch
    ) {
        /**
         * 验证查询不为空
         */
        public ChatRequest {
            if (query == null || query.isBlank()) {
                throw new IllegalArgumentException("查询不能为空");
            }
        }
    }

    /**
     * 混合搜索请求
     *
     * @param query 搜索查询
     * @param topK 返回结果数量
     */
    public record HybridSearchRequest(
            String query,
            Integer topK
    ) {
        /**
         * 验证查询不为空
         */
        public HybridSearchRequest {
            if (query == null || query.isBlank()) {
                throw new IllegalArgumentException("查询不能为空");
            }
        }
    }
}