package com.jiraclone.task.dto;

import com.jiraclone.task.domain.TaskAttachment;
import com.jiraclone.task.domain.TaskComment;
import com.jiraclone.task.domain.TaskDocument;
import com.jiraclone.task.domain.TaskPriority;
import com.jiraclone.task.domain.TaskStatus;

import java.time.Instant;
import java.util.List;

public record TaskResponse(
        String id,
        String projectId,
        String sprintId,
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        String assignee,
        String reporter,
        List<String> labels,
        List<TaskComment> comments,
        List<TaskAttachment> attachments,
        Instant createdAt,
        Instant updatedAt
) {
    public static TaskResponse from(TaskDocument task) {
        return new TaskResponse(
                task.getId(),
                task.getProjectId(),
                task.getSprintId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getAssignee(),
                task.getReporter(),
                List.copyOf(task.getLabels()),
                List.copyOf(task.getComments()),
                List.copyOf(task.getAttachments()),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
