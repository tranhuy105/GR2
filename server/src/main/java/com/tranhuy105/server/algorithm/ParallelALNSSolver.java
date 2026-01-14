package com.tranhuy105.server.algorithm;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Service;

import com.tranhuy105.server.algorithm.operator.station.GreedyStationInsertion;
import com.tranhuy105.server.config.ALNSProperties;
import com.tranhuy105.server.domain.Instance;
import com.tranhuy105.server.domain.Route;
import com.tranhuy105.server.domain.Solution;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Parallel ALNS Solver using Island Model.
 * Multiple workers run ALNS independently and sync best solution periodically.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ParallelALNSSolver {
    private final ALNSProperties properties;
    private final OperatorRegistry operatorRegistry;
    private final InitialSolutionBuilder initialSolutionBuilder;
    private final GreedyStationInsertion stationInsertion;
    private final RouteEvaluator evaluator;

    private static final int NUM_WORKERS = 4;
    private static final int SYNC_INTERVAL = 200;  // Sync every 200 iterations
    private static final String REMOVAL_GROUP = "removal";
    private static final String INSERTION_GROUP = "insertion";

    /**
     * Solve using parallel workers
     */
    public Solution solve(Instance instance) {
        return solve(instance, properties.defaultIterations(), properties.defaultTimeLimit());
    }

    public Solution solve(Instance instance, int maxIterations, double maxTimeSeconds) {
        log.info("Starting Parallel ALNS with {} workers", NUM_WORKERS);
        
        // Shared best solution (thread-safe)
        AtomicReference<Solution> globalBest = new AtomicReference<>(null);
        AtomicReference<Solution> globalBestFeasible = new AtomicReference<>(null);
        
        // Build initial solution once
        Solution initialSol = initialSolutionBuilder.build(instance);
        globalBest.set(initialSol.copy());
        if (initialSol.isFeasible()) {
            globalBestFeasible.set(initialSol.copy());
        }

        log.info("Initial: Cost={}, Dist={}, Vehicles={}, Feasible={}",
                String.format("%.2f", initialSol.getCost()),
                String.format("%.2f", initialSol.getTotalDistance()),
                initialSol.getVehicleCount(),
                initialSol.isFeasible());

        // Create virtual thread executor (Java 21) or fallback to fixed pool
        ExecutorService executor;
        try {
            // Try to use virtual threads if available (Java 21+)
            executor = (ExecutorService) Executors.class
                .getMethod("newVirtualThreadPerTaskExecutor")
                .invoke(null);
            log.info("Using Virtual Threads");
        } catch (Exception e) {
            // Fallback to fixed thread pool
            executor = Executors.newFixedThreadPool(NUM_WORKERS);
            log.info("Using Fixed Thread Pool (Virtual Threads not available)");
        }

        long startTime = System.currentTimeMillis();
        long maxTimeMs = maxTimeSeconds > 0 ? (long) (maxTimeSeconds * 1000) : Long.MAX_VALUE;
        int iterationsPerWorker = maxIterations / NUM_WORKERS;

        // Launch workers
        List<Future<WorkerResult>> futures = new ArrayList<>();
        for (int workerId = 0; workerId < NUM_WORKERS; workerId++) {
            final int wId = workerId;
            final long seed = System.nanoTime() + workerId * 1000;
            
            futures.add(executor.submit(() -> 
                runWorker(wId, seed, instance, initialSol.copy(), iterationsPerWorker, 
                         startTime, maxTimeMs, globalBest, globalBestFeasible)
            ));
        }

        // Wait for all workers
        WorkerResult bestResult = null;
        for (Future<WorkerResult> future : futures) {
            try {
                WorkerResult result = future.get();
                if (bestResult == null || isBetter(result.bestFeasible, bestResult.bestFeasible, 
                                                    result.best, bestResult.best)) {
                    bestResult = result;
                }
            } catch (Exception e) {
                log.error("Worker failed", e);
            }
        }

        executor.shutdown();

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("Parallel ALNS completed in {}ms", elapsed);

        // Return best feasible or best overall
        Solution finalSolution = globalBestFeasible.get();
        if (finalSolution != null) {
            log.info("Returning best feasible: Dist={}, Vehicles={}", 
                    String.format("%.2f", finalSolution.getTotalDistance()),
                    finalSolution.getVehicleCount());
            return finalSolution;
        } else {
            finalSolution = globalBest.get();
            log.warn("No feasible solution found! Returning best infeasible.");
            return finalSolution;
        }
    }

    private WorkerResult runWorker(int workerId, long seed, Instance instance, 
                                   Solution startSolution, int maxIterations,
                                   long startTime, long maxTimeMs,
                                   AtomicReference<Solution> globalBest,
                                   AtomicReference<Solution> globalBestFeasible) {
        
        Random random = new Random(seed);
        
        // Each worker has its own weight manager
        AdaptiveWeightManager weightManager = new AdaptiveWeightManager();
        weightManager.registerGroup(REMOVAL_GROUP, new ArrayList<>(operatorRegistry.getRemovalOperatorNames()));
        weightManager.registerGroup(INSERTION_GROUP, new ArrayList<>(operatorRegistry.getInsertionOperatorNames()));

        Solution currentSol = startSolution;
        Solution localBest = currentSol.copy();
        Solution localBestFeasible = currentSol.isFeasible() ? currentSol.copy() : null;

        double tInit = currentSol.getCost() * 0.05 / Math.log(2);
        double temperature = tInit;

        int iteration = 0;
        int bestFoundAt = -1;

        while (iteration < maxIterations) {
            // Check time limit
            if ((System.currentTimeMillis() - startTime) >= maxTimeMs) {
                break;
            }

            double progress = (double) iteration / maxIterations;

            // ==================== SYNC WITH GLOBAL BEST ====================
            if (iteration > 0 && iteration % SYNC_INTERVAL == 0) {
                // Check if global best is better than local
                Solution gBest = globalBestFeasible.get();
                if (gBest == null) gBest = globalBest.get();
                
                if (gBest != null && isBetterSolution(gBest, localBest)) {
                    // Adopt global best with some probability
                    if (random.nextDouble() < 0.5) {
                        currentSol = gBest.copy();
                        temperature = tInit * 0.3;  // Reset temperature partially
                    }
                }
                
                // Share our best to global
                updateGlobalBest(localBest, localBestFeasible, globalBest, globalBestFeasible);
            }

            // ==================== DESTROY PHASE ====================
            Solution tempSol = currentSol.copy();
            
            String removalOp = weightManager.select(REMOVAL_GROUP);
            weightManager.recordUsage(REMOVAL_GROUP, removalOp);

            int nCustomers = instance.getCustomers().size();
            int minRemove = Math.max(1, (int) (nCustomers * 0.1));
            int maxRemove = Math.max(2, (int) (nCustomers * 0.4));
            int q = random.nextInt(maxRemove - minRemove + 1) + minRemove;

            List<Integer> removed = operatorRegistry.getRemovalOperator(removalOp)
                    .remove(tempSol, q, instance);
            tempSol.getRoutes().removeIf(Route::isEmpty);

            // ==================== REPAIR PHASE ====================
            String insertionOp = weightManager.select(INSERTION_GROUP);
            weightManager.recordUsage(INSERTION_GROUP, insertionOp);

            operatorRegistry.getInsertionOperator(insertionOp)
                    .insert(tempSol, removed, instance);
            stationInsertion.repair(tempSol, instance);
            tempSol.getRoutes().removeIf(Route::isEmpty);

            // ==================== EVALUATION ====================
            evaluator.calculateTotalCost(tempSol, instance);
            
            boolean accepted = acceptSolution(tempSol, currentSol, temperature, random);
            if (accepted) {
                currentSol = tempSol;
            }

            // Update local best
            ResultType resultType = null;
            if (isBetterSolution(tempSol, localBest)) {
                localBest = tempSol.copy();
                resultType = ResultType.NEW_BEST;
                bestFoundAt = iteration;
            } else if (accepted) {
                resultType = tempSol.getCost() < currentSol.getCost() ? 
                            ResultType.BETTER : ResultType.ACCEPTED_WORSE;
            }

            if (tempSol.isFeasible()) {
                if (localBestFeasible == null || isBetterSolution(tempSol, localBestFeasible)) {
                    localBestFeasible = tempSol.copy();
                }
            }

            // Update scores
            if (resultType != null) {
                weightManager.updateScore(REMOVAL_GROUP, removalOp, resultType.getScore());
                weightManager.updateScore(INSERTION_GROUP, insertionOp, resultType.getScore());
            }

            if ((iteration + 1) % properties.segmentSize() == 0) {
                weightManager.updateAllWeights();
            }

            temperature *= properties.coolingRate();
            iteration++;
        }

        // Final sync
        updateGlobalBest(localBest, localBestFeasible, globalBest, globalBestFeasible);

        log.debug("Worker {} completed {} iterations, best at iter {}", 
                 workerId, iteration, bestFoundAt);

        return new WorkerResult(localBest, localBestFeasible, iteration, bestFoundAt);
    }

    private synchronized void updateGlobalBest(Solution localBest, Solution localBestFeasible,
                                               AtomicReference<Solution> globalBest,
                                               AtomicReference<Solution> globalBestFeasible) {
        // Update global best
        Solution gBest = globalBest.get();
        if (gBest == null || isBetterSolution(localBest, gBest)) {
            globalBest.compareAndSet(gBest, localBest.copy());
        }

        // Update global best feasible
        if (localBestFeasible != null) {
            Solution gBestFeasible = globalBestFeasible.get();
            if (gBestFeasible == null || isBetterSolution(localBestFeasible, gBestFeasible)) {
                globalBestFeasible.compareAndSet(gBestFeasible, localBestFeasible.copy());
            }
        }
    }

    private boolean isBetterSolution(Solution a, Solution b) {
        if (a == null) return false;
        if (b == null) return true;
        
        boolean aFeasible = a.isFeasible();
        boolean bFeasible = b.isFeasible();
        
        if (aFeasible && !bFeasible) return true;
        if (!aFeasible && bFeasible) return false;
        
        if (a.getVehicleCount() < b.getVehicleCount()) return true;
        if (a.getVehicleCount() > b.getVehicleCount()) return false;
        
        return a.getTotalDistance() < b.getTotalDistance();
    }

    private boolean isBetter(Solution aFeasible, Solution bFeasible, Solution a, Solution b) {
        if (aFeasible != null && bFeasible == null) return true;
        if (aFeasible == null && bFeasible != null) return false;
        if (aFeasible != null) {
            return isBetterSolution(aFeasible, bFeasible);
        }
        return isBetterSolution(a, b);
    }

    private boolean acceptSolution(Solution newSol, Solution currentSol, double temperature, Random random) {
        int currVehicles = currentSol.getVehicleCount();
        int newVehicles = newSol.getVehicleCount();

        if (newVehicles < currVehicles) return true;
        if (newVehicles > currVehicles) return false;

        double delta = newSol.getCost() - currentSol.getCost();
        if (delta < 0) return true;
        
        if (temperature > 1e-10) {
            return random.nextDouble() < Math.exp(-delta / temperature);
        }
        return false;
    }

    private record WorkerResult(Solution best, Solution bestFeasible, int iterations, int bestFoundAt) {}
}
