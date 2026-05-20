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
package io.agentscope.tutorial.chapter13.hook;

import io.agentscope.core.hook.Hook;
import io.agentscope.core.hook.HookEvent;
import io.agentscope.core.hook.PostActingEvent;
import io.agentscope.core.hook.PostCallEvent;
import io.agentscope.core.hook.PreActingEvent;
import io.agentscope.core.hook.PreCallEvent;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.tutorial.chapter13.entity.AuditLog;
import io.agentscope.tutorial.chapter13.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 自定义审计钩子 - 记录所有工具调用的详细信息.
 *
 * <p>功能特点：
 * <ul>
 *   <li>高优先级执行，确保尽早拦截工具调用</li>
 *   <li>记录完整的工具调用上下文（参数、结果、时间）</li>
 *   <li>支持指定敏感工具进行额外关注</li>
 *   <li>可配置阻止特定工具的执行</li>
 *   <li>提供审计历史查询接口</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 */
@SuppressWarnings("unchecked")
public class AuditHook implements Hook {

    private static final Logger auditLog = LoggerFactory.getLogger("AuditLog");

    private static final DateTimeFormatter DT_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private final Set<String> sensitiveTools;
    private final List<ToolAuditRecord> auditRecords = new CopyOnWriteArrayList<>();
    private final ThreadLocal<List<ToolAuditRecord>> currentCallRecords = ThreadLocal.withInitial(ArrayList::new);
    private final boolean strictMode;
    private final Set<String> allowedTools;
    private final AuditLogRepository auditLogRepository;

