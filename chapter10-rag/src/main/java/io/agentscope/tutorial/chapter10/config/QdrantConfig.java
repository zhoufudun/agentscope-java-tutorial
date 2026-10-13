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
 * Qdrant 向量数据库配置类
 *
 * <p>配置与 Qdrant 向量数据库的连接，用于存储和检索文档嵌入向量。
 *
 * <h2>Qdrant 简介</h2>
 * <p>Qdrant 是一个高性能的向量相似度搜索引擎，专为 AI 应用设计。
 * 它支持：
 * <ul>
 *   <li>高维向量的存储和检索</li>
 *   <li>基于距离度量的相似度搜索</li>
 *   <li>元数据过滤</li>
 *   <li>分布式部署</li>
 * </ul>
 *
 * <h2>AgentScope 学习要点</h2>
 * <ul>
 *   <li>使用 @ConfigurationProperties 绑定配置</li>
 *   <li>向量维度必须与嵌入模型输出匹配</li>
 *   <li>支持多种连接模式：HTTP、gRPC、内存</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "qdrant")
public class QdrantConfig {

    /**
     * Qdrant 服务器位置（URL 或 host:port）
     *
     * <p>支持的格式：
     * <ul>
     *   <li>"http://localhost:6333" - HTTP 连接（默认 gRPC 端口为 6334）</li>
     *   <li>"localhost:6334" - 直接 gRPC 连接</li>
     *   <li>"file:///path/to/qdrant" - 本地存储</li>
     *   <li>":memory:" - 内存模式（仅用于测试）</li>
     * </ul>
     */
    private String location = "http://localhost:6333";

    /**
     * 存储文档嵌入的集合名称
     *
     * <p>集合是 Qdrant 中组织向量数据的基本单位，
     * 类似于关系数据库中的表。
     */
    private String collectionName = "tutorial_rag";

    /**
     * Qdrant 认证 API Key（可选）
     *
     * <p>如果 Qdrant 启用了认证，需要配置此项。
     */
    private String apiKey;

    /**
     * 是否为 gRPC 连接使用 TLS
     *
     * <p>生产环境建议启用 TLS 加密。
     */
    private boolean useTls = false;

    /**
     * 是否检查客户端-服务器版本兼容性
     *
     * <p>建议在生产环境中启用，确保客户端和服务器版本兼容。
     */
    private boolean checkCompatibility = true;

    /**
     * 向量维度（必须与嵌入模型输出匹配）
     *
     * <p>常见的维度：
     * <ul>
     *   <li>768 - nomic-embed-text, all-mpnet-base-v2</li>
     *   <li>1536 - OpenAI text-embedding-ada-002</li>
     *   <li>3072 - OpenAI text-embedding-3-large</li>
     * </ul>
     */
    private int dimensions = 768;

    /**
     * 连接超时时间（毫秒）
     */
    private int timeout = 30000;

    /**
     * 连接池最大连接数
     */
    private int maxConnections = 10;
}