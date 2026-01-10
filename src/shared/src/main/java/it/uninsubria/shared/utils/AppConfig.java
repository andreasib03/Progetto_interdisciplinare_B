package it.uninsubria.shared.utils;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;


/**
 * * Classe di utilità per la gestione della configurazione dell'applicazione.
 * * Carica le impostazioni da un file esterno "config.properties" se presente,
 *
 * @author Team BookRecommender
 * @version 1.0
 */

public class AppConfig {
    private static final Logger logger = Logger.getLogger(AppConfig.class.getName());
    private static String serverHost = "localhost";
    private static String dbName = "postgres";
    private static String dbUser = "postgres";
    private static String dbPassword = "";
    private static int poolSize = 10;
    private static String jwtSecret = "default";
    private static String language = "it"; // default language

    static {
        // Try to load external config from config.properties
        // First check project directory, then classpath
        Properties p = new Properties();

        // Try project directory first
        try {
            java.io.File configFile = new java.io.File("config.properties");
            if (configFile.exists() && configFile.length() > 0) {
                try (java.io.FileInputStream in = new java.io.FileInputStream(configFile)) {
                    p.load(in);
                } catch (Exception e) {
                    logger.warning("Failed to load config.properties from project directory: " + e.getMessage());
                    // Continue with classpath loading
                }
            }
        } catch (Exception e) {
            logger.warning("Error accessing config.properties in project directory: " + e.getMessage());
            // Continue with classpath loading
        }

        // Then try classpath as fallback
        try (InputStream in = AppConfig.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (in != null) {
                // If we already loaded from file, merge with classpath properties
                if (!p.isEmpty()) {
                    Properties classpathProps = new Properties();
                    classpathProps.load(in);
                    // File properties take precedence over classpath
                    classpathProps.putAll(p);
                    p = classpathProps;
                } else {
                    p.load(in);
                }
            }
        } catch (Exception ignored) {
        }

        // Apply loaded properties
        if (p.getProperty("serverHost") != null) serverHost = p.getProperty("serverHost");
        if (p.getProperty("dbName") != null) dbName = p.getProperty("dbName");
        if (p.getProperty("dbUser") != null) dbUser = p.getProperty("dbUser");
        if (p.getProperty("dbPassword") != null) dbPassword = p.getProperty("dbPassword");
        if (p.getProperty("poolSize") != null) poolSize = Integer.parseInt(p.getProperty("poolSize"));
        if (p.getProperty("jwtSecret") != null) jwtSecret = p.getProperty("jwtSecret");
        if (p.getProperty("language") != null) language = p.getProperty("language");
        // Environment overrides for dev/prod/testing
        String eHost = System.getenv("APP_SERVER_HOST");
        if (eHost != null && !eHost.isEmpty()) serverHost = eHost;
        String eDbName = System.getenv("APP_DB_NAME");
        if (eDbName != null && !eDbName.isEmpty()) dbName = eDbName;
        String eDbUser = System.getenv("APP_DB_USER");
        if (eDbUser != null && !eDbUser.isEmpty()) dbUser = eDbUser;
        String eDbPassword = System.getenv("APP_DB_PASSWORD");
        if (eDbPassword != null) dbPassword = eDbPassword;
        String ePool = System.getenv("APP_POOL_SIZE");
        if (ePool != null && !ePool.isEmpty()) poolSize = Integer.parseInt(ePool);
        String eJwt = System.getenv("APP_JWT_SECRET");
        if (eJwt != null && !eJwt.isEmpty()) jwtSecret = eJwt;
        String eLang = System.getenv("APP_LANGUAGE");
        if (eLang != null && !eLang.isEmpty()) language = eLang;
    }

    /**
     * Sets the server host address.
     *
     * @param host the server host address
     */
    public static void setServerHost(String host) {
        serverHost = host;
    }

    /**
     * Gets the server host address.
     *
     * @return the server host address
     */
    public static String getServerHost() {
        return serverHost;
    }

    /**
     * Gets the database name.
     *
     * @return the database name
     */
    public static String getDbName() {
        return dbName;
    }

    /**
     * Gets the database user.
     *
     * @return the database user
     */
    public static String getDbUser() {
        return dbUser;
    }

    /**
     * Gets the database password.
     *
     * @return the database password
     */
    public static String getDbPassword() {
        return dbPassword;
    }

    /**
     * Gets the connection pool size.
     *
     * @return the connection pool size
     */
    public static int getPoolSize() {
        return poolSize;
    }

    /**
     * Gets the JWT secret key.
     *
     * @return the JWT secret key
     */
    public static String getJwtSecret() {
        return jwtSecret;
    }

    /**
     * Sets the application language.
     *
     * @param lang the language code
     */
    public static void setLanguage(String lang) {
        language = lang;
    }

    /**
     * Gets the application language.
     *
     * @return the language code
     */
    public static String getLanguage() {
        return language;
    }

    /**
     * Reloads configuration from external config.properties file
     * This allows dynamic configuration updates without restarting
     */
    public static void reloadConfiguration() {
        Properties p = new Properties();
        boolean loadedFromFile = false;

        // Try project directory first
        try {
            java.io.File configFile = new java.io.File("config.properties");
            if (configFile.exists() && configFile.length() > 0) {
                try (java.io.FileInputStream in = new java.io.FileInputStream(configFile)) {
                    p.load(in);
                    loadedFromFile = true;
                    // Only log successful loads to avoid spam - very rarely
                } catch (Exception e) {
                    // Log error but don't spam console
                    if (System.currentTimeMillis() % 10000 < 100) { // Log only once every ~10 seconds
                        logger.fine("Failed to load config from project directory: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            // Log error but don't spam console
            if (System.currentTimeMillis() % 10000 < 100) { // Log only once every ~10 seconds
                logger.fine("Error accessing config file: " + e.getMessage());
            }
        }
            

        // Apply reloaded properties only if we successfully loaded something
        if (loadedFromFile) {
            if (p.getProperty("language") != null) {
                String newLanguage = p.getProperty("language");
                if (!newLanguage.equals(language)) {
                    language = newLanguage;
                    // Only log actual language changes, not every reload
                    logger.info("Language changed to: " + language);
                }
            }
            // Reload other dynamic properties as needed
            if (p.getProperty("serverHost") != null) serverHost = p.getProperty("serverHost");
            if (p.getProperty("poolSize") != null) poolSize = Integer.parseInt(p.getProperty("poolSize"));
        }
    }
}
