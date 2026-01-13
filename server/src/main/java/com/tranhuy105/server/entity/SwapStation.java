package com.tranhuy105.server.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Battery swap station entity
 */
@Entity
@Table(name = "swap_stations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SwapStation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false)
    private String address;
    
    @Column(nullable = false)
    private Double lat;
    
    @Column(nullable = false)
    private Double lng;
    
    @Column(nullable = false)
    @Builder.Default
    private Integer availableBatteries = 10;
    
    @Column(nullable = false)
    @Builder.Default
    private Integer totalSlots = 20;
    
    // Operating hours
    @Builder.Default
    private Double openTime = 6.0;  // 6:00 AM
    
    @Builder.Default
    private Double closeTime = 22.0; // 10:00 PM
    
    @Builder.Default
    private Boolean isActive = true;
}
