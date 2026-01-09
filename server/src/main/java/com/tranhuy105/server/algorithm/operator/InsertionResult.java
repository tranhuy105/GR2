package com.tranhuy105.server.algorithm.operator;

/**
 * Result of finding the best insertion position
 */
public record InsertionResult(
    int position,
    double costIncrease,
    Integer stationBefore,
    Integer stationAfter
) {
    public static InsertionResult notFound() {
        return new InsertionResult(-1, Double.POSITIVE_INFINITY, null, null);
    }
    
    public boolean isValid() {
        return position >= 0;
    }
}
