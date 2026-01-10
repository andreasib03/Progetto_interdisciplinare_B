 package it.uninsubria.server.dao.impl;

 import java.sql.Connection;
 import java.sql.PreparedStatement;
 import java.sql.ResultSet;
 import java.sql.SQLException;
 import java.util.logging.Logger;
 import javax.sql.DataSource;
 import java.util.ArrayList;
 import java.util.List;

import it.uninsubria.server.ServerMessageManager;
import it.uninsubria.server.dao.LibraryDAO;
 import it.uninsubria.shared.model.Book;
 import it.uninsubria.shared.model.Library;

 public class LibraryDAOImpl implements LibraryDAO {

     private static final Logger logger = Logger.getLogger(LibraryDAOImpl.class.getName());

    private final Connection conn;
    private final DataSource ds;
    public LibraryDAOImpl(Connection conn) {
        this.conn = conn;
        this.ds = null;
    }

    public LibraryDAOImpl(DataSource ds) {
        this.ds = ds;
        this.conn = null;
    }

    private Connection getConnection() throws SQLException {
        if (conn != null) return conn;
        if (ds != null) return ds.getConnection();
        throw new SQLException(ServerMessageManager.getString("server.error.dao.datasource"));
    }

    // validations
    private void validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException(ServerMessageManager.getString("server.error.dao.username.invalid"));
        }
        if (username.length() >100) {
            throw new IllegalArgumentException(ServerMessageManager.getString("server.error.dao.username.too.long"));
        }
    }

    private void validateLibraryName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException(ServerMessageManager.getString("server.error.dao.library.name.invalid"));
        }
        if (name.length() > 100) {
            throw new IllegalArgumentException(ServerMessageManager.getString("server.error.dao.library.name.too.long"));
        }
    }

    private void validateBooksList(List<Book> libri) {
        if (libri == null) {
            throw new IllegalArgumentException(ServerMessageManager.getString("server.error.dao.library.books.null"));
        }
        for (Book b : libri) {
            if (b == null) throw new IllegalArgumentException(ServerMessageManager.getString("server.error.dao.library.book.null"));
            if (b.getBook_id() <= 0) throw new IllegalArgumentException(ServerMessageManager.getString("server.error.dao.library.book.id.invalid"));
        }
    }

    @Override
    public List<Library> getUserLibraries(String username) throws Exception {
        validateUsername(username);
        logger.fine("Getting libraries for user '" + username + "'");
        List<Library> libraries = new ArrayList<>();
        String sql = "SELECT l.library_id, l.name_library FROM Library l WHERE l.user_id = (SELECT user_id FROM Users WHERE userid = ?)";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logger.fine("Found library ID " + rs.getInt("library_id") + ", name: " + rs.getString("name_library"));
                    Library library = new Library(rs.getInt("library_id"), username, rs.getString("name_library"));
                    library.setLibriDisponibili(getBooksForLibrary(rs.getInt("library_id")));
                    libraries.add(library);
                }
            }
        }
        logger.fine("Total libraries found: " + libraries.size());
        return libraries;
    }

    @Override
    public List<Integer> getBooksInLibraries(int libreriaId) throws Exception {
        List<Integer> bookIds = new ArrayList<>();
        String sql = "SELECT book_id FROM Books_Libraries WHERE libraries_id = ?";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, libreriaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    bookIds.add(rs.getInt("book_id"));
                }
            }
        }
        return bookIds;
    }

    private List<Book> getBooksForLibrary(int libraryId) throws Exception {
        List<Book> books = new ArrayList<>();
        String sql = "SELECT b.* FROM Books b JOIN Books_Libraries bl ON b.book_id = bl.book_id WHERE bl.libraries_id = ?";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, libraryId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Book book = new Book(
                        rs.getInt("book_id"),
                        rs.getString("title"),
                        rs.getString("authors"),
                        rs.getInt("publish_date_year")
                    );
                    book.setDescription(rs.getString("descriptions"));
                    book.setCategory(rs.getString("category"));
                    book.setPublisher(rs.getString("publisher"));
                    book.setPrice(rs.getString("price"));
                    book.setPublisher_month(rs.getString("publish_date_month"));
                    books.add(book);
                }
            }
        }
        return books;
    }

    @Override
    public boolean libraryExistsForUser(String username, String libraryName) throws Exception {
        validateUsername(username);
        validateLibraryName(libraryName);
        String sql = "SELECT COUNT(*) FROM Library l WHERE l.user_id = (SELECT user_id FROM Users WHERE userid = ?) AND l.name_library = ?";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, libraryName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    @Override
    public Library addLibraryForUser(String username, Library library) throws Exception {
        validateUsername(username);
        validateLibraryName(library.getName());
        validateBooksList(library.getLibriDisponibili());

        logger.fine("Adding library '" + library.getName() + "' for user '" + username + "' with " + library.getLibriDisponibili().size() + " books");
        for (Book book : library.getLibriDisponibili()) {
            logger.fine("  - Book: " + book.getTitle() + " (ID: " + book.getBook_id() + ")");
        }

        // Verifica che i libri non siano già presenti in altre librerie dell'utente
        validateBooksNotInOtherLibraries(username, library.getLibriDisponibili());

        String sql = "INSERT INTO Library (name_library, user_id) VALUES (?, (SELECT user_id FROM Users WHERE userid = ?)) RETURNING library_id";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, library.getName());
            ps.setString(2, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    library.setId(rs.getInt(1));
                    // Add books
                    addBooksToLibrary(library.getId(), library.getLibriDisponibili());
                    if (c.getAutoCommit() == false) {
                        c.commit();
                        logger.fine("Transaction committed for library creation");
                    }
                }
            }
        }
        return library;
    }

    @Override
    public void updateLibraryForUser(String username, Library oldLibrary, Library newLibrary) throws Exception {
        validateUsername(username);
        if (newLibrary == null) {
            throw new IllegalArgumentException(ServerMessageManager.getString("server.error.dao.library.name.null"));
        }
        validateLibraryName(newLibrary.getName());
        validateBooksList(newLibrary.getLibriDisponibili());

        // Verifica che i libri non siano già presenti in altre librerie dell'utente (escludendo la libreria corrente)
        validateBooksNotInOtherLibrariesExcludingCurrent(username, newLibrary.getLibriDisponibili(), oldLibrary.getId());

        String sql = "UPDATE Library SET name_library = ? WHERE library_id = ? AND user_id = (SELECT user_id FROM Users WHERE userid = ?)";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, newLibrary.getName());
            ps.setInt(2, oldLibrary.getId());
            ps.setString(3, username);
            ps.executeUpdate();
            // Update books
            updateBooksInLibrary(oldLibrary.getId(), newLibrary.getLibriDisponibili());
        }
    }

    @Override
    public void deleteLibraryForUser(String username, Library library) throws Exception {
        validateUsername(username);
        logger.fine("Deleting library '" + library.getName() + "' (ID: " + library.getId() + ") for user '" + username + "'");

        // First delete all book associations for this library
        String deleteBooksSql = "DELETE FROM Books_Libraries WHERE libraries_id = ?";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(deleteBooksSql)) {
            ps.setInt(1, library.getId());
            int booksDeleted = ps.executeUpdate();
            logger.fine("Deleted " + booksDeleted + " book associations for library ID " + library.getId());
        }

        // Then delete the library itself
        String sql = "DELETE FROM Library WHERE library_id = ? AND user_id = (SELECT user_id FROM Users WHERE userid = ?)";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, library.getId());
            ps.setString(2, username);
            int librariesDeleted = ps.executeUpdate();
            logger.fine("Deleted " + librariesDeleted + " libraries with ID " + library.getId());
            if (c.getAutoCommit() == false) {
                c.commit();
                logger.fine("Transaction committed");
            }
        }
    }



    private void validateBooksNotInOtherLibraries(String username, List<Book> books) throws Exception {
        validateBooksNotInOtherLibrariesExcludingCurrent(username, books, -1);
    }

    private void validateBooksNotInOtherLibrariesExcludingCurrent(String username, List<Book> books, int excludeLibraryId) throws Exception {
        if (books == null || books.isEmpty()) {
            logger.fine("No books to validate");
            return;
        }

        logger.fine("Validating " + books.size() + " books for user '" + username + "', excluding library ID " + excludeLibraryId);
        String sql = "SELECT bl.book_id, l.name_library FROM Books_Libraries bl " +
                      "JOIN Library l ON bl.libraries_id = l.library_id " +
                      "WHERE l.user_id = (SELECT user_id FROM Users WHERE userid = ?) " +
                      "AND bl.book_id = ? AND bl.libraries_id != ?";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (Book book : books) {
                logger.fine("Checking book '" + book.getTitle() + "' (ID: " + book.getBook_id() + ")");
                ps.setString(1, username);
                ps.setInt(2, book.getBook_id());
                ps.setInt(3, excludeLibraryId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        logger.fine("Book '" + book.getTitle() + "' already exists in library '" + rs.getString("name_library") + "'");
                        throw new IllegalArgumentException(ServerMessageManager.getString("server.error.dao.library.book.exists", book.getTitle(), rs.getString("name_library")));
                    } else {
                        logger.fine("Book '" + book.getTitle() + "' is available");
                    }
                }
            }
        }
        logger.fine("All books passed validation");
    }

    private void addBooksToLibrary(int libraryId, List<Book> books) throws Exception {
        String sql = "INSERT INTO Books_Libraries (libraries_id, book_id) VALUES (?, ?)";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (Book book : books) {
                ps.setInt(1, libraryId);
                ps.setInt(2, book.getBook_id());
                ps.addBatch();
            }
            ps.executeBatch();
            if (c.getAutoCommit() == false) {
                c.commit();
                logger.fine("Transaction committed for books addition");
            }
        }
    }

    private void updateBooksInLibrary(int libraryId, List<Book> books) throws Exception {
        // Delete old
        String deleteSql = "DELETE FROM Books_Libraries WHERE libraries_id = ?";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(deleteSql)) {
            ps.setInt(1, libraryId);
            ps.executeUpdate();
        }
        // Add new
        addBooksToLibrary(libraryId, books);
    }
}