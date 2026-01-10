package it.uninsubria.client.utils.classesUI;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Monitor prestazioni per tracciare tempi di esecuzione e statistiche delle operazioni critiche.
 * Utile per identificare colli di bottiglia e ottimizzazioni.
 */
public class PerformanceMonitor {
    private static final Logger logger = Logger.getLogger(PerformanceMonitor.class.getName());

    // Mappa per tracciare statistiche per operazione
    private static final ConcurrentHashMap<String, OperationStats> operationStats = new ConcurrentHashMap<>();

    /**
     * Misura il tempo di esecuzione di un'operazione
     */
    public static void measure(String operationName, Runnable operation) {
        long startTime = System.nanoTime();
        try {
            operation.run();
        } finally {
            long endTime = System.nanoTime();
            recordTiming(operationName, endTime - startTime);
        }
    }

    /**
     * Misura il tempo di esecuzione di un'operazione che restituisce un valore
     */
    public static <T> T measure(String operationName, java.util.function.Supplier<T> operation) {
        long startTime = System.nanoTime();
        try {
            T result = operation.get();
            return result;
        } finally {
            long endTime = System.nanoTime();
            recordTiming(operationName, endTime - startTime);
        }
    }

    /**
     * Registra un tempo di esecuzione per un'operazione
     */
    private static void recordTiming(String operationName, long nanoTime) {
        operationStats.computeIfAbsent(operationName, k -> new OperationStats())
                     .recordTiming(nanoTime);
    }

    /**
     * Ottiene statistiche per un'operazione specifica
     */
    public static OperationStats getStats(String operationName) {
        return operationStats.get(operationName);
    }

    /**
     * Ottiene un report completo delle prestazioni
     */
    public static String getPerformanceReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== REPORT PRESTAZIONI ===\n");

        operationStats.forEach((name, stats) -> {
            report.append(String.format("%-30s: %s\n", name, stats.toString()));
        });

        report.append("===========================\n");
        return report.toString();
    }

    /**
     * Azzera tutte le statistiche
     */
    public static void resetStats() {
        operationStats.clear();
        logger.info("Statistiche prestazioni azzerate");
    }

    /**
     * Logga statistiche operazioni lente (sopra una soglia)
     */
    public static void logSlowOperations(long thresholdMs) {
        operationStats.forEach((name, stats) -> {
            double avgMs = stats.getAverageTimeMs();
            if (avgMs > thresholdMs) {
                logger.warning(String.format("Operazione lenta rilevata: %s - Tempo medio: %.2f ms",
                    name, avgMs));
            }
        });
    }

    /**
     * Classe per statistiche di un'operazione
     */
    public static class OperationStats {
        private final AtomicLong totalTimeNs = new AtomicLong(0);
        private final AtomicLong callCount = new AtomicLong(0);
        private volatile long minTimeNs = Long.MAX_VALUE;
        private volatile long maxTimeNs = Long.MIN_VALUE;

        public synchronized void recordTiming(long nanoTime) {
            totalTimeNs.addAndGet(nanoTime);
            callCount.incrementAndGet();

            if (nanoTime < minTimeNs) minTimeNs = nanoTime;
            if (nanoTime > maxTimeNs) maxTimeNs = nanoTime;

            // Log operazioni molto lente (> 10 secondi)
            if (nanoTime > 10_000_000_000L) { // 10 secondi in nanosecondi
                logger.warning(String.format("Operazione molto lenta rilevata: %.2f secondi",
                    nanoTime / 1_000_000_000.0));
            }
        }

        public long getCallCount() {
            return callCount.get();
        }

        public double getAverageTimeMs() {
            long calls = callCount.get();
            return calls > 0 ? (totalTimeNs.get() / (double) calls) / 1_000_000.0 : 0.0;
        }

        public double getMinTimeMs() {
            return minTimeNs == Long.MAX_VALUE ? 0.0 : minTimeNs / 1_000_000.0;
        }

        public double getMaxTimeMs() {
            return maxTimeNs == Long.MIN_VALUE ? 0.0 : maxTimeNs / 1_000_000.0;
        }

        public double getTotalTimeMs() {
            return totalTimeNs.get() / 1_000_000.0;
        }

        @Override
        public String toString() {
            return String.format("Conteggio: %d, Media: %.2f ms, Min: %.2f ms, Max: %.2f ms, Totale: %.2f ms",
                getCallCount(), getAverageTimeMs(), getMinTimeMs(), getMaxTimeMs(), getTotalTimeMs());
        }
    }

    // Operazioni predefinite da monitorare
    public static final String OP_BOOK_LOADING = "Book Loading";
    public static final String OP_CACHE_LOADING = "Cache Loading";
    public static final String OP_IMAGE_LOADING = "Image Loading";
    public static final String OP_SEARCH_EXECUTION = "Search Execution";
    public static final String OP_UI_UPDATE = "UI Update";
    public static final String OP_NETWORK_CALL = "Network Call";

    /**
     * Wrapper per monitorare caricamento libri
     */
    public static <T> T monitorBookLoading(java.util.function.Supplier<T> operation) {
        return measure(OP_BOOK_LOADING, operation);
    }

    /**
     * Wrapper per monitorare caricamento cache
     */
    public static <T> T monitorCacheLoading(java.util.function.Supplier<T> operation) {
        return measure(OP_CACHE_LOADING, operation);
    }

    /**
     * Wrapper per monitorare caricamento immagini
     */
    public static <T> T monitorImageLoading(java.util.function.Supplier<T> operation) {
        return measure(OP_IMAGE_LOADING, operation);
    }

    /**
     * Wrapper per monitorare esecuzione ricerche
     */
    public static <T> T monitorSearchExecution(java.util.function.Supplier<T> operation) {
        return measure(OP_SEARCH_EXECUTION, operation);
    }

    /**
     * Wrapper per monitorare aggiornamenti UI
     */
    public static void monitorUIUpdate(Runnable operation) {
        measure(OP_UI_UPDATE, operation);
    }

    /**
     * Wrapper per monitorare chiamate di rete
     */
    public static <T> T monitorNetworkCall(java.util.function.Supplier<T> operation) {
        return measure(OP_NETWORK_CALL, operation);
    }
}