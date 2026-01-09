package com.tranhuy105.server.algorithm.operator;

import java.util.List;

import com.tranhuy105.server.domain.Instance;
import com.tranhuy105.server.domain.Solution;

/**
 * Interface for insertion operators in ALNS
 */
public interface InsertionOperator {
    /**
     * Get unique name for adaptive weight tracking
     */
    String getName();
    
    /**
     * Insert customers into solution
     * @param solution Current solution (will be modified)
     * @param customersToInsert List of customer IDs to insert
     * @param instance Problem instance
     */
    void insert(Solution solution, List<Integer> customersToInsert, Instance instance);
}
