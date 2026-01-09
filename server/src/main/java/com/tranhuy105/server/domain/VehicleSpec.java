package com.tranhuy105.server.domain;

import lombok.Builder;
import lombok.Data;

/**
 * Vehicle specification for the routing problem
 */
@Data
@Builder
public class VehicleSpec {
    @Builder.Default
    private double batteryCapacity = 100.0;
    
    @Builder.Default
    private double cargoCapacity = 200.0;
    
    @Builder.Default
    private double consumptionRate = 1.0;
    
    @Builder.Default
    private double refuelRate = 1.0;
    
    @Builder.Default
    private double velocity = 1.0;
    
    @Builder.Default
    private ChargingMode chargingMode = ChargingMode.FULL_RECHARGE;
    
    @Builder.Default
    private double batterySwapTime = 5.0;

    /**
     * Maximum distance reachable on full battery
     */
    public double getMaxReachableDistance() {
        return batteryCapacity / consumptionRate;
    }
}
