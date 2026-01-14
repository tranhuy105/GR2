package com.tranhuy105.server.algorithm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Adaptive weight system for operator selection in ALNS.
 * Uses roulette wheel selection with score-based weight updates.
 */
@Component
@Slf4j
public class AdaptiveWeightManager {
    private static final double RHO = 0.1;  // Learning rate
    
    private final Map<String, double[]> operatorGroups = new HashMap<>();
    // Each group: [weight, score, usage] arrays indexed by operator
    
    private final Map<String, Map<String, Integer>> nameToIndex = new HashMap<>();
    private final Map<String, String[]> indexToName = new HashMap<>();
    private final Random random = new Random();

    /**
     * Register a group of operators
     */
    public void registerGroup(String groupName, List<String> operatorNames) {
        int n = operatorNames.size();
        double[] data = new double[n * 3]; // weight, score, usage per operator
        
        Map<String, Integer> nameMap = new HashMap<>();
        String[] names = new String[n];
        
        for (int i = 0; i < n; i++) {
            data[i] = 1.0; // Initial weight = 1.0
            nameMap.put(operatorNames.get(i), i);
            names[i] = operatorNames.get(i);
        }
        
        operatorGroups.put(groupName, data);
        nameToIndex.put(groupName, nameMap);
        indexToName.put(groupName, names);
        
        log.debug("Registered operator group '{}' with {} operators", groupName, n);
    }

    /**
     * Select an operator using roulette wheel selection
     */
    public String select(String groupName) {
        double[] data = operatorGroups.get(groupName);
        String[] names = indexToName.get(groupName);
        int n = names.length;
        
        // Calculate total weight
        double total = 0.0;
        for (int i = 0; i < n; i++) {
            total += data[i];
        }
        
        if (total < 1e-9) {
            return names[random.nextInt(n)];
        }
        
        // Roulette wheel selection
        double r = random.nextDouble() * total;
        double cumsum = 0.0;
        
        for (int i = 0; i < n; i++) {
            cumsum += data[i];
            if (r <= cumsum) {
                return names[i];
            }
        }
        
        return names[n - 1];
    }

    /**
     * Record that an operator was used
     */
    public void recordUsage(String groupName, String operatorName) {
        double[] data = operatorGroups.get(groupName);
        int idx = nameToIndex.get(groupName).get(operatorName);
        int n = indexToName.get(groupName).length;
        data[2 * n + idx]++; // usage is at offset 2*n
    }

    /**
     * Update score for an operator
     */
    public void updateScore(String groupName, String operatorName, int score) {
        double[] data = operatorGroups.get(groupName);
        int idx = nameToIndex.get(groupName).get(operatorName);
        int n = indexToName.get(groupName).length;
        data[n + idx] += score; // score is at offset n
    }

    /**
     * Update weights based on accumulated scores and usage.
     * Formula: weight = weight * (1 - RHO) + RHO * (score / usage)
     */
    public void updateWeights(String groupName) {
        double[] data = operatorGroups.get(groupName);
        String[] names = indexToName.get(groupName);
        int n = names.length;
        double oneMinusRho = 1.0 - RHO;
        
        for (int i = 0; i < n; i++) {
            double usage = data[2 * n + i];
            if (usage > 0) {
                double score = data[n + i];
                data[i] = data[i] * oneMinusRho + RHO * (score / usage);
            }
        }
        
        // Reset scores and usage
        for (int i = 0; i < n; i++) {
            data[n + i] = 0.0;
            data[2 * n + i] = 0.0;
        }
        
        if (log.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder("Updated weights for '" + groupName + "': {");
            for (int i = 0; i < n; i++) {
                if (i > 0) sb.append(", ");
                sb.append(names[i]).append("=").append(String.format("%.4f", data[i]));
            }
            sb.append("}");
            log.debug(sb.toString());
        }
    }

    /**
     * Update weights for all registered groups
     */
    public void updateAllWeights() {
        for (String groupName : operatorGroups.keySet()) {
            updateWeights(groupName);
        }
    }

    /**
     * Get registered operator names for a group
     */
    public Set<String> getOperatorNames(String groupName) {
        return nameToIndex.get(groupName).keySet();
    }
}
