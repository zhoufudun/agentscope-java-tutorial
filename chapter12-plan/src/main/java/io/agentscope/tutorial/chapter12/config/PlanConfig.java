package io.agentscope.tutorial.chapter12.config;

import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.extensions.model.dashscope.DashScopeChatModel;
import io.agentscope.extensions.model.dashscope.formatter.DashScopeChatFormatter;
import io.agentscope.harness.agent.HarnessAgent;
import io.agentscope.tutorial.chapter12.plan.PlanNotebook;
import io.agentscope.tutorial.chapter12.service.PlanService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 计划系统配置类.
 *
 * <p>【2.x 迁移说明 —— 本章核心变化】
 * <ul>
 *   <li>1.x 里 Agent 的“计划能力”是把 {@code PlanNotebook + PlanStorage + DefaultPlanToHint}
 *       接到 {@code ReActAgent}（通过 {@code .planNotebook(...)}）。这一整套
 *       {@code io.agentscope.core.plan.*} 在 2.x 已被移除。</li>
 *   <li>2.x 用 harness 的<b>计划模式（Plan Mode）</b>重建“先规划、再执行”的能力：
 *       <pre>{@code
 *       HarnessAgent.builder()
 *           .enablePlanMode()   // 注册 plan_enter / plan_write / plan_exit 工具 + PlanModeMiddleware
 *           .enableTaskList()   // 注册 todo_write，配合计划逐步执行
 *           .workspace(dir)     // 计划以 workspace 下 plans/PLAN.md 文件呈现
 *           .build();
 *       }</pre>
 *       进入/退出计划模式由 {@code agent.enterPlanMode(ctx)/exitPlanMode(ctx)/isPlanModeActive(ctx)} 控制，
 *       计划模式状态保存在 {@code io.agentscope.core.state.PlanModeContextState}。</li>
 *   <li>{@code InMemoryMemory} / {@code .memory(...)} 已移除：2.x 会话历史保存在 AgentState，
 *       由 HarnessAgent 依据 (userId, sessionId) 自动加载/保存（默认 JsonFileAgentStateStore）。</li>
 *   <li>文件读写 / Shell 工具不再手动注册：HarnessAgent 会依据 workspace 自动提供
 *       {@code FilesystemTool} 与（沙箱模式下的）shell 工具。本例开启
 *       {@code allowShellInPlanMode()}，允许计划模式下只读地执行 shell 做调研。</li>
 * </ul>
 *
 * <p>另外仍保留教程自有的 {@link PlanNotebook}（结构化计划 CRUD + H2 持久化），
 * 它与 harness Plan Mode 是两条独立能力线，服务于 {@code PlanController} 的可视化演示。
 *
 * @author AgentScope Tutorial
 */
@Configuration
public class PlanConfig {

    private static final Logger log = LoggerFactory.getLogger(PlanConfig.class);

    @Value("${agentscope.model.api-key:}")
    private String apiKey;

    @Value("${agentscope.model.model-name:qwen-plus}")
    private String modelName;

