// auth-service/src/main/java/com/jiraclone/auth/dto/RegisterRequest.java
package com.jiraclone.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8) String password
) {
}
