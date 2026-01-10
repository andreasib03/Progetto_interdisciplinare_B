package it.uninsubria.client.utils.classesUI;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import it.uninsubria.shared.utils.LoggerUtil;

import static org.junit.Assert.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class NotificationManagerTest {

    private static final Logger logger = LoggerUtil.getLogger(NotificationManagerTest.class);
    @BeforeClass
    public static void setUpClass() {
        // Disabilita JavaFX per testing in ambiente headless
        NotificationManager.disableJavaFx();
    }

    @Before
    public void setUp() {
        // Reset ThreadPoolManager prima di ogni test per garantire pool attivi
        ThreadPoolManager.reset();
    }

    @Test
    public void testNotificationTypes() {
        // Test che i tipi di notifica siano definiti correttamente
        assertNotNull("NotificationType.SUCCESS should exist", NotificationManager.NotificationType.SUCCESS);
        assertNotNull("NotificationType.ERROR should exist", NotificationManager.NotificationType.ERROR);
        assertNotNull("NotificationType.WARNING should exist", NotificationManager.NotificationType.WARNING);
        assertNotNull("NotificationType.INFO should exist", NotificationManager.NotificationType.INFO);
        assertNotNull("NotificationType.LOADING should exist", NotificationManager.NotificationType.LOADING);

        logger.info("✅ Test tipi notifica superato");
    }

    @Test
    public void testLoadingState() throws InterruptedException {
        // Test gestione stati di caricamento
        String operationId = "test_operation";

        // Avvia caricamento
        NotificationManager.LoadingState state = NotificationManager.startLoading(operationId, "Test loading");
        assertNotNull("LoadingState should not be null", state);
        assertEquals("Operation ID should match", operationId, state.id);
        assertEquals("Message should match", "Test loading", state.message);
        assertTrue("State should be active", state.active);

        // Verifica che sia registrato
        assertTrue("Operation should be loading", NotificationManager.isLoading(operationId));

        // Attendi un po'
        Thread.sleep(100);

        // Verifica tempo trascorso
        assertTrue("Elapsed time should be > 0", state.getElapsedTime() > 0);

        // Termina caricamento
        NotificationManager.endLoading(operationId);

        // Verifica che sia terminato
        assertFalse("Operation should not be loading anymore", NotificationManager.isLoading(operationId));

        logger.info("✅ Test stati caricamento superato");
    }

    @Test
    public void testLoadingStateWithError() {
        // Test gestione errori di caricamento
        String operationId = "test_error_operation";

        assertTrue("Operation should be loading", NotificationManager.isLoading(operationId));

        // Termina con errore
        NotificationManager.endLoadingWithError(operationId, "Test error message");

        // Verifica che sia terminato
        assertFalse("Operation should not be loading anymore", NotificationManager.isLoading(operationId));

        logger.info("✅ Test stati caricamento con errore superato");
    }

    @Test
    public void testActiveLoadings() {
        // Test gestione multiple operazioni di caricamento
        String op1 = "operation1";
        String op2 = "operation2";

        // Avvia due operazioni
        NotificationManager.startLoading(op1, "Loading 1");
        NotificationManager.startLoading(op2, "Loading 2");

        // Verifica che entrambe siano attive
        var activeLoadings = NotificationManager.getActiveLoadings();
        assertEquals("Should have 2 active loadings", 2, activeLoadings.size());
        assertTrue("Operation1 should be active", activeLoadings.containsKey(op1));
        assertTrue("Operation2 should be active", activeLoadings.containsKey(op2));

        // Termina una
        NotificationManager.endLoading(op1);

        // Verifica che ne rimanga una
        activeLoadings = NotificationManager.getActiveLoadings();
        assertEquals("Should have 1 active loading", 1, activeLoadings.size());
        assertFalse("Operation1 should not be active", activeLoadings.containsKey(op1));
        assertTrue("Operation2 should still be active", activeLoadings.containsKey(op2));

        // Termina l'altra
        NotificationManager.endLoading(op2);

        // Verifica che non ne rimanga nessuna
        activeLoadings = NotificationManager.getActiveLoadings();
        assertEquals("Should have 0 active loadings", 0, activeLoadings.size());

        logger.info("✅ Test operazioni multiple superato");
    }

    @Test
    public void testThreadPoolWithFeedback() throws Exception {
        // Test integrazione ThreadPoolManager con notifiche
        String operationId = "integration_test";

        // Verifica che non sia in caricamento inizialmente
        assertFalse("Should not be loading initially", NotificationManager.isLoading(operationId));

        // Esegui operazione con feedback
        var future = ThreadPoolManager.executeIOWithFeedback(
            () -> {
                Thread.sleep(200); // Simula lavoro
                return "Test Result";
            },
            "Test operation in progress...",
            "Test operation completed!"
        );

        // Attendi completamento
        String result = future.get(5, TimeUnit.SECONDS);
        assertEquals("Result should match", "Test Result", result);

        // Verifica che il caricamento sia terminato
        assertFalse("Should not be loading after completion", NotificationManager.isLoading(operationId));

        logger.info("✅ Test integrazione ThreadPool superato");
    }
}