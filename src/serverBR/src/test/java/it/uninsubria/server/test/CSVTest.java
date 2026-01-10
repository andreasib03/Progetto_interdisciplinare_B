package it.uninsubria.server.test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Simple test to verify CSV parsing
 */
public class CSVTest {
    public static void main(String[] args) {
        try {
            System.out.println("Testing CSV parsing...");

            InputStream input = CSVTest.class.getClassLoader().getResourceAsStream("BooksDatasetClean.csv");
            if (input == null) {
                System.out.println("CSV file not found!");
                return;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                String line;
                int lineCount = 0;
                int validLines = 0;

                while ((line = reader.readLine()) != null && lineCount < 100) { // Test first 100 lines
                    lineCount++;

                    if (lineCount == 1) {
                        System.out.println("Header: " + line);
                        continue; // Skip header
                    }

                    String[] parts = line.split("‰", -1);
                    if (parts.length >= 8) {
                        String title = parts[0].trim();
                        String authors = parts[1].trim();

                        if (!title.isEmpty() && !authors.isEmpty()) {
                            validLines++;
                            if (validLines <= 5) { // Show first 5 valid lines
                                System.out.println("Line " + lineCount + ": Title='" + title + "', Authors='" + authors + "', Fields=" + parts.length);
                            }
                        } else {
                            System.out.println("Line " + lineCount + " skipped: empty title or authors");
                        }
                    } else {
                        System.out.println("Line " + lineCount + " skipped: only " + parts.length + " fields");
                    }
                }

                System.out.println("Test completed: " + lineCount + " lines read, " + validLines + " valid books found");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}