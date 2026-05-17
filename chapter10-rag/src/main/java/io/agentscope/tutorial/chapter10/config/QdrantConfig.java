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
 * Qdrant vector database configuration.
 *
 * <p>Configures connection to Qdrant vector database for storing
 * and retrieving document embeddings.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "qdrant")
public class QdrantConfig {

    /**
     * Qdrant server location (URL or host:port).
     * Examples:
     * - "http://localhost:6333" (HTTP with default gRPC port 6334)
     * - "localhost:6334" (direct gRPC)
     * - "file:///path/to/qdrant" (local storage)
     * - ":memory:" (in-memory, for testing only)
     */
    private String location = "http://localhost:6333";

    /**
     * Collection name for storing document embeddings.
     */
    private String collectionName = "tutorial_rag";

    /**
     * API key for Qdrant authentication (optional).
     */
    private String apiKey;

    /**
     * 是否 use TLS for gRPC connections.
     */
    private boolean useTls = false;

    /**
     * 是否 check client-server version compatibility.
     */
    private boolean checkCompatibility = true;

    /**
     * Vector dimensions (must match embedding model output).
     */
    private int dimensions = 768;

    /**
     * Connection timeout in milliseconds.
     */
    private int timeout = 30000;

    /**
     * Maximum connections in the pool.
     */
    private int maxConnections = 10;
}