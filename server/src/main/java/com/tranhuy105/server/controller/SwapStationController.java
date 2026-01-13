package com.tranhuy105.server.controller;

import com.tranhuy105.server.dto.SwapStationCreateRequest;
import com.tranhuy105.server.dto.SwapStationDTO;
import com.tranhuy105.server.service.SwapStationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/swap-stations")
@RequiredArgsConstructor
public class SwapStationController {
    
    private final SwapStationService stationService;
    
    @GetMapping
    public ResponseEntity<List<SwapStationDTO>> getAllStations() {
        return ResponseEntity.ok(stationService.getAllStations());
    }
    
    @GetMapping("/active")
    public ResponseEntity<List<SwapStationDTO>> getActiveStations() {
        return ResponseEntity.ok(stationService.getActiveStations());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<SwapStationDTO> getStationById(@PathVariable Long id) {
        return ResponseEntity.ok(stationService.getStationById(id));
    }
    
    @PostMapping
    public ResponseEntity<SwapStationDTO> createStation(@Valid @RequestBody SwapStationCreateRequest request) {
        return ResponseEntity.ok(stationService.createStation(request));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<SwapStationDTO> updateStation(@PathVariable Long id, @Valid @RequestBody SwapStationCreateRequest request) {
        return ResponseEntity.ok(stationService.updateStation(id, request));
    }
    
    @PutMapping("/{id}/batteries")
    public ResponseEntity<SwapStationDTO> updateBatteryCount(@PathVariable Long id, @RequestParam Integer count) {
        return ResponseEntity.ok(stationService.updateBatteryCount(id, count));
    }
    
    @PutMapping("/{id}/toggle-active")
    public ResponseEntity<SwapStationDTO> toggleActive(@PathVariable Long id) {
        return ResponseEntity.ok(stationService.toggleActive(id));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStation(@PathVariable Long id) {
        stationService.deleteStation(id);
        return ResponseEntity.noContent().build();
    }
}
