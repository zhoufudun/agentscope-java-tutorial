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
 * RAG configuration properties.
 *
 * <p>Configures the RAG system including embedding model, chunking strategy,
 * and retrieval parameters.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "rag")
public class RAGConfig {

    /**
     * Embedding model configuration.
     */
    private EmbeddingConfig embedding = new EmbeddingConfig();

    /**
     * Chunking strategy configuration.
     */
    private ChunkingConfig chunking = new ChunkingConfig();

    /**
     * Retrieval configuration.
     */
    private RetrievalConfig retrieval = new RetrievalConfig();

    /**
     * Embedding model configuration.
     */
    @Data
    public static class EmbeddingConfig {
        /**
         * Embedding model type: OPENAI, DASHSCOPE, OLLAMA.
         */
        private String modelType = "OLLAMA";

        /**
         * Model name for embedding.
         */
        private String modelName = "nomic-embed-text";

        /**
         * Embedding dimensions.
         */
        private int dimensions = 768;

        /**
         * API key for cloud embedding services.
         */
        private String apiKey;

        /**
         * Base URL for embedding API (for custom endpoints).
         */
        private String baseUrl = "http://localhost:11434";
    }

    /**
     * Chunking strategy configuration.
     */
    @Data
    public static class ChunkingConfig {
        /**
         * Chunking strategy: CHARACTER, PARAGRAPH, TOKEN, SEMANTIC.
         */
        private String strategy = "PARAGRAPH";

        /**
         * Target chunk size (interpreted based on strategy).
         */
        private int chunkSize = 500;

        /**
         * Overlap size between chunks.
         */
        private int overlapSize = 50;

        /**
         * Minimum chunk length to keep.
         */
        private int minChunkLength = 10;

        /**
         * Maximum chunk length.
         */
        private int maxChunkLength = 2000;
    }

    /**
     * Retrieval configuration.
     */
    @Data
    public static class RetrievalConfig {
        /**
         * Maximum number of documents to retrieve.
         */
        private int topK = 5;

        /**
         * Minimum similarity score threshold (0-1).
         */
        private double scoreThreshold = 0.5;

        /**
         * Enable hybrid search (combining vector and keyword search).
         */
        private boolean hybridSearch = false;

        /**
         * Keyword search weight in hybrid mode.
         */
        private double keywordWeight = 0.3;
    }
}