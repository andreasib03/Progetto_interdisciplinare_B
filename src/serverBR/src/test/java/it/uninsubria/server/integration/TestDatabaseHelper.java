package it.uninsubria.server.integration;

import java.sql.Connection;
import java.sql.Statement;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.h2.jdbcx.JdbcDataSource;

import it.uninsubria.server.dao.BookDAO;
import it.uninsubria.shared.utils.LoggerUtil;
import it.uninsubria.server.dao.LibraryDAO;
import it.uninsubria.server.dao.ReviewDAO;
import it.uninsubria.server.dao.UserDAO;
import it.uninsubria.server.dao.SuggestionDAO;
import it.uninsubria.server.dao.impl.BookDAOImpl;
import it.uninsubria.server.dao.impl.LibraryDAOImpl;
import it.uninsubria.server.dao.impl.ReviewDAOImpl;
import it.uninsubria.server.dao.impl.UserDAOImpl;
import it.uninsubria.server.dao.impl.SuggestionDAOImpl;
import it.uninsubria.server.di.ServerDIContainer;
import it.uninsubria.server.util.ConnectionPoolManager;

/**
 * Helper class for integration tests to set up H2 in-memory database
 * and initialize the ServerDIContainer with test data.
 */
public class TestDatabaseHelper {

    private static final Logger logger = LoggerUtil.getLogger(TestDatabaseHelper.class);

    private static final String H2_URL = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1";
    private static final String H2_USER = "sa";
    private static final String H2_PASSWORD = "";

    public static DataSource createH2DataSource() throws Exception {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL(H2_URL);
        ds.setUser(H2_USER);
        ds.setPassword(H2_PASSWORD);
        return ds;
    }

