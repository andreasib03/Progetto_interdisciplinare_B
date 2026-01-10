package it.uninsubria.client.utils.classesUI;

import it.uninsubria.shared.model.Book;
import it.uninsubria.shared.utils.AppConstants;
import it.uninsubria.shared.utils.LoggerUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class BookServiceManager {
    private static final Logger logger = LoggerUtil.getLogger(BookServiceManager.class);
    private static BookServiceManager instance;

    // Constants for loading and caching
    private static final int PAGE_SIZE = 50; // Dimensione pagina per lazy loading
    private static final int MAX_POPULAR_BOOKS = 7; // Numero di libri popolari da selezionare per la home
    private static final int BATCH_SIZE = AppConstants.Batch.DEFAULT_SIZE;

    // Cache state constants
    private static final int UNINITIALIZED_COUNT = -1;

    private List<Book> allBooks = new CopyOnWriteArrayList<>();
    private List<Book> popularBooks = new CopyOnWriteArrayList<>(); // Libri popolari selezionati
    private boolean loaded = false;
    private boolean popularBooksSelected = false; // Flag per indicare se i libri popolari sono stati selezionati
    private int totalBooksCount = UNINITIALIZED_COUNT; // Cache del conteggio totale

    private BookServiceManager() {}

    // Helper methods for common operations
    private void addBooksToCache(List<Book> books) {
        for (Book book : books) {
            if (!containsBook(book.getBook_id())) {
                allBooks.add(book);
            }
        }
    }

    private void updateLoadingState(boolean isLoaded) {
        this.loaded = isLoaded;
        if (isLoaded && !popularBooksSelected) {
            selectPopularBooksInternal();
        }
    }

    private void saveToCacheAndLog(List<Book> books, String message) {
        BookCacheManager.saveBooksToCache(books);
        logger.info(message + ": " + books.size());
    }

    public static synchronized BookServiceManager getInstance() {
        if (instance == null) {
            instance = new BookServiceManager();
        }
        return instance;
    }

    /**
     * Carica tutti i libri dal server (una sola volta), con supporto cache locale
     */
    public CompletableFuture<List<Book>> loadAllBooks() {
        if (loaded) {
            return CompletableFuture.completedFuture(allBooks);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Prima prova a caricare dalla cache locale
                List<Book> cachedBooks = loadFromLocalCache();
                if (cachedBooks != null && !cachedBooks.isEmpty()) {
                    return cachedBooks;
                }

                // Cache non valida o vuota, carica TUTTI i libri dal server
                return loadAllBooksFromServer();

            } catch (Exception e) {
                logger.severe("Errore imprevisto nel caricamento libri: " + e.getMessage());
                return allBooks;
            }
        });
    }

    private List<Book> loadFromLocalCache() {
        List<Book> cachedBooks = BookCacheManager.loadBooksFromCache();
        if (cachedBooks != null && !cachedBooks.isEmpty()) {
            logger.info("Caricamento libri dalla cache locale...");
            allBooks.addAll(cachedBooks);
            updateLoadingState(true);
            logger.info("Libri caricati dalla cache: " + allBooks.size());
            return allBooks;
        }
        return null;
    }

    private List<Book> loadAllBooksFromServer() {
        logger.info("Cache non disponibile, caricamento di tutti i libri dal server per categorie complete...");

        try {
            int totalBooks = getTotalBooksCountFromServer();
            List<Book> allServerBooks = loadBooksInBatches(totalBooks);

            if (!allServerBooks.isEmpty()) {
                allBooks.addAll(allServerBooks);
                updateLoadingState(true);
                saveToCacheAndLog(allServerBooks, "Tutti i libri caricati dal server e salvati in cache");
            } else {
                logger.warning("Nessun libro ricevuto dal server");
            }

        } catch (Exception retryEx) {
            logger.severe("Falliti tutti i tentativi di caricamento libri: " + retryEx.getMessage());
        }

        return allBooks;
    }

    private int getTotalBooksCountFromServer() throws Exception {
        int totalBooks = RetryUtils.executeWithRetry(
            () -> it.uninsubria.client.di.DIContainer.getBookService().getTotalBooksCount(),
            "Ottieni numero totale di libri"
        );
        logger.info("Totale libri nel database: " + totalBooks);
        return totalBooks;
    }

    private List<Book> loadBooksInBatches(int totalBooks) throws Exception {
        List<Book> allServerBooks = new ArrayList<>();

        for (int offset = 0; offset < totalBooks; ) {
            int remaining = totalBooks - offset;
            int currentBatchSize = Math.min(BATCH_SIZE, remaining);
            final int currentOffset = offset;

            logger.info("Caricamento batch: offset=" + currentOffset + ", size=" + currentBatchSize);

            List<Book> batch = loadSingleBatch(currentOffset, currentBatchSize);
            if (batch != null && !batch.isEmpty()) {
                allServerBooks.addAll(batch);
                logger.info("Batch caricato: " + batch.size() + " libri (totale: " + allServerBooks.size() + ")");
            } else {
                logger.warning("Batch vuoto ricevuto per offset=" + currentOffset);
            }

            offset += currentBatchSize;
        }

        return allServerBooks;
    }

    private List<Book> loadSingleBatch(int offset, int batchSize) throws Exception {
        return RetryUtils.executeWithRetry(
            () -> it.uninsubria.client.di.DIContainer.getBookService().searchGloballyPaged(offset, batchSize),
            "Caricamento batch libri (offset=" + offset + ", size=" + batchSize + ")"
        );
    }

    /**
     * Carica un numero limitato di libri per avvio veloce (lazy loading)
     */
    public CompletableFuture<List<Book>> loadLimitedBooks(int limit) {
        if (loaded) {
            // Se già caricati tutti, restituisci i primi 'limit'
            return CompletableFuture.completedFuture(
                allBooks.subList(0, Math.min(limit, allBooks.size()))
            );
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Caricamento ottimizzato di " + limit + " libri dal server...");
                List<Book> limitedBooks = loadSingleBatch(0, limit);

                addBooksToCache(limitedBooks);
                updateLoadingState(true);

                logger.info("Caricati " + limitedBooks.size() + " libri ottimizzati");
                return limitedBooks;
            } catch (Exception e) {
                logger.severe("Errore nel caricamento ottimizzato dei libri: " + e.getMessage());
                return new ArrayList<>();
            }
        });
    }

    public List<Book> getAllBooks() {
        return allBooks;
    }

    /**
     * Ottiene i libri popolari selezionati (se disponibili) o tutti i libri se non ancora selezionati
     */
    public List<Book> getPopularBooks() {
        if (popularBooksSelected && !popularBooks.isEmpty()) {
            return popularBooks;
        }
        // Fallback: seleziona libri popolari ora se non ancora fatto
        selectPopularBooks();
        return popularBooks.isEmpty() ? allBooks : popularBooks;
    }

    /**
     * Verifica se i libri popolari sono stati selezionati
     */
    public boolean arePopularBooksSelected() {
        return popularBooksSelected;
    }

    public boolean isLoaded() {
        return loaded;
    }

    /**
     * Carica una pagina specifica di libri (lazy loading)
     */
    public CompletableFuture<List<Book>> loadBooksPage(int page) {
        return loadBooksPage(page, PAGE_SIZE);
    }

    /**
     * Carica una pagina specifica di libri con dimensione personalizzata
     */
    public CompletableFuture<List<Book>> loadBooksPage(int page, int pageSize) {
        int offset = page * pageSize;
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Caricamento pagina " + page + " di libri (offset: " + offset + ", limit: " + pageSize + ")");
                List<Book> pageBooks = loadSingleBatch(offset, pageSize);

                addBooksToCache(pageBooks);
                return pageBooks;
            } catch (Exception e) {
                logger.severe("Errore nel caricamento della pagina " + page + ": " + e.getMessage());
                return new ArrayList<>();
            }
        });
    }

    /**
     * Carica libri per ricerca con paginazione
     */
    public CompletableFuture<List<Book>> searchBooksPaged(String query, int page, int pageSize) {
        return loadAllBooks(); // Simplified - just load all books for now
    }

    /**
     * Ottiene il conteggio totale dei libri
     */
    public CompletableFuture<Integer> getTotalBooksCount() {
        if (totalBooksCount > 0) {
            return CompletableFuture.completedFuture(totalBooksCount);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                totalBooksCount = it.uninsubria.client.di.DIContainer.getBookService().getTotalBooksCount();
                logger.info("Conteggio totale libri: " + totalBooksCount);
                return totalBooksCount;
            } catch (Exception e) {
                logger.severe("Errore nel conteggio libri: " + e.getMessage());
                return 0;
            }
        });
    }

    /**
     * Verifica se un libro è già nella cache locale
     */
    private boolean containsBook(int bookId) {
        return allBooks.stream().anyMatch(book -> book.getBook_id() == bookId);
    }

    /**
     * Ottiene libri dalla cache locale (senza caricamento dal server)
     */
    public List<Book> getCachedBooks() {
        return new ArrayList<>(allBooks);
    }

    /**
     * Azzera la cache locale (utile per refresh forzato)
     */
    public void clearCache() {
        allBooks.clear();
        popularBooks.clear();
        loaded = false;
        popularBooksSelected = false;
        totalBooksCount = -1;
        BookCacheManager.invalidateCache();
        logger.info("Cache libri azzerata (memoria e disco)");
    }

    /**
     * Forza ricaricamento dalla cache disco (ignora cache memoria)
     */
    public void reloadFromCache() {
        allBooks.clear();
        loaded = false;
        totalBooksCount = -1;

        List<Book> cachedBooks = BookCacheManager.loadBooksFromCache();
        if (cachedBooks != null && !cachedBooks.isEmpty()) {
            allBooks.addAll(cachedBooks);
            loaded = true;
            logger.info("Ricaricato dalla cache disco: " + allBooks.size() + " libri");
        }
    }

    /**
     * Verifica se la cache su disco è valida
     */
    public boolean isDiskCacheValid() {
        return BookCacheManager.isCacheValid();
    }

    /**
     * Ottiene informazioni sulla cache per debug
     */
    public String getCacheInfo() {
        StringBuilder info = new StringBuilder();
        info.append("Cache Memoria: ").append(loaded ? allBooks.size() + " libri" : "non caricata").append("\n");
        info.append(BookCacheManager.getCacheInfo());
        return info.toString();
    }

    /**
     * Seleziona libri popolari basati sulle statistiche aggregate
     * Questa selezione avviene durante il caricamento iniziale per ottimizzare le performance
     */
    private void selectPopularBooksInternal() {
        if (popularBooksSelected || allBooks.isEmpty()) {
            return;
        }

        try {
            logger.info("Selezionando libri popolari per la home page...");
            BookStatisticsManager statsManager = BookStatisticsManager.getInstance();

            List<Book> selectedBooks = selectBooksWithStatistics(statsManager);
            if (selectedBooks.isEmpty()) {
                selectedBooks = selectRandomBooksAsFallback();
            }

            // Assicurati di avere sempre MAX_POPULAR_BOOKS libri
            if (selectedBooks.size() < MAX_POPULAR_BOOKS && allBooks.size() >= MAX_POPULAR_BOOKS) {
                // Se abbiamo meno libri del necessario ma abbiamo abbastanza libri totali,
                // aggiungiamo libri casuali aggiuntivi
                Set<Integer> usedIds = selectedBooks.stream()
                    .map(Book::getBook_id)
                    .collect(Collectors.toSet());

                List<Book> additionalBooks = allBooks.stream()
                    .filter(book -> !usedIds.contains(book.getBook_id()))
                    .collect(Collectors.toList());

                Collections.shuffle(additionalBooks);
                for (Book book : additionalBooks) {
                    if (selectedBooks.size() < MAX_POPULAR_BOOKS) {
                        selectedBooks.add(book);
                    } else {
                        break;
                    }
                }
                logger.info("AGGIUNTI libri casuali aggiuntivi: ora " + selectedBooks.size() + " libri totali");
            }

            popularBooks.addAll(selectedBooks);
            popularBooksSelected = true;
            logger.info("Selezionati " + popularBooks.size() + " libri popolari per la home page");

        } catch (Exception e) {
            logger.warning("Errore nella selezione dei libri popolari: " + e.getMessage() + ". Usando fallback casuale.");
            handlePopularBooksSelectionError();
        }
    }

    private List<Book> selectBooksWithStatistics(BookStatisticsManager statsManager) {
        List<Book> booksWithStats = allBooks.stream()
                .filter(book -> statsManager.getBookStats(book.getBook_id()) != null)
                .sorted((b1, b2) -> Double.compare(
                    statsManager.getBookStats(b2.getBook_id()).getPopularityScore(),
                    statsManager.getBookStats(b1.getBook_id()).getPopularityScore()))
                .limit(MAX_POPULAR_BOOKS)
                .collect(Collectors.toList());

        if (booksWithStats.isEmpty()) {
            logger.warning("Nessun libro con statistiche trovato, selezionando libri casuali invece");
        }

        return booksWithStats;
    }

    private List<Book> selectRandomBooksAsFallback() {
        List<Book> shuffledBooks = new ArrayList<>(allBooks);
        Collections.shuffle(shuffledBooks);

        // Assicurati di avere almeno MAX_POPULAR_BOOKS libri
        int availableBooks = Math.min(shuffledBooks.size(), MAX_POPULAR_BOOKS);
        List<Book> selectedBooks = shuffledBooks.stream().limit(availableBooks).collect(Collectors.toList());

        logger.info("FALLBACK: Selezionati " + selectedBooks.size() + " libri casuali da " + shuffledBooks.size() + " disponibili");
        return selectedBooks;
    }

    private void handlePopularBooksSelectionError() {
        if (popularBooks.isEmpty()) {
            popularBooks.addAll(selectRandomBooksAsFallback());
        }
        popularBooksSelected = true;
    }

    /**
     * Seleziona libri popolari (metodo pubblico per uso esterno)
     */
    public void selectPopularBooks() {
        selectPopularBooksInternal();
    }

    /**
     * Forza la riselezione dei libri popolari (utile dopo aggiornamenti statistiche)
     */
    public void refreshPopularBooks() {
        popularBooks.clear();
        popularBooksSelected = false;
        if (loaded) {
            selectPopularBooksInternal();
        }
    }
}
