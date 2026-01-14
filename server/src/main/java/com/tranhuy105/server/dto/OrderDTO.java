package com.tranhuy105.server.dto;

import java.time.LocalDateTime;

import com.tranhuy105.server.entity.DeliveryOrder;
import com.tranhuy105.server.entity.OrderStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {
    private Long id;
    private String customerName;
    private String customerPhone;
    // EVRPTW: Single customer location
    private Double lat;
    private Double lng;
    private String address;
    private Double timeWindowStart;
    private Double timeWindowEnd;
    private Double demand;
    private Double serviceTime;
    private OrderStatus status;
    private Long assignedRouteId;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private String notes;
    
    public static OrderDTO fromEntity(DeliveryOrder order) {
        return OrderDTO.builder()
                .id(order.getId())
                .customerName(order.getCustomerName())
                .customerPhone(order.getCustomerPhone())
                .lat(order.getLat())
                .lng(order.getLng())
                .address(order.getAddress())
                .timeWindowStart(order.getTimeWindowStart())
                .timeWindowEnd(order.getTimeWindowEnd())
                .demand(order.getDemand())
                .serviceTime(order.getServiceTime())
                .status(order.getStatus())
                .assignedRouteId(order.getAssignedRoute() != null ? order.getAssignedRoute().getId() : null)
                .createdAt(order.getCreatedAt())
                .completedAt(order.getCompletedAt())
                .notes(order.getNotes())
                .build();
    }
}
