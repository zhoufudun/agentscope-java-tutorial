package io.agentscope.tutorial.chapter15.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Harness 配置属性.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "harness")
public class HarnessConfig {

    private SandboxConfig sandbox = new SandboxConfig();
    private ExecutionConfig execution = new ExecutionConfig();

    @Data
    public static class SandboxConfig {
        private int defaultTimeout = 60;
        private int maxTimeout = 300;
        private String workspaceRoot = "/workspace";
        private int maxConcurrentSandboxes = 10;
        private long workspaceIdleTimeoutMs = 3600000; // 1 hour
    }

    @Data
    public static class ExecutionConfig {
        private long maxOutputSize = 1024 * 1024; // 1MB
        private List<String> allowedCommands = List.of(
                "ls", "cat", "grep", "find", "echo", "mkdir", "touch", "rm", "cp", "mv"
        );
        private boolean enableSecurityChecks = true;
        private List<String> blockedPatterns = List.of(
                "rm -rf /",
                "DROP DATABASE",
                "DELETE FROM",
                "--os-shell"
        );
    }

    /**
     * Gets effective timeout for an execution request.
     */
    public int getEffectiveTimeout(Integer requestedTimeout) {
        if (requestedTimeout == null) {
            return sandbox.get默认Timeout();
        }
        return Math.min(requestedTimeout, sandbox.getMaxTimeout());
    }

    /**
     * Checks if a command is allowed.
     */
    public boolean isCommandAllowed(String command) {
        if (!execution.isEnableSecurityChecks()) {
            return true;
        }
        return execution.getAllowedCommands().stream()
                .anyMatch(cmd -> command.trim().startsWith(cmd));
    }

    /**
     * Checks if a command contains blocked patterns.
     */
    public boolean containsBlockedPatterns(String command) {
        if (!execution.isEnableSecurityChecks()) {
            return false;
        }
        String lowerCommand = command.toLowerCase();
        return execution.getBlockedPatterns().stream()
                .anyMatch(pattern -> lowerCommand.contains(pattern.toLowerCase()));
    }
}