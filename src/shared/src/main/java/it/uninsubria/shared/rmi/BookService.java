package it.uninsubria.shared.rmi;

import it.uninsubria.shared.model.SuggestionEntry;
import it.uninsubria.shared.model.Book;
import it.uninsubria.shared.model.ReviewManager;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Remote interface for book-related operations in the Book Recommender system.
 * Provides methods for book search, retrieval, caching, and suggestion management
 * via RMI (Remote Method Invocation).
 *
 * <p>This interface supports:
 * <ul>
 * <li>Global and filtered book searches</li>
 * <li>Paginated results for performance</li>
 * <li>Per-user caching for concurrent clients</li>
 * <li>Book suggestion and recommendation features</li>
 * <li>Review aggregation and retrieval</li>
 * </ul>
 *
 * @author Book Recommender Team
 * @version 1.0
 * @since 1.0
 */
public interface BookService extends Remote {
    /**
     * Retrieves all books from the system for global search.
     *
     * @return a list of all books in the system
     * @throws RemoteException if a communication error occurs
     */
    List<Book> searchGlobally() throws RemoteException;
    /**
     * Searches for books by title using the specified query string.
     *
     * @param query the search query for book titles
     * @return a list of books matching the title query
     * @throws RemoteException if a communication error occurs
     */
    List<Book> searchByTitle(String query) throws RemoteException;
    /**
     * Searches for books by author.
     *
     * @param author the author name
     * @return a list of books by the author
     * @throws RemoteException if a communication error occurs
     */
    List<Book> searchByAuthor(String author) throws RemoteException;
    /**
     * Searches for books by author and publication year.
     *
     * @param author the author name
     * @param year the publication year
     * @return a list of books by the author in the specified year
     * @throws RemoteException if a communication error occurs
     */
    List<Book> searchByAuthorAndYear(String author, int year) throws RemoteException;
    /**
     * Searches for books by title and publication year.
     *
     * @param title the book title
     * @param year the publication year
     * @return a list of books with the title in the specified year
     * @throws RemoteException if a communication error occurs
     */
    List<Book> searchByTitleAndYear(String title, int year) throws RemoteException;

    // Metodi per paginazione e lazy loading
    /**
     * Retrieves all books with pagination.
     *
     * @param offset the offset for pagination
     * @param limit the maximum number of results
     * @return a list of books with pagination
     * @throws RemoteException if a communication error occurs
     */
    List<Book> searchGloballyPaged(int offset, int limit) throws RemoteException;
    /**
     * Searches for books by title with pagination.
     *
     * @param query the search query
     * @param offset the offset for pagination
     * @param limit the maximum number of results
     * @return a list of books matching the query with pagination
     * @throws RemoteException if a communication error occurs
     */
    List<Book> searchByTitlePaged(String query, int offset, int limit) throws RemoteException;
    /**
     * Searches for books by author with pagination.
     *
     * @param author the author name
     * @param offset the offset for pagination
     * @param limit the maximum number of results
     * @return a list of books by the author with pagination
     * @throws RemoteException if a communication error occurs
     */
    List<Book> searchByAuthorPaged(String author, int offset, int limit) throws RemoteException;
    /**
     * Gets the total number of books in the system.
     *
     * @return the total count of books
     * @throws RemoteException if a communication error occurs
     */
    int getTotalBooksCount() throws RemoteException;
    /**
     * Retrieves detailed information for a specific book by its ID.
     *
     * @param bookId the unique identifier of the book
     * @return the book object with full details, or null if not found
     * @throws RemoteException if a communication error occurs
     */
    Book getBookDetails(int bookId) throws RemoteException;
    /**
     * Gets aggregated reviews for a book.
     *
     * @param bookId the book ID
     * @return a list of aggregated reviews
     * @throws RemoteException if a communication error occurs
     */
    List<ReviewManager> getAggregatedReviews(int bookId) throws RemoteException;
    /**
     * Gets books in a specific library.
     *
     * @param libraryId the library ID
     * @return a list of books in the library
     * @throws RemoteException if a communication error occurs
     */
    List<Book> getLibraryBooks(int libraryId) throws RemoteException;
    /**
     * Gets suggested books for a given book.
     *
     * @param bookId the base book ID
     * @return a list of suggested books
     * @throws RemoteException if a communication error occurs
     */
    List<Book> getSuggestedBooks(int bookId) throws RemoteException;
    /**
     * Invalidates the global books cache.
     *
     * @throws RemoteException if a communication error occurs
     */
    void invalidateBooksCache() throws RemoteException;

