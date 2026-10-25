package com.mumbai.best.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Represents a Mumbai BEST bus route with sequential stops and bus type.
 */
public class BusRoute {
    private final String routeNumber;
    private final String routeName;
    private final BusType busType;
    private final List<Stop> stops;

    public BusRoute(String routeNumber, String routeName, BusType busType, List<Stop> stops) {
        this.routeNumber = routeNumber;
        this.routeName = routeName;
        this.busType = busType;
        this.stops = new ArrayList<>(stops);
    }

    public String getRouteNumber() {
        return routeNumber;
    }

    public String getRouteName() {
        return routeName;
    }

    public BusType getBusType() {
        return busType;
    }

    public List<Stop> getStops() {
        return Collections.unmodifiableList(stops);
    }

    public Optional<Stop> findStopById(String stopId) {
        return stops.stream()
                .filter(s -> s.getStopId().equalsIgnoreCase(stopId))
                .findFirst();
    }

    public Optional<Stop> findStopByName(String stopName) {
        return stops.stream()
                .filter(s -> s.getName().equalsIgnoreCase(stopName))
                .findFirst();
    }

    /**
     * Calculates distance traveled between two stops on this route.
     */
    public double calculateDistanceBetween(Stop from, Stop to) {
        int indexFrom = stops.indexOf(from);
        int indexTo = stops.indexOf(to);

        if (indexFrom == -1 || indexTo == -1) {
            throw new IllegalArgumentException("One or both stops are not found on route " + routeNumber);
        }

        return Math.abs(to.getDistanceFromOriginKm() - from.getDistanceFromOriginKm());
    }

    @Override
    public String toString() {
        return String.format("[%s] %s (%s, %d stops, Total: %.1f km)",
                routeNumber, routeName, busType.getDisplayName(), stops.size(),
                stops.isEmpty() ? 0.0 : stops.get(stops.size() - 1).getDistanceFromOriginKm());
    }
}
