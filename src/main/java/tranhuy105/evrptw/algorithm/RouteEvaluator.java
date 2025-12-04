package tranhuy105.evrptw.algorithm;

import java.util.List;

import tranhuy105.evrptw.model.Instance;
import tranhuy105.evrptw.model.Node;
import tranhuy105.evrptw.model.RouteStats;
import tranhuy105.evrptw.model.Solution;
import tranhuy105.evrptw.util.Constants;

/**
 * Evaluates routes and calculates costs with constraint violations.
 * Optimized with cached primitive arrays for O(1) node data access.
 */
public class RouteEvaluator {
    private final Instance instance;
    
    // Cached primitive arrays for fast access (avoid Node object lookups)
    private final double[][] distMatrix;
    private final double[][] travelTimeMatrix;
    private final double[][] energyMatrix;
    private final double[] readyTimes;
    private final double[] dueTimes;
    private final double[] demands;
    private final double[] serviceTimes;
    private final int[] nodeTypes; // 0=depot, 1=customer, 2=station
    
    private final double qBattery;
    private final double cCapacity;
    private final double gRefuelRate;
    private final double depotDue;

    public RouteEvaluator(Instance instance) {
        this.instance = instance;
        
        // Cache matrices
        this.distMatrix = instance.getDistanceMatrix();
        this.travelTimeMatrix = instance.getTravelTimeMatrix();
        this.energyMatrix = instance.getEnergyMatrix();
        
        // Cache parameters
        this.qBattery = instance.getBatteryCapacity();
        this.cCapacity = instance.getCargoCapacity();
        this.gRefuelRate = instance.getRefuelRate();
        this.depotDue = instance.getDepot().getDueTime();
        
        // Build primitive arrays for node data (O(1) access vs O(1) with object overhead)
        List<Node> allNodes = instance.getAllNodes();
        int n = allNodes.size();
        this.readyTimes = new double[n];
        this.dueTimes = new double[n];
        this.demands = new double[n];
        this.serviceTimes = new double[n];
        this.nodeTypes = new int[n];
        
        for (int i = 0; i < n; i++) {
            Node node = allNodes.get(i);
            readyTimes[i] = node.getReadyTime();
            dueTimes[i] = node.getDueTime();
            demands[i] = node.getDemand();
            serviceTimes[i] = node.getServiceTime();
            nodeTypes[i] = switch (node.getType()) {
                case DEPOT -> 0;
                case CUSTOMER -> 1;
                case STATION -> 2;
            };
        }
    }

