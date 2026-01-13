package com.tranhuy105.server.service;

import com.tranhuy105.server.dto.DriverCreateRequest;
import com.tranhuy105.server.dto.DriverDTO;
import com.tranhuy105.server.entity.Driver;
import com.tranhuy105.server.entity.DriverStatus;
import com.tranhuy105.server.entity.Vehicle;
import com.tranhuy105.server.repository.DriverRepository;
import com.tranhuy105.server.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DriverService {
    
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    
    public List<DriverDTO> getAllDrivers() {
        return driverRepository.findAll().stream()
                .map(DriverDTO::fromEntity)
                .toList();
    }
    
    public DriverDTO getDriverById(Long id) {
        return driverRepository.findById(id)
                .map(DriverDTO::fromEntity)
                .orElseThrow(() -> new RuntimeException("Driver not found: " + id));
    }
    
    @Transactional
    public DriverDTO createDriver(DriverCreateRequest request) {
        Driver driver = Driver.builder()
                .name(request.getName())
                .phone(request.getPhone())
                .status(DriverStatus.OFFLINE)
                .build();
        
        if (request.getVehicleId() != null) {
            Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                    .orElseThrow(() -> new RuntimeException("Vehicle not found: " + request.getVehicleId()));
            driver.setCurrentVehicle(vehicle);
        }
        
        return DriverDTO.fromEntity(driverRepository.save(driver));
    }
    
    @Transactional
    public DriverDTO updateDriver(Long id, DriverCreateRequest request) {
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Driver not found: " + id));
        
        driver.setName(request.getName());
        driver.setPhone(request.getPhone());
        
        if (request.getVehicleId() != null) {
            Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                    .orElseThrow(() -> new RuntimeException("Vehicle not found: " + request.getVehicleId()));
            driver.setCurrentVehicle(vehicle);
        } else {
            driver.setCurrentVehicle(null);
        }
        
        return DriverDTO.fromEntity(driverRepository.save(driver));
    }
    
    @Transactional
    public DriverDTO updateStatus(Long id, DriverStatus status) {
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Driver not found: " + id));
        driver.setStatus(status);
        return DriverDTO.fromEntity(driverRepository.save(driver));
    }
    
    @Transactional
    public void deleteDriver(Long id) {
        driverRepository.deleteById(id);
    }
    
    public List<DriverDTO> getAvailableDrivers() {
        return driverRepository.findByStatus(DriverStatus.AVAILABLE).stream()
                .map(DriverDTO::fromEntity)
                .toList();
    }
}
