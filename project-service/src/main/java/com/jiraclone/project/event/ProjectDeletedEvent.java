// project-service/src/main/java/com/jiraclone/project/event/ProjectDeletedEvent.java
package com.jiraclone.project.event;

import java.time.Instant;
import java.util.UUID;

public record ProjectDeletedEvent(
        UUID projectId,
        Instant timestamp
) {
}
