package com.tranhuy105.server.algorithm.operator.removal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.springframework.stereotype.Component;

import com.tranhuy105.server.algorithm.operator.RemovalOperator;
import com.tranhuy105.server.domain.CustomerPosition;
import com.tranhuy105.server.domain.Instance;
import com.tranhuy105.server.domain.NodeType;
import com.tranhuy105.server.domain.Route;
import com.tranhuy105.server.domain.Solution;

/**
 * Random removal: randomly select customers to remove
 */
@Component
public class RandomRemoval implements RemovalOperator {
    private final Random random = new Random();

    @Override
    public String getName() {
        return "random";
    }

    @Override
    public List<Integer> remove(Solution solution, int count, Instance instance) {
        List<CustomerPosition> customers = getAllCustomersInRoutes(solution, instance);
        if (customers.isEmpty()) {
            return new ArrayList<>();
        }

        count = Math.min(count, customers.size());
        
        // Shuffle and take first 'count' customers
        Collections.shuffle(customers, random);
        List<CustomerPosition> toRemove = customers.subList(0, count);
        
        return removeCustomers(solution, toRemove);
    }

    protected List<CustomerPosition> getAllCustomersInRoutes(Solution solution, Instance instance) {
        List<CustomerPosition> result = new ArrayList<>();
        List<Route> routes = solution.getRoutes();
        
        for (int rIdx = 0; rIdx < routes.size(); rIdx++) {
            List<Integer> stops = routes.get(rIdx).getStops();
            for (int pos = 0; pos < stops.size(); pos++) {
                int nodeId = stops.get(pos);
                if (instance.getAllNodes().get(nodeId).getType() == NodeType.CUSTOMER) {
                    result.add(new CustomerPosition(rIdx, pos, nodeId));
                }
            }
        }
        return result;
    }

    protected List<Integer> removeCustomers(Solution solution, List<CustomerPosition> toRemove) {
        // Sort by (routeIdx, position) descending to avoid index shift issues
        toRemove.sort((a, b) -> {
            int cmp = Integer.compare(b.routeIndex(), a.routeIndex());
            if (cmp != 0) return cmp;
            return Integer.compare(b.position(), a.position());
        });

        List<Integer> removedIds = new ArrayList<>();
        for (CustomerPosition cp : toRemove) {
            List<Integer> stops = solution.getRoutes().get(cp.routeIndex()).getStops();
            if (cp.position() < stops.size()) {
                stops.remove(cp.position());
                removedIds.add(cp.customerId());
            }
        }
        
        // Clean empty routes
        solution.getRoutes().removeIf(Route::isEmpty);
        
        return removedIds;
    }
}
