package it.uninsubria.server.exception;

/**
 * Exception for database-related errors in the application.
 */
public class DatabaseException extends RuntimeException {

    /**
     * Constructs a DatabaseException with the specified message.
     *
     * @param message the detail message
     */
    public DatabaseException(String message) {
        super(message);
    }

    /**
     * Constructs a DatabaseException with the specified message and cause.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}