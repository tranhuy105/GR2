package tranhuy105.evrptw.model;

/**
 * Represents a customer's position in the solution
 */
public record CustomerPosition(
        int routeIndex,
        int position,
        int customerId
) {
}
