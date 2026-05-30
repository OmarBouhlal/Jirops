// project-service/src/main/java/com/jiraclone/project/dto/ProjectResponse.java
package com.jiraclone.project.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProjectResponse(
        UUID id,
        String name,
        String key,
        String description,
        UUID ownerId,
        List<UUID> members,
        Instant createdAt
) {
}
