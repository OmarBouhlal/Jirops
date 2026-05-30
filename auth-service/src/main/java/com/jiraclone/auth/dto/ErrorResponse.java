// auth-service/src/main/java/com/jiraclone/auth/dto/ErrorResponse.java
package com.jiraclone.auth.dto;

public record ErrorResponse(
        String error,
        int status
) {
}
