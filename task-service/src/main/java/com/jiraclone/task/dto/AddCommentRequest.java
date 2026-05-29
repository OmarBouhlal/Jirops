package com.jiraclone.task.dto;

import jakarta.validation.constraints.NotBlank;

public record AddCommentRequest(@NotBlank String body) {
}
