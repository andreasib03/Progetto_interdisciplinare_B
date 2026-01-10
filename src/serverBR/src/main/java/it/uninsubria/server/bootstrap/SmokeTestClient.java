package it.uninsubria.server.bootstrap;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.uninsubria.shared.rmi.BookService;
import it.uninsubria.shared.model.Book;

public class SmokeTestClient {
    private static final Logger logger = LoggerFactory.getLogger(SmokeTestClient.class);

    public static void main(String[] args) {
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            BookService bookService = (BookService) registry.lookup("BookService");
            List<Book> books = bookService.searchGlobally();
            logger.info("Smoke test - books received: {}", books.size());
            // Test author search with correct format
            List<Book> authorBooks = bookService.searchByAuthor("king, stephen");
            logger.info("Search by author 'king, stephen': {} results", authorBooks.size());
            if (!authorBooks.isEmpty()) {
                logger.info(" - {}", authorBooks.get(0).getTitle());
            }
        } catch (Exception e) {
            logger.error("Smoke test client error", e);
        }
    }
}
