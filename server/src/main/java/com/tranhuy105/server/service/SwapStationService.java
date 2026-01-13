package com.tranhuy105.server.service;

import com.tranhuy105.server.dto.SwapStationCreateRequest;
import com.tranhuy105.server.dto.SwapStationDTO;
import com.tranhuy105.server.entity.SwapStation;
import com.tranhuy105.server.repository.SwapStationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SwapStationService {
    
    private final SwapStationRepository stationRepository;
    
    public List<SwapStationDTO> getAllStations() {
        return stationRepository.findAll().stream()
                .map(SwapStationDTO::fromEntity)
                .toList();
    }
    
    public List<SwapStationDTO> getActiveStations() {
        return stationRepository.findByIsActiveTrue().stream()
                .map(SwapStationDTO::fromEntity)
                .toList();
    }
    
    public SwapStationDTO getStationById(Long id) {
        return stationRepository.findById(id)
                .map(SwapStationDTO::fromEntity)
                .orElseThrow(() -> new RuntimeException("Station not found: " + id));
    }
    
    @Transactional
    public SwapStationDTO createStation(SwapStationCreateRequest request) {
        SwapStation station = SwapStation.builder()
                .name(request.getName())
                .address(request.getAddress())
                .lat(request.getLat())
                .lng(request.getLng())
                .totalSlots(request.getTotalSlots())
                .availableBatteries(request.getAvailableBatteries())
                .openTime(request.getOpenTime())
                .closeTime(request.getCloseTime())
                .isActive(true)
                .build();
        
        return SwapStationDTO.fromEntity(stationRepository.save(station));
    }
    
    @Transactional
    public SwapStationDTO updateStation(Long id, SwapStationCreateRequest request) {
        SwapStation station = stationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Station not found: " + id));
        
        station.setName(request.getName());
        station.setAddress(request.getAddress());
        station.setLat(request.getLat());
        station.setLng(request.getLng());
        station.setTotalSlots(request.getTotalSlots());
        station.setAvailableBatteries(request.getAvailableBatteries());
        station.setOpenTime(request.getOpenTime());
        station.setCloseTime(request.getCloseTime());
        
        return SwapStationDTO.fromEntity(stationRepository.save(station));
    }
    
    @Transactional
    public SwapStationDTO updateBatteryCount(Long id, Integer availableBatteries) {
        SwapStation station = stationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Station not found: " + id));
        station.setAvailableBatteries(availableBatteries);
        return SwapStationDTO.fromEntity(stationRepository.save(station));
    }
    
    @Transactional
    public SwapStationDTO toggleActive(Long id) {
        SwapStation station = stationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Station not found: " + id));
        station.setIsActive(!station.getIsActive());
        return SwapStationDTO.fromEntity(stationRepository.save(station));
    }
    
    @Transactional
    public void deleteStation(Long id) {
        stationRepository.deleteById(id);
    }
}
