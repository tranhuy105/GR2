package com.tranhuy105.server.repository;

import com.tranhuy105.server.entity.Driver;
import com.tranhuy105.server.entity.DriverStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DriverRepository extends JpaRepository<Driver, Long> {
    List<Driver> findByStatus(DriverStatus status);
    List<Driver> findByCurrentVehicleIsNotNull();
}
