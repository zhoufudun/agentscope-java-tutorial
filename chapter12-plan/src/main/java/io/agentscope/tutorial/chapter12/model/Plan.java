package io.agentscope.tutorial.chapter12.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 计划领域对象（教程自有模型）.
 *
 * <p>【2.x 迁移说明】原类型 {@code io.agentscope.core.plan.model.Plan} 在 2.x 已被移除，
 * 且没有等价的结构化 Plan/SubTask 数据模型。2.x 中 Agent 的“计划能力”由 harness 的
 * 计划模式（Plan Mode）提供，计划以 workspace 下 {@code plans/PLAN.md} 文件呈现
 * （参见 {@code io.agentscope.harness.agent.workspace.plan.PlanModeManager} 与
 * {@code io.agentscope.core.state.PlanModeContextState}）。
 *
 * <p>本类作为“教程自有领域模型”保留，服务于本章的结构化计划 CRUD 演示与 H2 持久化，
 * 与 Agent 的 harness Plan Mode 是两条独立的能力线：前者是教程自建的可视化任务分解模型，
 * 后者是 2.x 官方的“先规划再执行”运行时机制。
 */
public class Plan {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private String id;
    private String name;
    private String description;
    private String expectedOutcome;
    private String outcome;
    private PlanState state = PlanState.TODO;
    private List<SubTask> subtasks = new ArrayList<>();
    private String createdAt;
    private String finishedAt;

    public Plan() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now().format(FORMATTER);
    }

    public Plan(String name, String description, String expectedOutcome) {
        this();
        this.name = name;
        this.description = description;
        this.expectedOutcome = expectedOutcome;
    }

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

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public PlanState getState() {
        return state;
    }

    public void setState(PlanState state) {
        this.state = state != null ? state : PlanState.TODO;
    }

    public List<SubTask> getSubtasks() {
        return subtasks;
    }

    public void setSubtasks(List<SubTask> subtasks) {
        this.subtasks = subtasks != null ? subtasks : new ArrayList<>();
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
