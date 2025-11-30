package tranhuy105.evrptw.operators.station;

import java.util.ArrayList;
import java.util.List;

import tranhuy105.evrptw.algorithm.RouteEvaluator;
import tranhuy105.evrptw.model.Instance;
import tranhuy105.evrptw.model.Node;
import tranhuy105.evrptw.model.NodeType;
import tranhuy105.evrptw.model.RouteStats;
import tranhuy105.evrptw.model.Solution;

/**
 * Greedy station insertion to fix battery violations
 */
public class GreedyStationInsertion {
    private final Instance instance;
    private final RouteEvaluator evaluator;

    public GreedyStationInsertion(Instance instance) {
        this.instance = instance;
        this.evaluator = new RouteEvaluator(instance);
    }

    /**
     * Repair solution by inserting stations to fix battery violations
     */
    public void repair(Solution solution) {
        for (int rIdx = 0; rIdx < solution.getRoutes().size(); rIdx++) {
            List<Integer> route = solution.getRoutes().get(rIdx);
            if (route.isEmpty()) {
                continue;
            }

            // Keep repairing until feasible or no improvement
            int maxRepairs = 10;
            for (int attempt = 0; attempt < maxRepairs; attempt++) {
                // Find first battery violation
                ViolationInfo violation = findFirstBatteryViolation(route);
                
                if (violation == null) {
                    break;  // Route is battery feasible
                }

                // Find best station to insert
                StationInsertionResult best = findBestStationInsertion(route, violation);
                
                if (best != null) {
                    route.add(best.position, best.stationId);
                } else {
                    break;  // Cannot fix this route
                }
            }
        }
    }

    /**
     * Find first battery violation in route
     */
    private ViolationInfo findFirstBatteryViolation(List<Integer> route) {
        List<Node> allNodes = instance.getAllNodes();
        double[][] energyMatrix = instance.getEnergyMatrix();
        double qBattery = instance.getBatteryCapacity();

        double currBat = qBattery;
        int prevId = 0;

        for (int pos = 0; pos < route.size(); pos++) {
            int nodeId = route.get(pos);
            Node node = allNodes.get(nodeId);
            
            currBat -= energyMatrix[prevId][nodeId];

            if (currBat < -1e-6) {
                return new ViolationInfo(pos, nodeId);
            }

            if (node.getType() == NodeType.STATION) {
                currBat = qBattery;
            }

            prevId = nodeId;
        }

        // Check return to depot
        if (!route.isEmpty()) {
            int lastId = route.get(route.size() - 1);
            if (currBat - energyMatrix[lastId][0] < -1e-6) {
                return new ViolationInfo(route.size(), 0);
            }
        }

        return null;  // No violation
    }

    /**
     * Find best station to insert near violation point.
     * Optimized: uses in-place evaluation to avoid ArrayList creation.
     */
    private StationInsertionResult findBestStationInsertion(List<Integer> route, ViolationInfo violation) {
        // Get nearest stations to the node before violation
        int refNodeId = violation.position > 0 ? route.get(violation.position - 1) : 0;
        
        List<Integer> candidateStations = instance.getNearestStations().get(refNodeId);
        if (candidateStations == null || candidateStations.isEmpty()) {
            // Fallback: use first 5 stations
            candidateStations = new ArrayList<>();
            for (int i = 0; i < Math.min(5, instance.getStations().size()); i++) {
                candidateStations.add(instance.getStations().get(i).getId());
            }
        }

        StationInsertionResult best = null;
        double bestCost = Double.POSITIVE_INFINITY;

        // Limit search positions (2 positions before violation)
        int startPos = Math.max(0, violation.position - 2);
        int endPos = violation.position + 1;

        for (int insertPos = startPos; insertPos < endPos; insertPos++) {
            for (int stationId : candidateStations) {
                // Use in-place evaluation (no ArrayList creation)
                RouteStats stats = evaluator.evaluateWithInsertion(route, insertPos, stationId);

                // Check if battery feasible and better cost
                if (stats.batteryViolation() < 1e-6 && stats.cost() < bestCost) {
                    bestCost = stats.cost();
                    best = new StationInsertionResult(insertPos, stationId);
                    
                    // Early termination if we found a feasible solution
                    if (stats.batteryViolation() < 1e-6) {
                        return best;
                    }
                }
            }
        }

        return best;
    }

    /**
     * Information about a battery violation
     */
    private static class ViolationInfo {
        final int position;
        final int nodeId;

        ViolationInfo(int position, int nodeId) {
            this.position = position;
            this.nodeId = nodeId;
        }
    }

    /**
     * Result of station insertion search
     */
    private static class StationInsertionResult {
        final int position;
        final int stationId;

        StationInsertionResult(int position, int stationId) {
            this.position = position;
            this.stationId = stationId;
        }
    }
}
