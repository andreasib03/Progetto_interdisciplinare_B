package it.uninsubria.server.test;

import java.sql.Connection;
import java.util.List;
import it.uninsubria.server.dao.impl.BookDAOImpl;
import it.uninsubria.shared.model.Book;
import it.uninsubria.server.db.DataSeeder;

/**
 * Test to check how many books are returned from database
 */
public class BookCountTest {
    public static void main(String[] args) {
        try {
            System.out.println("Testing database book count...");

            // Initialize H2 test database
            it.uninsubria.server.integration.TestDatabaseHelper.initializeTestDatabase();

            // Get connection
            Connection conn = it.uninsubria.server.util.ConnectionPoolManager.getConnection();
            if (conn == null) {
                System.out.println("No database connection available");
                return;
            }

            // Seed the database with CSV data
            System.out.println("Seeding database with CSV data...");
            DataSeeder.seedBooksFromCSV(conn);
            System.out.println("Seeding completed");

            BookDAOImpl dao = new BookDAOImpl(conn);

            // Test total count
            List<Book> allBooks = dao.findAllPaged(0, 100000); // Try to get many books
            System.out.println("Books returned from findAllPaged(0, 100000): " + allBooks.size());

            // Test smaller batch
            List<Book> firstBatch = dao.findAllPaged(0, 5000);
            System.out.println("Books returned from findAllPaged(0, 5000): " + firstBatch.size());

            // Test second batch
            List<Book> secondBatch = dao.findAllPaged(5000, 5000);
            System.out.println("Books returned from findAllPaged(5000, 5000): " + secondBatch.size());

            // Test third batch
            List<Book> thirdBatch = dao.findAllPaged(10000, 5000);
            System.out.println("Books returned from findAllPaged(10000, 5000): " + thirdBatch.size());

            // Test a larger batch
            List<Book> largeBatch = dao.findAllPaged(0, 20000);
            System.out.println("Books returned from findAllPaged(0, 20000): " + largeBatch.size());

            conn.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}