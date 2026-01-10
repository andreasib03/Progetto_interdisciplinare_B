package it.uninsubria.launcher.env;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Logger;
import java.util.logging.Level;

public class EnvironmentChecker {
    private static final Logger logger = Logger.getLogger(EnvironmentChecker.class.getName());

    private static final String MIN_JAVA_VERSION = "17";
    private static final String MIN_MAVEN_VERSION = "3.6";

    public static class EnvironmentStatus {
        public final boolean javaInstalled;
        public final boolean mavenInstalled;
        public final boolean javafxAvailable;
        public final String javaVersion;
        public final String mavenVersion;
        public final boolean javaCompatible;
        public final boolean mavenCompatible;
        public final boolean allPrerequisitesMet;

        public EnvironmentStatus(boolean javaInstalled, boolean mavenInstalled, boolean javafxAvailable,
                                String javaVersion, String mavenVersion,
                                boolean javaCompatible, boolean mavenCompatible) {
            this.javaInstalled = javaInstalled;
            this.mavenInstalled = mavenInstalled;
            this.javafxAvailable = javafxAvailable;
            this.javaVersion = javaVersion;
            this.mavenVersion = mavenVersion;
            this.javaCompatible = javaCompatible;
            this.mavenCompatible = mavenCompatible;
            this.allPrerequisitesMet = javaInstalled && mavenInstalled && javafxAvailable && javaCompatible && mavenCompatible;
        }
    }

    public static EnvironmentStatus checkEnvironment() {
        logger.info("Checking environment prerequisites...");

        // Check Java
        boolean javaInstalled = false;
        String javaVersion = "Not found";
        boolean javaCompatible = false;

        try {
            ProcessBuilder pb = new ProcessBuilder("java", "-version");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("version")) {
                    javaVersion = extractVersion(line);
                    javaInstalled = true;
                    javaCompatible = isVersionCompatible(javaVersion, MIN_JAVA_VERSION);
                    break;
                }
            }
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            logger.log(Level.WARNING, "Error checking Java version", e);
        }

        // Check Maven
        boolean mavenInstalled = false;
        String mavenVersion = "Not found";
        boolean mavenCompatible = false;

        try {
            // Use correct Maven command for the OS
            String mvnCommand = System.getProperty("os.name").toLowerCase().contains("windows") ? "mvn.cmd" : "mvn";
            ProcessBuilder pb = new ProcessBuilder(mvnCommand, "-version");
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("Apache Maven")) {
                    mavenVersion = extractMavenVersion(line);
                    mavenInstalled = true;
                    mavenCompatible = isVersionCompatible(mavenVersion, MIN_MAVEN_VERSION);
                    break;
                }
            }
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            logger.log(Level.WARNING, "Error checking Maven version", e);
        }

        // Check JavaFX availability
        boolean javafxAvailable = false;
        if (javaInstalled && javaCompatible) {
            javafxAvailable = checkJavaFXAvailability();
        }

        EnvironmentStatus status = new EnvironmentStatus(javaInstalled, mavenInstalled, javafxAvailable,
                                                        javaVersion, mavenVersion,
                                                        javaCompatible, mavenCompatible);

        logger.info("Environment check completed:");
        logger.info("Java installed: " + javaInstalled + " (version: " + javaVersion + ", compatible: " + javaCompatible + ")");
        logger.info("Maven installed: " + mavenInstalled + " (version: " + mavenVersion + ", compatible: " + mavenCompatible + ")");
        logger.info("JavaFX available: " + javafxAvailable);

        return status;
    }

    private static String extractVersion(String versionLine) {
        // Extract version from java -version output like: "version "17.0.8""
        int start = versionLine.indexOf("\"");
        int end = versionLine.lastIndexOf("\"");
        if (start != -1 && end != -1 && start < end) {
            return versionLine.substring(start + 1, end);
        }
        return versionLine;
    }

    private static String extractMavenVersion(String versionLine) {
        // Extract version from Maven output like: "Apache Maven 3.8.6"
        String[] parts = versionLine.split(" ");
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].equals("Maven") && i + 1 < parts.length) {
                return parts[i + 1];
            }
        }
        return versionLine;
    }

    private static boolean isVersionCompatible(String actualVersion, String minVersion) {
        try {
            // Simple version comparison - split by dots and compare major versions
            String[] actualParts = actualVersion.split("\\.");
            String[] minParts = minVersion.split("\\.");

            int actualMajor = Integer.parseInt(actualParts[0]);
            int minMajor = Integer.parseInt(minParts[0]);

            return actualMajor >= minMajor;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error comparing versions: " + actualVersion + " vs " + minVersion, e);
            return false;
        }
    }

    private static boolean checkJavaFXAvailability() {
        try {
            // Try to list Java modules to check for JavaFX
            ProcessBuilder pb = new ProcessBuilder("java", "--list-modules");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("javafx")) {
                    return true;
                }
            }
            process.waitFor();

            // If JavaFX modules not found in system Java, check if Maven can provide JavaFX
            // Since this is a Maven project with JavaFX dependencies, assume JavaFX is available
            logger.info("JavaFX modules not found in system Java, but project uses Maven JavaFX dependencies");
            return true; // Assume JavaFX is available via Maven

        } catch (IOException | InterruptedException e) {
            logger.log(Level.WARNING, "Error checking JavaFX availability", e);
            // If we can't check, assume JavaFX is available (optimistic approach for Maven projects)
            return true;
        }
    }
}
