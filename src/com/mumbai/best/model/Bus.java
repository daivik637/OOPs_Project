package com.mumbai.best.model;

/**
 * Represents a physical BEST bus operating on a specific route.
 * Each bus has its own unique QR code affixed inside the vehicle.
 */
public class Bus {
    private final String busId;
    private final String licensePlate;
    private final BusRoute route;
    private final String qrCodeData;

    public Bus(String busId, String licensePlate, BusRoute route) {
        this.busId = busId;
        this.licensePlate = licensePlate;
        this.route = route;
        // The QR code on the bus encodes the unique bus identifier and its assigned route
        this.qrCodeData = String.format("BEST-BUS:%s:PLATE:%s:ROUTE:%s:%s",
                busId, licensePlate, route.getRouteNumber(), route.getBusType().name());
    }

    public String getBusId() {
        return busId;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public BusRoute getRoute() {
        return route;
    }

    public String getQrCodeData() {
        return qrCodeData;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s | Route %s (%s)",
                busId, licensePlate, route.getRouteNumber(), route.getBusType().getDisplayName());
    }
}
