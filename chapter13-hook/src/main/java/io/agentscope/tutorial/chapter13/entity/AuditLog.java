/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.agentscope.tutorial.chapter13.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审计日志实体.
 *
 * <p>记录 Agent 工具调用的详细信息，用于审计和追踪
 *
 * @author AgentScope Tutorial
 */
@Data
@Entity
@Table(name = "audit_log", indexes = {
    @Index(name = "idx_audit_log_agent", columnList = "agent_name"),
    @Index(name = "idx_audit_log_tool", columnList = "tool_name"),
    @Index(name = "idx_audit_log_time", columnList = "start_time"),
    @Index(name = "idx_audit_log_sensitive", columnList = "sensitive")
})
public class AuditLog {

    /**
     * 主键 ID.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Agent 名称.
     */
    @Column(name = "agent_name", nullable = false)
    private String agentName;

    /**
     * 工具名称.
     */
    @Column(name = "tool_name", nullable = false)
    private String toolName;

    /**
     * 输入内容（JSON 格式）.
     */
    @Column(name = "input_content", columnDefinition = "TEXT")
    private String inputContent;

    /**
     * 输出内容（JSON 格式）.
     */
    @Column(name = "output_content", columnDefinition = "TEXT")
    private String outputContent;

    /**
     * 开始时间.
     */
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    /**
     * 执行耗时（毫秒）.
     */
    @Column(name = "duration_ms", nullable = false)
    private Long durationMs;

    /**
     * 是否为敏感工具.
     */
    @Column(name = "sensitive")
    private Boolean sensitive = false;

    /**
     * 是否被阻止.
     */
    @Column(name = "blocked")
    private Boolean blocked = false;

    /**
     * 追踪 ID（用于关联多个调用）.
     */
    @Column(name = "trace_id")
    private String traceId;

    /**
     * 创建时间.
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * 持久化前设置创建时间.
     */
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}