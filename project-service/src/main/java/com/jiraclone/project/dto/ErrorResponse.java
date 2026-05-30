// project-service/src/main/java/com/jiraclone/project/dto/ErrorResponse.java
package com.jiraclone.project.dto;

public record ErrorResponse(String error, int status) {
}
