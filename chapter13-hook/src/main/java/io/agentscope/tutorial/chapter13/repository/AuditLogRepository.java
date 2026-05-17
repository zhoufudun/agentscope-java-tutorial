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
package io.agentscope.tutorial.chapter13.repository;

import io.agentscope.tutorial.chapter13.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审计日志仓库接口.
 *
 * <p>提供审计日志的 CRUD 操作和自定义查询
 *
 * @author AgentScope Tutorial
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * 根据 Agent 名称查询审计记录.
     *
     * @param agentName Agent 名称
     * @return 审计记录列表
     */
    List<AuditLog> findByAgentName(String agentName);

    /**
     * 根据工具名称查询审计记录.
     *
     * @param toolName 工具名称
     * @return 审计记录列表
     */
    List<AuditLog> findByToolName(String toolName);

    /**
     * 根据 Agent 名称分页查询.
     *
     * @param agentName Agent 名称
     * @param pageable 分页参数
     * @return 分页审计记录
     */
    Page<AuditLog> findByAgentName(String agentName, Pageable pageable);

    /**
     * 查询敏感工具的审计记录.
     *
     * @return 敏感工具审计记录列表
     */
    List<AuditLog> findBySensitiveTrue();

    /**
     * 查询被阻止的工具调用记录.
     *
     * @return 被阻止的记录列表
     */
    List<AuditLog> findByBlockedTrue();

    /**
     * 根据时间范围查询.
     *
     * @param start 开始时间
     * @param end 结束时间
     * @return 审计记录列表
     */
    List<AuditLog> findByStartTimeBetween(LocalDateTime start, LocalDateTime end);

    /**
     * 根据追踪 ID 查询.
     *
     * @param traceId 追踪 ID
     * @return 审计记录列表
     */
    List<AuditLog> findByTraceId(String traceId);

    /**
     * 统计指定 Agent 的调用次数.
     *
     * @param agentName Agent 名称
     * @return 调用次数
     */
    long countByAgentName(String agentName);

    /**
     * 统计指定工具的调用次数.
     *
     * @param toolName 工具名称
     * @return 调用次数
     */
    long countByToolName(String toolName);

    /**
     * 统计敏感工具调用次数.
     *
     * @return 敏感工具调用次数
     */
    long countBySensitiveTrue();

    /**
     * 查询最近 N 条审计记录.
     *
     * @param limit 限制数量
     * @return 审计记录列表
     */
    @Query(value = "SELECT * FROM audit_log ORDER BY start_time DESC LIMIT :limit", nativeQuery = true)
    List<AuditLog> findRecentRecords(@Param("limit") int limit);

    /**
     * 统计平均执行耗时.
     *
     * @return 平均耗时（毫秒）
     */
    @Query("SELECT AVG(a.durationMs) FROM AuditLog a")
    Double findAverageDuration();

    /**
     * 根据工具名称分组统计调用次数和平均耗时.
     *
     * @return 统计结果列表
     */
    @Query("SELECT a.toolName, COUNT(a), AVG(a.durationMs) FROM AuditLog a GROUP BY a.toolName")
    List<Object[]> findToolStatistics();

    /**
     * 删除指定时间之前的记录.
     *
     * @param beforeTime 时间节点
     * @return 删除数量
     */
    int deleteByStartTimeBefore(LocalDateTime beforeTime);

    /**
     * 根据 Agent 名称和工具名称查询.
     *
     * @param agentName Agent 名称
     * @param toolName 工具名称
     * @return 审计记录列表
     */
    List<AuditLog> findByAgentNameAndToolName(String agentName, String toolName);
}