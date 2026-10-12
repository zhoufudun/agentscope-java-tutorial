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
 * 核心 RAG（检索增强生成）服务
 *
 * <p>本服务实现完整的 RAG 工作流：
 * <ol>
 *   <li>接收用户查询</li>
 *   <li>将查询转换为嵌入向量</li>
 *   <li>从向量存储中检索相关文档</li>
 *   <li>从检索到的文档构建上下文</li>
 *   <li>基于上下文生成响应</li>
 * </ol>
 *
 * <h2>AgentScope 学习要点</h2>
 * <ul>
 *   <li><b>RAG 流程</b>：Query -> Embed -> Retrieve -> Context -> Generate</li>
 *   <li><b>向量检索</b>：基于嵌入向量的相似度搜索</li>
 *   <li><b>混合搜索</b>：结合向量搜索和关键词搜索</li>
 *   <li><b>分数阈值</b>：过滤低相关性文档</li>
 * </ul>
 *
 * <p>注意：这是一个演示服务。在生产环境中，需要集成 LLM API 进行实际的文本生成。
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 执行 RAG 查询
 * RAGResult result = ragService.query(
 *     "文档的主要内容是什么？",
 *     RAGConfig.builder()
 *         .topK(5)
 *         .scoreThreshold(0.6)
 *         .build()
 * ).join();
 *
 * System.out.println("答案: " + result.getAnswer());
 * System.out.println("来源: " + result.getSources());
 * }</pre>
 *
 * @author AgentScope Tutorial
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RAGService {

    private final RAGConfig ragConfig;
    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;

    /**
     * 执行 RAG 查询并返回结果
     *
     * <p>使用默认的检索配置。
     *
     * @param query 用户查询
     * @return 包含 RAG 结果的 CompletableFuture
     */
    public CompletableFuture<RAGResult> query(String query) {
        return query(query, new RAGConfig.RetrievalConfig());
    }

    /**
     * 执行带自定义检索配置的 RAG 查询
     *
     * <p>RAG 流程：
     * <ol>
     *   <li>查询向量化</li>
     *   <li>相似度检索</li>
     *   <li>构建上下文</li>
     *   <li>生成答案</li>
     *   <li>返回结果</li>
     * </ol>
     *
     * @param query 用户查询
     * @param retrievalConfig 检索配置
     * @return 包含 RAG 结果的 CompletableFuture
     */
    public CompletableFuture<RAGResult> query(String query, RAGConfig.RetrievalConfig retrievalConfig) {
        log.info("处理 RAG 查询：'{}'", query);

        return CompletableFuture.supplyAsync(() -> {
            try {
                // 步骤1：将查询转换为嵌入向量
                double[] queryEmbedding = embeddingService.embedText(query).join();
                log.debug("查询嵌入向量已生成：{} 维", queryEmbedding.length);

                // 步骤2：检索相关文档
                int topK = retrievalConfig.getTopK() > 0
                        ? retrievalConfig.getTopK()
                        : ragConfig.getRetrieval().getTopK();
                double threshold = retrievalConfig.getScoreThreshold() > 0
                        ? retrievalConfig.getScoreThreshold()
                        : ragConfig.getRetrieval().getScoreThreshold();

                List<Document> retrievedDocs = vectorStoreService
                        .searchWithThreshold(query, queryEmbedding, topK, threshold)
                        .join();

                log.info("检索到 {} 个超过阈值的文档", retrievedDocs.size());

                // 步骤3：从检索到的文档构建上下文
                String context = buildContext(retrievedDocs);
                log.debug("上下文已构建：{} 字符", context.length());

                // 步骤4：生成答案（占位符 - 生产环境需调用 LLM API）
                String answer = generateAnswer(query, context, retrievedDocs);
                log.info("答案已生成");

                // 步骤5：构建并返回结果
                List<RAGSource> sources = retrievedDocs.stream()
                        .map(this::convertToSource)
                        .collect(Collectors.toList());

                return new RAGResult(query, answer, context, sources);
            } catch (Exception e) {
                log.error("RAG 查询失败", e);
                throw new RuntimeException("RAG 查询失败：" + e.getMessage(), e);
            }
        });
    }

    /**
     * 执行纯检索操作（不生成）
     *
     * <p>仅返回检索到的文档，不进行答案生成。
     *
     * @param query 搜索查询
     * @return 检索到的文档列表
     */
    public CompletableFuture<List<Document>> retrieve(String query) {
        return retrieve(query, ragConfig.getRetrieval().getTopK());
    }

    /**
     * 执行带自定义 topK 的纯检索
     *
     * @param query 搜索查询
     * @param topK 要检索的文档数量
     * @return 检索到的文档列表
     */
    public CompletableFuture<List<Document>> retrieve(String query, int topK) {
        log.info("检索文档：query='{}'，topK={}", query, topK);

        return CompletableFuture.supplyAsync(() -> {
            try {
                double[] queryEmbedding = embeddingService.embedText(query).join();
                return vectorStoreService.search(query, queryEmbedding, topK).join();
            } catch (Exception e) {
                log.error("检索失败", e);
                throw new RuntimeException("检索失败：" + e.getMessage(), e);
            }
        });
    }

    /**
     * 执行混合搜索，结合向量搜索和关键词搜索
     *
     * <p>混合搜索通过加权组合向量搜索和关键词搜索的结果，
     * 可以提高检索的准确性和召回率。
     *
     * @param query 搜索查询
     * @param topK 要检索的文档数量
     * @return 带组合分数的检索文档列表
     */
    public CompletableFuture<List<Document>> hybridSearch(String query, int topK) {
        log.info("执行混合搜索：query='{}'，topK={}", query, topK);

        return CompletableFuture.supplyAsync(() -> {
            try {
                // 获取向量搜索结果
                double[] queryEmbedding = embeddingService.embedText(query).join();
                List<Document> vectorResults = vectorStoreService
                        .search(query, queryEmbedding, topK * 2)  // 多检索一些以便合并
                        .join();

                // 关键词搜索（简单实现 - 生产环境使用 BM25 或类似算法）
                List<Document> keywordResults = keywordSearch(query, topK * 2);

                // 按权重组合结果
                return combineResults(vectorResults, keywordResults, topK);
            } catch (Exception e) {
                log.error("混合搜索失败", e);
                throw new RuntimeException("混合搜索失败：" + e.getMessage(), e);
            }
        });
    }

    /**
     * 简单的关键词搜索实现
     *
     * <p>这是一个简化实现。生产环境中应使用 BM25 或 Elasticsearch 进行适当的关键词搜索。
     *
     * @param query 搜索查询
     * @param limit 最大结果数
     * @return 匹配的文档列表（占位符）
     */
    private List<Document> keywordSearch(String query, int limit) {
        // 简化实现
        // 生产环境中应使用专门的关键词搜索引擎
        log.debug("执行关键词搜索：{}", query);
        return new ArrayList<>();
    }

    /**
     * 合并向量搜索和关键词搜索结果
     *
     * <p>使用配置的权重对两种搜索结果进行加权合并。
     *
     * @param vectorResults 向量搜索结果
     * @param keywordResults 关键词搜索结果
     * @param topK 最终返回的结果数
     * @return 合并并排序后的结果
     */
    private List<Document> combineResults(
            List<Document> vectorResults,
            List<Document> keywordResults,
            int topK) {

        Map<String, Document> combinedMap = new HashMap<>();
        // 计算向量搜索权重（1 - 关键词权重）
        double vectorWeight = 1.0 - ragConfig.getRetrieval().getKeywordWeight();

        // 添加向量搜索结果，应用向量权重
        for (Document doc : vectorResults) {
            doc.setScore(doc.getScore() * vectorWeight);
            combinedMap.put(doc.getId(), doc);
        }

        // 添加关键词搜索结果，应用关键词权重
        for (Document doc : keywordResults) {
            Document existing = combinedMap.get(doc.getId());
            if (existing != null) {
                // 文档已存在，累加分数
                existing.setScore(existing.getScore() + doc.getScore() * ragConfig.getRetrieval().getKeywordWeight());
            } else {
                doc.setScore(doc.getScore() * ragConfig.getRetrieval().getKeywordWeight());
                combinedMap.put(doc.getId(), doc);
            }
        }

        // 按组合分数排序并返回 topK
        return combinedMap.values().stream()
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .limit(topK)
                .collect(Collectors.toList());
    }

    /**
     * 从检索到的文档构建上下文字符串
     *
     * <p>将多个文档的内容格式化为 LLM 可以理解的上下文格式。
     *
     * @param documents 检索到的文档
     * @return 格式化的上下文字符串
     */
    private String buildContext(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return "未找到相关文档。";
        }

        StringBuilder context = new StringBuilder();
        context.append("=== 相关文档 ===\n\n");

        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);
            String content = doc.getMetadata().getContentText();
            Double score = doc.getScore();

            context.append(String.format("[文档 %d]（相关度：%.2f）\n", i + 1, score != null ? score : 0.0));
            context.append(content).append("\n\n");
        }

        return context.toString();
    }

    /**
     * 根据查询和上下文生成答案
     *
     * <p>这是一个占位符实现。生产环境中需要集成 LLM API
     * （如 OpenAI、DashScope）来生成实际的答案。
     *
     * @param query 用户查询
     * @param context 检索到的上下文
     * @param documents 源文档
     * @return 生成的答案
     */
    private String generateAnswer(String query, String context, List<Document> documents) {
        // 占位符实现
        // 生产环境中需要调用 LLM API

        if (documents.isEmpty()) {
            return "未找到相关信息来回答您的问题。请尝试重新表述查询或提供更多上下文。";
        }

        StringBuilder answer = new StringBuilder();
        answer.append("根据检索到的文档，以下是我的回答：\n\n");

        // 从前几个文档中提取关键信息
        for (int i = 0; i < Math.min(3, documents.size()); i++) {
            Document doc = documents.get(i);
            String content = doc.getMetadata().getContentText();
            // 取前200字符作为摘要
            String summary = content.length() > 200 ? content.substring(0, 200) + "..." : content;
            answer.append(String.format("根据文档 %d：%s\n\n", i + 1, summary));
        }

        answer.append("\n注意：这是一个 RAG 演示响应。");
        answer.append("生产环境中需要集成 LLM API 进行适当的文本生成。");

        return answer.toString();
    }

    /**
     * 将 Document 转换为 RAGSource
     *
     * @param document 源文档
     * @return 带格式化元数据的 RAGSource
     */
    private RAGSource convertToSource(Document document) {
        String content = document.getMetadata().getContentText();
        Map<String, Object> metadata = document.getMetadata().getPayload();

        String title = metadata != null && metadata.containsKey("title")
                ? metadata.get("title").toString()
                : "未知";

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
     * RAG 查询结果
     *
     * @param query 原始用户查询
     * @param answer 生成的答案
     * @param context 检索到的上下文
     * @param sources 使用的源文档
     */
    public record RAGResult(
            String query,
            String answer,
            String context,
            List<RAGSource> sources
    ) {
        /**
         * 获取格式化的来源引用
         *
         * @return 格式化的来源字符串
         */
        public String getFormattedSources() {
            if (sources == null || sources.isEmpty()) {
                return "无可用来源。";
            }

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < sources.size(); i++) {
                RAGSource source = sources.get(i);
                sb.append(String.format("[%d] %s（分数：%.2f）\n",
                        i + 1, source.title(), source.score()));
            }
            return sb.toString();
        }
    }

    /**
     * 源文档引用
     *
     * @param id 文档 ID
     * @param title 文档标题
     * @param content 文档内容摘录
     * @param score 相关性分数
     * @param chunkIndex 文档内的块索引
     */
    public record RAGSource(
            String id,
            String title,
            String content,
            Double score,
            Integer chunkIndex
    ) {}
}