package tranhuy105.evrptw;

import java.nio.file.Path;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import tranhuy105.evrptw.algorithm.ALNS;
import tranhuy105.evrptw.io.InstanceReader;
import tranhuy105.evrptw.io.SolutionPlotter;
import tranhuy105.evrptw.io.SolutionVerifier;
import tranhuy105.evrptw.io.SolutionWriter;
import tranhuy105.evrptw.model.ChargingMode;
import tranhuy105.evrptw.model.Instance;
import tranhuy105.evrptw.model.Solution;
import tranhuy105.evrptw.util.LogLevel;
import tranhuy105.evrptw.util.Logger;

/**
 * Main entry point for EVRPTW ALNS Solver
 */
public class Main {
    private static final int DEFAULT_ITERATIONS = 5000;
    private static final String DEFAULT_OUTPUT_DIR = "solutions";

    public static void main(String[] args) {
        Options options = createOptions();
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();

        try {
            CommandLine cmd = parser.parse(options, args);

            // Get positional argument (instance file)
            String[] remaining = cmd.getArgs();
            if (remaining.length < 1) {
                System.err.println("Error: Instance file path is required");
                formatter.printHelp("evrptw-solver <instance_file> [options]", options);
                System.exit(1);
            }
            String instancePath = remaining[0];

            // Parse options
            int iterations = Integer.parseInt(cmd.getOptionValue("iterations", 
                    String.valueOf(DEFAULT_ITERATIONS)));
            double timeLimit = Double.parseDouble(cmd.getOptionValue("time", "0"));
            String outputDir = cmd.getOptionValue("output-dir", DEFAULT_OUTPUT_DIR);
            boolean verify = !cmd.hasOption("no-verify");
            String verifierPath = cmd.getOptionValue("verifier");
            boolean plot = cmd.hasOption("plot");
            String logLevelStr = cmd.getOptionValue("log-level", "INFO");
            String chargingModeStr = cmd.getOptionValue("charging-mode", "FULL_RECHARGE");
            double swapTime = Double.parseDouble(cmd.getOptionValue("swap-time", "2.0"));

            // Set log level
            try {
                Logger.setLevel(LogLevel.valueOf(logLevelStr.toUpperCase()));
            } catch (IllegalArgumentException e) {
                Logger.warning("Invalid log level: " + logLevelStr + ", using INFO");
            }

            // Parse charging mode
            ChargingMode chargingMode;
            try {
                chargingMode = ChargingMode.valueOf(chargingModeStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                Logger.warning("Invalid charging mode: " + chargingModeStr + ", using FULL_RECHARGE");
                chargingMode = ChargingMode.FULL_RECHARGE;
            }

            // Run solver
            runSolver(instancePath, iterations, timeLimit, outputDir, verify, verifierPath, plot,
                     chargingMode, swapTime);

        } catch (ParseException e) {
            System.err.println("Error parsing arguments: " + e.getMessage());
            formatter.printHelp("evrptw-solver <instance_file> [options]", options);
            System.exit(1);
        } catch (Exception e) {
            Logger.error("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static Options createOptions() {
        Options options = new Options();

        options.addOption(Option.builder("i")
                .longOpt("iterations")
                .hasArg()
                .desc("Number of ALNS iterations (default: " + DEFAULT_ITERATIONS + ")")
                .build());

        options.addOption(Option.builder("t")
                .longOpt("time")
                .hasArg()
                .desc("Time limit in seconds (default: 0 = no limit)")
                .build());

        options.addOption(Option.builder("o")
                .longOpt("output-dir")
                .hasArg()
                .desc("Output directory for solutions (default: " + DEFAULT_OUTPUT_DIR + ")")
                .build());

        options.addOption(Option.builder()
                .longOpt("no-verify")
                .desc("Skip solution verification")
                .build());

        options.addOption(Option.builder()
                .longOpt("verifier")
                .hasArg()
                .desc("Path to verifier JAR file")
                .build());

        options.addOption(Option.builder("p")
                .longOpt("plot")
                .desc("Generate solution plot (PNG image)")
                .build());

        options.addOption(Option.builder()
                .longOpt("log-level")
                .hasArg()
                .desc("Logging level: DEBUG, INFO, WARNING, ERROR (default: INFO)")
                .build());

        options.addOption(Option.builder("h")
                .longOpt("help")
                .desc("Show help message")
                .build());

        options.addOption(Option.builder("cm")
                .longOpt("charging-mode")
                .hasArg()
                .desc("Charging mode: FULL_RECHARGE or BATTERY_SWAP (default: FULL_RECHARGE)")
                .build());

        options.addOption(Option.builder("st")
                .longOpt("swap-time")
                .hasArg()
                .desc("Battery swap time in minutes (default: 2.0, only used with BATTERY_SWAP mode)")
                .build());

        return options;
    }

    private static void runSolver(String instancePath, int iterations, double timeLimit,
                                   String outputDir, boolean verify, 
                                   String verifierPath, boolean plot,
                                   ChargingMode chargingMode, double swapTime) throws Exception {
        Logger.info("Reading instance: " + instancePath);
        
        InstanceReader reader = new InstanceReader();
        Instance instance = reader.read(instancePath);

        // Set charging mode
        instance.setChargingMode(chargingMode);
        instance.setBatterySwapTime(swapTime);

        Logger.info(String.format("Loaded: %d customers, %d stations",
                instance.getCustomers().size(), instance.getStations().size()));
        Logger.info(String.format("Parameters: Q=%.2f, C=%.2f, r=%.2f, g=%.2f, v=%.2f",
                instance.getBatteryCapacity(), instance.getCargoCapacity(),
                instance.getConsumptionRate(), instance.getRefuelRate(),
                instance.getVelocity()));
        Logger.info(String.format("Charging mode: %s%s", chargingMode,
                chargingMode == ChargingMode.BATTERY_SWAP ? 
                String.format(" (swap time: %.1f min)", swapTime) : ""));

        // Build stopping criteria message
        String stopCriteria;
        if (timeLimit > 0 && iterations < Integer.MAX_VALUE) {
            stopCriteria = String.format("%d iterations or %.1f seconds", iterations, timeLimit);
        } else if (timeLimit > 0) {
            stopCriteria = String.format("%.1f seconds", timeLimit);
        } else {
            stopCriteria = String.format("%d iterations", iterations);
        }
        Logger.info("Starting ALNS with " + stopCriteria + "...");
        
        long startTime = System.currentTimeMillis();

        ALNS alns = new ALNS(instance, iterations, timeLimit, true);
        Solution bestSolution = alns.solve();

        long elapsed = System.currentTimeMillis() - startTime;
        Logger.info(String.format("Optimization completed in %.2f seconds", elapsed / 1000.0));

        // Print solution
        SolutionWriter writer = new SolutionWriter();
        writer.print(bestSolution);

        // Export solution
        String solutionPath = getSolutionFilepath(instancePath, outputDir);
        writer.write(bestSolution, solutionPath);
        Logger.info("Solution exported to: " + solutionPath);

        // Generate plot if requested
        if (plot) {
            String plotPath = getPlotFilepath(instancePath, outputDir);
            SolutionPlotter plotter = new SolutionPlotter();
            plotter.plot(bestSolution, plotPath);
        }

        // Verify if requested
        if (verify) {
            SolutionVerifier verifier = verifierPath != null ? 
                    new SolutionVerifier(verifierPath) : new SolutionVerifier();
            verifier.verify(instancePath, solutionPath);
        }
    }

    private static String getPlotFilepath(String problemPath, String outputDir) {
        String problemName = Path.of(problemPath).getFileName().toString();
        int dotIndex = problemName.lastIndexOf('.');
        if (dotIndex > 0) {
            problemName = problemName.substring(0, dotIndex);
        }
        return Path.of(outputDir, problemName + "_solution.png").toString();
    }

    private static String getSolutionFilepath(String problemPath, String outputDir) {
        String problemName = Path.of(problemPath).getFileName().toString();
        int dotIndex = problemName.lastIndexOf('.');
        if (dotIndex > 0) {
            problemName = problemName.substring(0, dotIndex);
        }
        return Path.of(outputDir, problemName + "_solution.txt").toString();
    }
}
