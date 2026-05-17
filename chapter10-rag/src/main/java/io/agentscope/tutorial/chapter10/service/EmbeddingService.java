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

import io.agentscope.core.embedding.EmbeddingModel;
import io.agentscope.core.embedding.ollama.OllamaTextEmbedding;
import io.agentscope.tutorial.chapter10.config.RAGConfig;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgs构造函数;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for generating text embeddings using various embedding models.
 *
 * <p>This service provides a unified interface for text vectorization, supporting
 * multiple embedding providers (Ollama, OpenAI, DashScope).
 *
 * <p>Example usage:
 * <pre>{@code
 * // Generate embedding for a single text
 * double[] embedding = embeddingService.embedText("Hello, world!");
 *
 * // Generate embeddings for multiple texts
 * List<double[]> embeddings = embeddingService.embedTexts(List.of("Text 1", "Text 2"));
 * }</pre>
 */
@Slf4j
@Service
@RequiredArgs构造函数
public class EmbeddingService {

    private final RAGConfig ragConfig;
    private EmbeddingModel embeddingModel;

    /**
     * Initialize the embedding model after bean construction.
     */
    @PostConstruct
    public void init() {
        String modelType = ragConfig.getEmbedding().getModelType();
        log.info("Initializing embedding model: type={}, model={}",
                modelType, ragConfig.getEmbedding().getModelName());

        this.embeddingModel = createEmbeddingModel(modelType);
        log.info("Embedding model initialized: dimensions={}", embeddingModel.getDimensions());
    }

    /**
     * Creates an embedding model based on the specified type.
     *
     * @param modelType the model type (OLLAMA, OPENAI, DASHSCOPE)
     * @return the configured EmbeddingModel
     */
    private EmbeddingModel createEmbeddingModel(String modelType) {
        return switch (modelType.toUpperCase()) {
            case "OLLAMA" -> createOllamaEmbedding();
            case "OPENAI" -> createOpenAIEmbedding();
            case "DASHSCOPE" -> createDashScopeEmbedding();
            default -> throw new IllegalArgumentException("Unsupported embedding model type: " + modelType);
        };
    }

    /**
     * Creates an Ollama text embedding model.
     *
     * @return configured OllamaTextEmbedding
     */
    private OllamaTextEmbedding createOllamaEmbedding() {
        return OllamaTextEmbedding.builder()
                .baseUrl(ragConfig.getEmbedding().getBaseUrl())
                .modelName(ragConfig.getEmbedding().getModelName())
                .dimensions(ragConfig.getEmbedding().getDimensions())
                .build();
    }

    /**
     * Creates an OpenAI text embedding model.
     *
     * @return configured OpenAITextEmbedding (placeholder for demonstration)
     */
    private io.agentscope.core.embedding.openai.OpenAITextEmbedding createOpenAIEmbedding() {
        // Note: In production, you would configure API key from secure storage
        return io.agentscope.core.embedding.openai.OpenAITextEmbedding.builder()
                .apiKey(ragConfig.getEmbedding().getApiKey())
                .modelName(ragConfig.getEmbedding().getModelName())
                .dimensions(ragConfig.getEmbedding().getDimensions())
                .build();
    }

    /**
     * Creates a DashScope text embedding model.
     *
     * @return configured DashScopeTextEmbedding (placeholder for demonstration)
     */
    private io.agentscope.core.embedding.dashscope.DashScopeTextEmbedding createDashScopeEmbedding() {
        return io.agentscope.core.embedding.dashscope.DashScopeTextEmbedding.builder()
                .apiKey(ragConfig.getEmbedding().getApiKey())
                .modelName(ragConfig.getEmbedding().getModelName())
                .dimensions(ragConfig.getEmbedding().getDimensions())
                .build();
    }

    /**
     * Generate embedding vector for a single text.
     *
     * @param text the text to embed
     * @return Mono that emits the embedding vector as double array
     */
    public java.util.concurrent.CompletableFuture<double[]> embedText(String text) {
        log.debug("Generating embedding for text: {} chars", text.length());
        return embeddingModel.embed(
                io.agentscope.core.message.TextBlock.builder().text(text).build()
        ).toFuture();
    }

    /**
     * Generate embedding vectors for multiple texts.
     *
     * @param texts the list of texts to embed
     * @return list of embedding vectors
     */
    public java.util.concurrent.CompletableFuture<List<double[]>> embedTexts(List<String> texts) {
        log.debug("Generating embeddings for {} texts", texts.size());
        List<io.agentscope.core.message.ContentBlock> blocks = texts.stream()
                .map(t -> io.agentscope.core.message.TextBlock.builder().text(t).build())
                .toList();

        return reactor.core.publisher.Flux.fromIterable(blocks)
                .flatMap(embeddingModel::embed)
                .collectList()
                .toFuture();
    }

    /**
     * Get the embedding model instance.
     *
     * @return the EmbeddingModel
     */
    public EmbeddingModel getEmbeddingModel() {
        return embeddingModel;
    }

    /**
     * Get the embedding dimensions.
     *
     * @return the number of dimensions
     */
    public int getDimensions() {
        return embeddingModel.getDimensions();
    }

    /**
     * Get the model name.
     *
     * @return the model name
     */
    public String getModelName() {
        return embeddingModel.getModelName();
    }
}