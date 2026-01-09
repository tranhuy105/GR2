package com.tranhuy105.server.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.tranhuy105.server.algorithm.ALNSSolver;
import com.tranhuy105.server.algorithm.ParallelALNSSolver;
import com.tranhuy105.server.domain.Instance;
import com.tranhuy105.server.domain.Node;
import com.tranhuy105.server.domain.Route;
import com.tranhuy105.server.domain.Solution;
import com.tranhuy105.server.dto.OptimizationResponse;
import com.tranhuy105.server.dto.OptimizationResponse.RouteDTO;
import com.tranhuy105.server.dto.OptimizationResponse.StopDTO;
import com.tranhuy105.server.dto.OptimizationResponse.SummaryDTO;
import com.tranhuy105.server.exception.OptimizationException;

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

    /**
     * Optimize from uploaded file
     * @param parallel if true, use parallel ALNS with multiple workers
     */
    public OptimizationResponse optimizeFromFile(MultipartFile file, Integer iterations, 
                                                  Double timeLimit, Boolean parallel) {
        try {
            log.info("Received optimization request: file={}, size={} bytes, parallel={}", 
                    file.getOriginalFilename(), file.getSize(), parallel);
            
            long startTime = System.currentTimeMillis();
            
            // Parse instance from file
            Instance instance = parserService.parse(file.getInputStream());
            
            // Run optimization
            Solution solution = runOptimization(instance, iterations, timeLimit, parallel);
            
            long computeTime = System.currentTimeMillis() - startTime;
            
            // Convert to response
            return buildResponse(solution, instance, computeTime);
            
        } catch (IOException e) {
            throw new OptimizationException("Failed to read uploaded file", e);
        }
    }

    /**
     * Optimize from string content
     */
    public OptimizationResponse optimizeFromString(String content, Integer iterations, 
                                                    Double timeLimit, Boolean parallel) {
        log.info("Received optimization request from string content, parallel={}", parallel);
        
        long startTime = System.currentTimeMillis();
        
        Instance instance = parserService.parseFromString(content);
        
        Solution solution = runOptimization(instance, iterations, timeLimit, parallel);
        
        long computeTime = System.currentTimeMillis() - startTime;
        
        return buildResponse(solution, instance, computeTime);
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

    private OptimizationResponse buildResponse(Solution solution, Instance instance, long computeTime) {
        OptimizationResponse response = new OptimizationResponse();
        response.setComputeTimeMs(computeTime);
        
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
}
