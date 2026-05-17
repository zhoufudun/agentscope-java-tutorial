package io.agentscope.tutorial.chapter14.tool;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;

/**
 * 发送邮件工具（需要审批）.
 *
 * <p>此工具涉及敏感操作，在执行前必须经过人工审批.
 * 审批通过后，工具将输出邮件发送成功的模拟信息.
 *
 * @author AgentScope Tutorial
 */
public class SendEmailTool {

    /** 工具名称，需要在 ApprovalHook 中配置为需要审批的工具 */
    public static final String TOOL_NAME = "send_email";

    /**
     * 发送电子邮件.
     * 注意：此方法在审批通过前不会执行，实际项目中应集成真实的邮件发送服务.
     *
     * @param to      收件人邮箱地址
     * @param subject 邮件主题
     * @param body    邮件正文（支持多行文本）
     * @param cc      抄送人（可选，多个邮箱用逗号分隔）
     * @return 邮件发送结果
     */
    @Tool(
            name = TOOL_NAME,
            description = "发送电子邮件. 此操作需要审批通过后才能执行.\n"
                    + "适用于：发送通知、报告、提醒等正式邮件.\n"
                    + "注意：仅支持公司域名的收件人邮箱（@company.com 结尾）。")
    public String sendEmail(
            @ToolParam(name = "to", description = "收件人邮箱地址，例如：user@company.com", required = true) String to,
            @ToolParam(name = "subject", description = "邮件主题，最大 200 字符", required = true) String subject,
            @ToolParam(name = "body", description = "邮件正文内容，支持多行文本", required = true) String body,
            @ToolParam(name = "cc", description = "抄送人邮箱地址，多个用逗号分隔（可选）", required = false) String cc) {

        // 模拟邮件发送逻辑
        StringBuilder result = new StringBuilder();
        result.append("邮件已发送成功！\n");
        result.append("━━━━━━━━━━━━━━━\n");
        result.append("收件人: ").append(to).append("\n");
        if (cc != null && !cc.isBlank()) {
            result.append("抄送: ").append(cc).append("\n");
        }
        result.append("主题: ").append(subject).append("\n");
        result.append("━━━━━━━━━━━━━━━\n");
        result.append("正文:\n").append(body);

        return result.toString();
    }
}