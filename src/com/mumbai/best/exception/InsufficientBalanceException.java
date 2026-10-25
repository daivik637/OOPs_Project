package com.mumbai.best.exception;

/**
 * Thrown when a commuter's smart card or wallet does not have sufficient balance
 * to board the bus or to pay for the completed ride.
 */
public class InsufficientBalanceException extends TransitException {
    private final double currentBalance;
    private final double requiredAmount;

    public InsufficientBalanceException(String message, double currentBalance, double requiredAmount) {
        super(String.format("%s (Available: ₹%.2f, Required: ₹%.2f)", message, currentBalance, requiredAmount));
        this.currentBalance = currentBalance;
        this.requiredAmount = requiredAmount;
    }

    public double getCurrentBalance() {
        return currentBalance;
    }

    public double getRequiredAmount() {
        return requiredAmount;
    }
}
