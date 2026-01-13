package com.tranhuy105.server.service;

import com.tranhuy105.server.dto.VehicleCreateRequest;
import com.tranhuy105.server.dto.VehicleDTO;
import com.tranhuy105.server.entity.Vehicle;
import com.tranhuy105.server.entity.VehicleStatus;
import com.tranhuy105.server.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleService {
    
    private final VehicleRepository vehicleRepository;
    
    public List<VehicleDTO> getAllVehicles() {
        return vehicleRepository.findAll().stream()
                .map(VehicleDTO::fromEntity)
                .toList();
    }
    
    public VehicleDTO getVehicleById(Long id) {
        return vehicleRepository.findById(id)
                .map(VehicleDTO::fromEntity)
                .orElseThrow(() -> new RuntimeException("Vehicle not found: " + id));
    }
    
    @Transactional
    public VehicleDTO createVehicle(VehicleCreateRequest request) {
        Vehicle vehicle = Vehicle.builder()
                .licensePlate(request.getLicensePlate())
                .batteryCapacity(request.getBatteryCapacity())
                .batteryLevel(request.getBatteryCapacity())
                .status(VehicleStatus.AVAILABLE)
                .currentLat(request.getCurrentLat())
                .currentLng(request.getCurrentLng())
                .build();
        
        return VehicleDTO.fromEntity(vehicleRepository.save(vehicle));
    }
    
    @Transactional
    public VehicleDTO updateVehicle(Long id, VehicleCreateRequest request) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehicle not found: " + id));
        
        vehicle.setLicensePlate(request.getLicensePlate());
        vehicle.setBatteryCapacity(request.getBatteryCapacity());
        if (request.getCurrentLat() != null) {
            vehicle.setCurrentLat(request.getCurrentLat());
        }
        if (request.getCurrentLng() != null) {
            vehicle.setCurrentLng(request.getCurrentLng());
        }
        
        return VehicleDTO.fromEntity(vehicleRepository.save(vehicle));
    }
    
    @Transactional
    public VehicleDTO updateBatteryLevel(Long id, Double batteryLevel) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehicle not found: " + id));
        vehicle.setBatteryLevel(batteryLevel);
        return VehicleDTO.fromEntity(vehicleRepository.save(vehicle));
    }
    
    @Transactional
    public VehicleDTO updateStatus(Long id, VehicleStatus status) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehicle not found: " + id));
        vehicle.setStatus(status);
        return VehicleDTO.fromEntity(vehicleRepository.save(vehicle));
    }
    
    @Transactional
    public void deleteVehicle(Long id) {
        vehicleRepository.deleteById(id);
    }
    
    public List<VehicleDTO> getAvailableVehicles() {
        return vehicleRepository.findByStatus(VehicleStatus.AVAILABLE).stream()
                .map(VehicleDTO::fromEntity)
                .toList();
    }
}
