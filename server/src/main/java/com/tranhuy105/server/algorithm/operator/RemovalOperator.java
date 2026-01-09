package com.tranhuy105.server.algorithm.operator;

import java.util.List;

import com.tranhuy105.server.domain.Instance;
import com.tranhuy105.server.domain.Solution;

/**
 * Interface for removal operators in ALNS
 */
public interface RemovalOperator {
    /**
     * Get unique name for adaptive weight tracking
     */
    String getName();
    
    /**
     * Remove customers from solution
     * @param solution Current solution (will be modified)
     * @param count Number of customers to remove
     * @param instance Problem instance
     * @return List of removed customer IDs
     */
    List<Integer> remove(Solution solution, int count, Instance instance);
}
