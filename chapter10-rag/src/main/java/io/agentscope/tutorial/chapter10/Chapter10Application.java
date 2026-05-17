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
 * 主应用类 for the RAG tutorial chapter.
 *
 * <p>This application demonstrates a complete RAG (Retrieval-Augmented Generation) system
 * using AgentScope Java, including:
 * <ul>
 *   <li>Document upload and chunking</li>
 *   <li>Text embedding with Ollama/OpenAI/DashScope</li>
 *   <li>Vector storage with Qdrant</li>
 *   <li>Semantic search and retrieval</li>
 *   <li>Hybrid search (vector + keyword)</li>
 *   <li>RAG query with context augmentation</li>
 * </ul>
 *
 * <p>To run this application:
 * <ol>
 *   <li>Start Qdrant: docker-compose -f docker-compose-qdrant.yml up -d</li>
 *   <li>Start Ollama (optional, for local embeddings): ollama serve</li>
 *   <li>Run: mvn spring-boot:run</li>
 *   <li>Access API: http://localhost:8080</li>
 * </ol>
 *
 * <p>API endpoints:
 * <ul>
 *   <li>POST /api/documents - Upload document</li>
 *   <li>POST /api/documents/text - Upload text content</li>
 *   <li>POST /api/documents/chunk - Chunk text</li>
 *   <li>POST /api/chat - RAG query</li>
 *   <li>GET /api/chat/retrieve - Pure retrieval</li>
 *   <li>POST /api/chat/hybrid - Hybrid search</li>
 *   <li>GET /api/chat/stats - System statistics</li>
 * </ul>
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class Chapter10Application {

    public static void main(String[] args) {
        SpringApplication.run(Chapter10Application.class, args);
    }
}