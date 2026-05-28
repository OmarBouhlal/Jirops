package com.jiraclone.task.domain;

import java.time.Instant;

public record TaskComment(
        String id,
        String authorId,
        String body,
        Instant createdAt
) {
}
