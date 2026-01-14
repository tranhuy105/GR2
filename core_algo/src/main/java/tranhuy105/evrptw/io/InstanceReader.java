package tranhuy105.evrptw.io;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import tranhuy105.evrptw.model.Instance;
import tranhuy105.evrptw.model.Node;
import tranhuy105.evrptw.model.NodeType;
import tranhuy105.evrptw.util.Logger;

/**
 * Reads EVRPTW instances in Schneider format
 */
public class InstanceReader {

    /**
     * Read instance from file
     *
     * @param filepath Path to instance file
     * @return Parsed and finalized Instance
     * @throws IOException if file cannot be read or is malformed
     */
    public Instance read(String filepath) throws IOException {
        Path path = Path.of(filepath);
        
        if (!Files.exists(path)) {
            throw new IOException("File not found: " + filepath);
        }

        Instance instance = new Instance();

        try (BufferedReader reader = new BufferedReader(new FileReader(filepath))) {
            String line;
            boolean inParameterSection = false;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Skip empty lines
                if (line.isEmpty()) {
                    continue;
                }

                String[] parts = line.split("\\s+");

                // Check if we've reached parameter section
                if (parts.length > 0 && isParameterLine(parts[0])) {
                    inParameterSection = true;
                }

                if (inParameterSection) {
                    // Parse parameter line
                    parseParameter(line, instance);
                } else {
                    // Parse node line
                    if (parts.length >= 8 && !parts[0].equals("StringID")) {
                        try {
                            Node node = parseNode(parts);
                            
                            switch (node.getType()) {
                                case DEPOT -> instance.setDepot(node);
                                case CUSTOMER -> instance.getCustomers().add(node);
                                case STATION -> instance.getStations().add(node);
                            }
                        } catch (Exception e) {
                            // Skip malformed node lines
                            Logger.debug("Skipping malformed node line: " + line);
                        }
                    }
                }
            }
        }

        // Validate instance
        if (instance.getDepot() == null) {
            throw new IOException("No depot found in instance file");
        }
        if (instance.getCustomers().isEmpty()) {
            throw new IOException("No customers found in instance file");
        }

        // Finalize instance (compute matrices)
        instance.finalizeInstance();

        return instance;
    }

    /**
     * Check if line starts a parameter section
     */
    private boolean isParameterLine(String firstToken) {
        return firstToken.equals("Q") || firstToken.equals("C") || 
               firstToken.equals("r") || firstToken.equals("g") || 
               firstToken.equals("v");
    }

    /**
     * Parse a node line
     * Format: StringID Type X Y Demand ReadyTime DueTime ServiceTime
     */
    private Node parseNode(String[] parts) {
        String stringId = parts[0];
        NodeType type = NodeType.fromChar(parts[1].charAt(0));
        double x = Double.parseDouble(parts[2]);
        double y = Double.parseDouble(parts[3]);
        double demand = Double.parseDouble(parts[4]);
        double readyTime = Double.parseDouble(parts[5]);
        double dueTime = Double.parseDouble(parts[6]);
        double serviceTime = Double.parseDouble(parts[7]);

        return new Node(stringId, type, x, y, demand, readyTime, dueTime, serviceTime);
    }

    /**
     * Parse a parameter line
     * Format: Q Vehicle fuel tank capacity /77.75/
     */
    private void parseParameter(String line, Instance instance) {
        if (!line.contains("/")) {
            return;
        }

        // Extract parameter name (first character) and value (between slashes)
        String[] parts = line.split("\\s+");
        if (parts.length == 0) {
            return;
        }

        String paramName = parts[0].trim();
        
        // Extract value between slashes
        int firstSlash = line.indexOf('/');
        int lastSlash = line.lastIndexOf('/');
        
        if (firstSlash == -1 || lastSlash == -1 || firstSlash == lastSlash) {
            return;
        }

        try {
            String valueStr = line.substring(firstSlash + 1, lastSlash).trim();
            double value = Double.parseDouble(valueStr);

            switch (paramName) {
                case "Q" -> instance.setBatteryCapacity(value);
                case "C" -> instance.setCargoCapacity(value);
                case "r" -> instance.setConsumptionRate(value);
                case "g" -> instance.setRefuelRate(value);
                case "v" -> instance.setVelocity(value);
            }
        } catch (NumberFormatException e) {
            Logger.warning("Failed to parse parameter: " + line);
        }
    }
}
