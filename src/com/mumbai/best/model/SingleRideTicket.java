package com.mumbai.best.model;

import com.mumbai.best.exception.InsufficientBalanceException;
import com.mumbai.best.exception.TransitException;
import com.mumbai.best.service.FareCalculator;

import java.time.LocalDateTime;

/**
 * Represents a wallet-based smart card or single-ride QR ticket.
 * Deducts distance/zone based BEST bus fare dynamically upon tap-out.
 */
public class SingleRideTicket extends Ticket {
    private double walletBalance;

    public SingleRideTicket(String ticketId, String commuterName, LocalDateTime issuedAt, double initialBalance) {
        super(ticketId, commuterName, issuedAt);
        this.walletBalance = Math.max(0.0, initialBalance);
    }

    public double getWalletBalance() {
        return walletBalance;
    }

    public void recharge(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Recharge amount must be positive.");
        }
        this.walletBalance += amount;
    }

    @Override
    public double calculateFare(BusRoute route, Stop boardingStop, Stop alightingStop) {
        double distanceKm = route.calculateDistanceBetween(boardingStop, alightingStop);
        double baseFare = FareCalculator.calculateBestFare(distanceKm, route.getBusType());

        // Polymorphic zone consideration:
        // BEST standard fares are distance-based, but cross-zone travel spanning beyond suburban zone boundaries
        // can incur an inter-zone surcharge of ₹2 if traveling across distinct zones.
        boolean isInterZone = !boardingStop.getZone().equalsIgnoreCase(alightingStop.getZone());
        double zoneSurcharge = isInterZone ? 2.0 : 0.0;

        return baseFare + zoneSurcharge;
    }

    @Override
    public void validateTapIn(BusRoute route, Stop stop) throws TransitException {
        if (walletBalance < FareCalculator.MINIMUM_TAP_IN_BALANCE) {
            throw new InsufficientBalanceException(
                    "Insufficient smart card balance for tap-in.",
                    walletBalance,
                    FareCalculator.MINIMUM_TAP_IN_BALANCE
            );
        }
    }

    @Override
    public void processTapOut(BusRoute route, Stop stop, double fare) throws TransitException {
        if (walletBalance < fare) {
            throw new InsufficientBalanceException(
                    "Insufficient balance to deduct fare at exit.",
                    walletBalance,
                    fare
            );
        }
        this.walletBalance -= fare;
    }

    @Override
    public String getTicketType() {
        return "Single Ride Smart Card";
    }

    @Override
    public String getDetails() {
        return String.format("%s | Wallet Balance: ₹%.2f", toString(), walletBalance);
    }
}
