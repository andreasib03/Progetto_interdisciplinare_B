package it.uninsubria.server.cache;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.logging.Logger;
import java.util.Enumeration;

/**
 * Distributed Cache Manager: Extends Hybrid Cache with multi-instance synchronization
 * Provides distributed invalidation and synchronization across multiple application instances
 */
public class DistributedCacheManager extends HybridCacheManager {

    private static final Logger logger = Logger.getLogger(DistributedCacheManager.class.getName());

    private static final String MULTICAST_GROUP = "230.0.0.1";
    private static final int MULTICAST_PORT = 8888;

    private static final ScheduledExecutorService syncExecutor = Executors.newScheduledThreadPool(2, r -> {
        Thread t = new Thread(r, "DistributedCacheSync");
        t.setDaemon(true);
        return t;
    });

    private static final ConcurrentHashMap<String, Long> invalidationTimestamps = new ConcurrentHashMap<>();
    private static final AtomicLong localInvalidationCounter = new AtomicLong(0);

    private static DatagramSocket multicastSocket;
    private static MulticastSocket syncSocket;
    private static volatile boolean distributedMode = false;

    // Invalidation message format: "INVALIDATE:<key>:<timestamp>:<instanceId>"
    private static final String INVALIDATION_PREFIX = "INVALIDATE:";

    static {
        initializeDistributedCache();
    }

    /**
     * Initialize distributed cache features
     */
    private static void initializeDistributedCache() {
        try {
            // Try to enable distributed mode
            initializeMulticast();
            initializeSyncServer();
            distributedMode = true;
            logger.info("Distributed cache mode enabled successfully");

            // Start periodic health check
            syncExecutor.scheduleAtFixedRate(DistributedCacheManager::sendHeartbeat, 30, 30, TimeUnit.SECONDS);

        } catch (Exception e) {
            logger.warning("Failed to initialize distributed cache, falling back to local mode: " + e.getMessage());
            distributedMode = false;
        }
    }

    /**
     * Initialize multicast for cache invalidation broadcasts
     */
    private static void initializeMulticast() throws IOException {
        multicastSocket = new DatagramSocket();
        multicastSocket.setBroadcast(true);
        logger.fine("Multicast socket initialized for cache invalidation broadcasts");
    }

