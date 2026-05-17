package io.agentscope.tutorial.chapter12.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 计划记录实体 - 对应数据库中的 plan_records 表.
 *
 * <p>用于将 Plan 对象持久化到 H2 数据库中，支持历史计划的查询和恢复。
 *
 * <p>数据库表结构：
 * <ul>
 *   <li>id - UUID 主键</li>
 *   <li>name - 计划名称</li>
 *   <li>description - 计划描述</li>
 *   <li>expected_outcome - 预期结果</li>
 *   <li>actual_outcome - 实际完成结果</li>
 *   <li>state - 计划状态 (TODO/IN_PROGRESS/DONE/ABANDONED)</li>
 *   <li>subtasks_json - 子任务列表的 JSON 序列化</li>
 *   <li>created_at - 创建时间</li>
 *   <li>finished_at - 完成时间</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 */
@Entity
@Table(name = "plan_records")
public class PlanRecord {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "expected_outcome", length = 1000)
    private String expectedOutcome;

    @Column(name = "actual_outcome", length = 2000)
    private String actualOutcome;

    @Column(name = "state", length = 20, nullable = false)
    private String state = "TODO";

    @Column(name = "subtasks_json", columnDefinition = "TEXT")
    private String subtasksJson;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** 默认构造函数 */
    public PlanRecord() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /** 带名称的构造函数 */
    public PlanRecord(String id, String name) {
        this();
        this.id = id;
        this.name = name;
    }

    // ==================== Getters and Setters ====================

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getExpectedOutcome() {
        return expectedOutcome;
    }

    public void setExpectedOutcome(String expectedOutcome) {
        this.expectedOutcome = expectedOutcome;
    }

    public String getActualOutcome() {
        return actualOutcome;
    }

    public void setActualOutcome(String actualOutcome) {
        this.actualOutcome = actualOutcome;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getSubtasksJson() {
        return subtasksJson;
    }

    public void setSubtasksJson(String subtasksJson) {
        this.subtasksJson = subtasksJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "PlanRecord{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", state='" + state + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}