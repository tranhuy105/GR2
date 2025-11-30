package tranhuy105.evrptw.operators.station;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import tranhuy105.evrptw.model.NodeType;
import tranhuy105.evrptw.model.Solution;

/**
 * Random station removal: remove random stations from routes
 */
public class RandomStationRemoval implements StationRemovalOperator {
    private final Random random = new Random();

    @Override
    public List<Integer> remove(Solution solution, int sigma) {
        // Collect all stations in routes
        List<StationPosition> stations = new ArrayList<>();
        for (int rIdx = 0; rIdx < solution.getRoutes().size(); rIdx++) {
            List<Integer> route = solution.getRoutes().get(rIdx);
            for (int pos = 0; pos < route.size(); pos++) {
                int nodeId = route.get(pos);
                if (solution.getInstance().getAllNodes().get(nodeId).getType() == NodeType.STATION) {
                    stations.add(new StationPosition(rIdx, pos, nodeId));
                }
            }
        }

        if (stations.isEmpty()) {
            return new ArrayList<>();
        }

        sigma = Math.min(sigma, stations.size());
        
        // Randomly select stations to remove
        Collections.shuffle(stations, random);
        List<StationPosition> toRemove = new ArrayList<>(stations.subList(0, sigma));

        // Sort by (routeIdx, position) descending
        toRemove.sort((a, b) -> {
            int cmp = Integer.compare(b.routeIdx, a.routeIdx);
            if (cmp != 0) return cmp;
            return Integer.compare(b.position, a.position);
        });

        List<Integer> removed = new ArrayList<>();
        for (StationPosition sp : toRemove) {
            List<Integer> route = solution.getRoutes().get(sp.routeIdx);
            if (sp.position < route.size()) {
                route.remove(sp.position);
                removed.add(sp.nodeId);
            }
        }

        return removed;
    }

    private record StationPosition(int routeIdx, int position, int nodeId) {}
}
