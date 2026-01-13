package com.tranhuy105.server.dto;

import java.util.List;

import lombok.Data;

/**
 * Response DTO for optimization result
 */
@Data
public class OptimizationResponse {
    private List<RouteDTO> routes;
    private SummaryDTO summary;
    private long computeTimeMs;
    private String chargingMode;

    @Data
    public static class RouteDTO {
        private int vehicleId;
        private List<StopDTO> stops;
        private double distance;
        private boolean feasible;
    }

    @Data
    public static class StopDTO {
        private int nodeId;
        private String stringId;
        private String type;  // CUSTOMER, STATION
        private double x;
        private double y;
    }

    @Data
    public static class SummaryDTO {
        private int totalVehicles;
        private double totalDistance;
        private double totalCost;
        private boolean feasible;
        private int totalCustomers;
        private int totalStations;
    }
}
