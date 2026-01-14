package tranhuy105.evrptw.operators.insertion;

import java.util.List;

import tranhuy105.evrptw.model.Solution;

/**
 * Interface for insertion operators
 */
@FunctionalInterface
public interface InsertionOperator {
    /**
     * Insert unassigned customers into solution
     *
     * @param solution Solution to insert customers into
     * @param unassigned List of unassigned customer IDs
     */
    void insert(Solution solution, List<Integer> unassigned);
}
