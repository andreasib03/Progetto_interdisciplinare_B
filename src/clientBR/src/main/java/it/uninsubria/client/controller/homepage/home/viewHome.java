package it.uninsubria.client.controller.homepage.home;

import it.uninsubria.client.cache.ClientCacheManager;
import it.uninsubria.client.controller.ControllerBase;
import it.uninsubria.client.controller.homepage.BookDetailSimpleController;
import it.uninsubria.client.utils.classesLogic.ClassGetImages;
import it.uninsubria.client.utils.classesUI.BookServiceManager;
import it.uninsubria.client.utils.classesUI.BookStatisticsManager;
import it.uninsubria.client.utils.classesUI.EveryView;
import it.uninsubria.client.utils.classesUI.LanguageManager;
import it.uninsubria.client.utils.classesUI.NotificationManager;
import it.uninsubria.client.utils.classesUI.ResourceCache;
import it.uninsubria.client.utils.classesUI.ServiceLocator;
import it.uninsubria.client.utils.classesUI.ThemeManager;
import it.uninsubria.client.utils.classesUI.ThreadPoolManager;
import it.uninsubria.shared.model.Book;
import it.uninsubria.shared.model.Review;
import it.uninsubria.shared.utils.AppConstants;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;


/**
 * Controller per la visualizzazione della home page.
 *
 * NUOVA FUNZIONALITÀ (2024): Le ImageView mostrano ora libri CONSIGLIATI per il libro principale
 * invece di libri diversi dalla lista principale. Il primo elemento (myImageView1) mostra sempre
 * il libro "più popolare", mentre gli altri mostrano libri consigliati per esso.
 * Se non ci sono consigli disponibili, viene usata la logica originale come fallback.
 */
public class viewHome extends ControllerBase {

    @FXML
    private ImageView myImageView1;
    @FXML
    private ImageView myImageView2;
    @FXML
    private ImageView myImageView3;
    @FXML
    private ImageView myImageView4;
    @FXML
    private ImageView myImageView5;
    @FXML
    private ImageView myImageView6;
    @FXML
    private ImageView myImageView7;
    @FXML
    private ImageView mainImage;

    @FXML
    private Label showTitleBook1, showTitleBook2, showTitleBook3, showTitleBook4, showTitleBook5, showTitleBook6, showTitleBook7;
    @FXML
    private Label mainImageTitleLabel;

    @FXML
    private ProgressIndicator recommendationsLoadingIndicator;

    // Labels per le reviews dei primi 4 libri
    @FXML
    private Label reviewUserLabel1, reviewStarsLabel1, reviewNoteLabel1;
    @FXML
    private Label reviewUserLabel2, reviewStarsLabel2, reviewNoteLabel2;
    @FXML
    private Label reviewUserLabel3, reviewStarsLabel3, reviewNoteLabel3;
    @FXML
    private Label reviewUserLabel4, reviewStarsLabel4, reviewNoteLabel4;

    @FXML
    private Button viewAllReviewsButton;

    private List<ImageView> imageViews;
    private List<Label> titleLabels;
    private List<Book> books = new ArrayList<>();
    private static final int MAX_BOOKS_TO_DISPLAY = 7; // Limite per performance

    // Libro selezionato dalla ricerca per essere mostrato nella home page
    private static Book selectedBookFromSearch = null;
    // Flag per forzare il ricaricamento quando si torna alla home page
    private static boolean forceReloadBooks = false;

    // Cache per la selezione libri home page
    private static List<Book> cachedHomeBooks = null;
    private static long lastHomeBookUpdate = 0;
    private static final long HOME_BOOK_CACHE_DURATION = AppConstants.Time.TIMEOUT_10_MINUTES;

    // Flag per evitare inizializzazioni multiple
    private boolean initialized = false;
    private static final Logger logger = Logger.getLogger(viewHome.class.getName());

    //




    private void setupImageView(ImageView imageView) {
        imageView.setFitWidth(150);
        imageView.setFitHeight(200);

        // Clip arrotondato
        Rectangle clip = new Rectangle();
        clip.setArcWidth(30);
        clip.setArcHeight(30);

        // Collega le dimensioni del clip a quelle dell'imageView
        clip.widthProperty().bind(imageView.fitWidthProperty());
        clip.heightProperty().bind(imageView.fitHeightProperty());

        imageView.setClip(clip);
    }

    public void initializeData(){
        // Carica libri dal database in background per non bloccare l'UI
        // I consigli verranno caricati automaticamente dentro loadBooksFromDatabaseAsync()
        loadBooksFromDatabaseAsync();
    }

    /**
     * Metodo chiamato quando la vista diventa nuovamente visibile
     * Utile per aggiornare dinamicamente i consigli quando l'utente naviga
     */
    public void onViewActivated() {
        logger.info(resolveString("%viewhome.log.vista.riattivata"));
        refreshSuggestionsDisplay();
    }

    /**
     * Forza l'aggiornamento dinamico dei consigli
     * Può essere chiamato manualmente o da eventi esterni
     */
    public void forceSuggestionsRefresh() {
        logger.info(resolveString("%viewhome.log.refresh.forzato"));
        loadAndDisplaySuggestedBooks();
    }

    /**
     * Mostra suggerimenti di fallback quando non ci sono consigli reali
     */
    private void openFallbackSuggestionsWindow(Book book) {
        try {
            // Crea una finestra di dialogo invece di una finestra modale completa
            Alert suggestionsAlert = new Alert(Alert.AlertType.INFORMATION);
            suggestionsAlert.setTitle(resolveString("%viewhome.suggestions.fallback.title"));
            suggestionsAlert.setHeaderText(java.text.MessageFormat.format(resolveString("%viewhome.suggestions.fallback.header"), book.getTitle()));

            // Crea contenuto con suggerimenti basati su genere/autore simili
            StringBuilder content = new StringBuilder();
            content.append(resolveString("%viewhome.suggestions.fallback.content.header")).append("\n\n");

            // Aggiungi alcuni libri dalla lista popolare come esempi
            List<Book> fallbackBooks = books.stream()
                .filter(b -> b.getBook_id() != book.getBook_id()) // Escludi il libro corrente
                .limit(5)
                .collect(java.util.stream.Collectors.toList());

            for (int i = 0; i < fallbackBooks.size(); i++) {
                Book fallbackBook = fallbackBooks.get(i);
                content.append(String.format("%d. \"%s\"\n   %s %s\n\n",
                    i + 1,
                    fallbackBook.getTitle(),
                    resolveString("%viewhome.suggestions.book.author.prefix"),
                    fallbackBook.getAuthors()));
            }

            content.append(resolveString("%viewhome.suggestions.fallback.footer"));

            suggestionsAlert.setContentText(content.toString());
            suggestionsAlert.getDialogPane().setMinWidth(500);
            suggestionsAlert.getDialogPane().setMinHeight(400);

            suggestionsAlert.showAndWait();

        } catch (RuntimeException e) {
            logger.log(Level.SEVERE, "Errore nell'apertura finestra suggerimenti fallback", e);
            showErrorAlert(java.text.MessageFormat.format(resolveString("%viewhome.error.fallback.suggestions"), e.getMessage()));
        }
    }

    /**
     * Gestisce il click del pulsante "Vedi suggerimenti" per mostrare tutti i libri consigliati
     */
    private void showImageLoading() {
        imagesLoadingCount++;
        if (recommendationsLoadingIndicator != null) {
            Platform.runLater(() -> {
                recommendationsLoadingIndicator.setVisible(true);
                logger.fine("🖼️ MOSTRATO loading indicator (conteggio: " + imagesLoadingCount + ")");
            });
        }
    }

