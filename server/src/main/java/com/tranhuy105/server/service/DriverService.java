package com.tranhuy105.server.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tranhuy105.server.dto.DriverCreateRequest;
import com.tranhuy105.server.dto.DriverDTO;
import com.tranhuy105.server.entity.Driver;
import com.tranhuy105.server.entity.DriverStatus;
import com.tranhuy105.server.repository.DriverRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DriverService {
    
    private final DriverRepository driverRepository;
    
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
                .licensePlate(request.getLicensePlate())
                .batteryCapacity(request.getBatteryCapacity() != null ? request.getBatteryCapacity() : 100.0)
                .loadCapacity(request.getLoadCapacity() != null ? request.getLoadCapacity() : 50.0)
                .status(DriverStatus.OFFLINE)
                .build();
        
        return DriverDTO.fromEntity(driverRepository.save(driver));
    }
    
    @Transactional
    public DriverDTO updateDriver(Long id, DriverCreateRequest request) {
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Driver not found: " + id));
        
        driver.setName(request.getName());
        driver.setPhone(request.getPhone());
        
        if (request.getLicensePlate() != null) {
            driver.setLicensePlate(request.getLicensePlate());
        }
        if (request.getBatteryCapacity() != null) {
            driver.setBatteryCapacity(request.getBatteryCapacity());
        }
        if (request.getLoadCapacity() != null) {
            driver.setLoadCapacity(request.getLoadCapacity());
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
