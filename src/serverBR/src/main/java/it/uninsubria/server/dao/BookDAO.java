package it.uninsubria.server.dao;

import java.util.List;

import it.uninsubria.shared.model.Book;
import it.uninsubria.shared.model.ReviewManager;

/**
 * Data Access Object interface for Book entities.
 * Provides methods to query and retrieve book data from the database.
 */
public interface BookDAO {
    /**
     * Retrieves detailed information for a specific book.
     *
     * @param bookId the book ID
     * @return the book details
     * @throws Exception if database access fails
     */
    Book bookDetails(int bookId) throws Exception;
    /**
     * Finds books by title.
     *
     * @param title the book title
     * @return a list of books with the specified title
     * @throws Exception if database access fails
     */
    List<Book> findByTitle(String title) throws Exception;
    /**
     * Finds books by author.
     *
     * @param author the author name
     * @return a list of books by the specified author
     * @throws Exception if database access fails
     */
    List<Book> findByAuthor(String author) throws Exception;
    /**
     * Finds books by author and publication year.
     *
     * @param author the author name
     * @param year the publication year
     * @return a list of books by the author in the specified year
     * @throws Exception if database access fails
     */
    List<Book> findByAuthorAndYear(String author, int year) throws Exception;
    /**
     * Finds books by title and publication year.
     *
     * @param title the book title
     * @param year the publication year
     * @return a list of books with the title in the specified year
     * @throws Exception if database access fails
     */
    List<Book> findByTitleAndYear(String title, int year) throws Exception;
    /**
     * Finds a book by its ID.
     *
     * @param bookId the book ID
     * @return the book with the specified ID
     * @throws Exception if database access fails
     */
    Book findByID(int bookId) throws Exception;
    /**
     * Retrieves all books.
     *
     * @return a list of all books
     * @throws Exception if database access fails
     */
    List<Book> findAll() throws Exception;
    /**
     * Gets suggested books for a given book.
     *
     * @param bookId the base book ID
     * @return a list of suggested books
     * @throws Exception if database access fails
     */
    List<Book> getSuggestedBooks(int bookId) throws Exception;
    /**
     * Gets aggregated reviews for a book.
     *
     * @param bookId the book ID
     * @return a list of aggregated reviews
     * @throws Exception if database access fails
     */
    List<ReviewManager> getAggregatedReviews(int bookId) throws Exception;
    /**
     * Gets books in a specific library.
     *
     * @param libraryId the library ID
     * @return a list of books in the library
     * @throws Exception if database access fails
     */
    List<Book> getLibraryBooks(int libraryId) throws Exception;

    // Metodi per paginazione
    /**
     * Retrieves all books with pagination.
     *
     * @param offset the offset for pagination
     * @param limit the maximum number of results
     * @return a list of books with pagination
     * @throws Exception if database access fails
     */
    List<Book> findAllPaged(int offset, int limit) throws Exception;
    /**
     * Finds books by title with pagination.
     *
     * @param title the book title
     * @param offset the offset for pagination
     * @param limit the maximum number of results
     * @return a list of books with pagination
     * @throws Exception if database access fails
     */
    List<Book> findByTitlePaged(String title, int offset, int limit) throws Exception;
    /**
     * Finds books by author with pagination.
     *
     * @param author the author name
     * @param offset the offset for pagination
     * @param limit the maximum number of results
     * @return a list of books with pagination
     * @throws Exception if database access fails
     */
    List<Book> findByAuthorPaged(String author, int offset, int limit) throws Exception;
    /**
     * Gets the total number of books.
     *
     * @return the total count of books
     * @throws Exception if database access fails
     */
    int getTotalBooksCount() throws Exception;
 }
