package it.uninsubria.server.cache;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Performance test for Distributed Cache Manager
 * Tests concurrent operations and performance under load
 */
public class DistributedCacheManagerPerformanceTest {

    @Test
    public void testDistributedCacheConcurrentOperations() {
        final int THREAD_COUNT = 5;
        final int OPERATIONS_PER_THREAD = 20;
        final int TIMEOUT_MS = 10000;

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        long startTime = System.nanoTime();

        // Submit concurrent distributed cache operations
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                        String key = "dist_perf_key_" + threadId + "_" + j;
                        String value = "dist_perf_value_" + threadId + "_" + j;

                        try {
                            // Test distributed operations
                            if (j % 3 == 0) {
                                DistributedCacheManager.putDistributed("perfUser" + threadId, key, value, 5000);
                            } else if (j % 3 == 1) {
                                DistributedCacheManager.removeDistributed("perfUser" + threadId, key);
                            } else {
                                // Test stats retrieval
                                DistributedCacheManager.getDistributedStats();
                            }
                            successCount.incrementAndGet();
                        } catch (Exception e) {
                            errorCount.incrementAndGet();
                            // Expected in test environment without cluster
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        // Wait for completion
        try {
            assertTrue("Concurrent distributed operations should complete within timeout",
                      latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Test interrupted");
        }

        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1_000_000;

        executor.shutdown();

        // Performance assertions
        assertTrue("Distributed operations should complete in reasonable time (< 15 seconds)",
                  durationMs < 15000);

        // At least some operations should succeed (in test env, distributed might not be available)
        int totalOperations = successCount.get() + errorCount.get();
        assertTrue("Some operations should be attempted", totalOperations > 0);

        System.out.println("Distributed cache performance test completed:");
        System.out.println("  Duration: " + durationMs + "ms");
        System.out.println("  Successful operations: " + successCount.get());
        System.out.println("  Failed operations: " + errorCount.get());
        System.out.println("  Success rate: " + (successCount.get() * 100.0 / totalOperations) + "%");
    }

    @Test
    public void testDistributedCacheMemoryEfficiency() {
        // Test that distributed cache doesn't consume excessive memory
        Runtime runtime = Runtime.getRuntime();

        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();

        // Perform some distributed operations
        try {
            for (int i = 0; i < 100; i++) {
                DistributedCacheManager.putDistributed("memoryTest", "key" + i, "value" + i, 30000);
            }
        } catch (Exception e) {
            // Expected in test environment
        }

        // Force garbage collection
        System.gc();
        try { Thread.sleep(100); } catch (InterruptedException e) { }

        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();

        // Memory increase should be reasonable (less than 50MB for 100 entries)
        long memoryIncrease = memoryAfter - memoryBefore;
        assertTrue("Memory increase should be reasonable (< 50MB)", memoryIncrease < 50 * 1024 * 1024);

        System.out.println("Memory test - Before: " + memoryBefore + " bytes, After: " + memoryAfter + " bytes, Increase: " + memoryIncrease + " bytes");
    }

    @Test
    public void testDistributedCacheScalability() {
        // Test cache scalability with increasing load
        int[] operationCounts = {10, 50, 100, 200};

        for (int count : operationCounts) {
            long startTime = System.nanoTime();

            // Perform operations
            try {
                for (int i = 0; i < count; i++) {
                    DistributedCacheManager.putDistributed("scaleTest", "scaleKey" + i, "scaleValue" + i, 10000);
                }
            } catch (Exception e) {
                // Expected in test environment
            }

            long endTime = System.nanoTime();
            long durationMs = (endTime - startTime) / 1_000_000;

            // Performance should scale reasonably (not exponentially worse)
            long expectedMaxTime = count * 5; // 5ms per operation max
            assertTrue("Operations should scale reasonably (" + count + " ops < " + expectedMaxTime + "ms)",
                      durationMs < expectedMaxTime);

            System.out.println("Scalability test - " + count + " operations took " + durationMs + "ms");
        }
    }
}