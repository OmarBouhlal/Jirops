// planning-service/src/main/java/com/jiraclone/planning/dto/AddTaskToSprintRequest.java
package com.jiraclone.planning.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddTaskToSprintRequest(
        @NotNull(message = "taskId must not be null") UUID taskId
) {
}
