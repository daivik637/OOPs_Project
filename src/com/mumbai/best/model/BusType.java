package com.mumbai.best.model;

/**
 * BEST bus categories operating in Mumbai.
 */
public enum BusType {
    NON_AC("Regular Non-AC"),
    AC("Cool Air-Conditioned (AC)");

    private final String displayName;

    BusType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
