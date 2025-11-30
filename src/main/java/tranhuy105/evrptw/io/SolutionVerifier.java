package tranhuy105.evrptw.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import tranhuy105.evrptw.util.Logger;

/**
 * Verifies solutions using the EVRPTW verifier JAR
 */
public class SolutionVerifier {
    private static final String DEFAULT_VERIFIER_PATH = 
            "src/main/resources/verifier/evrptw-verifier-0.2.0.jar.verify";

    private final String verifierPath;

    public SolutionVerifier() {
        this(DEFAULT_VERIFIER_PATH);
    }

    public SolutionVerifier(String verifierPath) {
        this.verifierPath = verifierPath != null ? verifierPath : DEFAULT_VERIFIER_PATH;
    }

    /**
     * Verify solution using the EVRPTW verifier JAR
     *
     * @param problemPath Path to the problem instance file
     * @param solutionPath Path to the solution file
     * @return true if verification passed, false otherwise
     */
    public boolean verify(String problemPath, String solutionPath) {
        Path verifierFile = Path.of(verifierPath);
        
        if (!Files.exists(verifierFile)) {
            Logger.warning("Verifier not found at: " + verifierPath);
            Logger.warning("Skipping verification.");
            return false;
        }

        // Copy JAR to temp location (handles .jar.verify extension)
        Path tempJar = null;
        try {
            tempJar = Files.createTempFile("evrptw_verifier", ".jar");
            Files.copy(verifierFile, tempJar, StandardCopyOption.REPLACE_EXISTING);

            ProcessBuilder pb = new ProcessBuilder(
                    "java",
                    "-jar",
                    tempJar.toString(),
                    "-d",
                    problemPath,
                    solutionPath
            );
            pb.redirectErrorStream(true);

            Logger.info("Running verifier...");
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();

            if (!output.isEmpty()) {
                System.out.println();
                System.out.println("=".repeat(70));
                System.out.println("VERIFIER OUTPUT");
                System.out.println("=".repeat(70));
                System.out.println(output);
            }

            // Check if verification passed
            String outputLower = output.toString().toLowerCase();
            if (outputLower.contains("feasible") || outputLower.contains("valid")) {
                return true;
            }
            if (outputLower.contains("infeasible") || outputLower.contains("invalid")) {
                return false;
            }

            return exitCode == 0;

        } catch (IOException e) {
            if (e.getMessage().contains("java")) {
                Logger.error("Java not found. Please install Java to use the verifier.");
            } else {
                Logger.error("Verification failed: " + e.getMessage());
            }
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Logger.error("Verification interrupted");
            return false;
        } finally {
            // Cleanup temp file
            if (tempJar != null) {
                try {
                    Files.deleteIfExists(tempJar);
                } catch (IOException ignored) {
                }
            }
        }
    }
}
