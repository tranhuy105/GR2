package tranhuy105.evrptw.operators.removal;

import java.util.ArrayList;
import java.util.List;

import tranhuy105.evrptw.model.Instance;
import tranhuy105.evrptw.model.NodeType;
import tranhuy105.evrptw.model.Solution;
import tranhuy105.evrptw.util.StationAssociation;

/**
 * Helper methods for removal operators
 */
public class RemovalHelper {

    /**
     * Remove customer at position with optional station association
     */
    public static List<Integer> removeWithAssociation(Solution solution, int routeIdx, int pos,
                                                       StationAssociation association) {
        List<Integer> route = solution.getRoutes().get(routeIdx);
        if (pos >= route.size()) {
            return new ArrayList<>();
        }

        List<Integer> removed = new ArrayList<>();
        Instance inst = solution.getInstance();

        // Check bounds and remove based on association
        if (association == StationAssociation.RCWPS && pos > 0) {
            int prevNodeId = route.get(pos - 1);
            if (inst.getAllNodes().get(prevNodeId).getType() == NodeType.STATION) {
                removed.add(route.remove(pos - 1));
                pos--;
            }
        }

        if (pos < route.size()) {
            removed.add(route.remove(pos));
        }

        if (association == StationAssociation.RCWSS && pos < route.size()) {
            int nextNodeId = route.get(pos);
            if (inst.getAllNodes().get(nextNodeId).getType() == NodeType.STATION) {
                removed.add(route.remove(pos));
            }
        }

        return removed;
    }

    /**
     * Filter removed IDs to only include customers
     */
    public static List<Integer> filterCustomersOnly(List<Integer> removedIds, Instance instance) {
        List<Integer> customers = new ArrayList<>();
        for (int nodeId : removedIds) {
            if (instance.getAllNodes().get(nodeId).getType() == NodeType.CUSTOMER) {
                customers.add(nodeId);
            }
        }
        return customers;
    }
}
