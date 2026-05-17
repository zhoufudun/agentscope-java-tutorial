package io.agentscope.tutorial.chapter14.tool;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;

/**
 * 删除数据工具（需要审批）.
 *
 * <p>此工具涉及数据删除的不可逆操作，必须经过人工审批才能执行.
 *
 * @author AgentScope Tutorial
 */
public class DeleteDataTool {

    /** 工具名称，需要在 ApprovalHook 中配置为需要审批的工具 */
    public static final String TOOL_NAME = "delete_data";

    /**
     * 删除指定数据记录.
     * 注意：此操作不可逆，必须经过审批确认.
     *
     * @param table     数据表名
     * @param recordId  要删除的记录 ID
     * @param reason    删除原因
     * @param confirm   必须为 true 才能执行删除（防止误操作）
     * @return 删除操作的结果
     */
    @Tool(
            name = TOOL_NAME,
            description = "删除指定的数据记录。此操作不可逆且需要审批才能执行。\n"
                    + "适用场景：清理过期数据、删除测试记录、移除无效数据等。\n"
                    + "警告：删除后数据无法恢复，请确保已备份重要数据！")
    public String deleteData(
            @ToolParam(name = "table", description = "数据表名，例如：users、orders、logs", required = true) String table,
            @ToolParam(name = "record_id", description = "要删除的记录 ID", required = true) String recordId,
            @ToolParam(name = "reason", description = "删除原因说明", required = true) String reason,
            @ToolParam(name = "confirm", description = "确认删除操作，必须为 true", required = true) Boolean confirm) {

        if (!Boolean.TRUE.equals(confirm)) {
            return "错误：删除操作未确认。请将 confirm 参数设为 true 以确认删除。";
        }

        // 模拟删除操作（实际项目中应调用数据库或存储服务）
        return String.format(
                "数据删除成功！\n"
                        + "━━━━━━━━━━━━━━━\n"
                        + "表: %s\n"
                        + "记录ID: %s\n"
                        + "原因: %s\n"
                        + "━━━━━━━━━━━━━━━\n"
                        + "⚠️ 注意：此操作不可逆，数据已永久删除。",
                table, recordId, reason);
    }
}