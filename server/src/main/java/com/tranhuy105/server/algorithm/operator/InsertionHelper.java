package com.tranhuy105.server.algorithm.operator;

import java.util.List;

import org.springframework.stereotype.Component;

import com.tranhuy105.server.algorithm.RouteEvaluator;
import com.tranhuy105.server.domain.Instance;
import com.tranhuy105.server.domain.RouteStats;
import com.tranhuy105.server.domain.Solution;

import lombok.RequiredArgsConstructor;

/**
 * Helper class for finding best insertion positions
 */
@Component
@RequiredArgsConstructor
public class InsertionHelper {
    private final RouteEvaluator evaluator;

    /**
     * Find best position to insert customer in a route
     */
    public InsertionResult findBestPosition(Solution solution, int routeIdx, int customerId, Instance instance) {
        List<Integer> routeStops = solution.getRoutes().get(routeIdx).getStops();
        int routeLen = routeStops.size();

        // Cache forward states for optimized insertion
        double[][] forwardStates = evaluator.getForwardStates(routeStops, instance);
        
        // Calculate old cost
        RouteStats oldStats = evaluator.evaluate(routeStops, instance);
        double costOld = oldStats.cost();

        int bestPos = -1;
        double bestCost = Double.POSITIVE_INFINITY;
        Integer bestStationBefore = null;
        Integer bestStationAfter = null;

        // Get nearest stations for this customer
        List<Integer> nearestStationIds = instance.getNearestStations().get(customerId);
        int numStations = nearestStationIds != null ? Math.min(4, nearestStationIds.size()) : 0;

        // Evaluate ALL positions for best quality
        for (int pos = 0; pos <= routeLen; pos++) {
            // Scenario 1: Direct insertion
            RouteStats newStats = evaluator.evaluateWithInsertion(routeStops, pos, customerId, forwardStates, instance);
            double delta = newStats.cost() - costOld;

            if (delta < bestCost) {
                bestCost = delta;
                bestPos = pos;
                bestStationBefore = null;
                bestStationAfter = null;
                
                // Early termination if very good insertion
                if (delta < 1e-6 && newStats.batteryViolation() < 1e-6) {
                    return new InsertionResult(bestPos, bestCost, null, null);
                }
            }

            // Scenario 2: Insert with station if battery violation
            if (newStats.batteryViolation() > 0 && numStations > 0) {
                for (int s = 0; s < numStations; s++) {
                    int stId = nearestStationIds.get(s);
                    
                    // Station BEFORE customer
                    RouteStats statsBefore = evaluator.evaluateWithDoubleInsertion(routeStops, pos, stId, customerId, instance);
                    
                    if (statsBefore.batteryViolation() < 1e-6 && statsBefore.cost() - costOld < bestCost) {
                        bestCost = statsBefore.cost() - costOld;
                        bestPos = pos;
                        bestStationBefore = stId;
                        bestStationAfter = null;
                    }

                    // Station AFTER customer
                    RouteStats statsAfter = evaluator.evaluateWithDoubleInsertion(routeStops, pos, customerId, stId, instance);
                    
                    if (statsAfter.batteryViolation() < 1e-6 && statsAfter.cost() - costOld < bestCost) {
                        bestCost = statsAfter.cost() - costOld;
                        bestPos = pos;
                        bestStationBefore = null;
                        bestStationAfter = stId;
                    }
                }
            }
        }

        return new InsertionResult(bestPos, bestCost, bestStationBefore, bestStationAfter);
    }

    /**
     * Quick distance-based insertion cost estimate
     */
    public double quickInsertionCost(int prevId, int nextId, int insertId, Instance instance) {
        return instance.distance(prevId, insertId) + 
               instance.distance(insertId, nextId) - 
               instance.distance(prevId, nextId);
    }
}
