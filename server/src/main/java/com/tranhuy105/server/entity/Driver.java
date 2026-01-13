package com.tranhuy105.server.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

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
    
    @OneToOne
    @JoinColumn(name = "current_vehicle_id")
    private Vehicle currentVehicle;
    
    @OneToMany(mappedBy = "driver", cascade = CascadeType.ALL)
    @Builder.Default
    private List<AssignedRoute> routes = new ArrayList<>();
}