    /**
     * Nasconde l'indicatore di caricamento quando tutte le immagini sono caricate
     */
    private void hideImageLoading() {
        imagesLoadingCount--;
        if (imagesLoadingCount <= 0) {
            imagesLoadingCount = 0; // Sicurezza
            if (recommendationsLoadingIndicator != null) {
                Platform.runLater(() -> {
                    recommendationsLoadingIndicator.setVisible(false);
                    logger.fine("✅ NASCOSTO loading indicator - tutte le immagini caricate");
                });
            }
        } else {
            logger.fine("🖼️ Loading ancora in corso (rimanenti: " + imagesLoadingCount + ")");
        }
    }

    /**
     * Timeout di sicurezza per nascondere il loading dopo 30 secondi
     * Utile se qualche caricamento immagine rimane bloccato
     */
    private void setupImageLoadingTimeout() {
        java.util.Timer timer = new java.util.Timer(true);
        timer.schedule(new java.util.TimerTask() {
            @Override
            public void run() {
                if (recommendationsLoadingIndicator != null && recommendationsLoadingIndicator.isVisible()) {
                    Platform.runLater(() -> {
                        recommendationsLoadingIndicator.setVisible(false);
                        logger.warning("⏰ TIMEOUT loading immagini - nascosto automaticamente dopo 30 secondi");
                    });
                }
            }
        }, 30 * 1000);
    }

    /**
     * Metodo chiamato quando vengono aggiunti nuovi consigli nel sistema
     * Utile per aggiornare immediatamente la homepage quando altri utenti aggiungono consigli
     */
    public void onNewSuggestionsAdded() {
        logger.info(resolveString("%viewhome.log.nuovi.consigli"));
        forceSuggestionsRefresh();
    }

    /**
     * Metodo helper per applicare il libro selezionato dalla ricerca ai libri selezionati
     */
    private List<Book> applySelectedBookFromSearch(List<Book> selectedBooks) {
        if (selectedBookFromSearch != null) {
            logger.info("🔍 LIBRO SELEZIONATO DALLA RICERCA: " + selectedBookFromSearch.getTitle() + " - Verrà mostrato come libro principale");
            List<Book> modifiedBooks = new ArrayList<>(selectedBooks);
            if (!modifiedBooks.isEmpty()) {
                modifiedBooks.set(0, selectedBookFromSearch);
                logger.fine("Book replaced - Before: " + selectedBooks.get(0).getTitle() + ", After: " + selectedBookFromSearch.getTitle());
            } else {
                modifiedBooks = List.of(selectedBookFromSearch);
                logger.fine("Empty list, using only the selected book");
            }
            selectedBooks = modifiedBooks;
            selectedBookFromSearch = null; // Reset per la prossima volta
        } else {
            logger.fine("No book selected from search");
        }
        return selectedBooks;
    }

    /**
     * Carica libri dal database e attende che tutte le immagini siano pronte prima di mostrare l'UI.
     * Se è stato selezionato un libro dalla ricerca, viene posizionato come "più popolare" (primo libro).
     */
    private void loadBooksFromDatabaseAsync() {
        BookServiceManager bookManager = BookServiceManager.getInstance();

        // Usa i libri già caricati all'avvio dell'applicazione
        if (bookManager.isLoaded()) {
            // Usa direttamente i libri popolari già selezionati dal BookServiceManager
            List<Book> selectedBooks;
            if (bookManager.arePopularBooksSelected()) {
                selectedBooks = bookManager.getPopularBooks();
                logger.info("Usando libri popolari già selezionati dal BookServiceManager: " + selectedBooks.size());
            } else {
                // Fallback: seleziona libri popolari ora (non dovrebbe succedere normalmente)
                List<Book> allBooks = bookManager.getAllBooks();
                selectedBooks = selectFeaturedBooks(allBooks);
                logger.warning("Libri popolari non erano stati selezionati, fatto ora: " + selectedBooks.size());
             }

            // Log dei libri selezionati per confronto con precaricamento splash screen
            logger.info("🏠 Homepage: libri selezionati dal BookServiceManager (" + selectedBooks.size() + " libri):");
            for (int i = 0; i < selectedBooks.size(); i++) {
                Book book = selectedBooks.get(i);
                String cacheKey = book.getTitle() + "_" + book.getAuthors() + "_" + book.getPublish_date_year();
                boolean inCache = it.uninsubria.client.utils.classesLogic.ClassGetImages.isImageInCache(cacheKey);
                logger.info("   " + (i+1) + ". " + book.getTitle() + " (ID: " + book.getBook_id() + ") - Cache: " +
                    (inCache ? "✅ PRESENTE" : "❌ MANCANTE"));
            }

            selectedBooks = applySelectedBookFromSearch(selectedBooks);

            // Crea una variabile final per le lambda
            final List<Book> finalSelectedBooks = selectedBooks;

            // Le immagini sono già state precaricate durante lo splash screen
                Platform.runLater(() -> {
                    books = finalSelectedBooks;
                    loadAndDisplaySuggestedBooks(); // Carica e mostra libri consigliati
                    // Cancella le recensioni esistenti prima di caricarne di nuove
                    clearExistingReviews();
                    loadReviewsForDisplayedBooks(); // Carica reviews solo per i libri mostrati nelle vignette
                });
        } else {
            // Fallback: carica i libri se non sono stati precaricati
            CompletableFuture.supplyAsync(() -> {
                try {
                    // Usa libri popolari già selezionati dal BookServiceManager
                    List<Book> selectedBooks = bookManager.getPopularBooks();

                    selectedBooks = applySelectedBookFromSearch(selectedBooks);

                    return selectedBooks;

                } catch (RuntimeException e) {
                    logger.severe(java.text.MessageFormat.format("Errore nel caricamento dei libri: {0}", e.getMessage()));
                    // Fallback: usa libri di test se il DB non è disponibile
                    return getFallbackBooks();
                }
            }).thenCompose(selectedBooks -> {
                // Crea una variabile final per le lambda
                final List<Book> finalSelectedBooks = selectedBooks;
                // Le immagini sono già state precaricate durante lo splash screen
                return CompletableFuture.completedFuture(finalSelectedBooks);
            }).thenAccept(finalSelectedBooks -> {
                // Aggiorna l'UI nel thread JavaFX
                Platform.runLater(() -> {
                    books = finalSelectedBooks;
                    displayBooks();
                     loadReviewsForDisplayedBooks(); // Carica reviews per i libri mostrati nelle vignette
                });
            }).exceptionally(throwable -> {
                // In caso di errore, usa fallback
                logger.severe(java.text.MessageFormat.format("Errore nel caricamento completo: {0}", throwable.getMessage()));
                 Platform.runLater(() -> {
                     books = getFallbackBooks();
                     loadAndDisplaySuggestedBooks(); // Anche per fallback, prova a caricare consigli
                      loadReviewsForDisplayedBooks(); // Carica reviews per i libri mostrati nelle vignette
                 });
                return null;
            });
        }
    }

    /**
     * Seleziona i libri da mostrare nella home page (usato come fallback)
     * Normalmente i libri popolari sono già selezionati dal BookServiceManager
     */
    private List<Book> selectFeaturedBooks(List<Book> allBooks) {
        if (allBooks == null || allBooks.isEmpty()) {
            return getFallbackBooks();
        }

        long currentTime = System.currentTimeMillis();

        // Usa cache se valida e non vuota
        if (cachedHomeBooks != null && !cachedHomeBooks.isEmpty() &&
            (currentTime - lastHomeBookUpdate) < HOME_BOOK_CACHE_DURATION) {
            // Rimuovi eventuali duplicati dalla cache prima di restituirla
            List<Book> uniqueCachedBooks = cachedHomeBooks.stream()
                .collect(Collectors.toMap(
                    Book::getBook_id,
                    book -> book,
                    (existing, replacement) -> existing
                ))
                .values()
                .stream()
                .collect(Collectors.toList());
            return new ArrayList<>(uniqueCachedBooks); // Restituisci copia per sicurezza
        }

        // Seleziona libri popolari, ma se non ce ne sono abbastanza, usa libri qualsiasi
        BookStatisticsManager statsManager = BookStatisticsManager.getInstance();
        List<Book> popularBooks = getPopularBooks(allBooks, statsManager, MAX_BOOKS_TO_DISPLAY);

        // Riempi con libri casuali se necessario
        popularBooks = fillWithRandomBooks(popularBooks, allBooks, MAX_BOOKS_TO_DISPLAY);

        // Salva in cache
        cachedHomeBooks = new ArrayList<>(popularBooks);
        lastHomeBookUpdate = currentTime;

        // 🐛 DEBUG: Log libri selezionati per homepage
        logger.info(java.text.MessageFormat.format(resolveString("%viewhome.debug.homepage.books.selected"), popularBooks.size()));
        for (int i = 0; i < popularBooks.size(); i++) {
            Book book = popularBooks.get(i);
            logger.info(java.text.MessageFormat.format(resolveString("%viewhome.debug.book.selected"), i+1, book.getTitle(), book.getBook_id()));
        }

        return popularBooks;
    }

