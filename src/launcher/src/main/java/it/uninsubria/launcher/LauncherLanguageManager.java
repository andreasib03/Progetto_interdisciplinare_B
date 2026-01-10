package it.uninsubria.launcher;

import it.uninsubria.shared.utils.AppConfig;
import java.util.Locale;
import java.util.ResourceBundle;

public class LauncherLanguageManager {
    private static ResourceBundle bundle = loadBundle(AppConfig.getLanguage());
    private static String currentLanguage = getCurrentLanguage();


    // No language watcher needed for launcher - it controls language changes directly

    private static String getCurrentLanguage() {
        String lang = AppConfig.getLanguage();
        return (lang != null && !lang.trim().isEmpty()) ? lang : "it"; // default to Italian
    }

    private static ResourceBundle loadBundle(String langCode) {
        try {
            Locale locale = Locale.forLanguageTag(langCode);
            return ResourceBundle.getBundle("properties.Messages", locale);
        } catch (Exception e) {
            // Fallback to default if bundle not found
            Locale defaultLocale = Locale.forLanguageTag("it");
            return ResourceBundle.getBundle("properties.Messages", defaultLocale);
        }
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
            if (args.length > 0) {
                return String.format(message, args);
            } else {
                return message;
            }
        } catch (Exception e) {
            // Fallback to key if translation not found
            return key + (args.length > 0 ? " (with " + args.length + " args)" : "");
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
}