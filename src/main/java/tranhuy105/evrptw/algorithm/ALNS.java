package tranhuy105.evrptw.algorithm;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import tranhuy105.evrptw.model.Instance;
import tranhuy105.evrptw.model.NodeType;
import tranhuy105.evrptw.model.Solution;
import tranhuy105.evrptw.operators.insertion.GreedyInsertion;
import tranhuy105.evrptw.operators.insertion.InsertionHelper;
import tranhuy105.evrptw.operators.insertion.InsertionOperator;
import tranhuy105.evrptw.operators.insertion.RegretInsertion;
import tranhuy105.evrptw.operators.removal.GreedyRouteRemoval;
import tranhuy105.evrptw.operators.removal.RandomRemoval;
import tranhuy105.evrptw.operators.removal.RandomRouteRemoval;
import tranhuy105.evrptw.operators.removal.RemovalOperator;
import tranhuy105.evrptw.operators.removal.ShawRemoval;
import tranhuy105.evrptw.operators.removal.WorstDistanceRemoval;
import tranhuy105.evrptw.operators.removal.WorstTimeRemoval;
import tranhuy105.evrptw.operators.station.GreedyStationInsertion;
import tranhuy105.evrptw.operators.station.RandomStationRemoval;
import tranhuy105.evrptw.operators.station.StationRemovalOperator;
import tranhuy105.evrptw.operators.station.WorstDistanceStationRemoval;
import tranhuy105.evrptw.util.Constants;
import tranhuy105.evrptw.util.Logger;

/**
 * Adaptive Large Neighborhood Search for EVRPTW
 */
public class ALNS {
    private final Instance instance;
    private final int maxIterations;
    private final long maxTimeMs;  // Time limit in milliseconds (0 = no limit)
    private final boolean verbose;

    // Operators
    private final Map<String, RemovalOperator> removalOperators;
    private final Map<String, InsertionOperator> insertionOperators;
    private final Map<String, StationRemovalOperator> stationRemovalOperators;

    // Adaptive weights
    private final AdaptiveWeights removalWeights;
    private final AdaptiveWeights insertionWeights;
    private final AdaptiveWeights stationRemovalWeights;

    // Helpers
    private final InsertionHelper insertionHelper;
    private final GreedyStationInsertion stationInsertion;
    private final RouteEvaluator evaluator;
    private final Random random = new Random();

    // Simulated annealing
    private double temperature;
    private static final double COOLING_RATE = 0.9995;

    /**
     * Create ALNS solver with iteration limit only
     */
    public ALNS(Instance instance, int maxIterations, boolean verbose) {
        this(instance, maxIterations, 0, verbose);
    }

    /**
     * Create ALNS solver with both iteration and time limits
     * @param maxIterations Maximum iterations (use Integer.MAX_VALUE for time-only limit)
     * @param maxTimeSeconds Time limit in seconds (0 = no time limit)
     */
    public ALNS(Instance instance, int maxIterations, double maxTimeSeconds, boolean verbose) {
        this.instance = instance;
        this.maxIterations = maxIterations;
        this.maxTimeMs = (long) (maxTimeSeconds * 1000);
        this.verbose = verbose;

        // Initialize helpers
        this.insertionHelper = new InsertionHelper(instance);
        this.stationInsertion = new GreedyStationInsertion(instance);
        this.evaluator = new RouteEvaluator(instance);

        // Initialize removal operators (6 operators matching Python)
        this.removalOperators = new LinkedHashMap<>();
        removalOperators.put("random", new RandomRemoval());
        removalOperators.put("shaw", new ShawRemoval());
        removalOperators.put("worst_distance", new WorstDistanceRemoval());
        removalOperators.put("worst_time", new WorstTimeRemoval());
        removalOperators.put("random_route", new RandomRouteRemoval());
        removalOperators.put("greedy_route", new GreedyRouteRemoval());

        // Initialize insertion operators
        this.insertionOperators = new LinkedHashMap<>();
        insertionOperators.put("greedy", new GreedyInsertion(insertionHelper));
        insertionOperators.put("regret_2", new RegretInsertion(insertionHelper, 2));
        insertionOperators.put("regret_3", new RegretInsertion(insertionHelper, 3));

        // Initialize station removal operators
        this.stationRemovalOperators = new LinkedHashMap<>();
        stationRemovalOperators.put("random_station", new RandomStationRemoval());
        stationRemovalOperators.put("worst_distance_station", new WorstDistanceStationRemoval());

        // Initialize adaptive weights
        this.removalWeights = new AdaptiveWeights(new ArrayList<>(removalOperators.keySet()));
        this.insertionWeights = new AdaptiveWeights(new ArrayList<>(insertionOperators.keySet()));
        this.stationRemovalWeights = new AdaptiveWeights(new ArrayList<>(stationRemovalOperators.keySet()));
    }

