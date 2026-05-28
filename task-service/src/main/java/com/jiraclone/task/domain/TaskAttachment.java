package com.jiraclone.task.domain;

import java.time.Instant;

public record TaskAttachment(
        String id,
        String fileName,
        String contentType,
        long size,
        String url,
        String uploadedBy,
        Instant uploadedAt
) {
}
