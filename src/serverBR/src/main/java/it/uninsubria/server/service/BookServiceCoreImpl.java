package it.uninsubria.server.service;

import it.uninsubria.server.cache.HybridCacheManager;
import it.uninsubria.server.dao.BookDAO;
import it.uninsubria.server.util.InputValidator;
import it.uninsubria.shared.model.Book;
import it.uninsubria.shared.model.ReviewManager;
import it.uninsubria.shared.utils.AppConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Core implementation of BookService, handling business logic and caching for book operations.
 */
public class BookServiceCoreImpl implements BookServiceCore {
    private static final Logger logger = Logger.getLogger(BookServiceCoreImpl.class.getName());
    private final BookDAO bookDAO;

    private static final long DEFAULT_CACHE_TTL = AppConstants.Time.TIMEOUT_10_MINUTES;
    private static final long LARGE_DATASET_TTL = AppConstants.Time.TIMEOUT_1_HOUR;
    private static final long REVIEWS_CACHE_TTL = AppConstants.Time.TIMEOUT_2_MINUTES;
    private static final long LIBRARY_CACHE_TTL = AppConstants.Time.TIMEOUT_5_MINUTES;

    private static final int LARGE_DATASET_THRESHOLD = AppConstants.Batch.LARGE_DATASET_THRESHOLD;
    private static final int LARGE_DATASET_SIZE_THRESHOLD = AppConstants.Batch.LARGE_DATASET_SIZE_THRESHOLD;
    private static final int BATCH_SIZE = AppConstants.Batch.DEFAULT_SIZE;

    /**
     * Constructs a BookServiceCoreImpl with the given BookDAO.
     *
     * @param bookDAO the BookDAO instance
     */
    public BookServiceCoreImpl(BookDAO bookDAO) {
        this.bookDAO = bookDAO;
    }

    // Generic cache helper methods
    private <T> T getFromCache(String userId, String cacheKey, Class<T> type) {
        return HybridCacheManager.get(userId, cacheKey, type);
    }


    private <T> void putInCache(String userId, String cacheKey, T value, long ttl, String category) {
        HybridCacheManager.put(userId, cacheKey, value, ttl, category);
    }

    // Generic method execution with caching
    private <T> T executeWithCaching(String userId, String cacheKey, long ttl, String category,
                                   CacheableSupplier<T> supplier) throws Exception {
        @SuppressWarnings("unchecked")
        T cached = (T) getFromCache(userId, cacheKey, Object.class);
        if (cached != null) {
            return cached;
        }

        T result = supplier.get();
        putInCache(userId, cacheKey, result, ttl, category);
        return result;
    }

    // Validation helpers
    private void validateSearchInput(String query) throws Exception {
        InputValidator.validateSearchQuery(query);
    }

    private void validateSearchInputAndYear(String query, int year) throws Exception {
        InputValidator.validateSearchQuery(query);
        InputValidator.validateYear(year);
    }

    @FunctionalInterface
    private interface CacheableSupplier<T> {
        T get() throws Exception;
    }

    @Override
    public List<Book> searchGlobally() throws Exception {
        String cacheKey = "books_all";
        @SuppressWarnings("unchecked")
        List<Book> cached = HybridCacheManager.get("global", cacheKey, List.class);
        if (cached != null && !cached.isEmpty() && cached.get(0) instanceof Book) {
            return cached;
        }

        // Check total book count to decide loading strategy
        int totalBooks = bookDAO.getTotalBooksCount();

        List<Book> result;
        if (totalBooks > LARGE_DATASET_THRESHOLD) {
            result = loadBooksOptimized(totalBooks);
        } else {
            result = bookDAO.findAll();
        }

        // Cache with longer TTL for large datasets
        long ttl = totalBooks > LARGE_DATASET_SIZE_THRESHOLD ? LARGE_DATASET_TTL : DEFAULT_CACHE_TTL;
        HybridCacheManager.put("global", cacheKey, result, ttl, "books");
        return result;
    }

    /**
      * Optimized loading for large book catalogs
      */
    private List<Book> loadBooksOptimized(int totalBooks) throws Exception {
        List<Book> result = new ArrayList<>();
        int batches = calculateBatchCount(totalBooks);

        // Load first batch synchronously for immediate response
        result.addAll(loadFirstBatch());

        // Load remaining batches asynchronously and cache them
        if (batches > 1) {
            loadRemainingBatchesAsync(batches);
        }

        return result;
    }

