package com.mumbai.best.exception;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Thrown when a commuter attempts to use an expired Monthly Pass or Tourist Pass.
 */
public class ExpiredPassException extends TransitException {
    private final LocalDateTime expiryTime;

    public ExpiredPassException(String message, LocalDateTime expiryTime) {
        super(String.format("%s (Pass Expired On: %s)", 
                message, 
                expiryTime != null ? expiryTime.format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm")) : "N/A"));
        this.expiryTime = expiryTime;
    }

    public LocalDateTime getExpiryTime() {
        return expiryTime;
    }
}
