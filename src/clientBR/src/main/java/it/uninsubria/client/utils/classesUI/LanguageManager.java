package it.uninsubria.client.utils.classesUI;

import it.uninsubria.shared.utils.AppConfig;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;

/**
 * Language manager for handling internationalization and language switching
 */
public class LanguageManager {

    /**
     * Interface for components that want to be notified of language changes
     */
    /**
     * Listener interface for language change events.
     */
    public interface LanguageChangeListener {
        /**
         * Called when the language changes.
         *
         * @param newLanguage the new language code
         */
        void onLanguageChanged(String newLanguage);
    }

    // List of registered listeners
    private static final List<LanguageChangeListener> listeners = new CopyOnWriteArrayList<>();
    private static final Logger logger = Logger.getLogger(LanguageManager.class.getName());
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
        return ResourceBundle.getBundle("properties.Messages", locale);
    }

    private static void startLanguageWatcher() {
        languageWatcher = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "LanguageWatcher");
            t.setDaemon(true);
            return t;
        });

        // Check for language changes every 2 seconds
        languageWatcher.scheduleAtFixedRate(() -> {
            try {
                String newLanguage = AppConfig.getLanguage();
                if (!newLanguage.equals(currentLanguage)) {
                    logger.info("Language changed from " + currentLanguage + " to " + newLanguage);
                    currentLanguage = newLanguage;
                    bundle = loadBundle(newLanguage);
                    // Notify UI components about language change
                    notifyLanguageChange(newLanguage);
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error checking for language changes", e);
            }
        }, 2, 2, TimeUnit.SECONDS);
    }

    /**
     * Register a listener to be notified when language changes
     */
    public static void addLanguageChangeListener(LanguageChangeListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Remove a language change listener
     */
    public static void removeLanguageChangeListener(LanguageChangeListener listener) {
        listeners.remove(listener);
    }

    private static void notifyLanguageChange(String newLanguage) {
        // Notify all registered listeners
        javafx.application.Platform.runLater(() -> {
            try {
                logger.info("Language changed to: " + newLanguage + " - notifying " + listeners.size() + " listeners");

                for (LanguageChangeListener listener : listeners) {
                    try {
                        listener.onLanguageChanged(newLanguage);
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "Error notifying language change listener", e);
                    }
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error notifying language change", e);
            }
        });
    }

    public static void setLanguage(String langCode) {
        AppConfig.setLanguage(langCode);
        currentLanguage = langCode;
        bundle = loadBundle(langCode);
        notifyLanguageChange(langCode);
    }

    public static String getLanguage() {
        return currentLanguage;
    }

    public static ResourceBundle getBundle() {
        return bundle;
    }

    /**
     * Get localized string for given key
     */
    public static String getString(String key) {
        try {
            return bundle.getString(key);
        } catch (java.util.MissingResourceException e) {
            logger.warning("Missing resource key: " + key);
            return key;
        }
    }

    /**
     * Get localized string for given key with parameters
     */
    public static String getString(String key, Object... args) {
        try {
            return java.text.MessageFormat.format(bundle.getString(key), args);
        } catch (java.util.MissingResourceException e) {
            logger.warning("Missing resource key: " + key);
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
