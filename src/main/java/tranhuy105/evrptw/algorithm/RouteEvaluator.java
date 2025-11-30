package tranhuy105.evrptw.algorithm;

import java.util.List;

import tranhuy105.evrptw.model.Instance;
import tranhuy105.evrptw.model.Node;
import tranhuy105.evrptw.model.NodeType;
import tranhuy105.evrptw.model.RouteStats;
import tranhuy105.evrptw.model.Solution;
import tranhuy105.evrptw.util.Constants;

/**
 * Evaluates routes and calculates costs with constraint violations
 */
public class RouteEvaluator {
    private final Instance instance;

    public RouteEvaluator(Instance instance) {
        this.instance = instance;
    }

    /**
     * Evaluate a single route with FULL RECHARGE policy at stations.
     * Returns route statistics including cost, distance, and violations.
     *
     * @param route List of node IDs (excluding depot at start/end)
     * @return RouteStats with cost, distance, and violation metrics
     */
    public RouteStats evaluate(List<Integer> route) {
        // Cache frequently accessed values for performance
        List<Node> allNodes = instance.getAllNodes();
        double[][] distMatrix = instance.getDistanceMatrix();
        double[][] travelTimeMatrix = instance.getTravelTimeMatrix();
        double[][] energyMatrix = instance.getEnergyMatrix();
        
        double qBattery = instance.getBatteryCapacity();
        double cCapacity = instance.getCargoCapacity();
        double gRefuelRate = instance.getRefuelRate();
        double depotDue = instance.getDepot().getDueTime();

        // State variables
        double dist = 0.0;
        double load = 0.0;
        double currTime = 0.0;
        double currBat = qBattery;

        // Violation tracking
        double violCap = 0.0;
        double violTw = 0.0;
        double violBat = 0.0;

        int prevNodeId = 0;  // Start from depot

        // Simulate route
        for (int nodeId : route) {
            Node node = allNodes.get(nodeId);

            // Travel to this node
            dist += distMatrix[prevNodeId][nodeId];
            currTime += travelTimeMatrix[prevNodeId][nodeId];
            currBat -= energyMatrix[prevNodeId][nodeId];

            // Check battery violation
            if (currBat < -1e-6) {
                violBat += -currBat;
            }

            // Wait if arriving before ready time
            if (currTime < node.getReadyTime()) {
                currTime = node.getReadyTime();
            }

            // Check time window violation
            if (currTime > node.getDueTime()) {
                violTw += currTime - node.getDueTime();
            }

            NodeType nodeType = node.getType();
            
            if (nodeType == NodeType.CUSTOMER) {
                // Customer: add demand and service time
                load += node.getDemand();
                currTime += node.getServiceTime();
                
                // Check capacity violation
                if (load > cCapacity) {
                    violCap += load - cCapacity;
                }
                
            } else if (nodeType == NodeType.STATION) {
                // Station: full recharge
                double amountToCharge = qBattery - currBat;
                if (amountToCharge > 0) {
                    currTime += amountToCharge * gRefuelRate;
                }
                currBat = qBattery;
            }

            prevNodeId = nodeId;
        }

        // Return to depot
        dist += distMatrix[prevNodeId][0];
        currTime += travelTimeMatrix[prevNodeId][0];
        currBat -= energyMatrix[prevNodeId][0];

        // Check battery violation on return
        if (currBat < -1e-6) {
            violBat += -currBat;
        }

        // Check depot time window violation
        if (currTime > depotDue) {
            violTw += currTime - depotDue;
        }

        // Calculate total cost
        double cost = dist + 
                     (Constants.PENALTY_CAPACITY * violCap) +
                     (Constants.PENALTY_TIME * violTw) +
                     (Constants.PENALTY_BATTERY * violBat);

        return new RouteStats(cost, dist, violCap, violTw, violBat);
    }

    /**
     * Calculate total cost for a complete solution
     *
     * @param solution Solution to evaluate
     */
    public void calculateTotalCost(Solution solution) {
        double totalCost = solution.getRoutes().size() * Constants.PENALTY_VEHICLE;
        double totalDist = 0.0;
        double totalViol = 0.0;

        for (List<Integer> route : solution.getRoutes()) {
            if (route.isEmpty()) {
                continue;
            }

            RouteStats stats = evaluate(route);
            totalCost += stats.cost();
            totalDist += stats.distance();
            totalViol += (stats.capacityViolation() + 
                         stats.timeViolation() + 
                         stats.batteryViolation());
        }

        solution.setCost(totalCost);
        solution.setTotalDistance(totalDist);
        solution.setTotalViolations(totalViol);
    }
}
