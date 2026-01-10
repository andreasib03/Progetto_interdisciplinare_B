package it.uninsubria.server.exception;

/**
 * Exception for network and RMI-related errors in the application.
 */
public class NetworkException extends RuntimeException {

    public NetworkException(String message) {
        super(message);
    }

    public NetworkException(String message, Throwable cause) {
        super(message, cause);
    }
}