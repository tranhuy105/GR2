package tranhuy105.evrptw.operators.removal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import tranhuy105.evrptw.model.CustomerPosition;
import tranhuy105.evrptw.model.Solution;
import tranhuy105.evrptw.util.StationAssociation;

/**
 * Random removal: remove q random customers
 */
public class RandomRemoval implements RemovalOperator {
    private final Random random = new Random();

    @Override
    public List<Integer> remove(Solution solution, int q) {
        List<CustomerPosition> customers = solution.getAllCustomersInRoutes();
        if (customers.isEmpty()) {
            return new ArrayList<>();
        }

        q = Math.min(q, customers.size());
        
        // Randomly select customers to remove
        List<CustomerPosition> toRemove = new ArrayList<>();
        List<CustomerPosition> available = new ArrayList<>(customers);
        Collections.shuffle(available, random);
        
        for (int i = 0; i < q && i < available.size(); i++) {
            toRemove.add(available.get(i));
        }

        // Sort by (routeIdx, position) descending to maintain indices during removal
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
}
