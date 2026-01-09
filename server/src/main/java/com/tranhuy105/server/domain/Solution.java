package com.tranhuy105.server.domain;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * A complete solution with multiple routes
 */
@Data
@Builder
@AllArgsConstructor
public class Solution {
    @Builder.Default
    private List<Route> routes = new ArrayList<>();
    
    private double cost;
    private double totalDistance;
    private double totalViolations;

    public Solution() {
        this.routes = new ArrayList<>();
    }

    public boolean isFeasible() {
        return totalViolations < 1e-6;
    }

    public int getVehicleCount() {
        return routes.size();
    }

    /**
     * Create a deep copy
     */
    public Solution copy() {
        List<Route> copiedRoutes = new ArrayList<>(routes.size());
        for (Route route : routes) {
            copiedRoutes.add(route.copy());
        }
        return Solution.builder()
                .routes(copiedRoutes)
                .cost(cost)
                .totalDistance(totalDistance)
                .totalViolations(totalViolations)
                .build();
    }

    /**
     * Get all routes as list of node ID lists (for compatibility)
     */
    public List<List<Integer>> getRoutesAsLists() {
        return routes.stream()
                .map(Route::getStops)
                .toList();
    }
}
