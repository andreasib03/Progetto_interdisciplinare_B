package it.uninsubria.server.monitoring;

import io.micrometer.core.instrument.*;
import it.uninsubria.server.cache.HybridCacheManager;
import it.uninsubria.server.util.ConnectionPoolManager;


import java.util.logging.Logger;

/**
 * Advanced monitoring service with Micrometer metrics and health checks
 */
public class MonitoringService {

    private static final Logger logger = Logger.getLogger(MonitoringService.class.getName());

    private static MeterRegistry registry;
    private static MonitoringService instance;

    // Metrics
    private static Counter databaseQueries;
    private static Counter databaseErrors;
    private static Counter cacheHits;
    private static Counter cacheMisses;
    private MonitoringService() {
        initializeMetrics();
        startHealthChecks();
        logger.info("Monitoring service initialized");
    }

    public static synchronized MonitoringService getInstance() {
        if (instance == null) {
            instance = new MonitoringService();
        }
        return instance;
    }

    private void initializeMetrics() {
        // For now, use a simple in-memory registry
        // In production, you would configure JMX or other external registries
        registry = new io.micrometer.core.instrument.simple.SimpleMeterRegistry();

        // Simple counters for basic monitoring
        databaseQueries = registry.counter("db.queries.total", "description", "Total database queries");
        databaseErrors = registry.counter("db.errors.total", "description", "Total database errors");
        cacheHits = registry.counter("cache.hits.total", "description", "Total cache hits");
        cacheMisses = registry.counter("cache.misses.total", "description", "Total cache misses");

        registry.timer("db.query.duration", "description", "Database query duration");
        registry.timer("cache.operation.duration", "description", "Cache operation duration");

        logger.info("Basic monitoring metrics initialized");
    }

    private void startHealthChecks() {
        // Health checks are performed on-demand via getHealthStatus()
        logger.info("Health check system initialized");
    }

    // ===== PUBLIC API METHODS =====

    public void recordDatabaseQuery() {
        databaseQueries.increment();
    }

    public void recordDatabaseError() {
        databaseErrors.increment();
    }

    public void recordCacheHit() {
        cacheHits.increment();
    }

    public void recordCacheMiss() {
        cacheMisses.increment();
    }

    public Timer.Sample startQueryTimer() {
        return Timer.start(registry);
    }

    public Timer.Sample startCacheTimer() {
        return Timer.start(registry);
    }

    public MeterRegistry getRegistry() {
        return registry;
    }

    public HealthStatus getHealthStatus() {
        boolean dbHealthy = checkDatabaseHealth();
        boolean cacheHealthy = checkCacheHealth();

        return new HealthStatus(dbHealthy, cacheHealthy, dbHealthy && cacheHealthy);
    }

    private boolean checkDatabaseHealth() {
        try {
            // Simple health check - try to get a connection
            ConnectionPoolManager.getConnection().close();
            return true;
        } catch (Exception e) {
            logger.warning("Database health check failed: " + e.getMessage());
            return false;
        }
    }

    private boolean checkCacheHealth() {
        try {
            // Check if cache is responsive
            HybridCacheManager.getStats();
            return true;
        } catch (Exception e) {
            logger.warning("Cache health check failed: " + e.getMessage());
            return false;
        }
    }

    // ===== INNER CLASSES =====

    public static class HealthStatus {
        public final boolean databaseHealthy;
        public final boolean cacheHealthy;
        public final boolean overallHealthy;

        public HealthStatus(boolean db, boolean cache, boolean overall) {
            this.databaseHealthy = db;
            this.cacheHealthy = cache;
            this.overallHealthy = overall;
        }

        @Override
        public String toString() {
            return String.format("Health[db=%s, cache=%s, overall=%s]",
                    databaseHealthy, cacheHealthy, overallHealthy);
        }
    }
}