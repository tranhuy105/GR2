package com.tranhuy105.server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Vehicle entity representing an electric scooter
 */
@Entity
@Table(name = "vehicles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Vehicle {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String licensePlate;
    
    @Column(nullable = false)
    @Builder.Default
    private Double batteryLevel = 100.0;
    
    @Column(nullable = false)
    @Builder.Default
    private Double batteryCapacity = 100.0;
    
    @Column(nullable = false)
    @Builder.Default
    private Double loadCapacity = 200.0;  // Vehicle load capacity (C in EVRPTW)
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private VehicleStatus status = VehicleStatus.AVAILABLE;
    
    @OneToOne(mappedBy = "currentVehicle")
    private Driver currentDriver;
    
    // Current location
    private Double currentLat;
    private Double currentLng;
}