    public static void initializeTestDatabase() throws Exception {
        logger.info("Initializing test database with URL: " + H2_URL);

        // Initialize ConnectionPoolManager with H2 database first
        ConnectionPoolManager.initialize(
            H2_URL,
            "sa",
            "",
            "org.h2.Driver"
        );

        DataSource ds = ConnectionPoolManager.getDataSource();
        logger.info("Database initialized successfully");

        // Create tables using H2
        try (Connection conn = ds.getConnection();
              Statement stmt = conn.createStatement()) {

            // Create Users table
            stmt.execute("CREATE TABLE IF NOT EXISTS Users (" +
                "user_id SERIAL PRIMARY KEY," +
                "username VARCHAR(30) UNIQUE NOT NULL," +
                "email VARCHAR(255) UNIQUE NOT NULL," +
                "password_hash VARCHAR(255) NOT NULL," +
                "names VARCHAR(50)," +
                "surnames VARCHAR(50)," +
                "fiscal_code VARCHAR(16) UNIQUE," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")");

            // Create Books table
            stmt.execute("CREATE TABLE IF NOT EXISTS Books (" +
                "book_id SERIAL PRIMARY KEY," +
                "title VARCHAR(500) NOT NULL," +
                "authors VARCHAR(500)," +
                "descriptions TEXT," +
                "category VARCHAR(100)," +
                "publisher VARCHAR(200)," +
                "price VARCHAR(20)," +
                "publish_date_month VARCHAR(20)," +
                "publish_date_year INTEGER," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")");

            // Create Library table (user libraries)
            stmt.execute("CREATE TABLE IF NOT EXISTS Library (" +
                "library_id SERIAL PRIMARY KEY," +
                "user_id INTEGER NOT NULL," +
                "name VARCHAR(100) NOT NULL," +
                "description TEXT," +
                "is_public BOOLEAN DEFAULT FALSE," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "FOREIGN KEY (user_id) REFERENCES Users(user_id)" +
                ")");

            // Create Books_Libraries junction table
            stmt.execute("CREATE TABLE IF NOT EXISTS Books_Libraries (" +
                "id SERIAL PRIMARY KEY," +
                "book_id INTEGER NOT NULL," +
                "library_id INTEGER NOT NULL," +
                "added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "FOREIGN KEY (book_id) REFERENCES Books(book_id)," +
                "FOREIGN KEY (library_id) REFERENCES Library(library_id)" +
                ")");

            // Create Book_Reviews table
            stmt.execute("CREATE TABLE IF NOT EXISTS Book_Reviews (" +
                "book_reviews_id SERIAL PRIMARY KEY," +
                "book_id INTEGER NOT NULL," +
                "user_id INTEGER NOT NULL," +
                "rating INTEGER CHECK (rating >= 1 AND rating <= 5)," +
                "review_text TEXT," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "FOREIGN KEY (book_id) REFERENCES Books(book_id)," +
                "FOREIGN KEY (user_id) REFERENCES Users(user_id)" +
                ")");

            // Insert test data
            insertTestData(stmt);
        }

        logger.info("Creating DAOs...");
        // Initialize DAOs and DI Container using the same DataSource
        UserDAO userDAO = new UserDAOImpl(ds);
        BookDAO bookDAO = new BookDAOImpl(ds);
        LibraryDAO libraryDAO = new LibraryDAOImpl(ds);
        ReviewDAO reviewDAO = new ReviewDAOImpl(ds);
        SuggestionDAO suggestionDAO = new SuggestionDAOImpl(ds);

        logger.info("Initializing ServerDIContainer...");
        try {
            ServerDIContainer.init(bookDAO, libraryDAO, userDAO, reviewDAO, suggestionDAO);
            logger.info("ServerDIContainer initialized successfully");
        } catch (Exception e) {
            logger.severe("Failed to initialize ServerDIContainer: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    private static void insertTestData(Statement stmt) throws Exception {
        // Insert test user (only if not exists)
        try {
            stmt.execute("INSERT INTO Users (username, email, password_hash, names, surnames, fiscal_code) " +
                "VALUES ('testuser', 'test@example.com', 'hashedpassword', 'Test', 'User', 'ABCDEF12G34H567I')");
        } catch (Exception e) {
            // User might already exist, ignore
            logger.info("Test user already exists, skipping insertion");
        }

        // Insert test books (only if not exists)
        try {
            stmt.execute("INSERT INTO Books (title, authors, descriptions, category, publisher, price, publish_date_month, publish_date_year) " +
                "VALUES ('Java Programming', 'John Doe', 'Learn Java programming', 'Technology', 'Tech Books', '29.99', 'January', 2023)");
        } catch (Exception e) {
            logger.info("Java Programming book already exists, skipping insertion");
        }

        try {
            stmt.execute("INSERT INTO Books (title, authors, descriptions, category, publisher, price, publish_date_month, publish_date_year) " +
                "VALUES ('Python Guide', 'Jane Smith', 'Complete Python guide', 'Technology', 'Code Books', '34.99', 'March', 2022)");
        } catch (Exception e) {
            logger.info("Python Guide book already exists, skipping insertion");
        }

        try {
            stmt.execute("INSERT INTO Books (title, authors, descriptions, category, publisher, price, publish_date_month, publish_date_year) " +
                "VALUES ('Harry Potter', 'J.K. Rowling', 'Fantasy adventure', 'Fiction', 'Magic Books', '19.99', 'June', 1997)");
        } catch (Exception e) {
            logger.info("Harry Potter book already exists, skipping insertion");
        }

        // Insert test library (only if not exists)
        try {
            stmt.execute("INSERT INTO Library (user_id, name, description, is_public) " +
                "VALUES (1, 'My Favorites', 'My favorite books', true)");
        } catch (Exception e) {
            logger.info("Test library already exists, skipping insertion");
        }

        // Insert book-library relationships (only if not exist)
        try {
            stmt.execute("INSERT INTO Books_Libraries (book_id, library_id) VALUES (1, 1)");
        } catch (Exception e) {
            logger.info("Book-library relationship 1-1 already exists, skipping insertion");
        }

        try {
            stmt.execute("INSERT INTO Books_Libraries (book_id, library_id) VALUES (3, 1)");
        } catch (Exception e) {
            logger.info("Book-library relationship 3-1 already exists, skipping insertion");
        }
    }
}