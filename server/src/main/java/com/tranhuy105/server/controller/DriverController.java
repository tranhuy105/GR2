package com.tranhuy105.server.controller;

import com.tranhuy105.server.dto.DriverCreateRequest;
import com.tranhuy105.server.dto.DriverDTO;
import com.tranhuy105.server.entity.DriverStatus;
import com.tranhuy105.server.service.DriverService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/drivers")
@RequiredArgsConstructor
public class DriverController {
    
    private final DriverService driverService;
    
    @GetMapping
    public ResponseEntity<List<DriverDTO>> getAllDrivers() {
        return ResponseEntity.ok(driverService.getAllDrivers());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<DriverDTO> getDriverById(@PathVariable Long id) {
        return ResponseEntity.ok(driverService.getDriverById(id));
    }
    
    @GetMapping("/available")
    public ResponseEntity<List<DriverDTO>> getAvailableDrivers() {
        return ResponseEntity.ok(driverService.getAvailableDrivers());
    }
    
    @PostMapping
    public ResponseEntity<DriverDTO> createDriver(@Valid @RequestBody DriverCreateRequest request) {
        return ResponseEntity.ok(driverService.createDriver(request));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<DriverDTO> updateDriver(@PathVariable Long id, @Valid @RequestBody DriverCreateRequest request) {
        return ResponseEntity.ok(driverService.updateDriver(id, request));
    }
    
    @PutMapping("/{id}/status")
    public ResponseEntity<DriverDTO> updateStatus(@PathVariable Long id, @RequestParam DriverStatus status) {
        return ResponseEntity.ok(driverService.updateStatus(id, status));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDriver(@PathVariable Long id) {
        driverService.deleteDriver(id);
        return ResponseEntity.noContent().build();
    }
}
