package io.agentscope.tutorial.chapter12.plan;

import io.agentscope.tutorial.chapter12.model.Plan;
import io.agentscope.tutorial.chapter12.model.PlanState;
import io.agentscope.tutorial.chapter12.model.SubTask;
import io.agentscope.tutorial.chapter12.model.SubTaskState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * 计划笔记本（教程自有实现）.
 *
 * <p>【2.x 迁移说明 —— 重点】
 * <ul>
 *   <li>原类型 {@code io.agentscope.core.plan.PlanNotebook} 在 agentscope 2.x 已被<b>整体移除</b>，
 *       同时移除的还有 {@code plan.model.Plan/SubTask/PlanState/SubTaskState}、
 *       {@code plan.storage.PlanStorage}、{@code plan.hint.DefaultPlanToHint}。
 *       2.x 不再提供“结构化 Plan/SubTask 数据模型 + 计划工具”的这套 API。</li>
 *   <li>2.x 官方的“计划能力”是 harness 的<b>计划模式（Plan Mode）</b>：通过
 *       {@code HarnessAgent.builder().enablePlanMode().enableTaskList()} 开启，
 *       用 {@code agent.enterPlanMode(ctx)/exitPlanMode(ctx)/isPlanModeActive(ctx)} 控制，
 *       计划以 workspace 下 {@code plans/PLAN.md} 文件呈现，
 *       状态保存在 {@code io.agentscope.core.state.PlanModeContextState}。
 *       本章 Agent 的“先规划再执行”能力已迁移到 Plan Mode，见
 *       {@code io.agentscope.tutorial.chapter12.config.PlanConfig} 与
 *       {@code io.agentscope.tutorial.chapter12.service.ChatService}。</li>
 *   <li>本类作为“教程自有的结构化计划模型”保留：它是 REST CRUD（{@code PlanController}）
 *       与 H2 持久化演示的后端，用来直观展示“任务分解 / 进度追踪”这一教学主旨。
 *       它与 harness Plan Mode 是两条独立能力线，行为语义尽量沿用旧 PlanNotebook，
 *       以便教程读者对照理解迁移前后的差异。</li>
 * </ul>
 *
 * <p>本实现在内存中维护“当前计划”，并在每次变更后：
 * <ol>
 *   <li>通过 {@link PlanStorage}（若配置）持久化到数据库；</li>
 *   <li>触发已注册的变更回调（用于 SSE 广播 / 日志）。</li>
 * </ol>
 */
public class PlanNotebook {

    private static final Logger log = LoggerFactory.getLogger(PlanNotebook.class);
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final PlanStorage storage;
    private final boolean needUserConfirm;
    private final int maxSubtasks;

    /** 变更回调集合（key -> 回调），用于 SSE 广播、日志、额外持久化等. */
    private final Map<String, BiConsumer<PlanNotebook, Plan>> changeHooks = new LinkedHashMap<>();

    /** 当前活动计划（内存态）. */
    private volatile Plan currentPlan;

    private PlanNotebook(Builder builder) {
        this.storage = builder.storage;
        this.needUserConfirm = builder.needUserConfirm;
        this.maxSubtasks = builder.maxSubtasks > 0 ? builder.maxSubtasks : 20;
    }

    public static Builder builder() {
        return new Builder();
    }

    // ==================== 变更回调 ====================

    /**
     * 注册计划变更回调（幂等，同名覆盖）.
     *
     * @param name 回调名称
     * @param hook 回调，参数为 (本 notebook, 当前计划或 null)
     */
    public void addChangeHook(String name, BiConsumer<PlanNotebook, Plan> hook) {
        if (name != null && hook != null) {
            changeHooks.put(name, hook);
        }
    }

    private void notifyChange(Plan plan) {
        for (Map.Entry<String, BiConsumer<PlanNotebook, Plan>> e : changeHooks.entrySet()) {
            try {
                e.getValue().accept(this, plan);
            } catch (Exception ex) {
                log.warn("Change hook '{}' failed: {}", e.getKey(), ex.getMessage());
            }
        }
    }

    private void persist(Plan plan) {
        if (storage != null && plan != null) {
            try {
                storage.addPlan(plan).subscribe(
                        null,
                        err -> log.error("Failed to persist plan {}: {}", plan.getId(), err.getMessage()));
            } catch (Exception ex) {
                log.error("Failed to persist plan {}: {}", plan.getId(), ex.getMessage());
            }
        }
    }

    // ==================== 查询 ====================

    /** 返回当前活动计划，无则返回 {@code null}. */
    public Plan getCurrentPlan() {
        return currentPlan;
    }

