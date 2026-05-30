// planning-service/src/main/java/com/jiraclone/planning/exception/SprintNotFoundException.java
package com.jiraclone.planning.exception;

public class SprintNotFoundException extends RuntimeException {

    public SprintNotFoundException(String message) {
        super(message);
    }
}
