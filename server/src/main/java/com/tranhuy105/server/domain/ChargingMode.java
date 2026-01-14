package com.tranhuy105.server.domain;

/**
 * Charging mode for electric vehicles
 */
public enum ChargingMode {
    FULL_RECHARGE,  // Takes time proportional to charge needed
    BATTERY_SWAP    // Fixed swap time
}
