package it.uninsubria.server.db;

import it.uninsubria.shared.utils.AppConfig;
import it.uninsubria.server.util.SessionManager;
import it.uninsubria.server.util.ConnectionPoolManager;
import it.uninsubria.server.ServerMessageManager;
import java.sql.Connection;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerController {
    @FXML private TextField hostField;
    @FXML private TextField dbNameField;
    @FXML private TextField userField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;
    @FXML private ProgressIndicator resetProgressIndicator;
    @FXML private Button resetButton;
    @FXML private Button connectingButton;
    @FXML private Button startServerButton;
    @FXML private Button stopServerButton;
    @FXML private Button disconnectDBButton;

    // Labels for internationalization
    @FXML private Label titleLabel;
    @FXML private Label hostLabel;
    @FXML private Label dbNameLabel;
    @FXML private Label userLabel;
    @FXML private Label passwordLabel;
    private static final Logger logger = LoggerFactory.getLogger(ServerController.class);

    /**
     * Risolve una chiave di localizzazione usando ServerMessageManager.
     * Se inizia con %, usa il bundle del server, altrimenti restituisce la stringa così com'è.
     */
    private static String resolveString(String key) {
        if (key != null && key.startsWith("%")) {
            String actualKey = key.substring(1); // Rimuovi il %
            try {
                return ServerMessageManager.getString(actualKey);
            } catch (Exception e) {
                // Fallback alla chiave se la traduzione non è trovata
                return key;
            }
        }
        return key;
    }

    /**
     * Inizializza l'interfaccia con le stringhe localizzate.
     */
    private void initializeLocalizedStrings() {
        // Imposta le stringhe localizzate per i componenti dell'interfaccia
        if (titleLabel != null) {
            titleLabel.setText(resolveString("%server.ui.title"));
        }
        if (hostLabel != null) {
            hostLabel.setText(resolveString("%server.ui.label.host"));
        }
        if (dbNameLabel != null) {
            dbNameLabel.setText(resolveString("%server.ui.label.dbname"));
        }
        if (userLabel != null) {
            userLabel.setText(resolveString("%server.ui.label.username"));
        }
        if (passwordLabel != null) {
            passwordLabel.setText(resolveString("%server.ui.label.password"));
        }

        // Imposta i testi dei pulsanti
        if (connectingButton != null) {
            connectingButton.setText(resolveString("%server.ui.button.connect"));
        }
        if (startServerButton != null) {
            startServerButton.setText(resolveString("%server.ui.button.start"));
        }
        if (disconnectDBButton != null) {
            disconnectDBButton.setText(resolveString("%server.ui.button.disconnect"));
        }
        if (stopServerButton != null) {
            stopServerButton.setText(resolveString("%server.ui.button.stop"));
        }
        if (resetButton != null) {
            resetButton.setText(resolveString("%server.ui.button.reset"));
        }

        // Imposta i prompt text per i campi input
        if (hostField != null) {
            hostField.setPromptText(resolveString("%server.ui.prompt.host"));
        }
        if (dbNameField != null) {
            dbNameField.setPromptText(resolveString("%server.ui.prompt.dbname"));
        }
        if (userField != null) {
            userField.setPromptText(resolveString("%server.ui.prompt.username"));
        }
        if (passwordField != null) {
            passwordField.setPromptText(resolveString("%server.ui.prompt.password"));
        }
    }

    private void updateUIOnDBConnected(){
        statusLabel.getStyleClass().add("labelGreen");
        statusLabel.setText(ServerMessageManager.getString("server.ui.db.connected"));
        connectingButton.setDisable(true);
        connectingButton.setText("DB connesso");

        disconnectDBButton.setVisible(true);
        resetButton.setVisible(true);
        startServerButton.setDisable(false);
    }

    private void updateUIOnDBDisconnected() {
        statusLabel.getStyleClass().add("labelInfo");
        statusLabel.setText(ServerMessageManager.getString("server.ui.db.disconnected"));
        connectingButton.setDisable(false);
        connectingButton.setText("Connetti al DB");

        disconnectDBButton.setVisible(false);
        resetButton.setVisible(false);
        startServerButton.setDisable(true);
        stopServerButton.setVisible(false);
        startServerButton.setText("Avvia Server");
    }

    private void updateUIOnServerStarted() {
        startServerButton.setText("Server avviato");
        startServerButton.setDisable(true);
        stopServerButton.setVisible(true);
        statusLabel.getStyleClass().add("labelGreen");
        statusLabel.setText(ServerMessageManager.getString("server.ui.server.started"));
    }

    private void updateUIOnServerStopped() {
        stopServerButton.setVisible(false);
        startServerButton.setDisable(false);
        startServerButton.setText("Avvia Server");
        statusLabel.setText(ServerMessageManager.getString("server.ui.server.stopped"));
    }

    private void initializeCSS() {
        resetProgressIndicator.getStyleClass().add("progressIndicatorBlue");
    }

    @FXML
    public void initialize(){
        initializeCSS();

        // Inizializza le stringhe localizzate
        initializeLocalizedStrings();

        hostField.setText("localhost");
        dbNameField.setText("postgres");
        userField.setText("postgres");
        updateUIOnDBDisconnected();  // Stato iniziale = DB disconnesso
    }

    @FXML
    public void onDisconnectDB(){

        try {

            BookRecommenderServer.stopServer(); // anche se non attivo, non fa nulla
            
            startServerButton.setDisable(false);
            startServerButton.setText("Avvia Server");
            stopServerButton.setVisible(false);
            
            // Close connection pool
            DBInitializer.shutdown();
            updateUIOnDBDisconnected();
        } catch (Exception e) {
            statusLabel.getStyleClass().add("labelRed");
            statusLabel.setText(ServerMessageManager.getString("server.ui.db.disconnect.error") + e.getMessage());
            logger.error("Errore durante disconnessione DB", e);
        }
    }

    @FXML
    public void onStopServer(){
        try {
            BookRecommenderServer.stopServer();
            updateUIOnServerStopped();
        } catch (Exception e) {
            statusLabel.getStyleClass().add("labelRed");
            statusLabel.setText(ServerMessageManager.getString("server.ui.server.stop.error") + e.getMessage());
            logger.error("Errore durante l'arresto del server", e);
        }
    }    


    @FXML
    public void onConnectDB() {
        String host = hostField.getText().trim();
        String dbName = dbNameField.getText().trim();
        String user = userField.getText().trim();
        String password = passwordField.getText();

        if (host.isEmpty() || dbName.isEmpty() || user.isEmpty()) {
            statusLabel.getStyleClass().add("labelRed");
            statusLabel.setText(ServerMessageManager.getString("server.ui.fields.required"));
            return;
        }

        AppConfig.setServerHost(host);

        statusLabel.getStyleClass().add("labelInfo");
        statusLabel.setText(ServerMessageManager.getString("server.ui.db.connecting"));
        connectingButton.setDisable(true);

        Task<Void> connectTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                DBInitializer.initialize(host, dbName, user, password);
                // I dati di test verranno creati solo al login con credenziali di test
                return null;
            }
        };

        connectTask.setOnSucceeded(e -> updateUIOnDBConnected());

        connectTask.setOnFailed(e -> {
            statusLabel.getStyleClass().add("labelRed");
            statusLabel.setText(ServerMessageManager.getString("server.ui.db.connection.error") + connectTask.getException().getMessage());
            logger.error("Errore di connessione DB", connectTask.getException());
            logger.error("Connection task failed", connectTask.getException());
            connectingButton.setDisable(false);
            disconnectDBButton.setVisible(false);
            resetButton.setVisible(false);
        });

        new Thread(connectTask).start();
    }

    @FXML
    public void onResetDB() {
        if (!ConnectionPoolManager.isInitialized()) {
            statusLabel.getStyleClass().add("labelRed");
            statusLabel.setText(ServerMessageManager.getString("server.ui.db.not.initialized"));
            return;
        }

        // Disabilito tutti i controlli durante il reset per evitare conflitti
        resetButton.setDisable(true);
        startServerButton.setDisable(true);
        stopServerButton.setDisable(true);
        disconnectDBButton.setDisable(true);

        // Mostro l'indicatore di caricamento e aggiorno il testo
        resetProgressIndicator.setVisible(true);
        resetProgressIndicator.setProgress(-1); // Indeterminate progress
        statusLabel.getStyleClass().add("labelBlue");
        statusLabel.setText(ServerMessageManager.getString("server.ui.db.reset.progress"));

        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                // Get connection from pool for reset operations
                Connection resetConn = ConnectionPoolManager.getConnection();
                try {
                    // Eseguo il reset DB in background
                    DBInitializer.clearDatabase(resetConn);
                    // Re-apply init.sql schema
                    DBInitializer.applyInitSQL(resetConn);
                    // Ensure any new/altered schema is present
                    DBInitializer.ensureSchemaSync(resetConn);
                    // Ricarico i dati seed
                    DBInitializer.reloadBooksFromCSV(resetConn);
                } finally {
                    // Always close connection to return to pool
                    if (resetConn != null) {
                        resetConn.close();
                    }
                }
                return null;
            }
        };

        // Se il task termina con successo
        task.setOnSucceeded(ev -> {
            resetProgressIndicator.setVisible(false);
            statusLabel.getStyleClass().add("labelGreen");
            statusLabel.setText(ServerMessageManager.getString("server.ui.db.reset.success"));
            resetButton.setDisable(false);
            startServerButton.setDisable(false);
            stopServerButton.setDisable(false);
            disconnectDBButton.setDisable(false);
        });

        // Se il task termina con errore
        task.setOnFailed(ev -> {
            resetProgressIndicator.setVisible(false);
            Throwable e = task.getException();
            statusLabel.getStyleClass().add("labelRed");
            statusLabel.setText(ServerMessageManager.getString("server.ui.db.reset.error") + e.getMessage());
            logger.error("Errore durante il reset DB", e);
            resetButton.setDisable(false);
            startServerButton.setDisable(false);
            stopServerButton.setDisable(false);
            disconnectDBButton.setDisable(false);
        });

        // Eseguo il task in un thread separato
        new Thread(task).start();
    }

    @FXML
    public void onStartServer() {
        if (!ConnectionPoolManager.isInitialized()) {
            statusLabel.getStyleClass().add("labelRed");
            statusLabel.setText(ServerMessageManager.getString("server.ui.db.required"));
            return;
        }

        try {
            BookRecommenderServer.startServer();
            updateUIOnServerStarted();
        } catch (Exception e) {
            statusLabel.getStyleClass().add("labelRed");
            statusLabel.setText(ServerMessageManager.getString("server.ui.server.start.error") + e.getMessage());
            startServerButton.setDisable(false);
            stopServerButton.setVisible(false);
            logger.error("Errore durante l'avvio del server", e);
        }
    }




    public void stop() {
        try {
            BookRecommenderServer.stopServer();
            SessionManager.shutdown(); // Shutdown session manager
            DBInitializer.shutdown();
        } catch (Exception e) {
            logger.warn("Errore durante stop del server", e);
        }
    }
}
