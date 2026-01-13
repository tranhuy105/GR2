package com.tranhuy105.server.repository;

import com.tranhuy105.server.entity.SwapStation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SwapStationRepository extends JpaRepository<SwapStation, Long> {
    List<SwapStation> findByIsActiveTrue();
    
    @Query("SELECT s FROM SwapStation s WHERE s.availableBatteries > 0 AND s.isActive = true")
    List<SwapStation> findAvailableStations();
}
