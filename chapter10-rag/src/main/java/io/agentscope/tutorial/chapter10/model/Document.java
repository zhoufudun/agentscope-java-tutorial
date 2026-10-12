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
 * 文档实体类，用于在 H2 数据库中存储文档元数据
 *
 * <p>本类表示 RAG 系统中的一个文档，包括其元数据和当前处理状态。
 *
 * <h2>文档生命周期</h2>
 * <pre>
 * 上传 -> PENDING -> PROCESSING -> COMPLETED
 *                       ↓
 *                    FAILED
 * </pre>
 *
 * <h2>AgentScope 学习要点</h2>
 * <ul>
 *   <li>此类与 AgentScope 的 Document 类不同，是应用层的元数据模型</li>
 *   <li>AgentScope 的 Document 用于向量存储，包含嵌入向量</li>
 *   <li>本类用于追踪文档的处理状态和管理信息</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Document {

    /**
     * 文档唯一标识符（UUID 格式）
     */
    private String id;

    /**
     * 原始文件名
     */
    private String fileName;

    /**
     * 文件类型（如 txt、pdf、docx）
     */
    private String fileType;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 文档标题
     */
    private String title;

    /**
     * 文档内容摘要
     */
    private String summary;

    /**
     * 处理后的块数量
     */
    private Integer chunkCount;

    /**
     * 文档状态：PENDING、PROCESSING、COMPLETED、FAILED
     *
     * @see DocumentStatus
     */
    private String status;

    /**
     * 附加元数据（键值对）
     */
    private Map<String, String> metadata;

    /**
     * 处理失败时的错误信息
     */
    private String errorMessage;

    /**
     * 创建时间戳
     */
    private LocalDateTime createdAt;

    /**
     * 最后更新时间戳
     */
    private LocalDateTime updatedAt;
}