package com.tranhuy105.server.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tranhuy105.server.dto.ApplyOptimizationRequest;
import com.tranhuy105.server.dto.OrderDTO;
import com.tranhuy105.server.dto.RouteAssignRequest;
import com.tranhuy105.server.dto.RouteDTO;
import com.tranhuy105.server.dto.RouteStopDTO;
import com.tranhuy105.server.entity.AssignedRoute;
import com.tranhuy105.server.entity.DeliveryOrder;
import com.tranhuy105.server.entity.Driver;
import com.tranhuy105.server.entity.DriverStatus;
import com.tranhuy105.server.entity.OrderStatus;
import com.tranhuy105.server.entity.RouteStatus;
import com.tranhuy105.server.entity.Vehicle;
import com.tranhuy105.server.entity.VehicleStatus;
import com.tranhuy105.server.repository.AssignedRouteRepository;
import com.tranhuy105.server.repository.DeliveryOrderRepository;
import com.tranhuy105.server.repository.DriverRepository;
import com.tranhuy105.server.repository.VehicleRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RouteService {
    
    private final AssignedRouteRepository routeRepository;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final DeliveryOrderRepository orderRepository;
    private final ObjectMapper objectMapper;
    
    public List<RouteDTO> getAllRoutes() {
        return routeRepository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }
    
    public RouteDTO getRouteById(Long id) {
        return routeRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new RuntimeException("Route not found: " + id));
    }
    
    public List<RouteDTO> getRoutesByDriver(Long driverId) {
        return routeRepository.findByDriverId(driverId).stream()
                .map(this::toDTO)
                .toList();
    }
    
    public List<RouteDTO> getActiveRoutesByDriver(Long driverId) {
        return routeRepository.findByDriverIdAndStatus(driverId, RouteStatus.IN_PROGRESS).stream()
                .map(this::toDTO)
                .toList();
    }
    
    @Transactional
    public RouteDTO assignRoute(RouteAssignRequest request) {
        Driver driver = driverRepository.findById(request.getDriverId())
                .orElseThrow(() -> new RuntimeException("Driver not found: " + request.getDriverId()));
        
        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new RuntimeException("Vehicle not found: " + request.getVehicleId()));
        
        List<DeliveryOrder> orders = orderRepository.findAllById(request.getOrderIds());
        if (orders.size() != request.getOrderIds().size()) {
            throw new RuntimeException("Some orders not found");
        }
        
        // Build stops from orders - EVRPTW: single customer location
        List<RouteStopDTO> stops = new ArrayList<>();
        int sequence = 1;
        
        for (DeliveryOrder order : orders) {
            // Customer stop (EVRPTW: delivery from depot to customer)
            stops.add(RouteStopDTO.builder()
                    .sequence(sequence++)
                    .type("CUSTOMER")
                    .orderId(order.getId())
                    .lat(order.getLat())
                    .lng(order.getLng())
                    .address(order.getAddress())
                    .customerName(order.getCustomerName())
                    .completed(false)
                    .build());
        }
        
        // Use custom stops if provided
        if (request.getCustomStops() != null && !request.getCustomStops().isEmpty()) {
            stops = request.getCustomStops();
        }
        
        String stopsJson;
        try {
            stopsJson = objectMapper.writeValueAsString(stops);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize stops", e);
        }
        
        AssignedRoute route = AssignedRoute.builder()
                .driver(driver)
                .vehicle(vehicle)
                .status(RouteStatus.PLANNED)
                .stopsJson(stopsJson)
                .totalStops(stops.size())
                .completedStops(0)
                .createdAt(LocalDateTime.now())
                .build();
        
        route = routeRepository.save(route);
        
        // Update orders
        for (DeliveryOrder order : orders) {
            order.setAssignedRoute(route);
            order.setStatus(OrderStatus.ASSIGNED);
        }
        orderRepository.saveAll(orders);
        
        return toDTO(route);
    }
    
    @Transactional
    public RouteDTO startRoute(Long id, com.tranhuy105.server.entity.User user) {
        AssignedRoute route = routeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Route not found: " + id));
        
        // Security check: only the assigned driver, admin, or manager can start the route
        boolean isAdmin = user.getRole() == com.tranhuy105.server.entity.Role.ADMIN 
                       || user.getRole() == com.tranhuy105.server.entity.Role.MANAGER;
        boolean isAssignedDriver = user.getDriver() != null 
                                && route.getDriver().getId().equals(user.getDriver().getId());
        
        if (!isAdmin && !isAssignedDriver) {
            throw new RuntimeException("Bạn không có quyền bắt đầu lộ trình này");
        }
        
        route.setStatus(RouteStatus.IN_PROGRESS);
        route.setStartedAt(LocalDateTime.now());
        
        // Update driver and vehicle status
        route.getDriver().setStatus(DriverStatus.ON_ROUTE);
        route.getVehicle().setStatus(VehicleStatus.IN_USE);
        
        // Update orders
        for (DeliveryOrder order : route.getOrders()) {
            order.setStatus(OrderStatus.IN_PROGRESS);
        }
        
        return toDTO(routeRepository.save(route));
    }
    
    @Transactional
    public RouteDTO completeStop(Long routeId, Integer stopSequence) {
        AssignedRoute route = routeRepository.findById(routeId)
                .orElseThrow(() -> new RuntimeException("Route not found: " + routeId));
        
        try {
            List<RouteStopDTO> stops = objectMapper.readValue(
                    route.getStopsJson(), 
                    new TypeReference<List<RouteStopDTO>>() {}
            );
            
            for (RouteStopDTO stop : stops) {
                if (stop.getSequence().equals(stopSequence)) {
                    stop.setCompleted(true);
                    break;
                }
            }
            
            route.setStopsJson(objectMapper.writeValueAsString(stops));
            route.setCompletedStops(route.getCompletedStops() + 1);
            
            // Check if all stops completed
            if (route.getCompletedStops() >= route.getTotalStops()) {
                route.setStatus(RouteStatus.COMPLETED);
                route.setCompletedAt(LocalDateTime.now());
                route.getDriver().setStatus(DriverStatus.AVAILABLE);
                route.getVehicle().setStatus(VehicleStatus.AVAILABLE);
                
                for (DeliveryOrder order : route.getOrders()) {
                    order.setStatus(OrderStatus.COMPLETED);
                    order.setCompletedAt(LocalDateTime.now());
                }
            }
            
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to process stops", e);
        }
        
        return toDTO(routeRepository.save(route));
    }
    
    @Transactional
    public void deleteRoute(Long id) {
        AssignedRoute route = routeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Route not found: " + id));
        
        // Reset orders
        for (DeliveryOrder order : route.getOrders()) {
            order.setAssignedRoute(null);
            order.setStatus(OrderStatus.PENDING);
        }
        orderRepository.saveAll(route.getOrders());
        
        routeRepository.deleteById(id);
    }
    
    @Transactional
    public List<RouteDTO> applyOptimization(ApplyOptimizationRequest request) {
        // Get available drivers and vehicles
        List<Driver> availableDrivers = driverRepository.findByStatus(DriverStatus.AVAILABLE);
        List<Vehicle> availableVehicles = vehicleRepository.findByStatus(VehicleStatus.AVAILABLE);
        
        if (availableDrivers.isEmpty()) {
            throw new RuntimeException("Không có tài xế khả dụng để gán lộ trình");
        }
        if (availableVehicles.isEmpty()) {
            throw new RuntimeException("Không có xe khả dụng để gán lộ trình");
        }
        
        // Get orders
        List<DeliveryOrder> orders = orderRepository.findAllById(request.getOrderIds());
        
        List<RouteDTO> createdRoutes = new ArrayList<>();
        int driverIndex = 0;
        int vehicleIndex = 0;
        
        for (ApplyOptimizationRequest.OptimizedRouteDTO optRoute : request.getRoutes()) {
            if (optRoute.getStops() == null || optRoute.getStops().isEmpty()) {
                continue;
            }
            
            // Get next available driver and vehicle (round-robin)
            Driver driver = availableDrivers.get(driverIndex % availableDrivers.size());
            Vehicle vehicle = availableVehicles.get(vehicleIndex % availableVehicles.size());
            driverIndex++;
            vehicleIndex++;
            
            // Build stops from optimization result
            List<RouteStopDTO> stops = new ArrayList<>();
            List<DeliveryOrder> routeOrders = new ArrayList<>();
            int sequence = 1;
            
            for (ApplyOptimizationRequest.StopDTO stopData : optRoute.getStops()) {
                // Skip depot nodes (usually id 0 or negative)
                if (stopData.getNodeId() <= 0) {
                    continue;
                }
                
                // Find corresponding order by matching coordinates
                DeliveryOrder matchedOrder = null;
                for (DeliveryOrder order : orders) {
                    // EVRPTW: match by customer location
                    if (Math.abs(order.getLat() - stopData.getY()) < 0.0001 &&
                        Math.abs(order.getLng() - stopData.getX()) < 0.0001) {
                        matchedOrder = order;
                        break;
                    }
                }
                
                if (matchedOrder != null && !routeOrders.contains(matchedOrder)) {
                    routeOrders.add(matchedOrder);
                    
                    // Add customer stop (EVRPTW: single customer location)
                    stops.add(RouteStopDTO.builder()
                            .sequence(sequence++)
                            .type("CUSTOMER")
                            .orderId(matchedOrder.getId())
                            .lat(matchedOrder.getLat())
                            .lng(matchedOrder.getLng())
                            .address(matchedOrder.getAddress())
                            .customerName(matchedOrder.getCustomerName())
                            .completed(false)
                            .build());
                } else if ("CHARGING".equals(stopData.getType()) || (stopData.getType() != null && stopData.getType().contains("SWAP"))) {
                    // Battery swap station
                    stops.add(RouteStopDTO.builder()
                            .sequence(sequence++)
                            .type("SWAP")
                            .lat(stopData.getY())
                            .lng(stopData.getX())
                            .address("Trạm đổi pin")
                            .completed(false)
                            .build());
                }
            }
            
            if (stops.isEmpty() || routeOrders.isEmpty()) {
                continue;
            }
            
            String stopsJson;
            try {
                stopsJson = objectMapper.writeValueAsString(stops);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize stops", e);
            }
            
            AssignedRoute route = AssignedRoute.builder()
                    .driver(driver)
                    .vehicle(vehicle)
                    .status(RouteStatus.PLANNED)
                    .stopsJson(stopsJson)
                    .totalStops(stops.size())
                    .completedStops(0)
                    .totalDistance(optRoute.getTotalDistance())
                    .createdAt(LocalDateTime.now())
                    .build();
            
            route = routeRepository.save(route);
            
            // Update orders
            for (DeliveryOrder order : routeOrders) {
                order.setAssignedRoute(route);
                order.setStatus(OrderStatus.ASSIGNED);
            }
            orderRepository.saveAll(routeOrders);
            
            // Update driver and vehicle status
            driver.setStatus(DriverStatus.ON_ROUTE);
            vehicle.setStatus(VehicleStatus.IN_USE);
            driverRepository.save(driver);
            vehicleRepository.save(vehicle);
            
            createdRoutes.add(toDTO(route));
        }
        
        if (createdRoutes.isEmpty()) {
            throw new RuntimeException("Không thể tạo lộ trình từ kết quả tối ưu. Vui lòng kiểm tra dữ liệu.");
        }
        
        return createdRoutes;
    }
    
    private RouteDTO toDTO(AssignedRoute route) {
        RouteDTO dto = RouteDTO.fromEntity(route);
        
        // Parse stops
        if (route.getStopsJson() != null) {
            try {
                List<RouteStopDTO> stops = objectMapper.readValue(
                        route.getStopsJson(),
                        new TypeReference<List<RouteStopDTO>>() {}
                );
                dto.setStops(stops);
            } catch (JsonProcessingException e) {
                log.error("Failed to parse stops JSON", e);
            }
        }
        
        // Add orders
        List<OrderDTO> orderDTOs = route.getOrders().stream()
                .map(OrderDTO::fromEntity)
                .toList();
        dto.setOrders(orderDTOs);
        
        return dto;
    }
}
