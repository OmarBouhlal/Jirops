// project-service/src/main/java/com/jiraclone/project/dto/CreateProjectRequest.java
package com.jiraclone.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateProjectRequest(
        @NotBlank(message = "name must not be blank") String name,
        @NotBlank(message = "key must not be blank")
        @Size(min = 2, max = 10, message = "key must be between 2 and 10 characters")
        @Pattern(regexp = "^[A-Z0-9]+$", message = "key must contain only uppercase letters and digits") String key,
        String description
) {
}
