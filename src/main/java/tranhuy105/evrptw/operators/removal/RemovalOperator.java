package tranhuy105.evrptw.operators.removal;

import java.util.List;

import tranhuy105.evrptw.model.Solution;

/**
 * Interface for removal operators
 */
@FunctionalInterface
public interface RemovalOperator {
    /**
     * Remove customers from solution
     *
     * @param solution Solution to remove customers from
     * @param q Number of customers to remove
     * @return List of removed customer IDs
     */
    List<Integer> remove(Solution solution, int q);
}
