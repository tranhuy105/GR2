package tranhuy105.evrptw.algorithm;

import java.util.List;

import tranhuy105.evrptw.model.ChargingMode;
import tranhuy105.evrptw.model.Instance;
import tranhuy105.evrptw.model.Node;
import tranhuy105.evrptw.model.NodeType;
import tranhuy105.evrptw.model.RouteStats;
import tranhuy105.evrptw.model.Solution;
import tranhuy105.evrptw.util.Constants;

/**
 * Evaluates routes and calculates costs with constraint violations.
 * Supports both full recharge and battery swap charging modes.
 */
public class RouteEvaluator {
    private final Instance instance;
    private final List<Node> allNodes;
    private final double[][] distMatrix;
    private final double[][] travelTimeMatrix;
    private final double[][] energyMatrix;
    
    private final double qBattery;
    private final double cCapacity;
    private final double refuelRate;
    private final double depotDue;
    private final ChargingMode chargingMode;
    private final double swapTime;

    public RouteEvaluator(Instance instance) {
        this.instance = instance;
        this.allNodes = instance.getAllNodes();
        this.distMatrix = instance.getDistanceMatrix();
        this.travelTimeMatrix = instance.getTravelTimeMatrix();
        this.energyMatrix = instance.getEnergyMatrix();
        
        this.qBattery = instance.getBatteryCapacity();
        this.cCapacity = instance.getCargoCapacity();
        this.refuelRate = instance.getRefuelRate();
        this.depotDue = instance.getDepot().getDueTime();
        this.chargingMode = instance.getChargingMode();
        this.swapTime = instance.getBatterySwapTime();
    }

    /**
     * Mutable state holder for route simulation
     */
    private static class RouteState {
        double dist = 0.0;
        double time = 0.0;
        double battery;
        double load = 0.0;
        double violCap = 0.0;
        double violTw = 0.0;
        double violBat = 0.0;
        int prevNodeId = 0;

        RouteState(double battery) {
            this.battery = battery;
        }
    }


    /**
     * Process a single node visit, updating state accordingly
     */
    private void visitNode(RouteState state, int nodeId) {
        Node node = allNodes.get(nodeId);
        int prevId = state.prevNodeId;

        // Travel to node
        state.dist += distMatrix[prevId][nodeId];
        state.time += travelTimeMatrix[prevId][nodeId];
        state.battery -= energyMatrix[prevId][nodeId];

        // Check battery violation
        if (state.battery < -1e-6) {
            state.violBat -= state.battery;
        }

        // Wait if arriving early
        if (state.time < node.getReadyTime()) {
            state.time = node.getReadyTime();
        }

        // Check time window violation
        if (state.time > node.getDueTime()) {
            state.violTw += state.time - node.getDueTime();
        }

        // Process based on node type
        if (node.getType() == NodeType.CUSTOMER) {
            state.load += node.getDemand();
            state.time += node.getServiceTime();
            if (state.load > cCapacity) {
                state.violCap += state.load - cCapacity;
            }
        } else if (node.getType() == NodeType.STATION) {
            processCharging(state);
        }

        state.prevNodeId = nodeId;
    }

    /**
     * Process charging at a station based on charging mode
     */
    private void processCharging(RouteState state) {
        if (chargingMode == ChargingMode.BATTERY_SWAP) {
            state.time += swapTime;
        } else {
            // Full recharge
            double amountToCharge = qBattery - state.battery;
            if (amountToCharge > 0) {
                state.time += amountToCharge * refuelRate;
            }
        }
        state.battery = qBattery;
    }

    /**
     * Process return to depot
     */
    private void returnToDepot(RouteState state) {
        int lastId = state.prevNodeId;
        state.dist += distMatrix[lastId][0];
        state.time += travelTimeMatrix[lastId][0];
        state.battery -= energyMatrix[lastId][0];

        if (state.battery < -1e-6) {
            state.violBat -= state.battery;
        }
        if (state.time > depotDue) {
            state.violTw += state.time - depotDue;
        }
    }

    /**
     * Calculate cost from state
     */
    private double calculateCost(RouteState state) {
        return state.dist +
               Constants.PENALTY_CAPACITY * state.violCap +
               Constants.PENALTY_TIME * state.violTw +
               Constants.PENALTY_BATTERY * state.violBat;
    }

    /**
     * Build RouteStats from state
     */
    private RouteStats buildStats(RouteState state) {
        return new RouteStats(
            calculateCost(state),
            state.dist,
            state.violCap,
            state.violTw,
            state.violBat
        );
    }

    /**
     * Evaluate a single route
     */
    public RouteStats evaluate(List<Integer> route) {
        RouteState state = new RouteState(qBattery);

        for (int nodeId : route) {
            visitNode(state, nodeId);
        }

        if (!route.isEmpty()) {
            returnToDepot(state);
        }

        return buildStats(state);
    }

