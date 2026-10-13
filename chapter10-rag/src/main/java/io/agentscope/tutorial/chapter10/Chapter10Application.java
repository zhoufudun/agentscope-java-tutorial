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
package io.agentscope.tutorial.chapter10;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * 第十章：RAG（检索增强生成）- Spring Boot 启动类
 *
 * <p>本应用演示使用 AgentScope Java 构建完整的 RAG 系统，包括：
 * <ul>
 *   <li><b>文档上传和分块</b>：将文档切分为适合检索的文本块</li>
 *   <li><b>文本嵌入</b>：使用 Ollama/OpenAI/DashScope 生成向量</li>
 *   <li><b>向量存储</b>：使用 Qdrant 存储和检索文档嵌入</li>
 *   <li><b>语义搜索</b>：基于向量相似度的文档检索</li>
 *   <li><b>混合搜索</b>：结合向量搜索和关键词搜索</li>
 *   <li><b>RAG 查询</b>：结合检索结果生成回答</li>
 * </ul>
 *
 * <h2>AgentScope 学习要点</h2>
 * <ul>
 *   <li><b>EmbeddingModel</b>：文本向量化的核心接口</li>
 *   <li><b>VDBStoreBase</b>：向量数据库操作的抽象基类</li>
 *   <li><b>QdrantStore</b>：Qdrant 向量数据库的具体实现</li>
 *   <li><b>TextChunker</b>：文本分块工具</li>
 *   <li><b>Document</b>：RAG 中文档的标准表示</li>
 * </ul>
 *
 * <h2>运行方式</h2>
 * <ol>
 *   <li>启动 Qdrant：docker-compose -f docker-compose-qdrant.yml up -d</li>
 *   <li>启动 Ollama（可选，用于本地嵌入）：ollama serve</li>
 *   <li>运行：mvn spring-boot:run</li>
 *   <li>访问 API：http://localhost:8080</li>
 * </ol>
 *
 * <h2>API 端点</h2>
 * <ul>
 *   <li>POST /api/documents - 上传文档</li>
 *   <li>POST /api/documents/text - 上传文本内容</li>
 *   <li>POST /api/documents/chunk - 文本分块</li>
 *   <li>POST /api/chat - RAG 查询</li>
 *   <li>GET /api/chat/retrieve - 纯检索</li>
 *   <li>POST /api/chat/hybrid - 混合搜索</li>
 *   <li>GET /api/chat/stats - 系统统计</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class Chapter10Application {

    public static void main(String[] args) {
        SpringApplication.run(Chapter10Application.class, args);
    }
}