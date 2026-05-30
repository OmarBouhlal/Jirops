// auth-service/src/main/java/com/jiraclone/auth/exception/EmailAlreadyExistsException.java
package com.jiraclone.auth.exception;

public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String message) {
        super(message);
    }

    public EmailAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}
