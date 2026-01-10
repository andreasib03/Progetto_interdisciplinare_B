package it.uninsubria.server.exception;

/**
 * Exception thrown when input validation fails or potential injection is detected.
 */
public class ValidationException extends SecurityException {
    
    public ValidationException(String message) {
        super(message);
    }
    
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}