package com.tranhuy105.server.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Assigned route entity - represents a route assigned to a driver
 */
@Entity
@Table(name = "assigned_routes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignedRoute {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "driver_id", nullable = false)
    private Driver driver;
    
    @ManyToOne
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RouteStatus status = RouteStatus.PLANNED;
    
    // Route stops stored as JSON string
    // Format: [{"type": "PICKUP/DELIVERY/SWAP", "orderId": 1, "lat": 21.0, "lng": 105.8, "address": "...", "sequence": 1}]
    @Column(columnDefinition = "TEXT")
    private String stopsJson;
    
    @OneToMany(mappedBy = "assignedRoute", cascade = CascadeType.ALL)
    @Builder.Default
    private List<DeliveryOrder> orders = new ArrayList<>();
    
    // Route metrics
    private Double totalDistance;
    private Double estimatedTime;
    private Integer totalStops;
    private Integer completedStops;
    
    // Timestamps
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}
