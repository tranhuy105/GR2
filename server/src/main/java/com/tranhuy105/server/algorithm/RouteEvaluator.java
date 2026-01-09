package com.tranhuy105.server.algorithm;

import java.util.List;

import org.springframework.stereotype.Component;

import com.tranhuy105.server.config.ALNSProperties;
import com.tranhuy105.server.domain.ChargingMode;
import com.tranhuy105.server.domain.Instance;
import com.tranhuy105.server.domain.Node;
import com.tranhuy105.server.domain.NodeType;
import com.tranhuy105.server.domain.Route;
import com.tranhuy105.server.domain.RouteStats;
import com.tranhuy105.server.domain.Solution;
import com.tranhuy105.server.domain.VehicleSpec;

import lombok.RequiredArgsConstructor;

/**
 * Evaluates routes and calculates costs with constraint violations.
 * Supports both full recharge and battery swap charging modes.
 */
@Component
@RequiredArgsConstructor
public class RouteEvaluator {
    private final ALNSProperties properties;

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
     * Evaluate a single route
     */
    public RouteStats evaluate(List<Integer> route, Instance instance) {
        RouteState state = new RouteState(instance.getVehicleSpec().getBatteryCapacity());

        for (int nodeId : route) {
            visitNode(state, nodeId, instance);
        }

        if (!route.isEmpty()) {
            returnToDepot(state, instance);
        }

        return buildStats(state, instance);
    }

    /**
     * Evaluate route with a node inserted at a specific position
     */
    public RouteStats evaluateWithInsertion(List<Integer> route, int insertPos, int insertId, Instance instance) {
        RouteState state = new RouteState(instance.getVehicleSpec().getBatteryCapacity());

        // Process nodes before insertion
        for (int i = 0; i < insertPos; i++) {
            visitNode(state, route.get(i), instance);
        }

        // Process inserted node
        visitNode(state, insertId, instance);

        // Process nodes after insertion
        for (int i = insertPos; i < route.size(); i++) {
            visitNode(state, route.get(i), instance);
        }

        returnToDepot(state, instance);
        return buildStats(state, instance);
    }

    /**
     * Evaluate route with two nodes inserted at a specific position
     */
    public RouteStats evaluateWithDoubleInsertion(List<Integer> route, int insertPos,
                                                   int firstId, int secondId, Instance instance) {
        RouteState state = new RouteState(instance.getVehicleSpec().getBatteryCapacity());

        // Process nodes before insertion
        for (int i = 0; i < insertPos; i++) {
            visitNode(state, route.get(i), instance);
        }

        // Process inserted nodes
        visitNode(state, firstId, instance);
        visitNode(state, secondId, instance);

        // Process nodes after insertion
        for (int i = insertPos; i < route.size(); i++) {
            visitNode(state, route.get(i), instance);
        }

        returnToDepot(state, instance);
        return buildStats(state, instance);
    }

    /**
     * Get forward states for a route (for optimized insertion evaluation).
     * State array: [dist, time, battery, load, violCap, violTw, violBat]
     */
    public double[][] getForwardStates(List<Integer> route, Instance instance) {
        int size = route.size();
        double[][] states = new double[size + 1][7];

        RouteState state = new RouteState(instance.getVehicleSpec().getBatteryCapacity());

        // Save initial state
        saveState(states[0], state);

        for (int i = 0; i < size; i++) {
            visitNode(state, route.get(i), instance);
            saveState(states[i + 1], state);
        }

        return states;
    }

    /**
     * Optimized evaluation with insertion using cached forward states
     */
    public RouteStats evaluateWithInsertion(List<Integer> route, int insertPos,
                                            int insertId, double[][] forwardStates, Instance instance) {
        int prevNodeId = (insertPos == 0) ? 0 : route.get(insertPos - 1);
        RouteState state = loadState(forwardStates[insertPos], prevNodeId, instance);

        // Process inserted node
        visitNode(state, insertId, instance);

        // Process suffix
        for (int i = insertPos; i < route.size(); i++) {
            visitNode(state, route.get(i), instance);
        }

        returnToDepot(state, instance);
        return buildStats(state, instance);
    }

    /**
     * Calculate total cost for a complete solution
     */
    public void calculateTotalCost(Solution solution, Instance instance) {
        double totalCost = solution.getRoutes().size() * properties.penalties().vehicle();
        double totalDist = 0.0;
        double totalViol = 0.0;

        for (Route route : solution.getRoutes()) {
            if (route.isEmpty()) {
                continue;
            }

            RouteStats stats = evaluate(route.getStops(), instance);
            totalCost += stats.cost();
            totalDist += stats.distance();
            totalViol += stats.totalViolation();
            
            // Update route stats
            route.setDistance(stats.distance());
            route.setCapacityViolation(stats.capacityViolation());
            route.setTimeViolation(stats.timeViolation());
            route.setBatteryViolation(stats.batteryViolation());
        }

        solution.setCost(totalCost);
        solution.setTotalDistance(totalDist);
        solution.setTotalViolations(totalViol);
    }

    // ==================== Private Methods ====================

    private void visitNode(RouteState state, int nodeId, Instance instance) {
        Node node = instance.getAllNodes().get(nodeId);
        VehicleSpec vehicle = instance.getVehicleSpec();
        int prevId = state.prevNodeId;

        // Travel to node
        state.dist += instance.distance(prevId, nodeId);
        state.time += instance.travelTime(prevId, nodeId);
        state.battery -= instance.energy(prevId, nodeId);

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
            if (state.load > vehicle.getCargoCapacity()) {
                state.violCap += state.load - vehicle.getCargoCapacity();
            }
        } else if (node.getType() == NodeType.STATION) {
            processCharging(state, vehicle);
        }

        state.prevNodeId = nodeId;
    }

    private void processCharging(RouteState state, VehicleSpec vehicle) {
        if (vehicle.getChargingMode() == ChargingMode.BATTERY_SWAP) {
            state.time += vehicle.getBatterySwapTime();
        } else {
            // Full recharge
            double amountToCharge = vehicle.getBatteryCapacity() - state.battery;
            if (amountToCharge > 0) {
                state.time += amountToCharge * vehicle.getRefuelRate();
            }
        }
        state.battery = vehicle.getBatteryCapacity();
    }

    private void returnToDepot(RouteState state, Instance instance) {
        int lastId = state.prevNodeId;
        state.dist += instance.distance(lastId, 0);
        state.time += instance.travelTime(lastId, 0);
        state.battery -= instance.energy(lastId, 0);

        if (state.battery < -1e-6) {
            state.violBat -= state.battery;
        }
        
        double depotDue = instance.getDepot().getDueTime();
        if (state.time > depotDue) {
            state.violTw += state.time - depotDue;
        }
    }

    private RouteStats buildStats(RouteState state, Instance instance) {
        double cost = state.dist +
               properties.penalties().capacity() * state.violCap +
               properties.penalties().time() * state.violTw +
               properties.penalties().battery() * state.violBat;
        
        return new RouteStats(cost, state.dist, state.violCap, state.violTw, state.violBat);
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

    private RouteState loadState(double[] arr, int prevNodeId, Instance instance) {
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
}