    /**
     * Evaluate route with a node inserted at a specific position
     */
    public RouteStats evaluateWithInsertion(List<Integer> route, int insertPos, int insertId) {
        RouteState state = new RouteState(qBattery);

        // Process nodes before insertion
        for (int i = 0; i < insertPos; i++) {
            visitNode(state, route.get(i));
        }

        // Process inserted node
        visitNode(state, insertId);

        // Process nodes after insertion
        for (int i = insertPos; i < route.size(); i++) {
            visitNode(state, route.get(i));
        }

        returnToDepot(state);
        return buildStats(state);
    }

    /**
     * Evaluate route with two nodes inserted at a specific position
     */
    public RouteStats evaluateWithDoubleInsertion(List<Integer> route, int insertPos,
                                                   int firstId, int secondId) {
        RouteState state = new RouteState(qBattery);

        // Process nodes before insertion
        for (int i = 0; i < insertPos; i++) {
            visitNode(state, route.get(i));
        }

        // Process inserted nodes
        visitNode(state, firstId);
        visitNode(state, secondId);

        // Process nodes after insertion
        for (int i = insertPos; i < route.size(); i++) {
            visitNode(state, route.get(i));
        }

        returnToDepot(state);
        return buildStats(state);
    }


    /**
     * Get forward states for a route (for optimized insertion evaluation).
     * State array: [dist, time, battery, load, violCap, violTw, violBat]
     * Index i = state AFTER visiting route[i-1], or initial state if i=0.
     */
    public double[][] getForwardStates(List<Integer> route) {
        int size = route.size();
        double[][] states = new double[size + 1][7];

        RouteState state = new RouteState(qBattery);

        // Save initial state
        saveState(states[0], state);

        for (int i = 0; i < size; i++) {
            visitNode(state, route.get(i));
            saveState(states[i + 1], state);
        }

        return states;
    }

    private void saveState(double[] arr, RouteState state) {
        arr[0] = state.dist;
        arr[1] = state.time;
        arr[2] = state.battery;
        arr[3] = state.load;
        arr[4] = state.violCap;
        arr[5] = state.violTw;
        arr[6] = state.violBat;
    }

    private RouteState loadState(double[] arr, int prevNodeId) {
        RouteState state = new RouteState(arr[2]);
        state.dist = arr[0];
        state.time = arr[1];
        state.load = arr[3];
        state.violCap = arr[4];
        state.violTw = arr[5];
        state.violBat = arr[6];
        state.prevNodeId = prevNodeId;
        return state;
    }

    /**
     * Optimized evaluation with insertion using cached forward states
     */
    public RouteStats evaluateWithInsertion(List<Integer> route, int insertPos,
                                            int insertId, double[][] forwardStates) {
        int prevNodeId = (insertPos == 0) ? 0 : route.get(insertPos - 1);
        RouteState state = loadState(forwardStates[insertPos], prevNodeId);

        // Process inserted node
        visitNode(state, insertId);

        // Process suffix
        for (int i = insertPos; i < route.size(); i++) {
            visitNode(state, route.get(i));
        }

        returnToDepot(state);
        return buildStats(state);
    }

    /**
     * Optimized double insertion using cached forward states
     */
    public RouteStats evaluateWithDoubleInsertion(List<Integer> route, int insertPos,
                                                   int firstId, int secondId,
                                                   double[][] forwardStates) {
        int prevNodeId = (insertPos == 0) ? 0 : route.get(insertPos - 1);
        RouteState state = loadState(forwardStates[insertPos], prevNodeId);

        // Process inserted nodes
        visitNode(state, firstId);
        visitNode(state, secondId);

        // Process suffix
        for (int i = insertPos; i < route.size(); i++) {
            visitNode(state, route.get(i));
        }

        returnToDepot(state);
        return buildStats(state);
    }

    /**
     * Calculate route cost from pre-computed forward states
     */
    public double getCostFromForwardStates(double[][] forwardStates, List<Integer> route) {
        int size = route.size();
        if (size == 0) {
            return 0.0;
        }

        int lastNodeId = route.get(size - 1);
        RouteState state = loadState(forwardStates[size], lastNodeId);
        returnToDepot(state);
        return calculateCost(state);
    }

    /**
     * Calculate total cost for a complete solution
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
            totalViol += stats.capacityViolation() +
                        stats.timeViolation() +
                        stats.batteryViolation();
        }

        solution.setCost(totalCost);
        solution.setTotalDistance(totalDist);
        solution.setTotalViolations(totalViol);
    }

    public Instance getInstance() {
        return instance;
    }
}
