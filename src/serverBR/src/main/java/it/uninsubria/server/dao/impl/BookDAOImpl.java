package it.uninsubria.server.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

import it.uninsubria.server.dao.BookDAO;
import it.uninsubria.shared.model.Book;
import it.uninsubria.shared.model.Review;
import it.uninsubria.shared.model.ReviewManager;
import it.uninsubria.shared.model.User;

/**
 * Implementation of BookDAO for database operations on Book entities.
 */
public class BookDAOImpl implements BookDAO {
    private final Connection conn;
    private final DataSource ds;

    /**
     * Constructs a BookDAOImpl with a database connection.
     *
     * @param conn the database connection
     */
    public BookDAOImpl(Connection conn) {
        this.conn = conn;
        this.ds = null;
    }

    /**
     * Constructs a BookDAOImpl with a data source.
     *
     * @param ds the data source
     */
    public BookDAOImpl(DataSource ds) {
        this.ds = ds;
        this.conn = null;
    }

    private Connection getConnection() throws SQLException {
        if (conn != null) return conn;
        if (ds != null) return ds.getConnection();
        throw new SQLException("No DataSource or Connection configured for BookDAOImpl");
    }

    @Override
    public Book bookDetails(int bookId) throws Exception {
        return executeSingleBookQuery("SELECT * FROM Books WHERE book_id = ?", bookId);
    }

    @Override
    public List<Book> findAll() throws Exception {
        return executeBookListQuery("SELECT * FROM Books");
    }

    @Override
    public List<Book> findByTitle(String title) throws Exception {
        return executeBookListQuery("SELECT * FROM Books WHERE title ILIKE ?", buildLikePattern(title));
    }

    @Override
    public List<Book> findByAuthor(String author) throws Exception {
        return executeBookListQuery("SELECT * FROM Books WHERE authors ILIKE ?", buildLikePattern(author));
    }

    @Override
    public List<Book> findByAuthorAndYear(String author, int year) throws Exception {
        return executeBookListQuery("SELECT * FROM Books WHERE authors ILIKE ? AND publish_date_year = ?",
                                   buildLikePattern(author), year);
    }

    @Override
    public List<Book> findByTitleAndYear(String title, int year) throws Exception {
        return executeBookListQuery("SELECT * FROM Books WHERE title ILIKE ? AND publish_date_year = ?",
                                   buildLikePattern(title), year);
    }

    @Override
    public Book findByID(int bookId) throws Exception {
        return executeSingleBookQuery("SELECT * FROM Books WHERE book_id = ?", bookId);
    }

    @Override
    public List<ReviewManager> getAggregatedReviews(int bookId) throws Exception {
        return executeQuery(
            "SELECT br.*, u.names, u.surnames FROM book_reviews br " +
            "LEFT JOIN Users u ON br.user_id = u.user_id " +
            "WHERE br.book_id = ? ORDER BY br.book_reviews_id DESC",
            rs -> {
                ReviewManager rm = new ReviewManager();
                List<Review> reviews = new ArrayList<>();

                while (rs.next()) {
                    Review review = Review.fromResultSet(rs);

                    // Set user information if available
                    if (rs.getString("names") != null) {
                        User user = new User();
                        user.setName(rs.getString("names"));
                        user.setSurname(rs.getString("surnames"));
                        user.setUser_id(rs.getInt("user_id"));
                        review.setUserId(rs.getInt("user_id"));
                    }

                    reviews.add(review);
                }

                rm.setReviews(reviews);
                calculateReviewAverages(rm, reviews);

                List<ReviewManager> result = new ArrayList<>();
                result.add(rm);
                return result;
            },
            bookId
        );
    }

