package it.uninsubria.server.exception;

/**
 * Exception for business logic errors in the application.
 */
public class BusinessLogicException extends RuntimeException {

    /**
     * Constructs a BusinessLogicException with the specified message.
     *
     * @param message the detail message
     */
    public BusinessLogicException(String message) {
        super(message);
    }

    /**
     * Constructs a BusinessLogicException with the specified message and cause.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public BusinessLogicException(String message, Throwable cause) {
        super(message, cause);
    }
}