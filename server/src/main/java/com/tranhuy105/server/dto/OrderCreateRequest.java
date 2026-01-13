package com.tranhuy105.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderCreateRequest {
    @NotBlank(message = "Customer name is required")
    private String customerName;
    
    @NotBlank(message = "Customer phone is required")
    private String customerPhone;
    
    // EVRPTW: Single customer location
    @NotNull(message = "Latitude is required")
    private Double lat;
    
    @NotNull(message = "Longitude is required")
    private Double lng;
    
    @NotBlank(message = "Address is required")
    private String address;
    
    @NotNull(message = "Time window start is required")
    private Double timeWindowStart;
    
    @NotNull(message = "Time window end is required")
    private Double timeWindowEnd;
    
    private Double demand = 1.0;
    private Double serviceTime = 0.1;
    private String notes;
}
