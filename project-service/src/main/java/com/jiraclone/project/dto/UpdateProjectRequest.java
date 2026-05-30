// project-service/src/main/java/com/jiraclone/project/dto/UpdateProjectRequest.java
package com.jiraclone.project.dto;

import jakarta.validation.constraints.Size;

public record UpdateProjectRequest(
        @Size(max = 255, message = "name must be at most 255 characters") String name,
        String description
) {
}
