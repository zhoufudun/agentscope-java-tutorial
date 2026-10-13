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

/**
 * 文档处理状态枚举
 *
 * <p>表示 RAG 系统中文档的生命周期状态：
 * <ul>
 *   <li><b>PENDING</b> - 文档已上传，等待处理</li>
 *   <li><b>PROCESSING</b> - 文档正在被分块和嵌入</li>
 *   <li><b>COMPLETED</b> - 文档已成功处理并存储</li>
 *   <li><b>FAILED</b> - 处理失败，查看 errorMessage 获取详情</li>
 * </ul>
 *
 * <h2>状态转换</h2>
 * <pre>
 * PENDING -> PROCESSING -> COMPLETED
 *               ↓
 *            FAILED
 * </pre>
 *
 * @author AgentScope Tutorial
 */
public enum DocumentStatus {

    /**
     * 文档已上传，等待处理
     */
    PENDING,

    /**
     * 文档正在被分块和嵌入
     */
    PROCESSING,

    /**
     * 文档已成功处理并存储到向量数据库
     */
    COMPLETED,

    /**
     * 处理失败，查看 errorMessage 获取详细信息
     */
    FAILED
}