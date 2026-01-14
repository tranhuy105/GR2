package com.tranhuy105.server.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RouteAssignRequest {
    @NotNull(message = "Driver ID is required")
    private Long driverId;
    
    @NotEmpty(message = "At least one order is required")
    private List<Long> orderIds;
    
    // Optional: custom stop sequence
    private List<RouteStopDTO> customStops;
}
