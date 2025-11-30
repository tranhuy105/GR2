package tranhuy105.evrptw.util;

/**
 * Constants for EVRPTW ALNS solver
 */
public class Constants {
    // Penalty weights
    public static final double PENALTY_VEHICLE = 100000.0;
    public static final double PENALTY_CAPACITY = 10000.0;
    public static final double PENALTY_TIME = 10000.0;
    public static final double PENALTY_BATTERY = 10000.0;

    // Adaptive weight parameters
    public static final int SIGMA_NEW_BEST = 33;
    public static final int SIGMA_BETTER = 9;
    public static final int SIGMA_ACCEPTED_WORSE = 13;
    public static final double RHO = 0.1;  // Learning rate

    // ALNS parameters
    public static final int SEGMENT_SIZE = 100;  // Update weights every N iterations
    public static final int STATION_REMOVAL_INTERVAL = 500;

    // Shaw removal parameters
    public static final double[] SHAW_PHI = {1.0, 1.0, 1.0, 1.0};  // distance, time, route, demand
    public static final double SHAW_ETA = 2.0;  // determinism factor

    // Worst removal determinism
    public static final double WORST_KAPPA = 4.0;

    // Feasibility recovery parameters
    public static final double FEASIBILITY_FOCUS_THRESHOLD = 0.3;  // Start focusing on feasibility after 30%
    public static final double INFEASIBLE_RESTART_THRESHOLD = 0.7; // Restart from best feasible after 70%

    private Constants() {
        // Prevent instantiation
    }
}
