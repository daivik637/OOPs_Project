package com.mumbai.best.exception;

/**
 * Thrown when an invalid state transition occurs during tap-in/tap-out,
 * such as tapping out without tapping in, or tapping in twice on different routes.
 */
public class InvalidTripException extends TransitException {
    public InvalidTripException(String message) {
        super(message);
    }
}
