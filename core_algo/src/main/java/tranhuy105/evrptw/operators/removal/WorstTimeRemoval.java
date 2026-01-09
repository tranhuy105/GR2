package tranhuy105.evrptw.operators.removal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import tranhuy105.evrptw.model.CustomerPosition;
import tranhuy105.evrptw.model.Instance;
import tranhuy105.evrptw.model.Node;
import tranhuy105.evrptw.model.NodeType;
import tranhuy105.evrptw.model.Solution;
import tranhuy105.evrptw.util.Constants;
import tranhuy105.evrptw.util.StationAssociation;

/**
 * Worst time removal: remove customers with largest time slack
 */
public class WorstTimeRemoval implements RemovalOperator {
    private final Random random = new Random();

    @Override
    public List<Integer> remove(Solution solution, int q) {
        List<CustomerPosition> customers = solution.getAllCustomersInRoutes();
        if (customers.isEmpty()) {
            return new ArrayList<>();
        }

        Instance inst = solution.getInstance();
        List<Node> allNodes = inst.getAllNodes();
        double qBattery = inst.getBatteryCapacity();
        double gRefuelRate = inst.getRefuelRate();
        double[][] travelTimeMatrix = inst.getTravelTimeMatrix();
        double[][] energyMatrix = inst.getEnergyMatrix();

        q = Math.min(q, customers.size());

        // Simulate routes to get arrival times
        Map<CustomerPositionKey, Double> arrivalTimes = new HashMap<>();
        
        for (int rIdx = 0; rIdx < solution.getRoutes().size(); rIdx++) {
            List<Integer> route = solution.getRoutes().get(rIdx);
            double currTime = 0.0;
            double currBat = qBattery;
            int prevId = 0;

            for (int pos = 0; pos < route.size(); pos++) {
                int nodeId = route.get(pos);
                Node node = allNodes.get(nodeId);
                
                currTime += travelTimeMatrix[prevId][nodeId];
                currBat -= energyMatrix[prevId][nodeId];

                if (currTime < node.getReadyTime()) {
                    currTime = node.getReadyTime();
                }

                NodeType nodeType = node.getType();
                if (nodeType == NodeType.CUSTOMER) {
                    double slack = currTime - node.getReadyTime();
                    arrivalTimes.put(new CustomerPositionKey(rIdx, pos, nodeId), slack);
                    currTime += node.getServiceTime();
                } else if (nodeType == NodeType.STATION) {
                    double chargeAmount = qBattery - currBat;
                    if (chargeAmount > 0) {
                        currTime += chargeAmount * gRefuelRate;
                    }
                    currBat = qBattery;
                }

                prevId = nodeId;
            }
        }

        // Sort by slack descending
        List<SlackEntry> costs = new ArrayList<>();
        for (Map.Entry<CustomerPositionKey, Double> entry : arrivalTimes.entrySet()) {
            CustomerPositionKey key = entry.getKey();
            costs.add(new SlackEntry(entry.getValue(), 
                    new CustomerPosition(key.routeIdx, key.position, key.customerId)));
        }
        costs.sort((a, b) -> Double.compare(b.slack, a.slack));

        // Select with randomness
        List<CustomerPosition> toRemove = new ArrayList<>();
        List<SlackEntry> available = new ArrayList<>(costs);

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

    private record CustomerPositionKey(int routeIdx, int position, int customerId) {}
    private record SlackEntry(double slack, CustomerPosition position) {}
}
