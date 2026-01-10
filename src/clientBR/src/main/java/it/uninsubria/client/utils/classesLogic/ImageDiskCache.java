package it.uninsubria.client.utils.classesLogic;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;
import java.util.logging.Level;


/**
 * Cache persistente su disco per URL di immagini di copertina libri
 * Evita riscaricamenti ripetuti da internet salvando gli URL
 */
public class ImageDiskCache {

    private static final Logger logger = Logger.getLogger(ImageDiskCache.class.getName());

    private static final String IMAGE_CACHE_DIR =
        System.getProperty("user.home") + File.separator + ".bookrecommender" + File.separator + "images" + File.separator;

    private static final String CACHE_VERSION_FILE = "cache_version.txt";
    private static final String CACHE_VERSION = "1.0";

    static {
        // Inizializza directory cache
        try {
            Path cacheDir = Paths.get(IMAGE_CACHE_DIR);
            if (!Files.exists(cacheDir)) {
                Files.createDirectories(cacheDir);
                logger.info("Creata directory cache immagini: " + IMAGE_CACHE_DIR);
            }

            // Inizializza file versione cache
            Path versionFile = Paths.get(IMAGE_CACHE_DIR, CACHE_VERSION_FILE);
            if (!Files.exists(versionFile)) {
                Files.writeString(versionFile, CACHE_VERSION);
            }

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Errore inizializzazione cache immagini disco", e);
        }
    }


    /**
     * Salva l'URL di un'immagine su disco invece dell'immagine stessa
     * Questo approccio è più semplice e non richiede conversioni Swing/JavaFX
     */
    public static void saveImageUrlToDisk(String bookId, String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty() || bookId == null || bookId.trim().isEmpty()) {
            return;
        }

        String fileName = bookId.replaceAll("[^a-zA-Z0-9]", "_") + ".url";
        Path filePath = Paths.get(IMAGE_CACHE_DIR, fileName);

        try {
            // Salva semplicemente l'URL come testo
            Files.writeString(filePath, imageUrl.trim());
            logger.fine("URL immagine salvato su disco: " + bookId);

        } catch (IOException e) {
            logger.log(Level.WARNING, "Errore salvataggio URL immagine su disco: " + bookId, e);
        }
    }

    /**
     * Carica l'URL di un'immagine dalla cache disco
     */
    public static String loadImageUrlFromDisk(String bookId) {
        if (bookId == null || bookId.trim().isEmpty()) {
            return null;
        }

        String fileName = bookId.replaceAll("[^a-zA-Z0-9]", "_") + ".url";
        Path filePath = Paths.get(IMAGE_CACHE_DIR, fileName);

        try {
            if (Files.exists(filePath) && Files.size(filePath) > 0) {
                // Verifica che il file non sia troppo vecchio (30 giorni)
                long fileAge = System.currentTimeMillis() - Files.getLastModifiedTime(filePath).toMillis();
                long maxAge = 30L * 24 * 60 * 60 * 1000; // 30 giorni

                if (fileAge > maxAge) {
                    // File troppo vecchio, eliminalo
                    Files.delete(filePath);
                    logger.fine("URL immagine cache scaduto eliminato: " + bookId);
                    return null;
                }

                String imageUrl = Files.readString(filePath).trim();

                if (imageUrl.isEmpty()) {
                    // URL vuoto, elimina file
                    Files.delete(filePath);
                    logger.warning("URL immagine cache vuoto eliminato: " + bookId);
                    return null;
                }

                logger.fine("URL immagine caricato da cache disco: " + bookId);
                return imageUrl;
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Errore caricamento URL immagine da cache disco: " + bookId, e);
            // In caso di errore, prova a eliminare il file corrotto
            try {
                Files.deleteIfExists(filePath);
            } catch (IOException ignored) {}
        }

        return null;
    }

    /**
     * Verifica se l'URL di un'immagine è presente nella cache disco
     */
    public static boolean hasImageUrlInCache(String bookId) {
        if (bookId == null || bookId.trim().isEmpty()) {
            return false;
        }

        String fileName = bookId.replaceAll("[^a-zA-Z0-9]", "_") + ".url";
        Path filePath = Paths.get(IMAGE_CACHE_DIR, fileName);

        try {
            return Files.exists(filePath) && Files.size(filePath) > 0;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Ottiene statistiche della cache
     */
    public static CacheStats getCacheStats() {
        try {
            Path cacheDir = Paths.get(IMAGE_CACHE_DIR);
            if (!Files.exists(cacheDir)) {
                return new CacheStats(0, 0);
            }

            long fileCount = Files.walk(cacheDir)
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".url"))
                .count();

            long totalSize = Files.walk(cacheDir)
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".url"))
                .mapToLong(p -> {
                    try {
                        return Files.size(p);
                    } catch (IOException e) {
                        return 0L;
                    }
                })
                .sum();

            return new CacheStats(fileCount, totalSize);

        } catch (IOException e) {
            logger.log(Level.WARNING, "Errore ottenimento statistiche cache", e);
            return new CacheStats(0, 0);
        }
    }

    /**
     * Pulisce la cache rimuovendo URL immagini vecchi
     */
    public static void cleanupOldImageUrls() {
        try {
            Path cacheDir = Paths.get(IMAGE_CACHE_DIR);
            if (!Files.exists(cacheDir)) {
                return;
            }

            long maxAge = 30L * 24 * 60 * 60 * 1000; // 30 giorni
            long now = System.currentTimeMillis();

            long deletedCount = Files.walk(cacheDir)
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".url"))
                .filter(p -> {
                    try {
                        long fileAge = now - Files.getLastModifiedTime(p).toMillis();
                        return fileAge > maxAge;
                    } catch (IOException e) {
                        return false;
                    }
                })
                .mapToLong(p -> {
                    try {
                        Files.delete(p);
                        return 1;
                    } catch (IOException e) {
                        return 0;
                    }
                })
                .sum();

            if (deletedCount > 0) {
                logger.info("Puliti " + deletedCount + " URL immagini vecchi dalla cache");
            }

        } catch (IOException e) {
            logger.log(Level.WARNING, "Errore pulizia cache URL immagini", e);
        }
    }

    /**
     * Classe per statistiche cache
     */
    public static class CacheStats {
        public final long fileCount;
        public final long totalSizeBytes;

        public CacheStats(long fileCount, long totalSizeBytes) {
            this.fileCount = fileCount;
            this.totalSizeBytes = totalSizeBytes;
        }

        @Override
        public String toString() {
            double sizeMB = totalSizeBytes / (1024.0 * 1024.0);
            return String.format("Cache immagini: %d file (%.2f MB)", fileCount, sizeMB);
        }
    }
}