    /**
     * Riempie la lista di libri popolari con libri casuali se necessario
     */
    private List<Book> fillWithRandomBooks(List<Book> popularBooks, List<Book> allBooks, int targetCount) {
        if (popularBooks.isEmpty()) {
            logger.warning(resolveString("%viewhome.debug.popular.books.none"));
            List<Book> shuffledBooks = new ArrayList<>(allBooks);
            Collections.shuffle(shuffledBooks);
            return shuffledBooks.stream().limit(targetCount).collect(Collectors.toList());
        } else if (popularBooks.size() < targetCount) {
            List<Book> remainingBooks = allBooks.stream()
                .filter(book -> !popularBooks.contains(book))
                .collect(Collectors.toList());
            Collections.shuffle(remainingBooks);
            int booksNeeded = targetCount - popularBooks.size();
            popularBooks.addAll(remainingBooks.stream().limit(booksNeeded).collect(Collectors.toList()));
        }
        return popularBooks;
    }

    /**
     * Ottiene i libri più popolari basati sulle statistiche aggregate
     * Se non ci sono statistiche per alcuni libri, li esclude dalla selezione
     */
    private List<Book> getPopularBooks(List<Book> allBooks, BookStatisticsManager statsManager, int count) {
        List<Book> booksWithStats = allBooks.stream()
                .filter(book -> statsManager.getBookStats(book.getBook_id()) != null)
                .sorted((b1, b2) -> Double.compare(
                    statsManager.getBookStats(b2.getBook_id()).getPopularityScore(),
                    statsManager.getBookStats(b1.getBook_id()).getPopularityScore()))
                .limit(count)
                .collect(Collectors.toList());

        logger.info(java.text.MessageFormat.format(resolveString("%viewhome.debug.popular.books.found"), booksWithStats.size(), allBooks.size()));
        return booksWithStats;
    }

    /**
     * Invalida la cache dei libri home page - da chiamare quando ci sono cambiamenti significativi
     */
    public static void invalidateHomeBookCache() {
        cachedHomeBooks = null;
        lastHomeBookUpdate = 0;
        // Invalida anche i libri popolari nel BookServiceManager
        BookServiceManager.getInstance().refreshPopularBooks();
        logger.info(resolveString("%viewhome.debug.cache.invalidated"));
    }

    /**
     * Forza l'aggiornamento dei libri nella home page
     */
    public void refreshHomeBooks() {
        invalidateHomeBookCache();
        // Ricarica i libri se necessario
        initializeData();
    }

    /**
     * Imposta un libro da mostrare nella home page (selezionato dalla ricerca)
     */
    public static void setSelectedBookFromSearch(Book book) {
        selectedBookFromSearch = book;
        forceReloadBooks = true;
        logger.fine(java.text.MessageFormat.format(resolveString("%viewhome.debug.book.from.search.called"),
                          (book != null ? book.getTitle() + " (ID: " + book.getBook_id() + ")" : resolveString("%viewhome.debug.book.from.search.null"))));
    }

    /**
     * Forza il refresh completo della homepage inclusi libri e recensioni
     */
    public static void refreshHomePage() {
        invalidateHomeBookCache();
        forceReloadBooks = true;
        logger.info(resolveString("%viewhome.debug.homepage.refresh.forced"));
    }

    /**
     * Libri di fallback se il database non è disponibile
     */
    private List<Book> getFallbackBooks() {
        return List.of(
            new Book("Under the Dome", "Stephen King", 2009),
            new Book("The Shining", "Stephen King", 1977),
            new Book("It", "Stephen King", 1986),
            new Book("Misery", "Stephen King", 1987),
            new Book("1408", "Stephen King", 2002),
            new Book("The Green Mile", "Stephen King", 1996),
            new Book("Fairy Tale", "Stephen King", 2022)
        );
    }

    /**
     * Carica i libri consigliati per il libro principale e li visualizza.
     *
     * LOGICA USER-FRIENDLY implementata:
     * - Prende il libro principale (posizione 0, il "più popolare")
     * - Carica SOLO i libri consigliati reali per esso dal database
     * - Mostra i consigli reali nelle posizioni 1-N delle ImageView
     * - Posizioni senza consigli: mostra placeholder con messaggio informativo
     * - NESSUN riempimento artificiale con libri popolari
     *
     * Questo garantisce trasparenza: l'utente vede solo consigli reali
     * o un messaggio chiaro quando non ce ne sono.
     */
    private void loadAndDisplaySuggestedBooks() {
        // Evita chiamate multiple simultanee
        if (suggestionsLoading) {
            logger.fine("🔄 Caricamento consigli già in corso, skip");
            return;
        }

        if (books.isEmpty()) {
            logger.warning("Nessun libro disponibile per caricare suggerimenti");
            return;
        }

        suggestionsLoading = true;
        // Reset del contatore immagini per un nuovo caricamento
        imagesLoadingCount = 0;
        Book mainBook = books.get(0); // Libro principale (il più popolare)
         logger.info(java.text.MessageFormat.format(resolveString("%viewhome.log.caricamento.consigli"), mainBook.getTitle()));

        CompletableFuture.supplyAsync(() -> {
            try {
                // Prima controlla cache TTL-based per i suggerimenti
                String cacheKey = "suggestions:" + mainBook.getBook_id();
                List<Book> cachedSuggestions = ClientCacheManager.get(cacheKey);
                if (cachedSuggestions != null) {
                    logger.fine(java.text.MessageFormat.format(resolveString("%viewhome.log.suggestions.cached"), mainBook.getTitle()));
                    return cachedSuggestions;
                }

                // OTTIENI SOLO CONSIGLI DEL LIBRO PRINCIPALE
                List<Book> mainBookSuggestions = ServiceLocator.getBookService().getSuggestedBooks(mainBook.getBook_id());

                if (mainBookSuggestions != null && !mainBookSuggestions.isEmpty()) {
                    // Rimuovi duplicati basandosi sul book_id (non sull'oggetto Book)
                    Map<Integer, Book> uniqueSuggestionsMap = new LinkedHashMap<>();
                    for (Book suggestion : mainBookSuggestions) {
                        uniqueSuggestionsMap.putIfAbsent(suggestion.getBook_id(), suggestion);
                    }

                    List<Book> suggestions = new ArrayList<>(uniqueSuggestionsMap.values());

                     // Salva in cache TTL-based
                     ClientCacheManager.put(cacheKey, suggestions);

                     logger.info(java.text.MessageFormat.format(resolveString("%viewhome.log.consigli.trovati"), mainBook.getTitle(), suggestions.size()));
                    return suggestions;
                }

                // NESSUN CONSIGLIO DISPONIBILE - mostra messaggio invece di libri fittizi
                 logger.info(java.text.MessageFormat.format(resolveString("%viewhome.log.nessun.consiglio"), mainBook.getTitle()));
                return new ArrayList<Book>(); // Lista vuota per attivare logica placeholder

            } catch (Exception e) {
                logger.severe("Errore nel caricamento dei libri consigliati: " + e.getMessage());
                return new ArrayList<Book>(); // Ritorna lista vuota in caso di errore
            }
         }).thenAccept(finalSuggestedBooks -> {
             Platform.runLater(() -> {
                 try {
                     if (finalSuggestedBooks != null && !finalSuggestedBooks.isEmpty()) {
                          logger.info(java.text.MessageFormat.format(resolveString("%viewhome.log.consigli.reali.trovati"), finalSuggestedBooks.size(), mainBook.getTitle()));
                         displaySuggestedBooks(finalSuggestedBooks);
                     } else {
                          logger.info(java.text.MessageFormat.format(resolveString("%viewhome.log.nessun.consiglio.disponibile"), mainBook.getTitle()));
                         displaySuggestedBooks(finalSuggestedBooks); // Lista vuota per attivare placeholder

                     }
                } finally {
                    suggestionsLoading = false; // Reset flag sempre
                }
            });
        }).exceptionally(throwable -> {
             logger.severe(java.text.MessageFormat.format(resolveString("%viewhome.log.errore.caricamento.consigli"), throwable.getMessage()));
            suggestionsLoading = false; // Reset flag anche in caso di errore
            return null;
        });

        // Assicurati che i consigli vengano ricaricati periodicamente o su eventi
        // Questo rende la homepage sempre aggiornata con i consigli più recenti
        scheduleSuggestionsRefresh();
    }

