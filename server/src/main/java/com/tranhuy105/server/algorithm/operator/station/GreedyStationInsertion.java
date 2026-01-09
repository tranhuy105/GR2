package com.tranhuy105.server.algorithm.operator.station;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.tranhuy105.server.algorithm.RouteEvaluator;
import com.tranhuy105.server.domain.Instance;
import com.tranhuy105.server.domain.Node;
import com.tranhuy105.server.domain.NodeType;
import com.tranhuy105.server.domain.Route;
import com.tranhuy105.server.domain.RouteStats;
import com.tranhuy105.server.domain.Solution;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Greedy station insertion to fix battery violations
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GreedyStationInsertion {
    private final RouteEvaluator evaluator;
    private static final int MAX_REPAIRS = 10;

    /**
     * Repair solution by inserting stations to fix battery violations
     */
    public void repair(Solution solution, Instance instance) {
        for (Route route : solution.getRoutes()) {
            if (route.isEmpty()) {
                continue;
            }

            for (int attempt = 0; attempt < MAX_REPAIRS; attempt++) {
                List<Integer> stops = route.getStops();
                double[][] forwardStates = evaluator.getForwardStates(stops, instance);
                
                ViolationInfo violation = findFirstBatteryViolation(stops, instance);
                
                if (violation == null) {
                    break;  // Route is battery feasible
                }

                StationInsertionResult best = findBestStationInsertion(stops, violation, forwardStates, instance);
                
                if (best != null) {
                    stops.add(best.position, best.stationId);
                } else {
                    break;  // Cannot fix this route
                }
            }
        }
    }

    private ViolationInfo findFirstBatteryViolation(List<Integer> stops, Instance instance) {
        double currBat = instance.getVehicleSpec().getBatteryCapacity();
        int prevId = 0;

        for (int pos = 0; pos < stops.size(); pos++) {
            int nodeId = stops.get(pos);
            Node node = instance.getAllNodes().get(nodeId);
            
            currBat -= instance.energy(prevId, nodeId);

            if (currBat < -1e-6) {
                return new ViolationInfo(pos, nodeId);
            }

            if (node.getType() == NodeType.STATION) {
                currBat = instance.getVehicleSpec().getBatteryCapacity();
            }

            prevId = nodeId;
        }

        // Check return to depot
        if (!stops.isEmpty()) {
            int lastId = stops.get(stops.size() - 1);
            if (currBat - instance.energy(lastId, 0) < -1e-6) {
                return new ViolationInfo(stops.size(), 0);
            }
        }

        return null;
    }

    private StationInsertionResult findBestStationInsertion(List<Integer> stops, ViolationInfo violation,
                                                            double[][] forwardStates, Instance instance) {
        int refNodeId = violation.position > 0 ? stops.get(violation.position - 1) : 0;
        
        List<Integer> candidateStations = instance.getNearestStations().get(refNodeId);
        if (candidateStations == null || candidateStations.isEmpty()) {
            candidateStations = new ArrayList<>();
            for (int i = 0; i < Math.min(5, instance.getStations().size()); i++) {
                candidateStations.add(instance.getStations().get(i).getId());
            }
        }

        StationInsertionResult best = null;
        double bestCost = Double.POSITIVE_INFINITY;

        int startPos = Math.max(0, violation.position - 2);
        int endPos = violation.position + 1;

        for (int insertPos = startPos; insertPos < endPos; insertPos++) {
            for (int stationId : candidateStations) {
                RouteStats stats = evaluator.evaluateWithInsertion(stops, insertPos, stationId, forwardStates, instance);

                if (stats.batteryViolation() < 1e-6 && stats.cost() < bestCost) {
                    bestCost = stats.cost();
                    best = new StationInsertionResult(insertPos, stationId);
                    return best;  // Early termination
                }
            }
        }

        return best;
    }

    private record ViolationInfo(int position, int nodeId) {}
    private record StationInsertionResult(int position, int stationId) {}
}
