package tranhuy105.evrptw.algorithm;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import tranhuy105.evrptw.model.Instance;
import tranhuy105.evrptw.model.Node;
import tranhuy105.evrptw.model.Solution;
import tranhuy105.evrptw.operators.insertion.InsertionHelper;
import tranhuy105.evrptw.operators.insertion.InsertionResult;
import tranhuy105.evrptw.operators.station.GreedyStationInsertion;
import tranhuy105.evrptw.util.Constants;
import tranhuy105.evrptw.util.Logger;

/**
 * Builds initial solution using nearest-to-depot seeding strategy
 */
public class InitialSolutionBuilder {
    private final Instance instance;
    private final InsertionHelper insertionHelper;
    private final GreedyStationInsertion stationInsertion;
    private final RouteEvaluator evaluator;

    public InitialSolutionBuilder(Instance instance) {
        this.instance = instance;
        this.insertionHelper = new InsertionHelper(instance);
        this.stationInsertion = new GreedyStationInsertion(instance);
        this.evaluator = new RouteEvaluator(instance);
    }

    /**
     * Build initial solution using nearest-to-depot seeding
     */
    public Solution build() {
        Solution solution = new Solution(instance);
        List<Integer> unassigned = new ArrayList<>();
        
        // Collect all customer IDs
        for (Node customer : instance.getCustomers()) {
            unassigned.add(customer.getId());
        }

        while (!unassigned.isEmpty()) {
            // Start new route with customer nearest to depot
            int nearest = findNearestToDepot(unassigned);
            List<Integer> newRoute = new ArrayList<>();
            newRoute.add(nearest);
            solution.getRoutes().add(newRoute);
            unassigned.remove(Integer.valueOf(nearest));

            // Greedily add customers to current route
            int currentRouteIdx = solution.getRoutes().size() - 1;

            while (!unassigned.isEmpty()) {
                int bestCust = -1;
                int bestPos = -1;
                double bestCost = Double.POSITIVE_INFINITY;
                Integer bestStBefore = null;
                Integer bestStAfter = null;

                for (int custId : unassigned) {
                    InsertionResult result = insertionHelper.findBestPosition(
                            solution, currentRouteIdx, custId
                    );

                    // Only accept if cost increase is reasonable
                    if (result.costIncrease() < bestCost && 
                        result.costIncrease() < Constants.PENALTY_VEHICLE * 0.5) {
                        bestCost = result.costIncrease();
                        bestCust = custId;
                        bestPos = result.position();
                        bestStBefore = result.stationBefore();
                        bestStAfter = result.stationAfter();
                    }
                }

                if (bestCust == -1) {
                    break;  // Start new route
                }

                // Insert customer
                List<Integer> route = solution.getRoutes().get(currentRouteIdx);
                List<Integer> toInsert = new ArrayList<>();
                
                if (bestStBefore != null) {
                    toInsert.add(bestStBefore);
                }
                toInsert.add(bestCust);
                if (bestStAfter != null) {
                    toInsert.add(bestStAfter);
                }

                route.addAll(bestPos, toInsert);
                unassigned.remove(Integer.valueOf(bestCust));
            }
        }

        // Repair any battery violations
        stationInsertion.repair(solution);
        
        // Calculate total cost
        evaluator.calculateTotalCost(solution);

        Logger.info("Initial solution built: " + solution.getRoutes().size() + " vehicles");
        
        return solution;
    }

    /**
     * Find customer nearest to depot
     */
    private int findNearestToDepot(List<Integer> customers) {
        double[][] distMatrix = instance.getDistanceMatrix();
        return customers.stream()
                .min(Comparator.comparingDouble(c -> distMatrix[0][c]))
                .orElseThrow();
    }
}
