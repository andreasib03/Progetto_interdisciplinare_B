package it.uninsubria.shared.exception;

import java.io.Serializable;

/**
 * Exception thrown when business logic validation fails.
 */
public class BusinessLogicException extends RuntimeException implements Serializable {

    private static final long serialVersionUID = 1L;

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
