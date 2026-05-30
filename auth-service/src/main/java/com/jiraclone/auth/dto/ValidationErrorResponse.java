// auth-service/src/main/java/com/jiraclone/auth/dto/ValidationErrorResponse.java
package com.jiraclone.auth.dto;

import java.util.Map;

public record ValidationErrorResponse(
        String error,
        int status,
        Map<String, String> fieldErrors
) {
}
