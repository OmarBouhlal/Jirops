// auth-service/src/main/java/com/jiraclone/auth/exception/InvalidTokenException.java
package com.jiraclone.auth.exception;

public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException(String message) {
        super(message);
    }

    public InvalidTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
