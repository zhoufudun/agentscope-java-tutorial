package io.agentscope.tutorial.chapter05.tool;

import io.agentscope.core.tool.file.ReadFileTool;
import io.agentscope.core.tool.file.WriteFileTool;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import reactor.core.publisher.Mono;

/**
 * 内置工具示例
 *
 * <p>展示如何使用 AgentScope Java 内置的工具。
 * 内置工具包括：
 * <ul>
 *   <li>ReadFileTool: 文件读取工具（view_text_file, list_directory）</li>
 *   <li>WriteFileTool: 文件写入工具（write_text_file, insert_text_file）</li>
 * </ul>
 *
 * <p>这些工具位于 io.agentscope.core.tool.file 包下。
 *
 * @author AgentScope Tutorial
 * @since 1.0
 */
public class BuiltinTools {

    // 内置工具实例（可在外部配置工作目录）
    private final ReadFileTool readFileTool;
    private final WriteFileTool writeFileTool;

    /**
     * 创建内置工具实例
     *
     * @param workDir 工作目录（用于限制文件访问范围）
     */
    public BuiltinTools(String workDir) {
        this.readFileTool = new ReadFileTool(workDir);
        this.writeFileTool = new WriteFileTool(workDir);
    }

    /**
     * 创建内置工具实例（无限制）
     */
    public BuiltinTools() {
        this(null);
    }

    // ==================== 文件操作工具 ====================

    /**
     * 读取文本文件
     *
     * <p>使用内置的 ReadFileTool 读取文件内容。
     * 支持按行范围读取，也支持负索引（从尾部计算）。
     *
     * <p>使用示例：
     * <ul>
     *   <li>view_text_file("path/to/file.txt", "1,100") - 读取第 1-100 行</li>
     *   <li>view_text_file("path/to/file.txt", "-100,-1") - 读取最后 100 行</li>
     *   <li>view_text_file("path/to/file.txt", "50,50") - 读取第 50 行</li>
     * </ul>
     *
     * @param filePath 文件路径
     * @param ranges 行范围（格式：start,end 或负数，如 "1,100" 或 "-100,-1"）
     * @return 文件内容
     */
    @Tool(
        name = "view_text_file",
        description = "读取文本文件的内容。支持按行范围读取，也支持负索引（从尾部计算行数）。例如：'1,100' 读取第1-100行，'-100,-1' 读取最后100行。"
    )
    public Mono<String> viewTextFile(
        @ToolParam(name = "file_path", description = "文件路径", required = true) String filePath,
        @ToolParam(
            name = "ranges",
            description = "行范围（格式：start,end，如 '1,100' 或 '-100,-1'），省略则读取整个文件",
            required = false
        ) String ranges
    ) {
        return readFileTool.viewTextFile(filePath, ranges)
            .map(result -> result.getOutput().isEmpty() ? "无内容" :
                result.getOutput().get(0).toString());
    }

    /**
     * 列出目录内容
     *
     * @param dirPath 目录路径
     * @return 目录内容列表
     */
    @Tool(
        name = "list_directory",
        description = "列出指定目录的内容，包括文件和子目录。"
    )
    public Mono<String> listDirectory(
        @ToolParam(name = "dir_path", description = "目录路径", required = true) String dirPath
    ) {
        return readFileTool.listDirectory(dirPath)
            .map(result -> result.getOutput().isEmpty() ? "目录为空或不存在" :
                result.getOutput().get(0).toString());
    }

