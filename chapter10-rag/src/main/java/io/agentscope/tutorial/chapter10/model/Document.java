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
package io.agentscope.tutorial.chapter10.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Document entity for storing document metadata in H2 database.
 *
 * <p>该类 represents a document in the RAG system, including its metadata
 * and current processing status.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Document {

    /**
     * Document unique identifier (UUID).
     */
    private String id;

    /**
     * Original file name.
     */
    private String fileName;

    /**
     * File type (e.g., txt, pdf, docx).
     */
    private String fileType;

    /**
     * File size in bytes.
     */
    private Long fileSize;

    /**
     * Document title.
     */
    private String title;

    /**
     * Document content summary.
     */
    private String summary;

    /**
     * Number of chunks after processing.
     */
    private Integer chunkCount;

    /**
     * Document status: PENDING, PROCESSING, COMPLETED, FAILED.
     */
    private String status;

    /**
     * Additional metadata as key-value pairs.
     */
    private Map<String, String> metadata;

    /**
     * Error message if processing failed.
     */
    private String errorMessage;

    /**
     * Creation timestamp.
     */
    private LocalDateTime createdAt;

    /**
     * Last update timestamp.
     */
    private LocalDateTime updatedAt;
}