package com.tranhuy105.server.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Delivery order entity
 */
@Entity
@Table(name = "delivery_orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryOrder {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // Customer info
    @Column(nullable = false)
    private String customerName;
    
    @Column(nullable = false)
    private String customerPhone;
    
    // EVRPTW: Customer location (single point - delivery from depot)
    @Column(nullable = false)
    private Double lat;
    
    @Column(nullable = false)
    private Double lng;
    
    @Column(nullable = false)
    private String address;
    
    // Time window (in hours from midnight, e.g., 9.5 = 9:30 AM)
    @Column(nullable = false)
    private Double timeWindowStart;
    
    @Column(nullable = false)
    private Double timeWindowEnd;
    
    // Demand (weight/volume)
    @Builder.Default
    private Double demand = 1.0;
    
    // Service time at pickup and delivery (in hours)
    @Builder.Default
    private Double serviceTime = 0.1; // 6 minutes
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;
    
    // Assigned route reference
    @ManyToOne
    @JoinColumn(name = "assigned_route_id")
    private AssignedRoute assignedRoute;
    
    // Timestamps
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    private LocalDateTime completedAt;
    
    // Notes
    @Column(length = 500)
    private String notes;
}
