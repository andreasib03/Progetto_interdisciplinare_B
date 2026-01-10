package it.uninsubria.shared.exception;

import java.io.Serializable;

public class NetworkException extends RuntimeException implements Serializable {

    private static final long serialVersionUID = 1L;

    public NetworkException(String message) {
        super(message);
    }

    public NetworkException(String message, Throwable cause) {
        super(message, cause);
    }
}
