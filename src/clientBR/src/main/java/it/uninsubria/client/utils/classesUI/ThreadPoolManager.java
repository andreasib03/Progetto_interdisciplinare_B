package it.uninsubria.client.utils.classesUI;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.logging.Level;
import it.uninsubria.shared.utils.LoggerUtil;

/**
 * Thread Pool Manager per operazioni client-side.
 * Fornisce gestione centralizzata dei thread con priorità e monitoraggio.
 */
public class ThreadPoolManager {
    private static final Logger logger = LoggerUtil.getLogger(ThreadPoolManager.class);

    // Pool per operazioni I/O (rete, database)
    private static ExecutorService IO_EXECUTOR;

    // Pool per operazioni CPU-intensive (calcoli, parsing)
    private static ExecutorService CPU_EXECUTOR;

    // Pool per operazioni UI (aggiornamenti interfaccia)
    private static ExecutorService UI_EXECUTOR;

    // Pool per operazioni critiche (login, salvataggi importanti)
    private static ExecutorService CRITICAL_EXECUTOR;

    // Executor per operazioni pianificate
    private static ScheduledExecutorService SCHEDULED_EXECUTOR;

    // Inizializzazione lazy dei pool
    static {
        initializePools();
    }

    /**
     * Inizializza tutti i pool di thread
     */
    private static synchronized void initializePools() {
        if (IO_EXECUTOR == null || IO_EXECUTOR.isShutdown()) {
            IO_EXECUTOR = Executors.newFixedThreadPool(
                ClientAppConfig.getIOThreadPoolSize(),
                new ThreadFactory() {
                    private final AtomicInteger counter = new AtomicInteger(1);
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r, "Client-IO-" + counter.getAndIncrement());
                        t.setDaemon(true);
                        t.setPriority(Thread.NORM_PRIORITY);
                        return t;
                    }
                }
            );
        }

        if (CPU_EXECUTOR == null || CPU_EXECUTOR.isShutdown()) {
            CPU_EXECUTOR = Executors.newFixedThreadPool(
                ClientAppConfig.getCPUThreadPoolSize(),
                new ThreadFactory() {
                    private final AtomicInteger counter = new AtomicInteger(1);
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r, "Client-CPU-" + counter.getAndIncrement());
                        t.setDaemon(true);
                        t.setPriority(Thread.NORM_PRIORITY + 1);
                        return t;
                    }
                }
            );
        }

        if (UI_EXECUTOR == null || UI_EXECUTOR.isShutdown()) {
            UI_EXECUTOR = Executors.newSingleThreadExecutor(
                new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r, "Client-UI-" + System.currentTimeMillis());
                        t.setDaemon(true);
                        t.setPriority(Thread.NORM_PRIORITY);
                        return t;
                    }
                }
            );
        }

        if (CRITICAL_EXECUTOR == null || CRITICAL_EXECUTOR.isShutdown()) {
            CRITICAL_EXECUTOR = Executors.newCachedThreadPool(
                new ThreadFactory() {
                    private final AtomicInteger counter = new AtomicInteger(1);
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r, "Client-Critical-" + counter.getAndIncrement());
                        t.setDaemon(true);
                        t.setPriority(Thread.NORM_PRIORITY + 2);
                        return t;
                    }
                }
            );
        }

        if (SCHEDULED_EXECUTOR == null || SCHEDULED_EXECUTOR.isShutdown()) {
            SCHEDULED_EXECUTOR = Executors.newScheduledThreadPool(2,
                new ThreadFactory() {
                    private final AtomicInteger counter = new AtomicInteger(1);
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r, "Client-Scheduled-" + counter.getAndIncrement());
                        t.setDaemon(true);
                        t.setPriority(Thread.NORM_PRIORITY);
                        return t;
                    }
                }
            );
        }
    }



    // Statistiche
    private static final AtomicInteger activeTasks = new AtomicInteger(0);
    private static final AtomicInteger completedTasks = new AtomicInteger(0);
    private static final AtomicInteger failedTasks = new AtomicInteger(0);

    /**
     * Esegue un'operazione I/O (rete, database) in background
     */
    public static <T> CompletableFuture<T> executeIO(Callable<T> task) {
        return executeOnPool(IO_EXECUTOR, task, "IO");
    }

    /**
     * Esegue un'operazione I/O (rete, database) in background senza return
     */
    public static CompletableFuture<Void> executeIO(Runnable task) {
        return executeOnPool(IO_EXECUTOR, () -> {
            task.run();
            return null;
        }, "IO");
    }

    /**
     * Esegue un'operazione I/O con notifiche di caricamento
     */
    public static <T> CompletableFuture<T> executeIOWithFeedback(Callable<T> task, String loadingMessage, String successMessage) {
        String operationId = "io_" + System.currentTimeMillis();
        return executeOnPool(IO_EXECUTOR, () -> {
            try {
                T result = task.call();
                NotificationManager.endLoading(operationId);
                if (successMessage != null) {
                    NotificationManager.showSuccess(successMessage);
                }
                return result;
            } catch (Exception e) {
                NotificationManager.endLoadingWithError(operationId, e.getMessage());
                throw new RuntimeException("IO operation failed", e);
            }
        }, "IO");
    }

    /**
     * Esegue un'operazione I/O con notifiche di caricamento (senza return)
     */
    public static CompletableFuture<Void> executeIOWithFeedback(Runnable task, String loadingMessage, String successMessage) {
        return executeIOWithFeedback(() -> {
            task.run();
            return null;
        }, loadingMessage, successMessage);
    }

    /**
     * Esegue un'operazione CPU-intensive in background
     */
    public static <T> CompletableFuture<T> executeCPU(Callable<T> task) {
        return executeOnPool(CPU_EXECUTOR, task, "CPU");
    }

    /**
     * Esegue un'operazione CPU-intensive in background senza return
     */
    public static CompletableFuture<Void> executeCPU(Runnable task) {
        return executeOnPool(CPU_EXECUTOR, () -> {
            task.run();
            return null;
        }, "CPU");
    }

    /**
     * Esegue un'operazione UI in background (per aggiornamenti interfaccia)
     */
    public static <T> CompletableFuture<T> executeUI(Callable<T> task) {
        return executeOnPool(UI_EXECUTOR, task, "UI");
    }

    /**
     * Esegue un'operazione UI in background senza return
     */
    public static CompletableFuture<Void> executeUI(Runnable task) {
        return executeOnPool(UI_EXECUTOR, () -> {
            task.run();
            return null;
        }, "UI");
    }


    /**
     * Esegue un'operazione critica senza return
     */
    public static CompletableFuture<Void> executeCritical(Runnable task) {
        return executeOnPool(CRITICAL_EXECUTOR, () -> {
            task.run();
            return null;
        }, "CRITICAL");
    }

    /**
     * Pianifica un'operazione periodica
     */
    public static ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long initialDelay, long period, TimeUnit unit) {
        return SCHEDULED_EXECUTOR.scheduleAtFixedRate(
            () -> {
                try {
                    task.run();
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Errore nell'operazione pianificata", e);
                    failedTasks.incrementAndGet();
                }
            },
            initialDelay,
            period,
            unit
        );
    }

    /**
     * Pianifica un'operazione una tantum con delay
     */
    public static ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit unit) {
        return SCHEDULED_EXECUTOR.schedule(
            () -> {
                try {
                    task.run();
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Errore nell'operazione pianificata", e);
                    failedTasks.incrementAndGet();
                }
            },
            delay,
            unit
        );
    }

    /**
     * Metodo helper per esecuzione su pool specifico
     */
    private static <T> CompletableFuture<T> executeOnPool(ExecutorService pool, Callable<T> task, String poolName) {
        // Controlla se il pool è shutdown e reinizializza se necessario
        if (pool == null || pool.isShutdown()) {
            initializePools();
            // Reimposta il pool corretto basato sul nome
            switch (poolName) {
                case "IO": pool = IO_EXECUTOR; break;
                case "CPU": pool = CPU_EXECUTOR; break;
                case "UI": pool = UI_EXECUTOR; break;
                case "CRITICAL": pool = CRITICAL_EXECUTOR; break;
            }
        }

        activeTasks.incrementAndGet();

        return CompletableFuture.supplyAsync(() -> {
            Thread currentThread = Thread.currentThread();
            String originalName = currentThread.getName();

            try {
                // Imposta nome thread per debugging
                currentThread.setName(originalName + " [" + poolName + "]");

                long startTime = System.currentTimeMillis();
                T result = task.call();
                long duration = System.currentTimeMillis() - startTime;

                completedTasks.incrementAndGet();
                logger.fine(String.format("Task %s completato in %d ms su thread %s", poolName, duration, currentThread.getName()));

                return result;
            } catch (Exception e) {
                failedTasks.incrementAndGet();
                logger.log(Level.SEVERE, String.format("Errore nell'esecuzione task %s su thread %s", poolName, currentThread.getName()), e);
                throw new RuntimeException("Task execution failed on " + poolName + " pool", e);
            } finally {
                activeTasks.decrementAndGet();
                // Ripristina nome originale del thread
                currentThread.setName(originalName);
            }
        }, pool);
    }

    /**
     * Ottiene statistiche del thread pool
     */
    public static ThreadPoolStats getStats() {
        return new ThreadPoolStats(
            activeTasks.get(),
            completedTasks.get(),
            failedTasks.get(),
            getPoolSize(IO_EXECUTOR),
            getPoolSize(CPU_EXECUTOR),
            getPoolSize(UI_EXECUTOR),
            getPoolSize(CRITICAL_EXECUTOR)
        );
    }

    /**
     * Helper method to get pool size safely
     */
    private static int getPoolSize(ExecutorService executor) {
        try {
            // Try to get the underlying ThreadPoolExecutor
            if (executor instanceof ThreadPoolExecutor) {
                return ((ThreadPoolExecutor) executor).getPoolSize();
            }

            // For other types, try reflection to access the underlying executor
            // This handles wrapped executors like those from Executors.newSingleThreadExecutor()
            java.lang.reflect.Field[] fields = executor.getClass().getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                if (field.getType().equals(ExecutorService.class) ||
                    field.getType().equals(ThreadPoolExecutor.class)) {
                    field.setAccessible(true);
                    Object underlyingExecutor = field.get(executor);
                    if (underlyingExecutor instanceof ThreadPoolExecutor) {
                        return ((ThreadPoolExecutor) underlyingExecutor).getPoolSize();
                    }
                }
            }
        } catch (Exception e) {
            // Ignore exceptions and return default
            logger.fine("Could not determine pool size for " + executor.getClass().getSimpleName());
        }

        // Default fallback - assume single thread for unknown executor types
        return 1;
    }

    /**
     * Shutdown controllato di tutti i pool
     */
    public static void shutdown() {
        logger.info("Avvio shutdown thread pools...");

        if (SCHEDULED_EXECUTOR != null) SCHEDULED_EXECUTOR.shutdown();
        if (CRITICAL_EXECUTOR != null) CRITICAL_EXECUTOR.shutdown();
        if (UI_EXECUTOR != null) UI_EXECUTOR.shutdown();
        if (CPU_EXECUTOR != null) CPU_EXECUTOR.shutdown();
        if (IO_EXECUTOR != null) IO_EXECUTOR.shutdown();

        try {
            boolean allTerminated = true;
            if (SCHEDULED_EXECUTOR != null) allTerminated &= SCHEDULED_EXECUTOR.awaitTermination(5, TimeUnit.SECONDS);
            if (CRITICAL_EXECUTOR != null) allTerminated &= CRITICAL_EXECUTOR.awaitTermination(5, TimeUnit.SECONDS);
            if (UI_EXECUTOR != null) allTerminated &= UI_EXECUTOR.awaitTermination(5, TimeUnit.SECONDS);
            if (CPU_EXECUTOR != null) allTerminated &= CPU_EXECUTOR.awaitTermination(5, TimeUnit.SECONDS);
            if (IO_EXECUTOR != null) allTerminated &= IO_EXECUTOR.awaitTermination(5, TimeUnit.SECONDS);

            if (!allTerminated) {
                logger.warning("Not all threads terminated gracefully, forcing shutdown");
                shutdownNow();
            }
        } catch (InterruptedException e) {
            logger.warning("Shutdown interrupted, forcing shutdown");
            shutdownNow();
            Thread.currentThread().interrupt();
        }

        logger.info("Shutdown thread pools completato");
    }

    /**
     * Verifica se tutti i pool sono terminati
     */
    public static boolean isTerminated() {
        return (IO_EXECUTOR == null || IO_EXECUTOR.isTerminated()) &&
               (CPU_EXECUTOR == null || CPU_EXECUTOR.isTerminated()) &&
               (UI_EXECUTOR == null || UI_EXECUTOR.isTerminated()) &&
               (CRITICAL_EXECUTOR == null || CRITICAL_EXECUTOR.isTerminated()) &&
               (SCHEDULED_EXECUTOR == null || SCHEDULED_EXECUTOR.isTerminated());
    }

    /**
     * Shutdown di emergenza
     */
    public static void shutdownNow() {
        logger.warning("Shutdown immediato thread pools");
        if (SCHEDULED_EXECUTOR != null) SCHEDULED_EXECUTOR.shutdownNow();
        if (CRITICAL_EXECUTOR != null) CRITICAL_EXECUTOR.shutdownNow();
        if (UI_EXECUTOR != null) UI_EXECUTOR.shutdownNow();
        if (CPU_EXECUTOR != null) CPU_EXECUTOR.shutdownNow();
        if (IO_EXECUTOR != null) IO_EXECUTOR.shutdownNow();
    }

    /**
     * Reset dei pool per testing - ricrea tutti i pool
     */
    public static synchronized void reset() {
        logger.fine("Reset thread pools per testing");

        // Shutdown esistenti se attivi
        if (SCHEDULED_EXECUTOR != null && !SCHEDULED_EXECUTOR.isShutdown()) {
            SCHEDULED_EXECUTOR.shutdownNow();
        }
        if (CRITICAL_EXECUTOR != null && !CRITICAL_EXECUTOR.isShutdown()) {
            CRITICAL_EXECUTOR.shutdownNow();
        }
        if (UI_EXECUTOR != null && !UI_EXECUTOR.isShutdown()) {
            UI_EXECUTOR.shutdownNow();
        }
        if (CPU_EXECUTOR != null && !CPU_EXECUTOR.isShutdown()) {
            CPU_EXECUTOR.shutdownNow();
        }
        if (IO_EXECUTOR != null && !IO_EXECUTOR.isShutdown()) {
            IO_EXECUTOR.shutdownNow();
        }

        // Aspetta un momento per il shutdown
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Reinizializza
        initializePools();

        logger.fine("Reset thread pools completato");
    }

    /**
     * Ottiene informazioni dettagliate sui pool
     */
    public static String getDetailedStatus() {
        StringBuilder status = new StringBuilder();
        status.append("=== ThreadPoolManager Status ===\n");

        ThreadPoolStats stats = getStats();
        status.append(String.format("Tasks - Active: %d, Completed: %d, Failed: %d\n",
            stats.activeTasks, stats.completedTasks, stats.failedTasks));

        status.append(String.format("IO Pool: %d threads (%s)\n",
            stats.ioPoolSize, IO_EXECUTOR.isShutdown() ? "SHUTDOWN" : "ACTIVE"));
        status.append(String.format("CPU Pool: %d threads (%s)\n",
            stats.cpuPoolSize, CPU_EXECUTOR.isShutdown() ? "SHUTDOWN" : "ACTIVE"));
        status.append(String.format("UI Pool: %d threads (%s)\n",
            stats.uiPoolSize, UI_EXECUTOR.isShutdown() ? "SHUTDOWN" : "ACTIVE"));
        status.append(String.format("Critical Pool: %d threads (%s)\n",
            stats.criticalPoolSize, CRITICAL_EXECUTOR.isShutdown() ? "SHUTDOWN" : "ACTIVE"));
        status.append(String.format("Scheduled Pool: %s\n",
            SCHEDULED_EXECUTOR.isShutdown() ? "SHUTDOWN" : "ACTIVE"));

        status.append(String.format("Overall Status: %s\n",
            isTerminated() ? "TERMINATED" : "RUNNING"));

        return status.toString();
    }

    /**
     * Esegue un'operazione I/O con timeout configurabile
     */
    public static <T> CompletableFuture<T> executeIOWithTimeout(Callable<T> task, long timeoutMs) {
        CompletableFuture<T> future = CompletableFuture.supplyAsync(() -> {
            try {
                return task.call();
            } catch (Exception e) {
                throw new RuntimeException("Task execution failed", e);
            }
        }, IO_EXECUTOR);

        // Applica timeout
        return future.orTimeout(timeoutMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Esegue un'operazione I/O con timeout configurabile (Runnable)
     */
    public static CompletableFuture<Void> executeIOWithTimeout(Runnable task, long timeoutMs) {
        CompletableFuture<Void> future = CompletableFuture.runAsync(task, IO_EXECUTOR);
        return future.orTimeout(timeoutMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Cancella tutte le operazioni in corso sui pool I/O
     */
    public static void cancelAllIOOperations() {
        // Nota: I pool standard non supportano la cancellazione diretta,
        // ma possiamo shutdown e reinizializzare
        logger.warning("Cancelling all I/O operations - this will shutdown and reinitialize the I/O pool");
        shutdownIO();
        initializePools();
    }

    /**
     * Shutdown del pool I/O (per cancellazione forzata)
     */
    private static void shutdownIO() {
        if (IO_EXECUTOR != null && !IO_EXECUTOR.isShutdown()) {
            IO_EXECUTOR.shutdown();
            try {
                if (!IO_EXECUTOR.awaitTermination(2, TimeUnit.SECONDS)) {
                    IO_EXECUTOR.shutdownNow();
                }
            } catch (InterruptedException e) {
                IO_EXECUTOR.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Classe per statistiche del thread pool
     */
    public static class ThreadPoolStats {
        public final int activeTasks;
        public final int completedTasks;
        public final int failedTasks;
        public final int ioPoolSize;
        public final int cpuPoolSize;
        public final int uiPoolSize;
        public final int criticalPoolSize;

        public ThreadPoolStats(int active, int completed, int failed, int io, int cpu, int ui, int critical) {
            this.activeTasks = active;
            this.completedTasks = completed;
            this.failedTasks = failed;
            this.ioPoolSize = io;
            this.cpuPoolSize = cpu;
            this.uiPoolSize = ui;
            this.criticalPoolSize = critical;
        }

        @Override
        public String toString() {
            return String.format(
                "ThreadPool Stats - Active: %d, Completed: %d, Failed: %d, " +
                "IO Pool: %d, CPU Pool: %d, UI Pool: %d, Critical Pool: %d",
                activeTasks, completedTasks, failedTasks,
                ioPoolSize, cpuPoolSize, uiPoolSize, criticalPoolSize
            );
        }
    }
}