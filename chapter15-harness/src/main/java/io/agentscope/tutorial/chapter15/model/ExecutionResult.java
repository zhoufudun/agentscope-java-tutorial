package io.agentscope.tutorial.chapter15.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Code execution result model.
 *
 * @param success whether execution was successful
 * @param exitCode process exit code
 * @param stdout captured standard output
 * @param stderr captured standard error
 * @param executionTimeMs execution time in milliseconds
 * @param workspace workspace ID
 * @param sandboxId sandbox instance ID
 * @param errorMessage error message if execution failed
 * @param truncated whether output was truncated
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExecutionResult {

    private boolean success;
    private Integer exitCode;
    private String stdout;
    private String stderr;

    @Builder.Default
    private Long executionTimeMs = 0L;

    @Builder.Default
    private String workspace = "default";

    private String sandboxId;
    private String errorMessage;
    private Boolean truncated;

    @Builder.Default
    private LocalDateTime executedAt = LocalDateTime.now();

    /**
     * Creates a successful execution result.
     */
    public static ExecutionResult success(String sandboxId, String stdout, String stderr,
                                          int exitCode, long executionTimeMs, String workspace) {
        return ExecutionResult.builder()
                .success(exitCode == 0)
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
     * Creates a failed execution result.
     */
    public static ExecutionResult failure(String sandboxId, String errorMessage,
                                          String workspace, Integer timeoutSeconds) {
        return ExecutionResult.builder()
                .success(false)
                .exitCode(-1)
                .errorMessage(errorMessage)
                .workspace(workspace)
                .sandboxId(sandboxId)
                .executedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Creates a timeout result.
     */
    public static ExecutionResult timeout(String sandboxId, String workspace, int timeoutSeconds) {
        return ExecutionResult.builder()
                .success(false)
                .exitCode(-2)
                .errorMessage("Execution timed out after " + timeoutSeconds + " seconds")
                .workspace(workspace)
                .sandboxId(sandboxId)
                .executedAt(LocalDateTime.now())
                .build();
    }
}