/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the “License”);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an “AS IS” BASIS,
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
 * RAG 服务单元测试
 *
 * <p>本测试类覆盖以下功能：
 * <ul>
 *   <li>文本分块策略</li>
 *   <li>文档处理</li>
 *   <li>RAG 查询操作（可能需要外部服务）</li>
 * </ul>
 *
 * <h2>AgentScope 学习要点</h2>
 * <ul>
 *   <li><b>TextChunker</b>：AgentScope 提供的文本分块工具</li>
 *   <li><b>SplitStrategy</b>：分块策略枚举（PARAGRAPH、CHARACTER、TOKEN 等）</li>
 *   <li><b>DocumentService</b>：文档处理服务，负责分块和嵌入</li>
 * </ul>
 *
 * <p>注意：部分测试需要 Ollama 和 Qdrant 运行。
 * 设置以下环境变量启用集成测试：
 * <ul>
 *   <li>OLLAMA_URL=http://localhost:11434</li>
 *   <li>QDRANT_URL=http://localhost:6333</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 */
@DisplayName(“RAG 服务测试”)
class RAGServiceTest {

    private static final Logger log = LoggerFactory.getLogger(RAGServiceTest.class);

    private DocumentService documentService;
    private RAGConfig ragConfig;

    /**
     * 每个测试前初始化配置
     */
    @BeforeEach
    void setUp() {
        // 创建 RAG 配置
        ragConfig = new RAGConfig();

        // 配置分块参数
        ragConfig.setChunking(new RAGConfig.ChunkingConfig());
        ragConfig.getChunking().setStrategy(“PARAGRAPH”);   // 按段落分块
        ragConfig.getChunking().setChunkSize(500);          // 块大小
        ragConfig.getChunking().setOverlapSize(50);         // 重叠大小
        ragConfig.getChunking().setMinChunkLength(10);      // 最小块长度
        ragConfig.getChunking().setMaxChunkLength(2000);    // 最大块长度

        // 配置检索参数
        ragConfig.setRetrieval(new RAGConfig.RetrievalConfig());
        ragConfig.getRetrieval().setTopK(5);                // 返回前 5 个结果
        ragConfig.getRetrieval().setScoreThreshold(0.5);    // 分数阈值

        // 配置嵌入参数
        ragConfig.setEmbedding(new RAGConfig.EmbeddingConfig());
        ragConfig.getEmbedding().setModelType(“OLLAMA”);
        ragConfig.getEmbedding().setModelName(“nomic-embed-text”);
        ragConfig.getEmbedding().setDimensions(768);
        ragConfig.getEmbedding().setBaseUrl(“http://localhost:11434”);

        // 创建文档服务（不使用嵌入服务，仅测试分块功能）
        documentService = new DocumentService(ragConfig, null);
    }

    // =================================================================================
    // 文本分块测试
    // =================================================================================

    /**
     * 测试按段落策略分块
     *
     * <p>PARAGRAPH 策略会在段落边界处分割文本，
     * 适合结构化的文档内容。
     */
    @Test
    @DisplayName(“应该按段落策略分块文本”)
    void testChunkByParagraph() {
        String text = “””
            这是第一段。它包含一些内容。

            这是第二段。它有更多信息。

            这是第三段，包含额外的细节。
            “””;

        // 调用 AgentScope 的 TextChunker 进行分块
        List<String> chunks = TextChunker.chunkText(text, 100, SplitStrategy.PARAGRAPH, 10);

        assertNotNull(chunks);
        assertFalse(chunks.isEmpty());
        log.info(“文本按段落策略分割为 {} 个块”, chunks.size());
        chunks.forEach(chunk -> log.debug(“块：'{}'”, chunk));
    }

    /**
     * 测试按字符策略分块
     *
     * <p>CHARACTER 策略会按固定字符数分割文本，
     * 生成大小均匀的块。
     */
    @Test
    @DisplayName(“应该按字符策略分块文本”)
    void testChunkByCharacter() {
        String text = “这是一段很长的文本，需要被分割成多个块以便在 RAG 系统中处理。” +
                “每个块应该大约相同的大小以确保均衡处理。” +
                “基于字符的分块可以确保块大小一致，不受内容结构影响。”;

        List<String> chunks = TextChunker.chunkText(text, 50, SplitStrategy.CHARACTER, 10);

        assertNotNull(chunks);
        assertFalse(chunks.isEmpty());
        assertTrue(chunks.size() > 1, “长文本应该被分割成多个块”);
        log.info(“文本按字符策略分割为 {} 个块”, chunks.size());
    }

    /**
     * 测试按 Token 近似策略分块
     *
     * <p>TOKEN 策略会按近似 Token 数分割文本，
     * 适合需要控制 LLM 输入长度的场景。
     */
    @Test
    @DisplayName(“应该按 Token 近似策略分块文本”)
    void testChunkByToken() {
        String text = “””
            人工智能正在改变世界。机器学习使计算机能够从数据中学习。
            深度学习使用多层神经网络。自然语言处理帮助计算机理解人类语言。
            计算机视觉使机器能够解释视觉信息。强化学习通过奖励和惩罚训练代理。
            “””;

        List<String> chunks = TextChunker.chunkText(text, 50, SplitStrategy.TOKEN, 10);

        assertNotNull(chunks);
        assertFalse(chunks.isEmpty());
        log.info(“文本按 Token 策略分割为 {} 个块”, chunks.size());
    }

    /**
     * 测试空文本的优雅处理
     */
    @Test
    @DisplayName(“应该优雅处理空文本”)
    void testChunkEmptyText() {
        String text = “”;

        List<String> chunks = TextChunker.chunkText(text, 100, SplitStrategy.PARAGRAPH, 10);

        assertNotNull(chunks);
        assertEquals(1, chunks.size());
        assertEquals(“”, chunks.get(0));
    }

    /**
     * 测试无效块大小应抛出异常
     */
    @Test
    @DisplayName(“无效块大小应抛出异常”)
    void testChunkInvalidSize() {
        String text = “一些文本内容”;

        assertThrows(IllegalArgumentException.class, () ->
                TextChunker.chunkText(text, 0, SplitStrategy.PARAGRAPH, 10)
        );
    }

    /**
     * 测试重叠大小大于等于块大小应抛出异常
     */
    @Test
    @DisplayName(“重叠大小>=块大小应抛出异常”)
    void testChunkOverlapExceedsSize() {
        String text = “一些文本内容”;

        assertThrows(IllegalArgumentException.class, () ->
                TextChunker.chunkText(text, 100, SplitStrategy.PARAGRAPH, 100)
        );
    }

    // =================================================================================
    // 文档服务测试
    // =================================================================================

    /**
     * 测试内容验证
     */
    @Test
    @DisplayName(“应该正确验证内容”)
    void testValidateContent() {
        // 有效内容
        assertTrue(documentService.validateContent(“这是有效的内容”));

        // 无效 - null
        assertFalse(documentService.validateContent(null));

        // 无效 - 空字符串
        assertFalse(documentService.validateContent(“”));

        // 无效 - 仅空白
        assertFalse(documentService.validateContent(“   “));
    }

    /**
     * 测试获取文档统计信息
     */
    @Test
    @DisplayName(“应该获取文档统计信息”)
    void testGetDocumentStats() {
        String content = “””
            这是一个测试文档。它包含多个段落。
            每个段落都提供了一些关于主题的上下文。

            这是第二段。它继续讨论。
            这里有更多内容以确保足够的长度用于测试。
            “””;

        Map<String, Object> stats = documentService.getDocumentStats(content);

        assertNotNull(stats);
        assertTrue(stats.containsKey(“totalLength”));    // 总长度
        assertTrue(stats.containsKey(“wordCount”));      // 词数
        assertTrue(stats.containsKey(“estimatedTokens”)); // 估计 Token 数
        assertTrue(stats.containsKey(“estimatedChunks”)); // 估计块数

        assertEquals(content.length(), stats.get(“totalLength”));
        log.info(“文档统计：{}”, stats);
    }

    /**
     * 测试文档处理和块创建
     */
    @Test
    @DisplayName(“应该处理文档并创建块”)
    void testProcessDocument() throws Exception {
        // 2.x 的 TextChunker 是”按 chunkSize 打包”的：PARAGRAPH 策略会把多个小段落
        // 合并进同一个 chunk 直到接近 chunkSize。默认 chunkSize=500 时，本用例这段
        // 约 355 字的短文本会被合并成 1 个 chunk。为验证”多段落被切分成多个块”的意图，
        // 这里将 chunkSize 调小到 100（与 testChunkByParagraph 一致），使每个段落各成一块。
        ragConfig.getChunking().setChunkSize(100);

        String title = “测试文档”;
        String content = “””
            这是一个用于 RAG 处理的测试文档。
            它包含多个段落来测试分块功能。

            这第二段提供了额外的上下文。
            系统应该能够适当地分割这些内容。

            第三段确保我们有足够的内容进行有意义的测试。
            多个句子有助于验证分块行为。
            “””;

        Map<String, String> metadata = Map.of(“author”, “测试作者”, “category”, “测试”);

        CompletableFuture<List<DocumentService.ChunkInfo>> future =
                documentService.processDocument(title, content, metadata);

        List<DocumentService.ChunkInfo> chunks = future.get();

        assertNotNull(chunks);
        assertFalse(chunks.isEmpty());
        assertTrue(chunks.size() >= 3, “应该至少有 3 个块”);

        // 验证块结构
        for (DocumentService.ChunkInfo chunk : chunks) {
            assertNotNull(chunk.content());   // 内容不为空
            assertNotNull(chunk.docId());     // 文档 ID 不为空
            assertNotNull(chunk.chunkId());   // 块 ID 不为空
            assertNotNull(chunk.metadata());  // 元数据不为空

            // 验证元数据
            assertEquals(title, chunk.metadata().get(“title”));
        }

        log.info(“文档处理为 {} 个块”, chunks.size());
        chunks.forEach(chunk -> log.debug(“块 [{}]：'{}'”,
                chunk.chunkId(),
                chunk.content().substring(0, Math.min(50, chunk.content().length()))));
    }

    // =================================================================================
    // 集成测试（需要外部服务）
    // =================================================================================

    /**
     * 测试完整 RAG 查询流程
     *
     * <p>此测试需要：
     * <ol>
     *   <li>Ollama 在 localhost:11434 运行并安装 nomic-embed-text 模型</li>
     *   <li>Qdrant 在 localhost:6333 运行</li>
     * </ol>
     */
    @Test
    @Disabled(“需要 Ollama 和 Qdrant 运行”)
    @DisplayName(“应该执行完整 RAG 查询流程”)
    void testRAGQueryFlow() throws Exception {
        // 完整集成测试需要：
        // 1. 使用实际配置创建 EmbeddingService
        // 2. 创建 VectorStoreService
        // 3. 创建 RAGService
        // 4. 添加测试文档
        // 5. 执行 RAG 查询
        // 6. 验证结果

        log.info(“跳过完整 RAG 测试 - 需要外部服务”);
    }

    // =================================================================================
    // 辅助方法
    // =================================================================================

    /**
     * 生成用于测试的示例文档内容
     *
     * @param paragraphs 段落数量
     * @return 生成的内容
     */
    private String generateSampleContent(int paragraphs) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= paragraphs; i++) {
            sb.append(String.format(“””
                    这是第 %d 段。它包含了关于正在讨论的主题的信息。
                    额外的句子提供了更多上下文和细节供 RAG 系统处理。
                    嵌入模型将把这段文本转换为向量表示。
                    “””, i));
            if (i < paragraphs) {
                sb.append(“\n\n”);
            }
        }
        return sb.toString();
    }

    /**
     * 验证块的大小分布是否合理
     *
     * @param chunks 块列表
     * @param expectedAvgSize 期望的平均大小
     * @param tolerance 容差
     */
    private void verifyChunkSizeDistribution(List<String> chunks, int expectedAvgSize, int tolerance) {
        int totalSize = chunks.stream().mapToInt(String::length).sum();
        int avgSize = totalSize / chunks.size();

        assertTrue(Math.abs(avgSize - expectedAvgSize) <= tolerance,
                String.format(“平均块大小 %d 与期望值 %d 相差过大”, avgSize, expectedAvgSize));

        log.info(“块大小分布验证：avg={}，min={}，max={}”,
                avgSize,
                chunks.stream().mapToInt(String::length).min().orElse(0),
                chunks.stream().mapToInt(String::length).max().orElse(0));
    }
}