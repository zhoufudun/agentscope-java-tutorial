package io.agentscope.tutorial.chapter12.dto;

/**
 * 子任务请求 DTO.
 */
public class SubTaskRequest {

    private String name;
    private String description;
    private String expectedOutcome;

    public SubTaskRequest() {
    }

    public SubTaskRequest(String name, String description, String expectedOutcome) {
        this.name = name;
        this.description = description;
        this.expectedOutcome = expectedOutcome;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getExpectedOutcome() {
        return expectedOutcome;
    }

    public void setExpectedOutcome(String expectedOutcome) {
        this.expectedOutcome = expectedOutcome;
    }
}