package it.uninsubria.client.utils.classesUI;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.*;
import javafx.util.Duration;

import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import it.uninsubria.shared.utils.LoggerUtil;

/**
 * Sistema centralizzato per notifiche e stati di caricamento.
 * Fornisce toast notifications, progress indicators e gestione stati caricamento.
 */
public class NotificationManager {
    private static final Logger logger = LoggerUtil.getLogger(NotificationManager.class);

    // Stili per i diversi tipi di notifica
    public enum NotificationType {
        SUCCESS("#28a745", "✅"),
        ERROR("#dc3545", "❌"),
        WARNING("#ffc107", "⚠️"),
        INFO("#17a2b8", "ℹ️"),
        LOADING("#007bff", "⏳");

        public final String color;
        public final String icon;

        NotificationType(String color, String icon) {
            this.color = color;
            this.icon = icon;
        }
    }

    // Stati di caricamento globali
    private static final ConcurrentHashMap<String, LoadingState> activeLoadings = new ConcurrentHashMap<>();

    // Stage principale per le notifiche
    private static Stage notificationStage;
    private static VBox notificationContainer;
    private static boolean initialized = false;

    // Flag per disabilitare JavaFX in test (per ambienti headless)
    private static boolean javaFxDisabled = false;

    /**
     * Inizializza il sistema di notifiche (lazy initialization)
     */
    private static void initializeNotificationSystem() {
        if (initialized || javaFxDisabled) {
            return;
        }

        try {
            notificationStage = new Stage();
            notificationStage.initStyle(StageStyle.TRANSPARENT);
            notificationStage.setAlwaysOnTop(true);
            notificationStage.setResizable(false);

            // Posiziona in alto a destra
            notificationStage.setX(Screen.getPrimary().getVisualBounds().getMaxX() - 350);
            notificationStage.setY(50);

            notificationContainer = new VBox(10);
            notificationContainer.setPadding(new Insets(10));
            notificationContainer.setAlignment(Pos.TOP_RIGHT);

            Scene scene = new Scene(notificationContainer);
            scene.setFill(Color.TRANSPARENT);
            scene.getStylesheets().add("/css/CommonFeatures.css");

            notificationStage.setScene(scene);
            notificationStage.setTitle(LanguageManager.getBundle().getString("notifications.title"));

            initialized = true;
        } catch (Exception e) {
            logger.warning("Impossibile inizializzare il sistema di notifiche JavaFX: " + e.getMessage());
            javaFxDisabled = true;
        }
    }

    /**
     * Disabilita JavaFX per testing in ambienti headless
     */
    public static void disableJavaFx() {
        javaFxDisabled = true;
        logger.info("JavaFX disabilitato per testing");
    }

    /**
     * Riabilita JavaFX
     */
    public static void enableJavaFx() {
        javaFxDisabled = false;
        initialized = false;
    }

    // ===== NOTIFICHE TOAST =====

    /**
     * Mostra una notifica toast
     */
    public static void showNotification(String message, NotificationType type) {
        showNotification(message, type, 3000); // Default 3 secondi
    }

    /**
     * Mostra una notifica toast con durata personalizzata
     */
    public static void showNotification(String message, NotificationType type, long durationMs) {
        if (javaFxDisabled) {
            // In modalità test, logga solo il messaggio
            logger.info(String.format("NOTIFICATION [%s]: %s", type.name(), message));
            return;
        }

        Platform.runLater(() -> {
            initializeNotificationSystem();
            if (javaFxDisabled) return; // Controllo aggiuntivo

            HBox notification = createNotificationBox(message, type);

            // Aggiungi alla scena
            notificationContainer.getChildren().add(0, notification);
            notificationStage.show();

            // Animazione di entrata
            TranslateTransition slideIn = new TranslateTransition(Duration.millis(300), notification);
            slideIn.setFromX(350);
            slideIn.setToX(0);
            slideIn.play();

            // Rimozione automatica dopo la durata
            PauseTransition pause = new PauseTransition(Duration.millis(durationMs));
            pause.setOnFinished(e -> hideNotification(notification));
            pause.play();
        });
    }

