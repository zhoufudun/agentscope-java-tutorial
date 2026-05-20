package io.agentscope.tutorial.chapter15.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Code execution request model.
 *
 * @param code the code to execute
 * @param language programming language (java, python, javascript, etc.)
 * @param timeoutSeconds execution timeout in seconds
 * @param workspace workspace ID for isolation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionRequest {

    @NotBlank(message = "Code cannot be blank")
    private String code;

    @Builder.Default
    private String language = "java";

    @Min(value = 1, message = "Timeout must be at least 1 second")
    @Max(value = 300, message = "Timeout cannot exceed 300 seconds")
    @Builder.Default
    private Integer timeoutSeconds = 60;

    @Builder.Default
    private String workspace = "default";

    @Builder.Default
    private Boolean captureOutput = true;
}