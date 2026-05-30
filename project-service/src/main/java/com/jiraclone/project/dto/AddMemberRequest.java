// project-service/src/main/java/com/jiraclone/project/dto/AddMemberRequest.java
package com.jiraclone.project.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddMemberRequest(
        @NotNull(message = "userId must not be null") UUID userId
) {
}
