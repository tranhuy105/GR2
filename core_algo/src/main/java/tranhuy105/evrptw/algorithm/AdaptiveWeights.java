package tranhuy105.evrptw.algorithm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import tranhuy105.evrptw.util.Constants;
import tranhuy105.evrptw.util.Logger;

/**
 * Adaptive weight system for operator selection in ALNS.
 * Optimized: uses primitive arrays for O(1) access instead of HashMap lookups.
 */
public class AdaptiveWeights {
    private final String[] operators;
    private final Map<String, Integer> operatorIndex; // For name -> index lookup
    private final double[] weights;
    private final double[] scores;
    private final int[] usage;
    private final Random random;
    private final int n;

    public AdaptiveWeights(List<String> operatorList) {
        this.n = operatorList.size();
        this.operators = operatorList.toArray(new String[0]);
        this.operatorIndex = new HashMap<>();
        this.weights = new double[n];
        this.scores = new double[n];
        this.usage = new int[n];
        this.random = new Random();

        // Initialize all operators with weight 1.0
        for (int i = 0; i < n; i++) {
            operatorIndex.put(operators[i], i);
            weights[i] = 1.0;
            scores[i] = 0.0;
            usage[i] = 0;
        }
    }

    /**
     * Select an operator using roulette wheel selection based on weights.
     * Optimized: uses primitive array iteration.
     */
    public String select() {
        // Calculate total weight
        double total = 0.0;
        for (int i = 0; i < n; i++) {
            total += weights[i];
        }

        // If total is too small, select randomly
        if (total < 1e-9) {
            return operators[random.nextInt(n)];
        }

        // Roulette wheel selection
        double r = random.nextDouble() * total;
        double cumsum = 0.0;

        for (int i = 0; i < n; i++) {
            cumsum += weights[i];
            if (r <= cumsum) {
                return operators[i];
            }
        }

        // Fallback (should rarely happen due to floating point)
        return operators[n - 1];
    }

    /**
     * Record that an operator was used.
     * O(1) with HashMap lookup + array access.
     */
    public void recordUsage(String operator) {
        int idx = operatorIndex.get(operator);
        usage[idx]++;
    }

    /**
     * Update score for an operator based on result type.
     * O(1) with HashMap lookup + array access.
     */
    public void updateScore(String operator, ResultType resultType) {
        int idx = operatorIndex.get(operator);
        scores[idx] += resultType.getScore();
    }

    /**
     * Update weights based on accumulated scores and usage.
     * Formula: weight = weight * (1 - RHO) + RHO * (score / usage)
     */
    public void updateWeights() {
        double oneMinusRho = 1.0 - Constants.RHO;
        
        for (int i = 0; i < n; i++) {
            if (usage[i] > 0) {
                weights[i] = weights[i] * oneMinusRho + Constants.RHO * (scores[i] / usage[i]);
            }
        }

        // Reset scores and usage
        Arrays.fill(scores, 0.0);
        Arrays.fill(usage, 0);

        // Log updated weights at debug level
        if (Logger.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder("Updated weights: {");
            for (int i = 0; i < n; i++) {
                if (i > 0) sb.append(", ");
                sb.append(operators[i]).append("=").append(String.format("%.4f", weights[i]));
            }
            sb.append("}");
            Logger.debug(sb.toString());
        }
    }

    /**
     * Get current weights (for debugging/monitoring)
     */
    public Map<String, Double> getWeights() {
        Map<String, Double> result = new HashMap<>();
        for (int i = 0; i < n; i++) {
            result.put(operators[i], weights[i]);
        }
        return result;
    }

    /**
     * Get operators list
     */
    public List<String> getOperators() {
        return new ArrayList<>(Arrays.asList(operators));
    }
}
