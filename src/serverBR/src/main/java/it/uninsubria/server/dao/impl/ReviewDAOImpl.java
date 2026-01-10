package it.uninsubria.server.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

import it.uninsubria.server.dao.ReviewDAO;
import it.uninsubria.shared.model.User;
import it.uninsubria.shared.model.Review;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReviewDAOImpl implements ReviewDAO{
    private static final Logger logger = LoggerFactory.getLogger(ReviewDAOImpl.class);
    private final Connection conn;
    private final DataSource ds;

    public ReviewDAOImpl(Connection conn) {
        this.conn = conn;
        this.ds = null;
    }

    public ReviewDAOImpl(DataSource ds) {
        this.ds = ds;
        this.conn = null;
    }

    private Connection getConnection() throws SQLException {
        if (conn != null) return conn;
        if (ds != null) return ds.getConnection();
        throw new SQLException("No DataSource or Connection configured for ReviewDAOImpl");
    }



    @Override
    public boolean addReview(int userId, int book_id, Review v) throws Exception {
        String sql = "INSERT INTO book_reviews ("+
                     " user_id, book_id, libraries_id, style, style_note, content, content_note, pleasentness, pleasentness_note, odness, odness_note, editions, editions_note, final_note_score, final_score"+
                     ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, book_id);
            ps.setObject(3, v.getLibraryId(), java.sql.Types.INTEGER);
            ps.setInt(4, v.getStyle());
            ps.setString(5, v.getStyleNote());
            ps.setInt(6, v.getContent());
            ps.setString(7, v.getContentNote());
            ps.setInt(8, v.getPleasantness());
            ps.setString(9, v.getPleasantnessNote());
            ps.setInt(10, v.getOdness());
            ps.setString(11, v.getOdnessNote());
            ps.setInt(12, v.getEditions());
            ps.setString(13, v.getEditionsNote());
            ps.setString(14, v.getFinalNoteScore());
            ps.setInt(15, v.getFinalScore());
            int rows = ps.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            logger.error("Database error in addReview", e);
            throw e;
        }
    }

    @Override
    public List<Review> getReviewsForBookAndUser(int userId, int bookId) throws Exception {
        List<Review> reviews = new ArrayList<>();
        String sql = "SELECT * FROM book_reviews WHERE user_id = ? AND book_id = ?";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Review review = new Review();
                    review.setUserId(rs.getInt("user_id"));
                    review.setBookId(rs.getInt("book_id"));
                    review.setLibraryId(rs.getInt("libraries_id"));
                    review.setStyle(rs.getInt("style"));
                    review.setStyleNote(rs.getString("style_note"));
                    review.setContent(rs.getInt("content"));
                    review.setContentNote(rs.getString("content_note"));
                    review.setPleasantness(rs.getInt("pleasentness"));
                    review.setPleasantnessNote(rs.getString("pleasentness_note"));
                    review.setOdness(rs.getInt("odness"));
                    review.setOdnessNote(rs.getString("odness_note"));
                    review.setEditions(rs.getInt("editions"));
                    review.setEditionsNote(rs.getString("editions_note"));
                    review.setFinalNoteScore(rs.getString("final_note_score"));
                    review.setFinalScore(rs.getInt("final_score"));
                    reviews.add(review);
                }
            }
        } catch (SQLException e) {
            logger.error("Database error in getReviewsForBookAndUser", e);
            throw e;
        }
        return reviews;
    }



    @Override
    public List<Review> getReviewsByBook(int userId, int bookId) throws Exception {
        List<Review> reviews = new ArrayList<>();
        String sql = "SELECT br.*, u.names, u.surnames, u.userid FROM book_reviews br " +
                     "JOIN users u ON br.user_id = u.user_id WHERE br.user_id = ? AND br.book_id = ?";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Review review = new Review();
                    review.setUserId(rs.getInt("user_id"));
                    review.setBookId(rs.getInt("book_id"));
                    review.setLibraryId(rs.getInt("libraries_id"));
                    review.setStyle(rs.getInt("style"));
                    review.setStyleNote(rs.getString("style_note"));
                    review.setContent(rs.getInt("content"));
                    review.setContentNote(rs.getString("content_note"));
                    review.setPleasantness(rs.getInt("pleasentness"));
                    review.setPleasantnessNote(rs.getString("pleasentness_note"));
                    review.setOdness(rs.getInt("odness"));
                    review.setOdnessNote(rs.getString("odness_note"));
                    review.setEditions(rs.getInt("editions"));
                    review.setEditionsNote(rs.getString("editions_note"));
                    review.setFinalNoteScore(rs.getString("final_note_score"));
                    review.setFinalScore(rs.getInt("final_score"));

                    // Crea e popola l'oggetto User
                    User user = new User();
                    user.setUser_id(rs.getInt("user_id"));
                    user.setID(rs.getString("userid"));
                    user.setName(rs.getString("names"));
                    user.setSurname(rs.getString("surnames"));
                    review.setUser(user);

                    reviews.add(review);
                }
            }
        } catch (SQLException e) {
            logger.error("Database error in getReviewsByBook", e);
            throw e;
        }
        return reviews;
    }

    @Override
    public boolean updateReview(int userId, int bookId, Review v) throws Exception {
        String sql = "UPDATE book_reviews SET style = ?, style_note = ?, content = ?, content_note = ?, pleasentness = ?, pleasentness_note = ?, odness = ?, odness_note = ?, editions = ?, editions_note = ?, final_note_score = ?, final_score = ? WHERE user_id = ? AND book_id = ?";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, v.getStyle());
            ps.setString(2, v.getStyleNote());
            ps.setInt(3, v.getContent());
            ps.setString(4, v.getContentNote());
            ps.setInt(5, v.getPleasantness());
            ps.setString(6, v.getPleasantnessNote());
            ps.setInt(7, v.getOdness());
            ps.setString(8, v.getOdnessNote());
            ps.setInt(9, v.getEditions());
            ps.setString(10, v.getEditionsNote());
            ps.setString(11, v.getFinalNoteScore());
            ps.setInt(12, v.getFinalScore());
            ps.setInt(13, userId);
            ps.setInt(14, bookId);
            int rows = ps.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            logger.error("Database error in updateReview", e);
            throw e;
        }
    }

    @Override
    public List<Review> getReviewsByBook(int bookId) throws Exception {
        List<Review> reviews = new ArrayList<>();
        String sql = "SELECT br.*, u.names, u.surnames, u.userid FROM book_reviews br " +
                     "JOIN users u ON br.user_id = u.user_id WHERE br.book_id = ?";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Review review = new Review();
                    review.setUserId(rs.getInt("user_id"));
                    review.setBookId(rs.getInt("book_id"));
                    review.setStyle(rs.getInt("style"));
                    review.setStyleNote(rs.getString("style_note"));
                    review.setContent(rs.getInt("content"));
                    review.setContentNote(rs.getString("content_note"));
                    review.setPleasantness(rs.getInt("pleasentness"));
                    review.setPleasantnessNote(rs.getString("pleasentness_note"));
                    review.setOdness(rs.getInt("odness"));
                    review.setOdnessNote(rs.getString("odness_note"));
                    review.setEditions(rs.getInt("editions"));
                    review.setEditionsNote(rs.getString("editions_note"));
                    review.setFinalNoteScore(rs.getString("final_note_score"));
                    review.setFinalScore(rs.getInt("final_score"));

                    // Crea e popola l'oggetto User
                    User user = new User();
                    user.setUser_id(rs.getInt("user_id"));
                    user.setID(rs.getString("userid"));
                    user.setName(rs.getString("names"));
                    user.setSurname(rs.getString("surnames"));
                    review.setUser(user);

                    reviews.add(review);
                }
            }
        } catch (SQLException e) {
            logger.error("Database error in getReviewsByBook", e);
            throw e;
        }
        return reviews;
    }
}