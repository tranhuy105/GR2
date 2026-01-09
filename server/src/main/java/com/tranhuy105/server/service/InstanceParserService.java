package com.tranhuy105.server.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.springframework.stereotype.Service;

import com.tranhuy105.server.domain.Instance;
import com.tranhuy105.server.domain.Node;
import com.tranhuy105.server.domain.VehicleSpec;
import com.tranhuy105.server.exception.InstanceParseException;

import lombok.extern.slf4j.Slf4j;

/**
 * Parses EVRPTW instances in Schneider format from file content
 */
@Service
@Slf4j
public class InstanceParserService {

    /**
     * Parse instance from input stream (for file upload)
     */
    public Instance parse(InputStream inputStream) {
        Instance instance = new Instance();
        VehicleSpec.VehicleSpecBuilder vehicleBuilder = VehicleSpec.builder();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            boolean inParameterSection = false;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty()) {
                    continue;
                }

                String[] parts = line.split("\\s+");

                // Check if we've reached parameter section
                if (parts.length > 0 && isParameterLine(parts[0])) {
                    inParameterSection = true;
                }

                if (inParameterSection) {
                    parseParameter(line, vehicleBuilder);
                } else {
                    // Parse node line
                    if (parts.length >= 8 && !parts[0].equals("StringID")) {
                        try {
                            Node node = parseNode(parts);
                            instance.addNode(node);
                        } catch (Exception e) {
                            log.debug("Skipping malformed node line: {}", line);
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new InstanceParseException("Failed to read instance file", e);
        }

        // Set vehicle spec
        instance.setVehicleSpec(vehicleBuilder.build());

        // Validate instance
        if (instance.getDepot() == null) {
            throw new InstanceParseException("No depot found in instance file");
        }
        if (instance.getCustomers().isEmpty()) {
            throw new InstanceParseException("No customers found in instance file");
        }

        // Finalize instance (compute matrices)
        instance.finalizeInstance();

        log.info("Parsed instance: {} customers, {} stations",
                instance.getCustomers().size(), instance.getStations().size());

        return instance;
    }

    /**
     * Parse instance from string content
     */
    public Instance parseFromString(String content) {
        return parse(new java.io.ByteArrayInputStream(content.getBytes()));
    }

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
        char typeChar = parts[1].charAt(0);
        double x = Double.parseDouble(parts[2]);
        double y = Double.parseDouble(parts[3]);
        double demand = Double.parseDouble(parts[4]);
        double readyTime = Double.parseDouble(parts[5]);
        double dueTime = Double.parseDouble(parts[6]);
        double serviceTime = Double.parseDouble(parts[7]);

        return Node.fromFileFormat(stringId, typeChar, x, y, demand, readyTime, dueTime, serviceTime);
    }

    /**
     * Parse a parameter line
     * Format: Q Vehicle fuel tank capacity /77.75/
     */
    private void parseParameter(String line, VehicleSpec.VehicleSpecBuilder builder) {
        if (!line.contains("/")) {
            return;
        }

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
                case "Q" -> builder.batteryCapacity(value);
                case "C" -> builder.cargoCapacity(value);
                case "r" -> builder.consumptionRate(value);
                case "g" -> builder.refuelRate(value);
                case "v" -> builder.velocity(value);
            }
        } catch (NumberFormatException e) {
            log.warn("Failed to parse parameter: {}", line);
        }
    }
}
