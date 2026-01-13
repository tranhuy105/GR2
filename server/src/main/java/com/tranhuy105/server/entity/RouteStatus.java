package com.tranhuy105.server.entity;

/**
 * Status of an assigned route
 */
public enum RouteStatus {
    PLANNED,        // Route created but not started
    IN_PROGRESS,    // Driver is on this route
    COMPLETED,      // All stops completed
    CANCELLED       // Route cancelled
}
