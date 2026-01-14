package com.tranhuy105.server.dto;

import java.util.List;

import com.tranhuy105.server.domain.ChargingMode;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

/**
 * Request for optimizing fleet routes from database orders
 */
@Data
public class FleetOptimizationRequest {
    
    @NotEmpty(message = "At least one order ID is required")
    private List<Long> orderIds;
    
    private List<Long> driverIds;  // Optional: available drivers for this optimization
    
    private List<Long> stationIds;
    
    private ChargingMode chargingMode = ChargingMode.BATTERY_SWAP;
    
    private Double batterySwapTime = 0.083; // 5 minutes
    
    private Double batteryCapacity = 100.0;
    
    private Double consumptionRate = 1.0;
    
    private Double velocity = 30.0; // km/h
    
    private Double cargoCapacity = 50.0;
    
    private Integer iterations;
    
    private Double timeLimit;
    
    private Boolean parallel = true;
    
    // Depot location (default: Hanoi center)
    private Double depotLat = 21.0285;
    private Double depotLng = 105.8542;
}
