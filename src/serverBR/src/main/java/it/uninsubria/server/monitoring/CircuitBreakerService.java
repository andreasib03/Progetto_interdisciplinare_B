package it.uninsubria.server.monitoring;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import it.uninsubria.server.cache.HybridCacheManager;


import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Circuit breaker service for handling cache and database failures gracefully
 */
public class CircuitBreakerService {

    private static final Logger logger = Logger.getLogger(CircuitBreakerService.class.getName());

    private static CircuitBreakerRegistry registry;
    private static CircuitBreaker cacheCircuitBreaker;
    private static CircuitBreaker databaseCircuitBreaker;

    static {
        // Initialize circuit breaker registry
        registry = CircuitBreakerRegistry.ofDefaults();

        // Configure cache circuit breaker
        cacheCircuitBreaker = registry.circuitBreaker("cache-service",
                CircuitBreakerConfig.custom()
                    .failureRateThreshold(50)        // Open if 50% failures
                    .waitDurationInOpenState(java.time.Duration.ofSeconds(30))  // Wait 30s before half-open
                    .permittedNumberOfCallsInHalfOpenState(3)  // Test with 3 calls in half-open
                    .slidingWindowSize(10)           // Last 10 calls
                    .minimumNumberOfCalls(5)         // Need at least 5 calls to calculate failure rate
                    .build());

        // Configure database circuit breaker
        databaseCircuitBreaker = registry.circuitBreaker("database-service",
                CircuitBreakerConfig.custom()
                    .failureRateThreshold(30)        // Open if 30% failures (more sensitive)
                    .waitDurationInOpenState(java.time.Duration.ofSeconds(60))  // Wait longer for DB
                    .permittedNumberOfCallsInHalfOpenState(2)  // Test with 2 calls for DB
                    .slidingWindowSize(20)           // Last 20 calls for DB
                    .minimumNumberOfCalls(10)        // Need more calls for DB
                    .build());

        // Circuit breakers are monitored through Resilience4j registry

        logger.info("Circuit breaker service initialized");
    }

    // ===== CACHE OPERATIONS WITH CIRCUIT BREAKER =====

    public static <T> T getFromCache(String userId, String key, Class<T> type) {
        return cacheCircuitBreaker.executeSupplier(() -> {
            try {
                T result = HybridCacheManager.get(userId, key, type);
                MonitoringService.getInstance().recordCacheHit();
                return result;
            } catch (Exception e) {
                MonitoringService.getInstance().recordCacheMiss();
                throw new RuntimeException("Cache operation failed", e);
            }
        });
    }

    public static void putInCache(String userId, String key, Object value) {
        cacheCircuitBreaker.executeRunnable(() -> {
            try {
                HybridCacheManager.put(userId, key, value);
            } catch (Exception e) {
                logger.warning("Failed to put in cache: " + e.getMessage());
                throw new RuntimeException("Cache write failed", e);
            }
        });
    }

    // ===== DATABASE OPERATIONS WITH CIRCUIT BREAKER =====

    public static <T> T executeDatabaseOperation(Supplier<T> operation) {
        return databaseCircuitBreaker.executeSupplier(() -> {
            var timer = MonitoringService.getInstance().startQueryTimer();
            try {
                T result = operation.get();
                MonitoringService.getInstance().recordDatabaseQuery();
                return result;
            } catch (Exception e) {
                MonitoringService.getInstance().recordDatabaseError();
                throw new RuntimeException("Database operation failed", e);
            } finally {
                timer.stop(MonitoringService.getInstance().getRegistry().timer("db.query.duration"));
            }
        });
    }

    public static void executeDatabaseOperation(Runnable operation) {
        databaseCircuitBreaker.executeRunnable(() -> {
            var timer = MonitoringService.getInstance().startQueryTimer();
            try {
                operation.run();
                MonitoringService.getInstance().recordDatabaseQuery();
            } catch (Exception e) {
                MonitoringService.getInstance().recordDatabaseError();
                throw new RuntimeException("Database operation failed", e);
            } finally {
                timer.stop(MonitoringService.getInstance().getRegistry().timer("db.query.duration"));
            }
        });
    }

    // ===== CIRCUIT BREAKER STATUS =====

    public static String getCacheCircuitBreakerStatus() {
        return cacheCircuitBreaker.getState().toString();
    }

    public static String getDatabaseCircuitBreakerStatus() {
        return databaseCircuitBreaker.getState().toString();
    }

    public static CircuitBreaker getCacheCircuitBreaker() {
        return cacheCircuitBreaker;
    }

    public static CircuitBreaker getDatabaseCircuitBreaker() {
        return databaseCircuitBreaker;
    }

    // ===== FALLBACK METHODS =====

    public static <T> T executeWithFallback(Supplier<T> operation, Supplier<T> fallback) {
        try {
            return databaseCircuitBreaker.executeSupplier(operation);
        } catch (Exception e) {
            logger.warning("Primary operation failed, using fallback: " + e.getMessage());
            try {
                return fallback.get();
            } catch (Exception fallbackException) {
                logger.severe("Both primary and fallback operations failed: " + fallbackException.getMessage());
                throw new RuntimeException("All operations failed", fallbackException);
            }
        }
    }
}