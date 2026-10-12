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
import lombok.RequiredArgsConstructor;
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
 * 文档管理操作的 REST 控制器
 *
 * <h2>API 端点</h2>
 * <ul>
 *   <li>POST /api/documents - 上传并处理文档</li>
 *   <li>GET /api/documents - 列出所有文档</li>
 *   <li>GET /api/documents/{id} - 获取文档详情</li>
 *   <li>DELETE /api/documents/{id} - 删除文档</li>
 *   <li>POST /api/documents/chunk - 文本分块</li>
 *   <li>GET /api/documents/stats - 获取文档统计</li>
 * </ul>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 上传文档
 * curl -X POST -F "file=@document.txt" -F "title=我的文档" http://localhost:8080/api/documents
 *
 * // 文本分块
 * curl -X POST -H "Content-Type: application/json" -d '{"content":"长文本..."}' http://localhost:8080/api/documents/chunk
 * }</pre>
 *
 * <h2>AgentScope 学习要点</h2>
 * <ul>
 *   <li>文档处理流程：上传 -> 分块 -> 嵌入 -> 存储</li>
 *   <li>ChunkInfo 记录包含内容、ID 和元数据</li>
 *   <li>异步处理使用 CompletableFuture</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 */
@Slf4j
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final VectorStoreService vectorStoreService;

    /**
     * 上传并处理文档
     *
     * <p>处理流程：
     * <ol>
     *   <li>验证上传的文件</li>
     *   <li>读取文件内容</li>
     *   <li>将内容分割成块</li>
     *   <li>为每个块生成嵌入向量</li>
     *   <li>存储到向量数据库</li>
     * </ol>
     *
     * @param file 要上传的文件
     * @param title 文档标题（可选，未提供时使用文件名）
     * @param metadata 作为键值对的附加元数据
     * @return 处理结果和块 ID
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "metadata", required = false) String metadata) {

        log.info("收到文档上传：fileName={}，size={}", file.getOriginalFilename(), file.getSize());

        try {
            // 验证文件
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "文件为空"));
            }

            // 如果未提供，使用文件名作为标题
            String documentTitle = title != null ? title : file.getOriginalFilename();

            // 读取文件内容（针对文本文件）
            String content = new String(file.getBytes());
            if (!documentService.validateContent(content)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "内容无效或为空"));
            }

            // 解析元数据（如果提供）
            Map<String, String> metadataMap = parseMetadata(metadata);
            metadataMap.put("fileName", file.getOriginalFilename());
            metadataMap.put("fileSize", String.valueOf(file.getSize()));
            metadataMap.put("fileType", getFileExtension(file.getOriginalFilename()));

            // 处理文档：分块
            CompletableFuture<List<DocumentService.ChunkInfo>> future =
                    documentService.processDocument(documentTitle, content, metadataMap);

            List<DocumentService.ChunkInfo> chunks = future.join();

            // 创建带嵌入向量的文档并添加到向量存储
            documentService.createChunksWithEmbeddings(chunks)
                    .thenAccept(docs -> vectorStoreService.addDocuments(docs).join())
                    .join();

            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("documentId", chunks.isEmpty() ? null : chunks.get(0).docId());
            response.put("fileName", file.getOriginalFilename());
            response.put("title", documentTitle);
            response.put("chunkCount", chunks.size());
            response.put("message", String.format("文档已处理为 %d 个块", chunks.size()));

            log.info("文档处理成功：{} 个块", chunks.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("处理文档失败", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "处理文档失败：" + e.getMessage()));
        }
    }

    /**
     * 直接上传文本内容（无文件）
     *
     * @param request 上传请求
     * @return 处理结果
     */
    @PostMapping("/text")
    public ResponseEntity<Map<String, Object>> uploadText(@RequestBody TextUploadRequest request) {
        log.info("收到文本上传：title={}，contentLength={}",
                request.title(), request.content().length());

        try {
            if (!documentService.validateContent(request.content())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "内容无效或为空"));
            }

            Map<String, String> metadataMap = new HashMap<>();
            if (request.metadata() != null) {
                metadataMap.putAll(request.metadata());
            }

            CompletableFuture<List<DocumentService.ChunkInfo>> future =
                    documentService.processDocument(request.title(), request.content(), metadataMap);

            List<DocumentService.ChunkInfo> chunks = future.join();

            // 创建带嵌入向量的文档并添加到向量存储
            documentService.createChunksWithEmbeddings(chunks)
                    .thenAccept(docs -> vectorStoreService.addDocuments(docs).join())
                    .join();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("documentId", chunks.isEmpty() ? null : chunks.get(0).docId());
            response.put("chunkCount", chunks.size());
            response.put("message", String.format("文本已处理为 %d 个块", chunks.size()));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("处理文本失败", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "处理文本失败：" + e.getMessage()));
        }
    }

    /**
     * 文本分块（不存储）
     *
     * <p>仅执行分块操作，返回分块结果，不进行嵌入和存储。
     * 适用于预览分块效果。
     *
     * @param request 分块请求
     * @return 带元数据的块列表
     */
    @PostMapping("/chunk")
    public ResponseEntity<Map<String, Object>> chunkText(@RequestBody ChunkRequest request) {
        log.info("收到分块请求：contentLength={}", request.content().length());

        try {
            if (!documentService.validateContent(request.content())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "内容无效或为空"));
            }

            List<String> chunks = documentService.chunkText(request.content());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("chunkCount", chunks.size());
            response.put("chunks", chunks);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("文本分块失败", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "文本分块失败：" + e.getMessage()));
        }
    }

    /**
     * 获取文档处理统计信息
     *
     * @param content 要分析的内容
     * @return 统计信息
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats(@RequestParam String content) {
        try {
            Map<String, Object> stats = documentService.getDocumentStats(content);
            stats.put("success", true);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("获取统计信息失败", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "获取统计信息失败：" + e.getMessage()));
        }
    }

    /**
     * 从向量存储中删除文档
     *
     * @param documentId 文档 ID
     * @return 删除结果
     */
    @DeleteMapping("/{documentId}")
    public ResponseEntity<Map<String, Object>> deleteDocument(@PathVariable String documentId) {
        log.info("删除文档：{}", documentId);

        try {
            vectorStoreService.deleteDocument(documentId).join();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("documentId", documentId);
            response.put("message", "文档删除成功");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("删除文档失败", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "删除文档失败：" + e.getMessage()));
        }
    }

    /**
     * 解析元数据字符串（key1:value1,key2:value2 格式）
     *
     * @param metadataStr 元数据字符串
     * @return 解析后的 Map
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
     * 从文件名获取文件扩展名
     *
     * @param filename 文件名
     * @return 文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "unknown";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * 文本上传请求
     *
     * @param title 文档标题
     * @param content 文本内容
     * @param metadata 可选的元数据
     */
    public record TextUploadRequest(
            String title,
            String content,
            Map<String, String> metadata
    ) {}

    /**
     * 文本分块请求
     *
     * @param content 要分块的内容
     */
    public record ChunkRequest(String content) {}
}