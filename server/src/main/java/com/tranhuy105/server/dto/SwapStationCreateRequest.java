package com.tranhuy105.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SwapStationCreateRequest {
    @NotBlank(message = "Name is required")
    private String name;
    
    @NotBlank(message = "Address is required")
    private String address;
    
    @NotNull(message = "Latitude is required")
    private Double lat;
    
    @NotNull(message = "Longitude is required")
    private Double lng;
    
    private Integer totalSlots = 20;
    private Integer availableBatteries = 10;
    private Double openTime = 6.0;
    private Double closeTime = 22.0;
}
