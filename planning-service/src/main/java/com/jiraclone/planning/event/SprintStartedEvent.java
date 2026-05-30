// planning-service/src/main/java/com/jiraclone/planning/event/SprintStartedEvent.java
package com.jiraclone.planning.event;

import java.time.LocalDate;
import java.time.Instant;
import java.util.UUID;

public record SprintStartedEvent(
        UUID sprintId,
        UUID projectId,
        LocalDate startDate,
        Instant timestamp
) {
}
