package com.tranhuy105.server.entity;

/**
 * Status of a vehicle (electric scooter)
 */
public enum VehicleStatus {
    AVAILABLE,  // Ready to use
    IN_USE,     // Currently being used
    CHARGING    // At charging/swap station
}
