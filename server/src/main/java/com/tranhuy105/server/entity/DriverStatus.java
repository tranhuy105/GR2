package com.tranhuy105.server.entity;

/**
 * Status of a driver
 */
public enum DriverStatus {
    AVAILABLE,  // Ready to take orders
    ON_ROUTE,   // Currently delivering
    OFFLINE     // Not working
}