    /**
     * Run ALNS optimization
     */
    public Solution solve() {
        // Build initial solution
        Logger.info("Building initial solution...");
        InitialSolutionBuilder builder = new InitialSolutionBuilder(instance);
        Solution currentSol = builder.build();
        Solution bestSol = currentSol.copy();
        Solution bestFeasibleSol = bestSol.isFeasible() ? bestSol.copy() : null;

        Logger.info(String.format("Initial: Cost=%.2f, Dist=%.2f, Vehicles=%d, Feasible=%s",
                bestSol.getCost(), bestSol.getTotalDistance(),
                bestSol.getRoutes().size(), bestSol.isFeasible()));

        // Initialize temperature: accept 5% worse solution with 50% probability
        double tInit = bestSol.getCost() * 0.05 / Math.log(2);
        temperature = tInit;

        String currentRemovalOp = null;
        String currentInsertionOp = null;

        long startTime = System.currentTimeMillis();
        int iteration = 0;
        int iterationsWithoutFeasible = 0;
        int lastFeasibleIteration = -1;

        while (iteration < maxIterations) {
            // Check time limit
            if (maxTimeMs > 0 && (System.currentTimeMillis() - startTime) >= maxTimeMs) {
                Logger.info(String.format("Time limit reached after %d iterations", iteration));
                break;
            }
            
            // Calculate progress for adaptive behavior
            double progress = (double) iteration / maxIterations;
            
            // Feasibility recovery: restart from best feasible if stuck
            if (bestFeasibleSol != null && 
                !currentSol.isFeasible() &&
                progress > Constants.INFEASIBLE_RESTART_THRESHOLD &&
                iterationsWithoutFeasible > 200) {
                
                Logger.debug("Iter " + iteration + ": Restarting from best feasible solution");
                currentSol = bestFeasibleSol.copy();
                iterationsWithoutFeasible = 0;
                temperature = tInit * 0.1;  // Reheat slightly
            }
            
            Solution tempSol = currentSol.copy();
            List<Integer> removedCustomers;
            
            // Reduce destruction when focusing on feasibility
            boolean feasibilityFocus = progress > Constants.FEASIBILITY_FOCUS_THRESHOLD && 
                                       bestFeasibleSol == null;

            // ==================== DESTROY PHASE ====================

            // Station removal every N iterations
            if (iteration > 0 && iteration % Constants.STATION_REMOVAL_INTERVAL == 0) {
                String srOpName = stationRemovalWeights.select();
                stationRemovalWeights.recordUsage(srOpName);

                int numStations = countStationsInSolution(tempSol);
                // Python: random.uniform(0.1, 0.3) -> range [0.1, 0.3]
                int sigma = Math.max(1, (int) (numStations * (0.1 + random.nextDouble() * 0.2)));

                stationRemovalOperators.get(srOpName).remove(tempSol, sigma);
                Logger.debug("Iter " + iteration + ": Station removal (" + srOpName + ")");
            }

            // Select removal operator
            String removalOpName = removalWeights.select();
            removalWeights.recordUsage(removalOpName);
            currentRemovalOp = removalOpName;

            // Determine number of customers to remove
            // Use smaller destruction when focusing on feasibility
            int nCustomers = instance.getCustomers().size();
            int minRemove, maxRemove;
            if (feasibilityFocus) {
                minRemove = Math.max(1, (int) (nCustomers * 0.05));
                maxRemove = Math.max(2, (int) (nCustomers * 0.15));
            } else {
                minRemove = Math.max(1, (int) (nCustomers * 0.1));
                maxRemove = Math.max(2, (int) (nCustomers * 0.4));
            }
            
            if (removalOpName.equals("random_route") || removalOpName.equals("greedy_route")) {
                // Route removal operators don't use q parameter
                removedCustomers = removalOperators.get(removalOpName).remove(tempSol, 0);
            } else {
                int q = random.nextInt(maxRemove - minRemove + 1) + minRemove;
                removedCustomers = removalOperators.get(removalOpName).remove(tempSol, q);
            }

            // Clean empty routes
            tempSol.getRoutes().removeIf(List::isEmpty);

            // ==================== REPAIR PHASE ====================

            // Select insertion operator
            String insertionOpName = insertionWeights.select();
            insertionWeights.recordUsage(insertionOpName);
            currentInsertionOp = insertionOpName;

            insertionOperators.get(insertionOpName).insert(tempSol, removedCustomers);

            // Repair battery violations
            stationInsertion.repair(tempSol);

            // Clean empty routes again
            tempSol.getRoutes().removeIf(List::isEmpty);

            // ==================== EVALUATION & ACCEPTANCE ====================

            evaluator.calculateTotalCost(tempSol);
            double cost = tempSol.getCost();
            double dist = tempSol.getTotalDistance();
            double viol = tempSol.getTotalViolations();
            boolean isFeasible = viol < 1e-6;

            // Determine acceptance
            boolean accepted = false;
            int resultType = -1;  // -1 = rejected

            int currVehicles = currentSol.getRoutes().size();
            int newVehicles = tempSol.getRoutes().size();

            if (newVehicles < currVehicles) {
                // Fewer vehicles - always accept
                accepted = true;
                resultType = 1;  // Better
            } else if (newVehicles > currVehicles) {
                // More vehicles - reject
                accepted = false;
            } else {
                // Same vehicles - compare cost
                double delta = cost - currentSol.getCost();

                if (delta < 0) {
                    accepted = true;
                    resultType = 1;  // Better
                } else if (temperature > 1e-10 && random.nextDouble() < Math.exp(-delta / temperature)) {
                    accepted = true;
                    resultType = 2;  // Accepted worse
                }
            }

            if (accepted) {
                currentSol = tempSol;
            }

            // Update best solution
            boolean isNewBest = checkNewBest(tempSol, bestSol, isFeasible, viol, dist);

            if (isNewBest) {
                bestSol = tempSol.copy();
                resultType = 0;  // New best
                if (verbose) {
                    String feasibleStr = isFeasible ? "FEASIBLE" : "infeasible";
                    Logger.info(String.format("Iter %d: NEW BEST (%s)! Cost=%.2f, Dist=%.2f, Veh=%d, Viol=%.4f",
                            iteration, feasibleStr, cost, dist, bestSol.getRoutes().size(), viol));
                }
            }
            
            // Track best feasible solution separately
            if (isFeasible) {
                iterationsWithoutFeasible = 0;
                lastFeasibleIteration = iteration;
                
                if (bestFeasibleSol == null) {
                    bestFeasibleSol = tempSol.copy();
                    if (verbose) {
                        Logger.info("Iter " + iteration + ": First feasible solution found!");
                    }
                } else if (tempSol.getRoutes().size() < bestFeasibleSol.getRoutes().size() ||
                           (tempSol.getRoutes().size() == bestFeasibleSol.getRoutes().size() &&
                            dist < bestFeasibleSol.getTotalDistance())) {
                    bestFeasibleSol = tempSol.copy();
                }
            } else {
                iterationsWithoutFeasible++;
            }

            // Update operator scores
            if (resultType >= 0) {
                ResultType rt = switch (resultType) {
                    case 0 -> ResultType.NEW_BEST;
                    case 1 -> ResultType.BETTER;
                    default -> ResultType.ACCEPTED_WORSE;
                };
                removalWeights.updateScore(currentRemovalOp, rt);
                insertionWeights.updateScore(currentInsertionOp, rt);
            }

            // Update adaptive weights periodically
            if ((iteration + 1) % Constants.SEGMENT_SIZE == 0) {
                removalWeights.updateWeights();
                insertionWeights.updateWeights();
                stationRemovalWeights.updateWeights();

                if (verbose && iteration % 500 == 0) {
                    String hasFeasible = bestFeasibleSol != null ? "yes" : "no";
                    Logger.info(String.format("Iter %d: Current=%.2f, Best=%.2f, T=%.4f, HasFeasible=%s",
                            iteration, currentSol.getCost(), bestSol.getCost(), temperature, hasFeasible));
                }
            }

            // Cool down temperature
            temperature *= COOLING_RATE;
            iteration++;
        }

        Logger.info(String.format("Completed %d iterations", iteration));
        
        // Return best feasible solution if available
        if (bestFeasibleSol != null) {
            Logger.info("Returning best feasible solution (found at iter " + lastFeasibleIteration + ")");
            return bestFeasibleSol;
        } else {
            Logger.warning("No feasible solution found! Returning best infeasible solution.");
            return bestSol;
        }
    }

