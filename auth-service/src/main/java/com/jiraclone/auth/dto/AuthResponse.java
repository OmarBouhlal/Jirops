// auth-service/src/main/java/com/jiraclone/auth/dto/AuthResponse.java
package com.jiraclone.auth.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn
) {
}
