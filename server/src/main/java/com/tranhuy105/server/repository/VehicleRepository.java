package com.tranhuy105.server.repository;

import com.tranhuy105.server.entity.Vehicle;
import com.tranhuy105.server.entity.VehicleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    Optional<Vehicle> findByLicensePlate(String licensePlate);
    List<Vehicle> findByStatus(VehicleStatus status);
    List<Vehicle> findByCurrentDriverIsNull();
}
