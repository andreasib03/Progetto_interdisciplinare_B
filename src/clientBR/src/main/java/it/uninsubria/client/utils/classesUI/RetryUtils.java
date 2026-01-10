package it.uninsubria.client.utils.classesUI;

import java.rmi.RemoteException;
import java.util.concurrent.Callable;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Utilità per implementare retry mechanism per operazioni RMI.
 * Ritenta automaticamente le operazioni fallite a causa di problemi di connessione temporanei.
 */
public class RetryUtils {
    private static final Logger logger = Logger.getLogger(RetryUtils.class.getName());

    /**
     * Numero massimo di tentativi per le operazioni RMI (da configurazione)
     */
    public static final int MAX_RETRIES = ClientAppConfig.getRetryAttempts();

    /**
     * Ritardo tra tentativi (millisecondi) (da configurazione)
     */
    public static final long RETRY_DELAY_MS = ClientAppConfig.getRetryDelayMs();

    /**
     * Esegue un'operazione RMI con retry automatico in caso di fallimento
     *
     * @param operation l'operazione da eseguire
     * @param operationName nome dell'operazione per logging
     * @param <T> tipo di ritorno dell'operazione
     * @return il risultato dell'operazione
     * @throws Exception se tutti i tentativi falliscono
     */
    public static <T> T executeWithRetry(Callable<T> operation, String operationName) throws Exception {
        Exception lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                return operation.call();
            } catch (RemoteException e) {
                lastException = e;
                if (attempt < MAX_RETRIES) {
                    logger.log(Level.WARNING,
                        "Tentativo {0}/{1} fallito per ''{2}'': {3}. Nuovo tentativo in {4}ms...",
                        new Object[]{attempt, MAX_RETRIES, operationName, e.getMessage(), RETRY_DELAY_MS});

                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Operazione interrotta durante retry", ie);
                    }
                }
            } catch (Exception e) {
                // Per altre eccezioni (non RemoteException), non ritentare
                logger.log(Level.SEVERE, "Errore non recuperabile in ''{0}'': {1}",
                    new Object[]{operationName, e.getMessage()});
                throw e;
            }
        }

        logger.log(Level.SEVERE, "Tutti i {0} tentativi falliti per ''{1}''",
            new Object[]{MAX_RETRIES, operationName});
        throw new RuntimeException("Operazione fallita dopo " + MAX_RETRIES + " tentativi: " + operationName, lastException);
    }

    /**
     * Versione void dell'operazione con retry
     */
    public static void executeWithRetry(Runnable operation, String operationName) throws Exception {
        executeWithRetry(() -> {
            operation.run();
            return null;
        }, operationName);
    }

    /**
     * Esegue un'operazione RMI senza retry (per operazioni che non devono essere ritentate)
     */
    public static <T> T executeWithoutRetry(Callable<T> operation, String operationName) throws Exception {
        try {
            return operation.call();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore in ''{0}'': {1}",
                new Object[]{operationName, e.getMessage()});
            throw e;
        }
    }
}