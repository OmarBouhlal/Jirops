// planning-service/src/main/java/com/jiraclone/planning/event/TaskStatusChangedEvent.java
package com.jiraclone.planning.event;

import java.time.Instant;
import java.util.UUID;

public record TaskStatusChangedEvent(
        UUID taskId,
        UUID sprintId,
        String newStatus,
        Instant timestamp
) {
}
