package tranhuy105.evrptw.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a solution to the EVRPTW problem
 */
public class Solution {
    private final Instance instance;
    private List<List<Integer>> routes;
    private double cost;
    private double totalDistance;
    private double totalViolations;

    public Solution(Instance instance) {
        this.instance = instance;
        this.routes = new ArrayList<>();
        this.cost = 0.0;
        this.totalDistance = 0.0;
        this.totalViolations = 0.0;
    }

    public Instance getInstance() {
        return instance;
    }

    public List<List<Integer>> getRoutes() {
        return routes;
    }

    public void setRoutes(List<List<Integer>> routes) {
        this.routes = routes;
    }

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }

    public double getTotalDistance() {
        return totalDistance;
    }

    public void setTotalDistance(double totalDistance) {
        this.totalDistance = totalDistance;
    }

    public double getTotalViolations() {
        return totalViolations;
    }

    public void setTotalViolations(double totalViolations) {
        this.totalViolations = totalViolations;
    }

    /**
     * Create a deep copy of this solution.
     * Optimized: pre-allocates ArrayList capacity.
     */
    public Solution copy() {
        Solution newSol = new Solution(instance);
        int numRoutes = routes.size();
        newSol.routes = new ArrayList<>(numRoutes);
        for (int i = 0; i < numRoutes; i++) {
            List<Integer> route = routes.get(i);
            newSol.routes.add(new ArrayList<>(route));
        }
        newSol.cost = cost;
        newSol.totalDistance = totalDistance;
        newSol.totalViolations = totalViolations;
        return newSol;
    }

    /**
     * Check if solution is feasible (no violations)
     */
    public boolean isFeasible() {
        return totalViolations < 1e-6;
    }

    /**
     * Get all customers in routes with their positions
     */
    public List<CustomerPosition> getAllCustomersInRoutes() {
        List<CustomerPosition> result = new ArrayList<>();
        for (int rIdx = 0; rIdx < routes.size(); rIdx++) {
            List<Integer> route = routes.get(rIdx);
            for (int pos = 0; pos < route.size(); pos++) {
                int nodeId = route.get(pos);
                if (instance.getAllNodes().get(nodeId).getType() == NodeType.CUSTOMER) {
                    result.add(new CustomerPosition(rIdx, pos, nodeId));
                }
            }
        }
        return result;
    }

    /**
     * Get route statistics using RouteEvaluator
     * Note: This requires a RouteEvaluator instance. 
     * For direct use, create a RouteEvaluator and call evaluate().
     */
    public RouteStats getRouteStats(List<Integer> route) {
        // Delegate to RouteEvaluator
        return new tranhuy105.evrptw.algorithm.RouteEvaluator(instance).evaluate(route);
    }

    /**
     * Calculate total cost using RouteEvaluator
     */
    public void calculateTotalCost() {
        new tranhuy105.evrptw.algorithm.RouteEvaluator(instance).calculateTotalCost(this);
    }
}
