package it.uninsubria.launcher;

import it.uninsubria.launcher.env.EnvironmentChecker;
import it.uninsubria.launcher.utils.OSDetector;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Main application class for the Book Recommender System Launcher.
 * Provides a GUI interface to check system prerequisites, start server,
 * and launch client applications.
 *
 * <p>This launcher performs the following functions:
 * <ul>
 * <li>System environment checking (Java, Maven, PostgreSQL)</li>
 * <li>Server process management (start/stop monitoring)</li>
 * <li>Client application launching</li>
 * <li>Language switching support</li>
 * <li>Process monitoring and cleanup</li>
 * </ul>
 *
 * <p>The launcher ensures proper startup sequence and provides user feedback
 * for all operations. It supports multiple client instances and monitors
 * server availability.
 *
 * @author Book Recommender Team
 * @version 1.0
 * @since 1.0
 */
public class LauncherApp extends Application {
    private static final Logger logger = Logger.getLogger(LauncherApp.class.getName());

    @FXML private Button checkEnvButton;
    @FXML private Button runServerButton;
    @FXML private Button runClientButton;
    @FXML private ComboBox<String> languageComboBox;
    @FXML private TextArea statusArea;
    @FXML private ProgressBar progressBar;
    @FXML private ProgressIndicator serverProgressIndicator;
    @FXML private ProgressIndicator clientProgressIndicator;
    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private Label languageLabel;
    @FXML private Label statusLabel;
    @FXML private Label footerLabel;

    private EnvironmentChecker.EnvironmentStatus currentEnvStatus;

    private Process serverProcess = null;
    private boolean serverRunning = false;
    private boolean serverConnected = false; // Tracks if server is fully connected to DB and RMI
    private boolean clientStarting = false; // Tracks if a client is currently being started
    private java.util.concurrent.ScheduledFuture<?> clientButtonReenableTask; // Task to re-enable client button after timeout
    private java.util.List<Process> clientProcesses = new java.util.ArrayList<>();
    private java.util.concurrent.ScheduledExecutorService processMonitor;

    private Stage primaryStage; // Reference to the main stage

    /**
     * Starts the JavaFX application and initializes the launcher interface.
     * Sets up the main stage, loads FXML, and initializes monitoring services.
     *
     * @param primaryStage the primary stage for this application
     */
    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage; // Save stage reference

