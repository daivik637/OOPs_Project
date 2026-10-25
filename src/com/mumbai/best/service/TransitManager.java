package com.mumbai.best.service;

import com.mumbai.best.exception.InvalidTripException;
import com.mumbai.best.exception.TransitException;
import com.mumbai.best.model.Bus;
import com.mumbai.best.model.BusRoute;
import com.mumbai.best.model.ScanResult;
import com.mumbai.best.model.SingleRideTicket;
import com.mumbai.best.model.Stop;
import com.mumbai.best.model.Ticket;
import com.mumbai.best.model.Trip;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Coordinates transit cards, routes, tap-in/tap-out validation, and audit logging.
 */
public class TransitManager {
    private final Map<String, Ticket> ticketRegistry = new LinkedHashMap<>();
    private final Map<String, BusRoute> routeRegistry = new LinkedHashMap<>();
    private final Map<String, com.mumbai.best.model.Bus> busRegistry = new LinkedHashMap<>();
    private final Map<String, Trip> activeTrips = new LinkedHashMap<>(); // ticketId -> active Trip
    private final AuditLogService auditLogService;
    private final AtomicInteger tripIdCounter = new AtomicInteger(1000);

    public TransitManager(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    public AuditLogService getAuditLogService() {
        return auditLogService;
    }

    public void addRoute(BusRoute route) {
        routeRegistry.put(route.getRouteNumber().toUpperCase(), route);
    }

    public BusRoute getRoute(String routeNumber) {
        return routeRegistry.get(routeNumber.toUpperCase());
    }

    public List<BusRoute> getAllRoutes() {
        return new ArrayList<>(routeRegistry.values());
    }

    public void addBus(com.mumbai.best.model.Bus bus) {
        busRegistry.put(bus.getBusId().toUpperCase(), bus);
    }

    public com.mumbai.best.model.Bus getBus(String busId) {
        return busRegistry.get(busId.toUpperCase());
    }

    public List<com.mumbai.best.model.Bus> getAllBuses() {
        return new ArrayList<>(busRegistry.values());
    }

    public void registerTicket(Ticket ticket, double initialPayment) {
        ticketRegistry.put(ticket.getTicketId().toUpperCase(), ticket);
        auditLogService.logCardIssued(ticket, initialPayment);
    }

    public Ticket getTicket(String ticketId) {
        return ticketRegistry.get(ticketId.toUpperCase());
    }

    public List<Ticket> getAllTickets() {
        return new ArrayList<>(ticketRegistry.values());
    }

    public Map<String, Trip> getActiveTrips() {
        return Collections.unmodifiableMap(activeTrips);
    }

    /**
     * Recharges a SingleRideTicket and logs the audit event.
     */
    public void rechargeCard(String ticketId, double amount) throws TransitException {
        Ticket ticket = getTicket(ticketId);
        if (ticket == null) {
            throw new TransitException("Smart Card ID not recognized: " + ticketId);
        }

        if (!(ticket instanceof SingleRideTicket singleRideTicket)) {
            throw new TransitException(ticket.getTicketType() + " is an unlimited pass and cannot be reloaded with cash balance.");
        }

        singleRideTicket.recharge(amount);
        auditLogService.logRecharge(ticket, amount, singleRideTicket.getWalletBalance());
    }

    /**
     * Simulates QR code boarding scan at entry.
     */
    public Trip tapIn(String ticketId, String routeNumber, String stopId) throws TransitException {
        Ticket ticket = getTicket(ticketId);
        if (ticket == null) {
            auditLogService.logInfraction(ticketId, "Unknown", routeNumber, "Unregistered ticket scanned");
            throw new TransitException("Unregistered ticket ID scanned: " + ticketId);
        }

        BusRoute route = getRoute(routeNumber);
        if (route == null) {
            auditLogService.logInfraction(ticketId, ticket.getCommuterName(), routeNumber, "Unknown bus route scanned");
            throw new TransitException("Bus Route not found: " + routeNumber);
        }

        Stop stop = route.findStopById(stopId).orElse(null);
        if (stop == null) {
            auditLogService.logInfraction(ticketId, ticket.getCommuterName(), routeNumber, "Invalid stop ID scanned: " + stopId);
            throw new TransitException("Stop " + stopId + " does not belong to Route " + routeNumber);
        }

        // Validate no in-flight trip already active
        if (activeTrips.containsKey(ticket.getTicketId().toUpperCase())) {
            Trip currentTrip = activeTrips.get(ticket.getTicketId().toUpperCase());
            String reason = "Duplicate tap-in detected without tapping out of trip " + currentTrip.getTripId();
            auditLogService.logInfraction(ticketId, ticket.getCommuterName(), routeNumber, reason);
            throw new InvalidTripException(reason);
        }

        try {
            // Polymorphic entry validation (checks minimum balance or pass expiry)
            ticket.validateTapIn(route, stop);
        } catch (TransitException e) {
            auditLogService.logInfraction(ticketId, ticket.getCommuterName(), routeNumber, e.getMessage());
            throw e;
        }

        String tripId = "TRIP-" + tripIdCounter.incrementAndGet();
        Trip newTrip = new Trip(tripId, ticket, route, stop, LocalDateTime.now());
        activeTrips.put(ticket.getTicketId().toUpperCase(), newTrip);
        return newTrip;
    }

    /**
     * Simulates QR code alighting scan at exit.
     * Calculates dynamic fare based on distance & zones and deducts from smart card.
     */
    public Trip tapOut(String ticketId, String routeNumber, String stopId) throws TransitException {
        Ticket ticket = getTicket(ticketId);
        if (ticket == null) {
            throw new TransitException("Unregistered ticket ID scanned at exit: " + ticketId);
        }

        Trip activeTrip = activeTrips.get(ticket.getTicketId().toUpperCase());
        if (activeTrip == null) {
            String reason = "Tap-out scanned without an active tap-in for Ticket: " + ticketId;
            auditLogService.logInfraction(ticketId, ticket.getCommuterName(), routeNumber, reason);
            throw new InvalidTripException(reason);
        }

        if (!activeTrip.getRoute().getRouteNumber().equalsIgnoreCase(routeNumber)) {
            String reason = String.format("Mismatched exit bus: boarded Route %s but scanned exit on Route %s",
                    activeTrip.getRoute().getRouteNumber(), routeNumber);
            auditLogService.logInfraction(ticketId, ticket.getCommuterName(), routeNumber, reason);
            throw new InvalidTripException(reason);
        }

        BusRoute route = activeTrip.getRoute();
        Stop alightingStop = route.findStopById(stopId).orElse(null);
        if (alightingStop == null) {
            throw new TransitException("Stop " + stopId + " does not exist on Route " + routeNumber);
        }

        if (activeTrip.getBoardingStop().equals(alightingStop)) {
            throw new InvalidTripException("Tap-out cannot be at the exact same stop as tap-in.");
        }

        // Polymorphic dynamic fare calculation
        double fare = ticket.calculateFare(route, activeTrip.getBoardingStop(), alightingStop);

        // Polymorphic exit processing (deducts wallet or validates pass)
        try {
            ticket.processTapOut(route, alightingStop, fare);
        } catch (TransitException e) {
            auditLogService.logInfraction(ticketId, ticket.getCommuterName(), routeNumber, e.getMessage());
            throw e;
        }

        double distance = route.calculateDistanceBetween(activeTrip.getBoardingStop(), alightingStop);
        activeTrip.completeTrip(alightingStop, LocalDateTime.now(), distance, fare);

        // Remove from active trips and log completed transaction to audit file
        activeTrips.remove(ticket.getTicketId().toUpperCase());
        auditLogService.logTripCompleted(activeTrip);

        return activeTrip;
    }

    /**
     * Smart QR Code Scan Handler:
     * When scanned on a bus:
     * - If commuter is NOT on this bus -> automatically performs Tap-In (Boarding).
     * - If commuter is ALREADY onboard this bus -> automatically performs Tap-Out (Alighting & Fare Deduction).
     * - Route is tied directly to the bus QR code; GPS stop approximates location.
     */
    public ScanResult scanBusQr(String ticketId, String busId, String gpsStopId) throws TransitException {
        Ticket ticket = getTicket(ticketId);
        if (ticket == null) {
            throw new TransitException("Unrecognized card / ticket ID: " + ticketId);
        }

        Bus bus = getBus(busId);
        if (bus == null) {
            throw new TransitException("Unrecognized Bus QR code: " + busId);
        }

        Trip activeTrip = activeTrips.get(ticket.getTicketId().toUpperCase());
        if (activeTrip == null) {
            // First scan: Automatic Tap-In (Boarding)
            Trip newTrip = tapIn(ticket.getTicketId(), bus.getRoute().getRouteNumber(), gpsStopId);
            String msg = String.format("Welcome Aboard! Boarded Bus %s (%s) at %s.",
                    bus.getBusId(), bus.getLicensePlate(), newTrip.getBoardingStop().getName());
            return new ScanResult(ScanResult.ScanType.TAP_IN, bus, ticket, newTrip, msg, 0.0);
        } else {
            // Commuter already has an in-flight trip
            if (!activeTrip.getRoute().getRouteNumber().equalsIgnoreCase(bus.getRoute().getRouteNumber())) {
                String reason = String.format("Trip Conflict: You are already onboard Route %s (boarded at %s). You cannot board Bus %s without tapping out first!",
                        activeTrip.getRoute().getRouteNumber(), activeTrip.getBoardingStop().getName(), bus.getBusId());
                auditLogService.logInfraction(ticketId, ticket.getCommuterName(), bus.getRoute().getRouteNumber(), reason);
                throw new InvalidTripException(reason);
            }

            // Second scan: Automatic Tap-Out (Alighting)
            Trip completedTrip = tapOut(ticket.getTicketId(), bus.getRoute().getRouteNumber(), gpsStopId);
            String msg = String.format("Alighted from Bus %s at %s. Distance: %.2f km. BEST Fare: ₹%.2f",
                    bus.getBusId(), completedTrip.getAlightingStop().getName(),
                    completedTrip.getDistanceKm(), completedTrip.getFareCharged());
            return new ScanResult(ScanResult.ScanType.TAP_OUT, bus, ticket, completedTrip, msg, completedTrip.getFareCharged());
        }
    }
}
