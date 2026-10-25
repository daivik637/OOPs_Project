package com.mumbai.best.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Tracks an active or completed bus trip initiated via QR code tap-in and finalized via tap-out.
 */
public class Trip {
    private final String tripId;
    private final Ticket ticket;
    private final BusRoute route;
    private final Stop boardingStop;
    private final LocalDateTime tapInTime;

    private Stop alightingStop;
    private LocalDateTime tapOutTime;
    private double distanceKm;
    private double fareCharged;
    private boolean completed;

    public Trip(String tripId, Ticket ticket, BusRoute route, Stop boardingStop, LocalDateTime tapInTime) {
        this.tripId = tripId;
        this.ticket = ticket;
        this.route = route;
        this.boardingStop = boardingStop;
        this.tapInTime = tapInTime;
        this.completed = false;
        this.distanceKm = 0.0;
        this.fareCharged = 0.0;
    }

    public void completeTrip(Stop alightingStop, LocalDateTime tapOutTime, double distanceKm, double fareCharged) {
        this.alightingStop = alightingStop;
        this.tapOutTime = tapOutTime;
        this.distanceKm = distanceKm;
        this.fareCharged = fareCharged;
        this.completed = true;
    }

    public String getTripId() {
        return tripId;
    }

    public Ticket getTicket() {
        return ticket;
    }

    public BusRoute getRoute() {
        return route;
    }

    public Stop getBoardingStop() {
        return boardingStop;
    }

    public LocalDateTime getTapInTime() {
        return tapInTime;
    }

    public Stop getAlightingStop() {
        return alightingStop;
    }

    public LocalDateTime getTapOutTime() {
        return tapOutTime;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public double getFareCharged() {
        return fareCharged;
    }

    public boolean isCompleted() {
        return completed;
    }

    @Override
    public String toString() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");
        if (completed) {
            return String.format("Trip %s: %s | Route %s (%s) | %s -> %s (%.1f km) | Fare: ₹%.2f | [%s - %s]",
                    tripId, ticket.getTicketId(), route.getRouteNumber(), route.getBusType().name(),
                    boardingStop.getName(), alightingStop.getName(), distanceKm, fareCharged,
                    tapInTime.format(dtf), tapOutTime.format(dtf));
        } else {
            return String.format("Trip %s (IN-PROGRESS): %s | Route %s | Boarded at: %s at %s",
                    tripId, ticket.getTicketId(), route.getRouteNumber(), boardingStop.getName(),
                    tapInTime.format(dtf));
        }
    }
}
