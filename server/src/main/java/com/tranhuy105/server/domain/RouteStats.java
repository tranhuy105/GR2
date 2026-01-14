package com.tranhuy105.server.domain;

/**
 * Statistics for a single route
 */
public record RouteStats(
    double cost,
    double distance,
    double capacityViolation,
    double timeViolation,
    double batteryViolation
) {
    public double totalViolation() {
        return capacityViolation + timeViolation + batteryViolation;
    }

    public boolean isFeasible() {
        return totalViolation() < 1e-6;
    }
}