    public boolean isNeedUserConfirm() {
        return needUserConfirm;
    }

    public int getMaxSubtasks() {
        return maxSubtasks;
    }

    // ==================== 计划操作 ====================

    /**
     * 创建新计划（覆盖当前计划）.
     */
    public Mono<String> createPlan(String name,
                                   String description,
                                   String expectedOutcome,
                                   List<Map<String, Object>> subtasks) {
        return Mono.fromCallable(() -> {
            int count = subtasks != null ? subtasks.size() : 0;
            if (count > maxSubtasks) {
                return "Cannot create plan: " + count + " subtasks exceeds the maximum of "
                        + maxSubtasks + ".";
            }
            Plan plan = new Plan(name, description != null ? description : "",
                    expectedOutcome != null ? expectedOutcome : "");
            plan.setState(PlanState.TODO);
            if (subtasks != null) {
                for (Map<String, Object> m : subtasks) {
                    plan.getSubtasks().add(new SubTask(
                            str(m, "name", "Unnamed"),
                            str(m, "description", ""),
                            str(m, "expected_outcome", "")));
                }
            }
            this.currentPlan = plan;
            persist(plan);
            notifyChange(plan);
            log.info("Plan created: {} ({} subtasks)", name, plan.getSubtasks().size());
            return "Plan '" + name + "' created successfully with "
                    + plan.getSubtasks().size() + " subtasks.";
        });
    }

    /**
     * 更新计划基本信息（仅更新非 null 字段）.
     */
    public Mono<String> updatePlanInfo(String name, String description, String expectedOutcome) {
        return Mono.fromCallable(() -> {
            if (currentPlan == null) {
                return "There is no active plan to update.";
            }
            if (name != null) {
                currentPlan.setName(name);
            }
            if (description != null) {
                currentPlan.setDescription(description);
            }
            if (expectedOutcome != null) {
                currentPlan.setExpectedOutcome(expectedOutcome);
            }
            persist(currentPlan);
            notifyChange(currentPlan);
            return "Plan info updated. Current name: '" + currentPlan.getName() + "'.";
        });
    }

    /**
     * 修订当前计划的子任务（add / revise / delete）.
     */
    public Mono<String> reviseCurrentPlan(int index, String action, Map<String, Object> subtask) {
        return Mono.fromCallable(() -> {
            if (currentPlan == null) {
                return "There is no active plan to revise.";
            }
            List<SubTask> subs = currentPlan.getSubtasks();
            String act = action != null ? action.trim().toLowerCase() : "";
            switch (act) {
                case "add" -> {
                    if (subs.size() >= maxSubtasks) {
                        return "Cannot add subtask: plan already has the maximum of "
                                + maxSubtasks + " subtasks.";
                    }
                    SubTask st = new SubTask(
                            str(subtask, "name", "Unnamed"),
                            str(subtask, "description", ""),
                            str(subtask, "expected_outcome", ""));
                    int pos = Math.max(0, Math.min(index, subs.size()));
                    subs.add(pos, st);
                    persist(currentPlan);
                    notifyChange(currentPlan);
                    return "Subtask added successfully at index " + pos + ".";
                }
                case "revise" -> {
                    if (index < 0 || index >= subs.size()) {
                        return "Invalid subtask index: " + index + ".";
                    }
                    SubTask st = subs.get(index);
                    if (subtask != null) {
                        if (subtask.get("name") != null) {
                            st.setName(subtask.get("name").toString());
                        }
                        if (subtask.get("description") != null) {
                            st.setDescription(subtask.get("description").toString());
                        }
                        if (subtask.get("expected_outcome") != null) {
                            st.setExpectedOutcome(subtask.get("expected_outcome").toString());
                        }
                    }
                    persist(currentPlan);
                    notifyChange(currentPlan);
                    return "Subtask " + index + " revised successfully.";
                }
                case "delete" -> {
                    if (index < 0 || index >= subs.size()) {
                        return "Invalid subtask index: " + index + ".";
                    }
                    subs.remove(index);
                    persist(currentPlan);
                    notifyChange(currentPlan);
                    return "Subtask " + index + " deleted successfully.";
                }
                default -> {
                    return "Unknown action: '" + action + "'. Use add / revise / delete.";
                }
            }
        });
    }

