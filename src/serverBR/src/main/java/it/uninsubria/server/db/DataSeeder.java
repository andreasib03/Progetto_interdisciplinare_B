package it.uninsubria.server.db;

import it.uninsubria.shared.utils.AppConstants;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * Utility class for seeding the database with book data from CSV file.
 */
public class DataSeeder {

    private static final Logger logger = Logger.getLogger(DataSeeder.class.getName());

    /**
     * Seeds the database with book data from the CSV file.
     *
     * @param conn the database connection
     * @throws Exception if seeding fails
     */
    public static void seedBooksFromCSV(Connection conn) throws Exception {
        logger.info("Starting book data seeding from CSV...");

        int insertedCount = 0;
        int skippedCount = 0;
        int errorCount = 0;
        int totalLines = 0;
        boolean originalAutoCommit = true; // Default value

        try (InputStream input = DataSeeder.class.getClassLoader().getResourceAsStream("BooksDatasetClean.csv")) {
            if (input == null) {
                throw new IOException("BooksDatasetClean.csv not found in resources");
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                String line;
                boolean isFirstLine = true;

                String insertSQL = "INSERT INTO Books (title, authors, descriptions, category, publisher, price, publish_date_month, publish_date_year) " +
                                 "VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT DO NOTHING";

                // Disable auto-commit for better performance
                originalAutoCommit = conn.getAutoCommit();
                conn.setAutoCommit(false);

                try (PreparedStatement ps = conn.prepareStatement(insertSQL)) {
                    int batchCount = 0;
                    final int BATCH_SIZE = AppConstants.Batch.DATA_SEEDER_BATCH_SIZE;

                    while ((line = reader.readLine()) != null) {
                        totalLines++;
                        if (isFirstLine) {
                            isFirstLine = false; // Skip header
                            continue;
                        }

                        // More robust parsing: find the last 7 separators to ensure we get exactly 8 fields
                        String[] parts = parseCSVLine(line);
                        if (parts.length < 8) {
                            logger.warning("Line " + totalLines + " skipped: insufficient fields (" + parts.length + "/8) - " + line.substring(0, Math.min(100, line.length())));
                            skippedCount++;
                            continue;
                        }

                        try {
                            // Extract and clean data
                            String title = cleanString(parts[0]);
                            String authors = cleanString(parts[1]);

                            // Skip if required fields are empty
                            if (title.isEmpty() || authors.isEmpty()) {
                                skippedCount++;
                                continue;
                            }

                            String description = cleanString(parts[2]);
                            String category = cleanString(parts[3]);
                            String publisher = cleanString(parts[4]);
                            String price = cleanString(parts[5]);
                            String publishMonth = cleanString(parts[6]);
                            String publishYearStr = cleanString(parts[7]);

                            // Parse year
                            Integer publishYear = null;
                            try {
                                if (!publishYearStr.isEmpty()) {
                                    publishYear = Integer.parseInt(publishYearStr.trim());
                                    if (publishYear < 1000 || publishYear > 2100) {
                                        publishYear = null; // Invalid year
                                    }
                                }
                            } catch (NumberFormatException e) {
                                // Keep as null
                            }

                            // Set parameters for insert (ON CONFLICT DO NOTHING handles duplicates)
                            ps.setString(1, title);
                            ps.setString(2, authors);
                            ps.setString(3, description.isEmpty() ? null : description);
                            ps.setString(4, category.isEmpty() ? null : category);
                            ps.setString(5, publisher.isEmpty() ? null : publisher);
                            ps.setString(6, price.isEmpty() ? null : price);
                            ps.setString(7, publishMonth.isEmpty() ? null : publishMonth);
                            if (publishYear != null) {
                                ps.setInt(8, publishYear);
                            } else {
                                ps.setNull(8, java.sql.Types.INTEGER);
                            }

                            ps.executeUpdate(); // ON CONFLICT DO NOTHING means no rows affected for duplicates
                            insertedCount++;
                            batchCount++;

                            // Commit batch periodically
                            if (batchCount >= BATCH_SIZE) {
                                try {
                                    conn.commit();
                                    batchCount = 0;
                                    logger.info("Committed batch of " + BATCH_SIZE + " books. Total inserted: " + insertedCount);
                                } catch (Exception commitEx) {
                                    logger.severe("Failed to commit batch at line " + totalLines + ": " + commitEx.getMessage());
                                    throw commitEx;
                                }
                            }

                            // Log progress every 10000 books (ridotto per prestazioni)
                            if (insertedCount % 10000 == 0) {
                                logger.info("Inserted " + insertedCount + " books so far...");
                            }

                            } catch (Exception e) {
                                logger.warning("Error processing line " + totalLines + ": " + e.getMessage());
                                // Log the problematic line (truncated for readability)
                                String truncatedLine = line.length() > 200 ? line.substring(0, 200) + "..." : line;
                                logger.warning("Problematic line content: " + truncatedLine.replace("‰", " | "));
                                errorCount++;

                                // If we have too many errors, log a summary and continue
                                if (errorCount % 100 == 0) {
                                    logger.severe("Too many errors during seeding (" + errorCount + "). Check data quality.");
                                }

                                // Continue with next line
                            }
                    }

                    // Final commit
                    if (batchCount > 0) {
                        conn.commit();
                        logger.info("Committed final batch of " + batchCount + " books");
                    }
                }

                // Restore original auto-commit setting
                conn.setAutoCommit(originalAutoCommit);

                logger.info("Book data seeding completed:");
                logger.info("  Total lines processed: " + (totalLines - 1)); // -1 for header
                logger.info("  Books inserted: " + insertedCount);
                logger.info("  Books skipped (duplicates/insufficient fields): " + skippedCount);
                logger.info("  Books with processing errors: " + errorCount);
                logger.info("  Expected final count: " + ((totalLines - 1) - skippedCount - errorCount));
            }
                } catch (Exception e) {
                    logger.severe("Error during book data seeding at line " + totalLines + ": " + e.getMessage());
                    logger.severe("Seeding statistics at failure: inserted=" + insertedCount + ", skipped=" + skippedCount + ", errors=" + errorCount);
                    throw e;
                }
    }

    /**
     * Cleans and validates string data.
     *
     * @param input the input string
     * @return cleaned string
     */
    private static String cleanString(String input) {
        if (input == null) {
            return "";
        }

        String cleaned = input.trim();

        // Remove excessive quotes if present
        if (cleaned.startsWith("\"") && cleaned.endsWith("\"")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }

        // Limit length to prevent DB issues
        if (cleaned.length() > 10000) {
            cleaned = cleaned.substring(0, 10000);
        }

        return cleaned;
    }

    /**
     * Parses a CSV line with ‰ separator, handling quoted fields properly.
     * This is a simplified parser that assumes fields don't contain the separator.
     *
     * @param line the CSV line to parse
     * @return array of fields
     */
    public static String[] parseCSVLine(String line) {
        // For now, use a simple split. If this causes issues, we can implement a more sophisticated parser
        return line.split("‰", -1);
    }

    /**
     * Checks if books table is empty.
     *
     * @param conn the database connection
     * @return true if table is empty
     * @throws SQLException if query fails
     */
    public static boolean isBooksTableEmpty(Connection conn) throws SQLException {
        String sql = "SELECT COUNT(*) as count FROM Books";
        try (PreparedStatement ps = conn.prepareStatement(sql);
              var rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("count") == 0;
            }
        }
        return true;
    }
}