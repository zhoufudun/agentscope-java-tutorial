package io.agentscope.tutorial.chapter15.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgs构造函数;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgs构造函数;

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
@NoArgs构造函数
@AllArgs构造函数
public class ExecutionRequest {

    @NotBlank(message = "Code cannot be blank")
    private String code;

    @Builder.默认
    private String language = "java";

    @Min(value = 1, message = "Timeout must be at least 1 second")
    @Max(value = 300, message = "Timeout cannot exceed 300 seconds")
    @Builder.默认
    private Integer timeoutSeconds = 60;

    @Builder.默认
    private String workspace = "default";

    @Builder.默认
    private Boolean captureOutput = true;
}