package it.uninsubria.client.utils.classesUI;

import org.junit.Test;


import static org.junit.Assert.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class ThreadPoolManagerTest {

    private static final Logger logger = Logger.getLogger(ThreadPoolManagerTest.class.getName());

    @Test
    public void testIOExecutor() throws Exception {
        // Test esecuzione su pool I/O
        CompletableFuture<String> future = ThreadPoolManager.executeIO(() -> {
            try {
                Thread.sleep(100); // Simula lavoro I/O
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "IO Task Completed";
        });

        String result = future.get(2, TimeUnit.SECONDS);
        assertEquals("IO Task Completed", result);
        logger.info("✅ Test pool I/O superato");
    }

    @Test
    public void testCPUExecutor() throws Exception {
        // Test esecuzione su pool CPU
        CompletableFuture<Integer> future = ThreadPoolManager.executeCPU(() -> {
            int sum = 0;
            for (int i = 0; i < 1000; i++) {
                sum += i;
            }
            return sum;
        });

        Integer result = future.get(2, TimeUnit.SECONDS);
        assertEquals(499500, result.intValue()); // Somma 0-999
        logger.info("✅ Test pool CPU superato");
    }

    @Test
    public void testCriticalExecutor() throws Exception {
        // Test esecuzione su pool critico
        CompletableFuture<Void> future = ThreadPoolManager.executeCritical(() -> {
            // Simula operazione critica
            logger.info("Critical task executing...");
        });

        future.get(2, TimeUnit.SECONDS); // Aspetta completamento
        logger.info("✅ Test pool critico superato");
    }

    @Test
    public void testScheduledExecutor() throws Exception {
        // Test pianificazione
        CompletableFuture<String> testFuture = new CompletableFuture<>();

        ThreadPoolManager.schedule(() -> {
            testFuture.complete("Scheduled Task Completed");
        }, 200, TimeUnit.MILLISECONDS);

        String result = testFuture.get(1, TimeUnit.SECONDS);
        assertEquals("Scheduled Task Completed", result);
        logger.info("✅ Test pianificazione superato");
    }

    @Test
    public void testStats() {
        // Test statistiche
        ThreadPoolManager.ThreadPoolStats stats = ThreadPoolManager.getStats();

        assertNotNull("Stats should not be null", stats);
        assertTrue("Active tasks should be >= 0", stats.activeTasks >= 0);
        assertTrue("Completed tasks should be >= 0", stats.completedTasks >= 0);

        logger.info("✅ Test statistiche superato - " + stats);
    }
}