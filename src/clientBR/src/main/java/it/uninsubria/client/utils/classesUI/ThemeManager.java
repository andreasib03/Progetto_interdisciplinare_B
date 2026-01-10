package it.uninsubria.client.utils.classesUI;

import javafx.scene.Scene;

import java.util.prefs.Preferences;

public class ThemeManager {

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(ThemeManager.class.getName());

    private static final String DARK_THEME = "/css/DarkMode.css";
    private static final String LIGHT_THEME = "/css/LightMode.css";
    private static final String PREF_KEY = "darkMode";

    private static boolean isDarkMode  = false;

    static {
        Preferences prefs = Preferences.userNodeForPackage(ThemeManager.class);
        if(prefs.get(PREF_KEY, null) == null) {
            isDarkMode = false;
            prefs.putBoolean(PREF_KEY, false);
        } else{
            isDarkMode = prefs.getBoolean(PREF_KEY, false);
        }
    }

    public static void applyTheme(Scene scene) {
        if(scene == null)
            return;
        logger.info("ThemeManager: Applying theme - DarkMode: " + isDarkMode);
        scene.getStylesheets().clear();
        String theme = isDarkMode ? DARK_THEME : LIGHT_THEME;
        String themePath = ThemeManager.class.getResource(theme).toExternalForm();
        logger.info("ThemeManager: Loading theme: " + themePath);

        // Check if resource exists
        if (ThemeManager.class.getResource(theme) == null) {
            logger.severe("ThemeManager: Theme resource not found: " + theme);
            return;
        }

        try {
            scene.getStylesheets().add(themePath);
            logger.info("ThemeManager: Theme loaded successfully");
        } catch (Exception e) {
            logger.severe("ThemeManager: Error loading theme: " + e.getMessage());
            e.printStackTrace();
        }

        // Force CSS reapplication to update all controls immediately
        // This is crucial for dynamic theme switching to work properly
        scene.getRoot().applyCss();

        // Also force layout to ensure all controls are updated
        scene.getRoot().requestLayout();

        // Force update of all MenuButton popups to ensure menu items get new styles
        updateMenuButtons(scene.getRoot());

        logger.info("ThemeManager: Theme applied successfully");
    }

    public static void setDarkMode(boolean enabled, Scene scene) {
        isDarkMode = enabled;
        Preferences prefs = Preferences.userNodeForPackage(ThemeManager.class);
        prefs.putBoolean(PREF_KEY, enabled);
        applyTheme(scene);
    }

    public static boolean isDarkModeEnabled() {
        return isDarkMode;
    }

    public static void reapplySavedTheme(Scene scene){
        applyTheme(scene);
    }

    /**
     * Force update of MenuButton popups and other controls to apply new theme styles
     */
    private static void updateMenuButtons(javafx.scene.Parent root) {
        logger.info("ThemeManager: Updating menu buttons...");

        // Find all MenuButton instances and force their popup updates
        root.lookupAll(".menu-button").forEach(node -> {
            if (node instanceof javafx.scene.control.MenuButton) {
                logger.info("ThemeManager: Found MenuButton, applying CSS");
                javafx.scene.control.MenuButton menuButton = (javafx.scene.control.MenuButton) node;
                // Apply CSS to the menu button itself
                menuButton.applyCss();

                // Force refresh of the menu button by temporarily hiding and showing
                boolean wasShowing = menuButton.isShowing();
                if (wasShowing) {
                    menuButton.hide();
                    javafx.application.Platform.runLater(() -> menuButton.show());
                }
            }
        });

        // Force CSS reapplication to all buttons that might not have updated
        root.lookupAll(".navBarHome").forEach(node -> {
            logger.info("ThemeManager: Found navBarHome button, applying CSS");
            node.applyCss();
        });

        // Force CSS reapplication to all labels that might not have updated
        root.lookupAll(".labelTitleHome").forEach(node -> {
            logger.info("ThemeManager: Found labelTitleHome, applying CSS");
            node.applyCss();
        });

        // Force CSS reapplication to labelBoldHome (selected labels color based on theme)
        root.lookupAll(".labelBoldHome").forEach(node -> {
            logger.info("ThemeManager: Found labelBoldHome, applying theme-appropriate color");
            node.applyCss();
        });

        // Force CSS reapplication to categories menu
        root.lookupAll(".categoriesMenu").forEach(node -> {
            logger.info("ThemeManager: Found categoriesMenu, applying CSS");
            node.applyCss();
        });

        // Force CSS reapplication to search menu items
        root.lookupAll(".searchMenuItem").forEach(node -> {
            logger.info("ThemeManager: Found searchMenuItem, applying CSS");
            node.applyCss();
        });

        // Force CSS reapplication to ALL labels (catch-all for labels that might not have specific classes)
        root.lookupAll("Label").forEach(node -> {
            if (node instanceof javafx.scene.control.Label) {
                node.applyCss();
            }
        });

        // Force CSS reapplication to ALL buttons (catch-all for buttons that might not have specific classes)
        root.lookupAll("Button").forEach(node -> {
            if (node instanceof javafx.scene.control.Button) {
                node.applyCss();
            }
        });
    }

