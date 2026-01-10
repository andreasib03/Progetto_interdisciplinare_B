package it.uninsubria.shared.exception;

import java.io.Serializable;

public class DatabaseException extends RuntimeException implements Serializable {

    private static final long serialVersionUID = 1L;

    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
