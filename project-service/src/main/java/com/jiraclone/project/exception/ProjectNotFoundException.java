// project-service/src/main/java/com/jiraclone/project/exception/ProjectNotFoundException.java
package com.jiraclone.project.exception;

public class ProjectNotFoundException extends RuntimeException {

    public ProjectNotFoundException(String message) {
        super(message);
    }
}