    /**
     * Initialize sync server to receive invalidation messages
     */
    @SuppressWarnings("deprecation")
    private static void initializeSyncServer() throws IOException {
        syncSocket = new MulticastSocket(MULTICAST_PORT);
        InetAddress group = InetAddress.getByName(MULTICAST_GROUP);
        // Use modern joinGroup method with NetworkInterface for Java 14+ compatibility
        try {
            // Try to get the first available network interface
            NetworkInterface networkInterface = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
            if (networkInterface == null) {
                // Fallback: get first non-loopback interface
                Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
                while (interfaces.hasMoreElements()) {
                    NetworkInterface ni = interfaces.nextElement();
                    if (!ni.isLoopback() && ni.isUp()) {
                        networkInterface = ni;
                        break;
                    }
                }
            }
            if (networkInterface != null) {
                syncSocket.joinGroup(new InetSocketAddress(group, MULTICAST_PORT), networkInterface);
            } else {
                // Fallback: try without specifying interface (may still be deprecated)
                logger.warning("No suitable network interface found for multicast, using fallback method");
                syncSocket.joinGroup(group);
            }
        } catch (Exception e) {
            logger.warning("Failed to join multicast group with modern API, using deprecated method: " + e.getMessage());
            // Fallback to deprecated method for compatibility
            syncSocket.joinGroup(group);
        }

        // Start listener thread
        syncExecutor.submit(() -> {
            byte[] buffer = new byte[1024];
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    syncSocket.receive(packet);

                    String message = new String(packet.getData(), 0, packet.getLength());
                    processInvalidationMessage(message);

                } catch (Exception e) {
                    if (!Thread.currentThread().isInterrupted()) {
                        logger.warning("Error receiving invalidation message: " + e.getMessage());
                    }
                }
            }
        });

        logger.fine("Sync server initialized for receiving invalidation messages");
    }

    /**
     * Send heartbeat to announce presence and check cluster health
     */
    private static void sendHeartbeat() {
        if (!distributedMode) return;

        try {
            String heartbeat = "HEARTBEAT:" + getLocalInstanceId() + ":" + System.currentTimeMillis();
            broadcastMessage(heartbeat);
        } catch (Exception e) {
            logger.warning("Failed to send heartbeat: " + e.getMessage());
        }
    }

    /**
     * Broadcast invalidation message to all cluster instances
     */
    private static void broadcastInvalidation(String key, long timestamp) {
        if (!distributedMode) return;

        try {
            String message = INVALIDATION_PREFIX + key + ":" + timestamp + ":" + getLocalInstanceId();
            broadcastMessage(message);
            logger.fine("Broadcasted invalidation for key: " + key);
        } catch (Exception e) {
            logger.warning("Failed to broadcast invalidation: " + e.getMessage());
        }
    }

    /**
     * Send message via multicast
     */
    private static void broadcastMessage(String message) throws IOException {
        byte[] data = message.getBytes();
        DatagramPacket packet = new DatagramPacket(data, data.length,
                                                  InetAddress.getByName(MULTICAST_GROUP), MULTICAST_PORT);
        multicastSocket.send(packet);
    }

    /**
     * Process received invalidation message
     */
    private static void processInvalidationMessage(String message) {
        if (!message.startsWith(INVALIDATION_PREFIX)) {
            return; // Not an invalidation message
        }

        try {
            String[] parts = message.substring(INVALIDATION_PREFIX.length()).split(":");
            if (parts.length >= 3) {
                String key = parts[0];
                long timestamp = Long.parseLong(parts[1]);
                String instanceId = parts[2];

                // Skip if this invalidation originated from this instance
                if (!instanceId.equals(getLocalInstanceId())) {
                    Long lastInvalidation = invalidationTimestamps.get(key);
                    if (lastInvalidation == null || timestamp > lastInvalidation) {
                        // Invalidate local cache - use user-specific invalidation
                        invalidateLocalUser("system"); // System-wide invalidation
                        invalidationTimestamps.put(key, timestamp);
                        logger.fine("Processed distributed invalidation for key: " + key + " from instance: " + instanceId);
                    }
                }
            }
        } catch (Exception e) {
            logger.warning("Failed to process invalidation message: " + e.getMessage());
        }
    }

    /**
     * Get unique instance identifier
     */
    private static String getLocalInstanceId() {
        try {
            return InetAddress.getLocalHost().getHostName() + "_" + ProcessHandle.current().pid();
        } catch (Exception e) {
            // Fallback if hostname resolution fails
            return "instance_" + ProcessHandle.current().pid();
        }
    }

    /**
     * Invalidate local cache for user (helper method)
     */
    private static void invalidateLocalUser(String userId) {
        // Use available parent class method
        HybridCacheManager.invalidateUserCache(userId);
        logger.fine("Local cache invalidation triggered for user: " + userId);
    }

    // Override key methods to add distributed invalidation

    /**
     * Put with distributed invalidation broadcast (extended functionality)
     */
    public static void putDistributed(String userId, String key, Object value, long ttlMs) {
        // First put locally using parent class
        HybridCacheManager.put(userId, key, value, ttlMs);

        // Broadcast invalidation to ensure cluster consistency
        long timestamp = System.currentTimeMillis();
        localInvalidationCounter.incrementAndGet();
        broadcastInvalidation(key, timestamp);
    }

    /**
     * Invalidate user cache with distributed broadcast
     */
    public static void invalidateUserDistributed(String userId) {
        // Invalidate local user cache
        HybridCacheManager.invalidateUserCache(userId);

        // Broadcast invalidation to cluster
        long timestamp = System.currentTimeMillis();
        localInvalidationCounter.incrementAndGet();
        broadcastInvalidation(userId + ":*", timestamp); // User-specific invalidation
    }

    /**
     * Remove with distributed invalidation
     */
    public static void removeDistributed(String userId, String key) {
        // First remove locally using user cache invalidation
        HybridCacheManager.invalidateUserCache(userId);

        // Then broadcast invalidation
        long timestamp = System.currentTimeMillis();
        localInvalidationCounter.incrementAndGet();
        broadcastInvalidation(key, timestamp);
    }

    /**
     * Clear all with distributed broadcast
     */
    public static void clearDistributed() {
        // Clear locally using cache warmup (which clears everything)
        HybridCacheManager.warmUpCache(); // This effectively clears and rebuilds
        long timestamp = System.currentTimeMillis();
        localInvalidationCounter.incrementAndGet();
        broadcastInvalidation("*", timestamp); // Wildcard invalidation
    }

    /**
     * Check if distributed mode is enabled
     */
    public static boolean isDistributedMode() {
        return distributedMode;
    }

    /**
     * Get distributed cache statistics
     */
    public static Map<String, Object> getDistributedStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("distributedMode", distributedMode);
        stats.put("localInvalidations", localInvalidationCounter.get());
        stats.put("trackedInvalidations", invalidationTimestamps.size());
        stats.put("multicastGroup", MULTICAST_GROUP);
        stats.put("multicastPort", MULTICAST_PORT);
        return stats;
    }

    /**
     * Graceful shutdown
     */
    public static void shutdownDistributed() {
        distributedMode = false;

        if (multicastSocket != null && !multicastSocket.isClosed()) {
            multicastSocket.close();
        }

        if (syncSocket != null && !syncSocket.isClosed()) {
            syncSocket.close();
        }

        syncExecutor.shutdown();
        try {
            if (!syncExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                syncExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            syncExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        logger.info("Distributed cache shutdown completed");
    }
}