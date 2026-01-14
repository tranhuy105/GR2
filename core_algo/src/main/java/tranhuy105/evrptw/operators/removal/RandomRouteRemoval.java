package tranhuy105.evrptw.operators.removal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import tranhuy105.evrptw.model.NodeType;
import tranhuy105.evrptw.model.Solution;

/**
 * Random route removal: remove entire routes to reduce vehicle count
 */
public class RandomRouteRemoval implements RemovalOperator {
    private final Random random = new Random();

    @Override
    public List<Integer> remove(Solution solution, int q) {
        // q is ignored for route removal - we remove 10-30% of routes
        if (solution.getRoutes().size() <= 1) {
            return new ArrayList<>();
        }

        // Remove 10-30% of routes
        int numToRemove = Math.max(1, (int) (solution.getRoutes().size() * 
                                            (0.1 + random.nextDouble() * 0.2)));
        
        // Select routes to remove
        List<Integer> routeIndices = new ArrayList<>();
        for (int i = 0; i < solution.getRoutes().size(); i++) {
            routeIndices.add(i);
        }
        Collections.shuffle(routeIndices, random);
        
        List<Integer> routesToRemove = new ArrayList<>(routeIndices.subList(0, 
                Math.min(numToRemove, routeIndices.size())));
        
        // Sort descending to maintain indices during removal
        routesToRemove.sort(Collections.reverseOrder());

        List<Integer> removedIds = new ArrayList<>();
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
}
