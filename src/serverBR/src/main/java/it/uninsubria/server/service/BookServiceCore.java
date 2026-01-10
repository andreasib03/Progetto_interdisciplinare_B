package it.uninsubria.server.service;

import java.util.List;

import it.uninsubria.shared.model.Book;
import it.uninsubria.shared.model.ReviewManager;

/**
 * Core service interface for book-related operations.
 * <p>
 * This service provides comprehensive book management functionality including:
 * </p>
 * <ul>
 *   <li>Global and user-specific book searches</li>
 *   <li>Book details and reviews retrieval</li>
 *   <li>Library book management</li>
 *   <li>Intelligent caching with automatic invalidation</li>
 *   <li>Paginated search for large datasets</li>
 * </ul>
 * 
 * <p>
 * All methods that retrieve data support caching at the service layer level
 * for improved performance. Cache invalidation can be triggered manually
 * through {@link #invalidateBooksCache()} or {@link #invalidateBooksCacheForUser(String)}.
 * </p>
 * 
 * <p>
 * This service is the business logic layer for book-related operations,
 * managing interactions with the database through BookDAO.
 * </p>
 */
public interface BookServiceCore {

    /**
     * Searches for all books in the catalog (global search).
     * <p>
     * Results are cached for performance. Large datasets are loaded
     * in batches with the first batch returned immediately.
     * </p>
     * 
     * @return list of all books in the catalog
     * @throws Exception if search fails due to database or cache errors
     */
    List<Book> searchGlobally() throws Exception;

    /**
     * Searches for books by title (global search).
     * 
     * @param title the book title to search for (supports partial matching)
     * @return list of books matching the title
     * @throws Exception if search fails
     */
    List<Book> searchByTitle(String title) throws Exception;

    /**
     * Searches for books by author name (global search).
     * 
     * @param author the author name to search for (supports partial matching)
     * @return list of books by the specified author
     * @throws Exception if search fails
     */
    List<Book> searchByAuthor(String author) throws Exception;

    /**
     * Searches for books by author and publication year (global search).
     * 
     * @param author the author name to search for
     * @param year the publication year to filter by
     * @return list of books matching both author and year
     * @throws Exception if search fails
     */
    List<Book> searchByAuthorAndYear(String author, int year) throws Exception;

    /**
     * Searches for books by title and publication year (global search).
     * 
     * @param title the book title to search for
     * @param year the publication year to filter by
     * @return list of books matching both title and year
     * @throws Exception if search fails
     */
    List<Book> searchByTitleAndYear(String title, int year) throws Exception;

    /**
     * Retrieves detailed information about a specific book.
     * 
     * @param bookId the unique identifier of the book
     * @return Book object with complete details if found
     * @throws Exception if retrieval fails
     */
    Book getBookDetails(int bookId) throws Exception;

    /**
     * Retrieves all reviews for a specific book, aggregated by average scores.
     * 
     * @param bookId the unique identifier of the book
     * @return list of aggregated review information
     * @throws Exception if retrieval fails
     */
    List<ReviewManager> getAggregatedReviews(int bookId) throws Exception;

    /**
     * Retrieves all books in a specific library.
     * 
     * @param libraryId the unique identifier of the library
     * @return list of books in the specified library
     * @throws Exception if retrieval fails
     */
    List<Book> getLibraryBooks(int libraryId) throws Exception;

    /**
     * Invalidates the global book cache, forcing subsequent searches
     * to fetch fresh data from the database.
     * 
     * @throws Exception if cache invalidation fails
     */
    void invalidateBooksCache() throws Exception;

    /**
     * Performs paginated global search for books.
     * <p>
     * Useful for handling large datasets efficiently by loading
     * results in chunks rather than all at once.
     * </p>
     * 
     * @param offset the number of records to skip (for pagination)
     * @param limit the maximum number of records to return
     * @return paginated list of books
     * @throws Exception if search fails
     */
    List<Book> searchGloballyPaged(int offset, int limit) throws Exception;

    /**
     * Performs paginated search for books by title.
     * 
     * @param query the title to search for
     * @param offset the number of records to skip
     * @param limit the maximum number of records to return
     * @return paginated list of books matching the title
     * @throws Exception if search fails
     */
    List<Book> searchByTitlePaged(String query, int offset, int limit) throws Exception;

    /**
     * Performs paginated search for books by author.
     * 
     * @param author the author name to search for
     * @param offset the number of records to skip
     * @param limit the maximum number of records to return
     * @return paginated list of books by the specified author
     * @throws Exception if search fails
     */
    List<Book> searchByAuthorPaged(String author, int offset, int limit) throws Exception;

