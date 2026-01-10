package it.uninsubria.server.dao.impl;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;

import it.uninsubria.server.dao.SuggestionDAO;
import it.uninsubria.shared.model.Book;
import it.uninsubria.shared.model.SuggestionEntry;

public class SuggestionDAOImpl implements SuggestionDAO {
    private final DataSource ds;
    private final Connection singleConn;

    public SuggestionDAOImpl(DataSource ds) {
        this.ds = ds;
        this.singleConn = null;
    }

    public SuggestionDAOImpl(Connection conn) {
        this.ds = null;
        this.singleConn = conn;
    }

    private Connection getConnection() throws SQLException {
        if (singleConn != null) return singleConn;
        if (ds != null) return ds.getConnection();
        throw new SQLException("No DataSource or Connection configured for SuggestionDAOImpl");
    }

    @Override
    public List<SuggestionEntry> getSuggestionsForBaseBook(int baseBookId) {
        String sql = "SELECT suggested_id, user_id, libraries_id, base_book_id, suggested_book_id, created_at FROM Suggested_Books WHERE base_book_id = ?";
        List<SuggestionEntry> result = new ArrayList<>();
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, baseBookId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("suggested_id");
                    int userId = rs.getInt("user_id");
                    int libraryId = rs.getInt("libraries_id");
                    int baseId = rs.getInt("base_book_id");
                    int suggestedBookId = rs.getInt("suggested_book_id");
                    Timestamp ts = rs.getTimestamp("created_at");
                    result.add(new SuggestionEntry(id, userId, libraryId, baseId, suggestedBookId, ts));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    @Override
    public int addSuggestionReturningId(int userId, Integer libraryId, int baseBookId, int suggestedBookId)
            throws Exception {
        String countSql = "SELECT COALESCE(MAX(suggested_id), 0) + 1 FROM Suggested_Books WHERE user_id = ?";
        int suggestionId;
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(countSql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                suggestionId = rs.getInt(1);
            }
        }

        String insertSql = "INSERT INTO Suggested_Books (suggested_id, user_id, libraries_id, base_book_id, suggested_book_id) VALUES (?, ?, ?, ?, ?)";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(insertSql)) {
            ps.setInt(1, suggestionId);
            ps.setInt(2, userId);
            ps.setObject(3, libraryId, java.sql.Types.INTEGER);
            ps.setInt(4, baseBookId);
            ps.setInt(5, suggestedBookId);
            ps.executeUpdate();
        }
        return suggestionId;
    }
    

    @Override
    public boolean addSuggestion(int userId, Integer libraryId, int baseBookId, int suggestedBookId) {
        String sql = "INSERT INTO Suggested_Books (user_id, libraries_id, base_book_id, suggested_book_id) VALUES (?, ?, ?, ?)";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            if (libraryId != null) ps.setInt(2, libraryId); else ps.setNull(2, Types.INTEGER);
            ps.setInt(3, baseBookId);
            ps.setInt(4, suggestedBookId);
            int n = ps.executeUpdate();
            return n > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<SuggestionEntry> getAllUserSuggestions(int userId) throws Exception {
        String sql = "SELECT sb.suggested_id, sb.user_id, sb.libraries_id, sb.base_book_id, sb.suggested_book_id, sb.created_at " +
                    "FROM Suggested_Books sb WHERE sb.user_id = ? ORDER BY sb.created_at DESC";
        List<SuggestionEntry> suggestions = new ArrayList<>();
        try (Connection connection = getConnection();
            PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    suggestions.add(new SuggestionEntry(
                        rs.getInt("suggested_id"),
                        rs.getInt("user_id"),
                        rs.getInt("libraries_id"),
                        rs.getInt("base_book_id"),
                        rs.getInt("suggested_book_id"),
                        rs.getTimestamp("created_at")
                    ));
                }
            }
        }
        return suggestions;
    }

    @Override
    public List<SuggestionEntry> getUserSuggestionsForLibrary(int userId, int libraryId) throws Exception {
        String sql = "SELECT sb.suggested_id, sb.user_id, sb.libraries_id, sb.base_book_id, sb.suggested_book_id, sb.created_at " +
                    "FROM Suggested_Books sb WHERE sb.user_id = ? AND sb.libraries_id = ? ORDER BY sb.created_at DESC";
        List<SuggestionEntry> suggestions = new ArrayList<>();
        try (Connection connection = getConnection();
            PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, libraryId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    suggestions.add(new SuggestionEntry(
                        rs.getInt("suggested_id"),
                        rs.getInt("user_id"),
                        rs.getInt("libraries_id"),
                        rs.getInt("base_book_id"),
                        rs.getInt("suggested_book_id"),
                        rs.getTimestamp("created_at")
                    ));
                }
            }
        }
        return suggestions;
    }

    @Override
    public boolean deleteUserSuggestion(int userId, int baseBookId, int suggestedBookId) throws Exception {
        String sql = "DELETE FROM Suggested_Books WHERE user_id = ? AND book_id = ? AND suggested_book_id = ?";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, baseBookId);
            ps.setInt(3, suggestedBookId);
            int updated = ps.executeUpdate();
            return updated > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public List<Book> getUserSuggestions(int userId, int baseBookId) throws Exception {
        List<Book> list = new ArrayList<>();
        String sql = "SELECT b.* FROM Suggested_Books s JOIN Books b ON s.suggested_book_id = b.book_id WHERE s.user_id = ? AND s.book_id = ?";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, baseBookId);
            ResultSet rs = ps.executeQuery();
            while ( rs.next() ) {
                list.add(it.uninsubria.shared.model.Book.fromResultSet(rs));
            }
        } catch (SQLException e) {
            throw e;
        }
        return list;
    }

    @Override
    public List<Book> getUserSuggestionsForBookAndLibrary(int userId, int libraryId, int baseBookId) throws Exception {
        List<Book> list = new ArrayList<>();
        String sql = "SELECT b.* FROM Suggested_Books s JOIN Books b ON s.suggested_book_id = b.book_id WHERE s.user_id = ? AND s.libraries_id = ? AND s.base_book_id = ?";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, libraryId);
            ps.setInt(3, baseBookId);
            ResultSet rs = ps.executeQuery();
            while ( rs.next() ) {
                list.add(it.uninsubria.shared.model.Book.fromResultSet(rs));
            }
        } catch (SQLException e) {
            throw e;
        }
        return list;
    }

    @Override
    public int createBatch(int userId, Integer libraryId) throws Exception {
        // Calculate the next available suggested_id for this user
        // No need to insert a placeholder record - just return the next available ID
        String countSql = "SELECT COALESCE(MAX(suggested_id), 0) + 1 FROM Suggested_Books WHERE user_id = ?";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(countSql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    @Override
    public int addToBatch(int batchId, int userId, Integer libraryId, int baseBookId, int suggestedBookId) throws Exception {
        // Insert or update the suggestion - if it exists, update the batch_id
        String upsertSql = "INSERT INTO Suggested_Books (suggested_id, user_id, libraries_id, base_book_id, suggested_book_id) " +
                          "VALUES (?, ?, ?, ?, ?) " +
                          "ON CONFLICT (user_id, libraries_id, base_book_id, suggested_book_id) " +
                          "DO UPDATE SET suggested_id = EXCLUDED.suggested_id";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(upsertSql)) {
            ps.setInt(1, batchId);
            ps.setInt(2, userId);
            ps.setObject(3, libraryId, java.sql.Types.INTEGER);
            ps.setInt(4, baseBookId);
            ps.setInt(5, suggestedBookId);
            int rowsAffected = ps.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("Failed to insert or update suggestion - no rows affected");
            }
        }
        return batchId;
    }

    @Override
    public List<SuggestionEntry> getSuggestionsForBatch(int batchId) throws Exception {
        List<SuggestionEntry> result = new ArrayList<>();
        String sql = "SELECT * FROM Suggested_Books WHERE suggested_id = ? OR (user_id = (SELECT user_id FROM Suggested_Books WHERE suggested_id = ?) AND libraries_id = (SELECT libraries_id FROM Suggested_Books WHERE suggested_id = ?))";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, batchId);
            ps.setInt(2, batchId);
            ps.setInt(3, batchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("suggested_id");
                    int userId = rs.getInt("user_id");
                    int libraryId = rs.getInt("libraries_id");
                    int baseId = rs.getInt("base_book_id");
                    int suggestedBookId = rs.getInt("suggested_book_id");
                    Timestamp ts = rs.getTimestamp("created_at");
                    result.add(new SuggestionEntry(id, userId, libraryId, baseId, suggestedBookId, ts));
                }
            }
        } catch (SQLException e) {
            throw e;
        }
        return result;
    }

    @Override
    public boolean updateSuggestion(int userId, int suggestionId, int newSuggestedBookId) throws Exception {
        String sql = "UPDATE Suggested_Books SET suggested_book_id = ? WHERE user_id = ? AND suggested_id = ?";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, newSuggestedBookId);
            ps.setInt(2, userId);
            ps.setInt(3, suggestionId);
            int rows = ps.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            throw e;
        }
    }
}