    /**
     * 写入文本文件
     *
     * <p>使用内置的 WriteFileTool 写入文件内容。
     * 如果文件已存在，将覆盖原内容；如果指定了范围，则替换指定范围的行。
     *
     * @param filePath 文件路径
     * @param content 要写入的内容
     * @param ranges 行范围（可选，格式：start,end）
     * @return 操作结果
     */
    @Tool(
        name = "write_text_file",
        description = "向指定路径写入文本内容。如果文件已存在，将覆盖原内容。如果指定了 ranges，则替换指定范围的行。"
    )
    public Mono<String> writeTextFile(
        @ToolParam(name = "file_path", description = "文件路径", required = true) String filePath,
        @ToolParam(name = "content", description = "要写入的内容", required = true) String content,
        @ToolParam(
            name = "ranges",
            description = "行范围（可选，格式：start,end，如 '1,5'）",
            required = false
        ) String ranges
    ) {
        return writeFileTool.writeTextFile(filePath, content, ranges)
            .map(result -> result.getOutput().isEmpty() ? "写入成功" :
                result.getOutput().get(0).toString());
    }

    /**
     * 在指定行插入内容
     *
     * @param filePath 文件路径
     * @param content 要插入的内容
     * @param lineNumber 行号（从 1 开始）
     * @return 操作结果
     */
    @Tool(
        name = "insert_text_file",
        description = "在文件的指定行号位置插入内容。被插入位置后的内容会下移。"
    )
    public Mono<String> insertTextFile(
        @ToolParam(name = "file_path", description = "文件路径", required = true) String filePath,
        @ToolParam(name = "content", description = "要插入的内容", required = true) String content,
        @ToolParam(name = "line_number", description = "插入位置的行号（从 1 开始）", required = true) Integer lineNumber
    ) {
        return writeFileTool.insertTextFile(filePath, content, lineNumber)
            .map(result -> result.getOutput().isEmpty() ? "插入成功" :
                result.getOutput().get(0).toString());
    }

    // ==================== 工具注册演示 ====================

    /**
     * 展示如何注册内置工具
     *
     * <p>此方法本身不是工具，仅作为文档示例。
     *
     * <p>注册内置工具的示例代码：
     * <pre>{@code
     * // 创建 Toolkit
     * Toolkit toolkit = new Toolkit();
     *
     * // 创建内置工具（限制工作目录）
     * ReadFileTool readTool = new ReadFileTool("/data/workspace");
     * WriteFileTool writeTool = new WriteFileTool("/data/workspace");
     *
     * // 注册内置工具
     * toolkit.registerAgentTool(readTool);
     * toolkit.registerAgentTool(writeTool);
     *
     * // 或者使用 BuiltinTools 辅助类
     * toolkit.registerTool(new BuiltinTools("/data/workspace"));
     * }</pre>
     *
     * @return 工具注册说明
     */
    public String showRegistrationGuide() {
        return """
            内置工具注册指南：

            1. ReadFileTool（文件读取工具）
               - 位置：io.agentscope.core.tool.file.ReadFileTool
               - 功能：view_text_file（按行范围读取）、list_directory（列出目录）
               - 安全：支持路径遍历攻击防护，支持基础目录限制

            2. WriteFileTool（文件写入工具）
               - 位置：io.agentscope.core.tool.file.WriteFileTool
               - 功能：write_text_file（写入）、insert_text_file（插入）
               - 安全：同 ReadFileTool

            3. 注册示例：
               ReadFileTool readTool = new ReadFileTool("/data/workspace");
               toolkit.registerAgentTool(readTool);

            注意：请根据实际需求配置工作目录，避免访问敏感文件。
            """;
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取内置工具的 Schema 信息
     *
     * @return Schema 描述
     */
    public String getBuiltinToolSchemas() {
        StringBuilder result = new StringBuilder();
        result.append("内置工具 Schema 信息：\n\n");

        // ReadFileTool Schema
        result.append("1. ReadFileTool\n");
        result.append("   - view_text_file: 读取文本文件\n");
        result.append("   - list_directory: 列出目录内容\n");

        // WriteFileTool Schema
        result.append("\n2. WriteFileTool\n");
        result.append("   - write_text_file: 写入文件\n");
        result.append("   - insert_text_file: 插入到文件\n");

        return result.toString();
    }
}