package tranhuy105.evrptw.operators.insertion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

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
     * Find best position to insert customer in a route
     */
    public InsertionResult findBestPosition(Solution solution, int routeIdx, int customerId) {
        List<Integer> route = solution.getRoutes().get(routeIdx);
        int routeLen = route.size();

        // Cache route cost
        RouteStats oldStats = evaluator.evaluate(route);
        double costOld = oldStats.cost();

        int bestPos = -1;
        double bestCost = Double.POSITIVE_INFINITY;
        Integer bestStationBefore = null;
        Integer bestStationAfter = null;

        // Get nearest stations for this customer
        Map<Integer, List<Integer>> nearestStations = instance.getNearestStations();
        List<Integer> nearestStationIds = nearestStations.getOrDefault(customerId, new ArrayList<>());
        List<Integer> candidateStations = nearestStationIds.subList(0, Math.min(3, nearestStationIds.size()));

        // Quick distance estimates for all positions
        List<PositionEstimate> estimates = new ArrayList<>();
        for (int pos = 0; pos <= routeLen; pos++) {
            int prevId = pos > 0 ? route.get(pos - 1) : 0;
            int nextId = pos < routeLen ? route.get(pos) : 0;
            double quickCost = quickInsertionCost(prevId, nextId, customerId);
            estimates.add(new PositionEstimate(quickCost, pos));
        }

        // Sort by quick cost estimate
        estimates.sort(Comparator.comparingDouble(e -> e.cost));

        // Evaluate positions (prioritize promising ones)
        int positionsEvaluated = 0;
        int maxPositions = Math.min(routeLen + 1, Math.max(5, (routeLen + 1) / 2));

        for (PositionEstimate estimate : estimates) {
            int pos = estimate.position;
            
            // Early termination if estimate is much worse than best
            // Python: threshold = best_cost * 1.1 if best_cost != inf else inf
            if (bestCost != Double.POSITIVE_INFINITY && estimate.cost > bestCost * 1.1) {
                continue;
            }

            positionsEvaluated++;
            if (positionsEvaluated > maxPositions && bestPos != -1) {
                break;
            }

            // Scenario 1: Direct insertion
            List<Integer> newRoute = new ArrayList<>(route);
            newRoute.add(pos, customerId);
            RouteStats newStats = evaluator.evaluate(newRoute);
            double delta = newStats.cost() - costOld;

            if (delta < bestCost) {
                bestCost = delta;
                bestPos = pos;
                bestStationBefore = null;
                bestStationAfter = null;
            }

            // Scenario 2: Insert with station if battery violation
            if (newStats.batteryViolation() > 0 && !candidateStations.isEmpty()) {
                // Only try first 2 nearest stations for speed
                for (int stId : candidateStations.subList(0, Math.min(2, candidateStations.size()))) {
                    // Station BEFORE customer
                    List<Integer> rBefore = new ArrayList<>(route);
                    rBefore.add(pos, stId);
                    rBefore.add(pos + 1, customerId);
                    RouteStats statsBefore = evaluator.evaluate(rBefore);
                    
                    if (statsBefore.batteryViolation() < 1e-6 && statsBefore.cost() - costOld < bestCost) {
                        bestCost = statsBefore.cost() - costOld;
                        bestPos = pos;
                        bestStationBefore = stId;
                        bestStationAfter = null;
                    }

                    // Station AFTER customer
                    List<Integer> rAfter = new ArrayList<>(route);
                    rAfter.add(pos, customerId);
                    rAfter.add(pos + 1, stId);
                    RouteStats statsAfter = evaluator.evaluate(rAfter);
                    
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
     * Helper class for position estimates
     */
    private static class PositionEstimate {
        final double cost;
        final int position;

        PositionEstimate(double cost, int position) {
            this.cost = cost;
            this.position = position;
        }
    }
}
