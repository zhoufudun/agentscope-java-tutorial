package io.agentscope.tutorial.chapter14.hook;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.core.hook.Hook;
import io.agentscope.core.hook.HookEvent;
import io.agentscope.core.hook.PostReasoningEvent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.tutorial.chapter14.entity.ApprovalRequest;
import io.agentscope.tutorial.chapter14.service.ApprovalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 审批钩子（Approval Hook）.
 *
 * <p>此 Hook 在推理阶段完成后检查是否有需要审批的工具调用。
 * 如果有，它会停止代理执行，创建审批请求记录到数据库，
 * 并等待外部审批后恢复执行。
 *
 * <p>工作流程：
 * <ol>
 *   <li>推理完成 → PostReasoningEvent</li>
 *   <li>检查 ToolUseBlock 中是否有需要审批的工具</li>
 *   <li>有 → 调用 ApprovalService 保存审批请求，stopAgent()</li>
 *   <li>无 → 正常继续执行</li>
 * </ol>
 *
 * @author AgentScope Tutorial
 * @see ApprovalService
 * @see PostReasoningEvent
 */
public class ApprovalHook implements Hook {

    private static final Logger log = LoggerFactory.getLogger(ApprovalHook.class);

    /** 需要审批的工具名称集合 */
    private final Set<String> toolsRequiringApproval;

    /** 审批服务 */
    private final ApprovalService approvalService;

    /** JSON 序列化工具 */
    private final ObjectMapper objectMapper;

    /** 会话 ID 映射表（toolCallId -> sessionId） */
    private final ConcurrentHashMap<String, String> toolCallSessionMap = new ConcurrentHashMap<>();

    /** 计数器，用于生成会话 ID */
    private final AtomicLong sessionCounter = new AtomicLong(0);

    /**
     * 构造函数，注入需要审批的工具集合.
     *
     * @param toolsRequiringApproval 需要审批的工具名称集合
     * @param approvalService        审批服务
     */
    public ApprovalHook(Set<String> toolsRequiringApproval, ApprovalService approvalService) {
        this.toolsRequiringApproval = Set.copyOf(toolsRequiringApproval);
        this.approvalService = approvalService;
        this.objectMapper = new ObjectMapper();
        log.info("ApprovalHook 初始化，需要审批的工具: {}", toolsRequiringApproval);
    }

    /**
     * 处理钩子事件.
     *
     * @param event 钩子事件
     * @param <T>   事件具体类型
     * @return 包含潜在修改后事件的无状态 Mono
     */
    @Override
    public <T extends HookEvent> Mono<T> onEvent(T event) {
        // 只处理 PostReasoningEvent
        if (!(event instanceof PostReasoningEvent post)) {
            return Mono.just(event);
        }

        Msg reasoning = post.getReasoningMessage();
        if (reasoning == null) {
            return Mono.just(event);
        }

        // 获取推理结果中的所有工具调用
        List<ToolUseBlock> toolCalls = reasoning.getContentBlocks(ToolUseBlock.class);
        if (toolCalls.isEmpty()) {
            return Mono.just(event);
        }

        // 检查是否有需要审批的工具
        List<ToolUseBlock> pendingApprovals = toolCalls.stream()
                .filter(t -> toolsRequiringApproval.contains(t.getName()))
                .toList();

        if (!pendingApprovals.isEmpty()) {
            log.info("检测到 {} 个需要审批的工具调用，停止代理执行", pendingApprovals.size());

            // 为每个需要审批的工具创建审批请求
            // 使用时间戳 + 计数器生成会话 ID
            String sessionId = "session-" + System.currentTimeMillis() + "-" + sessionCounter.incrementAndGet();

            for (ToolUseBlock tool : pendingApprovals) {
                try {
                    String toolInputJson = objectMapper.writeValueAsString(tool.getInput());
                    ApprovalRequest request = new ApprovalRequest(
                            sessionId, tool.getId(), tool.getName(), toolInputJson);
                    request.setAgentName(post.getAgent().getName());
                    approvalService.createApprovalRequest(request);

                    // 保存 toolCallId 到 sessionId 的映射，用于后续恢复
                    toolCallSessionMap.put(tool.getId(), sessionId);

                    log.info("创建审批请求: tool={}, toolCallId={}, sessionId={}",
                            tool.getName(), tool.getId(), sessionId);
                } catch (JsonProcessingException e) {
                    log.error("序列化工具参数失败: {}", e.getMessage());
                }
            }

            // 停止代理执行，等待审批
            post.stopAgent();
        }

        return Mono.just(event);
    }

    /**
     * 获取工具调用对应的会话 ID.
     *
     * @param toolCallId 工具调用 ID
     * @return 会话 ID，如果没有找到返回 null
     */
    public String getSessionIdByToolCallId(String toolCallId) {
        return toolCallSessionMap.get(toolCallId);
    }

    /**
     * 清除会话映射.
     *
     * @param toolCallId 工具调用 ID
     */
    public void removeMapping(String toolCallId) {
        toolCallSessionMap.remove(toolCallId);
    }

    /**
     * 获取需要审批的工具集合.
     */
    public Set<String> getToolsRequiringApproval() {
        return toolsRequiringApproval;
    }

    /**
     * 添加需要审批的工具.
     *
     * @param toolName 工具名称
     */
    public void addTool(String toolName) {
        log.info("添加需要审批的工具: {}", toolName);
    }

    /**
     * 移除需要审批的工具.
     *
     * @param toolName 工具名称
     */
    public void removeTool(String toolName) {
        log.info("移除需要审批的工具: {}", toolName);
    }
}