package tranhuy105.evrptw.operators.removal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import tranhuy105.evrptw.model.NodeType;
import tranhuy105.evrptw.model.Solution;

/**
 * Greedy Route Removal (GRR): Remove routes with fewest customers.
 * Motivation: distribute customers from short routes into other routes
 * to reduce vehicle count.
 */
public class GreedyRouteRemoval implements RemovalOperator {

    @Override
    public List<Integer> remove(Solution solution, int q) {
        // q is ignored for route removal
        if (solution.getRoutes().size() <= 1) {
            return new ArrayList<>();
        }

        // Count customers per route (exclude stations)
        List<RouteCustomerCount> routeCustomerCounts = new ArrayList<>();
        for (int rIdx = 0; rIdx < solution.getRoutes().size(); rIdx++) {
            List<Integer> route = solution.getRoutes().get(rIdx);
            int custCount = 0;
            for (int nodeId : route) {
                if (solution.getInstance().getAllNodes().get(nodeId).getType() == NodeType.CUSTOMER) {
                    custCount++;
                }
            }
            routeCustomerCounts.add(new RouteCustomerCount(custCount, rIdx));
        }

        // Sort by customer count ascending (shortest routes first)
        routeCustomerCounts.sort((a, b) -> Integer.compare(a.customerCount, b.customerCount));

        // Remove 1-2 shortest routes
        int numToRemove = Math.min(2, Math.max(1, solution.getRoutes().size() / 5));

        List<Integer> removedIds = new ArrayList<>();
        List<Integer> routesToRemove = new ArrayList<>();

        for (int i = 0; i < Math.min(numToRemove, routeCustomerCounts.size()); i++) {
            routesToRemove.add(routeCustomerCounts.get(i).routeIndex);
        }

        // Remove in reverse order to maintain indices
        routesToRemove.sort(Collections.reverseOrder());

        for (int rIdx : routesToRemove) {
            List<Integer> route = solution.getRoutes().remove(rIdx);
            for (int nodeId : route) {
                if (solution.getInstance().getAllNodes().get(nodeId).getType() == NodeType.CUSTOMER) {
                    removedIds.add(nodeId);
                }
            }
        }

        return removedIds;
    }

    /**
     * Helper class to track route index and customer count
     */
    private static class RouteCustomerCount {
        final int customerCount;
        final int routeIndex;

        RouteCustomerCount(int customerCount, int routeIndex) {
            this.customerCount = customerCount;
            this.routeIndex = routeIndex;
        }
    }
}
