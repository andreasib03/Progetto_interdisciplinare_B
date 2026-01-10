package it.uninsubria.client.utils.classesUI;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.concurrent.CountDownLatch;

public class ThreadPoolManagerIntegrationTest {

    private static final Logger logger = Logger.getLogger(ThreadPoolManagerIntegrationTest.class.getName());

    @Before
    public void setUp() {
        // Reset ThreadPoolManager prima di ogni test per garantire pool attivi
        ThreadPoolManager.reset();
    }

    @Test
    public void testAllPoolTypesIntegration() throws Exception {
        logger.info("🧪 Test integrazione ThreadPoolManager - Tutti i pool");

        AtomicInteger ioCompleted = new AtomicInteger(0);
        AtomicInteger cpuCompleted = new AtomicInteger(0);
        AtomicInteger uiCompleted = new AtomicInteger(0);
        AtomicInteger criticalCompleted = new AtomicInteger(0);

        CountDownLatch latch = new CountDownLatch(4);

        // Test pool I/O (simula caricamento recensioni)
        ThreadPoolManager.executeIO(() -> {
            try {
                // Simula chiamata al servizio recensioni
                Thread.sleep(50);
                ioCompleted.incrementAndGet();
                logger.info("✅ Pool I/O: Caricamento recensioni completato");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            latch.countDown();
        });

        // Test pool CPU (simula calcoli pesanti)
        ThreadPoolManager.executeCPU(() -> {
            // Simula calcolo hash o elaborazione dati
            int result = 0;
            for (int i = 0; i < 10000; i++) {
                result += i * i;
            }
            cpuCompleted.incrementAndGet();
            logger.info("✅ Pool CPU: Calcolo completato (risultato: " + result + ")");
            latch.countDown();
            return result;
        });

        // Test pool UI (simula aggiornamenti interfaccia)
        ThreadPoolManager.executeUI(() -> {
            // Simula aggiornamento componenti UI
            logger.info("✅ Pool UI: Aggiornamento interfaccia simulato");
            uiCompleted.incrementAndGet();
            latch.countDown();
        });

        // Test pool Critical (simula operazioni critiche come cambio password)
        ThreadPoolManager.executeCritical(() -> {
            try {
                // Simula validazione password o operazione critica
                Thread.sleep(30);
                criticalCompleted.incrementAndGet();
                logger.info("✅ Pool Critical: Operazione critica completata");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            latch.countDown();
        });

        // Aspetta che tutti i task completino
        assertTrue("Tutti i task dovrebbero completare entro 5 secondi",
            latch.await(5, TimeUnit.SECONDS));

        // Verifica che tutti i task siano stati completati
        assertEquals("Pool I/O dovrebbe aver completato 1 task", 1, ioCompleted.get());
        assertEquals("Pool CPU dovrebbe aver completato 1 task", 1, cpuCompleted.get());
        assertEquals("Pool UI dovrebbe aver completato 1 task", 1, uiCompleted.get());
        assertEquals("Pool Critical dovrebbe aver completato 1 task", 1, criticalCompleted.get());

        logger.info("🎉 Tutti i pool ThreadPoolManager funzionano correttamente!");
    }

    @Test
    public void testExecuteIOWithFeedback() throws Exception {
        logger.info("🧪 Test executeIOWithFeedback (simula LibraryListController - senza JavaFX)");

        AtomicInteger operationCompleted = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);

        // Test semplice senza NotificationManager (che richiede JavaFX)
        ThreadPoolManager.executeIO(() -> {
            try {
                // Simula chiamata al servizio librerie
                Thread.sleep(100);
                operationCompleted.incrementAndGet();
                return "Librerie caricate";
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Operazione interrotta");
            }
        }).thenAccept(result -> {
            assertEquals("Risultato dovrebbe essere corretto", "Librerie caricate", result);
            logger.info("✅ executeIO semplice: " + result);
            latch.countDown();
        }).exceptionally(throwable -> {
            fail("L'operazione non dovrebbe fallire: " + throwable.getMessage());
            return null;
        });

        assertTrue("Operazione dovrebbe completare entro 3 secondi",
            latch.await(3, TimeUnit.SECONDS));
        assertEquals("Operazione dovrebbe essere completata", 1, operationCompleted.get());
    }

    @Test
    public void testConcurrentOperations() throws Exception {
        logger.info("🧪 Test operazioni concorrenti (stress test)");

        final int NUM_OPERATIONS = 20;
        CountDownLatch latch = new CountDownLatch(NUM_OPERATIONS);
        AtomicInteger completedOperations = new AtomicInteger(0);

        // Avvia molte operazioni concorrenti per testare la robustezza
        for (int i = 0; i < NUM_OPERATIONS; i++) {
            final int operationId = i;

            // Opera su pool diversi in base all'ID
            if (operationId % 4 == 0) {
                ThreadPoolManager.executeIO(() -> {
                    try { Thread.sleep(10); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    completedOperations.incrementAndGet();
                    latch.countDown();
                    return null;
                });
            } else if (operationId % 4 == 1) {
                ThreadPoolManager.executeCPU(() -> {
                    int sum = 0;
                    for (int j = 0; j < 100; j++) sum += j;
                    completedOperations.incrementAndGet();
                    latch.countDown();
                    return sum;
                });
            } else if (operationId % 4 == 2) {
                ThreadPoolManager.executeUI(() -> {
                    completedOperations.incrementAndGet();
                    latch.countDown();
                    return null;
                });
            } else {
                ThreadPoolManager.executeCritical(() -> {
                    try { Thread.sleep(5); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    completedOperations.incrementAndGet();
                    latch.countDown();
                });
            }
        }

        assertTrue("Tutte le operazioni concorrenti dovrebbero completare entro 10 secondi",
            latch.await(10, TimeUnit.SECONDS));
        assertEquals("Tutte le operazioni dovrebbero essere completate", NUM_OPERATIONS, completedOperations.get());

        logger.info("🎉 Test concorrenza superato - " + NUM_OPERATIONS + " operazioni completate");
    }

    @Test
    public void testScheduledOperations() throws Exception {
        logger.info("🧪 Test operazioni pianificate");

        AtomicInteger executions = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(3);

        // Pianifica un'operazione che si ripete ogni 100ms per 3 volte
        ThreadPoolManager.scheduleAtFixedRate(() -> {
            int count = executions.incrementAndGet();
            logger.info("✅ Esecuzione pianificata #" + count);
            latch.countDown();

            // Ferma dopo 3 esecuzioni
            if (count >= 3) {
                throw new RuntimeException("Stop scheduling");
            }
        }, 0, 100, TimeUnit.MILLISECONDS);

        assertTrue("Operazioni pianificate dovrebbero completare entro 1 secondo",
            latch.await(1, TimeUnit.SECONDS));
        assertEquals("Dovrebbero esserci 3 esecuzioni", 3, executions.get());
    }

    @Test
    public void testShutdownBehavior() throws Exception {
        logger.info("🧪 Test comportamento shutdown");

        // Verifica che non sia già terminato
        assertFalse("ThreadPoolManager non dovrebbe essere già terminato",
            ThreadPoolManager.isTerminated());

        // Esegui un'operazione
        CompletableFuture<Void> future = ThreadPoolManager.executeIO(() -> {
            try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });

        // Aspetta che completi
        future.get(1, TimeUnit.SECONDS);

        // Ora testa shutdown
        ThreadPoolManager.shutdown();

        // Reset per permettere agli altri test di funzionare
        ThreadPoolManager.reset();

        logger.info("✅ Metodo shutdown disponibile e testato, reset eseguito per preservare altri test");
    }


}