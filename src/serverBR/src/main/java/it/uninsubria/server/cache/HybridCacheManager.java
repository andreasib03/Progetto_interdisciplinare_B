package it.uninsubria.server.cache;

import it.uninsubria.shared.utils.AppConstants;
import java.io.*;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.logging.Logger;
import java.util.zip.*;

/**
 * Hybrid Cache Manager: In-memory + Disk Persistence
 * Combines fast in-memory access with disk persistence for large datasets
 */
public class HybridCacheManager {

    private static final Logger logger = Logger.getLogger(HybridCacheManager.class.getName());

    private static final String CACHE_DIR = "./cache";
    private static final long DISK_CACHE_SIZE_MB = 500;
    private static final long DEFAULT_TTL_MS = AppConstants.Time.TIMEOUT_10_MINUTES;

    private static final ExecutorService diskWriter = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "DiskCacheWriter");
        t.setDaemon(true);
        return t;
    });

    private static final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "HybridCacheCleanup");
        t.setDaemon(true);
        return t;
    });

    // Statistics
    private static final AtomicLong diskHits = new AtomicLong(0);
    private static final AtomicLong diskMisses = new AtomicLong(0);
    private static final AtomicLong diskWrites = new AtomicLong(0);

    static {
        // Initialize disk cache directory
        try {
            Files.createDirectories(Paths.get(CACHE_DIR));
            logger.info("Hybrid cache directory initialized: " + CACHE_DIR);
        } catch (IOException e) {
            logger.severe("Failed to initialize cache directory: " + e.getMessage());
        }

        cleanupExecutor.scheduleAtFixedRate(HybridCacheManager::cleanupExpiredEntries, 30, 30, TimeUnit.MINUTES);
    }

    // ===== PUBLIC API METHODS =====

    /**
     * Get value from hybrid cache (memory first, then disk)
     */
    public static <T> CompletableFuture<T> getAsync(String userId, String key, Class<T> type) {
        // Try memory first (fast)
        T result = CacheManager.get(userId, key, type);
        if (result != null) {
            return CompletableFuture.completedFuture(result);
        }

        // Load from disk asynchronously with timeout
        return loadFromDiskAsync(userId, key, type);
    }

    /**
     * Get value from hybrid cache (blocking version)
     */
    public static <T> T get(String userId, String key, Class<T> type) {
        return getAsync(userId, key, type).join();
    }

    /**
     * Put value in hybrid cache with default TTL
     */
    public static void put(String userId, String key, Object value) {
        put(userId, key, value, DEFAULT_TTL_MS, "default");
    }

    /**
     * Put value in hybrid cache with custom TTL
     */
    public static void put(String userId, String key, Object value, long ttlMs) {
        put(userId, key, value, ttlMs, "default");
    }

    /**
     * Put value in hybrid cache with custom TTL and category
     */
    public static void put(String userId, String key, Object value, long ttlMs, String category) {
        // Put in memory
        CacheManager.put(userId, key, value, ttlMs, category);

        // Async write to disk (don't block main thread)
        diskWriter.submit(() -> saveToDisk(userId, key, value, ttlMs, category));
        diskWrites.incrementAndGet();
    }

    /**
     * Invalidate cache for a specific user
     */
    public static void invalidateUserCache(String userId) {
        CacheManager.invalidateUserCache(userId);
        deleteUserDiskCache(userId);
        logger.fine("Invalidated hybrid cache for user: " + userId);
    }

    /**
     * Invalidate cache by category for a specific user
     */
    public static void invalidateByCategory(String userId, String category) {
        CacheManager.invalidateByCategory(userId, category);
        deleteUserDiskCacheByCategory(userId, category);
        logger.fine("Invalidated category '" + category + "' for user: " + userId);
    }

    /**
     * Get hybrid cache statistics
     */
    public static HybridCacheStats getStats() {
        var memStats = CacheManager.getStats();
        return new HybridCacheStats(
            memStats.size,
            memStats.hits,
            memStats.misses,
            memStats.evictions,
            memStats.hitRate,
            diskHits.get(),
            diskMisses.get(),
            diskWrites.get(),
            getDiskCacheSize()
        );
    }

    /**
     * Warm up cache with frequently accessed data
     */
    public static void warmUpCache() {
        CacheManager.warmUpCache();
        logger.info("Hybrid cache warm-up completed");
    }

    /**
     * Shutdown the hybrid cache system
     */
    public static void shutdown() {
        cleanupExecutor.shutdown();
        diskWriter.shutdown();
        try {
            if (!diskWriter.awaitTermination(5, TimeUnit.SECONDS)) {
                diskWriter.shutdownNow();
            }
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        logger.info("Hybrid cache system shutdown completed");
    }

    // ===== PRIVATE DISK MANAGEMENT METHODS =====

    private static <T> CompletableFuture<T> loadFromDiskAsync(String userId, String key, Class<T> type) {
        final long DISK_READ_TIMEOUT_MS = 3000; // 3 second timeout

        return CompletableFuture.supplyAsync(() -> loadFromDiskSync(userId, key, type), diskWriter)
            .orTimeout(DISK_READ_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .handle((result, ex) -> {
                if (ex != null) {
                    if (ex instanceof java.util.concurrent.TimeoutException) {
                        logger.warning("Disk cache read timeout for key: " + key);
                        diskMisses.incrementAndGet();
                    } else {
                        logger.fine("Disk cache read error for key " + key + ": " + ex.getMessage());
                        diskMisses.incrementAndGet();
                    }
                    return null;
                }
                return result;
            });
    }

    private static <T> T loadFromDiskSync(String userId, String key, Class<T> type) {
        Path filePath = Paths.get(CACHE_DIR, userId, key + ".cache");
        if (!Files.exists(filePath)) {
            diskMisses.incrementAndGet();
            return null;
        }

        try {
            // Check if file is expired
            long fileAge = System.currentTimeMillis() - Files.getLastModifiedTime(filePath).toMillis();
            if (fileAge > DEFAULT_TTL_MS) {
                Files.deleteIfExists(filePath); // Clean up expired file
                diskMisses.incrementAndGet();
                return null;
            }

            try (ObjectInputStream ois = new ObjectInputStream(
                    new GZIPInputStream(Files.newInputStream(filePath)))) {

                Object readObject = ois.readObject();

                // Handle both old format (direct object) and new format (DiskCacheEntry)
                Object value;
                String category = "default";
                if (readObject instanceof DiskCacheEntry) {
                    DiskCacheEntry entry = (DiskCacheEntry) readObject;
                    value = entry.value;
                    category = entry.category;
                } else {
                    // Legacy format - direct object
                    value = readObject;
                }

                @SuppressWarnings("unchecked")
                T result = (T) value;

                // Also put back in memory for faster future access
                CacheManager.put(userId, key, result, DEFAULT_TTL_MS, category);

                diskHits.incrementAndGet();
                return result;
            }
        } catch (Exception e) {
            diskMisses.incrementAndGet();
            logger.warning("Failed to load from disk cache: " + e.getMessage());
            try {
                Files.deleteIfExists(filePath);
            } catch (IOException ex) {
                logger.fine("Failed to delete corrupted cache file " + filePath + ": " + ex.getMessage());
            }
            return null;
        }
    }

    private static void saveToDisk(String userId, String key, Object value, long ttlMs, String category) {
        Path userDir = Paths.get(CACHE_DIR, userId);
        Path filePath = userDir.resolve(key + ".cache");

        try {
            Files.createDirectories(userDir);

            // Create cache entry with metadata
            DiskCacheEntry entry = new DiskCacheEntry(value, ttlMs, category);

            try (ObjectOutputStream oos = new ObjectOutputStream(
                    new GZIPOutputStream(Files.newOutputStream(filePath)))) {
                // Always write the DiskCacheEntry wrapper
                oos.writeObject(entry);
            }

            // Enforce disk size limits
            enforceDiskLimits(userDir);

        } catch (IOException e) {
            logger.warning("Failed to save to disk cache: " + e.getMessage());
        }
    }

    private static void deleteUserDiskCache(String userId) {
        Path userDir = Paths.get(CACHE_DIR, userId);
        try {
            if (Files.exists(userDir)) {
                Files.walk(userDir)
                    .filter(Files::isRegularFile)
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException ex) {
                            logger.fine("Failed to delete cache file " + p + ": " + ex.getMessage());
                        }
                    });
                Files.deleteIfExists(userDir);
            }
        } catch (IOException e) {
            logger.warning("Failed to delete user disk cache: " + e.getMessage());
        }
    }

    private static void deleteUserDiskCacheByCategory(String userId, String category) {
        Path userDir = Paths.get(CACHE_DIR, userId);
        if (!Files.exists(userDir)) return;

        try {
            Files.walk(userDir)
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".cache"))
                .forEach(p -> {
                    try {
                        // Load and check category
                        try (ObjectInputStream ois = new ObjectInputStream(
                                new GZIPInputStream(Files.newInputStream(p)))) {
                            DiskCacheEntry entry = (DiskCacheEntry) ois.readObject();
                            if (category.equals(entry.category)) {
                                Files.delete(p);
                            }
                        }
                    } catch (Exception ex) {
                        logger.fine("Failed to read cache file " + p + ": " + ex.getMessage());
                        try {
                            Files.delete(p);
                        } catch (IOException ex2) {
                            logger.fine("Failed to delete unreadable cache file " + p + ": " + ex2.getMessage());
                        }
                    }
                });
        } catch (IOException e) {
            logger.warning("Failed to delete category from disk cache: " + e.getMessage());
        }
    }

    private static void enforceDiskLimits(Path userDir) throws IOException {
        long totalSize = Files.walk(userDir)
            .filter(Files::isRegularFile)
            .mapToLong(p -> {
                try { return Files.size(p); } catch (IOException e) { return 0; }
            })
            .sum();

        if (totalSize > DISK_CACHE_SIZE_MB * 1024 * 1024) {
            // Remove oldest files until under limit
            Files.walk(userDir)
                .filter(Files::isRegularFile)
                .sorted((p1, p2) -> {
                    try {
                        return Files.getLastModifiedTime(p1)
                            .compareTo(Files.getLastModifiedTime(p2));
                    } catch (IOException e) { return 0; }
                })
                .limit(20) // Remove more files at a time for efficiency
                .forEach(p -> {
                    try { Files.delete(p); } catch (IOException ignored) {}
                });

            logger.fine("Enforced disk cache limits for user: " + userDir.getFileName());
        }
    }

    private static long getDiskCacheSize() {
        Path cacheDir = Paths.get(CACHE_DIR);
        if (!Files.exists(cacheDir)) return 0;

        try {
            return Files.walk(cacheDir)
                .filter(Files::isRegularFile)
                .mapToLong(p -> {
                    try { return Files.size(p); } catch (IOException e) { return 0; }
                })
                .sum();
        } catch (IOException e) {
            return 0;
        }
    }

    private static void cleanupExpiredEntries() {
        Path cacheDir = Paths.get(CACHE_DIR);
        if (!Files.exists(cacheDir)) return;

        try {
            long now = System.currentTimeMillis();
            AtomicInteger removedCount = new AtomicInteger(0);

            Files.walk(cacheDir)
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".cache"))
                .forEach(p -> {
                    try {
                        long fileAge = now - Files.getLastModifiedTime(p).toMillis();
                        if (fileAge > DEFAULT_TTL_MS) {
                            Files.delete(p);
                            removedCount.incrementAndGet();
                        }
                    } catch (IOException ignored) {}
                });

            if (removedCount.get() > 0) {
                logger.fine("Cleaned up " + removedCount.get() + " expired disk cache entries");
            }
        } catch (IOException e) {
            logger.warning("Failed to cleanup expired disk cache entries: " + e.getMessage());
        }
    }

    // ===== INNER CLASSES =====

    /**
     * Disk cache entry with metadata
     */
    private static class DiskCacheEntry implements Serializable {
        private static final long serialVersionUID = 1L;

        final Object value;
        final String category;
        DiskCacheEntry(Object value, long ttlMs, String category) {
            this.value = value;
            ttlMs = System.currentTimeMillis() + ttlMs;
            this.category = category;
            System.currentTimeMillis();
        }

        
    }

    /**
     * Hybrid cache statistics
     */
    public static class HybridCacheStats {
        public final int memorySize;
        public final long memoryHits;
        public final long memoryMisses;
        public final long memoryEvictions;
        public final double memoryHitRate;
        public final long diskHits;
        public final long diskMisses;
        public final long diskWrites;
        public final long diskSizeBytes;

        HybridCacheStats(int memorySize, long memoryHits, long memoryMisses, long memoryEvictions,
                        double memoryHitRate, long diskHits, long diskMisses, long diskWrites, long diskSizeBytes) {
            this.memorySize = memorySize;
            this.memoryHits = memoryHits;
            this.memoryMisses = memoryMisses;
            this.memoryEvictions = memoryEvictions;
            this.memoryHitRate = memoryHitRate;
            this.diskHits = diskHits;
            this.diskMisses = diskMisses;
            this.diskWrites = diskWrites;
            this.diskSizeBytes = diskSizeBytes;
        }

        @Override
        public String toString() {
            return String.format(
                "HybridCache[memory: size=%d, hits=%d, misses=%d, evictions=%d, hitRate=%.2f%% | " +
                "disk: hits=%d, misses=%d, writes=%d, size=%.2fMB]",
                memorySize, memoryHits, memoryMisses, memoryEvictions, memoryHitRate,
                diskHits, diskMisses, diskWrites, diskSizeBytes / (1024.0 * 1024.0)
            );
        }
    }
}