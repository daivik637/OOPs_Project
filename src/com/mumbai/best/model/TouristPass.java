package com.mumbai.best.model;

import com.mumbai.best.exception.ExpiredPassException;
import com.mumbai.best.exception.TransitException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents a prepaid Tourist Pass (e.g., 1-Day Mumbai Darshan or 3-Day Unlimited).
 * Allows unlimited travel across all zones on both AC and Non-AC BEST routes.
 */
public class TouristPass extends Ticket {
    private LocalDateTime validUntil;
    private final String passPackageName;
    private int touristTripsCount;

    public TouristPass(String ticketId, String commuterName, LocalDateTime issuedAt,
                       LocalDateTime validUntil, String passPackageName) {
        super(ticketId, commuterName, issuedAt);
        this.validUntil = validUntil;
        this.passPackageName = passPackageName;
        this.touristTripsCount = 0;
    }

    public LocalDateTime getValidUntil() {
        return validUntil;
    }

    public String getPassPackageName() {
        return passPackageName;
    }

    public int getTouristTripsCount() {
        return touristTripsCount;
    }

    @Override
    public double calculateFare(BusRoute route, Stop boardingStop, Stop alightingStop) {
        // Polymorphic fare: Tourist Pass entitles unlimited free hop-on hop-off rides across all zones
        return 0.0;
    }

    @Override
    public void validateTapIn(BusRoute route, Stop stop) throws TransitException {
        if (LocalDateTime.now().isAfter(validUntil)) {
            throw new ExpiredPassException("Tourist Pass validity has expired. Please purchase a new pass.", validUntil);
        }
    }

    @Override
    public void processTapOut(BusRoute route, Stop stop, double fare) throws TransitException {
        if (LocalDateTime.now().isAfter(validUntil)) {
            throw new ExpiredPassException("Tourist Pass expired before exit.", validUntil);
        }
        this.touristTripsCount++;
    }

    @Override
    public String getTicketType() {
        return "Tourist Pass (" + passPackageName + ")";
    }

    @Override
    public String getDetails() {
        return String.format("%s | Valid Until: %s | Package: %s | Free Rides Used: %d",
                toString(),
                validUntil.format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm")),
                passPackageName,
                touristTripsCount);
    }
}
