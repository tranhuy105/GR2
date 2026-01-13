package com.tranhuy105.server.dto;

import com.tranhuy105.server.entity.Vehicle;
import com.tranhuy105.server.entity.VehicleStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleDTO {
    private Long id;
    private String licensePlate;
    private Double batteryLevel;
    private Double batteryCapacity;
    private VehicleStatus status;
    private Long currentDriverId;
    private String currentDriverName;
    private Double currentLat;
    private Double currentLng;
    
    public static VehicleDTO fromEntity(Vehicle vehicle) {
        return VehicleDTO.builder()
                .id(vehicle.getId())
                .licensePlate(vehicle.getLicensePlate())
                .batteryLevel(vehicle.getBatteryLevel())
                .batteryCapacity(vehicle.getBatteryCapacity())
                .status(vehicle.getStatus())
                .currentDriverId(vehicle.getCurrentDriver() != null ? vehicle.getCurrentDriver().getId() : null)
                .currentDriverName(vehicle.getCurrentDriver() != null ? vehicle.getCurrentDriver().getName() : null)
                .currentLat(vehicle.getCurrentLat())
                .currentLng(vehicle.getCurrentLng())
                .build();
    }
}
