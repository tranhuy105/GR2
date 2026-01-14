package com.tranhuy105.server.algorithm.operator.insertion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.tranhuy105.server.algorithm.operator.InsertionHelper;
import com.tranhuy105.server.algorithm.operator.InsertionOperator;
import com.tranhuy105.server.algorithm.operator.InsertionResult;
import com.tranhuy105.server.config.ALNSProperties;
import com.tranhuy105.server.domain.Instance;
import com.tranhuy105.server.domain.Route;
import com.tranhuy105.server.domain.Solution;

import lombok.RequiredArgsConstructor;

/**
 * Greedy insertion: always insert customer with minimum cost increase.
 * Uses parallel stream for performance.
 */
@Component
@RequiredArgsConstructor
public class GreedyInsertion implements InsertionOperator {
    private final InsertionHelper insertionHelper;
    private final ALNSProperties properties;

    @Override
    public String getName() {
        return "greedy";
    }

    @Override
    public void insert(Solution solution, List<Integer> customersToInsert, Instance instance) {
        List<Integer> remaining = new ArrayList<>(customersToInsert);
        double[][] distMatrix = instance.getDistanceMatrix();

        // Precompute new route costs for single customers
        Map<Integer, Double> newRouteCosts = new HashMap<>();
        for (int custId : remaining) {
            double d = distMatrix[0][custId] + distMatrix[custId][0];
            newRouteCosts.put(custId, d + properties.penalties().vehicle());
        }

        while (!remaining.isEmpty()) {
            int numRoutes = solution.getRoutes().size();

            // Parallel evaluation of all remaining customers
            InsertionCandidate bestCandidate = remaining.parallelStream()
                .map(custId -> {
                    double bestCost = Double.POSITIVE_INFINITY;
                    int bestRouteIdx = -1;
                    int bestPos = -1;
                    Integer bestStBefore = null;
                    Integer bestStAfter = null;

                    // Try existing routes
                    for (int rIdx = 0; rIdx < numRoutes; rIdx++) {
                        InsertionResult result = insertionHelper.findBestPosition(solution, rIdx, custId, instance);
                        
                        if (result.costIncrease() < bestCost) {
                            bestCost = result.costIncrease();
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
                        bestRouteIdx = -1;
                        bestPos = 0;
                        bestStBefore = null;
                        bestStAfter = null;
                    }
                    
                    return new InsertionCandidate(custId, bestCost, bestRouteIdx, bestPos, bestStBefore, bestStAfter);
                })
                .min(Comparator.comparingDouble(InsertionCandidate::costIncrease))
                .orElse(null);

            if (bestCandidate == null || bestCandidate.costIncrease == Double.POSITIVE_INFINITY) {
                break;
            }

            // Execute insertion
            if (bestCandidate.routeIdx == -1) {
                // Create new route
                Route newRoute = new Route();
                newRoute.getStops().add(bestCandidate.customerId);
                solution.getRoutes().add(newRoute);
            } else {
                // Insert into existing route
                Route route = solution.getRoutes().get(bestCandidate.routeIdx);
                List<Integer> stops = route.getStops();
                
                List<Integer> toInsert = new ArrayList<>();
                if (bestCandidate.stationBefore != null) {
                    toInsert.add(bestCandidate.stationBefore);
                }
                toInsert.add(bestCandidate.customerId);
                if (bestCandidate.stationAfter != null) {
                    toInsert.add(bestCandidate.stationAfter);
                }

                stops.addAll(bestCandidate.position, toInsert);
            }

            remaining.remove(Integer.valueOf(bestCandidate.customerId));
        }
    }

    private record InsertionCandidate(
        int customerId,
        double costIncrease,
        int routeIdx,
        int position,
        Integer stationBefore,
        Integer stationAfter
    ) {}
}
