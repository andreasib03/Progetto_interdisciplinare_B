package it.uninsubria.client.utils.classesUI;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import it.uninsubria.shared.model.Book;
import it.uninsubria.shared.model.Review;

/**
 * Gestisce le statistiche aggregate dei libri (valutazioni globali)
 */
public class BookStatisticsManager {
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(BookStatisticsManager.class.getName());
    private static BookStatisticsManager instance;
    private final Map<Integer, BookStats> bookStats = new ConcurrentHashMap<>();
    private boolean loaded = false;

    private BookStatisticsManager() {}

    public static synchronized BookStatisticsManager getInstance() {
        if (instance == null) {
            instance = new BookStatisticsManager();
        }
        return instance;
    }

    /**
     * Statistiche aggregate per un libro
     */
    public static class BookStats {
        private final int bookId;
        private double averageRating;
        private int reviewCount;
        private double popularityScore; // Punteggio calcolato basato su valutazioni e numero di review

        public BookStats(int bookId, double averageRating, int reviewCount) {
            this.bookId = bookId;
            this.averageRating = averageRating;
            this.reviewCount = reviewCount;
            this.popularityScore = calculatePopularityScore();
        }

        private double calculatePopularityScore() {
            // Formula: valutazione media * (1 + log(numero di review))
            // Questo premia libri con valutazioni alte e molti review
            if (reviewCount == 0) return 0.0;
            return averageRating * (1.0 + Math.log(reviewCount + 1) / Math.log(10));
        }

        public int getBookId() { return bookId; }
        public double getAverageRating() { return averageRating; }
        public int getReviewCount() { return reviewCount; }
        public double getPopularityScore() { return popularityScore; }
    }

    /**
     * Carica le statistiche aggregate per tutti i libri
     */
    public CompletableFuture<Void> loadBookStatistics(List<Book> allBooks) {
        if (loaded) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            try {
                for (Book book : allBooks) {
                    List<Review> reviews = ServiceLocator.getReviewsService().getReviewsByBook(book.getBook_id());

                    if (!reviews.isEmpty()) {
                        double totalScore = 0.0;
                        for (Review review : reviews) {
                            totalScore += review.averageScore();
                        }
                        double averageRating = totalScore / reviews.size();

                        BookStats stats = new BookStats(book.getBook_id(), averageRating, reviews.size());
                        bookStats.put(book.getBook_id(), stats);
                    }
                }
                loaded = true;
                logger.info("Statistiche libri caricate: " + bookStats.size() + " libri con recensioni");
            } catch (Exception e) {
                System.err.println("Errore nel caricamento delle statistiche libri: " + e.getMessage());
            }
        });
    }

    /**
     * Ottiene le statistiche per un libro specifico
     */
    public BookStats getBookStats(int bookId) {
        return bookStats.get(bookId);
    }

    /**
     * Verifica se le statistiche sono state caricate
     */
    public boolean isLoaded() {
        return loaded;
    }

    /**
     * Azzera le statistiche (per refresh)
     */
    public void clearStats() {
        bookStats.clear();
        loaded = false;
    }
}