// project-service/src/main/java/com/jiraclone/project/exception/UnauthorizedException.java
package com.jiraclone.project.exception;

public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