        try {
            // Initialize process monitor
            processMonitor = java.util.concurrent.Executors.newScheduledThreadPool(2);

            // Schedule periodic cleanup of dead client processes (reduced frequency for better performance)
            processMonitor.scheduleAtFixedRate(this::cleanupDeadProcesses, 60, 60, java.util.concurrent.TimeUnit.SECONDS);

            // Ensure language is set correctly before creating UI
            String initialLanguage = loadCurrentLanguage();
            LauncherLanguageManager.setLanguage(initialLanguage);

            // Load the UI from FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/launcher-view.fxml"), LauncherLanguageManager.getBundle());
            loader.setController(this);
            Parent root = loader.load();

            // Create scene
            Scene scene = new Scene(root, 600, 400);

            // Configure stage
            primaryStage.setTitle(LauncherLanguageManager.getString("launcher.title"));
            primaryStage.setScene(scene);
            primaryStage.setResizable(false);

            // Handle application shutdown - ensure all child processes are terminated
            primaryStage.setOnCloseRequest(event -> {
                try {
                    handleApplicationShutdown();
                } catch (Exception e) {
                    logger.log(java.util.logging.Level.SEVERE, "Error during application shutdown", e);
                }
            });

            primaryStage.show();

            logger.info("Launcher GUI started successfully");

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to start launcher GUI", e);
            throw new RuntimeException("Failed to start launcher", e);
        }
    }

    @FXML
    private void initialize() {
        // Initialize UI components after FXML loading - keep this minimal and fast
        languageComboBox.getItems().addAll("it", "en");
        languageComboBox.setValue(LauncherLanguageManager.getLanguage());
        languageComboBox.setOnAction(e -> onLanguageChanged());

        // Set up button event handlers
        checkEnvButton.setOnAction(e -> onCheckEnvironment());
        runServerButton.setOnAction(e -> onRunServer());
        runClientButton.setOnAction(e -> onRunClient());

        // Posticipa operazioni pesanti dopo che la finestra è mostrata
        Platform.runLater(this::finishInitialization);
    }

    /**
     * Completa l'inizializzazione dopo che la finestra è visibile.
     * Questo evita di bloccare il rendering iniziale della UI.
     *
     * Ottimizzazioni implementate:
     * - refreshUITexts() posticipato per non rallentare l'avvio
     * - initializeStatusArea() usa StringBuilder invece di multiple appendText()
     * - Riduzione di Platform.runLater calls durante inizializzazione
     */
    private void finishInitialization() {
        // Aggiorna testi UI (operazione relativamente pesante)
        refreshUITexts();

        // Inizializza status area in modo efficiente
        initializeStatusArea();
    }

    /**
     * Inizializza l'area di stato in modo efficiente costruendo tutto in una volta
     */
    private void initializeStatusArea() {
        StringBuilder statusText = new StringBuilder();

        statusText.append(LauncherLanguageManager.getString("launcher.status.welcome")).append("\n");
        statusText.append(LauncherLanguageManager.getString("launcher.status.os",
                          OSDetector.getOSName(), OSDetector.getOSArch())).append("\n");
        statusText.append(LauncherLanguageManager.getString("launcher.status.purpose")).append("\n");
        statusText.append(LauncherLanguageManager.getString("launcher.status.server.note")).append("\n");
        statusText.append(LauncherLanguageManager.getString("launcher.status.ready")).append("\n");

        statusArea.setText(statusText.toString());
    }



    /**
     * Handles the check environment button click.
     * Performs system prerequisite checking for Java, Maven, and PostgreSQL.
     * Updates UI with results and enables/disables buttons based on check outcome.
     */
    private void onCheckEnvironment() {
        // Se il check è già stato fatto con successo, non permettere di rifarlo
        if (currentEnvStatus != null && currentEnvStatus.allPrerequisitesMet) {
            return; // Non fare niente se già controllato con successo
        }

        checkEnvButton.setDisable(true);
        progressBar.setVisible(true);
        progressBar.setProgress(-1); // Indeterminate progress
        appendToStatus(LauncherLanguageManager.getString("launcher.status.checking.prerequisites"));

        // Run environment check in background thread
        CompletableFuture.supplyAsync(EnvironmentChecker::checkEnvironment)
            .thenAccept(status -> {
                javafx.application.Platform.runLater(() -> {
                    currentEnvStatus = status;
                    displayEnvironmentStatus(status);
                    updateButtonsState(status.allPrerequisitesMet);
                    // Il pulsante rimane disabilitato se il check ha avuto successo
                    if (!status.allPrerequisitesMet) {
                        checkEnvButton.setDisable(false); // Riabilita solo se fallito
                    }
                    progressBar.setVisible(false);
                    progressBar.setProgress(0); // Reset progress
                });
            })
            .exceptionally(throwable -> {
                javafx.application.Platform.runLater(() -> {
                    appendToStatus(LauncherLanguageManager.getString("launcher.status.check.failed", throwable.getMessage()));
                    checkEnvButton.setDisable(false); // Riabilita in caso di errore
                    progressBar.setVisible(false);
                    progressBar.setProgress(0); // Reset progress
                });
                return null;
            });
    }

    /**
     * Handles the run server button click.
     * Starts the Book Recommender server process and monitors its startup.
     * Updates UI to reflect server running state and enables client launching.
     */
    private void onRunServer() {
        if (currentEnvStatus == null || !currentEnvStatus.allPrerequisitesMet) {
            showAlert(LauncherLanguageManager.getString("launcher.alert.prerequisites.title"),
                       LauncherLanguageManager.getString("launcher.alert.prerequisites.message"));
            return;
        }

        if (serverRunning) {
            showAlert(LauncherLanguageManager.getString("launcher.alert.server.running.title"),
                       LauncherLanguageManager.getString("launcher.alert.server.running.message"));
            return;
        }

        appendToStatus(LauncherLanguageManager.getString("launcher.status.server.starting"));
        serverRunning = true;
        serverConnected = false; // Server not yet connected
        updateButtonsState(currentEnvStatus != null && currentEnvStatus.allPrerequisitesMet);

        // Show progress indicator
        Platform.runLater(() -> {
            serverProgressIndicator.setVisible(true);
            serverProgressIndicator.setProgress(-1); // Indeterminate progress
        });

        // Run server startup in background senza aspettare che finisca
        CompletableFuture.runAsync(this::startServerProcessAsync)
            .thenRun(() -> {
                javafx.application.Platform.runLater(() -> {
                    serverProgressIndicator.setVisible(false);
                    serverProgressIndicator.setProgress(0); // Reset progress
                    serverConnected = true; // Server is now connected to DB and RMI
                    appendToStatus(LauncherLanguageManager.getString("launcher.status.server.started"));
                    appendToStatus(LauncherLanguageManager.getString("launcher.status.clients.available"));
                    updateButtonsState(currentEnvStatus != null && currentEnvStatus.allPrerequisitesMet);
                });
            })
            .exceptionally(throwable -> {
                javafx.application.Platform.runLater(() -> {
                    serverProgressIndicator.setVisible(false);
                    serverProgressIndicator.setProgress(0); // Reset progress
                    appendToStatus(LauncherLanguageManager.getString("launcher.status.server.failed", throwable.getMessage()));
                    serverRunning = false; // Reset se fallisce
                    serverConnected = false; // Server failed to connect - ensure client button is disabled
                    updateButtonsState(currentEnvStatus != null && currentEnvStatus.allPrerequisitesMet);
                });
                return null;
            });
    }

    /**
     * Handles the run client button click.
     * Launches a new client application instance.
     * Manages client process lifecycle and UI state updates.
     */
    private void onRunClient() {
        if (currentEnvStatus == null || !currentEnvStatus.allPrerequisitesMet) {
            showAlert(LauncherLanguageManager.getString("launcher.alert.prerequisites.title"),
                         LauncherLanguageManager.getString("launcher.alert.prerequisites.message"));
            return;
        }

        if (!serverConnected) {
            showAlert(LauncherLanguageManager.getString("launcher.alert.server.not.connected.title"),
                         LauncherLanguageManager.getString("launcher.alert.server.not.connected.message"));
            return;
        }

        // Check if a client is already being started or button is disabled
        if (clientStarting || runClientButton.isDisabled()) {
            return; // Ignore the click if a client is already starting or button is disabled
        }

        // Check if we have too many client processes running (safety limit)
        synchronized (clientProcesses) {
            // Clean up any dead processes first
            clientProcesses.removeIf(process -> !process.isAlive());

            if (clientProcesses.size() >= 5) {
                showAlert(LauncherLanguageManager.getString("launcher.alert.too.many.clients.title"),
                            LauncherLanguageManager.getString("launcher.alert.too.many.clients.message"));
                return;
            }
        }

        // Mark that a client is starting and immediately disable the button
        clientStarting = true;
        Platform.runLater(() -> {
            runClientButton.setDisable(true);
            logger.fine("Client button disabled immediately on click");
        });

        // Schedule button re-enabling after a safe timeout (15 seconds) to ensure client is fully loaded
        if (clientButtonReenableTask != null && !clientButtonReenableTask.isDone()) {
            clientButtonReenableTask.cancel(false);
        }
        clientButtonReenableTask = processMonitor.schedule(() -> {
            Platform.runLater(() -> {
                clientStarting = false;
                updateButtonsState(currentEnvStatus != null && currentEnvStatus.allPrerequisitesMet);
                logger.fine("Client button re-enabled after timeout");
            });
        }, 15, java.util.concurrent.TimeUnit.SECONDS);

        // Show progress indicator
        Platform.runLater(() -> {
            clientProgressIndicator.setVisible(true);
            clientProgressIndicator.setProgress(-1); // Indeterminate progress
            logger.fine("Client progress indicator set to VISIBLE");
        });

        appendToStatus(LauncherLanguageManager.getString("launcher.status.client.starting", (clientProcesses.size() + 1)));

        // Run client startup in background - permette di avviare più client contemporaneamente
        CompletableFuture.runAsync(this::startClientProcess)
            .thenRun(() -> {
                javafx.application.Platform.runLater(() -> {
                    clientProgressIndicator.setVisible(false);
                    clientProgressIndicator.setProgress(0); // Reset progress
                    logger.fine("Client progress indicator set to HIDDEN (success)");
                    appendToStatus(LauncherLanguageManager.getString("launcher.status.client.started"));

                    // Re-enable button immediately since client process has started successfully
                    // Cancel the timeout task since we don't need it anymore
                    if (clientButtonReenableTask != null && !clientButtonReenableTask.isDone()) {
                        clientButtonReenableTask.cancel(false);
                    }
                    clientStarting = false;
                    updateButtonsState(currentEnvStatus != null && currentEnvStatus.allPrerequisitesMet);
                });
            })
            .exceptionally(throwable -> {
                javafx.application.Platform.runLater(() -> {
                    clientProgressIndicator.setVisible(false);
                    clientProgressIndicator.setProgress(0); // Reset progress
                    logger.fine("Client progress indicator set to HIDDEN (error)");
                    appendToStatus(LauncherLanguageManager.getString("launcher.status.client.failed", throwable.getMessage()));

                    // On failure, re-enable button immediately since client startup failed
                    clientStarting = false;
                    if (clientButtonReenableTask != null) {
                        clientButtonReenableTask.cancel(false);
                    }
                    updateButtonsState(currentEnvStatus != null && currentEnvStatus.allPrerequisitesMet);
                });
                return null;
            });
    }



    private void onLanguageChanged() {
        String selectedLang = languageComboBox.getValue();
        String langName = selectedLang.equals("it") ? "Italian" : "English";

        // 1. Save to config file (persistent storage)
        saveLanguageToConfig(selectedLang);

        // 2. Update launcher language manager
        LauncherLanguageManager.setLanguage(selectedLang);

        // 3. Update UI immediately with new language
        Platform.runLater(() -> {
            try {
                // Refresh all UI texts with new language
                refreshUITexts();

                // Update window title
                if (primaryStage != null) {
                    primaryStage.setTitle(LauncherLanguageManager.getString("launcher.title"));
                }

                // Show confirmation messages in new language
                appendToStatus(LauncherLanguageManager.getString("launcher.status.language.changed", langName, selectedLang));
                appendToStatus(LauncherLanguageManager.getString("launcher.status.language.saved", langName));
                appendToStatus(LauncherLanguageManager.getString("launcher.status.language.note.dynamic", langName));

                logger.info("Launcher UI refreshed with new language: " + selectedLang);

            } catch (Exception e) {
                logger.log(Level.WARNING, "Error refreshing launcher UI after language change", e);
                // Fallback: try to refresh texts at least
                try {
                    refreshUITexts();
                } catch (Exception ex) {
                    logger.log(Level.SEVERE, "Critical error: Could not refresh launcher UI texts", ex);
                }
            }
        });
    }

    private void saveLanguageToConfig(String language) {
        try {
            // Get the project root directory (same as used for running server/client)
            java.io.File projectRoot = getProjectRootDirectory();

            // Save config.properties in project root so all modules can access it
            java.io.File configFile = new java.io.File(projectRoot, "config.properties");
            java.util.Properties props = new java.util.Properties();

            // Load existing properties
            if (configFile.exists()) {
                try (java.io.FileInputStream in = new java.io.FileInputStream(configFile)) {
                    props.load(in);
                }
            }

            // Update language
            props.setProperty("language", language);

            // Save back to file
            try (java.io.FileOutputStream out = new java.io.FileOutputStream(configFile)) {
                props.store(out, "BookRecommender Application Configuration");
            }

            appendToStatus(LauncherLanguageManager.getString("launcher.status.config.saved"));
            appendToStatus(LauncherLanguageManager.getString("launcher.status.language.note.dynamic", language));

        } catch (Exception e) {
            appendToStatus(LauncherLanguageManager.getString("launcher.status.language.save.failed", e.getMessage()));
            logger.log(Level.WARNING, "Failed to save language config", e);
        }
    }

    private String loadCurrentLanguage() {
        try {
            java.io.File configFile = new java.io.File("config.properties");
            if (configFile.exists()) {
                java.util.Properties props = new java.util.Properties();
                try (java.io.FileInputStream in = new java.io.FileInputStream(configFile)) {
                    props.load(in);
                    String lang = props.getProperty("language");
                    if (lang != null && !lang.trim().isEmpty()) {
                        return lang;
                    }
                }
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to load language from config", e);
        }
        return "it"; // default
    }

    private void displayEnvironmentStatus(EnvironmentChecker.EnvironmentStatus status) {
        appendToStatus(LauncherLanguageManager.getString("launcher.env.results.title"));

        // Java status
        appendToStatus(LauncherLanguageManager.getString("launcher.env.java"));
        appendToStatus(LauncherLanguageManager.getString("launcher.env.java.installed",
                        status.javaInstalled ? "YES" : "NO"));
        appendToStatus(LauncherLanguageManager.getString("launcher.env.java.version", status.javaVersion));
        appendToStatus(LauncherLanguageManager.getString("launcher.env.java.compatible",
                        status.javaCompatible ? "YES" : "NO"));

        // Maven status
        appendToStatus(LauncherLanguageManager.getString("launcher.env.maven"));
        appendToStatus(LauncherLanguageManager.getString("launcher.env.maven.installed",
                        status.mavenInstalled ? "YES" : "NO"));
        appendToStatus(LauncherLanguageManager.getString("launcher.env.maven.version", status.mavenVersion));
        appendToStatus(LauncherLanguageManager.getString("launcher.env.maven.compatible",
                        status.mavenCompatible ? "YES" : "NO"));

        // JavaFX status
        appendToStatus(LauncherLanguageManager.getString("launcher.env.javafx"));
        appendToStatus(LauncherLanguageManager.getString("launcher.env.javafx.available",
                        status.javafxAvailable ? "YES" : "NO"));

        // Overall status
        String overallStatus = status.allPrerequisitesMet ?
            LauncherLanguageManager.getString("launcher.env.status.ready") :
            LauncherLanguageManager.getString("launcher.env.status.not.ready");
        appendToStatus(LauncherLanguageManager.getString("launcher.env.overall.status", overallStatus));
        appendToStatus("");
    }

    private void updateButtonsState(boolean prerequisitesMet) {
        // Check Environment button: disabled when prerequisites are met (not repeatable)
        checkEnvButton.setDisable(prerequisitesMet);

        // Start Server button: disabled if prerequisites not met OR server already running
        runServerButton.setDisable(!prerequisitesMet || serverRunning);

        // Open Client button: disabled if prerequisites not met OR server not fully connected OR client already starting
        // The client button should only be enabled when:
        // - Prerequisites are met AND
        // - Server is running AND connected to DB/RMI AND
        // - No client is currently being started (checked via clientStarting flag)
        boolean shouldDisableClient = !prerequisitesMet || !serverConnected || clientStarting;
        runClientButton.setDisable(shouldDisableClient);

        // Language selection: disabled when a client is starting (to prevent language changes during client startup)
        languageComboBox.setDisable(clientStarting);

        // Hide progress indicators when buttons are disabled
        Platform.runLater(() -> {
            if (runServerButton.isDisabled() && !serverRunning) {
                serverProgressIndicator.setVisible(false);
                serverProgressIndicator.setProgress(0); // Reset progress
            }
            if (runClientButton.isDisabled() && !clientStarting) {
                clientProgressIndicator.setVisible(false);
                clientProgressIndicator.setProgress(0); // Reset progress
            }
        });

        if (prerequisitesMet) {
            if (serverConnected) {
                appendToStatus(LauncherLanguageManager.getString("launcher.env.ready.message"));
            } else if (serverRunning) {
                appendToStatus(LauncherLanguageManager.getString("launcher.env.server.starting.message"));
            } else {
                appendToStatus(LauncherLanguageManager.getString("launcher.env.server.ready.message"));
            }
        } else {
            appendToStatus(LauncherLanguageManager.getString("launcher.env.not.ready.message"));
        }
    }

    private void startServerProcessAsync() {
        try {
            ProcessBuilder pb;
                // Use Maven (preferred method for JavaFX applications)
                logger.info("Using Maven to start server (recommended for JavaFX applications)");
                appendToStatus(LauncherLanguageManager.getString("launcher.status.server.starting.jar"));

                // Get the project root directory
                java.io.File currentDir = new java.io.File(System.getProperty("user.dir"));
                java.io.File projectRoot = currentDir;

                // If we're in src/launcher, go up two levels: src/launcher -> src/ -> project root
                if (currentDir.getName().equals("launcher") && currentDir.getParentFile().getName().equals("src")) {
                    projectRoot = currentDir.getParentFile().getParentFile(); // project root
                }

                if (projectRoot == null) {
                    projectRoot = currentDir;
                }

                if (OSDetector.isWindows()) {
                    // Alternative approach: change to module directory and run mvn there
                    java.io.File moduleDir = new java.io.File(projectRoot, "src/serverBR");
                    if (!moduleDir.exists() || !moduleDir.isDirectory()) {
                        throw new RuntimeException("Module directory does not exist: " + moduleDir.getAbsolutePath());
                    }
                    String quotedDir = "\"" + moduleDir.getAbsolutePath() + "\"";
                    pb = new ProcessBuilder("cmd", "/c", "cd", "/d", quotedDir, "&&", "mvn.cmd", "javafx:run");
                    pb.directory(null);
                } else {
                    pb = new ProcessBuilder("mvn", "javafx:run", "-pl", "src/serverBR");
                    pb.directory(projectRoot);
                }
            

            // Set environment variables for language configuration
            pb.environment().put("APP_LANGUAGE", languageComboBox.getValue());
            pb.inheritIO(); // This will show output in the same console
            serverProcess = pb.start();

            // Monitor server process in background
            processMonitor.schedule(() -> monitorServerProcess(), 2, java.util.concurrent.TimeUnit.SECONDS);

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to start server process", e);
            throw new RuntimeException("Server startup failed", e);
        }
    }

    private void monitorServerProcess() {
        if (serverProcess != null) {
            try {
                int exitCode = serverProcess.waitFor();
                javafx.application.Platform.runLater(() -> {
                    if (serverRunning) {
                        appendToStatus(LauncherLanguageManager.getString("launcher.status.server.terminated", exitCode));
                        serverRunning = false;
                        serverConnected = false; // Server is no longer connected
                        updateButtonsState(currentEnvStatus != null && currentEnvStatus.allPrerequisitesMet);
                    }
                });
            } catch (InterruptedException e) {
                // Thread interrupted (normal during application shutdown)
                Thread.currentThread().interrupt();
                // Don't log as warning - this is expected during shutdown
                logger.fine("Server monitoring interrupted during shutdown");
            }
        }
    }

    private void startClientProcess() {
        try {
            ProcessBuilder pb;
                // Use Maven (preferred method for JavaFX applications)
                logger.info("Using Maven to start client (recommended for JavaFX applications)");
                appendToStatus(LauncherLanguageManager.getString("launcher.status.client.starting.jar"));

                // Get the project root directory
                java.io.File currentDir = new java.io.File(System.getProperty("user.dir"));
                java.io.File projectRoot = currentDir;

                // If we're in src/launcher, go up two levels: src/launcher -> src/ -> project root
                if (currentDir.getName().equals("launcher") && currentDir.getParentFile().getName().equals("src")) {
                    projectRoot = currentDir.getParentFile().getParentFile();
                }

                if (projectRoot == null) {
                    projectRoot = currentDir;
                }

                if (OSDetector.isWindows()) {
                    // Alternative approach: change to module directory and run mvn there
                    java.io.File moduleDir = new java.io.File(projectRoot, "src/clientBR");
                    if (!moduleDir.exists() || !moduleDir.isDirectory()) {
                        throw new RuntimeException("Client module directory does not exist: " + moduleDir.getAbsolutePath());
                    }
                    String quotedDir = "\"" + moduleDir.getAbsolutePath() + "\"";
                    pb = new ProcessBuilder("cmd", "/c", "cd", "/d", quotedDir, "&&", "mvn.cmd", "javafx:run");
                    pb.directory(null);
                } else {
                    pb = new ProcessBuilder("mvn", "javafx:run", "-pl", "src/clientBR");
                    pb.directory(projectRoot);
                }
            

            // Set environment variables for language configuration
            pb.environment().put("APP_LANGUAGE", languageComboBox.getValue());
            pb.inheritIO(); // This will show output in the same console
            Process process = pb.start();

            // Add to client processes list for tracking
            synchronized (clientProcesses) {
                clientProcesses.add(process);
            }

            // Monitor this client process
            processMonitor.execute(() -> monitorClientProcess(process));

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to start client process", e);
            throw new RuntimeException("Client startup failed", e);
        }
    }

    private void monitorClientProcess(Process process) {
        try {
            int exitCode = process.waitFor();
            synchronized (clientProcesses) {
                clientProcesses.remove(process);
            }
            javafx.application.Platform.runLater(() -> {
                    appendToStatus(LauncherLanguageManager.getString("launcher.status.client.terminated", exitCode));
            });
        } catch (InterruptedException e) {
            // Thread interrupted (normal during application shutdown)
            Thread.currentThread().interrupt();
            synchronized (clientProcesses) {
                clientProcesses.remove(process);
            }
            // Don't log as warning - this is expected during shutdown
            logger.fine("Client monitoring interrupted during shutdown");
        }
    }

    private void cleanupDeadProcesses() {
        synchronized (clientProcesses) {
            int beforeSize = clientProcesses.size();
            clientProcesses.removeIf(process -> {
                boolean isDead = process != null && !process.isAlive();
                if (isDead) {
                    logger.fine("Cleaned up dead client process");
                }
                return isDead;
            });
            int afterSize = clientProcesses.size();

            if (beforeSize != afterSize) {
                javafx.application.Platform.runLater(() -> {
                    appendToStatus(LauncherLanguageManager.getString("launcher.status.cleanup.dead",
                                     (beforeSize - afterSize), afterSize));
                });
            }
        }
    }

    private void appendToStatus(String message) {
        // Per ridurre il numero di aggiornamenti UI, accumula messaggi se possibile
        // Invece di usare Platform.runLater per ogni chiamata, considera di accumulare
        javafx.application.Platform.runLater(() -> {
            statusArea.appendText(message + "\n");
            // Scroll automatico alla fine
            statusArea.setScrollTop(Double.MAX_VALUE);
        });
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }



    private void handleApplicationShutdown() {
        appendToStatus(LauncherLanguageManager.getString("launcher.status.shutdown"));

        // Cancel any pending client button re-enable task and ensure button is re-enabled
        if (clientButtonReenableTask != null && !clientButtonReenableTask.isDone()) {
            clientButtonReenableTask.cancel(true);
            logger.fine("Cancelled client button re-enable task during shutdown");
        }
        // Ensure client button is re-enabled during shutdown
        clientStarting = false;
        Platform.runLater(() -> updateButtonsState(currentEnvStatus != null && currentEnvStatus.allPrerequisitesMet));

        // Language watcher removed - no shutdown needed

        // Stop process monitor
        if (processMonitor != null && !processMonitor.isShutdown()) {
            processMonitor.shutdown();
            try {
                if (!processMonitor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    processMonitor.shutdownNow();
                }
            } catch (InterruptedException e) {
                processMonitor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        // Terminate server process if running
        if (serverProcess != null && serverProcess.isAlive()) {
            try {
                logger.info("Terminating server process...");
                appendToStatus(LauncherLanguageManager.getString("launcher.status.server.terminating"));

                // First try graceful shutdown with SIGTERM
                serverProcess.destroy();

                // Wait up to 15 seconds for graceful shutdown
                if (!serverProcess.waitFor(15, java.util.concurrent.TimeUnit.SECONDS)) {
                    logger.warning("Server process didn't terminate gracefully within 15s, forcing termination...");
                    appendToStatus(LauncherLanguageManager.getString("launcher.status.server.force.terminate"));

                    // Force termination with SIGKILL
                    serverProcess.destroyForcibly();

                    // Wait additional 5 seconds for forced termination
                    if (!serverProcess.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)) {
                        logger.info("Server process could not be terminated even with SIGKILL");
                        appendToStatus(LauncherLanguageManager.getString("launcher.status.server.terminate.failed"));
                    } else {
                        logger.info("Server process forcibly terminated");
                        appendToStatus(LauncherLanguageManager.getString("launcher.status.server.force.terminated"));
                    }
                } else {
                    logger.info("Server process terminated gracefully");
                    appendToStatus(LauncherLanguageManager.getString("launcher.status.server.terminated"));
                }
            } catch (Exception e) {
                logger.log(java.util.logging.Level.WARNING, "Error terminating server process", e);
                appendToStatus(LauncherLanguageManager.getString("launcher.status.server.terminate.error"));
            }
        }
            
    }

    /**
     * Get the project root directory for saving shared configuration
     */
    private java.io.File getProjectRootDirectory() {
        java.io.File currentDir = new java.io.File(System.getProperty("user.dir"));
        logger.info("=== PROJECT ROOT DETECTION ===");
        logger.info("Current working directory: " + currentDir.getAbsolutePath());
        logger.info("Current dir exists: " + currentDir.exists());
        logger.info("Current dir is directory: " + currentDir.isDirectory());

        java.io.File projectRoot = null;
        java.io.File searchDir = currentDir;
        int searchLevel = 0;

        // Start from current directory and go up until we find the project root
        while (searchDir != null && searchLevel < 10) { // Limit to 10 levels up to prevent infinite loop
            logger.info("Searching level " + searchLevel + ": " + searchDir.getAbsolutePath());

            java.io.File pomFile = new java.io.File(searchDir, "pom.xml");
            java.io.File srcDir = new java.io.File(searchDir, "src");

            logger.info("  pom.xml exists: " + pomFile.exists());
            logger.info("  src/ exists: " + srcDir.exists());
            logger.info("  src/ is directory: " + srcDir.isDirectory());

            // Check if this is the project root (has pom.xml AND src directory with our modules)
            if (pomFile.exists() && srcDir.exists() && srcDir.isDirectory()) {
                // Additional check: make sure src contains the expected modules
                java.io.File serverModule = new java.io.File(srcDir, "serverBR");
                java.io.File clientModule = new java.io.File(srcDir, "clientBR");
                java.io.File launcherModule = new java.io.File(srcDir, "launcher");
                java.io.File sharedModule = new java.io.File(srcDir, "shared");

                logger.info("  serverBR exists: " + serverModule.exists());
                logger.info("  clientBR exists: " + clientModule.exists());
                logger.info("  launcher exists: " + launcherModule.exists());
                logger.info("  shared exists: " + sharedModule.exists());

                // Check if this directory contains all our project modules
                if (serverModule.exists() && clientModule.exists() && launcherModule.exists() && sharedModule.exists()) {
                    // This is definitely the project root!
                    projectRoot = searchDir;
                    logger.info("✓ Found project root with all modules: " + searchDir.getAbsolutePath());
                    break;
                } else {
                    logger.info("✗ Found pom.xml and src/, but missing expected modules (serverBR, clientBR, launcher, shared)");
                }
            } else {
                logger.info("✗ Missing pom.xml or src/ directory");
            }

            // Go up one directory
            searchDir = searchDir.getParentFile();
            searchLevel++;
        }

        if (projectRoot == null) {
            // Last resort: look for any directory containing our modules
            logger.info("Primary search failed, trying fallback module search...");
            projectRoot = findProjectRootByModules(currentDir);
        }

        if (projectRoot == null) {
            // ABSOLUTE last resort - this should almost never happen
            logger.info("❌ All searches failed!");
            logger.info("This might indicate the project structure is corrupted or you're running from an unexpected location.");
            logger.info("Expected structure: project_root/pom.xml + project_root/src/{serverBR,clientBR,launcher,shared}/pom.xml");

            // Instead of using currentDir, let's try to be smarter
            java.io.File guessedRoot = guessProjectRoot(currentDir);
            if (guessedRoot != null) {
                projectRoot = guessedRoot;
                logger.info("Using guessed project root: " + projectRoot.getAbsolutePath());
            } else {
                projectRoot = currentDir;
                logger.info("Using current directory as absolute fallback: " + currentDir.getAbsolutePath());
            }
        }

        logger.info("Final project root: " + projectRoot.getAbsolutePath());
        logger.info("=== END PROJECT ROOT DETECTION ===");
        return projectRoot;
    }

    /**
     * Fallback method to find project root by looking for our specific modules
     */
    private java.io.File findProjectRootByModules(java.io.File startDir) {
        logger.info("Fallback search starting from: " + startDir.getAbsolutePath());

        // Look for src/serverBR, src/clientBR, src/launcher in the tree
        java.io.File candidate = startDir;
        int levelsUp = 0;

        while (candidate != null && levelsUp < 10) {
            logger.info("Fallback checking level " + levelsUp + ": " + candidate.getAbsolutePath());

            java.io.File srcDir = new java.io.File(candidate, "src");
            if (srcDir.exists() && srcDir.isDirectory()) {
                java.io.File serverBR = new java.io.File(srcDir, "serverBR");
                java.io.File clientBR = new java.io.File(srcDir, "clientBR");
                java.io.File launcher = new java.io.File(srcDir, "launcher");
                java.io.File shared = new java.io.File(srcDir, "shared");

                logger.info("  src/ found, checking modules:");
                logger.info("    serverBR: " + serverBR.exists());
                logger.info("    clientBR: " + clientBR.exists());
                logger.info("    launcher: " + launcher.exists());
                logger.info("    shared: " + shared.exists());

                if (serverBR.exists() && clientBR.exists() && launcher.exists() && shared.exists()) {
                    logger.info("✓ Found all modules in: " + candidate.getAbsolutePath());
                    return candidate;
                }
            } else {
                logger.info("  src/ not found or not a directory");
            }

            candidate = candidate.getParentFile();
            levelsUp++;
        }

        logger.info("Fallback search found nothing");
        return null;
    }

    /**
     * Last resort method to guess project root based on common patterns
     */
    private java.io.File guessProjectRoot(java.io.File currentDir) {
        logger.info("Attempting to guess project root from: " + currentDir.getAbsolutePath());

        // Pattern 1: if we're in src/something, go up 2 levels
        String path = currentDir.getAbsolutePath();
        if (path.contains(java.io.File.separator + "src" + java.io.File.separator)) {
            java.io.File guessed = currentDir.getParentFile(); // out of src/something
            if (guessed != null) {
                guessed = guessed.getParentFile(); // out of src/
            }
            if (guessed != null && new java.io.File(guessed, "pom.xml").exists()) {
                logger.info("Guessed root (src/ pattern): " + guessed.getAbsolutePath());
                return guessed;
            }
        }

        // Pattern 2: if we're in target/, go up 3 levels (something/target/ -> something/ -> src/ -> root/)
        if (path.endsWith(java.io.File.separator + "target")) {
            java.io.File guessed = currentDir.getParentFile(); // out of target
            if (guessed != null) {
                guessed = guessed.getParentFile(); // out of src/something
                if (guessed != null) {
                    guessed = guessed.getParentFile(); // out of src/
                }
            }
            if (guessed != null && new java.io.File(guessed, "pom.xml").exists()) {
                logger.info("Guessed root (target/ pattern): " + guessed.getAbsolutePath());
                return guessed;
            }
        }

        logger.info("Could not guess project root");
        return null;
    }

    /**
     * Refresh all UI texts with the current language
     * Ottimizzato per ridurre controlli null dopo inizializzazione FXML
     */
    private void refreshUITexts() {
        // Refresh all UI texts with current language
        // Add null checks to prevent NPE during language changes
        try {
            if (titleLabel != null) {
                titleLabel.setText(LauncherLanguageManager.getString("launcher.app.title"));
            }
            if (subtitleLabel != null) {
                subtitleLabel.setText(LauncherLanguageManager.getString("launcher.subtitle"));
            }
            if (languageLabel != null) {
                languageLabel.setText(LauncherLanguageManager.getString("launcher.language.label"));
            }
            if (statusLabel != null) {
                statusLabel.setText(LauncherLanguageManager.getString("launcher.status.label"));
            }
            if (footerLabel != null) {
                footerLabel.setText(LauncherLanguageManager.getString("launcher.footer"));
            }

            // Update button texts
            if (checkEnvButton != null) {
                checkEnvButton.setText(LauncherLanguageManager.getString("launcher.button.check.environment"));
            }
            if (runServerButton != null) {
                runServerButton.setText(LauncherLanguageManager.getString("launcher.button.start.server"));
            }
            if (runClientButton != null) {
                runClientButton.setText(LauncherLanguageManager.getString("launcher.button.open.client"));
            }

            logger.fine("Launcher UI texts refreshed successfully");

        } catch (Exception e) {
            logger.log(Level.WARNING, "Error refreshing UI texts: " + e.getMessage(), e);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
