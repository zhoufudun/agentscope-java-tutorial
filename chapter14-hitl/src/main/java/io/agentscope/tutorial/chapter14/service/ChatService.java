package io.agentscope.tutorial.chapter14.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.Event;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.agent.StreamOptions;
import io.agentscope.core.state.AgentStateStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 聊天服务.
 *
 * <p>封装 Agent 的聊天逻辑，提供 SSE 流式响应.
 *
 * @author AgentScope Tutorial
 */
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    /** 需要审批的工具名称 */
    private static final Set<String> TOOLS_REQUIRING_APPROVAL = Set.of("send_email", "delete_data");

    /** 运行中的代理映射 */
    private final ConcurrentHashMap<String, ReActAgent> runningAgents = new ConcurrentHashMap<>();

    /** 原型代理 */
    private final ReActAgent prototypeAgent;

    /** 会话状态存储（2.x 替代旧的 Session） */
    private final AgentStateStore stateStore;

    /** 审批服务 */
    private final ApprovalService approvalService;

    /** JSON 工具 */
    private final ObjectMapper objectMapper;

    public ChatService(
            ReActAgent prototypeAgent,
            AgentStateStore stateStore,
            ApprovalService approvalService,
            ObjectMapper objectMapper) {
        this.prototypeAgent = prototypeAgent;
        this.stateStore = stateStore;
        this.approvalService = approvalService;
        this.objectMapper = objectMapper;
    }

    /**
     * 构造携带 sessionId 的运行时上下文.
     *
     * <p>2.x 里会话不再用 {@code SessionKey} 对象标识，而是用 String sessionId。
     * 每次调用把 sessionId 放进 {@link RuntimeContext}，Agent 便会依据它从
     * {@link AgentStateStore} 自动 load / save 对应会话的历史（userId 传 null 表示匿名会话）。
     */
    private RuntimeContext sessionContext(String sessionId) {
        return RuntimeContext.builder().sessionId(sessionId).build();
    }

    /**
     * 发送聊天消息并获取流式响应.
     *
     * @param sessionId 会话 ID
     * @param message   用户消息
     * @return SSE 事件流
     */
    public Flux<Map<String, Object>> chat(String sessionId, String message) {
        ReActAgent agent = getOrCreateAgent(sessionId);

        Msg userMsg = Msg.builder()
                .name("User")
                .role(MsgRole.USER)
                .content(TextBlock.builder().text(message).build())
                .build();

        // 2.x：stream(Msg) 已移除，改用 stream(List<Msg>, StreamOptions, RuntimeContext)；
        // 通过 RuntimeContext 携带 sessionId 让 Agent 自动 load/save 该会话状态。
        Flux<Map<String, Object>> events =
                agent.stream(List.of(userMsg), StreamOptions.defaults(), sessionContext(sessionId))
                        .flatMap(this::convertEvent);
        return wrapWithCompletion(events, sessionId, agent);
    }

    /**
     * 处理审批决策.
     *
     * @param sessionId  会话 ID
     * @param toolCallId 工具调用 ID
     * @param approved   审批决策
     * @param approver   审批人
     * @param comment    审批意见
     * @return SSE 事件流
     */
    public Flux<Map<String, Object>> handleApproval(
            String sessionId,
            String toolCallId,
            boolean approved,
            String approver,
            String comment) {

        // 查找对应的审批请求
        Optional<io.agentscope.tutorial.chapter14.entity.ApprovalRequest> optRequest =
                approvalService.findBySessionAndToolCallId(sessionId, toolCallId);

        if (optRequest.isEmpty()) {
            log.warn("未找到审批请求: session={}, toolCallId={}", sessionId, toolCallId);
            return Flux.just(Map.of("type", "ERROR", "error", "审批请求不存在"));
        }

        io.agentscope.tutorial.chapter14.entity.ApprovalRequest request = optRequest.get();

        // 执行审批决策
        Optional<io.agentscope.tutorial.chapter14.entity.ApprovalRequest> approvedRequest =
                approvalService.approve(request.getId(), approved, approver, comment);

        if (approvedRequest.isEmpty()) {
            return Flux.just(Map.of("type", "ERROR", "error", "审批失败"));
        }

        // 根据审批结果注入工具结果
        ReActAgent agent = getOrCreateAgent(sessionId);
        io.agentscope.core.message.ToolResultBlock result;

        if (approved) {
            // 批准：注入成功结果，工具将被执行
            result = io.agentscope.core.message.ToolResultBlock.of(
                    toolCallId,
                    request.getToolName(),
                    TextBlock.builder().text("审批通过，工具将执行").build());
        } else {
            // 拒绝：注入失败结果
            result = io.agentscope.core.message.ToolResultBlock.of(
                    toolCallId,
                    request.getToolName(),
                    TextBlock.builder().text("审批被拒绝: " + (comment != null ? comment : "未提供原因")).build());
        }

        Msg responseMsg = Msg.builder().role(MsgRole.TOOL).content(result).build();

        Flux<Map<String, Object>> events =
                agent.stream(List.of(responseMsg), StreamOptions.defaults(), sessionContext(sessionId))
                        .flatMap(this::convertEvent);
        return wrapWithCompletion(events, sessionId, agent);
    }

    /**
     * 获取或创建代理实例.
     */
    private ReActAgent getOrCreateAgent(String sessionId) {
        ReActAgent existing = runningAgents.get(sessionId);
        if (existing != null) {
            return existing;
        }

        // 2.x：不再手动 load 会话状态。Agent 已配置 stateStore + defaultSessionId，
        // 每次调用会依据 RuntimeContext 携带的 sessionId 自动从 stateStore 恢复历史。
        // 这里仅登记"运行中的代理"，用于后续的中断控制。
        ReActAgent agent = prototypeAgent;
        runningAgents.put(sessionId, agent);

        log.info("创建新代理实例: session={}", sessionId);
        return agent;
    }

    /**
     * 将事件流包装为包含完成标记的流.
     */
    private Flux<Map<String, Object>> wrapWithCompletion(
            Flux<Map<String, Object>> events, String sessionId, ReActAgent agent) {
        Map<String, Object> completeEvent = new HashMap<>();
        completeEvent.put("type", "COMPLETE");
        return Flux.concat(
                events,
                Flux.just(completeEvent))
                .doOnError(error ->
                        log.error("代理执行出错: session={}, error={}", sessionId, error.getMessage()))
                .doFinally(signal -> {
                    runningAgents.remove(sessionId);
                    // 2.x：无需手动 saveTo。每次 call 结束后，Agent 已按 sessionId 将
                    // AgentState 自动持久化到 stateStore。
                    log.info("会话结束: session={}", sessionId);
                });
    }

    /**
     * 将 Agent Event 转换为 SSE 友好的 Map.
     */
    private Flux<Map<String, Object>> convertEvent(Event event) {
        List<Map<String, Object>> events = new ArrayList<>();
        Msg msg = event.getMessage();

        switch (event.getType()) {
            case REASONING -> {
                if (event.isLast() && msg.hasContentBlocks(io.agentscope.core.message.ToolUseBlock.class)) {
                    // 推理结束且有工具调用
                    List<io.agentscope.core.message.ToolUseBlock> toolCalls =
                            msg.getContentBlocks(io.agentscope.core.message.ToolUseBlock.class);
                    for (io.agentscope.core.message.ToolUseBlock tool : toolCalls) {
                        Map<String, Object> eventMap = new HashMap<>();
                        if (TOOLS_REQUIRING_APPROVAL.contains(tool.getName())) {
                            // 需要审批的工具调用
                            eventMap.put("type", "PENDING_APPROVAL");
                            eventMap.put("toolCallId", tool.getId());
                            eventMap.put("toolName", tool.getName());
                            eventMap.put("toolInput", convertInput(tool.getInput()));
                            eventMap.put("sessionId", "pending");
                        } else {
                            // 普通工具调用
                            eventMap.put("type", "TOOL_USE");
                            eventMap.put("toolId", tool.getId());
                            eventMap.put("toolName", tool.getName());
                            eventMap.put("toolInput", convertInput(tool.getInput()));
                        }
                        events.add(eventMap);
                    }
                } else {
                    // 推理过程（流式文本）
                    String text = extractText(msg);
                    if (text != null && !text.isBlank()) {
                        Map<String, Object> eventMap = new HashMap<>();
                        eventMap.put("type", "REASONING");
                        eventMap.put("content", text);
                        eventMap.put("incremental", !event.isLast());
                        events.add(eventMap);
                    }
                }
            }
            case TOOL_RESULT -> {
                for (io.agentscope.core.message.ToolResultBlock result : msg.getContentBlocks(io.agentscope.core.message.ToolResultBlock.class)) {
                    Map<String, Object> eventMap = new HashMap<>();
                    eventMap.put("type", "TOOL_RESULT");
                    eventMap.put("toolId", result.getId());
                    eventMap.put("toolName", result.getName());
                    eventMap.put("toolResult", extractToolResultText(result));
                    events.add(eventMap);
                }
            }
            case AGENT_RESULT -> {
                String text = extractText(msg);
                if (text != null && !text.isBlank()) {
                    Map<String, Object> eventMap = new HashMap<>();
                    eventMap.put("type", "FINAL");
                    eventMap.put("content", text);
                    events.add(eventMap);
                }
            }
            default -> {
                // HINT, SUMMARY 等其他事件类型暂不处理
            }
        }

        return Flux.fromIterable(events);
    }

    /** 提取文本内容 */
    private String extractText(Msg msg) {
        List<TextBlock> blocks = msg.getContentBlocks(TextBlock.class);
        if (blocks.isEmpty()) {
            return null;
        }
        return blocks.stream().map(TextBlock::getText).collect(Collectors.joining());
    }

    /** 提取工具执行结果文本 */
    private String extractToolResultText(io.agentscope.core.message.ToolResultBlock result) {
        List<io.agentscope.core.message.ContentBlock> outputs = result.getOutput();
        if (outputs == null || outputs.isEmpty()) {
            return "(empty)";
        }
        return outputs.stream()
                .filter(b -> b instanceof TextBlock)
                .map(b -> ((TextBlock) b).getText())
                .collect(Collectors.joining("\n"));
    }

    /** 转换工具输入参数 */
    @SuppressWarnings("unchecked")
    private Map<String, Object> convertInput(Object input) {
        if (input == null) {
            return Map.of();
        }
        if (input instanceof Map) {
            return (Map<String, Object>) input;
        }
        try {
            return objectMapper.convertValue(input, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Map.of("value", input.toString());
        }
    }

    /**
     * 中断指定会话的代理执行.
     */
    public void interrupt(String sessionId) {
        ReActAgent agent = runningAgents.get(sessionId);
        if (agent != null) {
            // 2.x：按 sessionId 精确中断该会话的在途调用。
            agent.interrupt(sessionContext(sessionId));
            log.info("代理已中断: session={}", sessionId);
        }
    }

    /**
     * 清空会话状态.
     */
    public void clearSession(String sessionId) {
        runningAgents.remove(sessionId);
        // 2.x：用 AgentStateStore 按 (userId, sessionId) 删除持久化的会话状态；
        // userId 传 null 表示匿名会话（与 sessionContext 中一致）。
        stateStore.delete(null, sessionId);
        approvalService.deleteBySessionId(sessionId);
        log.info("会话已清空: session={}", sessionId);
    }
}