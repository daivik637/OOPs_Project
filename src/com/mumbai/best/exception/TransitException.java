package com.mumbai.best.exception;

/**
 * Base checked exception for transit system operations.
 */
public class TransitException extends Exception {
    public TransitException(String message) {
        super(message);
    }

    public TransitException(String message, Throwable cause) {
        super(message, cause);
    }
}
