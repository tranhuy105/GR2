package tranhuy105.evrptw.operators.insertion;

/**
 * Result of finding best insertion position for a customer
 */
public record InsertionResult(
        int position,
        double costIncrease,
        Integer stationBefore,
        Integer stationAfter
) {
}