    /**
     * Check if new solution is better than best
     */
    private boolean checkNewBest(Solution tempSol, Solution bestSol, 
                                  boolean isFeasible, double viol, double dist) {
        boolean bestIsFeasible = bestSol.isFeasible();

        if (isFeasible) {
            if (!bestIsFeasible) {
                return true;  // First feasible solution
            } else if (tempSol.getRoutes().size() < bestSol.getRoutes().size()) {
                return true;  // Fewer vehicles
            } else if (tempSol.getRoutes().size() == bestSol.getRoutes().size() && 
                       dist < bestSol.getTotalDistance()) {
                return true;  // Same vehicles, shorter distance
            }
        } else if (!bestIsFeasible) {
            // Both infeasible
            if (viol < bestSol.getTotalViolations() - 1e-6) {
                return true;  // Fewer violations
            } else if (Math.abs(viol - bestSol.getTotalViolations()) < 1e-6) {
                if (tempSol.getRoutes().size() < bestSol.getRoutes().size()) {
                    return true;
                } else if (tempSol.getRoutes().size() == bestSol.getRoutes().size() && 
                           dist < bestSol.getTotalDistance()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Count stations in solution
     */
    private int countStationsInSolution(Solution solution) {
        int count = 0;
        for (List<Integer> route : solution.getRoutes()) {
            for (int nodeId : route) {
                if (instance.getAllNodes().get(nodeId).getType() == NodeType.STATION) {
                    count++;
                }
            }
        }
        return count;
    }
}
