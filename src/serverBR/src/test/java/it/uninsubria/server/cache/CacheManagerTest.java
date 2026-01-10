package it.uninsubria.server.cache;

import static org.junit.Assert.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;

public class CacheManagerTest {

    private static final Logger logger = Logger.getLogger(CacheManagerTest.class.getName());

    @Before
    public void setUp() {
        CacheManager.clear();
    }

    @After
    public void tearDown() {
        CacheManager.clear();
    }

    @Test
    public void ttlExpires() throws InterruptedException {
        CacheManager.clear();
        String userId = "testUser";
        // TTL 100 ms
        CacheManager.put(userId, "k", "v", 100);
        assertEquals("v", CacheManager.get(userId, "k", String.class));
        Thread.sleep(150);
        assertNull(CacheManager.get(userId, "k", String.class));
    }

    @Test
    public void testCachePerformanceUnderLoad() {
        // Test cache performance under concurrent load
        final int THREAD_COUNT = 5; // Reduced for test stability
        final int OPERATIONS_PER_THREAD = 50;

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        long startTime = System.nanoTime();

        // Submit concurrent tasks
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                        String key = "test_key_" + threadId + "_" + j;
                        String value = "test_value_" + threadId + "_" + j;

                        // Mix of put and get operations
                        if (j % 2 == 0) {
                            CacheManager.put("testUser" + threadId, key, value, 5000); // 5 second TTL
                        } else {
                            CacheManager.get("testUser" + threadId, key, String.class);
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        // Wait for all threads to complete
        try {
            assertTrue("All threads should complete within timeout",
                      latch.await(30, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Test interrupted");
        }

        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1_000_000;

        executor.shutdown();

        // Performance assertions
        assertTrue("Concurrent cache operations should complete in reasonable time (< 5 seconds)",
                  durationMs < 5000);

        // Verify some data integrity
        String testValue = CacheManager.get("testUser0", "test_key_0_48", String.class);
        assertNotNull("Cache should contain some test data after concurrent operations", testValue);

        logger.info("Concurrent cache test completed in " + durationMs + "ms");
    }
}
