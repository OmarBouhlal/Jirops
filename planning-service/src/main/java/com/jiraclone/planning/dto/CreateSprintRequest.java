// planning-service/src/main/java/com/jiraclone/planning/dto/CreateSprintRequest.java
package com.jiraclone.planning.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record CreateSprintRequest(
        @NotNull(message = "projectId must not be null") UUID projectId,
        @NotBlank(message = "name must not be blank") String name,
        String goal,
        LocalDate startDate,
        LocalDate endDate
) {
}
