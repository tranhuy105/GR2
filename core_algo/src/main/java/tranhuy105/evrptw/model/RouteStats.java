package tranhuy105.evrptw.model;

/**
 * Statistics for a single route evaluation
 */
public record RouteStats(
        double cost,
        double distance,
        double capacityViolation,
        double timeViolation,
        double batteryViolation
) {
}
