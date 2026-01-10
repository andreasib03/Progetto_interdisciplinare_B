package it.uninsubria.client.utils.classesUI;

import javafx.scene.image.Image;
import java.util.HashMap;
import java.util.Map;

/**
 * Cache per risorse frequentemente utilizzate per evitare caricamenti duplicati
 */
public class ResourceCache {
    private static final Map<String, Image> imageCache = new HashMap<>();

    /**
     * Ottiene un'immagine dalla cache o la carica se non presente
     */
    public static Image getImage(String resourcePath) {
        return imageCache.computeIfAbsent(resourcePath, path -> {
            try {
                return new Image(ResourceCache.class.getResourceAsStream(path));
            } catch (Exception e) {
                System.err.println("Errore caricamento immagine: " + path + " - " + e.getMessage());
                return null;
            }
        });
    }

    /**
     * Precarica risorse comuni all'avvio dell'applicazione
     */
    public static void preloadCommonResources() {
        // Precarica l'immagine placeholder più utilizzata
        getImage("/images/placeholder_image.png");
        getImage("/images/logo.png");
    }

    /**
     * Svuota la cache delle immagini
     */
    public static void clearImageCache() {
        imageCache.clear();
    }
}