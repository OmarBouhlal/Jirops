// planning-service/src/main/java/com/jiraclone/planning/dto/SprintResponse.java
package com.jiraclone.planning.dto;

import com.jiraclone.planning.domain.SprintStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record SprintResponse(
        UUID id,
        UUID projectId,
        String name,
        String goal,
        LocalDate startDate,
        LocalDate endDate,
        SprintStatus status,
        List<UUID> taskIds,
        Instant createdAt
) {
}
