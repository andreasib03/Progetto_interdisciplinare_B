package it.uninsubria.client.utils.classesUI;

import java.util.Timer;
import java.util.TimerTask;
import java.util.prefs.Preferences;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.stage.Window;
import it.uninsubria.shared.utils.AppConstants;

/**
 * Manages automatic logout/timeout functionality based on user inactivity
 */
public class TimeoutManager {
    private static TimeoutManager instance;
    private Timer inactivityTimer;
    private long timeoutMillis = 0; // Default disabled (no timeout)
    private boolean enabled = true;

    // Preferences keys
    private static final String PREF_NODE = "BookRecommenderClient";
    private static final String KEY_TIMEOUT_MINUTES = "timeoutMinutes";

    private TimeoutManager() {
        loadTimeoutSetting();
    }

    public static TimeoutManager getInstance() {
        if (instance == null) {
            instance = new TimeoutManager();
        }
        return instance;
    }

    /**
     * Starts or resets the inactivity timer
     */
    public void resetTimer() {
        if (!enabled) return;

        cancelTimer();

        inactivityTimer = new Timer(true);
        inactivityTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    performTimeoutAction();
                });
            }
        }, timeoutMillis);
    }

    /**
     * Cancels the current inactivity timer
     */
    public void cancelTimer() {
        if (inactivityTimer != null) {
            inactivityTimer.cancel();
            inactivityTimer = null;
        }
    }

    /**
     * Sets the timeout duration in minutes
     */
    public void setTimeoutMinutes(int minutes) {
        if (minutes <= 0) {
            enabled = false;
            cancelTimer();
        } else {
            enabled = true;
            this.timeoutMillis = minutes * AppConstants.Time.MILLIS_PER_MINUTE;
            saveTimeoutSetting(minutes);
            resetTimer();
        }
    }

    /**
     * Sets timeout from string value (e.g., "15 minuti")
     */
    public void setTimeoutFromString(String timeoutString) {
        switch (timeoutString) {
            case "5 minuti":
                setTimeoutMinutes(5);
                break;
            case "10 minuti":
                setTimeoutMinutes(10);
                break;
            case "15 minuti":
                setTimeoutMinutes(15);
                break;
            case "30 minuti":
                setTimeoutMinutes(30);
                break;
            case "Mai":
                setTimeoutMinutes(0); // Disable timeout
                break;
            default:
                setTimeoutMinutes(0); // Default fallback - disabled
                break;
        }
    }

    /**
     * Gets the current timeout as a string for UI display
     */
    public String getCurrentTimeoutString() {
        if (!enabled || timeoutMillis == 0) return "Mai";

        long minutes = timeoutMillis / AppConstants.Time.MILLIS_PER_MINUTE;
        return minutes + " minuti";
    }

    /**
     * Gets the current timeout in minutes
     */
    public int getCurrentTimeoutMinutes() {
        if (!enabled || timeoutMillis == 0) return 0;
        return (int) (timeoutMillis / AppConstants.Time.MILLIS_PER_MINUTE);
    }

    /**
     * Performs the timeout action (closes application)
     */
    private void performTimeoutAction() {
        // Prima invalida la sessione lato server
        String token = SessionManager.getInstance().getSessionToken();
        if (token != null && !token.isEmpty()) {
            try {
                ServiceLocator.getUserService().invalidateSession(token);
            } catch (Exception e) {
                // Ignora errori nell'invalidazione server
            }
        }

        // Logout user if logged in
        SessionManager.getInstance().logout();

        // Close all windows
        for (Window window : Window.getWindows()) {
            if (window instanceof Stage) {
                Platform.runLater(() -> ((Stage) window).close());
            }
        }

        // Force exit if needed
        Platform.runLater(() -> System.exit(0));
    }

    /**
     * Loads timeout setting from preferences
     */
    private void loadTimeoutSetting() {
        try {
            Preferences prefs = Preferences.userRoot().node(PREF_NODE);
            // Forza il default a 0 minuti (Mai) se non è stato mai impostato esplicitamente
            // Questo resetta eventuali valori automatici impostati in precedenza
            int savedMinutes = prefs.getInt(KEY_TIMEOUT_MINUTES, 0);
            // Se il valore salvato è 15 minuti (che era il default automatico precedente),
            // consideralo come "non impostato" e usa 0 minuti
            if (savedMinutes == 15) {
                savedMinutes = 0;
                // Salva il valore corretto
                prefs.putInt(KEY_TIMEOUT_MINUTES, 0);
                prefs.flush();
            }
            setTimeoutMinutes(savedMinutes);
        } catch (Exception e) {
            System.err.println("Failed to load timeout setting: " + e.getMessage());
            setTimeoutMinutes(0); // Default fallback - disabled
        }
    }

    /**
     * Saves timeout setting to preferences
     */
    private void saveTimeoutSetting(int minutes) {
        try {
            Preferences prefs = Preferences.userRoot().node(PREF_NODE);
            prefs.putInt(KEY_TIMEOUT_MINUTES, minutes);
            prefs.flush();
        } catch (Exception e) {
            System.err.println("Failed to save timeout setting: " + e.getMessage());
        }
    }

    /**
     * Checks if timeout is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }
}