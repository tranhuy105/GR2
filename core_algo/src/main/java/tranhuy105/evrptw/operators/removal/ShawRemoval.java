package tranhuy105.evrptw.operators.removal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import tranhuy105.evrptw.model.CustomerPosition;
import tranhuy105.evrptw.model.Instance;
import tranhuy105.evrptw.model.Node;
import tranhuy105.evrptw.model.Solution;
import tranhuy105.evrptw.util.Constants;
import tranhuy105.evrptw.util.StationAssociation;

/**
 * Shaw removal: remove customers that are similar to each other
 */
public class ShawRemoval implements RemovalOperator {
    private final Random random = new Random();

    @Override
    public List<Integer> remove(Solution solution, int q) {
        List<CustomerPosition> customers = solution.getAllCustomersInRoutes();
        if (customers.isEmpty()) {
            return new ArrayList<>();
        }

        Instance inst = solution.getInstance();
        q = Math.min(q, customers.size());

        // Pick random seed customer
        int seedIdx = random.nextInt(customers.size());
        CustomerPosition seed = customers.get(seedIdx);
        Node seedNode = inst.getAllNodes().get(seed.customerId());

        // Calculate relatedness for all other customers
        List<RelatednessEntry> relatedness = new ArrayList<>();
        for (CustomerPosition cp : customers) {
            if (cp.customerId() == seed.customerId()) {
                continue;
            }
            Node node = inst.getAllNodes().get(cp.customerId());

            // R = phi1*dist + phi2*|time_diff| + phi3*same_route + phi4*|demand_diff|
            double dist = inst.distance(seed.customerId(), cp.customerId());
            double timeDiff = Math.abs(seedNode.getReadyTime() - node.getReadyTime());
            double sameRoute = cp.routeIndex() == seed.routeIndex() ? -1.0 : 1.0;
            double demandDiff = Math.abs(seedNode.getDemand() - node.getDemand());

            double R = Constants.SHAW_PHI[0] * dist +
                       Constants.SHAW_PHI[1] * timeDiff +
                       Constants.SHAW_PHI[2] * sameRoute +
                       Constants.SHAW_PHI[3] * demandDiff;

            relatedness.add(new RelatednessEntry(R, cp));
        }

        // Sort by relatedness (lower = more similar)
        relatedness.sort(Comparator.comparingDouble(e -> e.relatedness));

        // Select customers to remove (with randomness via eta)
        List<CustomerPosition> toRemove = new ArrayList<>();
        toRemove.add(seed);

        while (toRemove.size() < q && !relatedness.isEmpty()) {
            // Pick from sorted list with randomness
            int idx = (int) (relatedness.size() * Math.pow(random.nextDouble(), Constants.SHAW_ETA));
            idx = Math.min(idx, relatedness.size() - 1);
            toRemove.add(relatedness.remove(idx).position);
        }

        // Sort by (routeIdx, position) descending
        toRemove.sort((a, b) -> {
            int cmp = Integer.compare(b.routeIndex(), a.routeIndex());
            if (cmp != 0) return cmp;
            return Integer.compare(b.position(), a.position());
        });

        List<Integer> removedIds = new ArrayList<>();
        for (CustomerPosition cp : toRemove) {
            StationAssociation association = StationAssociation.random();
            removedIds.addAll(RemovalHelper.removeWithAssociation(
                    solution, cp.routeIndex(), cp.position(), association
            ));
        }

        return RemovalHelper.filterCustomersOnly(removedIds, solution.getInstance());
    }

    private record RelatednessEntry(double relatedness, CustomerPosition position) {}
}
