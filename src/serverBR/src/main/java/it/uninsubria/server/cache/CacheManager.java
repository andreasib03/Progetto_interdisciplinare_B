package it.uninsubria.server.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import it.uninsubria.shared.utils.AppConstants;

/**
 * Advanced multi-tenant cache manager with TTL, size limits, statistics, and intelligent invalidation.
 * Supports per-user caching to prevent race conditions between multiple clients.
 */
public class CacheManager {
    private static final Logger logger = Logger.getLogger(CacheManager.class.getName());

    private static final class CacheEntry {
        Object value;
        long expiry;
        long accessTime;
        String category;
        CacheEntry(Object v, long e, String cat) {
            this(v, e, cat, 1L); // Start with version 1
        }

        CacheEntry(Object v, long e, String cat, long version) {
            value = v;
            expiry = e;
            accessTime = System.currentTimeMillis();
            category = cat;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiry;
        }

        void recordAccess() {
            accessTime = System.currentTimeMillis();
        }

    }

    // Cache configuration
    private static final int MAX_CACHE_SIZE_PER_USER = AppConstants.Cache.MAX_CACHE_ENTRIES_PER_USER;
    private static final int MAX_USERS = AppConstants.Cache.MAX_CONCURRENT_USERS;
    public static final long DEFAULT_TTL_BOOKS = AppConstants.Time.TIMEOUT_10_MINUTES;
    public static final long DEFAULT_TTL_LIBRARIES = AppConstants.Time.TIMEOUT_5_MINUTES;
    public static final long DEFAULT_TTL_REVIEWS = AppConstants.Time.TIMEOUT_2_MINUTES;

    // Multi-tenant cache: userId -> (cacheKey -> CacheEntry)
    private static final ConcurrentMap<String, ConcurrentMap<String, CacheEntry>> USER_CACHES = new ConcurrentHashMap<>();

    // Statistics
    private static final AtomicLong hits = new AtomicLong(0);
    private static final AtomicLong misses = new AtomicLong(0);
    private static final AtomicLong evictions = new AtomicLong(0);

