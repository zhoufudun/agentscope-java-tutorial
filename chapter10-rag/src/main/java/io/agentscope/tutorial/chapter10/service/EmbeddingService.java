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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 文本嵌入服务，使用各种嵌入模型生成向量
 *
 * <p>本服务提供文本向量化的统一接口，支持多种嵌入模型提供商
 * （Ollama、OpenAI、DashScope）。
 *
 * <h2>支持的嵌入模型</h2>
 * <ul>
 *   <li><b>Ollama</b>：本地运行，免费，适合开发测试</li>
 *   <li><b>OpenAI</b>：高质量，需要 API Key</li>
 *   <li><b>DashScope</b>：阿里云服务，支持中文</li>
 * </ul>
 *
 * <h2>AgentScope 学习要点</h2>
 * <ul>
 *   <li><b>EmbeddingModel</b>：嵌入模型的抽象接口</li>
 *   <li><b>OllamaTextEmbedding</b>：Ollama 嵌入模型实现</li>
 *   <li><b>OpenAITextEmbedding</b>：OpenAI 嵌入模型实现</li>
 *   <li><b>DashScopeTextEmbedding</b>：DashScope 嵌入模型实现</li>
 *   <li><b>TextBlock</b>：文本内容块，是嵌入的输入格式</li>
 * </ul>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 为单个文本生成嵌入向量
 * double[] embedding = embeddingService.embedText("你好，世界！");
 *
 * // 为多个文本生成嵌入向量
 * List<double[]> embeddings = embeddingService.embedTexts(List.of("文本1", "文本2"));
 * }</pre>
 *
 * @author AgentScope Tutorial
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final RAGConfig ragConfig;

    /** 嵌入模型实例 */
    private EmbeddingModel embeddingModel;

    /**
     * Bean 构造后初始化嵌入模型
     *
     * <p>根据配置的模型类型创建相应的嵌入模型实例。
     */
    @PostConstruct
    public void init() {
        String modelType = ragConfig.getEmbedding().getModelType();
        log.info("初始化嵌入模型：type={}，model={}",
                modelType, ragConfig.getEmbedding().getModelName());

        this.embeddingModel = createEmbeddingModel(modelType);
        log.info("嵌入模型已初始化：dimensions={}", embeddingModel.getDimensions());
    }

    /**
     * 根据指定类型创建嵌入模型
     *
     * @param modelType 模型类型（OLLAMA、OPENAI、DASHSCOPE）
     * @return 配置好的 EmbeddingModel
     * @throws IllegalArgumentException 如果模型类型不支持
     */
    private EmbeddingModel createEmbeddingModel(String modelType) {
        return switch (modelType.toUpperCase()) {
            case "OLLAMA" -> createOllamaEmbedding();
            case "OPENAI" -> createOpenAIEmbedding();
            case "DASHSCOPE" -> createDashScopeEmbedding();
            default -> throw new IllegalArgumentException("不支持的嵌入模型类型：" + modelType);
        };
    }

    /**
     * 创建 Ollama 文本嵌入模型
     *
     * <p>Ollama 是一个本地运行的 LLM 服务，支持多种开源模型。
     * 适合开发和测试环境，无需 API Key。
     *
     * @return 配置好的 OllamaTextEmbedding
     */
    private OllamaTextEmbedding createOllamaEmbedding() {
        return OllamaTextEmbedding.builder()
                .baseUrl(ragConfig.getEmbedding().getBaseUrl())   // Ollama 服务地址
                .modelName(ragConfig.getEmbedding().getModelName()) // 模型名称
                .dimensions(ragConfig.getEmbedding().getDimensions()) // 向量维度
                .build();
    }

    /**
     * 创建 OpenAI 文本嵌入模型
     *
     * <p>OpenAI 提供高质量的嵌入模型，但需要 API Key。
     * 生产环境中应从安全存储获取 API Key。
     *
     * @return 配置好的 OpenAITextEmbedding
     */
    private io.agentscope.core.embedding.openai.OpenAITextEmbedding createOpenAIEmbedding() {
        // 注意：生产环境应从安全存储获取 API Key
        return io.agentscope.core.embedding.openai.OpenAITextEmbedding.builder()
                .apiKey(ragConfig.getEmbedding().getApiKey())
                .modelName(ragConfig.getEmbedding().getModelName())
                .dimensions(ragConfig.getEmbedding().getDimensions())
                .build();
    }

    /**
     * 创建 DashScope 文本嵌入模型
     *
     * <p>DashScope 是阿里云提供的 AI 服务，对中文支持较好。
     *
     * @return 配置好的 DashScopeTextEmbedding
     */
    private io.agentscope.core.embedding.dashscope.DashScopeTextEmbedding createDashScopeEmbedding() {
        return io.agentscope.core.embedding.dashscope.DashScopeTextEmbedding.builder()
                .apiKey(ragConfig.getEmbedding().getApiKey())
                .modelName(ragConfig.getEmbedding().getModelName())
                .dimensions(ragConfig.getEmbedding().getDimensions())
                .build();
    }

    /**
     * 为单个文本生成嵌入向量
     *
     * <p>将文本转换为 TextBlock 后调用嵌入模型。
     *
     * @param text 要嵌入的文本
     * @return 返回嵌入向量（double 数组）的 CompletableFuture
     */
    public java.util.concurrent.CompletableFuture<double[]> embedText(String text) {
        log.debug("为文本生成嵌入向量：{} 字符", text.length());
        return embeddingModel.embed(
                io.agentscope.core.message.TextBlock.builder().text(text).build()
        ).toFuture();
    }

    /**
     * 为多个文本生成嵌入向量
     *
     * <p>使用 Reactor 的 Flux 并行处理多个文本。
     *
     * @param texts 要嵌入的文本列表
     * @return 嵌入向量列表
     */
    public java.util.concurrent.CompletableFuture<List<double[]>> embedTexts(List<String> texts) {
        log.debug("为 {} 个文本生成嵌入向量", texts.size());

        // 将文本转换为 ContentBlock 列表
        List<io.agentscope.core.message.ContentBlock> blocks = new java.util.ArrayList<>();
        for (String t : texts) {
            blocks.add(io.agentscope.core.message.TextBlock.builder().text(t).build());
        }

        // 使用 Flux 并行处理
        return reactor.core.publisher.Flux.fromIterable(blocks)
                .flatMap(embeddingModel::embed)
                .collectList()
                .toFuture();
    }

    /**
     * 获取嵌入模型实例
     *
     * @return EmbeddingModel 实例
     */
    public EmbeddingModel getEmbeddingModel() {
        return embeddingModel;
    }

    /**
     * 获取嵌入向量维度
     *
     * @return 维度数
     */
    public int getDimensions() {
        return embeddingModel.getDimensions();
    }

    /**
     * 获取模型名称
     *
     * @return 模型名称
     */
    public String getModelName() {
        return embeddingModel.getModelName();
    }
}