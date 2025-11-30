package tranhuy105.evrptw.operators.insertion;

import java.util.List;

import tranhuy105.evrptw.algorithm.RouteEvaluator;
import tranhuy105.evrptw.model.Instance;
import tranhuy105.evrptw.model.RouteStats;
import tranhuy105.evrptw.model.Solution;

/**
 * Helper class for finding best insertion positions
 */
public class InsertionHelper {
    private final Instance instance;
    private final RouteEvaluator evaluator;

    public InsertionHelper(Instance instance) {
        this.instance = instance;
        this.evaluator = new RouteEvaluator(instance);
    }

    /**
     * Quick distance-based insertion cost estimate
     */
    public double quickInsertionCost(int prevId, int nextId, int insertId) {
        double[][] distMatrix = instance.getDistanceMatrix();
        return distMatrix[prevId][insertId] + 
               distMatrix[insertId][nextId] - 
               distMatrix[prevId][nextId];
    }

    /**
     * Find best position to insert customer in a route.
     * Evaluates ALL positions to ensure best solution quality.
     * Optimized: uses in-place evaluation to avoid ArrayList creation.
     */
    public InsertionResult findBestPosition(Solution solution, int routeIdx, int customerId) {
        List<Integer> route = solution.getRoutes().get(routeIdx);
        int routeLen = route.size();

        // Cache route cost (computed once)
        RouteStats oldStats = evaluator.evaluate(route);
        double costOld = oldStats.cost();

        int bestPos = -1;
        double bestCost = Double.POSITIVE_INFINITY;
        Integer bestStationBefore = null;
        Integer bestStationAfter = null;

        // Get nearest stations for this customer (use more for better coverage)
        List<Integer> nearestStationIds = instance.getNearestStations().get(customerId);
        int numStations = nearestStationIds != null ? Math.min(4, nearestStationIds.size()) : 0;

        // Evaluate ALL positions for best quality
        for (int pos = 0; pos <= routeLen; pos++) {
            // Scenario 1: Direct insertion (no ArrayList creation)
            RouteStats newStats = evaluator.evaluateWithInsertion(route, pos, customerId);
            double delta = newStats.cost() - costOld;

            if (delta < bestCost) {
                bestCost = delta;
                bestPos = pos;
                bestStationBefore = null;
                bestStationAfter = null;
            }

            // Scenario 2: Insert with station if battery violation
            if (newStats.batteryViolation() > 0 && numStations > 0) {
                for (int s = 0; s < numStations; s++) {
                    int stId = nearestStationIds.get(s);
                    
                    // Station BEFORE customer (no ArrayList creation)
                    RouteStats statsBefore = evaluator.evaluateWithDoubleInsertion(route, pos, stId, customerId);
                    
                    if (statsBefore.batteryViolation() < 1e-6 && statsBefore.cost() - costOld < bestCost) {
                        bestCost = statsBefore.cost() - costOld;
                        bestPos = pos;
                        bestStationBefore = stId;
                        bestStationAfter = null;
                    }

                    // Station AFTER customer (no ArrayList creation)
                    RouteStats statsAfter = evaluator.evaluateWithDoubleInsertion(route, pos, customerId, stId);
                    
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
}
