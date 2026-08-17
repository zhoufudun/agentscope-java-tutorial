package io.agentscope.tutorial.chapter12.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.event.ConfirmResult;
import io.agentscope.core.event.RequireUserConfirmEvent;
import io.agentscope.core.event.TextBlockDeltaEvent;
import io.agentscope.core.event.ThinkingBlockDeltaEvent;
import io.agentscope.core.event.ToolCallStartEvent;
import io.agentscope.core.event.ToolResultTextDeltaEvent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.message.UserMessage;
import io.agentscope.harness.agent.HarnessAgent;
import io.agentscope.tutorial.chapter12.model.Plan;
import io.agentscope.tutorial.chapter12.plan.PlanNotebook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 聊天服务 - 管理 Agent 对话和状态.
 *
 * <p>【2.x 迁移说明 —— 重点】
 * <ul>
 *   <li>底层 Agent 由 1.x 的 {@code ReActAgent + PlanNotebook} 改为 2.x 的
 *       {@link HarnessAgent}（开启 Plan Mode）。</li>
 *   <li>流式接口由 1.x 的 {@code agent.stream(msg, StreamOptions)}（返回 {@code Flux<Event>}）
 *       改为 2.x 的 {@code agent.streamEvents(msg, ctx)}（返回细粒度的 {@code Flux<AgentEvent>}）。
 *       事件由具体子类型区分：{@code TextBlockDeltaEvent}（文本增量）、
 *       {@code ThinkingBlockDeltaEvent}（思维增量）、{@code ToolResultTextDeltaEvent}（工具结果增量）、
 *       {@code ToolCallStartEvent}（工具开始）、{@code RequireUserConfirmEvent}（HITL 需确认）等。</li>
 *   <li>“暂停 / 恢复”的语义变化：1.x 靠 {@code GenerateReason.ACTING_STOP_REQUESTED} 暂停；
 *       2.x 计划模式下 {@code plan_exit} 会触发 {@code RequireUserConfirmEvent}（人类在环 HITL），
 *       我们据此发出 {@code {"t":"paused"}}，并在 resume 时用携带
 *       {@code ConfirmResult} 的消息（{@code Msg.METADATA_CONFIRM_RESULTS}）批准并继续。</li>
 *   <li>会话上下文调试推送（旧的 {@code {"t":"ctx",...}}）已移除：它依赖 1.x 的
 *       {@code agent.getMemory().getMessages()} 内省 API；2.x 会话历史保存在 AgentState，
 *       不再以这种方式暴露，教学上用工具事件流展示即可。</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 */
