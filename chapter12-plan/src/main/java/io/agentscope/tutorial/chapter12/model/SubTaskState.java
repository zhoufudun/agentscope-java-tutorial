package io.agentscope.tutorial.chapter12.model;

/**
 * 子任务状态枚举（教程自有领域模型）.
 *
 * <p>【2.x 迁移说明】原先来自 agentscope 的
 * {@code io.agentscope.core.plan.model.SubTaskState}，2.x 已随
 * {@code io.agentscope.core.plan.*} 整包移除。此处下沉为教程自有模型。
 */
public enum SubTaskState {

    /** 待执行. */
    TODO,

    /** 执行中. */
    IN_PROGRESS,

    /** 已完成. */
    DONE,

    /** 已放弃. */
    ABANDONED;

    /**
     * 返回持久化/展示用的小写字符串值.
     */
    public String getValue() {
        return name().toLowerCase();
    }

    /**
     * 从字符串解析状态，容错处理（无法识别时回退为 TODO）.
     */
    public static SubTaskState fromValue(String value) {
        if (value == null || value.isBlank()) {
            return TODO;
        }
        try {
            return SubTaskState.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return TODO;
        }
    }
}
