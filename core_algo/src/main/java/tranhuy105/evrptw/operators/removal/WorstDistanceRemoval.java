package tranhuy105.evrptw.operators.removal;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import tranhuy105.evrptw.model.CustomerPosition;
import tranhuy105.evrptw.model.Instance;
import tranhuy105.evrptw.model.Solution;
import tranhuy105.evrptw.util.Constants;
import tranhuy105.evrptw.util.StationAssociation;

/**
 * Worst distance removal: remove customers with highest distance cost
 */
public class WorstDistanceRemoval implements RemovalOperator {
    private final Random random = new Random();

    @Override
    public List<Integer> remove(Solution solution, int q) {
        List<CustomerPosition> customers = solution.getAllCustomersInRoutes();
        if (customers.isEmpty()) {
            return new ArrayList<>();
        }

        Instance inst = solution.getInstance();
        q = Math.min(q, customers.size());

        // Calculate cost for each customer
        List<CostEntry> costs = new ArrayList<>();
        for (CustomerPosition cp : customers) {
            List<Integer> route = solution.getRoutes().get(cp.routeIndex());
            int pos = cp.position();

            // Get predecessor and successor
            int prevId = pos > 0 ? route.get(pos - 1) : 0;
            int nextId = pos < route.size() - 1 ? route.get(pos + 1) : 0;

            // Cost = d(prev, cust) + d(cust, next) - d(prev, next)
            double cost = inst.distance(prevId, cp.customerId()) +
                         inst.distance(cp.customerId(), nextId) -
                         inst.distance(prevId, nextId);

            costs.add(new CostEntry(cost, cp));
        }

        // Sort by cost descending
        costs.sort((a, b) -> Double.compare(b.cost, a.cost));

        // Select with randomness
        List<CustomerPosition> toRemove = new ArrayList<>();
        List<CostEntry> available = new ArrayList<>(costs);

        while (toRemove.size() < q && !available.isEmpty()) {
            int idx = (int) (available.size() * Math.pow(random.nextDouble(), Constants.WORST_KAPPA));
            idx = Math.min(idx, available.size() - 1);
            toRemove.add(available.remove(idx).position);
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

    private record CostEntry(double cost, CustomerPosition position) {}
}
