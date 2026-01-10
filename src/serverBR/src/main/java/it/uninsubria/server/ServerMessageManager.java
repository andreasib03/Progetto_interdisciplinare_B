package it.uninsubria.server;

import it.uninsubria.shared.utils.AppConfig;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.logging.Level;

public class ServerMessageManager {
    private static final Logger logger = Logger.getLogger(ServerMessageManager.class.getName());
    private static ResourceBundle bundle = loadBundle(AppConfig.getLanguage());
    private static String currentLanguage = AppConfig.getLanguage();
    private static ScheduledExecutorService languageWatcher;

    static {
        // Start language change watcher
        startLanguageWatcher();

        // Force immediate reload to get correct language at startup
        reloadLanguage();
    }

    private static ResourceBundle loadBundle(String langCode) {
        Locale locale = Locale.forLanguageTag(langCode);
        return ResourceBundle.getBundle("properties.ServerMessages", locale);
    }

    private static void startLanguageWatcher() {
        languageWatcher = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ServerLanguageWatcher");
            t.setDaemon(true);
            return t;
        });

        // Check for language changes every 2 seconds
        languageWatcher.scheduleAtFixedRate(() -> {
            try {
                AppConfig.reloadConfiguration();
                String newLanguage = AppConfig.getLanguage();
                if (!newLanguage.equals(currentLanguage)) {
                    logger.info("Server language changed from " + currentLanguage + " to " + newLanguage);
                    currentLanguage = newLanguage;
                    bundle = loadBundle(newLanguage);
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error checking for server language changes", e);
            }
        }, 2, 2, TimeUnit.SECONDS);
    }

    public static void setLanguage(String langCode) {
        AppConfig.setLanguage(langCode);
        currentLanguage = langCode;
        bundle = loadBundle(langCode);
    }

    public static String getLanguage() {
        return currentLanguage;
    }

    public static ResourceBundle getBundle() {
        return bundle;
    }

    public static String getString(String key) {
        try {
            return bundle.getString(key);
        } catch (Exception e) {
            // Fallback to key if translation not found
            return key;
        }
    }

    public static String getString(String key, Object... args) {
        try {
            String message = bundle.getString(key);
            return String.format(message, args);
        } catch (Exception e) {
            // Fallback to key if translation not found
            return key;
        }
    }

    /**
     * Force reload language from configuration
     */
    public static void reloadLanguage() {
        AppConfig.reloadConfiguration();
        String newLanguage = AppConfig.getLanguage();
        if (!newLanguage.equals(currentLanguage)) {
            setLanguage(newLanguage);
        }
    }

    /**
     * Stop the language watcher (for shutdown)
     */
    public static void shutdown() {
        if (languageWatcher != null && !languageWatcher.isShutdown()) {
            languageWatcher.shutdown();
            try {
                if (!languageWatcher.awaitTermination(1, TimeUnit.SECONDS)) {
                    languageWatcher.shutdownNow();
                }
            } catch (InterruptedException e) {
                languageWatcher.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}