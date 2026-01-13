package com.tranhuy105.server.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.tranhuy105.server.algorithm.ALNSSolver;
import com.tranhuy105.server.algorithm.ParallelALNSSolver;
import com.tranhuy105.server.domain.ChargingMode;
import com.tranhuy105.server.domain.Instance;
import com.tranhuy105.server.domain.Node;
import com.tranhuy105.server.domain.NodeType;
import com.tranhuy105.server.domain.Route;
import com.tranhuy105.server.domain.Solution;
import com.tranhuy105.server.domain.VehicleSpec;
import com.tranhuy105.server.dto.FleetOptimizationRequest;
import com.tranhuy105.server.dto.OptimizationResponse;
import com.tranhuy105.server.dto.OptimizationResponse.RouteDTO;
import com.tranhuy105.server.dto.OptimizationResponse.StopDTO;
import com.tranhuy105.server.dto.OptimizationResponse.SummaryDTO;
import com.tranhuy105.server.entity.DeliveryOrder;
import com.tranhuy105.server.entity.SwapStation;
import com.tranhuy105.server.exception.OptimizationException;
import com.tranhuy105.server.repository.DeliveryOrderRepository;
import com.tranhuy105.server.repository.SwapStationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for orchestrating optimization requests
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OptimizationService {
    private final InstanceParserService parserService;
    private final ALNSSolver solver;
    private final ParallelALNSSolver parallelSolver;
    private final DeliveryOrderRepository orderRepository;
    private final SwapStationRepository stationRepository;

    /**
     * Optimize from uploaded file with charging mode support
     */
    public OptimizationResponse optimizeFromFile(MultipartFile file, Integer iterations, 
                                                  Double timeLimit, Boolean parallel,
                                                  ChargingMode chargingMode, Double batterySwapTime) {
        try {
            log.info("Received optimization request: file={}, size={} bytes, parallel={}, chargingMode={}", 
                    file.getOriginalFilename(), file.getSize(), parallel, chargingMode);
            
            long startTime = System.currentTimeMillis();
            
            // Parse instance from file
            Instance instance = parserService.parse(file.getInputStream());
            
            // Set charging mode
            VehicleSpec vehicleSpec = instance.getVehicleSpec();
            vehicleSpec.setChargingMode(chargingMode);
            vehicleSpec.setBatterySwapTime(batterySwapTime);
            
            // Run optimization
            Solution solution = runOptimization(instance, iterations, timeLimit, parallel);
            
            long computeTime = System.currentTimeMillis() - startTime;
            
            // Convert to response
            return buildResponse(solution, instance, computeTime, chargingMode);
            
        } catch (IOException e) {
            throw new OptimizationException("Failed to read uploaded file", e);
        }
    }

    /**
     * Optimize from string content with charging mode support
     */
    public OptimizationResponse optimizeFromString(String content, Integer iterations, 
                                                    Double timeLimit, Boolean parallel,
                                                    ChargingMode chargingMode, Double batterySwapTime) {
        log.info("Received optimization request from string content, parallel={}, chargingMode={}", 
                parallel, chargingMode);
        
        long startTime = System.currentTimeMillis();
        
        Instance instance = parserService.parseFromString(content);
        
        // Set charging mode
        VehicleSpec vehicleSpec = instance.getVehicleSpec();
        vehicleSpec.setChargingMode(chargingMode);
        vehicleSpec.setBatterySwapTime(batterySwapTime);
        
        Solution solution = runOptimization(instance, iterations, timeLimit, parallel);
        
        long computeTime = System.currentTimeMillis() - startTime;
        
        return buildResponse(solution, instance, computeTime, chargingMode);
    }

    /**
     * Optimize fleet routes from database orders
     * This builds an Instance from database entities and runs ALNS
     */
    public OptimizationResponse optimizeFleet(FleetOptimizationRequest request) {
        log.info("Optimizing fleet with {} orders, chargingMode={}", 
                request.getOrderIds().size(), request.getChargingMode());
        
        long startTime = System.currentTimeMillis();
        
        // Fetch orders
        List<DeliveryOrder> orders = orderRepository.findAllById(request.getOrderIds());
        if (orders.isEmpty()) {
            throw new OptimizationException("No orders found");
        }
        
        // Fetch stations
        List<SwapStation> stations;
        if (request.getStationIds() != null && !request.getStationIds().isEmpty()) {
            stations = stationRepository.findAllById(request.getStationIds());
        } else {
            stations = stationRepository.findAvailableStations();
        }
        
        // Build Instance
        Instance instance = buildInstanceFromFleet(request, orders, stations);
        
        // Run optimization
        Solution solution = runOptimization(instance, request.getIterations(), 
                request.getTimeLimit(), request.getParallel());
        
        long computeTime = System.currentTimeMillis() - startTime;
        
        return buildFleetResponse(solution, instance, computeTime, request.getChargingMode(), orders);
    }

    private Instance buildInstanceFromFleet(FleetOptimizationRequest request, 
                                             List<DeliveryOrder> orders,
                                             List<SwapStation> stations) {
        Instance instance = new Instance();
        
        // Use Haversine formula for real lat/lng coordinates
        instance.setUseGeoCoordinates(true);
        
        // Create depot node
        Node depot = Node.builder()
                .stringId("D0")
                .type(NodeType.DEPOT)
                .x(request.getDepotLng())
                .y(request.getDepotLat())
                .demand(0)
                .readyTime(0)
                .dueTime(24) // 24 hours
                .serviceTime(0)
                .build();
        instance.addNode(depot);
        
        // Create customer nodes from orders
        // EVRPTW: Each order has a single customer location (delivery from depot)
        int customerId = 1;
        for (DeliveryOrder order : orders) {
            Node customer = Node.builder()
                    .stringId("C" + customerId)
                    .type(NodeType.CUSTOMER)
                    .x(order.getLng())
                    .y(order.getLat())
                    .demand(order.getDemand())
                    .readyTime(order.getTimeWindowStart())
                    .dueTime(order.getTimeWindowEnd())
                    .serviceTime(order.getServiceTime())
                    .build();
            instance.addNode(customer);
            customerId++;
        }
        
        // Create station nodes
        int stationId = 1;
        for (SwapStation station : stations) {
            Node stationNode = Node.builder()
                    .stringId("S" + stationId)
                    .type(NodeType.STATION)
                    .x(station.getLng())
                    .y(station.getLat())
                    .demand(0)
                    .readyTime(station.getOpenTime())
                    .dueTime(station.getCloseTime())
                    .serviceTime(0)
                    .build();
            instance.addNode(stationNode);
            stationId++;
        }
        
        // Set vehicle spec
        VehicleSpec vehicleSpec = VehicleSpec.builder()
                .batteryCapacity(request.getBatteryCapacity())
                .cargoCapacity(request.getCargoCapacity())
                .consumptionRate(request.getConsumptionRate())
                .velocity(request.getVelocity())
                .chargingMode(request.getChargingMode())
                .batterySwapTime(request.getBatterySwapTime())
                .build();
        instance.setVehicleSpec(vehicleSpec);
        
        // Finalize instance
        instance.finalizeInstance();
        
        log.info("Built instance: {} customers, {} stations", 
                instance.getCustomers().size(), instance.getStations().size());
        
        return instance;
    }

    private Solution runOptimization(Instance instance, Integer iterations, 
                                      Double timeLimit, Boolean parallel) {
        boolean useParallel = parallel != null && parallel;
        
        if (useParallel) {
            log.info("Using Parallel ALNS solver");
            if (iterations != null && timeLimit != null) {
                return parallelSolver.solve(instance, iterations, timeLimit);
            } else if (iterations != null) {
                return parallelSolver.solve(instance, iterations, 0);
            } else {
                return parallelSolver.solve(instance);
            }
        } else {
            log.info("Using Sequential ALNS solver");
            if (iterations != null && timeLimit != null) {
                return solver.solve(instance, iterations, timeLimit);
            } else if (iterations != null) {
                return solver.solve(instance, iterations, 0);
            } else {
                return solver.solve(instance);
            }
        }
    }

    private OptimizationResponse buildResponse(Solution solution, Instance instance, 
                                                long computeTime, ChargingMode chargingMode) {
        OptimizationResponse response = new OptimizationResponse();
        response.setComputeTimeMs(computeTime);
        response.setChargingMode(chargingMode.name());
        
        // Build routes
        List<RouteDTO> routeDTOs = new ArrayList<>();
        int vehicleId = 1;
        
        for (Route route : solution.getRoutes()) {
            RouteDTO routeDTO = new RouteDTO();
            routeDTO.setVehicleId(vehicleId++);
            routeDTO.setDistance(route.getDistance());
            routeDTO.setFeasible(route.isFeasible());
            
            List<StopDTO> stops = new ArrayList<>();
            for (int nodeId : route.getStops()) {
                Node node = instance.getAllNodes().get(nodeId);
                StopDTO stop = new StopDTO();
                stop.setNodeId(nodeId);
                stop.setStringId(node.getStringId());
                stop.setType(node.getType().name());
                stop.setX(node.getX());
                stop.setY(node.getY());
                stops.add(stop);
            }
            routeDTO.setStops(stops);
            routeDTOs.add(routeDTO);
        }
        response.setRoutes(routeDTOs);
        
        // Build summary
        SummaryDTO summary = new SummaryDTO();
        summary.setTotalVehicles(solution.getVehicleCount());
        summary.setTotalDistance(solution.getTotalDistance());
        summary.setTotalCost(solution.getCost());
        summary.setFeasible(solution.isFeasible());
        summary.setTotalCustomers(instance.getCustomers().size());
        summary.setTotalStations(instance.getStations().size());
        response.setSummary(summary);
        
        return response;
    }

    private OptimizationResponse buildFleetResponse(Solution solution, Instance instance, 
                                                     long computeTime, ChargingMode chargingMode,
                                                     List<DeliveryOrder> orders) {
        OptimizationResponse response = buildResponse(solution, instance, computeTime, chargingMode);
        
        // Add order mapping to response for frontend
        // This helps the frontend map optimized routes back to original orders
        
        return response;
    }
}
