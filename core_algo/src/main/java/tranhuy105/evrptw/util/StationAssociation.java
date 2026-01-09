package tranhuy105.evrptw.util;

import java.util.Random;

/**
 * Station association strategies for customer removal
 */
public enum StationAssociation {
    RCO,      // Remove Customer Only
    RCWPS,    // Remove Customer With Preceding Station
    RCWSS;    // Remove Customer With Succeeding Station

    private static final Random random = new Random();

    /**
     * Randomly select a station association strategy
     */
    public static StationAssociation random() {
        StationAssociation[] values = values();
        return values[random.nextInt(values.length)];
    }
}
