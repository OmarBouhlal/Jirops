package com.jiraclone.task.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record AddAttachmentRequest(
        @NotBlank String fileName,
        String contentType,
        @Min(0) long size,
        @NotBlank String url
) {
}
