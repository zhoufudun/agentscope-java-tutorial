package io.agentscope.tutorial.chapter15.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 代码执行请求模型.
 *
 * <h2>概述</h2>
 * <p>本类定义了向 Harness 沙箱提交代码执行请求时所需的所有参数。
 * 它封装了代码内容、语言类型、执行约束和工作空间配置。
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * ExecutionRequest request = ExecutionRequest.builder()
 *     .code("System.out.println(\"Hello, World!\");")
 *     .language("java")
 *     .timeoutSeconds(60)
 *     .workspace("my-workspace")
 *     .captureOutput(true)
 *     .build();
 * }</pre>
 *
 * <h2>支持的语言</h2>
 * <ul>
 *   <li><b>java</b> - Java 代码（需要 main 方法或可执行语句）</li>
 *   <li><b>python/python3</b> - Python 脚本</li>
 *   <li><b>javascript/js</b> - JavaScript 代码（Node.js 环境）</li>
 *   <li><b>shell/bash</b> - Shell 脚本</li>
 * </ul>
 *
 * <h2>验证规则</h2>
 * <ul>
 *   <li>code 不能为空（@NotBlank）</li>
 *   <li>timeoutSeconds 必须在 1-300 秒之间</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 * @see ExecutionResult
 */
@Data  // Lombok: 自动生成 getter、setter、toString、equals、hashCode
@Builder  // Lombok: 启用 Builder 模式
@NoArgsConstructor  // Lombok: 生成无参构造函数
@AllArgsConstructor  // Lombok: 生成全参构造函数
public class ExecutionRequest {

    /**
     * 要执行的代码内容.
     *
     * <p>代码内容取决于指定的语言：
     * <ul>
     *   <li>Java: 完整的方法或类定义</li>
     *   <li>Python: Python 脚本</li>
     *   <li>JavaScript: JavaScript 代码</li>
     *   <li>Shell: Shell 命令或脚本</li>
     * </ul>
     *
     * <p>验证：不能为空或仅包含空白字符。
     */
    @NotBlank(message = "代码不能为空")
    private String code;

    /**
     * 编程语言.
     *
     * <p>指定代码的编程语言，决定使用哪个解释器/编译器执行。
     * 默认为 "java"。
     *
     * <p>支持的值：java, python, python3, javascript, js, shell, bash
     */
    @Builder.Default
    private String language = "java";

    /**
     * 执行超时时间（秒）.
     *
     * <p>指定代码执行的最大允许时间。超过此时间，执行将被强制终止。
     * 这是防止无限循环和资源耗尽的重要安全措施。
     *
     * <p>约束：
     * <ul>
     *   <li>最小值：1 秒</li>
     *   <li>最大值：300 秒（5 分钟）</li>
     *   <li>默认值：60 秒</li>
     * </ul>
     *
     * <p>注意：实际超时可能被服务器配置的 maxTimeout 进一步限制。
     */
    @Min(value = 1, message = "超时时间至少为 1 秒")
    @Max(value = 300, message = "超时时间不能超过 300 秒")
    @Builder.Default
    private Integer timeoutSeconds = 60;

    /**
     * 工作空间标识符.
     *
     * <p>指定代码在哪个工作空间中执行。每个工作空间有独立的文件系统，
     * 实现多租户隔离。
     *
     * <p>默认值："default"
     *
     * <p>工作空间用途：
     * <ul>
     *   <li>隔离不同用户/会话的文件</li>
     *   <li>持久化执行中间结果</li>
     *   <li>共享同一会话中的多次执行结果</li>
     * </ul>
     */
    @Builder.Default
    private String workspace = "default";

    /**
     * 是否捕获输出.
     *
     * <p>控制是否捕获代码执行的标准输出（stdout）和标准错误（stderr）。
     *
     * <p>当设置为 true（默认）时：
     * <ul>
     *   <li>执行结果将包含 stdout 和 stderr 内容</li>
     *   <li>输出可能被截断以防止内存溢出</li>
     * </ul>
     *
     * <p>当设置为 false 时：
     * <ul>
     *   <li>只返回退出码和执行状态</li>
     *   <li>可以略微提高性能</li>
     * </ul>
     */
    @Builder.Default
    private Boolean captureOutput = true;
}