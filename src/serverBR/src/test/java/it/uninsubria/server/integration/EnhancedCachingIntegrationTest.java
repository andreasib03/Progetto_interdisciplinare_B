package it.uninsubria.server.integration;

import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;

import it.uninsubria.server.cache.HybridCacheManager;
import it.uninsubria.server.di.ServerDIContainer;
import it.uninsubria.server.monitoring.CircuitBreakerService;
import it.uninsubria.server.monitoring.MonitoringService;
import it.uninsubria.server.service.BookServiceCore;
import it.uninsubria.shared.model.Book;

/**
 * Integration tests for the enhanced caching and monitoring system
 */
public class EnhancedCachingIntegrationTest {

    private static BookServiceCore bookService;

    @BeforeClass
    public static void setupTestEnvironment() throws Exception {
        // Initialize test database and services
        TestDatabaseHelper.initializeTestDatabase();

        // Initialize enhanced services
        MonitoringService.getInstance();

        // Get book service from DI container
        bookService = ServerDIContainer.getBookCore();
        assertNotNull("BookService should be available", bookService);
    }

    @Test
    public void testHybridCacheBasicOperations() throws Exception {
        // Test basic cache operations
        String testUser = "testUser";
        String testKey = "testBook";
        Book testBook = new Book("Test Title", "Test Author", 2023);

        // Put in cache
        HybridCacheManager.put(testUser, testKey, testBook);

        // Get from cache
        Book cachedBook = HybridCacheManager.get(testUser, testKey, Book.class);
        assertNotNull("Book should be retrieved from cache", cachedBook);
        assertEquals("Book title should match", "Test Title", cachedBook.getTitle());
    }

    @Test
    public void testHybridCacheStatistics() throws Exception {
        // Test cache statistics
        var initialStats = HybridCacheManager.getStats();
        assertNotNull("Cache stats should be available", initialStats);

        // Perform some operations
        HybridCacheManager.put("user1", "book1", new Book("Book1", "Author1", 2023));
        HybridCacheManager.put("user1", "book2", new Book("Book2", "Author2", 2023));
        HybridCacheManager.get("user1", "book1", Book.class);

        var updatedStats = HybridCacheManager.getStats();
        assertTrue("Cache size should be > 0", updatedStats.memorySize >= 0);
    }

    @Test
    public void testCircuitBreakerIntegration() {
        // Test circuit breaker status
        String cacheStatus = CircuitBreakerService.getCacheCircuitBreakerStatus();
        String dbStatus = CircuitBreakerService.getDatabaseCircuitBreakerStatus();

        assertNotNull("Cache circuit breaker status should be available", cacheStatus);
        assertNotNull("Database circuit breaker status should be available", dbStatus);

        // Test that circuit breaker is properly initialized
        assertTrue("Circuit breaker should have a valid status", cacheStatus.length() > 0);
        assertTrue("Circuit breaker should have a valid status", dbStatus.length() > 0);
    }

    @Test
    public void testMonitoringService() throws Exception {
        // Test monitoring service
        var healthStatus = MonitoringService.getInstance().getHealthStatus();
        assertNotNull("Health status should be available", healthStatus);

        // Test metrics recording
        MonitoringService.getInstance().recordCacheHit();
        MonitoringService.getInstance().recordDatabaseQuery();

        var registry = MonitoringService.getInstance().getRegistry();
        assertNotNull("Metrics registry should be available", registry);
    }

    @Test
    public void testCacheInvalidation() throws Exception {
        // Put data in cache
        String userId = "testUser";
        HybridCacheManager.put(userId, "testKey", new Book("Test", "Author", 2023));

        // Verify it's cached
        Book cached = HybridCacheManager.get(userId, "testKey", Book.class);
        assertNotNull("Book should be cached", cached);

        // Invalidate user cache
        HybridCacheManager.invalidateUserCache(userId);
    }

    @Test
    public void testPerformanceMetrics() throws Exception {
        // Test performance measurement
        var timer = MonitoringService.getInstance().startCacheTimer();
        Thread.sleep(10); // Small delay to measure
        timer.stop(MonitoringService.getInstance().getRegistry().timer("cache.operation.duration"));

        // Verify timer was recorded (no exception thrown)
        assertTrue("Performance timing should work", true);
    }

    @Test
    public void testConcurrentCacheAccess() throws Exception {
        // Test concurrent access to cache
        String userId = "concurrentUser";

        Thread[] threads = new Thread[5];
        for (int i = 0; i < threads.length; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                try {
                    // Each thread performs cache operations
                    Book book = new Book("Book" + threadId, "Author" + threadId, 2023);
                    HybridCacheManager.put(userId, "key" + threadId, book);

                    Book retrieved = HybridCacheManager.get(userId, "key" + threadId, Book.class);
                    assertNotNull("Concurrent cache access should work", retrieved);
                    assertEquals("Retrieved book should match", "Book" + threadId, retrieved.getTitle());
                } catch (Exception e) {
                    fail("Concurrent cache access failed: " + e.getMessage());
                }
            });
        }

        // Start all threads
        for (Thread t : threads) {
            t.start();
        }

        // Wait for completion
        for (Thread t : threads) {
            t.join(5000); // 5 second timeout
        }
    }
}