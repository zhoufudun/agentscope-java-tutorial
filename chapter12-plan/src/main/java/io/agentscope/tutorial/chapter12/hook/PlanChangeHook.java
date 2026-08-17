package io.agentscope.tutorial.chapter12.hook;

import io.agentscope.tutorial.chapter12.model.Plan;
import io.agentscope.tutorial.chapter12.model.SubTask;
import io.agentscope.tutorial.chapter12.plan.PlanNotebook;
import io.agentscope.tutorial.chapter12.service.PlanService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * 计划变更监听 Hook.
 *
 * <p>监听计划状态变化，执行必要的业务逻辑：
 * <ul>
 *   <li>记录计划变更日志</li>
 *   <li>通过 PlanService 广播计划变更到 SSE</li>
 *   <li>持久化计划到数据库</li>
 * </ul>
 *
 * <p>通过在 PlanNotebook 中注册，可以捕获以下事件：
 * <ul>
 *   <li>创建新计划</li>
 *   <li>添加/修改/删除子任务</li>
 *   <li>子任务状态变更</li>
 *   <li>计划完成或放弃</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 */
@Component
public class PlanChangeHook {

    private static final Logger log = LoggerFactory.getLogger(PlanChangeHook.class);

    private final PlanService planService;

    public PlanChangeHook(PlanService planService) {
        this.planService = planService;
    }

    /**
     * 注册到 PlanNotebook 的变更回调.
     *
     * @param planNotebook 计划笔记本实例
     */
    public void register(PlanNotebook planNotebook) {
        planNotebook.addChangeHook("databasePersistence", this::onPlanChanged);
        planNotebook.addChangeHook("sseBroadcast", (notebook, plan) -> planService.broadcastPlanChange());
        log.info("PlanChangeHook registered to PlanNotebook");
    }

    /**
     * 计划变更处理方法 - 持久化到数据库.
     *
     * @param notebook 计划笔记本实例
     * @param plan 当前计划（可能为 null 表示计划已清除）
     */
    private void onPlanChanged(PlanNotebook notebook, Plan plan) {
        if (plan == null) {
            log.info("Plan cleared");
            return;
        }

        log.info("Plan changed: {} (state={}, subtasks={})",
                plan.getName(),
                plan.getState(),
                plan.getSubtasks() != null ? plan.getSubtasks().size() : 0);

        // 打印子任务状态
        if (plan.getSubtasks() != null) {
            int idx = 0;
            for (SubTask subtask : plan.getSubtasks()) {
                log.debug("  Subtask[{}] {}: {}", idx++, subtask.getName(), subtask.getState());
            }
        }

        // 持久化计划
        try {
            planService.savePlan(plan);
        } catch (Exception e) {
            log.error("Failed to persist plan", e);
        }
    }

    // ==================== 静态辅助方法 ====================

    /**
     * 创建变更日志摘要.
     *
     * @param plan 计划对象
     * @return 格式化的日志字符串
     */
    public static String createLogSummary(Plan plan) {
        if (plan == null) {
            return "No active plan";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Plan: ").append(plan.getName()).append("\n");
        sb.append("State: ").append(plan.getState()).append("\n");
        sb.append("Subtasks:\n");

        if (plan.getSubtasks() != null) {
            int done = 0, inProgress = 0, todo = 0;
            for (SubTask subtask : plan.getSubtasks()) {
                String marker = switch (subtask.getState()) {
                    case DONE -> "[x]";
                    case IN_PROGRESS -> "[>]";
                    case ABANDONED -> "[-]";
                    default -> "[ ]";
                };
                sb.append("  ").append(marker).append(" ").append(subtask.getName()).append("\n");

                switch (subtask.getState()) {
                    case DONE -> done++;
                    case IN_PROGRESS -> inProgress++;
                    default -> todo++;
                }
            }
            sb.append("Progress: ").append(done).append(" done, ")
                    .append(inProgress).append(" in progress, ")
                    .append(todo).append(" todo");
        }

        return sb.toString();
    }

    /**
     * 格式化计划为 Markdown.
     */
    public static String formatAsMarkdown(Plan plan) {
        if (plan == null) {
            return "## No Active Plan\n\nNo plan is currently active.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("## Plan: ").append(plan.getName()).append("\n\n");
        sb.append("- **State**: ").append(plan.getState().getValue()).append("\n");
        sb.append("- **Description**: ").append(plan.getDescription()).append("\n");
        sb.append("- **Expected Outcome**: ").append(plan.getExpectedOutcome()).append("\n\n");

        if (plan.getSubtasks() != null && !plan.getSubtasks().isEmpty()) {
            sb.append("### Subtasks\n\n");
            int idx = 0;
            for (SubTask subtask : plan.getSubtasks()) {
                String statusIcon = switch (subtask.getState()) {
                    case DONE -> "✅";
                    case IN_PROGRESS -> "🔄";
                    case ABANDONED -> "❌";
                    default -> "⬜";
                };
                sb.append(String.format("%d. %s %s\n", idx++, statusIcon, subtask.getName()));
                sb.append("   - Description: ").append(subtask.getDescription()).append("\n");
                sb.append("   - Expected: ").append(subtask.getExpectedOutcome()).append("\n");
                if (subtask.getState().name().equals("DONE") && subtask.getOutcome() != null) {
                    sb.append("   - Result: ").append(subtask.getOutcome()).append("\n");
                }
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * 提取计划进度统计.
     */
    public static PlanProgress getProgress(Plan plan) {
        if (plan == null || plan.getSubtasks() == null || plan.getSubtasks().isEmpty()) {
            return new PlanProgress(0, 0, 0, 0, 0);
        }

        int total = plan.getSubtasks().size();
        int done = 0, inProgress = 0, abandoned = 0, todo = 0;

        for (SubTask subtask : plan.getSubtasks()) {
            switch (subtask.getState()) {
                case DONE -> done++;
                case IN_PROGRESS -> inProgress++;
                case ABANDONED -> abandoned++;
                default -> todo++;
            }
        }

        double percent = total > 0 ? (done * 100.0 / total) : 0;
        return new PlanProgress(total, done, inProgress, abandoned, todo, percent);
    }

    /**
     * 计划进度记录.
     */
    public record PlanProgress(
            int total,
            int done,
            int inProgress,
            int abandoned,
            int todo,
            double percentDone
    ) {
        public PlanProgress(int total, int done, int inProgress, int abandoned, int todo) {
            this(total, done, inProgress, abandoned, todo, total > 0 ? (done * 100.0 / total) : 0);
        }
    }
}