    /**
     * Adds a user suggestion and returns the ID.
     *
     * @param userId the user ID
     * @param baseBookId the base book ID
     * @param libraryId the library ID (nullable)
     * @param suggestedBookId the suggested book ID
     * @return the suggestion ID
     * @throws RemoteException if a communication error occurs
     */
    int addUserSuggestionReturningId(int userId, int baseBookId, Integer libraryId, int suggestedBookId) throws RemoteException;
    /**
     * Adds a user suggestion.
     *
     * @param userId the user ID
     * @param baseBookId the base book ID
     * @param libraryId the library ID (nullable)
     * @param suggestedBookId the suggested book ID
     * @return true if added successfully
     * @throws RemoteException if a communication error occurs
     */
    boolean addUserSuggestion(int userId, int baseBookId, Integer libraryId, int suggestedBookId) throws RemoteException;
    /**
     * Deletes a user suggestion.
     *
     * @param userId the user ID
     * @param baseBookId the base book ID
     * @param suggestedBookId the suggested book ID
     * @return true if deleted successfully
     * @throws RemoteException if a communication error occurs
     */
    boolean deleteUserSuggestion(int userId, int baseBookId, int suggestedBookId) throws RemoteException;
    /**
     * Gets all user suggestions.
     *
     * @param userId the user ID
     * @return a list of user suggestions
     * @throws RemoteException if a communication error occurs
     */
    List<SuggestionEntry> getAllUserSuggestions(int userId) throws RemoteException;
    /**
     * Gets user suggestions for a library.
     *
     * @param userId the user ID
     * @param libraryId the library ID
     * @return a list of suggestions for the library
     * @throws RemoteException if a communication error occurs
     */
    List<SuggestionEntry> getUserSuggestionsForLibrary(int userId, int libraryId) throws RemoteException;
    /**
     * Gets user suggestions for a specific book in a library.
     *
     * @param userId the user ID
     * @param libraryId the library ID
     * @param baseBookId the base book ID
     * @return a list of suggested books
     * @throws RemoteException if a communication error occurs
     */
    List<Book> getUserSuggestionsForBookAndLibrary(int userId, int libraryId, int baseBookId) throws RemoteException;
    /**
     * Creates a suggestion batch.
     *
     * @param userId the user ID
     * @param libraryId the library ID (nullable)
     * @return the batch ID
     * @throws RemoteException if a communication error occurs
     */
    int createSuggestionBatch(int userId, Integer libraryId) throws RemoteException;
    /**
     * Adds suggestions to a batch.
     *
     * @param batchId the batch ID
     * @param userId the user ID
     * @param libraryId the library ID (nullable)
     * @param baseBookId the base book ID
     * @param suggestedBookIds the list of suggested book IDs
     * @return the list of added suggestion IDs
     * @throws RemoteException if a communication error occurs
     */
    List<Integer> addSuggestionsToBatch(int batchId, int userId, Integer libraryId, int baseBookId, List<Integer> suggestedBookIds) throws RemoteException;

    // Metodi per cache per-user (per concorrenza multiclient)
    /**
     * Searches globally for a user.
     *
     * @param userId the user ID
     * @return a list of books for the user
     * @throws RemoteException if a communication error occurs
     */
    List<Book> searchGloballyForUser(String userId) throws RemoteException;
    /**
     * Searches by title for a user.
     *
     * @param userId the user ID
     * @param title the book title
     * @return a list of books for the user
     * @throws RemoteException if a communication error occurs
     */
    List<Book> searchByTitleForUser(String userId, String title) throws RemoteException;
    /**
     * Searches by author for a user.
     *
     * @param userId the user ID
     * @param author the author name
     * @return a list of books for the user
     * @throws RemoteException if a communication error occurs
     */
    List<Book> searchByAuthorForUser(String userId, String author) throws RemoteException;
    /**
     * Searches by author and year for a user.
     *
     * @param userId the user ID
     * @param author the author name
     * @param year the publication year
     * @return a list of books for the user
     * @throws RemoteException if a communication error occurs
     */
    List<Book> searchByAuthorAndYearForUser(String userId, String author, int year) throws RemoteException;
    /**
     * Searches by title and year for a user.
     *
     * @param userId the user ID
     * @param title the book title
     * @param year the publication year
     * @return a list of books for the user
     * @throws RemoteException if a communication error occurs
     */
    List<Book> searchByTitleAndYearForUser(String userId, String title, int year) throws RemoteException;
    /**
     * Gets book details for a user.
     *
     * @param userId the user ID
     * @param bookId the book ID
     * @return the book details
     * @throws RemoteException if a communication error occurs
     */
    Book getBookDetailsForUser(String userId, int bookId) throws RemoteException;
    /**
     * Gets aggregated reviews for a user and book.
     *
     * @param userId the user ID
     * @param bookId the book ID
     * @return a list of aggregated reviews
     * @throws RemoteException if a communication error occurs
     */
    List<ReviewManager> getAggregatedReviewsForUser(String userId, int bookId) throws RemoteException;
    /**
     * Gets library books for a user.
     *
     * @param userId the user ID
     * @param libraryId the library ID
     * @return a list of books in the library for the user
     * @throws RemoteException if a communication error occurs
     */
    List<Book> getLibraryBooksForUser(String userId, int libraryId) throws RemoteException;
    /**
     * Invalidates books cache for a user.
     *
     * @param userId the user ID
     * @throws RemoteException if a communication error occurs
     */
    void invalidateBooksCacheForUser(String userId) throws RemoteException;

    // Metodi per paginazione per-user
    /**
     * Searches globally with pagination for a user.
     *
     * @param userId the user ID
     * @param offset the offset
     * @param limit the limit
     * @return a list of books
     * @throws RemoteException if a communication error occurs
     */
    List<Book> searchGloballyPagedForUser(String userId, int offset, int limit) throws RemoteException;
    /**
     * Searches by title with pagination for a user.
     *
     * @param userId the user ID
     * @param query the search query
     * @param offset the offset
     * @param limit the limit
     * @return a list of books
     * @throws RemoteException if a communication error occurs
     */
    List<Book> searchByTitlePagedForUser(String userId, String query, int offset, int limit) throws RemoteException;
    /**
     * Searches by author with pagination for a user.
     *
     * @param userId the user ID
     * @param author the author name
     * @param offset the offset
     * @param limit the limit
     * @return a list of books
     * @throws RemoteException if a communication error occurs
     */
    List<Book> searchByAuthorPagedForUser(String userId, String author, int offset, int limit) throws RemoteException;
}
