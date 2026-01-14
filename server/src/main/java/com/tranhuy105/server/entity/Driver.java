package com.tranhuy105.server.entity;

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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Driver entity representing a delivery driver
 */
@Entity
@Table(name = "drivers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Driver {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false)
    private String phone;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private DriverStatus status = DriverStatus.OFFLINE;
    
    @Column(unique = true)
    private String licensePlate;
    
    @Column(nullable = false)
    @Builder.Default
    private Double batteryCapacity = 100.0;  // Max battery (kWh or units)
    
    @Column(nullable = false)
    @Builder.Default
    private Double loadCapacity = 50.0;  // Cargo capacity (kg)
    
    @OneToMany(mappedBy = "driver", cascade = CascadeType.ALL)
    @Builder.Default
    private List<AssignedRoute> routes = new ArrayList<>();
}
