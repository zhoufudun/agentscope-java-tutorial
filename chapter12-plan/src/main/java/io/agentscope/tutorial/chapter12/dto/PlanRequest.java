package io.agentscope.tutorial.chapter12.dto;

import java.util.List;

/**
 * 计划请求 DTO.
 */
public class PlanRequest {

    private String name;
    private String description;
    private String expectedOutcome;
    private List<SubTaskRequest> subtasks;

    public PlanRequest() {
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

    public List<SubTaskRequest> getSubtasks() {
        return subtasks;
    }

    public void setSubtasks(List<SubTaskRequest> subtasks) {
        this.subtasks = subtasks;
    }
}