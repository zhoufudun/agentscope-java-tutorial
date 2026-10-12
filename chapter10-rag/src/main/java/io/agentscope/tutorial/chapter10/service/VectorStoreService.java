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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 向量存储操作管理服务
 *
 * <p>本服务提供与 Qdrant 向量数据库交互的接口，用于存储文档嵌入向量和执行相似度搜索。
 *
 * <h2>AgentScope 学习要点</h2>
 * <ul>
 *   <li><b>VDBStoreBase</b>：向量数据库存储的抽象基类</li>
 *   <li><b>QdrantStore</b>：Qdrant 向量数据库的具体实现</li>
 *   <li><b>SearchDocumentDto</b>：搜索请求的数据传输对象</li>
 *   <li><b>Document</b>：包含嵌入向量和元数据的文档对象</li>
 *   <li><b>DocumentMetadata</b>：文档的元数据，包含内容、ID 和 payload</li>
 * </ul>
 *
 * <h2>Qdrant 向量数据库</h2>
 * <p>Qdrant 是一个高性能的向量相似度搜索引擎，特点：
 * <ul>
 *   <li>支持高维向量存储和检索</li>
 *   <li>支持过滤和条件搜索</li>
 *   <li>支持分布式部署</li>
 *   <li>提供 REST 和 gRPC 接口</li>
 * </ul>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 添加文档块到向量存储
 * List<Document> chunks = createChunks(content, docId);
 * vectorStoreService.addDocuments(chunks).join();
 *
 * // 搜索相似文档
 * List<Document> results = vectorStoreService.search("查询文本", queryEmbedding, 5);
 * }</pre>
 *
 * @author AgentScope Tutorial
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VectorStoreService {

    private final QdrantConfig qdrantConfig;
    private final EmbeddingService embeddingService;

    /** 向量存储实例 */
    private VDBStoreBase vectorStore;

    /**
     * Bean 构造后初始化向量存储
     *
     * <p>根据配置创建 Qdrant 向量存储实例。
     */
    @PostConstruct
    public void init() {
        log.info("初始化向量存储：location={}，collection={}",
                qdrantConfig.getLocation(), qdrantConfig.getCollectionName());

        this.vectorStore = createVectorStore();
        log.info("向量存储初始化成功");
    }

    /**
     * 根据配置创建向量存储
     *
     * <p>使用 Builder 模式配置 QdrantStore 的各项参数。
     *
     * @return 配置好的 VDBStoreBase（QdrantStore）
     * @throws RuntimeException 如果创建失败
     */
    private VDBStoreBase createVectorStore() {
        try {
            QdrantStore store = QdrantStore.builder()
                    .location(qdrantConfig.getLocation())       // Qdrant 服务地址
                    .collectionName(qdrantConfig.getCollectionName()) // 集合名称
                    .dimensions(qdrantConfig.getDimensions())   // 向量维度
                    .apiKey(qdrantConfig.getApiKey())           // API 密钥（可选）
                    .useTransportLayerSecurity(qdrantConfig.isUseTls()) // 是否使用 TLS
                    .checkCompatibility(qdrantConfig.isCheckCompatibility()) // 检查兼容性
                    .build();
            log.debug("QdrantStore 创建成功");
            return store;
        } catch (Exception e) {
            log.error("创建 QdrantStore 失败", e);
            throw new RuntimeException("初始化向量存储失败", e);
        }
    }

    /**
     * 添加文档块到向量存储
     *
     * <p>将带有嵌入向量的文档存储到 Qdrant。
     *
     * @param documents 要存储的带嵌入向量的文档
     * @return 完成时的 CompletableFuture
     */
    public CompletableFuture<Void> addDocuments(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        log.info("添加 {} 个文档到向量存储", documents.size());
        return vectorStore.add(documents)
                .doOnSuccess(v -> log.info("成功添加 {} 个文档", documents.size()))
                .doOnError(e -> log.error("添加文档失败", e))
                .toFuture();
    }

    /**
     * 在向量存储中搜索相似文档
     *
     * <p>基于查询向量进行余弦相似度搜索。
     *
     * @param query 查询文本（用于日志）
     * @param queryEmbedding 预计算的查询嵌入向量
     * @param topK 返回的最大结果数
     * @return 带相似度分数的匹配文档列表
     */
    public CompletableFuture<List<Document>> search(String query, double[] queryEmbedding, int topK) {
        log.debug("搜索相似文档：query='{}'，topK={}", query, topK);

        // 构建搜索请求 DTO
        SearchDocumentDto searchDto = SearchDocumentDto.builder()
                .queryEmbedding(queryEmbedding)  // 查询向量
                .limit(topK)                      // 返回数量限制
                .build();

        return vectorStore.search(searchDto)
                .doOnSuccess(results -> log.debug("找到 {} 个匹配文档", results.size()))
                .doOnError(e -> log.error("搜索失败", e))
                .toFuture();
    }

    /**
     * 带分数阈值的相似文档搜索
     *
     * <p>只返回相似度分数高于阈值的文档，有助于过滤低相关性结果。
     *
     * @param query 查询文本（用于日志）
     * @param queryEmbedding 预计算的查询嵌入向量
     * @param topK 返回的最大结果数
     * @param scoreThreshold 最小相似度分数（0-1）
     * @return 分数高于阈值的匹配文档列表
     */
    public CompletableFuture<List<Document>> searchWithThreshold(
            String query, double[] queryEmbedding, int topK, double scoreThreshold) {
        log.debug("带阈值搜索：query='{}'，topK={}，threshold={}",
                query, topK, scoreThreshold);

        SearchDocumentDto searchDto = SearchDocumentDto.builder()
                .queryEmbedding(queryEmbedding)
                .limit(topK)
                .scoreThreshold(scoreThreshold)  // 分数阈值
                .build();

        return vectorStore.search(searchDto)
                .doOnSuccess(results -> log.debug("找到 {} 个超过阈值的文档", results.size()))
                .toFuture();
    }

    /**
     * 从向量存储中删除文档
     *
     * @param documentId 要删除的文档 ID
     * @return 删除成功返回 true 的 CompletableFuture
     */
    public CompletableFuture<Boolean> deleteDocument(String documentId) {
        log.info("删除文档：{}", documentId);
        return vectorStore.delete(documentId)
                .doOnSuccess(result -> log.info("文档 {} 删除结果：{}", documentId, result))
                .doOnError(e -> log.error("删除文档 {} 失败", documentId, e))
                .toFuture();
    }

    /**
     * 创建带元数据和嵌入向量的 Document
     *
     * <p>这是一个工具方法，用于创建符合 AgentScope RAG 规范的 Document 对象。
     *
     * @param content 文本内容
     * @param docId 文档 ID
     * @param chunkId 块 ID
     * @param embedding 嵌入向量
     * @param additionalMetadata 附加元数据键值对
     * @return 准备好存储的 Document
     */
    public io.agentscope.core.rag.model.Document createDocument(
            String content,
            String docId,
            String chunkId,
            double[] embedding,
            Map<String, String> additionalMetadata) {

        // 构建 payload 元数据
        Map<String, Object> payload = new HashMap<>();
        if (additionalMetadata != null) {
            payload.putAll(additionalMetadata);
        }

        // 创建 DocumentMetadata
        DocumentMetadata metadata = new DocumentMetadata(
                io.agentscope.core.message.TextBlock.builder().text(content).build(),  // 内容块
                docId,      // 文档 ID
                chunkId,    // 块 ID
                payload     // 附加元数据
        );

        // 创建 Document 并设置嵌入向量
        Document doc = new io.agentscope.core.rag.model.Document(metadata);
        doc.setEmbedding(embedding);
        return doc;
    }

    /**
     * 获取底层向量存储实例
     *
     * @return VDBStoreBase 实例
     */
    public VDBStoreBase getVectorStore() {
        return vectorStore;
    }

    /**
     * 获取集合名称
     *
     * @return 集合名称
     */
    public String getCollectionName() {
        return qdrantConfig.getCollectionName();
    }

    /**
     * 关闭时清理资源
     *
     * <p>确保正确关闭向量存储连接。
     */
    @PreDestroy
    public void destroy() {
        log.info("关闭向量存储连接");
        if (vectorStore instanceof AutoCloseable closeable) {
            try {
                closeable.close();
            } catch (Exception e) {
                log.warn("关闭向量存储时出错", e);
            }
        }
    }
}