package io.agentscope.tutorial.chapter12.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 子任务记录实体 - 对应数据库中的 subtask_records 表.
 *
 * <p>用于存储每个计划的子任务详情，支持更细粒度的查询。
 *
 * <p>数据库表结构：
 * <ul>
 *   <li>id - 自增主键</li>
 *   <li>plan_id - 关联的计划 ID</li>
 *   <li>task_index - 子任务在计划中的索引</li>
 *   <li>name - 子任务名称</li>
 *   <li>description - 子任务描述</li>
 *   <li>expected_outcome - 预期结果</li>
 *   <li>actual_outcome - 实际完成结果</li>
 *   <li>state - 子任务状态 (TODO/IN_PROGRESS/DONE/ABANDONED)</li>
 *   <li>created_at - 创建时间</li>
 *   <li>finished_at - 完成时间</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 */
@Entity
@Table(name = "subtask_records")
public class SubTaskRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plan_id", nullable = false, length = 36)
    private String planId;

    @Column(name = "task_index", nullable = false)
    private Integer taskIndex;

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

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    /** 默认构造函数 */
    public SubTaskRecord() {
        this.createdAt = LocalDateTime.now();
    }

    /** 完整构造函数 */
    public SubTaskRecord(String planId, Integer taskIndex, String name, String description, String expectedOutcome) {
        this();
        this.planId = planId;
        this.taskIndex = taskIndex;
        this.name = name;
        this.description = description;
        this.expectedOutcome = expectedOutcome;
    }

    // ==================== Getters and Setters ====================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPlanId() {
        return planId;
    }

    public void setPlanId(String planId) {
        this.planId = planId;
    }

    public Integer getTaskIndex() {
        return taskIndex;
    }

    public void setTaskIndex(Integer taskIndex) {
        this.taskIndex = taskIndex;
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

    @Override
    public String toString() {
        return "SubTaskRecord{" +
                "id=" + id +
                ", planId='" + planId + '\'' +
                ", taskIndex=" + taskIndex +
                ", name='" + name + '\'' +
                ", state='" + state + '\'' +
                '}';
    }
}