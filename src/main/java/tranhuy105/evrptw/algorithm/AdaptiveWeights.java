package tranhuy105.evrptw.algorithm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import tranhuy105.evrptw.util.Constants;
import tranhuy105.evrptw.util.Logger;

/**
 * Adaptive weight system for operator selection in ALNS
 */
public class AdaptiveWeights {
    private final List<String> operators;
    private final Map<String, Double> weights;
    private final Map<String, Double> scores;
    private final Map<String, Integer> usage;
    private final Random random;

    public AdaptiveWeights(List<String> operators) {
        this.operators = new ArrayList<>(operators);
        this.weights = new HashMap<>();
        this.scores = new HashMap<>();
        this.usage = new HashMap<>();
        this.random = new Random();

        // Initialize all operators with weight 1.0
        for (String op : operators) {
            weights.put(op, 1.0);
            scores.put(op, 0.0);
            usage.put(op, 0);
        }
    }

    /**
     * Select an operator using roulette wheel selection based on weights
     */
    public String select() {
        // Calculate total weight
        double total = 0.0;
        for (String op : operators) {
            total += weights.get(op);
        }

        // If total is too small, select randomly
        if (total < 1e-9) {
            return operators.get(random.nextInt(operators.size()));
        }

        // Roulette wheel selection
        double r = random.nextDouble() * total;
        double cumsum = 0.0;

        for (String op : operators) {
            cumsum += weights.get(op);
            if (r <= cumsum) {
                return op;
            }
        }

        // Fallback (should rarely happen due to floating point)
        return operators.get(operators.size() - 1);
    }

    /**
     * Record that an operator was used
     */
    public void recordUsage(String operator) {
        usage.put(operator, usage.get(operator) + 1);
    }

    /**
     * Update score for an operator based on result type
     */
    public void updateScore(String operator, ResultType resultType) {
        double currentScore = scores.get(operator);
        scores.put(operator, currentScore + resultType.getScore());
    }

    /**
     * Update weights based on accumulated scores and usage
     * Formula: weight = weight * (1 - RHO) + RHO * (score / usage)
     */
    public void updateWeights() {
        for (String op : operators) {
            int usageCount = usage.get(op);
            if (usageCount > 0) {
                double currentWeight = weights.get(op);
                double score = scores.get(op);
                double newWeight = currentWeight * (1 - Constants.RHO) + 
                                  Constants.RHO * (score / usageCount);
                weights.put(op, newWeight);
            }
        }

        // Reset scores and usage
        for (String op : operators) {
            scores.put(op, 0.0);
            usage.put(op, 0);
        }

        // Log updated weights at debug level
        Logger.debug("Updated weights: " + weights);
    }

    /**
     * Get current weights (for debugging/monitoring)
     */
    public Map<String, Double> getWeights() {
        return new HashMap<>(weights);
    }

    /**
     * Get operators list
     */
    public List<String> getOperators() {
        return new ArrayList<>(operators);
    }
}
