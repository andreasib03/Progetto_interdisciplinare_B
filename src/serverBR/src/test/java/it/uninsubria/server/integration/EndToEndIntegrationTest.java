package it.uninsubria.server.integration;

import static org.junit.Assert.*;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import it.uninsubria.server.util.ConnectionPoolManager;
import it.uninsubria.shared.model.Book;
import it.uninsubria.shared.rmi.BookService;

/**
 * End-to-end integration test that starts the real RMI server and tests
 * client-server communication using H2 in-memory database.
 *
 * This test verifies the complete integration between server and client components.
 */
public class EndToEndIntegrationTest {

    private static Thread serverThread;
    private static volatile boolean serverStarted = false;
    private static volatile Exception serverException = null;

    @BeforeClass
    public static void setupTestEnvironment() throws Exception {
        System.out.println("TEST: Starting test environment setup...");

        // Clear any cached data from previous tests
        it.uninsubria.server.cache.CacheManager.clear();

        try {
            // Initialize H2 database for testing (this sets up ConnectionPoolManager)
            TestDatabaseHelper.initializeTestDatabase();

            // Start server once for all tests
            startServerInBackground();

            System.out.println("TEST: Test environment setup completed");
        } catch (Exception e) {
            System.err.println("TEST: Failed to setup test environment: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Before
    public void setupServerForTest() throws Exception {
        // Server is already started in @BeforeClass, just verify it's ready
        if (!serverStarted) {
            throw new RuntimeException("Server was not started properly in @BeforeClass");
        }

        // Quick check that services are still available
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            registry.lookup("BookService");
        } catch (Exception e) {
            System.err.println("TEST: Services not available in @Before: " + e.getMessage());
            throw e;
        }
    }

    @AfterClass
    public static void cleanupTestEnvironment() throws Exception {
        System.out.println("TEST: Starting cleanup...");
        stopServer();

        // Additional cleanup to ensure clean state for next test run
        try {
            // Try to unbind any remaining services
            java.rmi.registry.Registry registry = java.rmi.registry.LocateRegistry.getRegistry("localhost", 1099);
            String[] services = registry.list();
            for (String service : services) {
                try {
                    registry.unbind(service);
                    System.out.println("TEST: Unbound service: " + service);
                } catch (Exception e) {
                    // Ignore if service doesn't exist
                }
            }
        } catch (Exception e) {
            System.out.println("TEST: No registry to cleanup: " + e.getMessage());
        }

        // Force cleanup of any remaining RMI threads
        try {
            System.gc();
            Thread.sleep(1000); // Give time for cleanup
        } catch (Exception e) {
            // Ignore cleanup errors
        }

        ConnectionPoolManager.shutdown();
        System.out.println("TEST: Cleanup completed");
    }

    private static void cleanupRmiRegistry() {
        try {
            System.out.println("TEST: Attempting to cleanup existing RMI registry...");
            // Try to get existing registry and unbind all services
            Registry existingRegistry = LocateRegistry.getRegistry("localhost", 1099);
            String[] services = existingRegistry.list();
            for (String service : services) {
                try {
                    existingRegistry.unbind(service);
                    System.out.println("TEST: Unbound existing service: " + service);
                } catch (Exception e) {
                    // Service might not exist, continue
                }
            }
        } catch (Exception e) {
            System.out.println("TEST: No existing registry to cleanup: " + e.getMessage());
        }

        // Force garbage collection to clean up RMI objects
        System.gc();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void startServerInBackground() throws Exception {
        if (serverStarted) {
            System.out.println("TEST: Server already started, skipping...");
            return;
        }

        serverStarted = false;
        serverException = null;

        // Force cleanup of any existing RMI registry before starting
        cleanupRmiRegistry();

        System.out.println("TEST: Creating server thread...");
        serverThread = new Thread(() -> {
            System.out.println("TEST: Server thread started");
            try {
                System.out.println("TEST: Starting RMI server...");
                // Start the RMI server directly
                it.uninsubria.server.db.BookRecommenderServer.startServer();
                System.out.println("TEST: RMI server started successfully");
                serverStarted = true;
                System.out.println("TEST: serverStarted set to true");

                // Keep the thread alive for the duration of all tests
                // This prevents the server from shutting down
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        Thread.sleep(500); // Sleep to prevent busy waiting
                    } catch (InterruptedException e) {
                        System.out.println("TEST: Server thread interrupted, shutting down gracefully");
                        break;
                    }
                }

            } catch (Exception e) {
                System.err.println("TEST: Failed to start RMI server: " + e.getMessage());
                e.printStackTrace();
                serverException = e;
                serverStarted = false;
                System.out.println("TEST: serverStarted set to false due to exception");
            }
        });
        System.out.println("TEST: Server thread created, starting...");

        serverThread.setDaemon(true);
        serverThread.start();

        // Wait for server to start and services to be registered (reduced timeout to 15 seconds)
        long startTime = System.currentTimeMillis();
        boolean servicesReady = false;
        int attempts = 0;
        while (!serverStarted && !servicesReady && (System.currentTimeMillis() - startTime) < 15000) {
            attempts++;
            try {
                System.out.println("TEST: Attempt " + attempts + " to check server availability...");

                // Check if registry and services are available
                Registry registry = LocateRegistry.getRegistry("localhost", 1099);
                // Check if all expected services are registered
                String[] expectedServices = {"BookService", "LibraryService", "UserService", "ReviewsService"};
                boolean allServicesReady = true;

                for (String serviceName : expectedServices) {
                    try {
                        Object service = registry.lookup(serviceName);
                        if (service == null) {
                            allServicesReady = false;
                            break;
                        }
                    } catch (Exception e) {
                        allServicesReady = false;
                        break;
                    }
                }

                if (allServicesReady) {
                    System.out.println("TEST: All services ready!");
                    servicesReady = true;
                    serverStarted = true;
                    System.out.println("TEST: Server fully ready!");
                    break;
                } else {
                    System.out.println("TEST: Not all services ready yet, waiting...");
                    Thread.sleep(300); // Wait 0.3 seconds before retrying
                }

            } catch (Exception e) {
                System.out.println("TEST: Registry not available: " + e.getMessage());
                Thread.sleep(300); // Wait 0.3 seconds before retrying
            }
        }

        if (!serverStarted) {
            System.err.println("TEST: Server failed to start within 15 seconds");
            if (serverException != null) {
                System.err.println("TEST: Server exception: " + serverException.getMessage());
                serverException.printStackTrace();
            }
            throw new RuntimeException("Server failed to start within 15 seconds", serverException);
        }
    }

    private static void stopServer() {
        if (serverThread != null && serverThread.isAlive()) {
            try {
                // Try to stop the server gracefully
                it.uninsubria.server.db.BookRecommenderServer.stopServer();
            } catch (Exception e) {
                System.err.println("TEST: Error stopping server: " + e.getMessage());
                // Force stop if graceful shutdown fails
                serverThread.interrupt();
            }

            try {
                serverThread.join(5000); // Wait up to 5 seconds for thread to stop
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        // Reset server state
        serverStarted = false;
        serverThread = null;
    }

    @Test(timeout = 10000) // 10 seconds timeout
    public void testBookServiceSearchGlobally() throws Exception {
        // Connect to the running RMI server
        Registry registry = LocateRegistry.getRegistry("localhost", 1099);
        BookService bookService = (BookService) registry.lookup("BookService");

        // Test global book search
        List<Book> books = bookService.searchGlobally();

        // Debug: print what we got
        System.out.println("TEST: Found " + books.size() + " books");
        for (Book book : books) {
            System.out.println("TEST: Book: " + book.getTitle() + " by " + book.getAuthors());
        }

        // Verify we get some books (from test data)
        assertNotNull("Books list should not be null", books);
        assertTrue("Should have at least some test books", books.size() >= 3);

        // Verify book data structure
        Book firstBook = books.get(0);
        assertNotNull("Book title should not be null", firstBook.getTitle());
        assertTrue("Book should have a valid year", firstBook.getPublish_date_year() > 0);
    }

    @Test(timeout = 8000) // 8 seconds timeout
    public void testBookServiceSearchByTitle() throws Exception {
        Registry registry = LocateRegistry.getRegistry("localhost", 1099);
        BookService bookService = (BookService) registry.lookup("BookService");

        // Test title search
        List<Book> javaBooks = bookService.searchByTitle("Java");
        assertNotNull("Java books search should return results", javaBooks);

        // Should find our test book "Java Programming"
        boolean foundJavaBook = javaBooks.stream()
            .anyMatch(book -> book.getTitle().contains("Java"));
        assertTrue("Should find Java programming book", foundJavaBook);

        // Test empty search
        List<Book> emptyResults = bookService.searchByTitle("NonExistentBook12345");
        assertNotNull("Empty search should return empty list, not null", emptyResults);
    }

    @Test(timeout = 8000) // 8 seconds timeout
    public void testBookServiceSearchByAuthor() throws Exception {
        Registry registry = LocateRegistry.getRegistry("localhost", 1099);
        BookService bookService = (BookService) registry.lookup("BookService");

        // Test author search
        List<Book> rowlingBooks = bookService.searchByAuthor("Rowling");
        assertNotNull("Rowling books search should return results", rowlingBooks);

        // Should find our test book "Harry Potter"
        boolean foundHarryPotter = rowlingBooks.stream()
            .anyMatch(book -> book.getTitle().contains("Harry Potter"));
        assertTrue("Should find Harry Potter book", foundHarryPotter);
    }

    @Test(timeout = 8000) // 8 seconds timeout
    public void testBookServiceSearchByAuthorAndYear() throws Exception {
        Registry registry = LocateRegistry.getRegistry("localhost", 1099);
        BookService bookService = (BookService) registry.lookup("BookService");

        // Test 1: Combined search for existing author and year
        List<Book> results = bookService.searchByAuthorAndYear("Rowling", 1997);
        assertNotNull("Combined search should return results", results);
        assertTrue("Should find exactly one book", results.size() == 1);

        // Should find Harry Potter published in 1997
        boolean foundCorrectBook = results.stream()
            .anyMatch(book -> book.getTitle().contains("Harry Potter") && book.getPublish_date_year() == 1997);
        assertTrue("Should find Harry Potter from 1997", foundCorrectBook);

        // Test 2: Author exists but year doesn't match - should return empty
        List<Book> noResultsAuthorYear = bookService.searchByAuthorAndYear("Rowling", 2023);
        assertNotNull("Should return empty list, not null", noResultsAuthorYear);
        assertTrue("Should not find books when year doesn't match", noResultsAuthorYear.isEmpty());

        // Test 3: Year exists but author doesn't match - should return empty
        List<Book> noResultsYearAuthor = bookService.searchByAuthorAndYear("John Doe", 1997);
        assertNotNull("Should return empty list, not null", noResultsYearAuthor);
        assertTrue("Should not find books when author doesn't match", noResultsYearAuthor.isEmpty());

        // Test 4: Both author and year exist but not as a pair - should return empty
        List<Book> noResultsWrongPair = bookService.searchByAuthorAndYear("Jane Smith", 1997);
        assertNotNull("Should return empty list, not null", noResultsWrongPair);
        assertTrue("Should not find books when pair doesn't exist", noResultsWrongPair.isEmpty());
    }

    @Test(timeout = 15000) // 15 seconds timeout for concurrent requests
    public void testConcurrentClientRequests() throws Exception {
        Registry registry = LocateRegistry.getRegistry("localhost", 1099);
        BookService bookService = (BookService) registry.lookup("BookService");

        // Test multiple concurrent requests
        CompletableFuture<List<Book>> future1 = CompletableFuture.supplyAsync(() -> {
            try {
                return bookService.searchByTitle("Java");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        CompletableFuture<List<Book>> future2 = CompletableFuture.supplyAsync(() -> {
            try {
                return bookService.searchGlobally();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        // Wait for both requests to complete (reduced timeout)
        List<Book> javaResults = future1.get(5, TimeUnit.SECONDS);
        List<Book> globalResults = future2.get(5, TimeUnit.SECONDS);

        assertNotNull("Concurrent Java search should work", javaResults);
        assertNotNull("Concurrent global search should work", globalResults);
        assertTrue("Global search should return books", globalResults.size() > 0);
    }

    @Test
    public void testSimple() throws Exception {
        System.out.println("TEST: Simple test executed");
        assertTrue(true);
    }

    @Test(timeout = 5000) // 5 seconds timeout
    public void testRMIServiceAvailability() throws Exception {
        System.out.println("TEST: Starting testRMIServiceAvailability");

        // Server should already be started by setupServerForTest(), just verify services
        Registry registry = LocateRegistry.getRegistry("localhost", 1099);

        // Verify all expected services are available
        String[] expectedServices = {
            "BookService",
            "LibraryService",
            "ReviewsService",
            "UserService",
            "SuggestionsService",
            "RecommendationService"
        };

        String[] availableServices = registry.list();

        for (String service : expectedServices) {
            boolean serviceFound = false;
            for (String available : availableServices) {
                if (available.equals(service)) {
                    serviceFound = true;
                    break;
                }
            }
            assertTrue("Service " + service + " should be available in RMI registry", serviceFound);
        }
    }
}