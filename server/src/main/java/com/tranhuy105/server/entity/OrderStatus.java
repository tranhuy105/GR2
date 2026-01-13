package com.tranhuy105.server.entity;

/**
 * Status of a delivery order
 */
public enum OrderStatus {
    PENDING,        // Waiting to be assigned
    ASSIGNED,       // Assigned to a route
    IN_PROGRESS,    // Being delivered
    COMPLETED,      // Successfully delivered
    CANCELLED       // Order cancelled
}
