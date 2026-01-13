package com.tranhuy105.server.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VehicleCreateRequest {
    @NotBlank(message = "License plate is required")
    private String licensePlate;
    
    private Double batteryCapacity = 100.0;
    private Double currentLat;
    private Double currentLng;
}
