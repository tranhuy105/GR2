package com.tranhuy105.server.algorithm;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import com.tranhuy105.server.algorithm.operator.InsertionHelper;
import com.tranhuy105.server.algorithm.operator.InsertionResult;
import com.tranhuy105.server.algorithm.operator.station.GreedyStationInsertion;
import com.tranhuy105.server.domain.Instance;
import com.tranhuy105.server.domain.Node;
import com.tranhuy105.server.domain.Route;
import com.tranhuy105.server.domain.Solution;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Builds initial solution using nearest-to-depot seeding strategy
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InitialSolutionBuilder {
    private final InsertionHelper insertionHelper;
    private final GreedyStationInsertion stationInsertion;
    private final RouteEvaluator evaluator;

    /**
     * Build initial solution using nearest-to-depot seeding
     */
    public Solution build(Instance instance) {
        Solution solution = new Solution();
        List<Integer> unassigned = new ArrayList<>();
        
        // Collect all customer IDs
        for (Node customer : instance.getCustomers()) {
            unassigned.add(customer.getId());
        }

        while (!unassigned.isEmpty()) {
            // Start new route with customer nearest to depot
            int nearest = findNearestToDepot(unassigned, instance);
            Route newRoute = new Route();
            newRoute.getStops().add(nearest);
            solution.getRoutes().add(newRoute);
            unassigned.remove(Integer.valueOf(nearest));

            // Greedily add customers to current route
            int currentRouteIdx = solution.getRoutes().size() - 1;

            while (!unassigned.isEmpty()) {
                int bestCust = -1;
                InsertionResult bestResult = InsertionResult.notFound();

                for (int custId : unassigned) {
                    InsertionResult result = insertionHelper.findBestPosition(
                            solution, currentRouteIdx, custId, instance
                    );

                    // Only accept if cost increase is reasonable
                    double threshold = 100000.0 * 0.5;  // PENALTY_VEHICLE * 0.5
                    if (result.costIncrease() < bestResult.costIncrease() && 
                        result.costIncrease() < threshold) {
                        bestResult = result;
                        bestCust = custId;
                    }
                }

                if (bestCust == -1) {
                    break;  // Start new route
                }

                // Insert customer
                Route route = solution.getRoutes().get(currentRouteIdx);
                List<Integer> stops = route.getStops();
                
                List<Integer> toInsert = new ArrayList<>();
                if (bestResult.stationBefore() != null) {
                    toInsert.add(bestResult.stationBefore());
                }
                toInsert.add(bestCust);
                if (bestResult.stationAfter() != null) {
                    toInsert.add(bestResult.stationAfter());
                }

                stops.addAll(bestResult.position(), toInsert);
                unassigned.remove(Integer.valueOf(bestCust));
            }
        }

        // Repair any battery violations
        stationInsertion.repair(solution, instance);
        
        // Calculate total cost
        evaluator.calculateTotalCost(solution, instance);

        log.info("Initial solution built: {} vehicles", solution.getRoutes().size());
        
        return solution;
    }

    private int findNearestToDepot(List<Integer> customers, Instance instance) {
        return customers.stream()
                .min(Comparator.comparingDouble(c -> instance.distance(0, c)))
                .orElseThrow();
    }
}
