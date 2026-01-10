package it.uninsubria.server.exception;

/**
 * Exception thrown when authentication fails (invalid credentials, etc.).
 */
public class AuthenticationException extends SecurityException {
    
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