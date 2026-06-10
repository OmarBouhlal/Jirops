package com.jiraclone.auth.dto;

import java.util.UUID;

public record UserDirectoryResponse(
        UUID id,
        String email
) {
}
