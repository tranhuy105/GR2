package com.tranhuy105.server.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Assigned route entity - represents a route assigned to a driver
 * Since Driver = Vehicle, the vehicle info is derived from the driver
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
