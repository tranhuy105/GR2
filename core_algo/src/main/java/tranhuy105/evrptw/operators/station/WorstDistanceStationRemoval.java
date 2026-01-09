package tranhuy105.evrptw.operators.station;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import tranhuy105.evrptw.model.Instance;
import tranhuy105.evrptw.model.NodeType;
import tranhuy105.evrptw.model.Solution;
import tranhuy105.evrptw.util.Constants;

/**
 * Worst distance station removal: remove stations with highest distance cost
 */
public class WorstDistanceStationRemoval implements StationRemovalOperator {
    private final Random random = new Random();

    @Override
    public List<Integer> remove(Solution solution, int sigma) {
        Instance inst = solution.getInstance();
        
        // Collect all stations with their costs
        List<StationCostEntry> stations = new ArrayList<>();
        
        for (int rIdx = 0; rIdx < solution.getRoutes().size(); rIdx++) {
            List<Integer> route = solution.getRoutes().get(rIdx);
            for (int pos = 0; pos < route.size(); pos++) {
                int nodeId = route.get(pos);
                if (inst.getAllNodes().get(nodeId).getType() == NodeType.STATION) {
                    int prevId = pos > 0 ? route.get(pos - 1) : 0;
                    int nextId = pos < route.size() - 1 ? route.get(pos + 1) : 0;

                    double cost = inst.distance(prevId, nodeId) +
                                 inst.distance(nodeId, nextId) -
                                 inst.distance(prevId, nextId);

                    stations.add(new StationCostEntry(cost, rIdx, pos, nodeId));
                }
            }
        }

        if (stations.isEmpty()) {
            return new ArrayList<>();
        }

        // Sort by cost descending
        stations.sort((a, b) -> Double.compare(b.cost, a.cost));
        sigma = Math.min(sigma, stations.size());

        // Select with randomness
        List<StationCostEntry> toRemove = new ArrayList<>();
        List<StationCostEntry> available = new ArrayList<>(stations);

        for (int i = 0; i < sigma && !available.isEmpty(); i++) {
            int idx = (int) (available.size() * Math.pow(random.nextDouble(), Constants.WORST_KAPPA));
            idx = Math.min(idx, available.size() - 1);
            toRemove.add(available.remove(idx));
        }

        // Sort by (routeIdx, position) descending
        toRemove.sort((a, b) -> {
            int cmp = Integer.compare(b.routeIdx, a.routeIdx);
            if (cmp != 0) return cmp;
            return Integer.compare(b.position, a.position);
        });

        List<Integer> removed = new ArrayList<>();
        for (StationCostEntry entry : toRemove) {
            List<Integer> route = solution.getRoutes().get(entry.routeIdx);
            if (entry.position < route.size()) {
                route.remove(entry.position);
                removed.add(entry.nodeId);
            }
        }

        return removed;
    }

    private record StationCostEntry(double cost, int routeIdx, int position, int nodeId) {}
}
