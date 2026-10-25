package com.mumbai.best.model;

import com.mumbai.best.exception.TransitException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Abstract base class representing a commuter ticket or smart card in Mumbai BEST transit network.
 * Demonstrates OOP Inheritance and Polymorphism.
 */
public abstract class Ticket {
    private final String ticketId;
    private final String commuterName;
    private final LocalDateTime issuedAt;

    public Ticket(String ticketId, String commuterName, LocalDateTime issuedAt) {
        this.ticketId = ticketId;
        this.commuterName = commuterName;
        this.issuedAt = issuedAt;
    }

    public String getTicketId() {
        return ticketId;
    }

    public String getCommuterName() {
        return commuterName;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    /**
     * Polymorphic method to dynamically calculate fare based on route, stops, and zones.
     * Overridden differently by SingleRideTicket, MonthlyPass, and TouristPass.
     *
     * @param route         The bus route taken
     * @param boardingStop  The entry stop
     * @param alightingStop The exit stop
     * @return Calculated fare in Rupees
     */
    public abstract double calculateFare(BusRoute route, Stop boardingStop, Stop alightingStop);

    /**
     * Polymorphic method to validate whether the passenger can tap-in and board the bus.
     *
     * @param route The bus route
     * @param stop  The boarding stop
     * @throws TransitException if balance is insufficient or pass has expired
     */
    public abstract void validateTapIn(BusRoute route, Stop stop) throws TransitException;

    /**
     * Polymorphic method to process tap-out at the destination stop.
     *
     * @param route The bus route
     * @param stop  The alighting stop
     * @param fare  The computed fare
     * @throws TransitException if balance cannot cover final fare or validation fails
     */
    public abstract void processTapOut(BusRoute route, Stop stop, double fare) throws TransitException;

    /**
     * Returns the human-readable category name for this ticket.
     */
    public abstract String getTicketType();

    /**
     * Returns detailed status and balance/validity string.
     */
    public abstract String getDetails();

    @Override
    public String toString() {
        return String.format("[%s] ID: %s | Commuter: %s | Issued: %s",
                getTicketType(),
                ticketId,
                commuterName,
                issuedAt.format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm")));
    }
}
