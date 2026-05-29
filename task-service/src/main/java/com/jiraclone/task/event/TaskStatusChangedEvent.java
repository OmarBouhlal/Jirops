package com.jiraclone.task.event;

import com.jiraclone.task.domain.TaskStatus;

import java.time.Instant;

public record TaskStatusChangedEvent(
        String taskId,
        String projectId,
        String sprintId,
        TaskStatus previousStatus,
        TaskStatus newStatus,
        String changedBy,
        Instant changedAt
) {
}