    /**
     * Crea il box della notifica
     */
    private static HBox createNotificationBox(String message, NotificationType type) {
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(10, 15, 10, 15));
        box.setPrefWidth(320);
        box.setMaxWidth(320);
        box.setStyle(String.format(
            "-fx-background-color: %s; -fx-background-radius: 8; -fx-border-radius: 8; " +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 4, 0, 0, 2);",
            type.color
        ));

        // Icona
        Label iconLabel = new Label(type.icon);
        iconLabel.setStyle("-fx-font-size: 16px;");

        // Messaggio
        Label messageLabel = new Label(message);
        messageLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-wrap-text: true;");
        messageLabel.setMaxWidth(250);
        messageLabel.setWrapText(true);

        // Pulsante chiusura (opzionale)
        Label closeLabel = new Label("×");
        closeLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.7); -fx-font-size: 18px; -fx-cursor: hand;");
        closeLabel.setOnMouseClicked(e -> hideNotification(box));

        box.getChildren().addAll(iconLabel, messageLabel, closeLabel);
        HBox.setHgrow(messageLabel, Priority.ALWAYS);

        return box;
    }

    /**
     * Nasconde una notifica con animazione
     */
    private static void hideNotification(HBox notification) {
        TranslateTransition slideOut = new TranslateTransition(Duration.millis(300), notification);
        slideOut.setToX(350);
        slideOut.setOnFinished(e -> {
            notificationContainer.getChildren().remove(notification);
            if (notificationContainer.getChildren().isEmpty()) {
                notificationStage.hide();
            }
        });
        slideOut.play();
    }

    // ===== STATI DI CARICAMENTO =====

    /**
     * Classe per rappresentare uno stato di caricamento
     */
    public static class LoadingState {
        public final String id;
        public final String message;
        public final long startTime;
        public volatile boolean active;

        public LoadingState(String id, String message) {
            this.id = id;
            this.message = message;
            this.startTime = System.currentTimeMillis();
            this.active = true;
        }

        public long getElapsedTime() {
            return System.currentTimeMillis() - startTime;
        }
    }

    /**
     * Avvia uno stato di caricamento
     */
    public static LoadingState startLoading(String operationId, String message) {
        LoadingState state = new LoadingState(operationId, message);
        activeLoadings.put(operationId, state);

        logger.info("Avviato caricamento: " + operationId + " - " + message);

        // Mostra notifica di caricamento
        showNotification(message, NotificationType.LOADING, 0); // Durata infinita

        return state;
    }

    /**
     * Termina uno stato di caricamento con successo
     */
    public static void endLoading(String operationId) {
        LoadingState state = activeLoadings.remove(operationId);
        if (state != null) {
            state.active = false;
            long duration = state.getElapsedTime();

            logger.info(String.format("Caricamento completato: %s (%.2fs)",
                operationId, duration / 1000.0));

            // Mostra notifica di successo
            showNotification("Operazione completata", NotificationType.SUCCESS);
        }
    }

    /**
     * Termina uno stato di caricamento con errore
     */
    public static void endLoadingWithError(String operationId, String errorMessage) {
        LoadingState state = activeLoadings.remove(operationId);
        if (state != null) {
            state.active = false;
            long duration = state.getElapsedTime();

            logger.warning(String.format("Caricamento fallito: %s dopo %.2fs - %s",
                operationId, duration / 1000.0, errorMessage));

            // Mostra notifica di errore
            showNotification("Errore: " + errorMessage, NotificationType.ERROR, 5000);
        }
    }

    /**
     * Verifica se un'operazione è in caricamento
     */
    public static boolean isLoading(String operationId) {
        LoadingState state = activeLoadings.get(operationId);
        return state != null && state.active;
    }

    /**
     * Ottiene tutti gli stati di caricamento attivi
     */
    public static java.util.Map<String, LoadingState> getActiveLoadings() {
        return new java.util.HashMap<>(activeLoadings);
    }

    // ===== METODI DI UTILITÀ =====

    /**
     * Mostra una notifica di successo
     */
    public static void showSuccess(String message) {
        showNotification(message, NotificationType.SUCCESS);
    }

    /**
     * Mostra una notifica di errore
     */
    public static void showError(String message) {
        showNotification(message, NotificationType.ERROR, 5000);
    }

    /**
     * Mostra una notifica di warning
     */
    public static void showWarning(String message) {
        showNotification(message, NotificationType.WARNING, 4000);
    }

    /**
     * Mostra una notifica informativa
     */
    public static void showInfo(String message) {
        showNotification(message, NotificationType.INFO, 3000);
    }

    /**
     * Chiude tutte le notifiche
     */
    public static void clearAllNotifications() {
        if (javaFxDisabled) {
            logger.info("NOTIFICATIONS CLEARED (test mode)");
            return;
        }

        Platform.runLater(() -> {
            if (notificationContainer != null) {
                notificationContainer.getChildren().clear();
            }
            if (notificationStage != null) {
                notificationStage.hide();
            }
        });
    }

    /**
     * Chiude tutti gli stati di caricamento attivi
     */
    public static void clearAllLoadings() {
        activeLoadings.clear();
        clearAllNotifications();
        logger.info("Tutti gli stati di caricamento sono stati chiusi");
    }
}