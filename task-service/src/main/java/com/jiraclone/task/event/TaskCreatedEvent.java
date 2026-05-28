package com.jiraclone.task.event;

import com.jiraclone.task.domain.TaskPriority;
import com.jiraclone.task.domain.TaskStatus;

import java.time.Instant;

public record TaskCreatedEvent(
        String taskId,
        String projectId,
        String sprintId,
        String title,
        TaskStatus status,
        TaskPriority priority,
        String assignee,
        String reporter,
        Instant createdAt
) {
}
