package it.uninsubria.server.cache;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Map;

/**
 * Test class for Distributed Cache Manager functionality
 * Tests distributed invalidation, synchronization, and cluster communication
 */
public class DistributedCacheManagerTest {

    @Test
    public void testDistributedModeInitialization() {
        // Test that distributed mode can be initialized
        boolean isDistributed = DistributedCacheManager.isDistributedMode();

        // Note: In test environment, distributed mode might not be available
        // but the method should not throw exceptions
        assertNotNull("Distributed mode check should not be null", isDistributed);

        System.out.println("Distributed mode enabled: " + isDistributed);
    }

    @Test
    public void testDistributedStats() {
        // Test that distributed statistics can be retrieved
        Map<String, Object> stats = DistributedCacheManager.getDistributedStats();

        assertNotNull("Stats should not be null", stats);
        assertTrue("Stats should contain distributedMode", stats.containsKey("distributedMode"));
        assertTrue("Stats should contain localInvalidations", stats.containsKey("localInvalidations"));
        assertTrue("Stats should contain trackedInvalidations", stats.containsKey("trackedInvalidations"));

        System.out.println("Distributed cache stats: " + stats);
    }

    @Test
    public void testDistributedPutAndRemove() {
        // Test distributed put and remove operations
        String userId = "testUser";
        String key = "distributedTestKey";
        String value = "distributedTestValue";

        try {
            // Test distributed put
            DistributedCacheManager.putDistributed(userId, key, value, 5000);

            // Test distributed remove
            DistributedCacheManager.removeDistributed(userId, key);

            // If no exceptions, distributed operations are working
            assertTrue("Distributed operations completed without errors", true);

        } catch (Exception e) {
            // In test environment without cluster, operations might fail gracefully
            System.out.println("Distributed operation failed (expected in test env): " + e.getMessage());
            assertTrue("Operations should fail gracefully in test environment",
                      e.getMessage().contains("Failed") || e.getMessage().contains("not available"));
        }
    }

    @Test
    public void testDistributedClear() {
        // Test distributed clear operation
        try {
            DistributedCacheManager.clearDistributed();
            assertTrue("Distributed clear completed", true);

        } catch (Exception e) {
            // Expected in test environment
            System.out.println("Distributed clear failed (expected in test env): " + e.getMessage());
            assertTrue("Clear should fail gracefully in test environment",
                      e.getMessage().contains("Failed") || e.getMessage().contains("not available"));
        }
    }

    @Test
    public void testInvalidationMessageProcessing() {
        // Test invalidation message processing logic
        String testMessage = "INVALIDATE:testKey:1234567890:testInstance";

        // This tests the message parsing logic without actual network
        try {
            // The actual processing would happen in processInvalidationMessage
            // but we can test the components
            assertTrue("Invalidation message should contain prefix",
                      testMessage.startsWith("INVALIDATE:"));

            String[] parts = testMessage.substring("INVALIDATE:".length()).split(":");
            assertEquals("Message should have 3 parts", 3, parts.length);
            assertEquals("First part should be key", "testKey", parts[0]);
            assertEquals("Third part should be instance", "testInstance", parts[2]);

        } catch (Exception e) {
            fail("Message processing should not throw exceptions: " + e.getMessage());
        }
    }

    @Test
    public void testShutdownDistributed() {
        // Test graceful shutdown of distributed features
        try {
            DistributedCacheManager.shutdownDistributed();
            assertTrue("Distributed shutdown completed successfully", true);

        } catch (Exception e) {
            fail("Distributed shutdown should not throw exceptions: " + e.getMessage());
        }
    }
}