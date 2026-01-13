package com.tranhuy105.server.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplyOptimizationRequest {
    
    @NotEmpty(message = "Routes must not be empty")
    private List<OptimizedRouteDTO> routes;
    
    @NotEmpty(message = "Order IDs must not be empty")
    private List<Long> orderIds;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptimizedRouteDTO {
        private int vehicleId;
        private List<StopDTO> stops;
        private double totalDistance;
        private double totalCost;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StopDTO {
        private int nodeId;
        private String type;
        private double x;
        private double y;
        private double arrivalTime;
        private double departureTime;
        private double chargeOnArrival;
        private double chargeOnDeparture;
    }
}
