package tranhuy105.evrptw.operators.station;

import java.util.List;

import tranhuy105.evrptw.model.Solution;

/**
 * Interface for station removal operators
 */
@FunctionalInterface
public interface StationRemovalOperator {
    /**
     * Remove stations from solution
     *
     * @param solution Solution to remove stations from
     * @param sigma Number of stations to remove
     * @return List of removed station IDs
     */
    List<Integer> remove(Solution solution, int sigma);
}
