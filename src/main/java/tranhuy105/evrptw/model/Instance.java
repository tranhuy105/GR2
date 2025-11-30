package tranhuy105.evrptw.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents an EVRPTW problem instance with all nodes, parameters, and precomputed data
 */
public class Instance {
    private Node depot;
    private final List<Node> customers = new ArrayList<>();
    private final List<Node> stations = new ArrayList<>();
    private final List<Node> allNodes = new ArrayList<>();

    // Vehicle parameters
    private double batteryCapacity = 100.0;
    private double cargoCapacity = 200.0;
    private double consumptionRate = 1.0;
    private double refuelRate = 1.0;
    private double velocity = 1.0;

    // Precomputed matrices
    private double[][] distanceMatrix;
    private double[][] travelTimeMatrix;
    private double[][] energyMatrix;

    // Precomputed nearest stations (top 5 per node)
    private final Map<Integer, List<Integer>> nearestStations = new HashMap<>();
    private double maxReachableDistance;

    public Node getDepot() {
        return depot;
    }

    public void setDepot(Node depot) {
        this.depot = depot;
    }

    public List<Node> getCustomers() {
        return customers;
    }

    public List<Node> getStations() {
        return stations;
    }

    public List<Node> getAllNodes() {
        return allNodes;
    }

    public double getBatteryCapacity() {
        return batteryCapacity;
    }

    public void setBatteryCapacity(double batteryCapacity) {
        this.batteryCapacity = batteryCapacity;
    }

    public double getCargoCapacity() {
        return cargoCapacity;
    }

    public void setCargoCapacity(double cargoCapacity) {
        this.cargoCapacity = cargoCapacity;
    }

    public double getConsumptionRate() {
        return consumptionRate;
    }

    public void setConsumptionRate(double consumptionRate) {
        this.consumptionRate = consumptionRate;
    }

    public double getRefuelRate() {
        return refuelRate;
    }

    public void setRefuelRate(double refuelRate) {
        this.refuelRate = refuelRate;
    }

    public double getVelocity() {
        return velocity;
    }

    public void setVelocity(double velocity) {
        this.velocity = velocity;
    }

    public double[][] getDistanceMatrix() {
        return distanceMatrix;
    }

    public double[][] getTravelTimeMatrix() {
        return travelTimeMatrix;
    }

    public double[][] getEnergyMatrix() {
        return energyMatrix;
    }

    public Map<Integer, List<Integer>> getNearestStations() {
        return nearestStations;
    }

    public double getMaxReachableDistance() {
        return maxReachableDistance;
    }

    /**
     * Calculate Euclidean distance between two nodes
     */
    private double calculateDistance(Node n1, Node n2) {
        double dx = n1.getX() - n2.getX();
        double dy = n1.getY() - n2.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Finalize instance by assigning IDs and precomputing matrices
     */
    public void finalizeInstance() {
        // Assign IDs: depot=0, customers=1..n, stations=n+1..n+m
        depot.setId(0);
        allNodes.clear();
        allNodes.add(depot);

        int id = 1;
        for (Node customer : customers) {
            customer.setId(id++);
            allNodes.add(customer);
        }

        for (Node station : stations) {
            station.setId(id++);
            allNodes.add(station);
        }

        int size = allNodes.size();

        // Precompute distance matrix
        distanceMatrix = new double[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (i != j) {
                    distanceMatrix[i][j] = calculateDistance(allNodes.get(i), allNodes.get(j));
                }
            }
        }

        // Precompute travel time matrix
        travelTimeMatrix = new double[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                travelTimeMatrix[i][j] = distanceMatrix[i][j] / velocity;
            }
        }

        // Precompute energy consumption matrix
        energyMatrix = new double[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                energyMatrix[i][j] = distanceMatrix[i][j] * consumptionRate;
            }
        }

        // Max reachable distance on full battery
        maxReachableDistance = batteryCapacity / consumptionRate;

        // Precompute nearest stations for each node (top 5)
        List<Integer> stationIds = new ArrayList<>();
        for (Node station : stations) {
            stationIds.add(station.getId());
        }

        for (int i = 0; i < size; i++) {
            final int nodeId = i;
            List<Integer> sorted = new ArrayList<>(stationIds);
            sorted.sort(Comparator.comparingDouble(s -> distanceMatrix[nodeId][s]));
            nearestStations.put(i, sorted.subList(0, Math.min(5, sorted.size())));
        }
    }

    /**
     * Get distance between two nodes by ID
     */
    public double distance(int i, int j) {
        return distanceMatrix[i][j];
    }

    /**
     * Get travel time between two nodes by ID
     */
    public double travelTime(int i, int j) {
        return travelTimeMatrix[i][j];
    }

    /**
     * Get energy consumption between two nodes by ID
     */
    public double energy(int i, int j) {
        return energyMatrix[i][j];
    }
}
