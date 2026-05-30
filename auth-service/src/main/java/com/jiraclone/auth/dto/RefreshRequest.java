// auth-service/src/main/java/com/jiraclone/auth/dto/RefreshRequest.java
package com.jiraclone.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
        @NotBlank String refreshToken
) {
}
