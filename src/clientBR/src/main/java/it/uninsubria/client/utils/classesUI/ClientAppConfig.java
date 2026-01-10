package it.uninsubria.client.utils.classesUI;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Gestore configurazione dinamica dell'applicazione.
 * Carica impostazioni da file properties con valori di default.
 */
public class ClientAppConfig {
    private static final Logger logger = Logger.getLogger(ClientAppConfig.class.getName());
    private static final Properties properties = new Properties();

    // File di configurazione
    private static final String CONFIG_FILE = "/app.properties";

    // Chiavi di configurazione
    public static final String KEY_THREAD_POOL_SIZE = "app.thread.pool.size";
    public static final String KEY_IO_THREAD_POOL_SIZE = "app.thread.pool.io.size";
    public static final String KEY_CPU_THREAD_POOL_SIZE = "app.thread.pool.cpu.size";
    public static final String KEY_CACHE_TIMEOUT_MS = "app.cache.timeout.ms";
    public static final String KEY_BOOK_LOAD_TIMEOUT_SEC = "app.book.load.timeout.sec";
    public static final String KEY_IMAGE_LOAD_TIMEOUT_SEC = "app.image.load.timeout.sec";
    public static final String KEY_MAX_BOOKS_STARTUP = "app.books.max.startup";
    public static final String KEY_RETRY_ATTEMPTS = "app.retry.attempts";
    public static final String KEY_RETRY_DELAY_MS = "app.retry.delay.ms";

    // Valori di default
    private static final int DEFAULT_THREAD_POOL_SIZE = 10;
    private static final long DEFAULT_CACHE_TIMEOUT_MS = 120_000; // 2 minuti
    private static final int DEFAULT_BOOK_LOAD_TIMEOUT_SEC = 60;
    private static final int DEFAULT_IMAGE_LOAD_TIMEOUT_SEC = 25;
    private static final int DEFAULT_MAX_BOOKS_STARTUP = 1000;
    private static final int DEFAULT_RETRY_ATTEMPTS = 3;
    private static final long DEFAULT_RETRY_DELAY_MS = 1000;

    static {
        loadConfiguration();
    }

    /**
     * Carica la configurazione dal file properties
     */
    private static void loadConfiguration() {
        try (InputStream input = ClientAppConfig.class.getResourceAsStream(CONFIG_FILE)) {
            if (input != null) {
                properties.load(input);
                logger.info("Configurazione caricata da " + CONFIG_FILE);
            } else {
                logger.info("File configurazione non trovato, uso valori di default");
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Errore nel caricamento della configurazione, uso valori di default", e);
        }

        // Log configurazione caricata
        logConfiguration();
    }

    /**
     * Registra la configurazione corrente
     */
    private static void logConfiguration() {
        logger.info("=== CONFIGURAZIONE APPLICAZIONE ===");
        logger.info(KEY_THREAD_POOL_SIZE + " = " + getThreadPoolSize());
        logger.info(KEY_CACHE_TIMEOUT_MS + " = " + getCacheTimeoutMs());
        logger.info(KEY_BOOK_LOAD_TIMEOUT_SEC + " = " + getBookLoadTimeoutSec());
        logger.info(KEY_IMAGE_LOAD_TIMEOUT_SEC + " = " + getImageLoadTimeoutSec());
        logger.info(KEY_MAX_BOOKS_STARTUP + " = " + getMaxBooksStartup());
        logger.info(KEY_RETRY_ATTEMPTS + " = " + getRetryAttempts());
        logger.info(KEY_RETRY_DELAY_MS + " = " + getRetryDelayMs());
        logger.info("===================================");
    }

    // Getters per i valori di configurazione

    public static int getThreadPoolSize() {
        return getIntProperty(KEY_THREAD_POOL_SIZE, DEFAULT_THREAD_POOL_SIZE);
    }

    public static int getIOThreadPoolSize() {
        return getIntProperty(KEY_IO_THREAD_POOL_SIZE, Math.max(2, Runtime.getRuntime().availableProcessors()));
    }

    public static int getCPUThreadPoolSize() {
        return getIntProperty(KEY_CPU_THREAD_POOL_SIZE, Math.max(1, Runtime.getRuntime().availableProcessors() / 2));
    }

    public static long getCacheTimeoutMs() {
        return getLongProperty(KEY_CACHE_TIMEOUT_MS, DEFAULT_CACHE_TIMEOUT_MS);
    }

    public static int getBookLoadTimeoutSec() {
        return getIntProperty(KEY_BOOK_LOAD_TIMEOUT_SEC, DEFAULT_BOOK_LOAD_TIMEOUT_SEC);
    }

    public static int getImageLoadTimeoutSec() {
        return getIntProperty(KEY_IMAGE_LOAD_TIMEOUT_SEC, DEFAULT_IMAGE_LOAD_TIMEOUT_SEC);
    }

    public static int getMaxBooksStartup() {
        return getIntProperty(KEY_MAX_BOOKS_STARTUP, DEFAULT_MAX_BOOKS_STARTUP);
    }

    public static int getRetryAttempts() {
        return getIntProperty(KEY_RETRY_ATTEMPTS, DEFAULT_RETRY_ATTEMPTS);
    }

    public static long getRetryDelayMs() {
        return getLongProperty(KEY_RETRY_DELAY_MS, DEFAULT_RETRY_DELAY_MS);
    }

    // Metodi helper per leggere proprietà

    private static int getIntProperty(String key, int defaultValue) {
        try {
            String value = properties.getProperty(key);
            return value != null ? Integer.parseInt(value.trim()) : defaultValue;
        } catch (NumberFormatException e) {
            logger.warning("Valore non valido per " + key + ", uso default: " + defaultValue);
            return defaultValue;
        }
    }

    private static long getLongProperty(String key, long defaultValue) {
        try {
            String value = properties.getProperty(key);
            return value != null ? Long.parseLong(value.trim()) : defaultValue;
        } catch (NumberFormatException e) {
            logger.warning("Valore non valido per " + key + ", uso default: " + defaultValue);
            return defaultValue;
        }
    }

    /**
     * Ricarica la configurazione (utile per testing o ricarica runtime)
     */
    public static void reloadConfiguration() {
        properties.clear();
        loadConfiguration();
    }

    /**
     * Ottiene una rappresentazione stringa della configurazione
     */
    public static String getConfigurationInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("Thread Pool Size: ").append(getThreadPoolSize()).append("\n");
        sb.append("Cache Timeout: ").append(getCacheTimeoutMs()).append("ms\n");
        sb.append("Book Load Timeout: ").append(getBookLoadTimeoutSec()).append("s\n");
        sb.append("Image Load Timeout: ").append(getImageLoadTimeoutSec()).append("s\n");
        sb.append("Max Books Startup: ").append(getMaxBooksStartup()).append("\n");
        sb.append("Retry Attempts: ").append(getRetryAttempts()).append("\n");
        sb.append("Retry Delay: ").append(getRetryDelayMs()).append("ms\n");
        return sb.toString();
    }
}