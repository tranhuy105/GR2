package com.tranhuy105.server.algorithm.operator.removal;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.springframework.stereotype.Component;

import com.tranhuy105.server.algorithm.operator.RemovalOperator;
import com.tranhuy105.server.domain.Instance;
import com.tranhuy105.server.domain.NodeType;
import com.tranhuy105.server.domain.Route;
import com.tranhuy105.server.domain.Solution;

/**
 * Random route removal: remove all customers from a randomly selected route
 */
@Component
public class RandomRouteRemoval implements RemovalOperator {
    private final Random random = new Random();

    @Override
    public String getName() {
        return "random_route";
    }

    @Override
    public List<Integer> remove(Solution solution, int count, Instance instance) {
        List<Route> routes = solution.getRoutes();
        if (routes.isEmpty()) {
            return new ArrayList<>();
        }

        // Select random route
        int routeIdx = random.nextInt(routes.size());
        List<Integer> stops = routes.get(routeIdx).getStops();
        
        // Collect customer IDs
        List<Integer> removedCustomers = new ArrayList<>();
        for (int nodeId : stops) {
            if (instance.getAllNodes().get(nodeId).getType() == NodeType.CUSTOMER) {
                removedCustomers.add(nodeId);
            }
        }
        
        // Remove the entire route
        routes.remove(routeIdx);
        
        return removedCustomers;
    }
}
