package it.uninsubria.server.exception;

/**
 * Base exception for security-related errors in the application.
 */
public class SecurityException extends RuntimeException {
    
    public SecurityException(String message) {
        super(message);
    }
    
    public SecurityException(String message, Throwable cause) {
        super(message, cause);
    }
}