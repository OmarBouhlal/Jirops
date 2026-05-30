// auth-service/src/main/java/com/jiraclone/auth/dto/LoginRequest.java
package com.jiraclone.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password
) {
}
