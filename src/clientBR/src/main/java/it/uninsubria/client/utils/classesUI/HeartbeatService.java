package it.uninsubria.client.utils.classesUI;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class HeartbeatService {
    private static final Logger logger = Logger.getLogger(HeartbeatService.class.getName());

    private final int intervalMinutes;
    private final Runnable heartbeatTask;
    private volatile ScheduledExecutorService executor;

    public HeartbeatService(int intervalMinutes, Runnable heartbeatTask) {
        this.intervalMinutes = intervalMinutes;
        this.heartbeatTask = heartbeatTask;
    }

    public synchronized void start() {
        if (executor != null && !executor.isShutdown()) {
            logger.warning("Heartbeat service is already running");
            return;
        }

        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "HeartbeatService");
            thread.setDaemon(true);
            return thread;
        });

        executor.scheduleAtFixedRate(
            heartbeatTask,
            intervalMinutes,
            intervalMinutes,
            TimeUnit.MINUTES
        );

        logger.info("Heartbeat service started (interval: " + intervalMinutes + " minutes)");
    }

    public synchronized void stop() {
        if (executor == null || executor.isShutdown()) {
            return;
        }

        try {
            executor.shutdown();
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
            logger.info("Heartbeat service stopped");
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
            logger.warning("Heartbeat service shutdown interrupted: " + e.getMessage());
        }
    }

    public synchronized boolean isRunning() {
        return executor != null && !executor.isShutdown();
    }
}