    private int calculateBatchCount(int totalBooks) {
        return (int) Math.ceil(totalBooks / (double) BATCH_SIZE);
    }

    private List<Book> loadFirstBatch() throws Exception {
        return bookDAO.findAllPaged(0, BATCH_SIZE);
    }

    private void loadRemainingBatchesAsync(int totalBatches) {
        CompletableFuture.runAsync(() -> {
            try {
                for (int i = 1; i < totalBatches; i++) {
                    List<Book> batch = loadBatch(i);
                    cacheBatch(i, batch);
                }
            } catch (Exception e) {
                logger.severe("Error loading book batches: " + e.getMessage());
            }
        });
    }

    private List<Book> loadBatch(int batchIndex) throws Exception {
        int offset = batchIndex * BATCH_SIZE;
        return bookDAO.findAllPaged(offset, BATCH_SIZE);
    }

    private void cacheBatch(int batchIndex, List<Book> batch) {
        HybridCacheManager.put("global", "books_batch_" + batchIndex, batch, LARGE_DATASET_TTL, "books");
    }

    @Override
    public List<Book> searchByTitle(String title) throws Exception {
        validateSearchInput(title);
        return bookDAO.findByTitle(title);
    }

    @Override
    public List<Book> searchByAuthor(String author) throws Exception {
        validateSearchInput(author);
        return bookDAO.findByAuthor(author);
    }

    @Override
    public List<Book> searchByAuthorAndYear(String author, int year) throws Exception {
        validateSearchInputAndYear(author, year);
        return bookDAO.findByAuthorAndYear(author, year);
    }

    @Override
    public List<Book> searchByTitleAndYear(String title, int year) throws Exception {
        validateSearchInputAndYear(title, year);
        return bookDAO.findByTitleAndYear(title, year);
    }

    @Override
    public Book getBookDetails(int bookId) throws Exception {
        return bookDAO.findByID(bookId);
    }

    @Override
    public List<ReviewManager> getAggregatedReviews(int idLibro) throws Exception {
        return bookDAO.getAggregatedReviews(idLibro);
    }

    @Override
    public List<Book> getLibraryBooks(int libraryId) throws Exception {
        return bookDAO.getLibraryBooks(libraryId);
    }

    @Override
    public void invalidateBooksCache() throws Exception {
        // Invalidate global book cache
        HybridCacheManager.invalidateByCategory("global", "books");
        logger.fine("Invalidated global books cache");
    }

    @Override
    public List<Book> searchGloballyPaged(int offset, int limit) throws Exception {
        String cacheKey = "books_global_paged_" + offset + "_" + limit;
        return executeWithCaching("global", cacheKey, DEFAULT_CACHE_TTL, "books",
            () -> bookDAO.findAllPaged(offset, limit));
    }

    @Override
    public List<Book> searchByTitlePaged(String query, int offset, int limit) throws Exception {
        validateSearchInput(query);
        String cacheKey = "books_title_paged_" + query + "_" + offset + "_" + limit;
        return executeWithCaching("global", cacheKey, DEFAULT_CACHE_TTL, "books",
            () -> bookDAO.findByTitlePaged(query, offset, limit));
    }

    @Override
    public List<Book> searchByAuthorPaged(String author, int offset, int limit) throws Exception {
        validateSearchInput(author);
        String cacheKey = "books_author_paged_" + author + "_" + offset + "_" + limit;
        return executeWithCaching("global", cacheKey, DEFAULT_CACHE_TTL, "books",
            () -> bookDAO.findByAuthorPaged(author, offset, limit));
    }

    @Override
    public int getTotalBooksCount() throws Exception {
        String cacheKey = "books_total_count";
        return executeWithCaching("global", cacheKey, DEFAULT_CACHE_TTL, "books",
            () -> bookDAO.getTotalBooksCount());
    }

    // ===== NUOVI METODI PER CACHE PER-USER =====

