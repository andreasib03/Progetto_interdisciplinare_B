package it.uninsubria.server.service;

import static org.junit.Assert.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import it.uninsubria.server.cache.CacheManager;
import it.uninsubria.server.dao.TestUserDAO;
import it.uninsubria.shared.model.User;

import org.junit.Before;
import org.junit.After;

public class UserServiceCoreTest {

    @Before
    public void setUp() {
        CacheManager.clear();
    }

    @After
    public void tearDown() {
        CacheManager.clear();
    }

    @Test
    public void testUserCoreDelegation() throws Exception {
        TestUserDAO dao = new TestUserDAO();
        UserServiceCore core = new UserServiceCoreImpl(dao);
        User u = User.builder().id("alice").name("Alice").surname("Smith").email("alice@example.com").CF("").build();
        assertTrue(core.registerUser(u));
        assertTrue(core.login("alice","pwd"));
        assertTrue(core.phrasePassword("alice","phrase"));
        User loaded = core.getUserByUsernameOrEmail("alice");
        assertNotNull(loaded);
    }

    @Test
    public void testPasswordUpdateAndDelete() throws Exception {
        TestUserDAO dao = new TestUserDAO();
        UserServiceCore core = new UserServiceCoreImpl(dao);
        assertTrue(core.updatePassword("alice", "newpass"));
        assertTrue(core.deleteUser("alice"));
    }

    @Test
    public void testDatabaseConnectionFailureHandling() throws Exception {
        new TestUserDAO();
        // Simulate connection failure by setting dao to null or throwing exception
        try {
            UserServiceCore core = new UserServiceCoreImpl(null);
            // This should handle the null DAO gracefully
            assertFalse(core.login("test", "password"));
        } catch (Exception e) {
            // Expected to handle gracefully
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testDatabaseConnectionPoolStress() throws Exception {
        // Test database connection pool under stress
        TestUserDAO dao = new TestUserDAO();
        UserServiceCore core = new UserServiceCoreImpl(dao);

        final int THREAD_COUNT = 5;
        final int OPERATIONS_PER_THREAD = 20;

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        long startTime = System.nanoTime();

        // Submit concurrent database operations
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                        try {
                            // Mix of different operations
                            if (j % 3 == 0) {
                                core.registerUser(User.builder().id("stress_user_" + threadId + "_" + j)
                                                          .name("Test").surname("User").email("stress" + threadId + j + "@test.com").password("pwd").build());
                            } else if (j % 3 == 1) {
                                core.login("stress_user_" + threadId + "_" + (j-1), "pwd");
                            } else {
                                core.getUserByUsernameOrEmail("stress" + threadId + (j-2) + "@test.com");
                            }
                            successCount.incrementAndGet();
                        } catch (Exception e) {
                            errorCount.incrementAndGet();
                            // Expected for some operations in stress test
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        // Wait for completion with timeout
        assertTrue("Concurrent database operations should complete within 30 seconds",
                  latch.await(30, TimeUnit.SECONDS));

        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1_000_000;

        executor.shutdown();

        // Performance assertions
        assertTrue("Stress test should complete in reasonable time (< 10 seconds)",
                  durationMs < 10000);

        // At least some operations should succeed
        assertTrue("Some operations should succeed in stress test",
                  successCount.get() > 0);

        System.out.println("Database stress test completed: " + successCount.get() +
                          " successes, " + errorCount.get() + " errors in " + durationMs + "ms");
    }
}
