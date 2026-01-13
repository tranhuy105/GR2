package com.tranhuy105.server.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class RouteAssignRequest {
    @NotNull(message = "Driver ID is required")
    private Long driverId;
    
    @NotNull(message = "Vehicle ID is required")
    private Long vehicleId;
    
    @NotEmpty(message = "At least one order is required")
    private List<Long> orderIds;
    
    // Optional: custom stop sequence
    private List<RouteStopDTO> customStops;
}
