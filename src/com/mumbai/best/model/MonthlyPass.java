package com.mumbai.best.model;

import com.mumbai.best.exception.ExpiredPassException;
import com.mumbai.best.exception.TransitException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents a monthly commuter pass for Mumbai BEST network.
 * Supports zone-based access (e.g., "ALL_ZONES", "ZONE_1 - Island City") and AC eligibility.
 */
public class MonthlyPass extends Ticket {
    private LocalDateTime validUntil;
    private final String allowedZone; // e.g., "ALL" or specific zone
    private final boolean acPermitted;
    private int tripsCompleted;

    public MonthlyPass(String ticketId, String commuterName, LocalDateTime issuedAt,
                       LocalDateTime validUntil, String allowedZone, boolean acPermitted) {
        super(ticketId, commuterName, issuedAt);
        this.validUntil = validUntil;
        this.allowedZone = allowedZone;
        this.acPermitted = acPermitted;
        this.tripsCompleted = 0;
    }

    public LocalDateTime getValidUntil() {
        return validUntil;
    }

    public String getAllowedZone() {
        return allowedZone;
    }

    public boolean isAcPermitted() {
        return acPermitted;
    }

    public int getTripsCompleted() {
        return tripsCompleted;
    }

    public void extendPass(int months) {
        if (validUntil.isBefore(LocalDateTime.now())) {
            this.validUntil = LocalDateTime.now().plusMonths(months);
        } else {
            this.validUntil = this.validUntil.plusMonths(months);
        }
    }

    @Override
    public double calculateFare(BusRoute route, Stop boardingStop, Stop alightingStop) {
        // Polymorphic fare logic:
        // Regular monthly pass covers all rides within the subscribed zone/bus-type for ₹0.
        // If commuter rides an AC bus without AC privileges, an AC surcharge of ₹10 is applied.
        // If commuter exits beyond allowed zone (when not "ALL"), an out-of-zone penalty of ₹8 is applied.
        double supplementalFare = 0.0;

        if (route.getBusType() == BusType.AC && !acPermitted) {
            supplementalFare += 10.0; // AC upgrade charge
        }

        if (!"ALL".equalsIgnoreCase(allowedZone)) {
            boolean exitOutsideZone = !alightingStop.getZone().equalsIgnoreCase(allowedZone);
            if (exitOutsideZone) {
                supplementalFare += 8.0; // Zone extension fee
            }
        }

        return supplementalFare; // Normally 0.0 for compliant trips
    }

    @Override
    public void validateTapIn(BusRoute route, Stop stop) throws TransitException {
        if (LocalDateTime.now().isAfter(validUntil)) {
            throw new ExpiredPassException("Monthly Pass has expired. Please renew your subscription.", validUntil);
        }
    }

    @Override
    public void processTapOut(BusRoute route, Stop stop, double fare) throws TransitException {
        if (LocalDateTime.now().isAfter(validUntil)) {
            throw new ExpiredPassException("Monthly Pass expired during journey.", validUntil);
        }
        this.tripsCompleted++;
    }

    @Override
    public String getTicketType() {
        return "Monthly Pass";
    }

    @Override
    public String getDetails() {
        return String.format("%s | Valid Until: %s | Zone: %s | AC Allowed: %s | Trips: %d",
                toString(),
                validUntil.format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm")),
                allowedZone,
                acPermitted ? "YES" : "NO",
                tripsCompleted);
    }
}
