package io.agentscope.tutorial.chapter14.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.formatter.dashscope.DashScopeChatFormatter;
import io.agentscope.core.hook.Hook;
import io.agentscope.core.hook.recorder.JsonlTraceExporter;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.session.InMemorySession;
import io.agentscope.core.session.Session;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.tutorial.chapter14.hook.ApprovalHook;
import io.agentscope.tutorial.chapter14.repository.ApprovalRepository;
import io.agentscope.tutorial.chapter14.service.ApprovalService;
import io.agentscope.tutorial.chapter14.service.WorkflowService;
import io.agentscope.tutorial.chapter14.service.ChatService;
import io.agentscope.tutorial.chapter14.tool.SendEmailTool;
import io.agentscope.tutorial.chapter14.tool.DeleteDataTool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

/**
 * HITL 审批系统配置类.
 *
 * <p>配置所有必要的 Bean，包括：
 * <ul>
 *   <li>AgentScope 模型和 Agent 实例</li>
 *   <li>审批工作流相关服务</li>
 *   <li>工具包配置</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 */
@Configuration
public class HITLConfig {

    /** 需要审批的工具名称集合 */
    public static final Set<String> TOOLS_REQUIRING_APPROVAL =
            Set.of(SendEmailTool.TOOL_NAME, DeleteDataTool.TOOL_NAME);

    /** 系统提示词 */
    private static final String SYS_PROMPT = """
            你是一个企业助手，可以帮助用户完成日常办公任务。

            你可以使用以下工具：
            - query_data: 查询数据记录（不需要审批）
            - send_email: 发送电子邮件（需要审批）
            - delete_data: 删除数据记录（需要审批）

            重要规则：
            - 发送邮件前，必须确认收件人邮箱地址格式正确
            - 删除数据前，必须说明删除原因和影响范围
            - 如果用户没有提供足够的信息，不要随意猜测，主动向用户询问
            - 涉及敏感操作（邮件、删除）时，系统会自动暂停等待审批，无需额外提醒用户
            """;

    @Value("${agentscope.api-key}")
    private String apiKey;

    @Value("${agentscope.model.default:qwen-plus}")
    private String defaultModel;

    @Value("${agentscope.model.stream:true}")
    private boolean streamEnabled;

    /**
     * 配置 DashScope 聊天模型.
     */
    @Bean
    public DashScopeChatModel chatModel() {
        return DashScopeChatModel.builder()
                .apiKey(apiKey)
                .modelName(defaultModel)
                .stream(streamEnabled)
                .enableThinking(false)
                .formatter(new DashScopeChatFormatter())
                .build();
    }

    /**
     * 配置工具包.
     */
    @Bean
    public Toolkit toolkit() {
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(new SendEmailTool());
        toolkit.registerTool(new DeleteDataTool());
        return toolkit;
    }

    /**
     * 配置 ObjectMapper.
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    /**
     * 配置内存会话存储.
     */
    @Bean
    public Session session() {
        return new InMemorySession();
    }

    /**
     * 配置审批 Hook.
     *
     * @param approvalService 审批服务
     * @return ApprovalHook 实例
     */
    @Bean
    public ApprovalHook approvalHook(ApprovalService approvalService) {
        return new ApprovalHook(TOOLS_REQUIRING_APPROVAL, approvalService);
    }

    /**
     * 配置 ReActAgent 实例.
     */
    @Bean
    public ReActAgent reactAgent(
            DashScopeChatModel chatModel,
            Toolkit toolkit,
            Session session,
            ApprovalHook approvalHook) {

        return ReActAgent.builder()
                .name("EnterpriseAssistant")
                .sysPrompt(SYS_PROMPT)
                .model(chatModel)
                .toolkit(toolkit)
                .memory(new InMemoryMemory())
                .hook(approvalHook)
                .build();
    }

    /**
     * 配置 ChatService.
     */
    @Bean
    public ChatService chatService(
            ReActAgent reactAgent,
            Session session,
            ApprovalService approvalService,
            ObjectMapper objectMapper) {
        return new ChatService(reactAgent, session, approvalService, objectMapper);
    }
}