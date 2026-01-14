package com.tranhuy105.server.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.tranhuy105.server.entity.AssignedRoute;
import com.tranhuy105.server.entity.RouteStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteDTO {
    private Long id;
    private Long driverId;
    private String driverName;
    private String licensePlate;  // From driver's vehicle properties
    private RouteStatus status;
    private String stopsJson;
    private List<RouteStopDTO> stops;
    private Double totalDistance;
    private Double estimatedTime;
    private Integer totalStops;
    private Integer completedStops;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private List<OrderDTO> orders;
    
    public static RouteDTO fromEntity(AssignedRoute route) {
        return RouteDTO.builder()
                .id(route.getId())
                .driverId(route.getDriver().getId())
                .driverName(route.getDriver().getName())
                .licensePlate(route.getDriver().getLicensePlate())
                .status(route.getStatus())
                .stopsJson(route.getStopsJson())
                .totalDistance(route.getTotalDistance())
                .estimatedTime(route.getEstimatedTime())
                .totalStops(route.getTotalStops())
                .completedStops(route.getCompletedStops())
                .createdAt(route.getCreatedAt())
                .startedAt(route.getStartedAt())
                .completedAt(route.getCompletedAt())
                .build();
    }
}
