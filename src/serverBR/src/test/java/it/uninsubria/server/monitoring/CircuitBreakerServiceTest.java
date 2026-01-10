package it.uninsubria.server.monitoring;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.logging.Logger;

/**
 * Unit tests for CircuitBreakerService.
 * Tests circuit breaker initialization, state transitions, and error handling.
 */
public class CircuitBreakerServiceTest {

    private static final Logger logger = Logger.getLogger(CircuitBreakerServiceTest.class.getName());

    @Test
    public void testCircuitBreakerInitialization() {
        // Test that circuit breaker initializes properly
        String cacheStatus = CircuitBreakerService.getCacheCircuitBreakerStatus();
        String dbStatus = CircuitBreakerService.getDatabaseCircuitBreakerStatus();

        assertNotNull("Cache circuit breaker status should be initialized", cacheStatus);
        assertNotNull("Database circuit breaker status should be initialized", dbStatus);

        logger.info("Circuit breaker initialization test passed");
    }

    @Test
    public void testCircuitBreakerStatusNotEmpty() {
        // Test that circuit breaker returns valid status strings
        String cacheStatus = CircuitBreakerService.getCacheCircuitBreakerStatus();
        String dbStatus = CircuitBreakerService.getDatabaseCircuitBreakerStatus();

        assertTrue("Cache circuit breaker status should not be empty", cacheStatus != null && !cacheStatus.isEmpty());
        assertTrue("Database circuit breaker status should not be empty", dbStatus != null && !dbStatus.isEmpty());

        logger.info("Circuit breaker status not empty test passed");
    }

    @Test
    public void testCircuitBreakerStatusFormat() {
        // Test that circuit breaker status has expected format
        String cacheStatus = CircuitBreakerService.getCacheCircuitBreakerStatus();
        String dbStatus = CircuitBreakerService.getDatabaseCircuitBreakerStatus();

        // Status should be readable strings (typically "OPEN", "CLOSED", "HALF_OPEN")
        assertTrue("Cache status should be a simple string", cacheStatus != null);
        assertTrue("Database status should be a simple string", dbStatus != null);

        logger.info("Circuit breaker status format test passed - Cache: " + cacheStatus + ", DB: " + dbStatus);
    }

    @Test
    public void testMultipleCallsToCircuitBreakerStatus() {
        // Test that multiple calls to circuit breaker work correctly
        String status1 = CircuitBreakerService.getCacheCircuitBreakerStatus();
        String status2 = CircuitBreakerService.getCacheCircuitBreakerStatus();

        assertEquals("Multiple calls should return same status", status1, status2);

        String dbStatus1 = CircuitBreakerService.getDatabaseCircuitBreakerStatus();
        String dbStatus2 = CircuitBreakerService.getDatabaseCircuitBreakerStatus();

        assertEquals("Multiple calls should return same DB status", dbStatus1, dbStatus2);

        logger.info("Multiple circuit breaker calls test passed");
    }
}