    @Override
    public List<Book> searchGloballyForUser(String userId) throws Exception {
        String cacheKey = "books_all";
        @SuppressWarnings("unchecked")
        List<Book> cached = HybridCacheManager.get(userId, cacheKey, List.class);
        if (cached != null) return cached;

        List<Book> result = bookDAO.findAll();
        HybridCacheManager.put(userId, cacheKey, result, DEFAULT_CACHE_TTL, "books");
        return result;
    }

    @Override
    public List<Book> searchByTitleForUser(String userId, String title) throws Exception {
        validateSearchInput(title);
        String cacheKey = "books_title_" + title.toLowerCase();
        return executeWithCaching(userId, cacheKey, DEFAULT_CACHE_TTL, "books",
            () -> bookDAO.findByTitle(title));
    }

    @Override
    public List<Book> searchByAuthorForUser(String userId, String author) throws Exception {
        validateSearchInput(author);
        String cacheKey = "books_author_" + author.toLowerCase();
        return executeWithCaching(userId, cacheKey, DEFAULT_CACHE_TTL, "books",
            () -> bookDAO.findByAuthor(author));
    }

    @Override
    public List<Book> searchByAuthorAndYearForUser(String userId, String author, int year) throws Exception {
        validateSearchInputAndYear(author, year);
        String cacheKey = "books_author_year_" + author.toLowerCase() + "_" + year;
        return executeWithCaching(userId, cacheKey, DEFAULT_CACHE_TTL, "books",
            () -> bookDAO.findByAuthorAndYear(author, year));
    }

    @Override
    public List<Book> searchByTitleAndYearForUser(String userId, String title, int year) throws Exception {
        validateSearchInputAndYear(title, year);
        String cacheKey = "books_title_year_" + title.toLowerCase() + "_" + year;
        return executeWithCaching(userId, cacheKey, DEFAULT_CACHE_TTL, "books",
            () -> bookDAO.findByTitleAndYear(title, year));
    }

    @Override
    public Book getBookDetailsForUser(String userId, int bookId) throws Exception {
        String cacheKey = "book_details_" + bookId;
        return executeWithCaching(userId, cacheKey, DEFAULT_CACHE_TTL, "books",
            () -> bookDAO.findByID(bookId));
    }

    @Override
    public List<ReviewManager> getAggregatedReviewsForUser(String userId, int bookId) throws Exception {
        String cacheKey = "book_reviews_" + bookId;
        return executeWithCaching(userId, cacheKey, REVIEWS_CACHE_TTL, "reviews",
            () -> bookDAO.getAggregatedReviews(bookId));
    }

    @Override
    public List<Book> getLibraryBooksForUser(String userId, int libraryId) throws Exception {
        String cacheKey = "library_books_" + libraryId;
        return executeWithCaching(userId, cacheKey, LIBRARY_CACHE_TTL, "libraries",
            () -> bookDAO.getLibraryBooks(libraryId));
    }

    @Override
    public void invalidateBooksCacheForUser(String userId) throws Exception {
        HybridCacheManager.invalidateByCategory(userId, "books");
        HybridCacheManager.invalidateByCategory(userId, "reviews");
        HybridCacheManager.invalidateByCategory(userId, "suggestions");
    }

    @Override
    public List<Book> searchGloballyPagedForUser(String userId, int offset, int limit) throws Exception {
        String cacheKey = "books_global_paged_" + offset + "_" + limit;
        return executeWithCaching(userId, cacheKey, DEFAULT_CACHE_TTL, "books",
            () -> bookDAO.findAllPaged(offset, limit));
    }

    @Override
    public List<Book> searchByTitlePagedForUser(String userId, String query, int offset, int limit) throws Exception {
        validateSearchInput(query);
        String cacheKey = "books_title_paged_" + query + "_" + offset + "_" + limit;
        return executeWithCaching(userId, cacheKey, DEFAULT_CACHE_TTL, "books",
            () -> bookDAO.findByTitlePaged(query, offset, limit));
    }

    @Override
    public List<Book> searchByAuthorPagedForUser(String userId, String author, int offset, int limit) throws Exception {
        validateSearchInput(author);
        String cacheKey = "books_author_paged_" + author + "_" + offset + "_" + limit;
        return executeWithCaching(userId, cacheKey, DEFAULT_CACHE_TTL, "books",
            () -> bookDAO.findByAuthorPaged(author, offset, limit));
    }
}
