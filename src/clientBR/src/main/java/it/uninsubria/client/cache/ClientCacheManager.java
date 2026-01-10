package it.uninsubria.client.cache;

import it.uninsubria.shared.utils.AppConstants;
import java.util.concurrent.*;
import java.util.Map;

/**
 * Client-side TTL-based Cache Manager
 * Optimized for client-side caching with automatic expiration
 */
public class ClientCacheManager {

    private static final long DEFAULT_TTL_MS = AppConstants.Time.TIMEOUT_5_MINUTES;

    // In-memory cache with timestamps
    private static final Map<String, CacheEntry<?>> memoryCache = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "ClientCacheCleanup");
        t.setDaemon(true);
        return t;
    });

    static {
        // Schedule cleanup every 5 minutes
        cleanupExecutor.scheduleAtFixedRate(ClientCacheManager::cleanupExpiredEntries, 5, 5, TimeUnit.MINUTES);
    }

    /**
     * Cache entry with timestamp and TTL
     */
    private static class CacheEntry<T> {
        final T value;
        final long timestamp;
        final long ttlMs;

        CacheEntry(T value, long ttlMs) {
            this.value = value;
            this.timestamp = System.currentTimeMillis();
            this.ttlMs = ttlMs;
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > ttlMs;
        }
    }

    /**
     * Get cached value with default TTL
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(String key) {
        CacheEntry<?> entry = memoryCache.get(key);
        if (entry != null && !entry.isExpired()) {
            return (T) entry.value;
        }
        return null;
    }

    /**
     * Put value in cache with default TTL
     */
    public static <T> void put(String key, T value) {
        memoryCache.put(key, new CacheEntry<>(value, DEFAULT_TTL_MS));
    }

    /**
     * Invalidate specific key
     */
    public static void invalidate(String key) {
        memoryCache.remove(key);
    }

    /**
     * Clear all cache
     */
    public static void clear() {
        memoryCache.clear();
    }

    /**
     * Cleanup expired entries
     */
    private static void cleanupExpiredEntries() {
        memoryCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
}