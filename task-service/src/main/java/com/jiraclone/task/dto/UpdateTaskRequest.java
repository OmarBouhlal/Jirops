package com.jiraclone.task.dto;

import com.jiraclone.task.domain.TaskPriority;

import java.util.List;

public record UpdateTaskRequest(
        String projectId,
        String sprintId,
        String title,
        String description,
        TaskPriority priority,
        String assignee,
        List<String> labels
) {
}
