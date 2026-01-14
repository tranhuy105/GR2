package com.tranhuy105.server.dto;

import com.tranhuy105.server.entity.SwapStation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SwapStationDTO {
    private Long id;
    private String name;
    private String address;
    private Double lat;
    private Double lng;
    private Integer availableBatteries;
    private Integer totalSlots;
    private Double openTime;
    private Double closeTime;
    private Boolean isActive;
    
    public static SwapStationDTO fromEntity(SwapStation station) {
        return SwapStationDTO.builder()
                .id(station.getId())
                .name(station.getName())
                .address(station.getAddress())
                .lat(station.getLat())
                .lng(station.getLng())
                .availableBatteries(station.getAvailableBatteries())
                .totalSlots(station.getTotalSlots())
                .openTime(station.getOpenTime())
                .closeTime(station.getCloseTime())
                .isActive(station.getIsActive())
                .build();
    }
}
