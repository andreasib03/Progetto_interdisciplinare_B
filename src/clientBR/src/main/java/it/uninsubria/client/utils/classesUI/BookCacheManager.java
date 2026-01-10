package it.uninsubria.client.utils.classesUI;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

import it.uninsubria.shared.model.Book;

/**
 * Gestisce la cache locale persistente dei libri su disco.
 * Salva e carica i libri usando serializzazione Java per prestazioni ottimali.
 */
public class BookCacheManager {
    private static final Logger logger = Logger.getLogger(BookCacheManager.class.getName());
    private static final String CACHE_DIR = System.getProperty("user.home") + File.separator + ".bookrecommender";
    private static final String BOOKS_CACHE_FILE = "books_cache.dat";
    private static final String CACHE_VERSION_FILE = "cache_version.txt";
    private static final String CACHE_VERSION = "1.0";

    static {
        // Crea directory cache se non esiste
        try {
            Path cacheDir = Paths.get(CACHE_DIR);
            if (!Files.exists(cacheDir)) {
                Files.createDirectories(cacheDir);
                logger.info("Creata directory cache: " + CACHE_DIR);
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Errore nella creazione della directory cache", e);
        }

        // Registra cleanup nel ResourceManager
        ResourceManager.registerCleanupTask(BookCacheManager::cleanupCache);
    }

    /**
     * Salva la lista dei libri nella cache locale
     */
    public static void saveBooksToCache(List<Book> books) {
        if (books == null || books.isEmpty()) {
            logger.warning("Tentativo di salvare una lista libri vuota o nulla");
            return;
        }

        long startTime = System.currentTimeMillis();

        try {
            Path cacheFile = Paths.get(CACHE_DIR, BOOKS_CACHE_FILE);
            Path versionFile = Paths.get(CACHE_DIR, CACHE_VERSION_FILE);

            // Salva versione cache
            Files.writeString(versionFile, CACHE_VERSION);

            // Salva libri con serializzazione
            try (ObjectOutputStream oos = new ObjectOutputStream(
                    new BufferedOutputStream(Files.newOutputStream(cacheFile)))) {

                oos.writeObject(books);
                oos.flush();

                long endTime = System.currentTimeMillis();
                logger.info(String.format("Salvati %d libri in cache (%d ms)",
                    books.size(), (endTime - startTime)));
            }

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Errore nel salvataggio della cache libri", e);
        }
    }

    /**
     * Carica la lista dei libri dalla cache locale
     * @return lista libri o null se cache non valida/inesistente
     */
    @SuppressWarnings("unchecked")
    public static List<Book> loadBooksFromCache() {
        long startTime = System.currentTimeMillis();

        try {
            Path cacheFile = Paths.get(CACHE_DIR, BOOKS_CACHE_FILE);
            Path versionFile = Paths.get(CACHE_DIR, CACHE_VERSION_FILE);

            // Verifica che i file esistano
            if (!Files.exists(cacheFile) || !Files.exists(versionFile)) {
                logger.info("File cache non trovati, sarà necessario caricamento dal server");
                return null;
            }

            // Verifica versione cache
            String cachedVersion = Files.readString(versionFile).trim();
            if (!CACHE_VERSION.equals(cachedVersion)) {
                logger.info("Versione cache obsoleta, sarà necessario ricaricamento dal server");
                return null;
            }

            // Carica libri dalla cache
            try (ObjectInputStream ois = new ObjectInputStream(
                    new BufferedInputStream(Files.newInputStream(cacheFile)))) {

                List<Book> books = (List<Book>) ois.readObject();

                long endTime = System.currentTimeMillis();
                logger.info(String.format("Caricati %d libri dalla cache (%d ms)",
                    books.size(), (endTime - startTime)));

                return books;
            }

        } catch (IOException | ClassNotFoundException e) {
            logger.log(Level.WARNING, "Errore nel caricamento della cache libri, sarà necessario ricaricamento dal server", e);
            return null;
        }
    }

    /**
     * Verifica se la cache è valida e contiene dati
     */
    public static boolean isCacheValid() {
        try {
            Path cacheFile = Paths.get(CACHE_DIR, BOOKS_CACHE_FILE);
            Path versionFile = Paths.get(CACHE_DIR, CACHE_VERSION_FILE);

            if (!Files.exists(cacheFile) || !Files.exists(versionFile)) {
                return false;
            }

            String cachedVersion = Files.readString(versionFile).trim();
            return CACHE_VERSION.equals(cachedVersion) && Files.size(cacheFile) > 0;

        } catch (IOException e) {
            logger.log(Level.WARNING, "Errore nella verifica della cache", e);
            return false;
        }
    }

    /**
     * Invalida e pulisce la cache
     */
    public static void invalidateCache() {
        try {
            Path cacheFile = Paths.get(CACHE_DIR, BOOKS_CACHE_FILE);
            Path versionFile = Paths.get(CACHE_DIR, CACHE_VERSION_FILE);

            Files.deleteIfExists(cacheFile);
            Files.deleteIfExists(versionFile);

            logger.info("Cache libri invalidata e pulita");

        } catch (IOException e) {
            logger.log(Level.WARNING, "Errore nella pulizia della cache", e);
        }
    }

    /**
     * Ottiene informazioni sulla cache per debug
     */
    public static String getCacheInfo() {
        try {
            Path cacheFile = Paths.get(CACHE_DIR, BOOKS_CACHE_FILE);
            Path versionFile = Paths.get(CACHE_DIR, CACHE_VERSION_FILE);

            StringBuilder info = new StringBuilder();
            info.append("Cache Directory: ").append(CACHE_DIR).append("\n");
            info.append("Books Cache File: ").append(Files.exists(cacheFile)).append("\n");
            info.append("Version File: ").append(Files.exists(versionFile)).append("\n");
            info.append("Cache Valid: ").append(isCacheValid()).append("\n");

            if (Files.exists(cacheFile)) {
                info.append("Cache Size: ").append(Files.size(cacheFile)).append(" bytes\n");
            }

            return info.toString();

        } catch (IOException e) {
            return "Errore nell'ottenimento informazioni cache: " + e.getMessage();
        }
    }

    /**
     * Cleanup method chiamato dal ResourceManager all'uscita
     */
    private static void cleanupCache() {
        // La cache su disco non necessita di cleanup speciale,
        // ma possiamo loggare lo stato finale
        logger.info("Cache libri pronta per chiusura applicazione");
    }
}