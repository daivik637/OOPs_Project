package com.mumbai.best.service;

import com.mumbai.best.model.BusType;

/**
 * Implements Mumbai BEST (Brihanmumbai Electric Supply and Transport) bus fare tables.
 * Slabs are calibrated against official BEST distance-based pricing.
 */
public class FareCalculator {

    /**
     * Minimum tap-in balance required for single-ride card holders (equal to minimum BEST fare).
     */
    public static final double MINIMUM_TAP_IN_BALANCE = 6.0;

    /**
     * Calculates the standard BEST fare based on travel distance and bus type.
     *
     * @param distanceKm Distance traversed in kilometers
     * @param busType    Type of bus (NON_AC or AC)
     * @return Fare in Indian Rupees (₹)
     */
    public static double calculateBestFare(double distanceKm, BusType busType) {
        if (distanceKm <= 0.0) {
            return 0.0;
        }

        if (busType == BusType.NON_AC) {
            if (distanceKm <= 5.0) {
                return 6.0;
            } else if (distanceKm <= 10.0) {
                return 10.0;
            } else if (distanceKm <= 15.0) {
                return 15.0;
            } else if (distanceKm <= 20.0) {
                return 20.0;
            } else if (distanceKm <= 25.0) {
                return 25.0;
            } else if (distanceKm <= 30.0) {
                return 30.0;
            } else {
                return 35.0;
            }
        } else { // BusType.AC
            if (distanceKm <= 5.0) {
                return 6.0;
            } else if (distanceKm <= 10.0) {
                return 13.0;
            } else if (distanceKm <= 15.0) {
                return 19.0;
            } else if (distanceKm <= 20.0) {
                return 25.0;
            } else if (distanceKm <= 25.0) {
                return 31.0;
            } else if (distanceKm <= 30.0) {
                return 37.0;
            } else {
                return 45.0;
            }
        }
    }
}