    /**
     * Force refresh of specific UI components that might not update with theme changes
     */
    public static void forceRefreshComponents(Scene scene) {
        if (scene != null && scene.getRoot() != null) {
            logger.info("ThemeManager: Forcing refresh of UI components");
            updateMenuButtons(scene.getRoot());
            forceDirectColorUpdate(scene.getRoot());
            scene.getRoot().applyCss();
            scene.getRoot().requestLayout();
        }
    }

    /**
     * Force direct color update for problematic components
     */
    private static void forceDirectColorUpdate(javafx.scene.Parent root) {
        boolean isDarkMode = isDarkModeEnabled();
        logger.info("ThemeManager: Forcing direct color update for " + (isDarkMode ? "dark" : "light") + " mode");

        // Force navBarHome buttons
        root.lookupAll(".navBarHome").forEach(node -> {
            if (node instanceof javafx.scene.control.Button) {
                javafx.scene.control.Button button = (javafx.scene.control.Button) node;
                if (isDarkMode) {
                    button.setStyle("-fx-background-color: #1e1e2f; -fx-border-color: white; -fx-text-fill: white;");
                } else {
                    button.setStyle("-fx-background-color: white; -fx-border-color: #ddd; -fx-text-fill: black;");
                }
                logger.info("ThemeManager: Updated navBarHome button");
            }
        });

        // Force categoriesMenu (search filter)
        root.lookupAll(".categoriesMenu").forEach(node -> {
            if (node instanceof javafx.scene.control.MenuButton) {
                javafx.scene.control.MenuButton menuButton = (javafx.scene.control.MenuButton) node;
                if (isDarkMode) {
                    menuButton.setStyle("-fx-background-color: transparent; -fx-text-fill: white;");
                } else {
                    menuButton.setStyle("-fx-background-color: transparent; -fx-text-fill: black;");
                }
                logger.info("ThemeManager: Updated categoriesMenu");
            }
        });

        // Force labelTitleHome labels
        root.lookupAll(".labelTitleHome").forEach(node -> {
            if (node instanceof javafx.scene.control.Label) {
                javafx.scene.control.Label label = (javafx.scene.control.Label) node;
                if (isDarkMode) {
                    label.setStyle("-fx-text-fill: white;");
                } else {
                    label.setStyle("-fx-text-fill: black;");
                }
                logger.info("ThemeManager: Updated labelTitleHome");
            }
        });

        // Ensure labelBoldHome labels have appropriate color for selected state
        root.lookupAll(".labelBoldHome").forEach(node -> {
            if (node instanceof javafx.scene.control.Label) {
                javafx.scene.control.Label label = (javafx.scene.control.Label) node;
                // Selected labels: white in dark mode, black in light mode (for contrast on blue background)
                if (isDarkMode) {
                    label.setStyle("-fx-text-fill: white;");
                } else {
                    label.setStyle("-fx-text-fill: black;");
                }
                logger.info("ThemeManager: Updated labelBoldHome color for theme");
            }
        });
    }

    public static void applyIfAbsent(Scene scene, String cssPath) {
        String fullPath = ThemeManager.class.getResource(cssPath).toExternalForm();
        if (!scene.getStylesheets().contains(fullPath)) {
            scene.getStylesheets().add(fullPath);
        }
    }
}
