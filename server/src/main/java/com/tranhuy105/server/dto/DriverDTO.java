package com.tranhuy105.server.dto;

import com.tranhuy105.server.entity.Driver;
import com.tranhuy105.server.entity.DriverStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverDTO {
    private Long id;
    private String name;
    private String phone;
    private DriverStatus status;
    private Long currentVehicleId;
    private String currentVehiclePlate;
    
    public static DriverDTO fromEntity(Driver driver) {
        return DriverDTO.builder()
                .id(driver.getId())
                .name(driver.getName())
                .phone(driver.getPhone())
                .status(driver.getStatus())
                .currentVehicleId(driver.getCurrentVehicle() != null ? driver.getCurrentVehicle().getId() : null)
                .currentVehiclePlate(driver.getCurrentVehicle() != null ? driver.getCurrentVehicle().getLicensePlate() : null)
                .build();
    }
}