    private void calculateReviewAverages(ReviewManager rm, List<Review> reviews) {
        if (!reviews.isEmpty()) {
            rm.setAvgStyle(reviews.stream().mapToInt(Review::getStyle).average().orElse(0.0));
            rm.setAvgContent(reviews.stream().mapToInt(Review::getContent).average().orElse(0.0));
            rm.setAvgPleasantness(reviews.stream().mapToInt(Review::getPleasantness).average().orElse(0.0));
            rm.setAvgOdness(reviews.stream().mapToInt(Review::getOdness).average().orElse(0.0));
            rm.setAvgEditions(reviews.stream().mapToInt(Review::getEditions).average().orElse(0.0));
            rm.setAvgFinal(reviews.stream().mapToInt(Review::getFinalScore).average().orElse(0.0));
        }
    }

    @Override
    public List<Book> getSuggestedBooks(int bookId) throws Exception {
        return executeBookListQuery("SELECT b.* FROM Books b JOIN Suggested_Books s ON b.book_id = s.suggested_book_id WHERE s.base_book_id = ?", bookId);
    }

    @Override
    public List<Book> getLibraryBooks(int libraryId) throws Exception {
        return executeBookListQuery("SELECT b.* FROM Books b JOIN Books_Libraries bl ON b.book_id = bl.book_id WHERE bl.libraries_id = ?", libraryId);
    }

    @Override
    public List<Book> findAllPaged(int offset, int limit) throws Exception {
        return executeBookListQuery("SELECT * FROM Books ORDER BY book_id LIMIT ? OFFSET ?", limit, offset);
    }

    @Override
    public List<Book> findByTitlePaged(String title, int offset, int limit) throws Exception {
        return executeBookListQuery("SELECT * FROM Books WHERE LOWER(title) LIKE LOWER(?) ORDER BY book_id LIMIT ? OFFSET ?",
                                   buildLikePattern(title), limit, offset);
    }

    @Override
    public List<Book> findByAuthorPaged(String author, int offset, int limit) throws Exception {
        return executeBookListQuery("SELECT * FROM Books WHERE LOWER(authors) LIKE LOWER(?) ORDER BY book_id LIMIT ? OFFSET ?",
                                   buildLikePattern(author), limit, offset);
    }

    @Override
    public int getTotalBooksCount() throws Exception {
        return executeCountQuery("SELECT COUNT(*) FROM Books");
    }

    // Helper methods for common operations
    private <T> T executeQuery(String sql, QueryExecutor<T> executor, Object... params) throws Exception {
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            // Set parameters
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }

            try (ResultSet rs = ps.executeQuery()) {
                return executor.execute(rs);
            }
        } catch (SQLException e) {
            throw e;
        }
    }

    private List<Book> executeBookListQuery(String sql, Object... params) throws Exception {
        return executeQuery(sql, rs -> {
            List<Book> list = new ArrayList<>();
            while (rs.next()) {
                list.add(parseBook(rs));
            }
            return list;
        }, params);
    }

    private Book executeSingleBookQuery(String sql, Object... params) throws Exception {
        return executeQuery(sql, rs -> rs.next() ? parseBook(rs) : null, params);
    }

    private int executeCountQuery(String sql, Object... params) throws Exception {
        return executeQuery(sql, rs -> rs.next() ? rs.getInt(1) : 0, params);
    }

    private String buildLikePattern(String value) {
        return "%" + value + "%";
    }

    private Book parseBook(ResultSet rs) throws SQLException {
        Book book = new Book();
        book.setBook_id(rs.getInt("book_id"));
        book.setTitle(rs.getString("title"));
        book.setAuthors(rs.getString("authors"));
        book.setDescription(rs.getString("descriptions"));
        book.setCategory(rs.getString("category"));
        book.setPublisher(rs.getString("publisher"));
        book.setPrice(rs.getString("price"));
        book.setPublisher_month(rs.getString("publish_date_month"));
        book.setPublish_date_year(rs.getInt("publish_date_year"));
        return book;
    }

    @FunctionalInterface
    private interface QueryExecutor<T> {
        T execute(ResultSet rs) throws SQLException;
    }
}