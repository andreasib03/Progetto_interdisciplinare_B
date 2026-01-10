package it.uninsubria.shared.exception;

import java.io.Serializable;

/**
 * Exception thrown when authentication fails.
 */
public class AuthenticationException extends SecurityException implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs an AuthenticationException with the specified message.
     *
     * @param message the detail message
     */
    public AuthenticationException(String message) {
        super(message);
    }

    /**
     * Constructs an AuthenticationException with the specified message and cause.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
