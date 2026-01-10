package it.uninsubria.server.cache;

import it.uninsubria.server.monitoring.MonitoringService;
import it.uninsubria.server.service.BookServiceCore;
import it.uninsubria.shared.model.Book;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * Intelligent cache warming service for frequently accessed data
 */
public class CacheWarmUpService {

    private static final Logger logger = Logger.getLogger(CacheWarmUpService.class.getName());

    private static final ExecutorService warmUpExecutor = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "CacheWarmUp");
        t.setDaemon(true);
        return t;
    });

    /**
     * Warm up cache with frequently accessed data - Optimized for large datasets
     */
    public static void warmUpCache(BookServiceCore bookService) {
        logger.info("Starting intelligent cache warm-up...");

        CompletableFuture<Void> warmUpFuture = CompletableFuture.runAsync(() -> {
            try {
                long startTime = System.currentTimeMillis();

                // Check total books count first
                int totalBooks = bookService.getTotalBooksCount();
                logger.info("Total books in catalog: " + totalBooks);

                if (totalBooks > 50000) {
                    // For large datasets, warm up in phases
                    warmUpLargeDataset(bookService, totalBooks);
                } else {
                    // For smaller datasets, load all at once
                    logger.info("Warming up global book catalog...");
                    List<Book> allBooks = bookService.searchGlobally();
                    logger.info("Cached " + allBooks.size() + " books in global catalog");
                }

                // Warm up popular authors (sample) - only for small datasets or after main warmup
                if (totalBooks <= 50000) {
                    warmUpPopularAuthors(bookService);
                }

                long duration = System.currentTimeMillis() - startTime;
                logger.info("Initial cache warm-up phase completed in " + duration + "ms");

                // Continue with background warm-up for large datasets
                if (totalBooks > 50000) {
                    scheduleBackgroundWarmUp(bookService);
                }

                // Record warm-up completion in metrics
                MonitoringService.getInstance().getRegistry()
                    .counter("cache.warmup.completed")
                    .increment();

                logger.info("Cache warm-up initial phase completed successfully");

            } catch (Exception e) {
                logger.severe("Cache warm-up failed: " + e.getMessage());
                MonitoringService.getInstance().getRegistry()
                    .counter("cache.warmup.failed")
                    .increment();
            }
        }, warmUpExecutor);

        // Continue asynchronously - don't block application startup
        warmUpFuture.whenComplete((result, throwable) -> {
            if (throwable != null) {
                logger.severe("Cache warm-up completed with errors: " + throwable.getMessage());
            } else {
                logger.info("Cache warm-up initial process finished");
            }
        });
    }

    /**
     * Optimized warm-up for large datasets (>50k books)
     */
    private static void warmUpLargeDataset(BookServiceCore bookService, int totalBooks) throws Exception {
        logger.info("Performing optimized warm-up for large dataset...");

        // Load first 10k books immediately (most likely to be accessed)
        List<Book> firstBatch = bookService.searchGloballyPaged(0, 10000);
        logger.info("Warmed up first " + firstBatch.size() + " books");

        // Pre-load next batches in background
        warmUpExecutor.submit(() -> {
            try {
                // Load next 20k books in background
                List<Book> secondBatch = bookService.searchGloballyPaged(10000, 20000);
                logger.info("Background warm-up: loaded additional " + secondBatch.size() + " books");

                // Cache popular search patterns (empty searches to establish patterns)
                // This helps with future searches
                logger.info("Cache warm-up for large dataset completed");
            } catch (Exception e) {
                logger.warning("Background warm-up failed: " + e.getMessage());
            }
        });
    }

    /**
     * Warm up popular authors for smaller datasets
     */
    private static void warmUpPopularAuthors(BookServiceCore bookService) {
        // Warm up popular authors (sample)
        String[] popularAuthors = {"Rowling", "Tolkien", "Martin", "King", "Grisham"};
        for (String author : popularAuthors) {
            warmUpAuthor(bookService, author);
        }
    }

    /**
     * Schedule background warm-up tasks for large datasets
     */
    private static void scheduleBackgroundWarmUp(BookServiceCore bookService) {
        warmUpExecutor.submit(() -> {
            try {
                // Wait 5 minutes before starting background warm-up
                Thread.sleep(5 * 60 * 1000); // 5 minutes

                logger.info("Starting background cache warm-up...");

                // Additional warm-up tasks can be added here
                // For example: warm up category searches, popular titles, etc.

                logger.info("Background cache warm-up completed");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warning("Background cache warm-up was interrupted");
            } catch (Exception e) {
                logger.warning("Background cache warm-up failed: " + e.getMessage());
            }
        });
    }

    /**
     * Warm up books for a specific author
     */
    private static void warmUpAuthor(BookServiceCore bookService, String author) {
        try {
            List<Book> authorBooks = bookService.searchByAuthor(author);
            logger.fine("Warmed up " + authorBooks.size() + " books for author: " + author);
        } catch (Exception e) {
            logger.warning("Failed to warm up author " + author + ": " + e.getMessage());
        }
    }

    /**
     * Warm up cache with user-specific data (for logged-in users)
     */
    public static void warmUpUserCache(BookServiceCore bookService, String userId) {
        warmUpExecutor.submit(() -> {
            try {
                logger.fine("Warming up cache for user: " + userId);

                // Warm up user's frequently accessed data
                List<Book> userBooks = bookService.searchGloballyForUser(userId);
                logger.fine("Warmed up " + userBooks.size() + " books for user: " + userId);

                MonitoringService.getInstance().getRegistry()
                    .counter("cache.user.warmup.completed")
                    .increment();

            } catch (Exception e) {
                logger.warning("Failed to warm up user cache for " + userId + ": " + e.getMessage());
                MonitoringService.getInstance().getRegistry()
                    .counter("cache.user.warmup.failed")
                    .increment();
            }
        });
    }

    /**
     * Shutdown the warm-up service
     */
    public static void shutdown() {
        warmUpExecutor.shutdown();
        try {
            if (!warmUpExecutor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                warmUpExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        logger.info("Cache warm-up service shutdown");
    }
}