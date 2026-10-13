package io.agentscope.tutorial.chapter15.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Harness 配置属性类.
 *
 * <h2>概述</h2>
 * <p>本类定义了 Harness 安全执行环境的所有配置参数，包括沙箱设置和执行控制。
 * 通过 Spring Boot 的配置属性绑定机制，可以从 application.yml 或 application.properties
 * 文件中加载配置。
 *
 * <h2>配置层级</h2>
 * <pre>
 * harness:
 *   sandbox:
 *     defaultTimeout: 60        # 默认超时（秒）
 *     maxTimeout: 300           # 最大超时（秒）
 *     workspaceRoot: /workspace # 工作空间根目录
 *   execution:
 *     maxOutputSize: 1048576    # 最大输出大小（字节）
 *     enableSecurityChecks: true # 启用安全检查
 * </pre>
 *
 * <h2>安全特性</h2>
 * <ul>
 *   <li><b>命令白名单</b>：只允许预定义的安全命令执行</li>
 *   <li><b>危险模式黑名单</b>：阻止包含危险模式的命令</li>
 *   <li><b>超时限制</b>：防止代码无限执行</li>
 *   <li><b>输出大小限制</b>：防止内存溢出攻击</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 * @see CodeExecutionService
 */
@Data
@Configuration  // 标记为 Spring 配置类
@ConfigurationProperties(prefix = "harness")  // 绑定以 "harness" 为前缀的配置属性
public class HarnessConfig {

    /**
     * 沙箱配置.
     * 包含沙箱运行时的各项参数设置。
     */
    private SandboxConfig sandbox = new SandboxConfig();

    /**
     * 执行配置.
     * 包含代码/命令执行的安全控制参数。
     */
    private ExecutionConfig execution = new ExecutionConfig();

    /**
     * 沙箱配置内部类.
     *
     * <p>定义沙箱的运行参数，控制资源使用和隔离级别。
     */
    @Data
    public static class SandboxConfig {
        /**
         * 默认执行超时时间（秒）.
         * 当请求未指定超时时使用此值。
         */
        private int defaultTimeout = 60;

        /**
         * 最大允许超时时间（秒）.
         * 即使请求指定更大的值，也会被限制为此值。
         * 这是防止拒绝服务攻击的重要安全措施。
         */
        private int maxTimeout = 300;

        /**
         * 工作空间根目录.
         * 所有工作空间文件都存储在此目录下。
         */
        private String workspaceRoot = "/workspace";

        /**
         * 最大并发沙箱数量.
         * 限制同时运行的沙箱数量，防止资源耗尽。
         */
        private int maxConcurrentSandboxes = 10;

        /**
         * 工作空间空闲超时（毫秒）.
         * 空闲超过此时间的工作空间将被自动清理。
         * 默认 1 小时 = 3600000 毫秒。
         */
        private long workspaceIdleTimeoutMs = 3600000;
    }

    /**
     * 执行配置内部类.
     *
     * <p>定义代码和命令执行的安全策略。
     */
    @Data
    public static class ExecutionConfig {
        /**
         * 最大输出大小（字节）.
         * 超过此大小的输出将被截断，防止内存溢出。
         * 默认 1MB = 1024 * 1024 字节。
         */
        private long maxOutputSize = 1024 * 1024;

        /**
         * 允许执行的命令白名单.
         * 只有以这些命令开头的 shell 命令才能执行。
         * 这是沙箱安全的核心机制之一。
         */
        private List<String> allowedCommands = List.of(
                "ls",      // 列出目录内容
                "cat",     // 查看文件内容
                "grep",    // 搜索文本模式
                "find",    // 查找文件
                "echo",    // 输出文本
                "mkdir",   // 创建目录
                "touch",   // 创建空文件
                "rm",      // 删除文件（单个文件，不允许 rm -rf /）
                "cp",      // 复制文件
                "mv"       // 移动文件
        );

        /**
         * 是否启用安全检查.
         * 设置为 false 将禁用所有安全检查（仅用于测试）。
         */
        private boolean enableSecurityChecks = true;

        /**
         * 危险模式黑名单.
         * 包含这些模式的命令将被拒绝执行，
         * 即使它们以允许的命令开头。
         */
        private List<String> blockedPatterns = List.of(
                "rm -rf /",       // 删除根目录 - 灾难性操作
                "DROP DATABASE",  // SQL 注入攻击
                "DELETE FROM",    // SQL 删除操作
                "--os-shell"      // sqlmap 等工具的系统 shell 注入
        );
    }

    /**
     * 获取实际有效的超时时间.
     *
     * <p>此方法确保超时时间不会超过配置的最大值，
     * 防止用户请求过长的超时时间导致资源占用。
     *
     * <h3>计算逻辑</h3>
     * <ol>
     *   <li>如果请求未指定超时，使用默认超时</li>
     *   <li>如果请求的超时大于最大值，使用最大值</li>
     *   <li>否则使用请求的超时值</li>
     * </ol>
     *
     * @param requestedTimeout 请求的超时时间（秒），可以为 null
     * @return 实际使用的超时时间（秒）
     */
    public int getEffectiveTimeout(Integer requestedTimeout) {
        if (requestedTimeout == null) {
            // 未指定超时，使用默认值
            return sandbox.getDefaultTimeout();
        }
        // 取请求值和最大值中的较小者
        return Math.min(requestedTimeout, sandbox.getMaxTimeout());
    }

    /**
     * 检查命令是否在允许列表中.
     *
     * <p>此方法实现命令白名单检查，只允许执行预定义的安全命令。
     *
     * <h3>检查逻辑</h3>
     * <ol>
     *   <li>如果安全检查被禁用，直接返回 true</li>
     *   <li>检查命令是否以任何允许的命令开头</li>
     * </ol>
     *
     * @param command 要检查的命令
     * @return true 如果命令被允许，false 如果命令被拒绝
     */
    public boolean isCommandAllowed(String command) {
        // 如果禁用了安全检查，允许所有命令
        if (!execution.isEnableSecurityChecks()) {
            return true;
        }
        // 检查命令是否以允许列表中的任何命令开头
        return execution.getAllowedCommands().stream()
                .anyMatch(cmd -> command.trim().startsWith(cmd));
    }

    /**
     * 检查命令是否包含被阻止的危险模式.
     *
     * <p>此方法实现危险模式黑名单检查，阻止包含危险模式的命令执行。
     * 即使命令通过了白名单检查，如果包含危险模式也会被拒绝。
     *
     * <h3>检查逻辑</h3>
     * <ol>
     *   <li>如果安全检查被禁用，直接返回 false（不包含危险模式）</li>
     *   <li>将命令转为小写进行大小写不敏感匹配</li>
     *   <li>检查是否包含任何危险模式</li>
     * </ol>
     *
     * @param command 要检查的命令
     * @return true 如果命令包含危险模式，false 如果命令安全
     */
    public boolean containsBlockedPatterns(String command) {
        // 如果禁用了安全检查，不阻止任何命令
        if (!execution.isEnableSecurityChecks()) {
            return false;
        }
        // 大小写不敏感匹配
        String lowerCommand = command.toLowerCase();
        // 检查是否包含任何危险模式
        return execution.getBlockedPatterns().stream()
                .anyMatch(pattern -> lowerCommand.contains(pattern.toLowerCase()));
    }
}