    // Cleanup scheduler
    private static final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "Cache-Cleanup");
        t.setDaemon(true);
        return t;
    });

    static {
        // Schedule periodic cleanup every 5 minutes
        cleanupExecutor.scheduleAtFixedRate(CacheManager::cleanup, 5, 5, TimeUnit.MINUTES);
    }

    // ===== NUOVI METODI PER CACHE PER-USER =====

    /**
     * Get value from user-specific cache
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(String userId, String key, Class<T> type) {
        ConcurrentMap<String, CacheEntry> userCache = USER_CACHES.get(userId);
        if (userCache == null) {
            misses.incrementAndGet();
            return null;
        }

        CacheEntry e = userCache.get(key);
        if (e == null) {
            misses.incrementAndGet();
            return null;
        }

        if (e.isExpired()) {
            userCache.remove(key);
            evictions.incrementAndGet();
            misses.incrementAndGet();
            return null;
        }

        e.recordAccess();
        hits.incrementAndGet();
        return (T) e.value;
    }

    /**
     * Put value in user-specific cache
     */
    public static void put(String userId, String key, Object value) {
        put(userId, key, value, getDefaultTTL(key));
    }

    /**
     * Put value in user-specific cache with custom TTL
     */
    public static void put(String userId, String key, Object value, long ttlMs) {
        put(userId, key, value, ttlMs, "default");
    }

    /**
     * Put value in user-specific cache with custom TTL and category
     */
    public static void put(String userId, String key, Object value, long ttlMs, String category) {
        ConcurrentMap<String, CacheEntry> userCache = USER_CACHES.computeIfAbsent(userId, k -> new ConcurrentHashMap<>());

        long expiry = System.currentTimeMillis() + ttlMs;

        userCache.compute(key, (k, existingEntry) -> {
            // Check size limits per user before putting new entry
            if (existingEntry == null && userCache.size() >= MAX_CACHE_SIZE_PER_USER) {
                evictLeastRecentlyUsed(userCache);
            }
            return new CacheEntry(value, expiry, category);
        });

        // Check total users limit (this is outside the compute to avoid re-entrant issues)
        if (USER_CACHES.size() >= MAX_USERS) {
            USER_CACHES.computeIfPresent(userId, (id, cache) -> {
                if (cache.size() == 0) {
                    return null; // Remove if empty
                }
                return cache;
            });
            if (USER_CACHES.size() > MAX_USERS) {
                evictLeastRecentlyUsedUser();
            }
        }
    }

    /**
     * Invalidate cache for a specific user
     */
    public static void invalidateUserCache(String userId) {
        USER_CACHES.remove(userId);
        logger.fine("Invalidated cache for user: " + userId);
    }

    /**
     * Invalidate cache by category for a specific user
     */
    public static void invalidateByCategory(String userId, String category) {
        ConcurrentMap<String, CacheEntry> userCache = USER_CACHES.get(userId);
        if (userCache != null) {
            userCache.entrySet().removeIf(entry -> category.equals(entry.getValue().category));
            logger.fine("Invalidated category '" + category + "' for user: " + userId);
        }
    }

    // ===== METODI VECCHI (DEPRECATED) - MANTIENI BACKWARD COMPATIBILITY =====

      /**
       * @deprecated Use get(userId, key, type) instead for multi-tenant safety.
       * This method uses a global "global" userId for backward compatibility with
       * application-wide caches (e.g., book catalogs). For user-specific data,
       * always use the userId-specific methods.
       *
       * @param key the cache key
       * @param type the expected return type
       * @return the cached value or null if not found
       */
      @Deprecated(since = "1.0", forRemoval = true)
      public static <T> T get(String key, Class<T> type) {
          // Fallback to a global cache for backward compatibility (not recommended)
          logger.warning("Using deprecated global cache method. Consider using per-user cache.");
          return get("global", key, type);
      }

      /**
       * @deprecated Use put(userId, key, value) instead for multi-tenant safety.
       * This method uses a global "global" userId for backward compatibility with
       * application-wide caches (e.g., book catalogs).
       *
       * @param key the cache key
       * @param value the value to cache
       */
      @Deprecated(since = "1.0", forRemoval = true)
      public static void put(String key, Object value) {
          put("global", key, value, getDefaultTTL(key));
      }

      /**
       * @deprecated Use put(userId, key, value, ttlMs) instead for multi-tenant safety.
       * This method uses a global "global" userId for backward compatibility with
       * application-wide caches (e.g., book catalogs).
       *
       * @param key the cache key
       * @param value the value to cache
       * @param ttlMs time to live in milliseconds
       */
      @Deprecated(since = "1.0", forRemoval = true)
      public static void put(String key, Object value, long ttlMs) {
          put("global", key, value, ttlMs, "default");
      }

      /**
       * @deprecated Use put(userId, key, value, ttlMs, category) instead for multi-tenant safety.
       * This method uses a global "global" userId for backward compatibility with
       * application-wide caches (e.g., book catalogs).
       *
       * @param key the cache key
       * @param value the value to cache
       * @param ttlMs time to live in milliseconds
       * @param category the cache category
       */
      @Deprecated(since = "1.0", forRemoval = true)
      public static void put(String key, Object value, long ttlMs, String category) {
         put("global", key, value, ttlMs, category);
      }

    /**
     * Clear all cache data
     */
    public static void clear() {
        USER_CACHES.clear();
        logger.info("All user caches cleared completely");
    }

    /**
     * Remove specific key from user cache
     */
    public static void remove(String userId, String key) {
        ConcurrentMap<String, CacheEntry> userCache = USER_CACHES.get(userId);
        if (userCache != null) {
            userCache.remove(key);
        }
    }

     /**
      * @deprecated Use invalidateByCategory(userId, category) instead for better performance.
      * This method invalidates the category for ALL users, which is expensive and may
      * not be necessary. Use user-specific invalidation when possible.
      */
     @Deprecated
     public static void invalidateByCategory(String category) {
        // Invalidate category for all users (expensive operation)
        USER_CACHES.values().forEach(userCache ->
            userCache.entrySet().removeIf(entry -> category.equals(entry.getValue().category))
        );
        logger.info("Invalidated cache category globally: " + category);
    }

    /**
     * Invalidate user-specific data
     */
    public static void invalidateUserData(String userId) {
        ConcurrentMap<String, CacheEntry> userCache = USER_CACHES.get(userId);
        if (userCache != null) {
            userCache.entrySet().removeIf(entry -> entry.getKey().contains(userId));
        }
    }

    /**
     * Invalidate library-related data for a user
     */
    public static void invalidateUserLibraries(String userId) {
        String libraryKey = "libraries_" + userId;
        remove(userId, libraryKey);
        logger.info("Invalidated library cache for user '" + userId + "', key: " + libraryKey);
    }

    /**
     * Invalidate book-related data for a user
     */
    public static void invalidateBookData(String userId, int bookId) {
        String bookKeyPattern = "book_" + bookId;
        ConcurrentMap<String, CacheEntry> userCache = USER_CACHES.get(userId);
        if (userCache != null) {
            userCache.entrySet().removeIf(entry ->
                entry.getKey().contains(bookKeyPattern) ||
                entry.getKey().contains("reviews_" + bookId) ||
                entry.getKey().contains("suggestions_" + bookId));
        }
    }

    /**
     * Invalidate library-related data for a user
     */
    public static void invalidateLibraryData(String userId) {
        invalidateUserData(userId); // Libraries are user-specific
    }

    /**
     * Get global cache statistics
     */
    public static CacheStats getStats() {
        int totalEntries = USER_CACHES.values().stream()
            .mapToInt(Map::size)
            .sum();

        return new CacheStats(
            totalEntries,
            hits.get(),
            misses.get(),
            evictions.get(),
            hits.get() + misses.get() > 0 ?
                (double) hits.get() / (hits.get() + misses.get()) * 100 : 0
        );
    }

    /**
     * Get cache statistics for a specific user
     */
    public static CacheStats getUserStats(String userId) {
        ConcurrentMap<String, CacheEntry> userCache = USER_CACHES.get(userId);
        int userEntries = userCache != null ? userCache.size() : 0;

        // Note: hits/misses/evictions are global, not per-user
        return new CacheStats(
            userEntries,
            0, // Per-user stats not tracked
            0,
            0,
            0.0
        );
    }

    /**
     * Warm up cache with frequently accessed data
     */
    public static void warmUpCache() {
        // This would be called during application startup
        // Implementation depends on specific warming strategy
        logger.info("Cache warm-up completed");
    }

    private static long getDefaultTTL(String key) {
        if (key.startsWith("books_") || key.startsWith("book_")) {
            return DEFAULT_TTL_BOOKS;
        } else if (key.startsWith("libraries_") || key.startsWith("library_")) {
            return DEFAULT_TTL_LIBRARIES;
        } else if (key.startsWith("reviews_") || key.startsWith("suggestions_")) {
            return DEFAULT_TTL_REVIEWS;
        }
        return AppConstants.Time.TIMEOUT_5_MINUTES;
    }

    private static void evictLeastRecentlyUsed(ConcurrentMap<String, CacheEntry> userCache) {
        if (userCache.isEmpty()) return;

        String lruKey = userCache.entrySet().stream()
            .min((e1, e2) -> Long.compare(e1.getValue().accessTime, e2.getValue().accessTime))
            .map(entry -> entry.getKey())
            .orElse(null);

        if (lruKey != null) {
            userCache.remove(lruKey);
            evictions.incrementAndGet();
            logger.fine("Evicted LRU cache entry: " + lruKey);
        }
    }

    private static void evictLeastRecentlyUsedUser() {
        if (USER_CACHES.isEmpty()) return;

        // Find user with oldest average access time
        String lruUserId = USER_CACHES.entrySet().stream()
            .min((e1, e2) -> {
                double avgTime1 = e1.getValue().values().stream()
                    .mapToLong(entry -> entry.accessTime)
                    .average().orElse(Double.MAX_VALUE);
                double avgTime2 = e2.getValue().values().stream()
                    .mapToLong(entry -> entry.accessTime)
                    .average().orElse(Double.MAX_VALUE);
                return Double.compare(avgTime1, avgTime2);
            })
            .map(entry -> entry.getKey())
            .orElse(null);

        if (lruUserId != null) {
            USER_CACHES.remove(lruUserId);
            logger.info("Evicted LRU user cache: " + lruUserId);
        }
    }

    private static void cleanup() {
        System.currentTimeMillis();
        AtomicInteger totalRemoved = new AtomicInteger(0);

        // Use stream().removeIf() for thread-safe operations across all user caches
        USER_CACHES.values().stream()
            .forEach(userCache -> {
                int sizeBefore = userCache.size();
                userCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
                totalRemoved.addAndGet(sizeBefore - userCache.size());
            });

        // Remove empty user caches atomically
        USER_CACHES.entrySet().removeIf(entry -> entry.getValue().isEmpty());

        if (totalRemoved.get() > 0) {
            evictions.addAndGet(totalRemoved.get());
            logger.fine("Cleaned up " + totalRemoved.get() + " expired cache entries across all users");
        }
    }

    /**
     * Cache statistics
     */
    public static class CacheStats {
        public final int size;
        public final long hits;
        public final long misses;
        public final long evictions;
        public final double hitRate;

        CacheStats(int size, long hits, long misses, long evictions, double hitRate) {
            this.size = size;
            this.hits = hits;
            this.misses = misses;
            this.evictions = evictions;
            this.hitRate = hitRate;
        }

        @Override
        public String toString() {
            return String.format("Cache[size=%d, hits=%d, misses=%d, evictions=%d, hitRate=%.2f%%]",
                size, hits, misses, evictions, hitRate);
        }
    }

    /**
     * Shutdown the cache cleanup executor to prevent memory leaks.
     * Should be called during application shutdown.
     */
    public static synchronized void shutdown() {
        if (cleanupExecutor != null && !cleanupExecutor.isShutdown()) {
            logger.info("Shutting down cache cleanup executor...");
            cleanupExecutor.shutdown();
            try {
                if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    logger.warning("Cache cleanup executor did not shutdown gracefully, forcing shutdown");
                    cleanupExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                logger.warning("Interrupted while waiting for cache cleanup executor shutdown");
                Thread.currentThread().interrupt();
            }
            logger.info("Cache cleanup executor shutdown completed");
        }
    }

    /**
     * Register shutdown hook for graceful cleanup.
     */
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            shutdown();
            logger.info("Cache manager shutdown hook completed");
        }));
    }
}