    /**
     * Retrieves the total count of books in the catalog.
     * <p>
     * Useful for displaying pagination controls and total result counts.
     * </p>
     * 
     * @return total number of books in the database
     * @throws Exception if count fails
     */
    int getTotalBooksCount() throws Exception;

    /**
     * Searches for all books for a specific user (user-specific cache).
     * <p>
     * Results are cached separately per user to prevent cache pollution
     * between different users' searches.
     * </p>
     * 
     * @param userId the unique identifier of the user
     * @return list of all books
     * @throws Exception if search fails
     */
    List<Book> searchGloballyForUser(String userId) throws Exception;

    /**
     * Searches for books by title for a specific user.
     * 
     * @param userId the unique identifier of the user
     * @param title the book title to search for
     * @return list of books matching the title for this user
     * @throws Exception if search fails
     */
    List<Book> searchByTitleForUser(String userId, String title) throws Exception;

    /**
     * Searches for books by author for a specific user.
     * 
     * @param userId the unique identifier of the user
     * @param author the author name to search for
     * @return list of books by the specified author for this user
     * @throws Exception if search fails
     */
    List<Book> searchByAuthorForUser(String userId, String author) throws Exception;

    /**
     * Searches for books by author and year for a specific user.
     * 
     * @param userId the unique identifier of the user
     * @param author the author name to search for
     * @param year the publication year to filter by
     * @return list of books matching both author and year for this user
     * @throws Exception if search fails
     */
    List<Book> searchByAuthorAndYearForUser(String userId, String author, int year) throws Exception;

    /**
     * Searches for books by title and year for a specific user.
     * 
     * @param userId the unique identifier of the user
     * @param title the book title to search for
     * @param year the publication year to filter by
     * @return list of books matching both title and year for this user
     * @throws Exception if search fails
     */
    List<Book> searchByTitleAndYearForUser(String userId, String title, int year) throws Exception;

    /**
     * Retrieves detailed information about a specific book for a user.
     * 
     * @param userId the unique identifier of the user
     * @param bookId the unique identifier of the book
     * @return Book object with complete details if found
     * @throws Exception if retrieval fails
     */
    Book getBookDetailsForUser(String userId, int bookId) throws Exception;

    /**
     * Retrieves all reviews for a specific book, aggregated by average scores (user-specific).
     * 
     * @param userId the unique identifier of the user
     * @param bookId the unique identifier of the book
     * @return list of aggregated review information
     * @throws Exception if retrieval fails
     */
    List<ReviewManager> getAggregatedReviewsForUser(String userId, int bookId) throws Exception;

    /**
     * Retrieves all books in a specific library for a user.
     * 
     * @param userId the unique identifier of the user
     * @param libraryId the unique identifier of the library
     * @return list of books in the specified library for this user
     * @throws Exception if retrieval fails
     */
    List<Book> getLibraryBooksForUser(String userId, int libraryId) throws Exception;

    /**
     * Invalidates the book cache for a specific user.
     * <p>
     * Only affects the specified user's cache, leaving other users'
     * cached data intact.
     * </p>
     * 
     * @param userId the unique identifier of the user whose cache should be invalidated
     * @throws Exception if cache invalidation fails
     */
    void invalidateBooksCacheForUser(String userId) throws Exception;

    /**
     * Performs paginated global search for books (user-specific cache).
     * 
     * @param userId the unique identifier of the user
     * @param offset the number of records to skip
     * @param limit the maximum number of records to return
     * @return paginated list of books
     * @throws Exception if search fails
     */
    List<Book> searchGloballyPagedForUser(String userId, int offset, int limit) throws Exception;

    /**
     * Performs paginated search for books by title for a specific user.
     * 
     * @param userId the unique identifier of the user
     * @param query the title to search for
     * @param offset the number of records to skip
     * @param limit the maximum number of records to return
     * @return paginated list of books matching the title for this user
     * @throws Exception if search fails
     */
    List<Book> searchByTitlePagedForUser(String userId, String query, int offset, int limit) throws Exception;

    /**
     * Performs paginated search for books by author for a specific user.
     * 
     * @param userId the unique identifier of the user
     * @param author the author name to search for
     * @param offset the number of records to skip
     * @param limit the maximum number of records to return
     * @return paginated list of books by the specified author for this user
     * @throws Exception if search fails
     */
    List<Book> searchByAuthorPagedForUser(String userId, String author, int offset, int limit) throws Exception;
}
