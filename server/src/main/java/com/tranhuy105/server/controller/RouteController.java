package com.tranhuy105.server.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tranhuy105.server.dto.ApplyOptimizationRequest;
import com.tranhuy105.server.dto.RouteAssignRequest;
import com.tranhuy105.server.dto.RouteDTO;
import com.tranhuy105.server.entity.User;
import com.tranhuy105.server.service.RouteService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
public class RouteController {
    
    private final RouteService routeService;
    
    @GetMapping
    public ResponseEntity<List<RouteDTO>> getAllRoutes() {
        return ResponseEntity.ok(routeService.getAllRoutes());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<RouteDTO> getRouteById(@PathVariable Long id) {
        return ResponseEntity.ok(routeService.getRouteById(id));
    }
    
    @GetMapping("/driver/{driverId}")
    public ResponseEntity<List<RouteDTO>> getRoutesByDriver(@PathVariable Long driverId) {
        return ResponseEntity.ok(routeService.getRoutesByDriver(driverId));
    }
    
    @GetMapping("/my-routes")
    public ResponseEntity<List<RouteDTO>> getMyRoutes(@AuthenticationPrincipal User user) {
        if (user.getDriver() == null) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(routeService.getRoutesByDriver(user.getDriver().getId()));
    }
    
    @GetMapping("/my-routes/active")
    public ResponseEntity<List<RouteDTO>> getMyActiveRoutes(@AuthenticationPrincipal User user) {
        if (user.getDriver() == null) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(routeService.getActiveRoutesByDriver(user.getDriver().getId()));
    }
    
    @PostMapping("/assign")
    public ResponseEntity<RouteDTO> assignRoute(@Valid @RequestBody RouteAssignRequest request) {
        return ResponseEntity.ok(routeService.assignRoute(request));
    }
    
    @PostMapping("/apply-optimization")
    public ResponseEntity<List<RouteDTO>> applyOptimization(@Valid @RequestBody ApplyOptimizationRequest request) {
        return ResponseEntity.ok(routeService.applyOptimization(request));
    }
    
    @PutMapping("/{id}/start")
    public ResponseEntity<RouteDTO> startRoute(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(routeService.startRoute(id, user));
    }
    
    @PutMapping("/{id}/complete-stop")
    public ResponseEntity<RouteDTO> completeStop(@PathVariable Long id, @RequestParam Integer stopSequence) {
        return ResponseEntity.ok(routeService.completeStop(id, stopSequence));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoute(@PathVariable Long id) {
        routeService.deleteRoute(id);
        return ResponseEntity.noContent().build();
    }
}
