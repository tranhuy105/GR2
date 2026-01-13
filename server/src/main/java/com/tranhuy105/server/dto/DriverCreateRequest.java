package com.tranhuy105.server.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DriverCreateRequest {
    @NotBlank(message = "Name is required")
    private String name;
    
    @NotBlank(message = "Phone is required")
    private String phone;
    
    private Long vehicleId;
}
