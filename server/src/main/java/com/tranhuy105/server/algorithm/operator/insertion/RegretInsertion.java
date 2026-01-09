package com.tranhuy105.server.algorithm.operator.insertion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import org.springframework.stereotype.Component;

import com.tranhuy105.server.algorithm.operator.InsertionHelper;
import com.tranhuy105.server.algorithm.operator.InsertionOperator;
import com.tranhuy105.server.algorithm.operator.InsertionResult;
import com.tranhuy105.server.config.ALNSProperties;
import com.tranhuy105.server.domain.Instance;
import com.tranhuy105.server.domain.Route;
import com.tranhuy105.server.domain.Solution;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Regret-k insertion: insert customer with highest regret value.
 * Uses parallel stream for performance.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RegretInsertion implements InsertionOperator {
    private final InsertionHelper insertionHelper;
    private final ALNSProperties properties;
    private static final int K = 2;  // Regret level

    @Override
    public String getName() {
        return "regret_" + K;
    }

    @Override
    public void insert(Solution solution, List<Integer> customersToInsert, Instance instance) {
        List<Integer> remaining = new ArrayList<>(customersToInsert);
        double[][] distMatrix = instance.getDistanceMatrix();

        // Precompute new route costs
        Map<Integer, Double> newRouteCosts = new HashMap<>();
        for (int custId : remaining) {
            double d = distMatrix[0][custId] + distMatrix[custId][0];
            newRouteCosts.put(custId, d + properties.penalties().vehicle());
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
                        InsertionResult result = insertionHelper.findBestPosition(solution, rIdx, custId, instance);
                        
                        InsertionOption option = new InsertionOption(
                            result.costIncrease(),
                            rIdx,
                            result.position(),
                            result.stationBefore(),
                            result.stationAfter()
                        );

                        if (options.size() < K) {
                            options.offer(option);
                        } else if (option.cost < options.peek().cost) {
                            options.poll();
                            options.offer(option);
                        }
                    }

                    // New route option
                    double costNewRoute = newRouteCosts.get(custId);
                    InsertionOption newRouteOption = new InsertionOption(costNewRoute, -1, 0, null, null);
                    
                    if (options.size() < K) {
                        options.offer(newRouteOption);
                    } else if (newRouteOption.cost < options.peek().cost) {
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
                    if (sortedOptions.size() >= K) {
                        regret = sortedOptions.get(K - 1).cost - sortedOptions.get(0).cost;
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
                Route newRoute = new Route();
                newRoute.getStops().add(bestCandidate.customerId);
                solution.getRoutes().add(newRoute);
            } else {
                // Insert into existing route
                Route route = solution.getRoutes().get(best.routeIdx);
                List<Integer> stops = route.getStops();
                
                List<Integer> toInsert = new ArrayList<>();
                if (best.stationBefore != null) {
                    toInsert.add(best.stationBefore);
                }
                toInsert.add(bestCandidate.customerId);
                if (best.stationAfter != null) {
                    toInsert.add(best.stationAfter);
                }

                stops.addAll(best.position, toInsert);
            }

            remaining.remove(Integer.valueOf(bestCandidate.customerId));
        }
    }

    private record RegretCandidate(
        int customerId,
        double regretValue,
        InsertionOption bestOption
    ) {}

    private record InsertionOption(
        double cost,
        int routeIdx,
        int position,
        Integer stationBefore,
        Integer stationAfter
    ) {}
}