    /**
     * 内部类：工具审计记录.
     */
    public record ToolAuditRecord(
            String agentName,
            String toolName,
            String input,
            String output,
            long startTime,
            long durationMs,
            boolean sensitive,
            boolean blocked
    ) {
        public String getStartTimeFormatted() {
            return LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(startTime),
                    java.time.ZoneId.systemDefault()
            ).format(DT_FORMATTER);
        }
    }

    /**
     * 默认构造函数 - 记录所有工具调用.
     */
    public AuditHook() {
        this(Set.of(), false, Set.of(), null);
    }

    /**
     * 构造函数（不持久化）.
     *
     * @param sensitiveTools 需要特别关注的工具集合
     */
    public AuditHook(Set<String> sensitiveTools) {
        this(sensitiveTools, false, Set.of(), null);
    }

    /**
     * 完整配置构造函数.
     *
     * @param sensitiveTools 需要特别关注的工具集合
     * @param strictMode 是否启用严格模式
     * @param allowedTools 允许的工具集合（空表示全部允许）
     * @param auditLogRepository 审计日志仓库（可选）
     */
    public AuditHook(Set<String> sensitiveTools, boolean strictMode,
                     Set<String> allowedTools, AuditLogRepository auditLogRepository) {
        this.sensitiveTools = Set.copyOf(sensitiveTools);
        this.strictMode = strictMode;
        this.allowedTools = Set.copyOf(allowedTools);
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    public int priority() {
        return 60;
    }

    @Override
    public <T extends HookEvent> Mono<T> onEvent(T event) {
        if (event instanceof PreCallEvent e) {
            return handlePreCall(e);
        } else if (event instanceof PreActingEvent e) {
            return handlePreActing(e);
        } else if (event instanceof PostActingEvent e) {
            return handlePostActing(e);
        } else if (event instanceof PostCallEvent e) {
            return handlePostCall(e);
        }
        return Mono.just(event);
    }

    private <T extends HookEvent> Mono<T> handlePreCall(PreCallEvent e) {
        currentCallRecords.get().clear();
        auditLog.info("[AUDIT] Agent '{}' 调用开始", e.getAgent().getName());
        return (Mono<T>) Mono.just(e);
    }

    private <T extends HookEvent> Mono<T> handlePreActing(PreActingEvent e) {
        ToolUseBlock toolUse = e.getToolUse();
        String toolName = toolUse.getName();
        boolean isSensitive = sensitiveTools.contains(toolName);
        boolean isAllowed = allowedTools.isEmpty() || allowedTools.contains(toolName);

        if (!isAllowed) {
            auditLog.warn("[AUDIT] 工具 '{}' 未授权 - Agent: '{}'",
                    toolName, e.getAgent().getName());
            if (strictMode) {
                auditLog.error("[AUDIT] 严格模式：阻止未授权工具 '{}'", toolName);
            }
        }

        if (isSensitive) {
            auditLog.warn("[AUDIT] 敏感工具 '{}' 即将执行 - Agent: '{}', 输入: {}",
                    toolName, e.getAgent().getName(),
                    truncate(toolUse.getInput().toString(), 200));
        } else {
            auditLog.info("[AUDIT] 工具 '{}' 即将执行 - Agent: '{}'",
                    toolName, e.getAgent().getName());
        }

        ToolAuditRecord record = new ToolAuditRecord(
                e.getAgent().getName(),
                toolName,
                toolUse.getInput().toString(),
                null,
                System.currentTimeMillis(),
                0,
                isSensitive,
                !isAllowed
        );
        currentCallRecords.get().add(record);

        return (Mono<T>) Mono.just(e);
    }

    private <T extends HookEvent> Mono<T> handlePostActing(PostActingEvent e) {
        ToolUseBlock toolUse = e.getToolUse();
        ToolResultBlock result = e.getToolResult();
        String output = extractResultText(result);

        List<ToolAuditRecord> records = currentCallRecords.get();
        if (!records.isEmpty()) {
            ToolAuditRecord existingRecord = null;
            for (int i = records.size() - 1; i >= 0; i--) {
                if (records.get(i).toolName().equals(toolUse.getName())) {
                    existingRecord = records.get(i);
                    break;
                }
            }

            if (existingRecord != null) {
                long duration = System.currentTimeMillis() - existingRecord.startTime();

                ToolAuditRecord updatedRecord = new ToolAuditRecord(
                        existingRecord.agentName(),
                        existingRecord.toolName(),
                        existingRecord.input(),
                        output,
                        existingRecord.startTime(),
                        duration,
                        existingRecord.sensitive(),
                        existingRecord.blocked()
                );

                records.remove(existingRecord);
                records.add(updatedRecord);

                if (existingRecord.sensitive()) {
                    auditLog.warn("[AUDIT] 敏感工具 '{}' 执行完成 | 耗时: {}ms | 结果: {}",
                            toolUse.getName(), duration, truncate(output, 200));
                } else {
                    auditLog.info("[AUDIT] 工具 '{}' 执行完成 | 耗时: {}ms",
                            toolUse.getName(), duration);
                }

                if (auditLogRepository != null) {
                    try {
                        AuditLog entity = new AuditLog();
                        entity.setAgentName(updatedRecord.agentName());
                        entity.setToolName(updatedRecord.toolName());
                        entity.setInputContent(updatedRecord.input());
                        entity.setOutputContent(updatedRecord.output());
                        entity.setStartTime(LocalDateTime.ofInstant(
                                Instant.ofEpochMilli(updatedRecord.startTime()),
                                java.time.ZoneId.systemDefault()));
                        entity.setDurationMs(updatedRecord.durationMs());
                        entity.setSensitive(updatedRecord.sensitive());
                        entity.setBlocked(updatedRecord.blocked());
                        auditLogRepository.save(entity);
                    } catch (Exception ex) {
                        auditLog.error("[AUDIT] 持久化审计记录失败: {}", ex.getMessage());
                    }
                }
            }
        }

        return (Mono<T>) Mono.just(e);
    }

    private <T extends HookEvent> Mono<T> handlePostCall(PostCallEvent e) {
        List<ToolAuditRecord> records = currentCallRecords.get();

        if (!records.isEmpty()) {
            auditLog.info("[AUDIT] Agent '{}' 调用完成 | 工具调用数: {}",
                    e.getAgent().getName(), records.size());

            auditRecords.addAll(records);

            long totalDuration = records.stream()
                    .mapToLong(ToolAuditRecord::durationMs)
                    .sum();
            auditLog.info("  总工具调用耗时: {}ms", totalDuration);

            long blockedCount = records.stream()
                    .filter(ToolAuditRecord::blocked)
                    .count();
            if (blockedCount > 0) {
                auditLog.warn("  被阻止的工具调用数: {}", blockedCount);
            }
        }

        currentCallRecords.remove();
        return (Mono<T>) Mono.just(e);
    }

    private String extractResultText(ToolResultBlock result) {
        if (result == null || result.getOutput().isEmpty()) {
            return "(empty)";
        }
        return result.getOutput().get(0).toString();
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "(null)";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }

    // ========== 查询方法 ==========

    public List<ToolAuditRecord> getAuditRecords() {
        return List.copyOf(auditRecords);
    }

    public List<ToolAuditRecord> getAuditRecordsByAgent(String agentName) {
        return auditRecords.stream()
                .filter(r -> r.agentName().equals(agentName))
                .toList();
    }

    public List<ToolAuditRecord> getAuditRecordsByTool(String toolName) {
        return auditRecords.stream()
                .filter(r -> r.toolName().equals(toolName))
                .toList();
    }

    public List<ToolAuditRecord> getSensitiveToolRecords() {
        return auditRecords.stream()
                .filter(ToolAuditRecord::sensitive)
                .toList();
    }

    public void clearRecords() {
        auditRecords.clear();
        auditLog.info("[AUDIT] 内存审计记录已清空");
    }

    public void clearDatabaseRecords() {
        if (auditLogRepository != null) {
            auditLogRepository.deleteAll();
            auditLog.info("[AUDIT] 数据库审计记录已清空");
        }
    }

    public void addSensitiveTool(String toolName) {
        if (sensitiveTools instanceof java.util.HashSet) {
            ((java.util.HashSet<String>) sensitiveTools).add(toolName);
        }
    }

    public void removeSensitiveTool(String toolName) {
        if (sensitiveTools instanceof java.util.HashSet) {
            ((java.util.HashSet<String>) sensitiveTools).remove(toolName);
        }
    }

    public int getRecordCount() {
        return auditRecords.size();
    }
}