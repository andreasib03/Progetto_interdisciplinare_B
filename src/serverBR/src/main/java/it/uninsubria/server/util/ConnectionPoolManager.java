package it.uninsubria.server.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import it.uninsubria.shared.utils.AppConstants;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * Connection Pool Manager using HikariCP for optimal database connection management.
 * Provides high-performance connection pooling with proper resource management.
 *
 * <p>REQUIREMENTS:
 * Database credentials MUST be set via environment variables:
 * <ul>
 *   <li>DB_URL - JDBC connection URL (e.g., jdbc:postgresql://localhost:5432/database)</li>
 *   <li>DB_USERNAME - Database username</li>
 *   <li>DB_PASSWORD - Database password</li>
 * </ul>
 * Set them with:
 * <pre>
 * export DB_URL=jdbc:postgresql://localhost:5432/database
 * export DB_USERNAME=your_username
 * export DB_PASSWORD=your_password
 * </pre>
 */
public class ConnectionPoolManager {

    private static final Logger logger = Logger.getLogger(ConnectionPoolManager.class.getName());
    private static HikariDataSource dataSource;

    /**
     * Initializes the connection pool with environment variables.
     *
     * @throws IllegalStateException if required environment variables are not set
     */
    public static synchronized void initialize() {
        String url = System.getenv("DB_URL");
        String username = System.getenv("DB_USERNAME");
        String password = System.getenv("DB_PASSWORD");

        if (url == null || url.trim().isEmpty()) {
            throw new IllegalStateException(
                "DB_URL environment variable is not set. " +
                "Please set it with: export DB_URL=jdbc:postgresql://localhost:5432/database"
            );
        }
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalStateException(
                "DB_USERNAME environment variable is not set. " +
                "Please set it with: export DB_USERNAME=your_database_username"
            );
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalStateException(
                "DB_PASSWORD environment variable is not set. " +
                "Please set it with: export DB_PASSWORD=your_database_password"
            );
        }

        initialize(url, username, password, "org.postgresql.Driver");
    }
    
    /**
     * Initializes the connection pool with custom parameters.
     *
     * @param url the database URL
     * @param username the database username
     * @param password the database password
     */
    public static synchronized void initialize(String url, String username, String password) {
        initialize(url, username, password, "org.postgresql.Driver");
    }

    /**
     * Initializes the connection pool with custom parameters and driver.
     *
     * @param url the database URL
     * @param username the database username
     * @param password the database password
     * @param driverClassName the JDBC driver class name
     */
    public static synchronized void initialize(String url, String username, String password, String driverClassName) {
        if (dataSource != null && !dataSource.isClosed()) {
            logger.warning("Connection pool already initialized");
            return;
        }

        try {
            HikariConfig config = new HikariConfig();

            // Basic connection settings
            config.setJdbcUrl(url);
            config.setUsername(username);
            config.setPassword(password);
            config.setDriverClassName(driverClassName);

            // Read pool configuration from environment or use defaults from AppConstants
            int maxPoolSize = getIntEnvVar("DB_POOL_MAX_SIZE", AppConstants.Database.CONNECTION_POOL_MAX_SIZE);
            int minIdle = getIntEnvVar("DB_POOL_MIN_IDLE", AppConstants.Database.CONNECTION_POOL_MIN_IDLE);
            int connectionTimeout = getIntEnvVar("DB_CONNECTION_TIMEOUT", AppConstants.Database.CONNECTION_TIMEOUT_MS);
            int maxLifetime = getIntEnvVar("DB_MAX_LIFETIME", AppConstants.Database.MAX_LIFETIME_MS);
            int validationTimeout = getIntEnvVar("DB_VALIDATION_TIMEOUT", AppConstants.Database.VALIDATION_TIMEOUT_MS);

            logger.info("Using pool configuration from environment or defaults - Max: " + maxPoolSize + ", Min idle: " + minIdle);

            // Optimized pool configuration for high-performance
            config.setMaximumPoolSize(maxPoolSize);
            config.setMinimumIdle(minIdle);
            config.setConnectionTimeout(connectionTimeout);
            config.setIdleTimeout(AppConstants.Database.IDLE_TIMEOUT_MS);
            config.setMaxLifetime(maxLifetime);
            config.setKeepaliveTime(AppConstants.Database.KEEPALIVE_TIME_MS);
            config.setValidationTimeout(validationTimeout);

            // Enhanced leak detection and monitoring
            config.setLeakDetectionThreshold(AppConstants.Database.LEAK_DETECTION_THRESHOLD_MS);   // Detect leaks after 1 minute
            config.setConnectionTestQuery("SELECT 1");  // Simple validation query

            // Pool naming for monitoring
            config.setPoolName("BookRecommenderPool");

            // Advanced performance settings for PostgreSQL
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "500");        // Increased cache
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "4096");   // Larger SQL cache
            config.addDataSourceProperty("useServerPrepStmts", "true");
            config.addDataSourceProperty("rewriteBatchedStatements", "true");// Batch optimization
            config.addDataSourceProperty("cacheResultSetMetadata", "true");
            config.addDataSourceProperty("cacheServerConfiguration", "true");
            config.addDataSourceProperty("elideSetAutoCommits", "true");
            config.addDataSourceProperty("maintainTimeStats", "false");      // Reduce overhead
            
            dataSource = new HikariDataSource(config);

            logger.info("Connection pool initialized successfully");
            logger.info("Pool settings - Max size: " + config.getMaximumPoolSize() +
                       ", Min idle: " + config.getMinimumIdle() +
                       ", Connection timeout: " + config.getConnectionTimeout() + "ms" +
                       ", Max lifetime: " + config.getMaxLifetime() + "ms");
            
        } catch (Exception e) {
            logger.severe("Failed to initialize connection pool: " + e.getMessage());
            throw new RuntimeException("Connection pool initialization failed", e);
        }
    }
    
    /**
     * Gets a connection from the pool.
     * 
     * @return a database connection
     * @throws SQLException if unable to get a connection
     */
    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("Connection pool not initialized. Call initialize() first.");
        }
        
        Connection conn = dataSource.getConnection();
        logger.fine("Connection retrieved from pool. Active connections: " + 
                    dataSource.getHikariPoolMXBean().getActiveConnections());
        return conn;
    }
    
    /**
     * Closes the connection pool and releases all resources.
     */
    public static synchronized void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            logger.info("Shutting down connection pool...");
            dataSource.close();
            dataSource = null;
            logger.info("Connection pool shutdown completed");
        }
    }
    
    /**
     * Gets the DataSource for dependency injection.
     *
     * @return the HikariDataSource
     */
    public static HikariDataSource getDataSource() {
        if (dataSource == null) {
            throw new IllegalStateException("Connection pool not initialized. Call initialize() first.");
        }
        return dataSource;
    }

    /**
     * Gets pool statistics for monitoring.
     *
     * @return pool statistics as formatted string
     */
    public static String getPoolStats() {
        if (dataSource == null) {
            return "Connection pool not initialized";
        }
        
        var poolMXBean = dataSource.getHikariPoolMXBean();
        return String.format(
            "Pool Stats - Active: %d, Idle: %d, Waiting: %d, Total: %d",
            poolMXBean.getActiveConnections(),
            poolMXBean.getIdleConnections(),
            poolMXBean.getThreadsAwaitingConnection(),
            poolMXBean.getTotalConnections()
        );
    }
    
    /**
     * Checks if connection pool is initialized and running.
     *
     * @return true if pool is active, false otherwise
     */
    public static boolean isInitialized() {
        return dataSource != null && !dataSource.isClosed();
    }

    private static int getIntEnvVar(String varName, int defaultValue) {
        String value = System.getenv(varName);
        if (value != null && !value.trim().isEmpty()) {
            try {
                int parsed = Integer.parseInt(value.trim());
                logger.fine("Using environment variable " + varName + " = " + parsed);
                return parsed;
            } catch (NumberFormatException e) {
                logger.warning("Invalid value for " + varName + ": " + value + ", using default: " + defaultValue);
                return defaultValue;
            }
        }
        return defaultValue;
    }
}