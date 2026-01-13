package com.tranhuy105.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteStopDTO {
    private Integer sequence;
    private String type; // PICKUP, DELIVERY, SWAP_STATION, DEPOT
    private Long orderId;
    private Long stationId;
    private Double lat;
    private Double lng;
    private String address;
    private String customerName;
    private Boolean completed;
}
