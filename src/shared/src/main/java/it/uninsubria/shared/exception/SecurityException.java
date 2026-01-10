package it.uninsubria.shared.exception;

import java.io.Serializable;

public class SecurityException extends RuntimeException implements Serializable {

    private static final long serialVersionUID = 1L;

    public SecurityException(String message) {
        super(message);
    }

    public SecurityException(String message, Throwable cause) {
        super(message, cause);
    }
}
