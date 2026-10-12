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
package io.agentscope.tutorial.chapter10.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * RAG 配置属性类
 *
 * <p>配置 RAG 系统的各个组件，包括嵌入模型、分块策略和检索参数。
 *
 * <h2>RAG 系统架构</h2>
 * <pre>
 * 文档 -> [分块] -> [嵌入] -> [向量存储]
 *                              ↓
 * 查询 -> [嵌入] -> [检索] -> [上下文增强] -> [生成回答]
 * </pre>
 *
 * <h2>AgentScope 学习要点</h2>
 * <ul>
 *   <li>嵌入模型的选择影响检索质量</li>
 *   <li>分块策略决定检索粒度</li>
 *   <li>检索参数控制召回率和精确度的平衡</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "rag")
public class RAGConfig {

    /**
     * 嵌入模型配置
     */
    private EmbeddingConfig embedding = new EmbeddingConfig();

    /**
     * 分块策略配置
     */
    private ChunkingConfig chunking = new ChunkingConfig();

    /**
     * 检索配置
     */
    private RetrievalConfig retrieval = new RetrievalConfig();

    /**
     * 嵌入模型配置类
     *
     * <p>配置用于将文本转换为向量的嵌入模型。
     * 支持多种嵌入模型提供商。
     */
    @Data
    public static class EmbeddingConfig {
        /**
         * 嵌入模型类型：OPENAI、DASHSCOPE、OLLAMA
         *
         * <p>各类型说明：
         * <ul>
         *   <li>OLLAMA - 本地运行，免费，适合开发测试</li>
         *   <li>OPENAI - 质量高，需要 API Key</li>
         *   <li>DASHSCOPE - 阿里云服务，支持中文</li>
         * </ul>
         */
        private String modelType = "OLLAMA";

        /**
         * 嵌入模型名称
         *
         * <p>常见模型：
         * <ul>
         *   <li>nomic-embed-text - Ollama 默认</li>
         *   <li>text-embedding-ada-002 - OpenAI</li>
         *   <li>text-embedding-v1 - DashScope</li>
         * </ul>
         */
        private String modelName = "nomic-embed-text";

        /**
         * 嵌入向量维度
         *
         * <p>必须与所选模型的输出维度匹配，
         * 同时也要与 Qdrant 集合的维度配置一致。
         */
        private int dimensions = 768;

        /**
         * 云嵌入服务的 API Key
         */
        private String apiKey;

        /**
         * 嵌入 API 的基础 URL（用于自定义端点）
         */
        private String baseUrl = "http://localhost:11434";
    }

    /**
     * 分块策略配置类
     *
     * <p>配置如何将长文档分割成小块，以便于检索和处理。
     */
    @Data
    public static class ChunkingConfig {
        /**
         * 分块策略：CHARACTER、PARAGRAPH、TOKEN、SEMANTIC
         *
         * <p>各策略说明：
         * <ul>
         *   <li>CHARACTER - 按固定字符数切分</li>
         *   <li>PARAGRAPH - 按段落边界切分（推荐）</li>
         *   <li>TOKEN - 按近似 Token 数切分</li>
         *   <li>SEMANTIC - 按语义边界切分</li>
         * </ul>
         */
        private String strategy = "PARAGRAPH";

        /**
         * 目标块大小（根据策略解释不同）
         *
         * <p>对于 CHARACTER 策略是字符数，
         * 对于 TOKEN 策略是近似 Token 数。
         */
        private int chunkSize = 500;

        /**
         * 块之间的重叠大小
         *
         * <p>适当的重叠可以保持上下文的连贯性，
         * 通常设置为块大小的 10-20%。
         */
        private int overlapSize = 50;

        /**
         * 保留的最小块长度
         *
         * <p>过短的块可能没有足够的语义信息，
         * 会被过滤掉。
         */
        private int minChunkLength = 10;

        /**
         * 最大块长度
         *
         * <p>防止单个块过大，影响检索效果。
         */
        private int maxChunkLength = 2000;
    }

    /**
     * 检索配置类
     *
     * <p>配置文档检索的相关参数。
     */
    @Data
    public static class RetrievalConfig {
        /**
         * 最大检索文档数
         *
         * <p>返回相似度最高的前 K 个文档。
         * 太小可能漏掉相关文档，太大会引入噪音。
         */
        private int topK = 5;

        /**
         * 最小相似度阈值（0-1）
         *
         * <p>只返回相似度大于此阈值的文档。
         * 0.5 是一个合理的起始值。
         */
        private double scoreThreshold = 0.5;

        /**
         * 是否启用混合搜索（结合向量搜索和关键词搜索）
         *
         * <p>混合搜索可以结合语义理解和精确匹配的优点。
         */
        private boolean hybridSearch = false;

        /**
         * 混合模式下关键词搜索的权重
         *
         * <p>0-1 之间，值越大关键词搜索的影响越大。
         */
        private double keywordWeight = 0.3;
    }
}