package com.tranhuy105.server.domain;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * A single route consisting of node IDs to visit
 */
@Data
@Builder
@AllArgsConstructor
public class Route {
    @Builder.Default
    private List<Integer> stops = new ArrayList<>();
    
    private double distance;
    private double duration;
    private double capacityViolation;
    private double timeViolation;
    private double batteryViolation;

    public Route() {
        this.stops = new ArrayList<>();
    }

    public Route(List<Integer> stops) {
        this.stops = new ArrayList<>(stops);
    }

    public boolean isFeasible() {
        return capacityViolation < 1e-6 && timeViolation < 1e-6 && batteryViolation < 1e-6;
    }

    public boolean isEmpty() {
        return stops.isEmpty();
    }

    public int size() {
        return stops.size();
    }

    /**
     * Create a deep copy
     */
    public Route copy() {
        return Route.builder()
                .stops(new ArrayList<>(stops))
                .distance(distance)
                .duration(duration)
                .capacityViolation(capacityViolation)
                .timeViolation(timeViolation)
                .batteryViolation(batteryViolation)
                .build();
    }
}