    /**
     * 更新子任务状态.
     */
    public Mono<String> updateSubtaskState(int index, String state) {
        return Mono.fromCallable(() -> {
            if (currentPlan == null) {
                return "There is no active plan.";
            }
            List<SubTask> subs = currentPlan.getSubtasks();
            if (index < 0 || index >= subs.size()) {
                return "Invalid subtask index: " + index + ".";
            }
            SubTaskState newState = SubTaskState.fromValue(state);
            subs.get(index).setState(newState);
            if (newState == SubTaskState.DONE || newState == SubTaskState.ABANDONED) {
                subs.get(index).setFinishedAt(now());
            }
            persist(currentPlan);
            notifyChange(currentPlan);
            return "Subtask " + index + " state updated to " + newState.getValue() + ".";
        });
    }

    /**
     * 完成一个子任务：标记为 DONE 并记录结果，自动把下一个 TODO 子任务推进为 IN_PROGRESS.
     */
    public Mono<String> finishSubtask(int index, String outcome) {
        return Mono.fromCallable(() -> {
            if (currentPlan == null) {
                return "There is no active plan.";
            }
            List<SubTask> subs = currentPlan.getSubtasks();
            if (index < 0 || index >= subs.size()) {
                return "Invalid subtask index: " + index + ".";
            }
            SubTask st = subs.get(index);
            st.setState(SubTaskState.DONE);
            st.setOutcome(outcome);
            st.setFinishedAt(now());

            // 自动推进下一个待办子任务为进行中
            if (index + 1 < subs.size() && subs.get(index + 1).getState() == SubTaskState.TODO) {
                subs.get(index + 1).setState(SubTaskState.IN_PROGRESS);
            }

            persist(currentPlan);
            notifyChange(currentPlan);
            return "Subtask " + index + " marked as done successfully.";
        });
    }

    /**
     * 完成整个计划（done / abandoned），完成后清空当前计划.
     */
    public Mono<String> finishPlan(String state, String outcome) {
        return Mono.fromCallable(() -> {
            if (currentPlan == null) {
                return "There is no active plan to finish.";
            }
            boolean abandoned = state != null && state.trim().equalsIgnoreCase("abandoned");
            currentPlan.setState(abandoned ? PlanState.ABANDONED : PlanState.DONE);
            currentPlan.setOutcome(outcome);
            currentPlan.setFinishedAt(now());

            Plan finished = currentPlan;
            String name = finished.getName();

            // 先持久化并通知“已完成”的终态，再清空当前计划
            persist(finished);
            notifyChange(finished);
            this.currentPlan = null;
            notifyChange(null);

            return abandoned
                    ? "Plan '" + name + "' abandoned."
                    : "Plan '" + name + "' finished successfully.";
        });
    }

    /**
     * 返回当前计划的子任务数量及各状态统计的文字描述.
     */
    public Mono<String> getSubtaskCount() {
        return Mono.fromCallable(() -> {
            if (currentPlan == null || currentPlan.getSubtasks() == null) {
                return "There is no active plan (0 subtasks).";
            }
            List<SubTask> subs = currentPlan.getSubtasks();
            int todo = 0;
            int inProgress = 0;
            int done = 0;
            int abandoned = 0;
            for (SubTask st : subs) {
                switch (st.getState()) {
                    case DONE -> done++;
                    case IN_PROGRESS -> inProgress++;
                    case ABANDONED -> abandoned++;
                    default -> todo++;
                }
            }
            return "The current plan has " + subs.size() + " subtasks: "
                    + todo + " todo, " + inProgress + " in progress, "
                    + done + " done, " + abandoned + " abandoned.";
        });
    }

    // ==================== 私有工具 ====================

    private static String now() {
        return LocalDateTime.now().format(FORMATTER);
    }

    private static String str(Map<String, Object> map, String key, String def) {
        if (map == null) {
            return def;
        }
        Object v = map.get(key);
        return v != null ? v.toString() : def;
    }

    // ==================== Builder ====================

    /**
     * {@link PlanNotebook} 构建器.
     *
     * <p>【2.x 迁移说明】旧版 Builder 还有 {@code planToHint(DefaultPlanToHint)}，
     * 用于把计划渲染为提示词注入 ReActAgent。该能力在 2.x 已由 harness Plan Mode 的
     * 系统提示 banner（{@code PlanModeMiddleware}）取代，故此处不再提供 planToHint。
     */
    public static class Builder {
        private PlanStorage storage;
        private boolean needUserConfirm = true;
        private int maxSubtasks = 20;

        public Builder storage(PlanStorage storage) {
            this.storage = storage;
            return this;
        }

        public Builder needUserConfirm(boolean needUserConfirm) {
            this.needUserConfirm = needUserConfirm;
            return this;
        }

        public Builder maxSubtasks(int maxSubtasks) {
            this.maxSubtasks = maxSubtasks;
            return this;
        }

        public PlanNotebook build() {
            return new PlanNotebook(this);
        }
    }
}
