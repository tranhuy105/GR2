package com.tranhuy105.server.algorithm;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.springframework.stereotype.Service;

import com.tranhuy105.server.algorithm.operator.station.GreedyStationInsertion;
import com.tranhuy105.server.config.ALNSProperties;
import com.tranhuy105.server.domain.Instance;
import com.tranhuy105.server.domain.Route;
import com.tranhuy105.server.domain.Solution;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Adaptive Large Neighborhood Search (ALNS) solver for EVRPTW.
 * Implements destroy-repair metaheuristic with adaptive operator selection.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ALNSSolver {
    private final ALNSProperties properties;
    private final OperatorRegistry operatorRegistry;
    private final AdaptiveWeightManager weightManager;
    private final InitialSolutionBuilder initialSolutionBuilder;
    private final GreedyStationInsertion stationInsertion;
    private final RouteEvaluator evaluator;
    
    private final Random random = new Random();

    private static final String REMOVAL_GROUP = "removal";
    private static final String INSERTION_GROUP = "insertion";

    /**
     * Solve the EVRPTW problem instance
     */
    public Solution solve(Instance instance) {
        return solve(instance, properties.defaultIterations(), properties.defaultTimeLimit());
    }

    /**
     * Solve with custom iteration/time limits
     */
    public Solution solve(Instance instance, int maxIterations, double maxTimeSeconds) {
        // Register operators with weight manager
        weightManager.registerGroup(REMOVAL_GROUP, new ArrayList<>(operatorRegistry.getRemovalOperatorNames()));
        weightManager.registerGroup(INSERTION_GROUP, new ArrayList<>(operatorRegistry.getInsertionOperatorNames()));

        // Build initial solution
        log.info("Building initial solution...");
        Solution currentSol = initialSolutionBuilder.build(instance);
        Solution bestSol = currentSol.copy();
        Solution bestFeasibleSol = bestSol.isFeasible() ? bestSol.copy() : null;

        log.info("Initial: Cost={}, Dist={}, Vehicles={}, Feasible={}",
                String.format("%.2f", bestSol.getCost()),
                String.format("%.2f", bestSol.getTotalDistance()),
                bestSol.getVehicleCount(),
                bestSol.isFeasible());

        // Initialize temperature for simulated annealing
        double tInit = bestSol.getCost() * 0.05 / Math.log(2);
        double temperature = tInit;

        String currentRemovalOp = null;
        String currentInsertionOp = null;

        long startTime = System.currentTimeMillis();
        long maxTimeMs = (long) (maxTimeSeconds * 1000);
        int iteration = 0;
        int iterationsWithoutFeasible = 0;
        int bestFeasibleFoundAt = -1;  // Track when BEST feasible was found

        while (iteration < maxIterations) {
            // Check time limit
            if (maxTimeMs > 0 && (System.currentTimeMillis() - startTime) >= maxTimeMs) {
                log.info("Time limit reached after {} iterations", iteration);
                break;
            }

            double progress = (double) iteration / maxIterations;

            // Feasibility recovery
            if (bestFeasibleSol != null && 
                !currentSol.isFeasible() &&
                progress > 0.7 &&
                iterationsWithoutFeasible > 200) {
                log.debug("Iter {}: Restarting from best feasible solution", iteration);
                currentSol = bestFeasibleSol.copy();
                iterationsWithoutFeasible = 0;
                temperature = tInit * 0.1;
            }

            Solution tempSol = currentSol.copy();
            List<Integer> removedCustomers;

            boolean feasibilityFocus = progress > 0.3 && bestFeasibleSol == null;

            // ==================== DESTROY PHASE ====================
            currentRemovalOp = weightManager.select(REMOVAL_GROUP);
            weightManager.recordUsage(REMOVAL_GROUP, currentRemovalOp);

            int nCustomers = instance.getCustomers().size();
            int minRemove, maxRemove;
            if (feasibilityFocus) {
                minRemove = Math.max(1, (int) (nCustomers * 0.05));
                maxRemove = Math.max(2, (int) (nCustomers * 0.15));
            } else {
                minRemove = Math.max(1, (int) (nCustomers * 0.1));
                maxRemove = Math.max(2, (int) (nCustomers * 0.4));
            }

            int q = random.nextInt(maxRemove - minRemove + 1) + minRemove;
            removedCustomers = operatorRegistry.getRemovalOperator(currentRemovalOp)
                    .remove(tempSol, q, instance);

            tempSol.getRoutes().removeIf(Route::isEmpty);

            // ==================== REPAIR PHASE ====================
            currentInsertionOp = weightManager.select(INSERTION_GROUP);
            weightManager.recordUsage(INSERTION_GROUP, currentInsertionOp);

            operatorRegistry.getInsertionOperator(currentInsertionOp)
                    .insert(tempSol, removedCustomers, instance);

            stationInsertion.repair(tempSol, instance);
            tempSol.getRoutes().removeIf(Route::isEmpty);

            // ==================== EVALUATION & ACCEPTANCE ====================
            evaluator.calculateTotalCost(tempSol, instance);
            double cost = tempSol.getCost();
            double dist = tempSol.getTotalDistance();
            double viol = tempSol.getTotalViolations();
            boolean isFeasible = viol < 1e-6;

            boolean accepted = false;
            ResultType resultType = null;

            int currVehicles = currentSol.getVehicleCount();
            int newVehicles = tempSol.getVehicleCount();

            if (newVehicles < currVehicles) {
                accepted = true;
                resultType = ResultType.BETTER;
            } else if (newVehicles > currVehicles) {
                accepted = false;
            } else {
                double delta = cost - currentSol.getCost();

                if (delta < 0) {
                    accepted = true;
                    resultType = ResultType.BETTER;
                } else if (temperature > 1e-10 && random.nextDouble() < Math.exp(-delta / temperature)) {
                    accepted = true;
                    resultType = ResultType.ACCEPTED_WORSE;
                }
            }

            if (accepted) {
                currentSol = tempSol;
            }

            // Update best solution
            boolean isNewBest = checkNewBest(tempSol, bestSol, isFeasible, viol, dist);

            if (isNewBest) {
                bestSol = tempSol.copy();
                resultType = ResultType.NEW_BEST;
                log.debug("Iter {}: NEW BEST! Cost={}, Dist={}, Veh={}, Feasible={}",
                        iteration, String.format("%.2f", cost), String.format("%.2f", dist),
                        bestSol.getVehicleCount(), isFeasible);
            }

            // Track best feasible
            if (isFeasible) {
                iterationsWithoutFeasible = 0;

                if (bestFeasibleSol == null) {
                    bestFeasibleSol = tempSol.copy();
                    bestFeasibleFoundAt = iteration;
                    log.info("Iter {}: First feasible solution found!", iteration);
                } else if (tempSol.getVehicleCount() < bestFeasibleSol.getVehicleCount() ||
                           (tempSol.getVehicleCount() == bestFeasibleSol.getVehicleCount() &&
                            dist < bestFeasibleSol.getTotalDistance())) {
                    bestFeasibleSol = tempSol.copy();
                    bestFeasibleFoundAt = iteration;
                }
            } else {
                iterationsWithoutFeasible++;
            }

            // Update operator scores
            if (resultType != null) {
                weightManager.updateScore(REMOVAL_GROUP, currentRemovalOp, resultType.getScore());
                weightManager.updateScore(INSERTION_GROUP, currentInsertionOp, resultType.getScore());
            }

            // Update weights periodically
            if ((iteration + 1) % properties.segmentSize() == 0) {
                weightManager.updateAllWeights();

                if (iteration % 500 == 0) {
                    log.debug("Iter {}: Current={}, Best={}, T={}, HasFeasible={}",
                            iteration,
                            String.format("%.2f", currentSol.getCost()),
                            String.format("%.2f", bestSol.getCost()),
                            String.format("%.4f", temperature),
                            bestFeasibleSol != null);
                }
            }

            temperature *= properties.coolingRate();
            iteration++;
        }

        log.info("Completed {} iterations", iteration);

        if (bestFeasibleSol != null) {
            log.info("Returning best feasible solution (found at iter {})", bestFeasibleFoundAt);
            return bestFeasibleSol;
        } else {
            log.warn("No feasible solution found! Returning best infeasible solution.");
            return bestSol;
        }
    }

    private boolean checkNewBest(Solution tempSol, Solution bestSol,
                                  boolean isFeasible, double viol, double dist) {
        boolean bestIsFeasible = bestSol.isFeasible();

        if (isFeasible) {
            if (!bestIsFeasible) {
                return true;
            } else if (tempSol.getVehicleCount() < bestSol.getVehicleCount()) {
                return true;
            } else if (tempSol.getVehicleCount() == bestSol.getVehicleCount() &&
                       dist < bestSol.getTotalDistance()) {
                return true;
            }
        } else if (!bestIsFeasible) {
            if (viol < bestSol.getTotalViolations() - 1e-6) {
                return true;
            } else if (Math.abs(viol - bestSol.getTotalViolations()) < 1e-6) {
                if (tempSol.getVehicleCount() < bestSol.getVehicleCount()) {
                    return true;
                } else if (tempSol.getVehicleCount() == bestSol.getVehicleCount() &&
                           dist < bestSol.getTotalDistance()) {
                    return true;
                }
            }
        }
        return false;
    }
}
