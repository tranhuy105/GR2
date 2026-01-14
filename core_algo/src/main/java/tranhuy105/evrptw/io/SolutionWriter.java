package tranhuy105.evrptw.io;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import tranhuy105.evrptw.algorithm.RouteEvaluator;
import tranhuy105.evrptw.model.Instance;
import tranhuy105.evrptw.model.Node;
import tranhuy105.evrptw.model.NodeType;
import tranhuy105.evrptw.model.RouteStats;
import tranhuy105.evrptw.model.Solution;

/**
 * Writes solutions to files and console
 */
public class SolutionWriter {

    /**
     * Write solution to file for verification
     * Format: Line 1 = total distance, subsequent lines = routes with string IDs
     */
    public void write(Solution solution, String filepath) throws IOException {
        // Ensure output directory exists
        Path path = Path.of(filepath);
        Path parent = path.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filepath))) {
            // First line: total distance
            writer.write(String.format("%.6f%n", solution.getTotalDistance()));

            // Routes using StringID
            String depotId = solution.getInstance().getDepot().getStringId();
            for (List<Integer> route : solution.getRoutes()) {
                StringBuilder sb = new StringBuilder();
                sb.append(depotId);
                
                for (int nodeId : route) {
                    Node node = solution.getInstance().getAllNodes().get(nodeId);
                    sb.append(" ").append(node.getStringId());
                }
                
                sb.append(" ").append(depotId);
                writer.write(sb.toString());
                writer.newLine();
            }
        }
    }

    /**
     * Print solution details to console
     */
    public void print(Solution solution) {
        RouteEvaluator evaluator = new RouteEvaluator(solution.getInstance());
        evaluator.calculateTotalCost(solution);

        System.out.println();
        System.out.println("=".repeat(70));
        System.out.println("FINAL SOLUTION - EVRPTW (Enhanced ALNS)");
        System.out.println("=".repeat(70));

        System.out.printf("Total Cost:      %.2f%n", solution.getCost());
        System.out.printf("Total Distance:  %.2f%n", solution.getTotalDistance());
        System.out.printf("Violations:      %.6f%n", solution.getTotalViolations());
        System.out.printf("Vehicles Used:   %d%n", solution.getRoutes().size());
        System.out.printf("Feasible:        %s%n", solution.isFeasible());
        System.out.println("-".repeat(70));

        Instance inst = solution.getInstance();
        int vehicleNum = 1;
        
        for (List<Integer> route : solution.getRoutes()) {
            StringBuilder pathParts = new StringBuilder();
            pathParts.append("[D]").append(inst.getDepot().getStringId());

            for (int nodeId : route) {
                Node node = inst.getAllNodes().get(nodeId);
                if (node.getType() == NodeType.STATION) {
                    pathParts.append(" -> [S]").append(node.getStringId());
                } else {
                    pathParts.append(" -> [C]").append(node.getStringId());
                }
            }

            pathParts.append(" -> [D]").append(inst.getDepot().getStringId());

            // Calculate route stats
            RouteStats stats = evaluator.evaluate(route);

            System.out.printf("%nVehicle %d (dist=%.2f):%n", vehicleNum++, stats.distance());
            System.out.println("  " + pathParts);
        }

        System.out.println("=".repeat(70));
    }
}
