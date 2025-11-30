package tranhuy105.evrptw.operators.insertion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tranhuy105.evrptw.model.Solution;
import tranhuy105.evrptw.util.Constants;

/**
 * Greedy insertion: always insert customer with minimum cost increase
 */
public class GreedyInsertion implements InsertionOperator {
    private final InsertionHelper helper;

    public GreedyInsertion(InsertionHelper helper) {
        this.helper = helper;
    }

    @Override
    public void insert(Solution solution, List<Integer> unassigned) {
        List<Integer> remaining = new ArrayList<>(unassigned);
        double[][] distMatrix = solution.getInstance().getDistanceMatrix();

        // Precompute new route costs for single customers
        Map<Integer, Double> newRouteCosts = new HashMap<>();
        for (int custId : remaining) {
            double d = distMatrix[0][custId] + distMatrix[custId][0];
            newRouteCosts.put(custId, d + Constants.PENALTY_VEHICLE);
        }

        while (!remaining.isEmpty()) {
            double bestCost = Double.POSITIVE_INFINITY;
            int bestCust = -1;
            int bestRouteIdx = -1;
            int bestPos = -1;
            Integer bestStBefore = null;
            Integer bestStAfter = null;

            int numRoutes = solution.getRoutes().size();

            for (int custId : remaining) {
                // Try existing routes
                for (int rIdx = 0; rIdx < numRoutes; rIdx++) {
                    InsertionResult result = helper.findBestPosition(solution, rIdx, custId);
                    
                    if (result.costIncrease() < bestCost) {
                        bestCost = result.costIncrease();
                        bestCust = custId;
                        bestRouteIdx = rIdx;
                        bestPos = result.position();
                        bestStBefore = result.stationBefore();
                        bestStAfter = result.stationAfter();

                        // Early termination if cost is very good
                        if (bestCost < 0) {
                            break;
                        }
                    }
                }

                // Try new route
                double costNewRoute = newRouteCosts.get(custId);
                if (costNewRoute < bestCost) {
                    bestCost = costNewRoute;
                    bestCust = custId;
                    bestRouteIdx = -1;
                    bestPos = 0;
                    bestStBefore = null;
                    bestStAfter = null;
                }
            }

            if (bestCust == -1) {
                break;
            }

            // Execute insertion
            if (bestRouteIdx == -1) {
                // Create new route
                List<Integer> newRoute = new ArrayList<>();
                newRoute.add(bestCust);
                solution.getRoutes().add(newRoute);
            } else {
                // Insert into existing route
                List<Integer> route = solution.getRoutes().get(bestRouteIdx);
                List<Integer> toInsert = new ArrayList<>();
                
                if (bestStBefore != null) {
                    toInsert.add(bestStBefore);
                }
                toInsert.add(bestCust);
                if (bestStAfter != null) {
                    toInsert.add(bestStAfter);
                }

                route.addAll(bestPos, toInsert);
            }

            remaining.remove(Integer.valueOf(bestCust));
        }
    }
}
