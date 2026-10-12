package io.agentscope.tutorial.chapter15.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 代码执行结果模型.
 *
 * <h2>概述</h2>
 * <p>本类封装了代码在 Harness 沙箱中执行后的所有结果信息，
 * 包括执行状态、输出内容、错误信息和性能指标。
 *
 * <h2>结果状态</h2>
 * <ul>
 *   <li><b>成功</b>：success=true, exitCode=0</li>
 *   <li><b>失败</b>：success=false, exitCode!=0, 可能有 errorMessage</li>
 *   <li><b>超时</b>：success=false, exitCode=-2, 特定的超时错误消息</li>
 * </ul>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 检查执行是否成功
 * if (result.isSuccess()) {
 *     System.out.println("输出: " + result.getStdout());
 * } else {
 *     System.err.println("错误: " + result.getErrorMessage());
 * }
 * }</pre>
 *
 * <h2>JSON 序列化</h2>
 * <p>使用 @JsonInclude(JsonInclude.Include.NON_NULL) 注解，
 * 序列化时会忽略 null 值的字段，使 JSON 响应更简洁。
 *
 * @author AgentScope Tutorial
 * @see ExecutionRequest
 */
@Data  // Lombok: getter、setter、toString、equals、hashCode
@Builder  // Lombok: Builder 模式
@NoArgsConstructor  // 无参构造函数
@AllArgsConstructor  // 全参构造函数
@JsonInclude(JsonInclude.Include.NON_NULL)  // JSON 序列化时忽略 null 字段
public class ExecutionResult {

    /**
     * 执行是否成功.
     *
     * <p>通常根据 exitCode 判断：
     * <ul>
     *   <li>exitCode == 0: 成功</li>
     *   <li>exitCode != 0: 失败</li>
     * </ul>
     */
    private boolean success;

    /**
     * 进程退出码.
     *
     * <p>常见退出码含义：
     * <ul>
     *   <li>0: 正常退出</li>
     *   <li>1: 一般错误</li>
     *   <li>-1: 执行前失败（如安全检查未通过）</li>
     *   <li>-2: 超时终止</li>
     *   <li>其他: 程序特定的错误码</li>
     * </ul>
     */
    private Integer exitCode;

    /**
     * 标准输出内容.
     *
     * <p>捕获的程序标准输出（stdout）。
     * 对于打印语句、正常输出等都会在这里。
     */
    private String stdout;

    /**
     * 标准错误内容.
     *
     * <p>捕获的程序标准错误（stderr）。
     * 编译错误、运行时异常等通常输出到这里。
     */
    private String stderr;

    /**
     * 执行耗时（毫秒）.
     *
     * <p>从开始执行到执行完成的总时间，
     * 包括沙箱准备、代码编译（如果适用）和运行时间。
     */
    @Builder.Default
    private Long executionTimeMs = 0L;

    /**
     * 工作空间标识符.
     *
     * <p>代码在哪个工作空间中执行。
     */
    @Builder.Default
    private String workspace = "default";

    /**
     * 沙箱实例 ID.
     *
     * <p>执行代码的沙箱唯一标识符。
     * 可用于后续查询沙箱状态或销毁沙箱。
     */
    private String sandboxId;

    /**
     * 错误消息.
     *
     * <p>当执行失败时，包含错误描述。
     * 可能是安全检查失败、超时、或运行时错误等。
     */
    private String errorMessage;

    /**
     * 输出是否被截断.
     *
     * <p>当输出超过配置的最大大小时，
     * 输出会被截断，此字段设置为 true。
     */
    private Boolean truncated;

    /**
     * 执行时间戳.
     *
     * <p>记录代码执行完成的时间。
     */
    @Builder.Default
    private LocalDateTime executedAt = LocalDateTime.now();

    /**
     * 创建成功的执行结果.
     *
     * <p>当代码成功执行时使用此工厂方法创建结果。
     * success 字段根据 exitCode 是否为 0 自动设置。
     *
     * @param sandboxId 沙箱实例 ID
     * @param stdout 标准输出
     * @param stderr 标准错误
     * @param exitCode 退出码
     * @param executionTimeMs 执行时间（毫秒）
     * @param workspace 工作空间 ID
     * @return 执行结果对象
     */
    public static ExecutionResult success(String sandboxId, String stdout, String stderr,
                                          int exitCode, long executionTimeMs, String workspace) {
        return ExecutionResult.builder()
                .success(exitCode == 0)  // exitCode 为 0 才算成功
                .exitCode(exitCode)
                .stdout(stdout)
                .stderr(stderr)
                .executionTimeMs(executionTimeMs)
                .workspace(workspace)
                .sandboxId(sandboxId)
                .executedAt(LocalDateTime.now())
                .build();
    }

    /**
     * 创建失败的执行结果.
     *
     * <p>当执行失败时使用此工厂方法。
     * 用于安全检查失败、沙箱创建失败等情况。
     *
     * @param sandboxId 沙箱实例 ID（可能为 null）
     * @param errorMessage 错误消息
     * @param workspace 工作空间 ID
     * @param timeoutSeconds 请求的超时时间（可能为 null）
     * @return 失败的执行结果对象
     */
    public static ExecutionResult failure(String sandboxId, String errorMessage,
                                          String workspace, Integer timeoutSeconds) {
        return ExecutionResult.builder()
                .success(false)
                .exitCode(-1)  // -1 表示执行前失败
                .errorMessage(errorMessage)
                .workspace(workspace)
                .sandboxId(sandboxId)
                .executedAt(LocalDateTime.now())
                .build();
    }

    /**
     * 创建超时的执行结果.
     *
     * <p>当执行超过指定时间被强制终止时使用。
     *
     * @param sandboxId 沙箱实例 ID
     * @param workspace 工作空间 ID
     * @param timeoutSeconds 超时时间（秒）
     * @return 超时的执行结果对象
     */
    public static ExecutionResult timeout(String sandboxId, String workspace, int timeoutSeconds) {
        return ExecutionResult.builder()
                .success(false)
                .exitCode(-2)  // -2 表示超时
                .errorMessage("执行超时，超过 " + timeoutSeconds + " 秒")
                .workspace(workspace)
                .sandboxId(sandboxId)
                .executedAt(LocalDateTime.now())
                .build();
    }
}