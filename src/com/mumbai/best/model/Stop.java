package com.mumbai.best.model;

import java.util.Objects;

/**
 * Represents a transit bus stop along a BEST route.
 */
public class Stop {
    private final String stopId;
    private final String name;
    private final double distanceFromOriginKm;
    private final String zone;

    public Stop(String stopId, String name, double distanceFromOriginKm, String zone) {
        this.stopId = stopId;
        this.name = name;
        this.distanceFromOriginKm = distanceFromOriginKm;
        this.zone = zone;
    }

    public String getStopId() {
        return stopId;
    }

    public String getName() {
        return name;
    }

    public double getDistanceFromOriginKm() {
        return distanceFromOriginKm;
    }

    public String getZone() {
        return zone;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Stop stop)) return false;
        return Objects.equals(stopId, stop.stopId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stopId);
    }

    @Override
    public String toString() {
        return String.format("%s (%s, %.1f km, Zone: %s)", name, stopId, distanceFromOriginKm, zone);
    }
}
