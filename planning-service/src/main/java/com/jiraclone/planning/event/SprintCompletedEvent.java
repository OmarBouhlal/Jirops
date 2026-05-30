// planning-service/src/main/java/com/jiraclone/planning/event/SprintCompletedEvent.java
package com.jiraclone.planning.event;

import java.time.Instant;
import java.util.UUID;

public record SprintCompletedEvent(
        UUID sprintId,
        UUID projectId,
        Instant completedAt,
        Instant timestamp
) {
}
