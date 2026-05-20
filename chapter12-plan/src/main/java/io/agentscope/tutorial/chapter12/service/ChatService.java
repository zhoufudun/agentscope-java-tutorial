package io.agentscope.tutorial.chapter12.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.Event;
import io.agentscope.core.agent.EventType;
import io.agentscope.core.agent.StreamOptions;
import io.agentscope.core.hook.Hook;
import io.agentscope.core.hook.HookEvent;
import io.agentscope.core.hook.PostActingEvent;
import io.agentscope.core.hook.PreReasoningEvent;
import io.agentscope.core.message.*;
import io.agentscope.core.plan.PlanNotebook;
import io.agentscope.core.plan.model.Plan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 聊天服务 - 管理 Agent 对话和状态.
 *
 * <p>核心职责：
 * <ul>
 *   <li>发送消息并获取 Agent 的流式响应</li>
 *   <li>管理 Agent 的暂停/恢复/停止状态</li>
 *   <li>通过 Hook 捕获中间状态用于 SSE 展示</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 */
@Service
public class ChatService implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private static final ObjectMapper SSE_JSON = new ObjectMapper();

    /** SSE 上下文消息最大字符数 */
    private static final int CTX_FLAT_MAX_CHARS = 48_000;

    /** SSE 上下文字段最大字符数 */
    private static final int CTX_FIELD_MAX_CHARS = 8_000;

    /** 计划工具名称集合（用于日志记录） */
    private static final Set<String> PLAN_TOOL_NAMES = Set.of(
            "create_plan", "update_plan_info", "revise_current_plan",
            "update_subtask_state", "finish_subtask", "get_subtask_count",
            "finish_plan", "view_subtasks", "view_historical_plans", "recover_historical_plan"
    );

    private final PlanNotebook planNotebook;
    private final ReActAgent agent;

    /** 暂停状态标志 */
    private final AtomicBoolean isPaused = new AtomicBoolean(false);

    /** 停止请求标志 */
    private final AtomicBoolean stopRequested = new AtomicBoolean(false);

    /** 待处理的工具输入（用于 SSE 展示） */
    private final ConcurrentLinkedQueue<Map<String, Object>> pendingToolInputs = new ConcurrentLinkedQueue<>();

    /** 待处理的上下文行（用于 SSE 调试） */
    private final ConcurrentLinkedQueue<String> pendingContextLines = new ConcurrentLinkedQueue<>();
    private final AtomicInteger contextSeq = new AtomicInteger(0);

    public ChatService(PlanNotebook planNotebook, ReActAgent planAgent) {
        this.planNotebook = planNotebook;
        this.agent = planAgent;
    }

    @Override
    public void afterPropertiesSet() {
        // 注册计划变更 Hook
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
        log.info("ChatService initialized");
    }

    /**
     * 发送消息并获取流式响应.
     */
    public Flux<String> chat(String sessionId, String message) {
        // 重置状态
        isPaused.set(false);
        stopRequested.set(false);
        pendingToolInputs.clear();
        pendingContextLines.clear();
        contextSeq.set(0);

        // 构建用户消息
        Msg userMsg = Msg.builder()
                .role(MsgRole.USER)
                .content(TextBlock.builder().text(message).build())
                .build();

        log.info("Chat request [session={}]: {}", sessionId, truncate(message, 100));

        // 执行对话并附加上下文
        return attachPendingContext(
                agent.stream(userMsg, createStreamOptions())
                        .subscribeOn(Schedulers.boundedElastic())
        );
    }

    /**
     * 恢复暂停中的 Agent.
     */
    public Flux<String> resume(String sessionId) {
        if (isPaused.compareAndSet(true, false)) {
            log.info("Resuming agent execution [session={}]", sessionId);
            pendingToolInputs.clear();
            pendingContextLines.clear();
            contextSeq.set(0);

            return attachPendingContext(
                    agent.stream(createStreamOptions())
                            .subscribeOn(Schedulers.boundedElastic())
            );
        } else {
            log.warn("Tried to resume but agent is not paused [session={}]", sessionId);
            try {
                return Flux.just(SSE_JSON.writeValueAsString(
                        Map.of("t", "error", "d", "Agent is not paused or is already resuming.")
                ));
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                return Flux.just("{\"t\":\"error\",\"d\":\"Agent is not paused or is already resuming.\"}");
            }
        }
    }

    /**
     * 请求停止 Agent（在下一个计划工具执行后暂停）.
     */
    public void requestStop() {
        log.info("Stop requested - will pause after next plan tool");
        stopRequested.set(true);
    }

    /**
     * 检查 Agent 是否暂停.
     */
    public boolean isPaused() {
        return isPaused.get();
    }

    /**
     * 重置 Agent.
     */
    public void reset() {
        log.info("Resetting agent");
        isPaused.set(false);
        stopRequested.set(false);
        pendingToolInputs.clear();
        pendingContextLines.clear();
        contextSeq.set(0);
        log.info("Agent reset completed");
    }

    /**
     * 获取当前计划.
     */
    public Plan getCurrentPlan() {
        return planNotebook.getCurrentPlan();
    }

    // ==================== 私有辅助方法 ====================

    private StreamOptions createStreamOptions() {
        return StreamOptions.builder()
                .eventTypes(EventType.REASONING, EventType.TOOL_RESULT, EventType.AGENT_RESULT)
                .incremental(true)
                .includeActingChunk(false)
                .build();
    }

    private Flux<String> attachPendingContext(Flux<Event> events) {
        return events.concatMap(event -> {
            List<String> prefix = drainPendingContextLines();
            String mapped = mapEventToString(event);
            boolean hasPrefix = !prefix.isEmpty();
            boolean hasBody = mapped != null && !mapped.isEmpty();
            if (!hasPrefix && !hasBody) {
                return Flux.empty();
            }
            Flux<String> head = Flux.fromIterable(prefix);
            return hasBody ? head.concatWith(Flux.just(mapped)) : head;
        });
    }

    private List<String> drainPendingContextLines() {
        List<String> out = new ArrayList<>();
        String line;
        while ((line = pendingContextLines.poll()) != null) {
            out.add(line);
        }
        return out;
    }

    private void offerContextLine(String phase, String modelName, List<Msg> messages) {
        try {
            int seq = contextSeq.incrementAndGet();
            Map<String, Object> root = new LinkedHashMap<>();
            root.put("t", "ctx");
            root.put("phase", phase);
            root.put("seq", seq);
            root.put("model", modelName != null ? modelName : "");
            List<Map<String, Object>> rows = new ArrayList<>();
            for (Msg m : messages) {
                rows.add(msgToDebugRow(m));
            }
            root.put("messages", rows);
            String flat = truncateIfNeeded(buildFlatTranscript(messages), CTX_FLAT_MAX_CHARS);
            root.put("flat", flat);
            pendingContextLines.offer(SSE_JSON.writeValueAsString(root));
        } catch (Exception e) {
            log.warn("Failed to serialize context for SSE: {}", e.getMessage());
        }
    }

    private Map<String, Object> msgToDebugRow(Msg m) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("role", m.getRole().name());
        if (m.getName() != null) {
            row.put("name", m.getName());
        }
        List<Object> parts = new ArrayList<>();
        for (ContentBlock b : m.getContent()) {
            parts.add(contentBlockToDebugMap(b));
        }
        row.put("content", parts);
        return row;
    }

    private Object contentBlockToDebugMap(ContentBlock b) {
        if (b instanceof TextBlock tb) {
            return Map.of("type", "text", "text", truncateIfNeeded(tb.getText(), CTX_FIELD_MAX_CHARS));
        }
        if (b instanceof ThinkingBlock th) {
            return Map.of("type", "thinking", "thinking", truncateIfNeeded(th.getThinking(), CTX_FIELD_MAX_CHARS));
        }
        if (b instanceof ToolUseBlock tu) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("type", "tool_use");
            m.put("id", tu.getId());
            m.put("name", tu.getName());
            m.put("input", tu.getInput() != null ? tu.getInput() : Map.of());
            return m;
        }
        if (b instanceof ToolResultBlock tr) {
            return Map.of("type", "tool_result", "name", tr.getName(), "output",
                    truncateIfNeeded(flattenToolOutput(tr), CTX_FIELD_MAX_CHARS));
        }
        return Map.of("type", "other");
    }

    private String buildFlatTranscript(List<Msg> messages) {
        StringBuilder sb = new StringBuilder();
        for (Msg m : messages) {
            sb.append("--- ").append(m.getRole().name()).append(" ---\n");
            for (ContentBlock b : m.getContent()) {
                appendContentBlockForFlat(sb, b);
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    private void appendContentBlockForFlat(StringBuilder sb, ContentBlock o) {
        if (o instanceof TextBlock tb) {
            sb.append(tb.getText() != null ? tb.getText() : "");
        } else if (o instanceof ThinkingBlock th) {
            sb.append(th.getThinking() != null ? th.getThinking() : "");
        } else if (o instanceof ToolUseBlock tu) {
            sb.append("[tool_use ").append(tu.getName()).append("] ");
            sb.append(tu.getInput() != null ? tu.getInput().toString() : "{}");
        } else if (o instanceof ToolResultBlock tr) {
            sb.append("[tool_result ").append(tr.getName()).append("]\n");
            sb.append(flattenToolOutput(tr));
        }
    }

    private String mapEventToString(Event event) {
        try {
            // 处理 Agent 结果
            if (event.getType() == EventType.AGENT_RESULT) {
                Msg msg = event.getMessage();
                if (msg != null && msg.getGenerateReason() == GenerateReason.ACTING_STOP_REQUESTED) {
                    isPaused.set(true);
                    return SSE_JSON.writeValueAsString(Map.of("t", "paused"));
                }
                return "";
            }

            // 处理工具结果
            if (event.getType() == EventType.TOOL_RESULT) {
                Msg msg = event.getMessage();
                if (msg == null) return "";
                List<ToolResultBlock> blocks = msg.getContentBlocks(ToolResultBlock.class);
                if (blocks.isEmpty()) return "";
                ToolResultBlock tr = blocks.get(0);
                Map<String, Object> payload = new LinkedHashMap<>(4);
                payload.put("t", "tool");
                payload.put("n", tr.getName() != null ? tr.getName() : "");
                payload.put("d", flattenToolOutput(tr));
                Map<String, Object> inputSnapshot = pendingToolInputs.poll();
                if (inputSnapshot != null && !inputSnapshot.isEmpty()) {
                    payload.put("i", inputSnapshot);
                }
                return SSE_JSON.writeValueAsString(payload);
            }

            if (event.isLast()) return "";

            Msg msg = event.getMessage();
            if (msg == null) return "";

            // 处理思维内容
            List<ThinkingBlock> thinkingBlocks = msg.getContentBlocks(ThinkingBlock.class);
            if (!thinkingBlocks.isEmpty()) {
                String delta = thinkingBlocks.get(0).getThinking();
                if (delta != null && !delta.isEmpty()) {
                    return SSE_JSON.writeValueAsString(Map.of("t", "think", "d", delta));
                }
            }

            // 处理文本内容
            List<TextBlock> textBlocks = msg.getContentBlocks(TextBlock.class);
            if (!textBlocks.isEmpty()) {
                String delta = textBlocks.get(0).getText();
                if (delta != null && !delta.isEmpty()) {
                    return SSE_JSON.writeValueAsString(Map.of("t", "text", "d", delta));
                }
            }
            return "";
        } catch (Exception e) {
            log.warn("Failed to encode SSE chunk: {}", e.getMessage());
            return "";
        }
    }

    private String flattenToolOutput(ToolResultBlock block) {
        StringBuilder sb = new StringBuilder();
        for (ContentBlock o : block.getOutput()) {
            if (o instanceof TextBlock tb) {
                sb.append(tb.getText());
            }
        }
        return sb.toString();
    }

    private String truncateIfNeeded(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return s.substring(0, max) + "\n...(truncated)";
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return s.substring(0, max) + "...";
    }
}