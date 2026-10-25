package com.mumbai.best.model;

/**
 * Result of scanning a bus QR code, indicating whether it was an entry tap-in
 * or an exit tap-out, along with journey metrics.
 */
public class ScanResult {
    public enum ScanType {
        TAP_IN,
        TAP_OUT
    }

    private final ScanType scanType;
    private final Bus bus;
    private final Ticket ticket;
    private final Trip trip;
    private final String message;
    private final double fareCharged;

    public ScanResult(ScanType scanType, Bus bus, Ticket ticket, Trip trip, String message, double fareCharged) {
        this.scanType = scanType;
        this.bus = bus;
        this.ticket = ticket;
        this.trip = trip;
        this.message = message;
        this.fareCharged = fareCharged;
    }

    public ScanType getScanType() {
        return scanType;
    }

    public Bus getBus() {
        return bus;
    }

    public Ticket getTicket() {
        return ticket;
    }

    public Trip getTrip() {
        return trip;
    }

    public String getMessage() {
        return message;
    }

    public double getFareCharged() {
        return fareCharged;
    }

    public boolean isTapIn() {
        return scanType == ScanType.TAP_IN;
    }

    public boolean isTapOut() {
        return scanType == ScanType.TAP_OUT;
    }
}
