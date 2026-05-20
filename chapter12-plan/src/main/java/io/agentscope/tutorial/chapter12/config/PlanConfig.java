package io.agentscope.tutorial.chapter12.config;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.formatter.dashscope.DashScopeChatFormatter;
import io.agentscope.core.hook.Hook;
import io.agentscope.core.hook.HookEvent;
import io.agentscope.core.hook.PostActingEvent;
import io.agentscope.core.hook.PreReasoningEvent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.message.*;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.plan.PlanNotebook;
import io.agentscope.core.plan.hint.DefaultPlanToHint;
import io.agentscope.core.plan.storage.PlanStorage;
import io.agentscope.core.tool.Toolkit;
import reactor.core.publisher.Mono;
import io.agentscope.core.tool.coding.ShellCommandTool;
import io.agentscope.core.tool.file.ReadFileTool;
import io.agentscope.core.tool.file.WriteFileTool;
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
import java.util.List;
import java.util.Set;

/**
 * 计划系统配置类.
 *
 * <p>演示如何在 Spring Boot 中配置 AgentScope 的计划系统：
 * <ul>
 *   <li>PlanNotebook 配置</li>
 *   <li>PlanStorage 配置（数据库持久化）</li>
 *   <li>ReActAgent 集成</li>
 *   <li>计划变更 Hook 注册</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 */
@Configuration
public class PlanConfig {

    private static final Logger log = LoggerFactory.getLogger(PlanConfig.class);

    /**
     * Allowed shell commands (whitelist).
     */
    private static final Set<String> SHELL_COMMAND_WHITELIST = Set.of(
            "ls", "pwd", "cat", "echo", "mkdir", "rmdir", "cp", "mv", "rm", "wc", "head",
            "bash", "sh", "cd", "find", "grep"
    );

    @Value("${agentscope.model.api-key:}")
    private String apiKey;

    @Value("${agentscope.model.model-name:qwen-plus}")
    private String modelName;

    private final PlanService planService;

    public PlanConfig(PlanService planService) {
        this.planService = planService;
    }

    /**
     * 配置工作目录（用于文件读写工具）.
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
     * 配置 Toolkit（包含文件操作和 Shell 工具）.
     */
    @Bean
    public Toolkit toolkit(String workspaceRoot) {
        Toolkit toolkit = new Toolkit();

        // 文件读写工具
        toolkit.registerTool(new ReadFileTool(workspaceRoot));
        toolkit.registerTool(new WriteFileTool(workspaceRoot));

        // Shell 命令工具（白名单限制）
        toolkit.registerTool(new ShellCommandTool(workspaceRoot, SHELL_COMMAND_WHITELIST, null));

        log.info("Toolkit initialized with {} tools", toolkit.getToolNames().size());
        return toolkit;
    }

    /**
     * 配置 PlanNotebook.
     *
     * <p>使用自定义的 PlanService 作为存储后端（实现数据库持久化），
     * 并注册计划变更 Hook 用于实时通知.
     */
    @Bean
    public PlanNotebook planNotebook(PlanService planService) {
        PlanNotebook notebook = PlanNotebook.builder()
                .planToHint(new DefaultPlanToHint())
                .storage(planService)  // 使用 PlanService 作为存储后端
                .needUserConfirm(true)
                .maxSubtasks(15)
                .build();

        // 注册计划变更监听器（用于 SSE 广播）
        notebook.addChangeHook("planServiceBroadcast", (notebook1, plan) -> {
            planService.broadcastPlanChange();
        });

        log.info("PlanNotebook initialized with needUserConfirm=true, maxSubtasks=15");
        return notebook;
    }

    /**
     * 配置 ReActAgent.
     *
     * <p>将 PlanNotebook 与 Agent 关联，使其可以自动接收计划相关提示.
     */
    @Bean
    public ReActAgent planAgent(
            PlanNotebook planNotebook,
            Toolkit toolkit,
            String workspaceRoot) {

        String effectiveApiKey = System.getenv("DASHSCOPE_API_KEY");
        if (effectiveApiKey == null || effectiveApiKey.isEmpty()) {
            effectiveApiKey = apiKey;
        }

        if (effectiveApiKey == null || effectiveApiKey.isEmpty()) {
            log.warn("DASHSCOPE_API_KEY not configured, agent will use placeholder key");
            effectiveApiKey = "dummy-key";
        }

        InMemoryMemory memory = new InMemoryMemory();

        ReActAgent agent = ReActAgent.builder()
                .name("PlanAssistant")
                .sysPrompt(buildSystemPrompt())
                .model(DashScopeChatModel.builder()
                        .apiKey(effectiveApiKey)
                        .modelName(modelName)
                        .stream(true)
                        .enableThinking(true)
                        .defaultOptions(GenerateOptions.builder()
                                .thinkingBudget(8192)
                                .build())
                        .formatter(new DashScopeChatFormatter())
                        .build())
                .memory(memory)
                .toolkit(toolkit)
                .maxIters(50)
                .planNotebook(planNotebook)
                .hook(createStopHook())
                .build();

        log.info("PlanAgent initialized with model: {}", modelName);
        return agent;
    }

    /**
     * 构建系统提示词.
     */
    private String buildSystemPrompt() {
        return """
                你是一个系统性助手，通过结构化计划帮助用户完成复杂任务。

                核心能力：
                1. 复杂任务规划：当用户提出复杂请求时（如编程、调研、设计），主动创建计划
                2. 任务分解：将大任务分解为可执行的小步骤
                3. 进度追踪：实时更新任务状态，确保按计划执行
                4. 结果验证：完成每个子任务后，验证是否达成预期结果

                工作流程：
                1. 理解用户需求
                2. 如需要，创建详细计划（使用 create_plan 工具）
                3. 逐步执行子任务（使用 update_subtask_state 标记进度）
                4. 完成后记录结果（使用 finish_subtask）
                5. 计划全部完成后总结（使用 finish_plan）

                注意事项：
                - 每个子任务完成后必须调用 finish_subtask 记录具体结果
                - 结果应该是具体的数据、信息或文件路径，而非"已完成"
                - 用户可能随时修改计划，需要灵活应对
                - 如果用户表示不需要继续执行计划，使用 finish_plan 放弃
                """;
    }

    /**
     * 创建停止 Hook（用于在计划工具执行后暂停，等待用户确认）.
     */
    private Hook createStopHook() {
        return new Hook() {
            @Override
            public <T extends HookEvent> Mono<T> onEvent(T event) {
                if (event instanceof PostActingEvent postActing) {
                    ToolUseBlock toolUse = postActing.getToolUse();
                    if (toolUse != null) {
                        log.debug("Tool executed: {}", toolUse.getName());
                    }
                }
                return Mono.just(event);
            }
        };
    }
}