    /**
     * 配置工作目录（HarnessAgent 的 workspace，计划文件 plans/PLAN.md 落在此处）.
     */
    @Bean
    public String workspaceRoot() {
        String override = System.getenv("PLAN_WORKSPACE");
        Path root = override != null && !override.isEmpty()
                ? Paths.get(override).toAbsolutePath().normalize()
                : Paths.get(System.getProperty("user.home"), ".agentscope", "chapter12-plan", "workspace")
                        .toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create workspace directory: " + root, e);
        }
        log.info("Plan workspace root: {}", root);
        return root.toString();
    }

    /**
     * 配置教程自有的 PlanNotebook（结构化计划 CRUD 的后端）.
     *
     * <p>【2.x 迁移说明】旧版这里是 {@code io.agentscope.core.plan.PlanNotebook.builder()}
     * 并带 {@code .planToHint(new DefaultPlanToHint())}；2.x 该类型已删除，故改用教程自有实现，
     * 且不再需要 planToHint（提示注入已由 harness 的 PlanModeMiddleware 系统提示 banner 取代）。
     */
    @Bean
    public PlanNotebook planNotebook(PlanService planService) {
        PlanNotebook notebook = PlanNotebook.builder()
                .storage(planService)   // 使用 PlanService 作为存储后端（H2 持久化）
                .needUserConfirm(true)
                .maxSubtasks(15)
                .build();

        // 注册计划变更监听器（用于 SSE 广播）
        notebook.addChangeHook("planServiceBroadcast", (nb, plan) -> planService.broadcastPlanChange());

        log.info("Tutorial PlanNotebook initialized with needUserConfirm=true, maxSubtasks=15");
        return notebook;
    }

    /**
     * 配置 HarnessAgent（2.x 计划模式）.
     *
     * <p>用 {@code enablePlanMode()} + {@code enableTaskList()} 重建“先规划再执行”的能力，
     * 替代 1.x 的 {@code ReActAgent.builder().planNotebook(...)}。
     */
    @Bean(destroyMethod = "close")
    public HarnessAgent planAgent(String workspaceRoot) {
        String effectiveApiKey = System.getenv("DASHSCOPE_API_KEY");
        if (effectiveApiKey == null || effectiveApiKey.isEmpty()) {
            effectiveApiKey = apiKey;
        }
        if (effectiveApiKey == null || effectiveApiKey.isEmpty()) {
            log.warn("DASHSCOPE_API_KEY not configured, agent will use placeholder key (demo mode)");
            effectiveApiKey = "dummy-key";
        }

        // 【2.x 迁移】DashScopeChatModel 已迁到扩展模块 io.agentscope.extensions.model.dashscope.*
        Model model = DashScopeChatModel.builder()
                .apiKey(effectiveApiKey)
                .modelName(modelName)
                .stream(true)
                .enableThinking(true)
                .defaultOptions(GenerateOptions.builder()
                        .thinkingBudget(8192)
                        .build())
                .formatter(new DashScopeChatFormatter())
                .build();

        HarnessAgent agent = HarnessAgent.builder()
                .name("PlanAssistant")
                .sysPrompt(buildSystemPrompt())
                .model(model)
                // 计划文件（plans/PLAN.md）与会话状态都落在这个 workspace 下
                .workspace(Paths.get(workspaceRoot))
                // 开启计划模式：注册 plan_enter / plan_write / plan_exit 工具与 PlanModeMiddleware
                .enablePlanMode()
                // 开启任务清单：注册 todo_write，进入 BUILD 模式后按计划逐步执行
                .enableTaskList()
                // 允许计划模式下“只读地”运行 shell 做调研（cat/ls/grep 等）
                .allowShellInPlanMode()
                .maxIters(50)
                .build();

        log.info("PlanAgent (HarnessAgent, plan mode enabled) initialized with model: {}", modelName);
        return agent;
    }

    /**
     * 构建系统提示词（面向 harness 计划模式）.
     */
    private String buildSystemPrompt() {
        return """
                你是一个系统性助手，通过“先规划、再执行”的方式帮助用户完成复杂任务。

                工作方式（harness 计划模式 Plan Mode）：
                1. 遇到复杂、多步骤或不明确的任务时，先调用 plan_enter 进入只读的“计划模式”。
                2. 在计划模式中只能进行只读调研（读取文件、只读 shell 等），不能修改文件或执行有副作用的命令。
                3. 调研清楚后，用 plan_write 把完整计划写入 plans/PLAN.md（包含目标、步骤、风险、验证方式）。
                4. 计划就绪后调用 plan_exit 请求用户确认；用户批准后你会回到 BUILD 模式，才可以真正动手执行。
                5. 回到 BUILD 模式后，用 todo_write 把计划步骤登记为任务清单，保持恰好一个任务处于进行中，逐步完成。

                注意事项：
                - 不要“只描述不行动”：决定写计划就立刻调用 plan_write，决定执行就调用 plan_exit。
                - 如果信息不足以产出具体计划，停下来向用户提出一个明确的澄清问题，不要臆造计划。
                - 计划完成后逐步执行，直到任务真正完成，而不是在产出计划后就停下。
                """;
    }
}
