// auth-service/src/main/java/com/jiraclone/auth/exception/InvalidCredentialsException.java
package com.jiraclone.auth.exception;

public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException(String message) {
        super(message);
    }

    public InvalidCredentialsException(String message, Throwable cause) {
        super(message, cause);
    }
}
