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
package io.agentscope.tutorial.chapter13.controller;

import io.agentscope.tutorial.chapter13.entity.AuditLog;
import io.agentscope.tutorial.chapter13.hook.AuditHook;
import io.agentscope.tutorial.chapter13.repository.AuditLogRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 审计控制器 - 提供审计记录查询接口.
 *
 * <p>提供以下 API 端点：
 * <ul>
 *   <li>GET /api/audit/records - 获取审计记录</li>
 *   <li>GET /api/audit/records/database - 获取数据库中的审计记录</li>
 *   <li>GET /api/audit/records/tool - 按工具名称查询</li>
 *   <li>GET /api/audit/records/sensitive - 获取敏感工具记录</li>
 *   <li>GET /api/audit/statistics - 获取统计信息</li>
 *   <li>DELETE /api/audit/records - 清空审计记录</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 */
@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditHook auditHook;

    private final AuditLogRepository auditLogRepository;

    public AuditController(AuditHook auditHook, AuditLogRepository auditLogRepository) {
        this.auditHook = auditHook;
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * 获取内存中的审计记录.
     */
    @GetMapping("/records")
    public ResponseEntity<Map<String, Object>> getRecords(
            @RequestParam(required = false) String tool) {

        List<AuditHook.ToolAuditRecord> records;
        if (tool != null && !tool.isBlank()) {
            records = auditHook.getAuditRecordsByTool(tool);
        } else {
            records = auditHook.getAuditRecords();
        }

        return ResponseEntity.ok(Map.of(
                "count", records.size(),
                "source", "memory",
                "records", records.stream()
                        .map(r -> Map.of(
                                "agent", r.agentName(),
                                "tool", r.toolName(),
                                "input", truncate(r.input(), 100),
                                "output", truncate(r.output(), 100),
                                "durationMs", r.durationMs(),
                                "sensitive", r.sensitive(),
                                "blocked", r.blocked(),
                                "startTime", r.getStartTimeFormatted()
                        ))
                        .toList()
        ));
    }

    /**
     * 获取数据库中的审计记录.
     */
    @GetMapping("/records/database")
    public ResponseEntity<Map<String, Object>> getDatabaseRecords(
            @RequestParam(required = false) String agent,
            @RequestParam(required = false) String tool,
            @RequestParam(defaultValue = "100") int limit) {

        List<AuditLog> records;
        if (agent != null && !agent.isBlank()) {
            records = auditLogRepository.findByAgentName(agent);
        } else if (tool != null && !tool.isBlank()) {
            records = auditLogRepository.findByToolName(tool);
        } else {
            records = auditLogRepository.findRecentRecords(limit);
        }

        return ResponseEntity.ok(Map.of(
                "count", records.size(),
                "source", "database",
                "records", records.stream()
                        .map(r -> Map.of(
                                "id", r.getId(),
                                "agent", r.getAgentName(),
                                "tool", r.getToolName(),
                                "input", truncate(r.getInputContent(), 100),
                                "output", truncate(r.getOutputContent(), 100),
                                "durationMs", r.getDurationMs(),
                                "sensitive", r.getSensitive(),
                                "blocked", r.getBlocked(),
                                "startTime", r.getStartTime()
                        ))
                        .toList()
        ));
    }

    /**
     * 按工具名称查询审计记录.
     */
    @GetMapping("/records/tool")
    public ResponseEntity<Map<String, Object>> getRecordsByTool(
            @RequestParam String tool) {

        List<AuditHook.ToolAuditRecord> memoryRecords = auditHook.getAuditRecordsByTool(tool);
        List<AuditLog> dbRecords = auditLogRepository.findByToolName(tool);

        return ResponseEntity.ok(Map.of(
                "tool", tool,
                "memoryCount", memoryRecords.size(),
                "databaseCount", dbRecords.size(),
                "memoryRecords", memoryRecords.stream()
                        .map(r -> Map.of(
                                "agent", r.agentName(),
                                "durationMs", r.durationMs(),
                                "startTime", r.getStartTimeFormatted()
                        ))
                        .toList(),
                "databaseRecords", dbRecords.stream()
                        .map(r -> Map.of(
                                "id", r.getId(),
                                "agent", r.getAgentName(),
                                "durationMs", r.getDurationMs(),
                                "startTime", r.getStartTime()
                        ))
                        .toList()
        ));
    }

    /**
     * 获取敏感工具的审计记录.
     */
    @GetMapping("/records/sensitive")
    public ResponseEntity<Map<String, Object>> getSensitiveRecords() {
        List<AuditHook.ToolAuditRecord> records = auditHook.getSensitiveToolRecords();
        List<AuditLog> dbRecords = auditLogRepository.findBySensitiveTrue();

        return ResponseEntity.ok(Map.of(
                "count", records.size(),
                "databaseCount", dbRecords.size(),
                "records", records.stream()
                        .map(r -> Map.of(
                                "agent", r.agentName(),
                                "tool", r.toolName(),
                                "input", truncate(r.input(), 100),
                                "durationMs", r.durationMs(),
                                "startTime", r.getStartTimeFormatted()
                        ))
                        .toList()
        ));
    }

    /**
     * 获取审计统计信息.
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        Double avgDuration = auditLogRepository.findAverageDuration();
        List<Object[]> toolStats = auditLogRepository.findToolStatistics();

        return ResponseEntity.ok(Map.of(
                "totalRecords", auditLogRepository.count(),
                "memoryRecords", auditHook.getRecordCount(),
                "averageDurationMs", avgDuration != null ? avgDuration : 0,
                "sensitiveCount", auditLogRepository.countBySensitiveTrue(),
                "blockedCount", auditLogRepository.findByBlockedTrue().size(),
                "toolStatistics", toolStats.stream()
                        .map(stat -> Map.of(
                                "tool", stat[0],
                                "count", stat[1],
                                "avgDurationMs", stat[2]
                        ))
                        .toList()
        ));
    }

    /**
     * 清空审计记录.
     */
    @DeleteMapping("/records")
    public ResponseEntity<Map<String, Object>> clearRecords() {
        auditHook.clearRecords();
        auditHook.clearDatabaseRecords();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "审计记录已清空"
        ));
    }

    /**
     * 截断过长文本.
     */
    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }
}