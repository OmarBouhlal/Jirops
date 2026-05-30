// project-service/src/main/java/com/jiraclone/project/event/ProjectCreatedEvent.java
package com.jiraclone.project.event;

import java.time.Instant;
import java.util.UUID;

public record ProjectCreatedEvent(
        UUID projectId,
        String name,
        UUID ownerId,
        Instant timestamp
) {
}
