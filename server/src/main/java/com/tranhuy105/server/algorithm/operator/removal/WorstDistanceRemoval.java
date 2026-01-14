package com.tranhuy105.server.algorithm.operator.removal;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.springframework.stereotype.Component;

import com.tranhuy105.server.algorithm.operator.RemovalOperator;
import com.tranhuy105.server.domain.CustomerPosition;
import com.tranhuy105.server.domain.Instance;
import com.tranhuy105.server.domain.Node;
import com.tranhuy105.server.domain.NodeType;
import com.tranhuy105.server.domain.Route;
import com.tranhuy105.server.domain.Solution;

import lombok.RequiredArgsConstructor;

/**
 * Worst distance removal: remove customers with highest distance contribution
 */
@Component
@RequiredArgsConstructor
public class WorstDistanceRemoval implements RemovalOperator {
    private static final double KAPPA = 4.0;  // Determinism factor
    private final Random random = new Random();

    @Override
    public String getName() {
        return "worst_distance";
    }

    @Override
    public List<Integer> remove(Solution solution, int count, Instance instance) {
        List<CustomerWithCost> customerCosts = new ArrayList<>();
        
        for (int rIdx = 0; rIdx < solution.getRoutes().size(); rIdx++) {
            List<Integer> stops = solution.getRoutes().get(rIdx).getStops();
            
            for (int pos = 0; pos < stops.size(); pos++) {
                int nodeId = stops.get(pos);
                Node node = instance.getAllNodes().get(nodeId);
                
                if (node.getType() == NodeType.CUSTOMER) {
                    // Calculate distance contribution
                    int prevId = (pos == 0) ? 0 : stops.get(pos - 1);
                    int nextId = (pos == stops.size() - 1) ? 0 : stops.get(pos + 1);
                    
                    double cost = instance.distance(prevId, nodeId) + instance.distance(nodeId, nextId);
                    customerCosts.add(new CustomerWithCost(new CustomerPosition(rIdx, pos, nodeId), cost));
                }
            }
        }

        if (customerCosts.isEmpty()) {
            return new ArrayList<>();
        }

        count = Math.min(count, customerCosts.size());
        
        // Sort by cost descending (worst first)
        customerCosts.sort((a, b) -> Double.compare(b.cost, a.cost));
        
        // Select with randomness
        List<CustomerPosition> toRemove = new ArrayList<>();
        while (toRemove.size() < count && !customerCosts.isEmpty()) {
            int idx = (int) (customerCosts.size() * Math.pow(random.nextDouble(), KAPPA));
            idx = Math.min(idx, customerCosts.size() - 1);
            toRemove.add(customerCosts.remove(idx).position);
        }

        return removeCustomers(solution, toRemove);
    }

    private List<Integer> removeCustomers(Solution solution, List<CustomerPosition> toRemove) {
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
        
        solution.getRoutes().removeIf(Route::isEmpty);
        return removedIds;
    }

    private record CustomerWithCost(CustomerPosition position, double cost) {}
}
