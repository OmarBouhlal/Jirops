// project-service/src/main/java/com/jiraclone/project/exception/DuplicateKeyException.java
package com.jiraclone.project.exception;

public class DuplicateKeyException extends RuntimeException {

    public DuplicateKeyException(String message) {
        super(message);
    }
}
