package com.jiraclone.task.dto;

import com.jiraclone.task.domain.TaskPriority;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record CreateTaskRequest(
        @NotBlank String projectId,
        String sprintId,
        @NotBlank String title,
        String description,
        TaskPriority priority,
        String assignee,
        List<String> labels
) {
}
