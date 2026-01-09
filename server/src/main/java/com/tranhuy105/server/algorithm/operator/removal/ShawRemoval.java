package com.tranhuy105.server.algorithm.operator.removal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import org.springframework.stereotype.Component;

import com.tranhuy105.server.algorithm.operator.RemovalOperator;
import com.tranhuy105.server.config.ALNSProperties;
import com.tranhuy105.server.domain.CustomerPosition;
import com.tranhuy105.server.domain.Instance;
import com.tranhuy105.server.domain.Node;
import com.tranhuy105.server.domain.NodeType;
import com.tranhuy105.server.domain.Route;
import com.tranhuy105.server.domain.Solution;

import lombok.RequiredArgsConstructor;

/**
 * Shaw removal: remove customers that are similar to each other
 */
@Component
@RequiredArgsConstructor
public class ShawRemoval implements RemovalOperator {
    private final ALNSProperties properties;
    private final Random random = new Random();

    @Override
    public String getName() {
        return "shaw";
    }

    @Override
    public List<Integer> remove(Solution solution, int count, Instance instance) {
        List<CustomerPosition> customers = getAllCustomersInRoutes(solution, instance);
        if (customers.isEmpty()) {
            return new ArrayList<>();
        }

        count = Math.min(count, customers.size());

        // Pick random seed customer
        int seedIdx = random.nextInt(customers.size());
        CustomerPosition seed = customers.get(seedIdx);
        Node seedNode = instance.getAllNodes().get(seed.customerId());

        // Calculate relatedness for all other customers
        double[] phi = properties.shaw().phi();
        double eta = properties.shaw().eta();
        
        List<RelatednessEntry> relatedness = new ArrayList<>();
        for (CustomerPosition cp : customers) {
            if (cp.customerId() == seed.customerId()) {
                continue;
            }
            Node node = instance.getAllNodes().get(cp.customerId());

            // R = phi1*dist + phi2*|time_diff| + phi3*same_route + phi4*|demand_diff|
            double dist = instance.distance(seed.customerId(), cp.customerId());
            double timeDiff = Math.abs(seedNode.getReadyTime() - node.getReadyTime());
            double sameRoute = cp.routeIndex() == seed.routeIndex() ? -1.0 : 1.0;
            double demandDiff = Math.abs(seedNode.getDemand() - node.getDemand());

            double R = phi[0] * dist + phi[1] * timeDiff + phi[2] * sameRoute + phi[3] * demandDiff;
            relatedness.add(new RelatednessEntry(R, cp));
        }

        // Sort by relatedness (lower = more similar)
        relatedness.sort(Comparator.comparingDouble(e -> e.relatedness));

        // Select customers to remove (with randomness via eta)
        List<CustomerPosition> toRemove = new ArrayList<>();
        toRemove.add(seed);

        while (toRemove.size() < count && !relatedness.isEmpty()) {
            int idx = (int) (relatedness.size() * Math.pow(random.nextDouble(), eta));
            idx = Math.min(idx, relatedness.size() - 1);
            toRemove.add(relatedness.remove(idx).position);
        }

        return removeCustomers(solution, toRemove);
    }

    private List<CustomerPosition> getAllCustomersInRoutes(Solution solution, Instance instance) {
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

    private record RelatednessEntry(double relatedness, CustomerPosition position) {}
}
