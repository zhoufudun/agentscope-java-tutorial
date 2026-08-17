package io.agentscope.tutorial.chapter12.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 子任务领域对象（教程自有模型）.
 *
 * <p>【2.x 迁移说明】原类型 {@code io.agentscope.core.plan.model.SubTask} 在 2.x 已被移除。
 * agentscope 2.x 不再提供结构化的子任务模型，计划以 workspace 下的 {@code plans/PLAN.md}
 * markdown 文件呈现。本类作为教程自有领域模型保留，用于演示任务分解与进度追踪，
 * 并配合 H2 持久化。字段与旧 API 的 getter/setter 保持一致，以最小化改动。
 */
public class SubTask {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private String name;
    private String description;
    private String expectedOutcome;
    private String outcome;
    private SubTaskState state = SubTaskState.TODO;
    private String createdAt;
    private String finishedAt;

    public SubTask() {
        this.createdAt = LocalDateTime.now().format(FORMATTER);
    }

    public SubTask(String name, String description, String expectedOutcome) {
        this();
        this.name = name;
        this.description = description;
        this.expectedOutcome = expectedOutcome;
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

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public SubTaskState getState() {
        return state;
    }

    public void setState(SubTaskState state) {
        this.state = state != null ? state : SubTaskState.TODO;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(String finishedAt) {
        this.finishedAt = finishedAt;
    }
}