@Service
public class ChatService implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private static final ObjectMapper SSE_JSON = new ObjectMapper();

    private final PlanNotebook planNotebook;
    private final HarnessAgent agent;

    /** 暂停状态标志（等待用户对 plan_exit 的确认）. */
    private final AtomicBoolean isPaused = new AtomicBoolean(false);

    /** 停止请求标志. */
    private final AtomicBoolean stopRequested = new AtomicBoolean(false);

    /** 每个会话对应的 RuntimeContext（resume 时复用同一 session）. */
    private final Map<String, RuntimeContext> sessionContexts = new ConcurrentHashMap<>();

    /** 每个会话待确认的 HITL 事件（通常是 plan_exit）. */
    private final Map<String, RequireUserConfirmEvent> pendingConfirms = new ConcurrentHashMap<>();

    public ChatService(PlanNotebook planNotebook, HarnessAgent planAgent) {
        this.planNotebook = planNotebook;
        this.agent = planAgent;
    }

    @Override
    public void afterPropertiesSet() {
        // 注册计划变更 Hook（针对教程自有的结构化 PlanNotebook，用于日志）
        planNotebook.addChangeHook("chatServiceLog", (notebook, plan) -> {
            if (plan != null) {
                log.info("Plan changed: {} (state={}, subtasks={})",
                        plan.getName(),
                        plan.getState(),
                        plan.getSubtasks() != null ? plan.getSubtasks().size() : 0);
            } else {
                log.info("Plan cleared");
            }
        });
        log.info("ChatService initialized (HarnessAgent plan mode)");
    }

    /**
     * 发送消息并获取流式响应.
     */
    public Flux<String> chat(String sessionId, String message) {
        String sid = normalizeSession(sessionId);

        // 重置状态
        isPaused.set(false);
        stopRequested.set(false);
        pendingConfirms.remove(sid);

        RuntimeContext ctx = RuntimeContext.builder().sessionId(sid).build();
        sessionContexts.put(sid, ctx);

        log.info("Chat request [session={}]: {}", sid, truncate(message, 100));

        Msg userMsg = new UserMessage(message);
        return runStream(sid, agent.streamEvents(userMsg, ctx));
    }

    /**
     * 恢复暂停中的 Agent（批准 plan_exit 的 HITL 确认，回到 BUILD 模式继续执行）.
     */
    public Flux<String> resume(String sessionId) {
        String sid = normalizeSession(sessionId);
        RequireUserConfirmEvent pending = pendingConfirms.remove(sid);
        RuntimeContext ctx = sessionContexts.get(sid);

        if (pending == null || ctx == null) {
            log.warn("Tried to resume but no pending confirmation [session={}]", sid);
            return Flux.just(safeJson(Map.of(
                    "t", "error",
                    "d", "Agent is not paused or has no pending confirmation.")));
        }

        isPaused.set(false);
        log.info("Resuming agent execution [session={}], approving {} tool call(s)",
                sid, pending.getToolCalls().size());

        // 批准所有待确认的工具调用（通常是 plan_exit）
        List<ConfirmResult> results = new ArrayList<>();
        for (ToolUseBlock call : pending.getToolCalls()) {
            results.add(new ConfirmResult(true, call));
        }
        Msg resumeMsg = UserMessage.builder()
                .metadata(Map.of(Msg.METADATA_CONFIRM_RESULTS, results))
                .build();

        return runStream(sid, agent.streamEvents(resumeMsg, ctx));
    }

    /**
     * 请求停止 Agent.
     *
     * <p>【2.x 迁移说明】1.x 通过标志位在下一个计划工具后暂停；2.x 直接使用
     * {@link HarnessAgent#interrupt()} 中断当前运行。
     */
    public void requestStop() {
        log.info("Stop requested - interrupting agent");
        stopRequested.set(true);
        try {
            agent.interrupt();
        } catch (Exception e) {
            log.warn("Interrupt failed: {}", e.getMessage());
        }
    }

    /**
     * 检查 Agent 是否暂停（等待 plan_exit 确认）.
     */
    public boolean isPaused() {
        return isPaused.get();
    }

    /**
     * 重置 Agent 状态.
     */
    public void reset() {
        log.info("Resetting agent");
        isPaused.set(false);
        stopRequested.set(false);
        pendingConfirms.clear();
        sessionContexts.clear();
        log.info("Agent reset completed");
    }

    /**
     * 获取当前（教程自有结构化）计划.
     */
    public Plan getCurrentPlan() {
        return planNotebook.getCurrentPlan();
    }

    // ==================== 私有辅助方法 ====================

    private Flux<String> runStream(String sessionId, Flux<AgentEvent> events) {
        return events
                .concatMap(event -> {
                    String mapped = mapEventToString(sessionId, event);
                    return (mapped == null || mapped.isEmpty()) ? Flux.empty() : Flux.just(mapped);
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 将 2.x 的 {@link AgentEvent} 映射为前端 SSE JSON 字符串.
     */
    private String mapEventToString(String sessionId, AgentEvent event) {
        try {
            // 思维内容增量
            if (event instanceof ThinkingBlockDeltaEvent e) {
                String delta = e.getDelta();
                if (delta != null && !delta.isEmpty()) {
                    return safeJson(Map.of("t", "think", "d", delta));
                }
                return "";
            }

            // 文本内容增量
            if (event instanceof TextBlockDeltaEvent e) {
                String delta = e.getDelta();
                if (delta != null && !delta.isEmpty()) {
                    return safeJson(Map.of("t", "text", "d", delta));
                }
                return "";
            }

            // 工具调用开始
            if (event instanceof ToolCallStartEvent e) {
                return safeJson(Map.of(
                        "t", "tool_start",
                        "n", nullToEmpty(e.getToolCallName())));
            }

            // 工具结果增量
            if (event instanceof ToolResultTextDeltaEvent e) {
                String delta = e.getDelta();
                Map<String, Object> payload = new LinkedHashMap<>(3);
                payload.put("t", "tool");
                payload.put("n", nullToEmpty(e.getToolCallName()));
                payload.put("d", nullToEmpty(delta));
                return safeJson(payload);
            }

            // HITL：plan_exit 需要用户确认 -> 暂停等待前端 resume
            if (event instanceof RequireUserConfirmEvent e) {
                pendingConfirms.put(sessionId, e);
                isPaused.set(true);
                log.info("Agent paused for HITL confirmation [session={}]", sessionId);
                return safeJson(Map.of("t", "paused"));
            }

            return "";
        } catch (Exception ex) {
            log.warn("Failed to encode SSE chunk: {}", ex.getMessage());
            return "";
        }
    }

    private String safeJson(Map<String, Object> payload) {
        try {
            return SSE_JSON.writeValueAsString(payload);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String normalizeSession(String sessionId) {
        return (sessionId == null || sessionId.isEmpty()) ? "default" : sessionId;
    }

    private String nullToEmpty(String s) {
        return s != null ? s : "";
    }

    private String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, max) + "...";
    }
}
