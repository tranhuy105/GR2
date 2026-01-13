package com.tranhuy105.server.entity;

/**
 * User roles for authorization
 */
public enum Role {
    ADMIN,      // Full access
    MANAGER,    // Manage orders, drivers, view routes
    DRIVER      // View assigned routes only
}