    // Flag per evitare timer multipli
    private boolean refreshTimerScheduled = false;

    // Flag per evitare chiamate multiple simultanee di caricamento consigli
    private boolean suggestionsLoading = false;

    // Contatore per il caricamento immagini
    private int imagesLoadingCount = 0;

    /**
     * Pianifica aggiornamenti periodici dei consigli per mantenere la homepage dinamica
     */
    private void scheduleSuggestionsRefresh() {
        // Evita di schedulare timer multipli
        if (refreshTimerScheduled) {
             logger.fine(resolveString("%viewhome.log.timer.gia.schedulato"));
            return;
        }

        refreshTimerScheduled = true;

        // Ricarica i consigli ogni 5 minuti per mantenere la homepage dinamica
        // Questo è utile se l'utente lascia la pagina aperta e altri utenti aggiungono consigli
        java.util.Timer timer = new java.util.Timer(true);
        timer.scheduleAtFixedRate(new java.util.TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                     logger.fine(resolveString("%viewhome.log.refresh.periodico.consigli"));
                    refreshSuggestionsDisplay();
                });
            }
        }, AppConstants.Time.TIMEOUT_5_MINUTES, AppConstants.Time.TIMEOUT_5_MINUTES);

         logger.info(resolveString("%viewhome.log.timer.schedulato"));
    }



     /**
      * Visualizza i libri consigliati nell'interfaccia (LOGICA OTTIMIZZATA PER USER EXPERIENCE)
      *
      * Distribuzione user-friendly:
      * - Posizione 0: Libro principale (più popolare)
      * - Posizioni 1-6: Libri consigliati reali, altrimenti placeholder con messaggio
      * - Garanzia: Interfaccia consistente senza spazi vuoti confusi
      * - Click: Placeholder mostra messaggio informativo
      *
      * @param suggestedBooks Lista di libri consigliati (solo consigli reali, non riempimento artificiale)
      */
     private void displaySuggestedBooks(List<Book> suggestedBooks) {
         logger.fine("displaySuggestedBooks called with " + suggestedBooks.size() + " suggested books");

         // Setup timeout di sicurezza per il caricamento immagini
         setupImageLoadingTimeout();

         // Il libro principale è già nella posizione 0 (gestito da displayBooks())
         Book mainBook = books.isEmpty() ? null : books.get(0);

         for (int i = 0; i < imageViews.size(); i++) {
             ImageView imageView = imageViews.get(i);
             Label label = titleLabels.get(i);
             Book bookToShow;

             if (i == 0 && mainBook != null) {
                 // Prima posizione: sempre il libro principale
                 bookToShow = mainBook;
             } else if (i - 1 < suggestedBooks.size()) {
                 // Posizioni successive: libri consigliati reali
                 bookToShow = suggestedBooks.get(i - 1);
             } else {
                 // Se non ci sono abbastanza consigli, mostra placeholder
                 bookToShow = null;
             }

             if (bookToShow != null) {
                 logger.fine("Posizione " + i + " - Libro consigliato: " + bookToShow.getTitle() + " (ID: " + bookToShow.getBook_id() + ")");

                 setupImageView(imageView);
                 label.setText(java.text.MessageFormat.format(resolveString("%book.display.format"), bookToShow.getTitle(), bookToShow.getAuthors(), bookToShow.getPublish_date_year()));

                 int index = i;
                 Book finalBook = bookToShow;
                 imageView.setOnMouseClicked(e -> handleSuggestedBookClick(finalBook, index));

                 // MOSTRA LOADING prima di iniziare il caricamento
                 showImageLoading();

                 // Carica immagine di copertina (dovrebbe essere già in cache dallo splash screen)
                 String cacheKey = bookToShow.getTitle() + "_" + bookToShow.getAuthors() + "_" + bookToShow.getPublish_date_year();
                 boolean wasInCache = it.uninsubria.client.utils.classesLogic.ClassGetImages.isImageInCache(cacheKey);

                 String loadingType = wasInCache ? "✅ DA CACHE (ottimizzato)" : "⚠️ DA SERVER (caricamento in background)";
                 logger.fine("🖼️ Caricamento immagine per posizione " + index + " ('" + bookToShow.getTitle() + "'): " + loadingType);

                 // Log speciale per il libro selezionato dalla ricerca (posizione 0)
                 if (index == 0 && selectedBookFromSearch != null) {
                     logger.info(java.text.MessageFormat.format(resolveString("%viewhome.log.caricamento.immagine.libro.selezionato"), bookToShow.getTitle(), loadingType));
                 }

                 ClassGetImages.setImageFromCacheOrLoad(bookToShow, imageView, loadedImage -> {
                     // NASCONDI LOADING quando l'immagine è caricata
                     hideImageLoading();

                     if (index == 0 && loadedImage != null) {
                         // Il primo libro diventa automaticamente il "più popolare"
                         Platform.runLater(() -> {
                             mainImage.setImage(loadedImage);
                             mainImageTitleLabel.setText(java.text.MessageFormat.format(resolveString("%book.display.format"), finalBook.getTitle(), finalBook.getAuthors(), finalBook.getPublish_date_year()));

                             // Notifica discreta quando l'immagine del libro selezionato è caricata
                             if (!wasInCache) { // Solo se non era in cache (quindi è stata caricata ora)
                                  logger.info(java.text.MessageFormat.format(resolveString("%viewhome.log.immagine.caricata.libro.selezionato"), bookToShow.getTitle()));
                                 NotificationManager.showInfo("Immagine caricata per: " + bookToShow.getTitle());
                             }
                         });
                     }
                 });
             } else {
                 // MOSTRA PLACEHOLDER invece di nascondere completamente
                 logger.fine("Posizione " + i + " - Placeholder: nessun consiglio disponibile");

                 setupImageView(imageView);

                 Image placeholderImage = ResourceCache.getImage("/images/no-suggestions-placeholder.png");
                 imageView.setImage(placeholderImage);

                 label.setText(resolveString("%viewhome.suggestions.no.more.available"));

                 // Click handler per placeholder
                 imageView.setOnMouseClicked(e -> showNoSuggestionsMessage());

                 // Considera completato il caricamento per questa posizione
                 hideImageLoading();
             }
         }

         if (mainBook != null) {
             setupImageView(mainImage);
         }
     }

    /**
     * Visualizza i libri nell'interfaccia usando una lista specifica (logica originale - usato come fallback)
     */
    private void displayBooks(List<Book> booksToDisplay) {
        logger.fine("displayBooks called with " + booksToDisplay.size() + " books (fallback logic)");
        for (int i = 0; i < booksToDisplay.size() && i < imageViews.size(); i++) {
            Book book = booksToDisplay.get(i);
            ImageView imageView = imageViews.get(i);
            Label label = titleLabels.get(i);

            logger.fine(java.text.MessageFormat.format(resolveString("%viewhome.debug.fallback.book.elaborated"), i, book.getTitle(), book.getBook_id()));

            setupImageView(imageView);
            imageView.setVisible(true);
            label.setVisible(true);

            // Mostra titolo, autore e anno
            label.setText(java.text.MessageFormat.format(resolveString("%book.display.format"), book.getTitle(), book.getAuthors(), book.getPublish_date_year()));

            int index = i;
            Book bookForClick = book; // Capture for lambda
            imageView.setOnMouseClicked(e -> handleImageClick(bookForClick, index));

            // Carica immagine di copertina usando cache ottimizzata (precaricata durante splash screen)
            String cacheKey = book.getTitle() + "_" + book.getAuthors() + "_" + book.getPublish_date_year();
            boolean wasInCache = it.uninsubria.client.utils.classesLogic.ClassGetImages.isImageInCache(cacheKey);

            logger.fine(java.text.MessageFormat.format(resolveString("%viewhome.debug.fallback.image.loading"), index, book.getTitle(),
                (wasInCache ? resolveString("%viewhome.debug.cache.hit") : resolveString("%viewhome.debug.cache.miss"))));

            ClassGetImages.setImageFromCacheOrLoad(book, imageView, loadedImage -> {
                 if (index == 0 && loadedImage != null) {
                    // Il primo libro diventa automaticamente il "più popolare"
                    Platform.runLater(() -> {
                        mainImage.setImage(loadedImage);
                        mainImageTitleLabel.setText(java.text.MessageFormat.format(resolveString("%book.display.format"), book.getTitle(), book.getAuthors(), book.getPublish_date_year()));
                    });
                }
            });
        }

        // Nasconde elementi extra se ci sono meno libri della capacità massima
        for (int i = booksToDisplay.size(); i < imageViews.size(); i++) {
            imageViews.get(i).setVisible(false);
            titleLabels.get(i).setVisible(false);
        }

        if (!booksToDisplay.isEmpty()) {
            setupImageView(mainImage);
        }
    }

    /**
     * Versione compatibilità che usa this.books
     */
    private void displayBooks() {
        displayBooks(this.books);
    }

    /**
     * Metodi helper per gestire le labels delle recensioni
     */
    private void getReviewLabels(int index, Label[] labels) {
        switch (index) {
            case 0 -> { labels[0] = reviewUserLabel1; labels[1] = reviewStarsLabel1; labels[2] = reviewNoteLabel1; }
            case 1 -> { labels[0] = reviewUserLabel2; labels[1] = reviewStarsLabel2; labels[2] = reviewNoteLabel2; }
            case 2 -> { labels[0] = reviewUserLabel3; labels[1] = reviewStarsLabel3; labels[2] = reviewNoteLabel3; }
            case 3 -> { labels[0] = reviewUserLabel4; labels[1] = reviewStarsLabel4; labels[2] = reviewNoteLabel4; }
        }
    }

    private void clearReviewLabels(int index) {
        Label[] labels = new Label[3];
        getReviewLabels(index, labels);
        for (Label label : labels) {
            if (label != null) label.setText("");
        }
    }

    private boolean areReviewLabelsAvailable(int index) {
        Label[] labels = new Label[3];
        getReviewLabels(index, labels);
        return labels[0] != null && labels[1] != null && labels[2] != null;
    }

    /**
     * Cancella le recensioni esistenti dall'interfaccia
     */
    private void clearExistingReviews() {
        logger.fine(resolveString("%viewhome.debug.reviews.clear"));
        for (int i = 0; i < 4; i++) {
            clearReviewLabels(i);
        }
    }

    /**
     * Carica le reviews SOLO del libro principale mostrato nella home page
     * Ogni sezione review mostra recensioni DIVERSE dello stesso libro (diversi utenti)
     */
    private void loadReviewsForDisplayedBooks() {
        if (books.isEmpty()) {
            logger.fine(resolveString("%viewhome.debug.reviews.load.none"));
            return;
        }

        // Prendi SOLO il libro principale (il primo della lista)
        Book mainBook = books.get(0);
        logger.fine(java.text.MessageFormat.format(resolveString("%viewhome.debug.reviews.load.main"), mainBook.getTitle(), mainBook.getBook_id()));

        CompletableFuture.runAsync(() -> {
            try {
                // Carica recensioni SOLO del libro principale
                List<Review> mainBookReviews = ServiceLocator.getReviewsService().getReviewsByBook(mainBook.getBook_id());

                logger.fine(java.text.MessageFormat.format(resolveString("%viewhome.debug.reviews.main.found"),
                    mainBook.getTitle(), mainBookReviews != null ? mainBookReviews.size() : 0));

                Platform.runLater(() -> {
                    // Distribuisci le recensioni nelle 4 sezioni, prendendo recensioni diverse dello stesso libro
                    for (int sectionIndex = 0; sectionIndex < 4; sectionIndex++) {
                        if (mainBookReviews != null && !mainBookReviews.isEmpty() && sectionIndex < mainBookReviews.size()) {
                            // Prendi una recensione diversa per ogni sezione (diversi utenti)
                            Review selectedReview = mainBookReviews.get(sectionIndex);

                            // Mostra la recensione trovata per questa sezione
                            List<Review> singleReviewList = List.of(selectedReview);
                            updateReviewUI(sectionIndex, singleReviewList);

                            String userName = selectedReview.getUser() != null ?
                                (selectedReview.getUser().getName() != null ? selectedReview.getUser().getName() : selectedReview.getUser().getID()) :
                                resolveString("%viewhome.anonymous.user");

                            logger.fine(java.text.MessageFormat.format(resolveString("%viewhome.debug.reviews.section.assigned.main"),
                                sectionIndex + 1, mainBook.getTitle(), userName));
                        } else {
                            // Non ci sono abbastanza recensioni per questa sezione
                            updateReviewUI(sectionIndex, null);
                            logger.fine(java.text.MessageFormat.format(resolveString("%viewhome.debug.reviews.section.empty"), sectionIndex + 1));
                        }
                    }
                });

            } catch (Exception e) {
                logger.severe(java.text.MessageFormat.format(resolveString("%viewhome.log.loading.reviews.error.main"), mainBook.getTitle(), e.getMessage()));
                Platform.runLater(() -> {
                    // Mostra "nessuna recensione" in tutte le sezioni
                    for (int i = 0; i < 4; i++) {
                        updateReviewUI(i, null);
                    }
                });
            }
        });
    }

    /**
     * Aggiorna l'interfaccia delle reviews per un libro specifico
     */
    private void updateReviewUI(int bookIndex, List<Review> reviews) {
        logger.fine(java.text.MessageFormat.format(resolveString("%viewhome.debug.reviews.ui.update"), bookIndex, (reviews != null ? reviews.size() : 0)));

        // Assicurati che i controlli siano disponibili
        if (!areReviewLabelsAvailable(bookIndex)) {
            logger.fine(java.text.MessageFormat.format(resolveString("%viewhome.debug.reviews.ui.null"), bookIndex));
            return;
        }

        Label[] labels = new Label[3];
        getReviewLabels(bookIndex, labels);
        Label userLabel = labels[0], starsLabel = labels[1], noteLabel = labels[2];

        if (reviews == null || reviews.isEmpty()) {
            // Nessuna recensione disponibile
            logger.fine(java.text.MessageFormat.format(resolveString("%viewhome.debug.reviews.ui.none"), bookIndex));
            userLabel.setText(resolveString("%homepage.no.reviews"));
            starsLabel.setText("");
            noteLabel.setText("");
        } else {
            // Verifica che ci sia un libro nella posizione corrispondente
            if (bookIndex < books.size()) {
                Book currentBook = books.get(bookIndex);
                logger.fine(java.text.MessageFormat.format(resolveString("%viewhome.debug.reviews.ui.book"), currentBook.getTitle()));
            }

            // Scegli la review: se c'è una sola review specifica, usa quella; altrimenti random
            Review selectedReview;
            if (reviews.size() == 1) {
                // Usa la review specifica assegnata a questa sezione
                selectedReview = reviews.get(0);
                logger.fine(java.text.MessageFormat.format(resolveString("%viewhome.debug.reviews.ui.specific"), bookIndex + 1));
            } else {
                // Scegli una review random (comportamento legacy)
                selectedReview = reviews.get((int) (Math.random() * reviews.size()));
                logger.fine(java.text.MessageFormat.format(resolveString("%viewhome.debug.reviews.ui.random"), bookIndex + 1));
            }

            // Mostra il nome utente (o ID se nome non disponibile)
            String userName = selectedReview.getUser() != null ?
                (selectedReview.getUser().getName() != null ? selectedReview.getUser().getName() : selectedReview.getUser().getID()) :
                resolveString("%viewhome.anonymous.user");
            userLabel.setText(resolveString("%homepage.user.label") + userName);

            // Mostra le stelle come simboli ★ più grandi con punteggio numerico
            int fullStars = (int) Math.floor(selectedReview.averageScore());
            int emptyStars = 5 - fullStars;
            String starsDisplay = "★".repeat(Math.max(1, fullStars)) +
                                "☆".repeat(Math.max(0, emptyStars)) +
                                " (" + String.format("%.1f", selectedReview.averageScore()) + "/5)";
            starsLabel.setText(starsDisplay);

            // Mostra TUTTE le note disponibili della recensione
            String allNotes = getAllReviewNotes(selectedReview);
            noteLabel.setText(allNotes != null && !allNotes.trim().isEmpty() ? allNotes : resolveString("%homepage.no.notes"));
        }
    }

    /**
     * Estrae TUTTE le note dalle varie categorie di review disponibili
     */
    private String getAllReviewNotes(Review review) {
        java.util.List<String> availableNotes = new java.util.ArrayList<>();

        // Aggiungi tutte le note disponibili
        if (review.getNoteStyleScore() != null && !review.getNoteStyleScore().trim().isEmpty()) {
            availableNotes.add("Stile: " + review.getNoteStyleScore());
        }
        if (review.getNoteContentScore() != null && !review.getNoteContentScore().trim().isEmpty()) {
            availableNotes.add("Contenuto: " + review.getNoteContentScore());
        }
        if (review.getNotePleasantnessScore() != null && !review.getNotePleasantnessScore().trim().isEmpty()) {
            availableNotes.add("Gradevolezza: " + review.getNotePleasantnessScore());
        }
        if (review.getNoteOdnessScore() != null && !review.getNoteOdnessScore().trim().isEmpty()) {
            availableNotes.add("Originalità: " + review.getNoteOdnessScore());
        }
        if (review.getNoteEditionScore() != null && !review.getNoteEditionScore().trim().isEmpty()) {
            availableNotes.add("Edizione: " + review.getNoteEditionScore());
        }
        if (review.getNoteFinalScore() != null && !review.getNoteFinalScore().trim().isEmpty()) {
            availableNotes.add("Voto finale: " + review.getNoteFinalScore());
        }

        // Restituisci tutte le note unite, o null se nessuna disponibile
        return availableNotes.isEmpty() ? null : String.join("\n", availableNotes);
    }


    @FXML
    public void initialize(){
        // Evita inizializzazioni multiple
        if (initialized) {
            return;
        }
        initialized = true;

        imageViews = List.of(
            myImageView1, myImageView2, myImageView3,
            myImageView4, myImageView5, myImageView6, myImageView7
        );

        titleLabels = List.of(
            showTitleBook1, showTitleBook2, showTitleBook3,
            showTitleBook4, showTitleBook5, showTitleBook6, showTitleBook7
        );

        // Controlla se dobbiamo forzare il ricaricamento dei libri PRIMA di initializeData
        if (forceReloadBooks) {
            // Non chiamare initializeData() normale, usa loadBooksFromDatabaseAsync direttamente
            loadBooksFromDatabaseAsync();
            forceReloadBooks = false; // Reset del flag
        } else {
            initializeData();
        }

        // Inizializza il pulsante visualizza consigliati
        if (viewAllReviewsButton != null) {
            logger.fine(resolveString("%viewhome.debug.pulsante.viewallreviews"));
            viewAllReviewsButton.setOnAction(e -> handleViewAllReviews());
        } else {
            logger.warning(resolveString("%viewhome.debug.pulsante.null"));
        }
    }

    //Graphics and logic for click events

     /**
      * Gestisce il click su un libro consigliato
      */
     private void handleSuggestedBookClick(Book clickedBook, int index) {
         logger.info(java.text.MessageFormat.format(resolveString("%viewhome.log.libro.consigliato.cliccato"), clickedBook.getTitle(), index));

         if (index == 0) {
             // Se è il libro principale (primo dei consigliati), apri la schermata di dettaglio
             logger.info(java.text.MessageFormat.format(resolveString("%viewhome.log.libro.principale.cliccato"), clickedBook.getTitle()));
             openBookDetailNewWindow(clickedBook);
             return;
         }

         // Sostituisci il libro principale con quello consigliato e ricarica tutto
         if (!books.isEmpty()) {
             Book oldMainBook = books.get(0);
             books.set(0, clickedBook);
             logger.info(java.text.MessageFormat.format(resolveString("%viewhome.log.libro.principale.sostituito"), oldMainBook.getTitle(), clickedBook.getTitle()));

             // Forza aggiornamento immediato e completo
             forceSuggestionsRefresh();
             clearExistingReviews();
             loadReviewsForDisplayedBooks();

             logger.info(resolveString("%viewhome.log.homepage.aggiornata"));
         }
     }

    /**
     * Gestisce il click su un libro nella modalità fallback (logica originale)
     */
    private void handleImageClick(Book clickedBook, int index) {
        logger.info("Click su libro fallback: " + clickedBook.getTitle() + " alla posizione " + index);

        // Nella modalità fallback, mantieni il comportamento originale ma con i libri attuali
        List<Book> reorderedBooks = new ArrayList<>(books);
        if (reorderedBooks.contains(clickedBook)) {
            reorderedBooks.remove(clickedBook);
            reorderedBooks.add(0, clickedBook);
            books = reorderedBooks;

            // Ricarica l'interfaccia con il nuovo ordine
            loadAndDisplaySuggestedBooks(); // Prova prima i consigli, poi fallback
        } else {
            // Se il libro cliccato non è nei libri attuali, sostituiscilo come principale
            if (!books.isEmpty()) {
                books.set(0, clickedBook);
                loadAndDisplaySuggestedBooks();
            }
        }

        // Carica suggerimenti per questo libro in background
        loadSuggestionsForBook(clickedBook);
    }

    /**
     * Carica e mostra libri consigliati per il libro principale (LOGICA SEMPLIFICATA)
     *
     * Algoritmo:
     * 1. Prima priorità: Consigli diretti del libro principale (posizioni 1-6)
     * 2. Rimozione automatica duplicati con LinkedHashSet
     * 3. Seconda priorità: Fallback con libri popolari dalla cache
     * 4. Posizione 0: Sempre il libro principale
     *
     * Risultato: Homepage dinamica che mostra consigli quando disponibili,
     * altrimenti libri popolari senza duplicati
     *
     * @return true se i consigli sono stati caricati con successo, false altrimenti
     */
    public boolean refreshSuggestionsDisplay() {
        logger.info("🔄 REFRESH SUGGERIMENTI RICHIESTO - Ricarico consigli per homepage dinamica");
        loadAndDisplaySuggestedBooks();
        return true;
    }
    private void loadSuggestionsForBook(Book book) {
        // Per ora solo log, ma si può implementare per mostrare suggerimenti nell'UI
        CompletableFuture.runAsync(() -> {
            try {
                // Nota: per i suggerimenti servirebbe l'ID utente, che non abbiamo qui
                // Questa è una demo di come si potrebbero caricare i suggerimenti
                logger.info(java.text.MessageFormat.format(resolveString("%viewhome.log.loading.suggestions"), book.getTitle()));

                // Esempio di chiamata (richiederebbe userId):
                // List<Book> suggestions = ServiceLocator.getSuggestionsService().getDynamicSuggestedBooks(book.getBook_id(), libraryId, userId, 5);

            } catch (RuntimeException e) {
                logger.severe(java.text.MessageFormat.format(resolveString("%viewhome.log.loading.suggestions.error"), book.getTitle(), e.getMessage()));
            }
        });
    }

    /**
     * Gestisce il click del pulsante "Vedi suggerimenti" per mostrare tutti i libri consigliati
     */
    @FXML
    private void handleViewAllReviews() {
        if (books.isEmpty()) {
            showErrorAlert(resolveString("%viewhome.error.no.books.reviews"));
            return;
        }

        // Prendi il primo libro (quello "più popolare") per mostrare le sue recensioni
        Book selectedBook = books.get(0);

        // Controlla se ci sono recensioni reali prima di aprire la finestra
        ThreadPoolManager.executeIO(() -> {
            try {
                List<Review> reviews = ServiceLocator.getReviewsService().getReviewsByBook(selectedBook.getBook_id());

                Platform.runLater(() -> {
                    if (reviews != null && !reviews.isEmpty()) {
                        // Ci sono recensioni reali - apri la finestra completa
                        openAllReviewsWindow(selectedBook);
                    } else {
                        // Nessuna recensione - mostra un messaggio informativo con suggerimenti
                        showNoReviewsDialog(selectedBook);
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    logger.log(Level.SEVERE, "Errore nel controllo recensioni", e);
                    showErrorAlert(java.text.MessageFormat.format(resolveString("%viewhome.error.loading.reviews"), e.getMessage()));
                });
            }
        });
    }

    /**
     * Mostra un dialog quando non ci sono recensioni per il libro
     */
    private void showNoReviewsDialog(Book book) {
        Alert noReviewsAlert = new Alert(Alert.AlertType.INFORMATION);
        noReviewsAlert.setTitle(resolveString("%viewhome.no.reviews.title"));
        noReviewsAlert.setHeaderText(java.text.MessageFormat.format(resolveString("%viewhome.no.reviews.header"), book.getTitle()));

        String content = resolveString("%viewhome.no.reviews.message");

        noReviewsAlert.setContentText(content);
        noReviewsAlert.getDialogPane().setMinWidth(500);
        noReviewsAlert.getDialogPane().setMinHeight(350);

        // Aggiungi pulsante per andare alla pagina del libro
        noReviewsAlert.getButtonTypes().clear();
        noReviewsAlert.getButtonTypes().addAll(
            new ButtonType(resolveString("%noreviews.button.go.to.book"), ButtonBar.ButtonData.OK_DONE),
            new ButtonType(resolveString("%noreviews.button.close"), ButtonBar.ButtonData.CANCEL_CLOSE)
        );

        Optional<ButtonType> result = noReviewsAlert.showAndWait();
        if (result.isPresent() && result.get().getButtonData() == ButtonBar.ButtonData.OK_DONE) {
            // Apri la pagina del libro per permettere di aggiungere una recensione
            openBookDetailWindow(book);
        }
    }

     /**
      * Apre una finestra modale con i dettagli del libro (senza recensioni)
      */
     private void openBookDetailWindow(Book book) {
         try {
             // Carica la vista di dettaglio libro semplificata
             FXMLLoader loader = new FXMLLoader(getClass().getResource(EveryView.BOOK_DETAIL_SIMPLE.getPath()),LanguageManager.getBundle());
             loader.setResources(LanguageManager.getBundle());
             Parent root = loader.load();

              // Ottieni il controller e passa il libro
              Object controllerObj = loader.getController();
              if (controllerObj instanceof BookDetailSimpleController) {
                  BookDetailSimpleController controller = (BookDetailSimpleController) controllerObj;
                  controller.setBook(book);
              } else {
                  throw new RuntimeException("Expected BookDetailSimpleController but got: " +
                      (controllerObj != null ? controllerObj.getClass().getName() : "null"));
              }

             // Crea la finestra modale
             Stage bookDetailStage = new Stage();
             bookDetailStage.setTitle(resolveString("%details.books") + book.getTitle());
             Scene bookScene = new Scene(root);
             ThemeManager.applyTheme(bookScene); // Applica il tema corrente (light/dark mode)
             bookDetailStage.setScene(bookScene);
             bookDetailStage.initModality(Modality.APPLICATION_MODAL);
             bookDetailStage.setResizable(false);

             // Mostra la finestra
             bookDetailStage.showAndWait();

          } catch (Exception e) {
              logger.severe(java.text.MessageFormat.format(resolveString("%viewhome.log.errore.apertura.finestra.libro"), e.getMessage()));
              Alert errorAlert = new Alert(Alert.AlertType.ERROR);
              errorAlert.setTitle(resolveString("%viewhome.error.book.details.title"));
              errorAlert.setHeaderText(resolveString("%viewhome.error.book.details.header"));
              errorAlert.setContentText(java.text.MessageFormat.format(resolveString("%viewhome.error.book.details.message"), e.getMessage()));
              errorAlert.showAndWait();
          }
     }

      /**
       * Apre una nuova finestra con i dettagli completi del libro (con recensioni)
       */
      private void openBookDetailNewWindow(Book book) {
          try {
              // Carica la vista di dettaglio libro completa
              FXMLLoader loader = new FXMLLoader(getClass().getResource(EveryView.BOOK_DETAIL_SIMPLE.getPath()), LanguageManager.getBundle());
              loader.setResources(LanguageManager.getBundle());
              Parent root = loader.load();

              // Ottieni il controller e passa il libro tramite setBook
              Object controllerObj = loader.getController();
              if (controllerObj instanceof BookDetailSimpleController) {
                  BookDetailSimpleController controller = (BookDetailSimpleController) controllerObj;
                  controller.setBook(book);
              } else {
                  throw new RuntimeException("Expected BookDetailSimpleController but got: " +
                      (controllerObj != null ? controllerObj.getClass().getName() : "null"));
              }

              // Crea una nuova finestra (non modale)
              Stage bookDetailStage = new Stage();
              bookDetailStage.setTitle(resolveString("%details.books") + " - " + book.getTitle());
              Scene bookScene = new Scene(root);
              ThemeManager.applyTheme(bookScene); // Applica il tema corrente (light/dark mode)
              bookDetailStage.setScene(bookScene);
              bookDetailStage.setResizable(true);
               bookDetailStage.setMinWidth(AppConstants.UI.WINDOW_WIDTH);
               bookDetailStage.setMinHeight(AppConstants.UI.WINDOW_HEIGHT);

              // Mostra la finestra
              bookDetailStage.show();

              logger.info(java.text.MessageFormat.format(resolveString("%viewhome.log.nuova.finestra.dettaglio.libro.aperta"), book.getTitle()));

           } catch (Exception e) {
               logger.severe(java.text.MessageFormat.format(resolveString("%viewhome.log.errore.apertura.nuova.finestra.dettaglio.libro"), e.getMessage()));
               // Fallback: mostra messaggio di errore
               Alert errorAlert = new Alert(Alert.AlertType.ERROR);
               errorAlert.setTitle(resolveString("%viewhome.error.book.details.title"));
               errorAlert.setHeaderText(resolveString("%viewhome.error.book.details.header"));
               errorAlert.setContentText(java.text.MessageFormat.format(resolveString("%viewhome.error.book.details.message"), e.getMessage()));
               errorAlert.showAndWait();
           }
      }

    /**
     * Apre una finestra con tutte le recensioni per il libro selezionato
     */
    private void openAllReviewsWindow(Book book) {
        try {
            // Carica la vista delle recensioni
            FXMLLoader loader = new FXMLLoader(getClass().getResource(EveryView.ALL_REVIEWS.getPath()), LanguageManager.getBundle());
            Parent root = loader.load();

            // Ottieni il controller e imposta il libro
            AllReviewsController controller = loader.getController();
            controller.setBook(book);

            // Crea e configura la finestra
            Stage stage = new Stage();
            stage.setTitle(resolveString("%viewhome.reviews.title.all") + book.getTitle());
            Scene reviewsScene = new Scene(root);
            ThemeManager.applyTheme(reviewsScene); // Applica il tema corrente (light/dark mode)
            stage.setScene(reviewsScene);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.showAndWait();

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore nell'apertura della finestra recensioni", e);
            showErrorAlert(java.text.MessageFormat.format(resolveString("%viewhome.error.opening.reviews"), e.getMessage()));
        }
    }

    /**
     * Apre una finestra elegante con tutti i libri consigliati per il libro selezionato
     */
    private void openAllSuggestedBooksWindow(Book book) {
        try {
            // Carica i libri consigliati in background
            ThreadPoolManager.executeIO(() -> {
                try {
                    List<Book> suggestedBooks = ServiceLocator.getBookService().getSuggestedBooks(book.getBook_id());

                    Platform.runLater(() -> {
                        // Crea una finestra personalizzata invece di un semplice Alert
                        Stage suggestionsStage = new Stage();
                        suggestionsStage.setTitle("📚 Libri Consigliati - " + book.getTitle());
                        suggestionsStage.initModality(Modality.APPLICATION_MODAL);
                        suggestionsStage.setMinWidth(500);
                        suggestionsStage.setMinHeight(400);

                        // Layout principale
                        VBox mainLayout = new VBox(15);
                        mainLayout.setPadding(new Insets(20));
                        mainLayout.setStyle("-fx-background-color: #f8f9fa;");

                        // Header con info del libro
                        Label headerLabel = new Label(resolveString("%viewhome.suggestions.header"));
                        headerLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

                        Label bookInfoLabel = new Label(java.text.MessageFormat.format(resolveString("%viewhome.suggestions.book.info"), book.getTitle(), book.getAuthors()));
                        bookInfoLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #34495e; -fx-padding: 0 0 10 0;");

                        // Contenitore per la lista dei libri
                        VBox booksContainer = new VBox(8);
                        booksContainer.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-border-color: #dee2e6; -fx-border-radius: 5;");

                        if (suggestedBooks != null && !suggestedBooks.isEmpty()) {
                            // Rimuovi duplicati
                            List<Book> uniqueBooks = suggestedBooks.stream()
                                .collect(java.util.stream.Collectors.toMap(
                                    Book::getBook_id,
                                    b -> b,
                                    (existing, replacement) -> existing
                                ))
                                .values()
                                .stream()
                                .collect(java.util.stream.Collectors.toList());

                            Label countLabel = new Label(java.text.MessageFormat.format(resolveString("%viewhome.suggestions.found"), uniqueBooks.size()));
                            countLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #27ae60;");
                            booksContainer.getChildren().add(countLabel);

                            for (int i = 0; i < uniqueBooks.size(); i++) {
                                Book suggestedBook = uniqueBooks.get(i);

                                // Crea un HBox per ogni libro con numero e info
                                HBox bookRow = new HBox(10);
                                bookRow.setPadding(new Insets(8));
                                bookRow.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #e9ecef; -fx-border-radius: 3;");

                                Label numberLabel = new Label((i + 1) + ".");
                                numberLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #6c757d; -fx-min-width: 25;");

                                VBox bookInfo = new VBox(2);
                                Label titleLabel = new Label(suggestedBook.getTitle());
                                titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50;");

                                Label authorLabel = new Label(resolveString("%viewhome.suggestions.book.author.prefix") + " " + suggestedBook.getAuthors() +
                                    (suggestedBook.getPublish_date_year() > 0 ? " (" + suggestedBook.getPublish_date_year() + ")" : ""));
                                authorLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #6c757d;");

                                bookInfo.getChildren().addAll(titleLabel, authorLabel);
                                bookRow.getChildren().addAll(numberLabel, bookInfo);

                                booksContainer.getChildren().add(bookRow);
                            }
                        } else {
                            Label noSuggestionsLabel = new Label(resolveString("%viewhome.suggestions.no.books"));
                            noSuggestionsLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #6c757d; -fx-text-alignment: center;");
                            noSuggestionsLabel.setWrapText(true);
                            booksContainer.getChildren().add(noSuggestionsLabel);
                        }

                        // Pulsante chiudi
                        Button closeButton = new Button("✅ Chiudi");
                        closeButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16;");
                        closeButton.setOnAction(e -> suggestionsStage.close());
                        HBox buttonContainer = new HBox(closeButton);
                        buttonContainer.setAlignment(Pos.CENTER);

                        mainLayout.getChildren().addAll(headerLabel, bookInfoLabel, booksContainer, buttonContainer);

                        Scene scene = new Scene(mainLayout);
                        suggestionsStage.setScene(scene);
                        suggestionsStage.showAndWait();
                    });

                } catch (Exception e) {
                    Platform.runLater(() -> {
                        Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                        errorAlert.setTitle(resolveString("%homepage.suggested.error.title"));
                        errorAlert.setHeaderText(resolveString("%homepage.suggested.error.header"));
                        errorAlert.setContentText(resolveString("%homepage.suggested.error.message") + " " + e.getMessage());
                        errorAlert.showAndWait();
                    });
                }
            });

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore nell'apertura della finestra libri consigliati", e);
            showErrorAlert(java.text.MessageFormat.format(resolveString("%viewhome.error.opening.suggestions"), e.getMessage()));
        }
    }

    /**
     * Mostra un alert di errore
     */
    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Errore");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Gestisce il click del pulsante "Vedi suggerimenti" per mostrare tutti i libri consigliati
     */
    @FXML
    private void onSuggestionsButtonClick() {
        if (books.isEmpty()) {
            showErrorAlert(resolveString("%viewhome.error.no.books.suggestions"));
            return;
        }

        // Prendi il primo libro (quello "più popolare") per mostrare i suoi consigliati
        Book selectedBook = books.get(0);
        logger.info("Mostrando libri consigliati per: " + selectedBook.getTitle());

        // Prima prova a caricare suggerimenti reali, se non ci sono usa fallback intelligente
        ThreadPoolManager.executeIO(() -> {
            try {
                List<Book> suggestedBooks = ServiceLocator.getBookService().getSuggestedBooks(selectedBook.getBook_id());

                Platform.runLater(() -> {
                    if (suggestedBooks != null && !suggestedBooks.isEmpty()) {
                        // Ci sono suggerimenti reali - mostra quelli
                        openAllSuggestedBooksWindow(selectedBook);
                    } else {
                        // Nessun suggerimento - mostra fallback con libri correlati per genere/autore
                        openFallbackSuggestionsWindow(selectedBook);
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    logger.log(Level.SEVERE, "Errore nel caricamento suggerimenti", e);
                    // In caso di errore, mostra comunque il fallback
                    openFallbackSuggestionsWindow(selectedBook);
                });
            }
        });
    }

    /**
     * Mostra un messaggio informativo quando non ci sono consigli disponibili
     */
    private void showNoSuggestionsMessage() {
        String message = resolveString("%viewhome.suggestions.none.available.message");
        String title = resolveString("%viewhome.suggestions.none.available.title");

        // Crea un alert informativo
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

}
