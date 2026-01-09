package com.tranhuy105.server.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Data;

/**
 * Complete problem instance with all nodes and precomputed matrices
 */
@Data
public class Instance {
    private Node depot;
    private final List<Node> customers = new ArrayList<>();
    private final List<Node> stations = new ArrayList<>();
    private final List<Node> allNodes = new ArrayList<>();
    
    private VehicleSpec vehicleSpec = VehicleSpec.builder().build();
    
    // Precomputed matrices
    private double[][] distanceMatrix;
    private double[][] travelTimeMatrix;
    private double[][] energyMatrix;
    
    // Nearest stations cache (top 5 per node)
    private final Map<Integer, List<Integer>> nearestStations = new HashMap<>();
    
    /**
     * Add a node to the appropriate list based on type
     */
    public void addNode(Node node) {
        switch (node.getType()) {
            case DEPOT -> setDepot(node);
            case CUSTOMER -> customers.add(node);
            case STATION -> stations.add(node);
        }
    }

    /**
     * Finalize instance by assigning IDs and precomputing matrices.
     * Must be called after all nodes are added.
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

        // Precompute distance matrix (Euclidean)
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
                travelTimeMatrix[i][j] = distanceMatrix[i][j] / vehicleSpec.getVelocity();
            }
        }

        // Precompute energy consumption matrix
        energyMatrix = new double[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                energyMatrix[i][j] = distanceMatrix[i][j] * vehicleSpec.getConsumptionRate();
            }
        }

        // Precompute nearest stations for each node (top 5)
        List<Integer> stationIds = stations.stream()
                .map(Node::getId)
                .toList();

        for (int i = 0; i < size; i++) {
            final int nodeId = i;
            List<Integer> sorted = new ArrayList<>(stationIds);
            sorted.sort(Comparator.comparingDouble(s -> distanceMatrix[nodeId][s]));
            nearestStations.put(i, sorted.subList(0, Math.min(5, sorted.size())));
        }
    }

    /**
     * Calculate Euclidean distance between two nodes
     */
    private double calculateDistance(Node n1, Node n2) {
        double dx = n1.getX() - n2.getX();
        double dy = n1.getY() - n2.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }

    public double distance(int i, int j) {
        return distanceMatrix[i][j];
    }

    public double travelTime(int i, int j) {
        return travelTimeMatrix[i][j];
    }

    public double energy(int i, int j) {
        return energyMatrix[i][j];
    }
    
    public int getNodeCount() {
        return allNodes.size();
    }
}
