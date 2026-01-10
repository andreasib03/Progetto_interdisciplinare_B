package it.uninsubria.server.cache;

import static org.junit.Assert.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import it.uninsubria.server.cache.CacheManager.CacheStats;

public class CacheManagerEdgeCaseTest {

    private static final String TEST_USER_ID = "edge_case_test_user";

    @Before
    public void setUp() {
        CacheManager.clear();
    }

    @After
    public void tearDown() {
        CacheManager.clear();
    }

    @Test
    public void testNullKey() {
        try {
            CacheManager.put(TEST_USER_ID, null, "value", 5000, "test");
            fail("Should throw exception for null key");
        } catch (NullPointerException | IllegalArgumentException e) {
            // Expected
        }
    }

    @Test
    public void testNullValue() {
        CacheManager.put(TEST_USER_ID, "key", null, 5000, "test");
        Object value = CacheManager.get(TEST_USER_ID, "key", Object.class);
        assertNull("Null values should be storable", value);
    }

    @Test
    public void testEmptyKey() {
        CacheManager.put(TEST_USER_ID, "", "value", 5000, "test");
        Object value = CacheManager.get(TEST_USER_ID, "", Object.class);
        assertEquals("Empty key should work", "value", value);
    }

    @Test
    public void testEmptyValue() {
        CacheManager.put(TEST_USER_ID, "key", "", 5000, "test");
        String value = CacheManager.get(TEST_USER_ID, "key", String.class);
        assertEquals("Empty string value should work", "", value);
    }

    @Test
    public void testNegativeTTL() {
        CacheManager.put(TEST_USER_ID, "key", "value", -1, "test");
        Object value = CacheManager.get(TEST_USER_ID, "key", Object.class);
        assertNull("Negative TTL should expire immediately", value);
    }

    @Test
    public void testZeroTTL() {
        CacheManager.put(TEST_USER_ID, "key", "value", 0, "test");
        Object value = CacheManager.get(TEST_USER_ID, "key", Object.class);
        assertNull("Zero TTL should expire immediately", value);
    }

    @Test
    public void testVeryLargeTTL() {
        long veryLargeTTL = Long.MAX_VALUE;
        CacheManager.put(TEST_USER_ID, "key", "value", veryLargeTTL, "test");
        Object value = CacheManager.get(TEST_USER_ID, "key", String.class);
        assertNotNull("Very large TTL should work", value);
        assertEquals("value", value);
    }

    @Test
    public void testConcurrentPutSameKey() throws InterruptedException {
        final int THREAD_COUNT = 10;
        final String KEY = "concurrent_key";

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            final int value = i;
            executor.submit(() -> {
                try {
                    CacheManager.put(TEST_USER_ID, KEY, value, 5000, "test");
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue("All threads should complete", latch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        Integer finalValue = CacheManager.get(TEST_USER_ID, KEY, Integer.class);
        assertNotNull("Value should be set", finalValue);
        assertTrue("Value should be from one of the threads", finalValue >= 0 && finalValue < THREAD_COUNT);
    }

    @Test
    public void testConcurrentGetSameKey() throws InterruptedException {
        final int THREAD_COUNT = 20;
        final String KEY = "get_test_key";
        final String VALUE = "test_value";

        CacheManager.put(TEST_USER_ID, KEY, VALUE, 5000, "test");

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                try {
                    String value = CacheManager.get(TEST_USER_ID, KEY, String.class);
                    if (VALUE.equals(value)) {
                        successCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue("All threads should complete", latch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        assertEquals("All gets should succeed", THREAD_COUNT, successCount.get());
    }

    @Test
    public void testConcurrentPutAndGetDifferentKeys() throws InterruptedException {
        final int THREAD_COUNT = 10;
        final int OPERATIONS_PER_THREAD = 100;

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                        String key = "key_" + threadId + "_" + j;
                        CacheManager.put(TEST_USER_ID, key, "value_" + j, 5000, "test");
                        String value = CacheManager.get(TEST_USER_ID, key, String.class);
                        if (value != null && value.startsWith("value_")) {
                            successCount.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue("All threads should complete", latch.await(30, TimeUnit.SECONDS));
        executor.shutdown();

        assertTrue("Most operations should succeed", successCount.get() > THREAD_COUNT * OPERATIONS_PER_THREAD * 0.9);
    }

    @Test
    public void testInvalidateNonExistentKey() {
        CacheManager.remove(TEST_USER_ID, "non_existent_key");
        Object value = CacheManager.get(TEST_USER_ID, "non_existent_key", String.class);
        assertNull("Non-existent key should return null", value);
    }

    @Test
    public void testInvalidateNonExistentUser() {
        CacheManager.invalidateUserCache("non_existent_user");
        CacheStats stats = CacheManager.getUserStats("non_existent_user");
        assertEquals("Non-existent user should have 0 entries", 0, stats.size);
    }

    @Test
    public void testInvalidateCategoryNonExistent() {
        CacheManager.invalidateByCategory(TEST_USER_ID, "non_existent_category");
    }

    @Test
    public void testCacheEvictionUnderLoad() throws InterruptedException {
        final int MAX_SIZE = 10;
        final int THREAD_COUNT = 5;

        for (int i = 0; i < MAX_SIZE; i++) {
            CacheManager.put(TEST_USER_ID, "key_" + i, "value_" + i, 5000, "test");
        }

        CacheStats statsBefore = CacheManager.getUserStats(TEST_USER_ID);
        assertEquals("Cache should have MAX_SIZE entries", MAX_SIZE, statsBefore.size);

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < 100; j++) {
                        String key = "new_key_" + System.currentTimeMillis() + "_" + j;
                        CacheManager.put(TEST_USER_ID, key, "new_value", 5000, "test");
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue("All threads should complete", latch.await(30, TimeUnit.SECONDS));
        executor.shutdown();

        CacheStats statsAfter = CacheManager.getUserStats(TEST_USER_ID);
        assertTrue("Cache should respect MAX_SIZE limit", statsAfter.size <= MAX_SIZE + THREAD_COUNT);
    }

    @Test
    public void testStatsAccuracy() {
        for (int i = 0; i < 10; i++) {
            CacheManager.put(TEST_USER_ID, "key_" + i, "value_" + i, 5000, "test");
        }

        for (int i = 0; i < 5; i++) {
            CacheManager.get(TEST_USER_ID, "key_" + i, String.class);
        }

        CacheStats stats = CacheManager.getUserStats(TEST_USER_ID);
        assertEquals("Stats should show correct size", 10, stats.size);
    }

    @Test
    public void testClearDoesNotAffectOtherUsers() {
        String user1 = "user1";
        String user2 = "user2";

        CacheManager.put(user1, "key1", "value1", 5000, "test");
        CacheManager.put(user2, "key2", "value2", 5000, "test");

        CacheManager.invalidateUserCache(user1);

        assertNull("user1's cache should be cleared", CacheManager.get(user1, "key1", String.class));
        assertNotNull("user2's cache should remain", CacheManager.get(user2, "key2", String.class));
    }
}
