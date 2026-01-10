package it.uninsubria.client.utils.classesUI;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Gestore centralizzato per la chiusura ordinata delle risorse dell'applicazione.
 * Assicura che tutte le risorse vengano liberate correttamente all'uscita.
 */
public class ResourceManager {
    private static final Logger logger = Logger.getLogger(ResourceManager.class.getName());
    private static final List<Runnable> cleanupTasks = new ArrayList<>();

    /**
     * Registra un task di pulizia da eseguire all'uscita
     */
    public static void registerCleanupTask(Runnable task) {
        synchronized (cleanupTasks) {
            cleanupTasks.add(task);
        }
    }

    /**
     * Rimuove un task di pulizia registrato
     */
    public static void unregisterCleanupTask(Runnable task) {
        synchronized (cleanupTasks) {
            cleanupTasks.remove(task);
        }
    }

    /**
     * Esegue tutti i task di pulizia registrati
     */
    public static void cleanupAll() {
        logger.info("Avvio pulizia risorse dell'applicazione...");

        List<Runnable> tasksToRun;
        synchronized (cleanupTasks) {
            tasksToRun = new ArrayList<>(cleanupTasks);
        }

        // Esegui i task in ordine inverso di registrazione (LIFO)
        for (int i = tasksToRun.size() - 1; i >= 0; i--) {
            try {
                tasksToRun.get(i).run();
            } catch (Exception e) {
                logger.log(Level.WARNING, "Errore durante la pulizia di una risorsa", e);
            }
        }

        // Task specifici dell'applicazione
        performApplicationCleanup();

        logger.info("Pulizia risorse completata");
    }

    /**
     * Task di pulizia specifici dell'applicazione BookRecommender
     */
    private static void performApplicationCleanup() {
        try {
            // 1. Invalida sessione lato server prima di tutto (IMPORTANTE per sicurezza)
            logger.info("Invalidazione sessione lato server...");
            invalidateServerSession();

            // 2. Chiudi thread pool
            logger.info("Chiusura thread pool...");
            ThreadPoolManager.shutdown();

            // 3. Salva cache sessione (ora senza token valido)
            logger.info("Salvataggio cache sessione...");
            SessionManager.getInstance().saveToCache();

            // 4. Chiudi connessione pool del server (se applicabile)
            logger.info("Chiusura connessioni database...");

            // 5. Cancella timer di timeout
            logger.info("Cancellazione timer...");
            TimeoutManager.getInstance().cancelTimer();

            // 6. Altre pulizie specifiche possono essere aggiunte qui se necessario

            logger.info("Tutte le risorse applicative sono state liberate");

            // Log report prestazioni finale
            logger.info("Report prestazioni finale:\n" + PerformanceMonitor.getPerformanceReport());

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore durante la pulizia delle risorse applicative", e);
        }
    }

    /**
     * Invalida la sessione lato server per motivi di sicurezza
     * Questo assicura che quando l'applicazione viene chiusa, nessun altro possa usare la sessione
     */
    private static void invalidateServerSession() {
        try {
            SessionManager sessionManager = SessionManager.getInstance();
            String token = sessionManager.getSessionToken();

            if (token != null && !token.isEmpty()) {
                // Tenta di invalidare la sessione lato server
                try {
                    ServiceLocator.getUserService().invalidateSession(token);
                    logger.info("Sessione invalidata lato server con successo");
                } catch (Exception serverError) {
                    // Se il server non è raggiungibile, logga ma continua
                    logger.warning("Impossibile invalidare la sessione lato server (server non raggiungibile): " + serverError.getMessage());
                }

                // In ogni caso, fai logout locale per sicurezza
                sessionManager.logout();
                logger.info("Logout locale completato");
            } else {
                logger.info("Nessuna sessione attiva da invalidare");
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Errore durante l'invalidazione della sessione", e);
            // Non bloccare la chiusura dell'applicazione per questo errore
        }
    }

    /**
     * Metodo di utilità per eseguire operazioni con garanzia di cleanup
     */
    public static <T> T executeWithResourceCleanup(CompletableFuture<T> future, Runnable cleanup) throws Exception {
        try {
            T result = future.get();
            return result;
        } finally {
            if (cleanup != null) {
                try {
                    cleanup.run();
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Errore durante il cleanup", e);
                }
            }
        }
    }

    /**
     * Verifica se ci sono task di cleanup registrati
     */
    public static boolean hasCleanupTasks() {
        synchronized (cleanupTasks) {
            return !cleanupTasks.isEmpty();
        }
    }

    /**
     * Ottiene il numero di task di cleanup registrati
     */
    public static int getCleanupTaskCount() {
        synchronized (cleanupTasks) {
            return cleanupTasks.size();
        }
    }

    /**
     * Registra un task di cleanup che verrà eseguito automaticamente all'uscita
     * Utile per risorse che devono essere chiuse anche in caso di crash
     */
    public static CleanupRegistration register(Runnable cleanup) {
        registerCleanupTask(cleanup);
        return new CleanupRegistration(cleanup);
    }

    /**
     * Handle per la deregistrazione automatica di un task di cleanup
     */
    public static class CleanupRegistration implements AutoCloseable {
        private final Runnable cleanupTask;

        private CleanupRegistration(Runnable cleanupTask) {
            this.cleanupTask = cleanupTask;
        }

        @Override
        public void close() {
            unregisterCleanupTask(cleanupTask);
        }
    }
}