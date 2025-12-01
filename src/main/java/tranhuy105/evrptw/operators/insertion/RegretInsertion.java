package tranhuy105.evrptw.operators.insertion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import tranhuy105.evrptw.model.Solution;
import tranhuy105.evrptw.util.Constants;

/**
 * Regret-k insertion: insert customer with highest regret value
 */
public class RegretInsertion implements InsertionOperator {
    private final InsertionHelper helper;
    private final int k;

    public RegretInsertion(InsertionHelper helper, int k) {
        this.helper = helper;
        this.k = k;
    }

    @Override
    public void insert(Solution solution, List<Integer> unassigned) {
        List<Integer> remaining = new ArrayList<>(unassigned);
        double[][] distMatrix = solution.getInstance().getDistanceMatrix();

        // Precompute new route costs
        Map<Integer, Double> newRouteCosts = new HashMap<>();
        for (int custId : remaining) {
            double d = distMatrix[0][custId] + distMatrix[custId][0];
            newRouteCosts.put(custId, d + Constants.PENALTY_VEHICLE);
        }

        while (!remaining.isEmpty()) {
            int numRoutes = solution.getRoutes().size();

            // Parallel evaluation of all remaining customers
            RegretCandidate bestCandidate = remaining.parallelStream()
                .map(custId -> {
                    // Collect insertion options using heap for top-k
                    PriorityQueue<InsertionOption> options = new PriorityQueue<>(
                        Comparator.comparingDouble(InsertionOption::cost).reversed()
                    );

                    // Existing routes
                    for (int rIdx = 0; rIdx < numRoutes; rIdx++) {
                        InsertionResult result = helper.findBestPosition(solution, rIdx, custId);
                        
                        InsertionOption option = new InsertionOption(
                            result.costIncrease(),
                            rIdx,
                            result.position(),
                            result.stationBefore(),
                            result.stationAfter()
                        );

                        if (options.size() < k) {
                            options.offer(option);
                        } else if (option.cost < options.peek().cost) {
                            // Replace worst (highest cost) with better option
                            options.poll();
                            options.offer(option);
                        }
                    }

                    // New route option
                    double costNewRoute = newRouteCosts.get(custId);
                    InsertionOption newRouteOption = new InsertionOption(costNewRoute, -1, 0, null, null);
                    
                    if (options.size() < k) {
                        options.offer(newRouteOption);
                    } else if (newRouteOption.cost < options.peek().cost) {
                        // Replace worst (highest cost) with better option
                        options.poll();
                        options.offer(newRouteOption);
                    }

                    if (options.isEmpty()) {
                        return new RegretCandidate(custId, Double.NEGATIVE_INFINITY, null);
                    }

                    // Extract sorted options (smallest cost first)
                    List<InsertionOption> sortedOptions = new ArrayList<>(options);
                    sortedOptions.sort(Comparator.comparingDouble(InsertionOption::cost));

                    // Calculate regret
                    double regret;
                    if (sortedOptions.size() >= k) {
                        regret = sortedOptions.get(k - 1).cost - sortedOptions.get(0).cost;
                    } else if (sortedOptions.size() >= 2) {
                        regret = sortedOptions.get(sortedOptions.size() - 1).cost - sortedOptions.get(0).cost;
                    } else {
                        regret = Double.POSITIVE_INFINITY;
                    }
                    
                    return new RegretCandidate(custId, regret, sortedOptions.get(0));
                })
                .max(Comparator.comparingDouble(RegretCandidate::regretValue))
                .orElse(null);

            if (bestCandidate == null || bestCandidate.bestOption == null) {
                break;
            }

            // Execute insertion
            InsertionOption best = bestCandidate.bestOption;
            if (best.routeIdx == -1) {
                // Create new route
                List<Integer> newRoute = new ArrayList<>();
                newRoute.add(bestCandidate.customerId);
                solution.getRoutes().add(newRoute);
            } else {
                // Insert into existing route
                List<Integer> route = solution.getRoutes().get(best.routeIdx);
                List<Integer> toInsert = new ArrayList<>();
                
                if (best.stationBefore != null) {
                    toInsert.add(best.stationBefore);
                }
                toInsert.add(bestCandidate.customerId);
                if (best.stationAfter != null) {
                    toInsert.add(best.stationAfter);
                }

                route.addAll(best.position, toInsert);
            }

            remaining.remove(Integer.valueOf(bestCandidate.customerId));
        }
    }

    private record RegretCandidate(
        int customerId,
        double regretValue,
        InsertionOption bestOption
    ) {}

    /**
     * Helper record for insertion options
     */
    private record InsertionOption(
        double cost,
        int routeIdx,
        int position,
        Integer stationBefore,
        Integer stationAfter
    ) {}
}