    /**
     * Evaluate a single route with FULL RECHARGE policy at stations.
     * Returns route statistics including cost, distance, and violations.
     * Optimized: uses primitive arrays for O(1) data access.
     *
     * @param route List of node IDs (excluding depot at start/end)
     * @return RouteStats with cost, distance, and violation metrics
     */
    public RouteStats evaluate(List<Integer> route) {
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

        // Simulate route using primitive array lookups
        for (int i = 0, size = route.size(); i < size; i++) {
            int nodeId = route.get(i);

            // Travel to this node (direct array access)
            dist += distMatrix[prevNodeId][nodeId];
            currTime += travelTimeMatrix[prevNodeId][nodeId];
            currBat -= energyMatrix[prevNodeId][nodeId];

            // Check battery violation
            if (currBat < -1e-6) {
                violBat -= currBat;
            }

            // Wait if arriving before ready time
            double ready = readyTimes[nodeId];
            if (currTime < ready) {
                currTime = ready;
            }

            // Check time window violation
            double due = dueTimes[nodeId];
            if (currTime > due) {
                violTw += currTime - due;
            }

            int nodeType = nodeTypes[nodeId];
            
            if (nodeType == 1) { // CUSTOMER
                // Customer: add demand and service time
                load += demands[nodeId];
                currTime += serviceTimes[nodeId];
                
                // Check capacity violation
                if (load > cCapacity) {
                    violCap += load - cCapacity;
                }
                
            } else if (nodeType == 2) { // STATION
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
            violBat -= currBat;
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
     * Evaluate route with a customer inserted at a specific position.
     * Avoids creating new ArrayList - simulates insertion in-place.
     * 
     * @param route Original route
     * @param insertPos Position to insert (0 to route.size())
     * @param customerId Customer to insert
     * @return RouteStats for the modified route
     */
    public RouteStats evaluateWithInsertion(List<Integer> route, int insertPos, int customerId) {
        double dist = 0.0;
        double load = 0.0;
        double currTime = 0.0;
        double currBat = qBattery;
        double violCap = 0.0;
        double violTw = 0.0;
        double violBat = 0.0;

        int prevNodeId = 0;
        int routeSize = route.size();
        
        // Process nodes before insertion point
        for (int i = 0; i < insertPos; i++) {
            int nodeId = route.get(i);
            dist += distMatrix[prevNodeId][nodeId];
            currTime += travelTimeMatrix[prevNodeId][nodeId];
            currBat -= energyMatrix[prevNodeId][nodeId];
            
            if (currBat < -1e-6) violBat -= currBat;
            
            double ready = readyTimes[nodeId];
            if (currTime < ready) currTime = ready;
            
            double due = dueTimes[nodeId];
            if (currTime > due) violTw += currTime - due;
            
            int nodeType = nodeTypes[nodeId];
            if (nodeType == 1) {
                load += demands[nodeId];
                currTime += serviceTimes[nodeId];
                if (load > cCapacity) violCap += load - cCapacity;
            } else if (nodeType == 2) {
                double charge = qBattery - currBat;
                if (charge > 0) currTime += charge * gRefuelRate;
                currBat = qBattery;
            }
            prevNodeId = nodeId;
        }
        
        // Process inserted customer
        dist += distMatrix[prevNodeId][customerId];
        currTime += travelTimeMatrix[prevNodeId][customerId];
        currBat -= energyMatrix[prevNodeId][customerId];
        
        if (currBat < -1e-6) violBat -= currBat;
        
        double ready = readyTimes[customerId];
        if (currTime < ready) currTime = ready;
        
        double due = dueTimes[customerId];
        if (currTime > due) violTw += currTime - due;
        
        int nodeType = nodeTypes[customerId];
        if (nodeType == 1) {
            load += demands[customerId];
            currTime += serviceTimes[customerId];
            if (load > cCapacity) violCap += load - cCapacity;
        } else if (nodeType == 2) {
            double charge = qBattery - currBat;
            if (charge > 0) currTime += charge * gRefuelRate;
            currBat = qBattery;
        }
        prevNodeId = customerId;
        
        // Process nodes after insertion point
        for (int i = insertPos; i < routeSize; i++) {
            int nodeId = route.get(i);
            dist += distMatrix[prevNodeId][nodeId];
            currTime += travelTimeMatrix[prevNodeId][nodeId];
            currBat -= energyMatrix[prevNodeId][nodeId];
            
            if (currBat < -1e-6) violBat -= currBat;
            
            ready = readyTimes[nodeId];
            if (currTime < ready) currTime = ready;
            
            due = dueTimes[nodeId];
            if (currTime > due) violTw += currTime - due;
            
            nodeType = nodeTypes[nodeId];
            if (nodeType == 1) {
                load += demands[nodeId];
                currTime += serviceTimes[nodeId];
                if (load > cCapacity) violCap += load - cCapacity;
            } else if (nodeType == 2) {
                double charge = qBattery - currBat;
                if (charge > 0) currTime += charge * gRefuelRate;
                currBat = qBattery;
            }
            prevNodeId = nodeId;
        }
        
        // Return to depot
        dist += distMatrix[prevNodeId][0];
        currTime += travelTimeMatrix[prevNodeId][0];
        currBat -= energyMatrix[prevNodeId][0];
        
        if (currBat < -1e-6) violBat -= currBat;
        if (currTime > depotDue) violTw += currTime - depotDue;
        
        double cost = dist + (Constants.PENALTY_CAPACITY * violCap) +
                     (Constants.PENALTY_TIME * violTw) + (Constants.PENALTY_BATTERY * violBat);
        
        return new RouteStats(cost, dist, violCap, violTw, violBat);
    }
    
    /**
     * Evaluate route with two nodes inserted at a specific position.
     * Used for station+customer insertion. Avoids ArrayList creation.
     */
    public RouteStats evaluateWithDoubleInsertion(List<Integer> route, int insertPos, 
                                                   int firstId, int secondId) {
        double dist = 0.0;
        double load = 0.0;
        double currTime = 0.0;
        double currBat = qBattery;
        double violCap = 0.0;
        double violTw = 0.0;
        double violBat = 0.0;

        int prevNodeId = 0;
        int routeSize = route.size();
        
        // Process nodes before insertion point
        for (int i = 0; i < insertPos; i++) {
            int nodeId = route.get(i);
            dist += distMatrix[prevNodeId][nodeId];
            currTime += travelTimeMatrix[prevNodeId][nodeId];
            currBat -= energyMatrix[prevNodeId][nodeId];
            
            if (currBat < -1e-6) violBat -= currBat;
            
            double ready = readyTimes[nodeId];
            if (currTime < ready) currTime = ready;
            
            double due = dueTimes[nodeId];
            if (currTime > due) violTw += currTime - due;
            
            int nodeType = nodeTypes[nodeId];
            if (nodeType == 1) {
                load += demands[nodeId];
                currTime += serviceTimes[nodeId];
                if (load > cCapacity) violCap += load - cCapacity;
            } else if (nodeType == 2) {
                double charge = qBattery - currBat;
                if (charge > 0) currTime += charge * gRefuelRate;
                currBat = qBattery;
            }
            prevNodeId = nodeId;
        }
        
        // Process first inserted node
        dist += distMatrix[prevNodeId][firstId];
        currTime += travelTimeMatrix[prevNodeId][firstId];
        currBat -= energyMatrix[prevNodeId][firstId];
        
        if (currBat < -1e-6) violBat -= currBat;
        
        double ready = readyTimes[firstId];
        if (currTime < ready) currTime = ready;
        
        double due = dueTimes[firstId];
        if (currTime > due) violTw += currTime - due;
        
        int nodeType = nodeTypes[firstId];
        if (nodeType == 1) {
            load += demands[firstId];
            currTime += serviceTimes[firstId];
            if (load > cCapacity) violCap += load - cCapacity;
        } else if (nodeType == 2) {
            double charge = qBattery - currBat;
            if (charge > 0) currTime += charge * gRefuelRate;
            currBat = qBattery;
        }
        prevNodeId = firstId;
        
        // Process second inserted node
        dist += distMatrix[prevNodeId][secondId];
        currTime += travelTimeMatrix[prevNodeId][secondId];
        currBat -= energyMatrix[prevNodeId][secondId];
        
        if (currBat < -1e-6) violBat -= currBat;
        
        ready = readyTimes[secondId];
        if (currTime < ready) currTime = ready;
        
        due = dueTimes[secondId];
        if (currTime > due) violTw += currTime - due;
        
        nodeType = nodeTypes[secondId];
        if (nodeType == 1) {
            load += demands[secondId];
            currTime += serviceTimes[secondId];
            if (load > cCapacity) violCap += load - cCapacity;
        } else if (nodeType == 2) {
            double charge = qBattery - currBat;
            if (charge > 0) currTime += charge * gRefuelRate;
            currBat = qBattery;
        }
        prevNodeId = secondId;
        
        // Process nodes after insertion point
        for (int i = insertPos; i < routeSize; i++) {
            int nodeId = route.get(i);
            dist += distMatrix[prevNodeId][nodeId];
            currTime += travelTimeMatrix[prevNodeId][nodeId];
            currBat -= energyMatrix[prevNodeId][nodeId];
            
            if (currBat < -1e-6) violBat -= currBat;
            
            ready = readyTimes[nodeId];
            if (currTime < ready) currTime = ready;
            
            due = dueTimes[nodeId];
            if (currTime > due) violTw += currTime - due;
            
            nodeType = nodeTypes[nodeId];
            if (nodeType == 1) {
                load += demands[nodeId];
                currTime += serviceTimes[nodeId];
                if (load > cCapacity) violCap += load - cCapacity;
            } else if (nodeType == 2) {
                double charge = qBattery - currBat;
                if (charge > 0) currTime += charge * gRefuelRate;
                currBat = qBattery;
            }
            prevNodeId = nodeId;
        }
        
        // Return to depot
        dist += distMatrix[prevNodeId][0];
        currTime += travelTimeMatrix[prevNodeId][0];
        currBat -= energyMatrix[prevNodeId][0];
        
        if (currBat < -1e-6) violBat -= currBat;
        if (currTime > depotDue) violTw += currTime - depotDue;
        
        double cost = dist + (Constants.PENALTY_CAPACITY * violCap) +
                     (Constants.PENALTY_TIME * violTw) + (Constants.PENALTY_BATTERY * violBat);
        
        return new RouteStats(cost, dist, violCap, violTw, violBat);
    }

    /**
     * Calculate total cost for a complete solution
     *
     * @param solution Solution to evaluate
     */
    /**
     * Get forward states for a route.
     * State: [dist, time, battery, load, violCap, violTw, violBat]
     * Index i corresponds to state AFTER visiting node at route index i-1 (or depot if i=0).
     */
    public double[][] getForwardStates(List<Integer> route) {
        int size = route.size();
        double[][] states = new double[size + 1][7];
        
        // Initial state at depot
        states[0][0] = 0.0; // dist
        states[0][1] = 0.0; // time
        states[0][2] = qBattery; // battery
        states[0][3] = 0.0; // load
        states[0][4] = 0.0; // violCap
        states[0][5] = 0.0; // violTw
        states[0][6] = 0.0; // violBat
        
        int prevNodeId = 0;
        for (int i = 0; i < size; i++) {
            int nodeId = route.get(i);
            double[] prev = states[i];
            double[] curr = states[i+1];
            
            // Start with previous values
            double dist = prev[0];
            double currTime = prev[1];
            double currBat = prev[2];
            double load = prev[3];
            double violCap = prev[4];
            double violTw = prev[5];
            double violBat = prev[6];
            
            // Travel
            dist += distMatrix[prevNodeId][nodeId];
            currTime += travelTimeMatrix[prevNodeId][nodeId];
            currBat -= energyMatrix[prevNodeId][nodeId];
            
            if (currBat < -1e-6) violBat -= currBat;
            
            double ready = readyTimes[nodeId];
            if (currTime < ready) currTime = ready;
            
            double due = dueTimes[nodeId];
            if (currTime > due) violTw += currTime - due;
            
            int nodeType = nodeTypes[nodeId];
            if (nodeType == 1) { // CUSTOMER
                load += demands[nodeId];
                currTime += serviceTimes[nodeId];
                if (load > cCapacity) violCap += load - cCapacity;
            } else if (nodeType == 2) { // STATION
                double charge = qBattery - currBat;
                if (charge > 0) currTime += charge * gRefuelRate;
                currBat = qBattery;
            }
            
            curr[0] = dist;
            curr[1] = currTime;
            curr[2] = currBat;
            curr[3] = load;
            curr[4] = violCap;
            curr[5] = violTw;
            curr[6] = violBat;
            
            prevNodeId = nodeId;
        }
        return states;
    }

    /**
     * Optimized evaluation with insertion using cached forward states.
     */
    public RouteStats evaluateWithInsertion(List<Integer> route, int insertPos, int customerId, double[][] forwardStates) {
        // Start from cached state
        double[] state = forwardStates[insertPos];
        double dist = state[0];
        double currTime = state[1];
        double currBat = state[2];
        double load = state[3];
        double violCap = state[4];
        double violTw = state[5];
        double violBat = state[6];
        
        int prevNodeId = (insertPos == 0) ? 0 : route.get(insertPos - 1);
        
        // Process inserted customer
        dist += distMatrix[prevNodeId][customerId];
        currTime += travelTimeMatrix[prevNodeId][customerId];
        currBat -= energyMatrix[prevNodeId][customerId];
        
        if (currBat < -1e-6) violBat -= currBat;
        
        double ready = readyTimes[customerId];
        if (currTime < ready) currTime = ready;
        
        double due = dueTimes[customerId];
        if (currTime > due) violTw += currTime - due;
        
        // Customer logic (we know it's a customer)
        load += demands[customerId];
        currTime += serviceTimes[customerId];
        if (load > cCapacity) violCap += load - cCapacity;
        
        prevNodeId = customerId;
        
        // Process suffix
        int routeSize = route.size();
        for (int i = insertPos; i < routeSize; i++) {
            int nodeId = route.get(i);
            dist += distMatrix[prevNodeId][nodeId];
            currTime += travelTimeMatrix[prevNodeId][nodeId];
            currBat -= energyMatrix[prevNodeId][nodeId];
            
            if (currBat < -1e-6) violBat -= currBat;
            
            ready = readyTimes[nodeId];
            if (currTime < ready) currTime = ready;
            
            due = dueTimes[nodeId];
            if (currTime > due) violTw += currTime - due;
            
            int nodeType = nodeTypes[nodeId];
            if (nodeType == 1) {
                load += demands[nodeId];
                currTime += serviceTimes[nodeId];
                if (load > cCapacity) violCap += load - cCapacity;
            } else if (nodeType == 2) {
                double charge = qBattery - currBat;
                if (charge > 0) currTime += charge * gRefuelRate;
                currBat = qBattery;
            }
            prevNodeId = nodeId;
        }
        
        // Return to depot
        dist += distMatrix[prevNodeId][0];
        currTime += travelTimeMatrix[prevNodeId][0];
        currBat -= energyMatrix[prevNodeId][0];
        
        if (currBat < -1e-6) violBat -= currBat;
        if (currTime > depotDue) violTw += currTime - depotDue;
        
        double cost = dist + (Constants.PENALTY_CAPACITY * violCap) +
                     (Constants.PENALTY_TIME * violTw) + (Constants.PENALTY_BATTERY * violBat);
        
        return new RouteStats(cost, dist, violCap, violTw, violBat);
    }

    /**
     * Optimized double insertion using cached forward states.
     */
    public RouteStats evaluateWithDoubleInsertion(List<Integer> route, int insertPos, 
                                                   int firstId, int secondId, double[][] forwardStates) {
        // Start from cached state
        double[] state = forwardStates[insertPos];
        double dist = state[0];
        double currTime = state[1];
        double currBat = state[2];
        double load = state[3];
        double violCap = state[4];
        double violTw = state[5];
        double violBat = state[6];
        
        int prevNodeId = (insertPos == 0) ? 0 : route.get(insertPos - 1);
        
        // Process first inserted node
        dist += distMatrix[prevNodeId][firstId];
        currTime += travelTimeMatrix[prevNodeId][firstId];
        currBat -= energyMatrix[prevNodeId][firstId];
        
        if (currBat < -1e-6) violBat -= currBat;
        
        double ready = readyTimes[firstId];
        if (currTime < ready) currTime = ready;
        
        double due = dueTimes[firstId];
        if (currTime > due) violTw += currTime - due;
        
        int nodeType = nodeTypes[firstId];
        if (nodeType == 1) {
            load += demands[firstId];
            currTime += serviceTimes[firstId];
            if (load > cCapacity) violCap += load - cCapacity;
        } else if (nodeType == 2) {
            double charge = qBattery - currBat;
            if (charge > 0) currTime += charge * gRefuelRate;
            currBat = qBattery;
        }
        prevNodeId = firstId;
        
        // Process second inserted node
        dist += distMatrix[prevNodeId][secondId];
        currTime += travelTimeMatrix[prevNodeId][secondId];
        currBat -= energyMatrix[prevNodeId][secondId];
        
        if (currBat < -1e-6) violBat -= currBat;
        
        ready = readyTimes[secondId];
        if (currTime < ready) currTime = ready;
        
        due = dueTimes[secondId];
        if (currTime > due) violTw += currTime - due;
        
        nodeType = nodeTypes[secondId];
        if (nodeType == 1) {
            load += demands[secondId];
            currTime += serviceTimes[secondId];
            if (load > cCapacity) violCap += load - cCapacity;
        } else if (nodeType == 2) {
            double charge = qBattery - currBat;
            if (charge > 0) currTime += charge * gRefuelRate;
            currBat = qBattery;
        }
        prevNodeId = secondId;
        
        // Process suffix
        int routeSize = route.size();
        for (int i = insertPos; i < routeSize; i++) {
            int nodeId = route.get(i);
            dist += distMatrix[prevNodeId][nodeId];
            currTime += travelTimeMatrix[prevNodeId][nodeId];
            currBat -= energyMatrix[prevNodeId][nodeId];
            
            if (currBat < -1e-6) violBat -= currBat;
            
            ready = readyTimes[nodeId];
            if (currTime < ready) currTime = ready;
            
            due = dueTimes[nodeId];
            if (currTime > due) violTw += currTime - due;
            
            nodeType = nodeTypes[nodeId];
            if (nodeType == 1) {
                load += demands[nodeId];
                currTime += serviceTimes[nodeId];
                if (load > cCapacity) violCap += load - cCapacity;
            } else if (nodeType == 2) {
                double charge = qBattery - currBat;
                if (charge > 0) currTime += charge * gRefuelRate;
                currBat = qBattery;
            }
            prevNodeId = nodeId;
        }
        
        // Return to depot
        dist += distMatrix[prevNodeId][0];
        currTime += travelTimeMatrix[prevNodeId][0];
        currBat -= energyMatrix[prevNodeId][0];
        
        if (currBat < -1e-6) violBat -= currBat;
        if (currTime > depotDue) violTw += currTime - depotDue;
        
        double cost = dist + (Constants.PENALTY_CAPACITY * violCap) +
                     (Constants.PENALTY_TIME * violTw) + (Constants.PENALTY_BATTERY * violBat);
        
        return new RouteStats(cost, dist, violCap, violTw, violBat);
    }

    /**
     * Calculate route cost from pre-computed forward states.
     * This avoids redundant route traversal when forward states are already computed.
     * 
     * @param forwardStates Pre-computed forward states from getForwardStates()
     * @param route The route (used to get the last node for return-to-depot calculation)
     * @return Total route cost
     */
    public double getCostFromForwardStates(double[][] forwardStates, List<Integer> route) {
        int size = route.size();
        
        if (size == 0) {
            // Empty route
            return 0.0;
        }
        
        // Get the final state after visiting all nodes
        double[] finalState = forwardStates[size];
        double dist = finalState[0];
        double currTime = finalState[1];
        double currBat = finalState[2];
        double violCap = finalState[4];
        double violTw = finalState[5];
        double violBat = finalState[6];
        
        // Add return to depot
        int lastNodeId = route.get(size - 1);
        dist += distMatrix[lastNodeId][0];
        currTime += travelTimeMatrix[lastNodeId][0];
        currBat -= energyMatrix[lastNodeId][0];
        
        if (currBat < -1e-6) violBat -= currBat;
        if (currTime > depotDue) violTw += currTime - depotDue;
        
        // Calculate total cost
        double cost = dist + (Constants.PENALTY_CAPACITY * violCap) +
                     (Constants.PENALTY_TIME * violTw) + (Constants.PENALTY_BATTERY * violBat);
        
        return cost;
    }

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
