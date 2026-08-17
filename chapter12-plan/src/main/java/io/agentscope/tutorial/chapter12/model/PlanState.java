package io.agentscope.tutorial.chapter12.model;

/**
 * 计划状态枚举（教程自有领域模型）.
 *
 * <p>【2.x 迁移说明】原先本枚举来自 agentscope 的
 * {@code io.agentscope.core.plan.model.PlanState}。在 agentscope 2.x 中，
 * 整个 {@code io.agentscope.core.plan.*} 结构化计划数据模型已被移除，
 * 2.x 的“计划能力”改由 harness 的计划模式（Plan Mode）以 workspace 下的
 * {@code plans/PLAN.md} 文件形式承载，不再提供结构化的 Plan/SubTask 类型。
 *
 * <p>因此本枚举被下沉为“教程自有领域模型”，用于本章 REST CRUD 与 H2 持久化演示，
 * 保持“将复杂任务拆解为可追踪子任务”的教学主旨。
 */
public enum PlanState {

    /** 待执行. */
    TODO,

    /** 执行中. */
    IN_PROGRESS,

    /** 已完成. */
    DONE,

    /** 已放弃. */
    ABANDONED;

    /**
     * 返回持久化/展示用的小写字符串值（与旧 agentscope API 的 getValue() 语义一致）.
     */
    public String getValue() {
        return name().toLowerCase();
    }

    /**
     * 从字符串解析状态，容错处理（无法识别时回退为 TODO）.
     */
    public static PlanState fromValue(String value) {
        if (value == null || value.isBlank()) {
            return TODO;
        }
        try {
            return PlanState.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return TODO;
        }
    }
}
