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
package io.agentscope.tutorial.chapter10.controller;

import io.agentscope.core.rag.model.Document;
import io.agentscope.tutorial.chapter10.service.DocumentService;
import io.agentscope.tutorial.chapter10.service.VectorStoreService;
import lombok.RequiredArgs构造函数;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * REST controller for document management operations.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>POST /api/documents - Upload and process a document</li>
 *   <li>GET /api/documents - List all documents</li>
 *   <li>GET /api/documents/{id} - Get document details</li>
 *   <li>DELETE /api/documents/{id} - Delete a document</li>
 *   <li>POST /api/documents/chunk - Chunk text content</li>
 *   <li>GET /api/documents/stats - Get document statistics</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * // 上传文档
 * curl -X POST -F "file=@document.txt" -F "title=My Document" http://localhost:8080/api/documents
 *
 * // Chunk text
 * curl -X POST -H "Content-Type: application/json" -d '{"content":"Long text..."}' http://localhost:8080/api/documents/chunk
 * }</pre>
 */
@Slf4j
@RestController
@RequestMapping("/api/documents")
@RequiredArgs构造函数
public class DocumentController {

    private final DocumentService documentService;
    private final VectorStoreService vectorStoreService;

    /**
     * Upload and process a document.
     *
     * @param file the file to upload
     * @param title the document title (optional, uses filename if not provided)
     * @param metadata additional metadata as key-value pairs
     * @return processing result with chunk IDs
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "metadata", required = false) String metadata) {

        log.info("Received document upload: fileName={}, size={}", file.getOriginalFilename(), file.getSize());

        try {
            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "File is empty"));
            }

            // Use filename as title if not provided
            String documentTitle = title != null ? title : file.getOriginalFilename();

            // Read file content (for text files)
            String content = new String(file.getBytes());
            if (!documentService.validateContent(content)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid or empty content"));
            }

            // Parse metadata if provided
            Map<String, String> metadataMap = parseMetadata(metadata);
            metadataMap.put("fileName", file.getOriginalFilename());
            metadataMap.put("fileSize", String.valueOf(file.getSize()));
            metadataMap.put("fileType", getFileExtension(file.getOriginalFilename()));

            // Process document
            CompletableFuture<List<DocumentService.ChunkInfo>> future =
                    documentService.processDocument(documentTitle, content, metadataMap);

            List<DocumentService.ChunkInfo> chunks = future.join();

            // Create documents with embeddings and add to vector store
            documentService.createChunksWithEmbeddings(chunks)
                    .thenAccept(docs -> vectorStoreService.addDocuments(docs).join())
                    .join();

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("documentId", chunks.isEmpty() ? null : chunks.get(0).docId());
            response.put("fileName", file.getOriginalFilename());
            response.put("title", documentTitle);
            response.put("chunkCount", chunks.size());
            response.put("message", String.format("Document processed into %d chunks", chunks.size()));

            log.info("Document processed successfully: {} chunks", chunks.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to process document", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to process document: " + e.getMessage()));
        }
    }

    /**
     * Upload text content directly (no file).
     *
     * @param request the upload request
     * @return processing result
     */
    @PostMapping("/text")
    public ResponseEntity<Map<String, Object>> uploadText(@RequestBody TextUploadRequest request) {
        log.info("Received text upload: title={}, contentLength={}",
                request.title(), request.content().length());

        try {
            if (!documentService.validateContent(request.content())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid or empty content"));
            }

            Map<String, String> metadataMap = new HashMap<>();
            if (request.metadata() != null) {
                metadataMap.putAll(request.metadata());
            }

            CompletableFuture<List<DocumentService.ChunkInfo>> future =
                    documentService.processDocument(request.title(), request.content(), metadataMap);

            List<DocumentService.ChunkInfo> chunks = future.join();

            // Create documents with embeddings and add to vector store
            documentService.createChunksWithEmbeddings(chunks)
                    .thenAccept(docs -> vectorStoreService.addDocuments(docs).join())
                    .join();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("documentId", chunks.isEmpty() ? null : chunks.get(0).docId());
            response.put("chunkCount", chunks.size());
            response.put("message", String.format("Text processed into %d chunks", chunks.size()));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to process text", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to process text: " + e.getMessage()));
        }
    }

    /**
     * Chunk text content without storing.
     *
     * @param request the chunk request
     * @return list of chunks with metadata
     */
    @PostMapping("/chunk")
    public ResponseEntity<Map<String, Object>> chunkText(@RequestBody ChunkRequest request) {
        log.info("Received chunk request: contentLength={}", request.content().length());

        try {
            if (!documentService.validateContent(request.content())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid or empty content"));
            }

            List<String> chunks = documentService.chunkText(request.content());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("chunkCount", chunks.size());
            response.put("chunks", chunks);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to chunk text", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to chunk text: " + e.getMessage()));
        }
    }

    /**
     * Get document processing statistics.
     *
     * @param content the content to analyze
     * @return statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats(@Request参数 String content) {
        try {
            Map<String, Object> stats = documentService.getDocumentStats(content);
            stats.put("success", true);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Failed to get stats", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get stats: " + e.getMessage()));
        }
    }

    /**
     * Delete a document from the vector store.
     *
     * @param documentId the document ID
     * @return deletion result
     */
    @DeleteMapping("/{documentId}")
    public ResponseEntity<Map<String, Object>> deleteDocument(@PathVariable String documentId) {
        log.info("Deleting document: {}", documentId);

        try {
            vectorStoreService.deleteDocument(documentId).join();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("documentId", documentId);
            response.put("message", "Document deleted successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to delete document", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to delete document: " + e.getMessage()));
        }
    }

    /**
     * Parse metadata string (key1:value1,key2:value2 format).
     */
    private Map<String, String> parseMetadata(String metadataStr) {
        Map<String, String> metadata = new HashMap<>();
        if (metadataStr == null || metadataStr.isBlank()) {
            return metadata;
        }

        String[] pairs = metadataStr.split(",");
        for (String pair : pairs) {
            String[] keyValue = pair.split(":");
            if (keyValue.length == 2) {
                metadata.put(keyValue[0].trim(), keyValue[1].trim());
            }
        }

        return metadata;
    }

    /**
     * Get file extension from filename.
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "unknown";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * Request for text upload.
     *
     * @param title the document title
     * @param content the text content
     * @param metadata optional metadata
     */
    public record TextUploadRequest(
            String title,
            String content,
            Map<String, String> metadata
    ) {}

    /**
     * Request for text chunking.
     *
     * @param content the content to chunk
     */
    public record ChunkRequest(String content) {}
}