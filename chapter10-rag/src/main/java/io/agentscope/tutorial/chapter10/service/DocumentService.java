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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 文档处理服务
 *
 * <p>本服务处理文档上传、文本分块，以及准备块用于嵌入和存储到向量数据库。
 *
 * <h2>分块策略</h2>
 * <ul>
 *   <li><b>CHARACTER</b>：按固定字符数切分</li>
 *   <li><b>PARAGRAPH</b>：按段落边界切分（推荐用于大多数文档）</li>
 *   <li><b>TOKEN</b>：按近似 Token 数切分</li>
 *   <li><b>SEMANTIC</b>：按语义边界切分（句子/子句级别）</li>
 * </ul>
 *
 * <h2>AgentScope 学习要点</h2>
 * <ul>
 *   <li><b>TextChunker</b>：AgentScope 提供的文本分块工具</li>
 *   <li><b>SplitStrategy</b>：分块策略枚举</li>
 *   <li><b>Document</b>：RAG 模块中的文档类，包含元数据和嵌入向量</li>
 *   <li><b>DocumentMetadata</b>：文档元数据，包含内容、ID 和附加信息</li>
 * </ul>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 上传并处理文档
 * List<DocumentChunk> chunks = documentService.processDocument(
 *     "文档标题",
 *     "完整的文档文本内容...",
 *     Map.of("author", "张三")
 * ).join();
 *
 * // 仅分块文本（不完整处理）
 * List<String> textChunks = documentService.chunkText(content);
 * }</pre>
 *
 * @author AgentScope Tutorial
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final RAGConfig ragConfig;
    private final EmbeddingService embeddingService;

    /**
     * 处理文档：分块文本并准备嵌入
     *
     * <p>处理流程：
     * <ol>
     *   <li>将文本分割成块</li>
     *   <li>为每个块创建元数据</li>
     *   <li>过滤太短的块</li>
     * </ol>
     *
     * @param title 文档标题
     * @param content 完整文本内容
     * @param metadata 附加元数据
     * @return 准备好进行嵌入的已处理块列表
     */
    public CompletableFuture<List<ChunkInfo>> processDocument(
            String title,
            String content,
            Map<String, String> metadata) {

        String docId = UUID.randomUUID().toString();
        log.info("处理文档：title='{}'，contentLength={}", title, content.length());

        return CompletableFuture.supplyAsync(() -> {
            // 步骤1：将文本分割成块
            List<String> textChunks = chunkText(content);
            log.info("文档已分割为 {} 个块", textChunks.size());

            // 步骤2：创建带元数据的块信息
            List<ChunkInfo> chunks = new ArrayList<>();
            for (int i = 0; i < textChunks.size(); i++) {
                String chunkContent = textChunks.get(i);

                // 跳过空的或太短的块
                if (chunkContent.trim().length() < ragConfig.getChunking().getMinChunkLength()) {
                    continue;
                }

                // 构建块元数据
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

            log.info("从文档创建了 {} 个有效块", chunks.size());
            return chunks;
        });
    }

    /**
     * 根据配置的策略分块文本
     *
     * <p>使用 AgentScope 的 TextChunker 工具进行分块。
     *
     * @param content 要分块的文本
     * @return 文本块列表
     */
    public List<String> chunkText(String content) {
        String strategy = ragConfig.getChunking().getStrategy();
        int chunkSize = ragConfig.getChunking().getChunkSize();
        int overlapSize = ragConfig.getChunking().getOverlapSize();

        // 解析分块策略
        SplitStrategy splitStrategy = parseSplitStrategy(strategy);

        log.debug("分块文本：strategy={}，chunkSize={}，overlap={}",
                strategy, chunkSize, overlapSize);

        // 调用 AgentScope 的 TextChunker
        return TextChunker.chunkText(content, chunkSize, splitStrategy, overlapSize);
    }

    /**
     * 从字符串解析分块策略
     *
     * @param strategy 策略名称
     * @return SplitStrategy 枚举值
     */
    private SplitStrategy parseSplitStrategy(String strategy) {
        try {
            return SplitStrategy.valueOf(strategy.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("未知的分块策略 '{}'，使用 PARAGRAPH", strategy);
            return SplitStrategy.PARAGRAPH;
        }
    }

    /**
     * 创建带嵌入向量的文档块
     *
     * <p>为每个块调用嵌入服务生成向量，然后创建 Document 对象。
     *
     * @param chunks 块信息列表
     * @return 带嵌入向量的 Document 列表
     */
    public CompletableFuture<List<Document>> createChunksWithEmbeddings(List<ChunkInfo> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return CompletableFuture.completedFuture(List.of());
        }

        log.info("创建 {} 个带嵌入向量的文档块", chunks.size());

        return CompletableFuture.supplyAsync(() -> {
            List<Document> documents = new ArrayList<>();

            for (ChunkInfo chunk : chunks) {
                try {
                    // 为块生成嵌入向量
                    double[] embedding = embeddingService.embedText(chunk.content()).join();

                    // 构建 payload 元数据
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("title", chunk.metadata().get("title"));
                    payload.put("chunkIndex", chunk.metadata().get("chunkIndex"));
                    payload.put("totalChunks", chunk.metadata().get("totalChunks"));

                    // 创建 AgentScope 的 DocumentMetadata
                    io.agentscope.core.rag.model.DocumentMetadata metadata =
                            new io.agentscope.core.rag.model.DocumentMetadata(
                                    TextBlock.builder().text(chunk.content()).build(),
                                    chunk.docId(),
                                    chunk.chunkId(),
                                    payload
                            );

                    // 创建 Document 并设置嵌入向量
                    Document doc = new Document(metadata);
                    doc.setEmbedding(embedding);

                    documents.add(doc);
                } catch (Exception e) {
                    log.error("为块 {} 创建嵌入失败", chunk.chunkId(), e);
                }
            }

            log.info("创建了 {} 个带嵌入向量的文档", documents.size());
            return documents;
        });
    }

    /**
     * 验证文档内容
     *
     * @param content 要验证的内容
     * @return 如果内容有效可处理则返回 true
     */
    public boolean validateContent(String content) {
        if (content == null || content.isBlank()) {
            return false;
        }

        int maxLength = ragConfig.getChunking().getMaxChunkLength() * 100; // 粗略估计
        if (content.length() > maxLength) {
            log.warn("内容长度 {} 超过建议的最大值 {}", content.length(), maxLength);
        }

        return true;
    }

    /**
     * 获取文档统计信息
     *
     * @param content 文档内容
     * @return 统计信息 Map
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
     * 文档块信息记录
     *
     * @param content 块文本内容
     * @param docId 父文档 ID
     * @param chunkId 块唯一标识符
     * @param metadata 附加块元数据
     */
    public record ChunkInfo(
            String content,
            String docId,
            String chunkId,
            Map<String, Object> metadata
    ) {}

    /**
     * 文档处理状态枚举（本地定义，与 model 包中的保持一致）
     */
    public enum DocumentStatus {
        /** 等待处理 */
        PENDING,
        /** 处理中 */
        PROCESSING,
        /** 处理完成 */
        COMPLETED,
        /** 处理失败 */
        FAILED
    }
}