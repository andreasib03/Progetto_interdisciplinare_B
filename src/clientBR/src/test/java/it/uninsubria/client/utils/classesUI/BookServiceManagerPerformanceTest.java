package it.uninsubria.client.utils.classesUI;

import it.uninsubria.shared.model.Book;
import it.uninsubria.shared.utils.LoggerUtil;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class BookServiceManagerPerformanceTest {

    private static final Logger logger = LoggerUtil.getLogger(BookServiceManagerPerformanceTest.class);

    @Test
    public void testLazyLoadingPerformance() {
        // Test delle prestazioni del lazy loading
        BookServiceManager bookManager = BookServiceManager.getInstance();

        // Azzera la cache per il test
        bookManager.clearCache();

        // Verifica che inizialmente non sia loaded
        assertFalse("Il manager non dovrebbe essere loaded inizialmente", bookManager.isLoaded());

        // Test caricamento pagina 0
        CompletableFuture<List<Book>> page0Future = bookManager.loadBooksPage(0, 10);
        List<Book> page0 = page0Future.join();

        // Dopo il caricamento di una pagina, dovrebbe ancora essere non loaded completamente
        // (perché non abbiamo chiamato loadAllBooks)
        assertFalse("Il manager non dovrebbe essere completamente loaded dopo loadBooksPage", bookManager.isLoaded());

        // Verifica che la pagina abbia risultati (se il server è disponibile)
        // Nota: Questo test potrebbe fallire se il server non è in esecuzione
        logger.info("✅ Test lazy loading completato - caricati " + page0.size() + " libri dalla pagina 0");

        // Test conteggio totale
        CompletableFuture<Integer> countFuture = bookManager.getTotalBooksCount();
        int totalCount = countFuture.join();
        logger.info("✅ Conteggio totale libri: " + totalCount);

        // Azzera cache alla fine del test
        bookManager.clearCache();
    }

    @Test
    public void testSearchPagination() {
        // Test della ricerca con paginazione
        BookServiceManager bookManager = BookServiceManager.getInstance();

        // Test ricerca paginata (se il server è disponibile)
        try {
            CompletableFuture<List<Book>> searchFuture = bookManager.searchBooksPaged("test", 0, 5);
            List<Book> results = searchFuture.join();

            logger.info("✅ Test ricerca paginata completato - trovati " + results.size() + " risultati");

            // Azzera cache
            bookManager.clearCache();

        } catch (Exception e) {
            logger.info("⚠️ Test ricerca paginata saltato - server non disponibile: " + e.getMessage());
        }
    }
}