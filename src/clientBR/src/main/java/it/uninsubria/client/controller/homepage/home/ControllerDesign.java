package it.uninsubria.client.controller.homepage.home;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.kordamp.ikonli.javafx.FontIcon;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import it.uninsubria.client.controller.ControllerBase;
import it.uninsubria.client.controller.login.ControllerLogin;
import it.uninsubria.client.utils.classesUI.BookServiceManager;
import it.uninsubria.client.utils.classesUI.EveryView;
import it.uninsubria.client.utils.classesUI.InputValidatorClient;
import it.uninsubria.client.utils.classesUI.LanguageManager;
import it.uninsubria.client.utils.classesUI.Navigator;
import it.uninsubria.client.utils.classesUI.NotificationManager;
import it.uninsubria.client.utils.classesUI.PerformanceMonitor;
import it.uninsubria.client.utils.classesUI.ResourceCache;
import it.uninsubria.client.cache.ClientCacheManager;
import it.uninsubria.client.utils.classesUI.ServiceLocator;
import it.uninsubria.client.utils.classesUI.SessionManager;
import it.uninsubria.client.utils.classesUI.ThreadPoolManager;
import it.uninsubria.client.utils.classesUI.UIAnimator;
import it.uninsubria.client.utils.classesUI.UIHelpers;
import it.uninsubria.shared.model.Book;
import it.uninsubria.shared.utils.LoggerUtil;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

public class ControllerDesign extends ControllerBase {

    /**
     * Enum per i filtri di ricerca
     */
    private enum SearchFilter {
        TUTTI, TITOLO, AUTORE, AUTORE_ANNO
    }

    @FXML
    private Button homeButton;
    @FXML
    private Button categoriesButton;
    @FXML
    private Button settingsButton;
    @FXML
    private Button aboutButton;
    @FXML
    private Button exitButton;
    @FXML
    private Button libraryButton;
    @FXML
    private Button loginInfoButton;
    @FXML
    private Button loginRegisterButton;

    @FXML
    private FontIcon iconHome;
    @FXML
    private FontIcon categoriesIcon;
    @FXML
    private FontIcon librariesIcon;
    @FXML
    private FontIcon settingsIcon;
    @FXML
    private FontIcon aboutIcon;
    @FXML
    private FontIcon exitIcon;

    @FXML
    private Label titleHomeButton;
    @FXML
    private Label titleSettingsButton;
    @FXML
    private Label titleAboutButton;
    @FXML
    private Label titleExitButton;
    @FXML
    private Label titleCategoriesButton;
    @FXML
    private Label titleLibrariesButton;

    @FXML
    private HBox homeBox;
    @FXML
    private HBox settingsBox;
    @FXML
    private HBox aboutBox;
    @FXML
    private HBox exitBox;
    @FXML
    private HBox categoriesBox;
    @FXML
    private HBox librariesBox;
    @FXML
    private HBox HBoxContent;

    @FXML
    private MenuButton iconLoginProfile;
    @FXML
    private MenuButton searchFilterMenu;

    @FXML
    private MenuItem searchByAll;
    @FXML
    private MenuItem searchByTitle;
    @FXML
    private MenuItem searchByAuthor;
    @FXML
    private MenuItem searchByAuthorAndYear;

    @FXML
    private StackPane contenitorIconLoginProfile;

    @FXML
    private ImageView profileImageView;

    @FXML
    private TextField searchingBooks;

    @FXML
    private ListView<Book> resultsListView;

    // Campi per le raccomandazioni (implementazione incrementale)
    @FXML
    private VBox recommendationsContainer;
    @FXML
    private Label recommendationsTitle;
    @FXML
    private HBox recommendationsContent;
    @FXML
    private ProgressIndicator recommendationsLoadingIndicator;

    private Button activeButton = null;
    private FontIcon activeIcon = null;
    private Label activeLabel = null;

    // Campi mancanti ripristinati
    private SearchFilter currentSearchFilter = SearchFilter.TUTTI;
    private MenuItem selectedMenuItem = null;

    private Timer debounceTimer = null;

    private static List<Book> allBooks; // cache locale di tutti i libri
    private static Map<String, List<Book>> invertedIndex = new ConcurrentHashMap<>();
    private static Map<String, List<Book>> authorIndex = new ConcurrentHashMap<>();
    private static Map<Integer, List<Book>> yearIndex = new ConcurrentHashMap<>();
    private static Map<String, List<Book>> categoryIndex = new ConcurrentHashMap<>();

    private static boolean libriCaricati = false;
    private static boolean categoriesBuilt = false;
    private static long lastCategoryBuildTime = 0;

    // Cache su disco per le categorie (multiplatform)
    private static final String CATEGORIES_CACHE_DIR = System.getProperty("user.home") + File.separator
            + "BookRecommender_Cache";
    private static final String CATEGORIES_CACHE_FILE = CATEGORIES_CACHE_DIR + File.separator + "categories_cache.json";
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .enable(SerializationFeature.INDENT_OUTPUT);

    private static final Logger logger = LoggerUtil.getLogger(ControllerDesign.class);

    // Campi per paginazione lazy loading
    private static final int SEARCH_PAGE_SIZE = 20; // Risultati per pagina nella ricerca
    private int currentSearchPage = 0;
    private List<Book> allCurrentSearchResults = new ArrayList<>(); // Tutti i risultati della ricerca corrente
    private static final BookServiceManager bookManager = BookServiceManager.getInstance();

    public static Map<String, List<Book>> getCategoryIndex() {
        return categoryIndex;
    }

    /**
     * Restituisce le categorie in modo consistente, assicurandosi che siano state
     * calcolate
     * Utilizza caching temporizzato per evitare ricalcoli frequenti
     * Ora sfrutta direttamente la cache del BookServiceManager per prestazioni
     * ottimali
     */
    public static Map<String, List<Book>> getCategories() {
        // ⚡ OTTIMIZZAZIONE: Le categorie sono ora pre-calcolate durante l'avvio!
        // Questo metodo ora restituisce quasi istantaneamente i dati già pronti

        if (categoriesBuilt) {
            return categoryIndex; // 🚀 IMMEDIATO: già calcolato durante avvio
        }

        // Fallback: se per qualche motivo non sono state pre-calcolate
        logger.warning(resolveString("%design.log.warning.categories.not.precalculated"));
        return getCategoriesFallback();
    }

    /**
     * Fallback per calcolo categorie on-demand (ottimizzato per velocità)
     */
    public static Map<String, List<Book>> getCategoriesFallback() {
        long currentTime = System.currentTimeMillis();

        // ⚠️ ASSICURA CHE TUTTI I LIBRI SIANO CARICATI PRIMA DI COSTRUIRE CATEGORIE
        ensureBooksLoaded();

        // Ora usa tutti i libri disponibili
        List<Book> booksToUse = allBooks != null ? allBooks : new CopyOnWriteArrayList<>();

        if (booksToUse.isEmpty()) {
            logger.warning(resolveString("%design.log.warning.no.books.for.categories"));
            return new ConcurrentHashMap<>();
        }

        logger.info(java.text.MessageFormat.format(resolveString("%design.log.info.building.fallback.categories"), booksToUse.size()));

        // Costruisci l'indice delle categorie (ora ottimizzato)
        buildCategoryIndexOptimized(booksToUse);

        if (categoryIndex != null && !categoryIndex.isEmpty()) {
            categoriesBuilt = true;
            lastCategoryBuildTime = currentTime;
            logger.info(java.text.MessageFormat.format(resolveString("%design.log.info.categories.calculated.fallback"), categoryIndex.size()));
        } else {
            // Fallback: assicurati che i libri siano caricati
            ensureBooksLoaded();

            if (allBooks != null && !allBooks.isEmpty()) {
                buildCategoryIndexOptimized(allBooks);
                categoriesBuilt = true;
                lastCategoryBuildTime = currentTime;
                logger.info(resolveString("%design.log.info.categories.calculated.fallback.database"));
            } else {
                logger.warning(resolveString("%design.log.warning.cannot.build.categories"));
            }
        }
        return categoryIndex;
    }

    /**
     * Assicura che i libri siano caricati
     */
    public static void ensureBooksLoaded() {
        if (!libriCaricati) {
            try {
                // Prima prova a usare i libri già caricati dal BookServiceManager
                if (bookManager.isLoaded() && !bookManager.getAllBooks().isEmpty()) {
                    allBooks = new CopyOnWriteArrayList<>(bookManager.getAllBooks());
                    libriCaricati = true;
                    logger.info(java.text.MessageFormat.format(resolveString("%design.log.info.books.loaded.from.cache"), allBooks.size()));
                    // Costruisci indici di ricerca per performance
                    buildInvertedIndex(allBooks);
                    // NON pre-costruire le categorie per velocità di avvio - saranno calcolate
                    // on-demand
                } else {
                    // Carica TUTTI i libri dal database per garantire categorie complete
                    logger.info(resolveString("%design.log.info.loading.all.books.for.categories"));
                    List<Book> allBooksFromDB = bookManager.loadAllBooks().get(30,
                            java.util.concurrent.TimeUnit.SECONDS);
                    allBooks = new CopyOnWriteArrayList<>(allBooksFromDB);
                    libriCaricati = true;
                    logger.info(java.text.MessageFormat.format(resolveString("%design.log.info.all.books.loaded.from.database"), allBooks.size()));
                    // Costruisci indici di ricerca per performance
                    buildInvertedIndex(allBooks);
                    // NON pre-costruire le categorie per velocità di avvio - saranno calcolate
                    // on-demand
                }
            } catch (java.util.concurrent.TimeoutException e) {
                logger.severe(java.text.MessageFormat.format(resolveString("%design.error.timeout.loading.books"), e.getMessage()));
                allBooks = new CopyOnWriteArrayList<>();
                libriCaricati = true;
            } catch (InterruptedException e) {
                logger.severe(java.text.MessageFormat.format(resolveString("%design.error.interrupted.loading.books"), e.getMessage()));
                allBooks = new CopyOnWriteArrayList<>();
                libriCaricati = true;
                Thread.currentThread().interrupt(); // Restore interrupted status
            } catch (java.util.concurrent.ExecutionException e) {
                // Cattura ExecutionException che wrappa RemoteException
                if (e.getCause() instanceof java.rmi.RemoteException) {
                    logger.severe(java.text.MessageFormat.format(resolveString("%design.error.connection.loading.books"), e.getCause().getMessage()));
                } else {
                    logger.severe(java.text.MessageFormat.format(resolveString("%design.error.execution.loading.books"), e.getMessage()));
                }
                allBooks = new CopyOnWriteArrayList<>();
                libriCaricati = true;
            } catch (RuntimeException e) {
                logger.severe(java.text.MessageFormat.format(resolveString("%design.error.runtime.loading.books"), e.getMessage()));
                allBooks = new CopyOnWriteArrayList<>();
                libriCaricati = true;
            }
        }
    }

    /**
     * Forza il refresh della cache delle categorie
     */
    public static void refreshCategoriesCache() {
        categoriesBuilt = false;
        lastCategoryBuildTime = 0;
        categoryIndex.clear();
        logger.info(resolveString("%design.log.info.categories.cache.invalidated"));
    }

    /**
     * Costruisce l'indice delle categorie in modo ultra-ottimizzato usando streams
     * paralleli avanzati
     * per prestazioni massime con grandi quantità di dati
     */
    private static void buildCategoryIndexOptimized(List<Book> books) {
        long startTime = System.currentTimeMillis();
        categoryIndex.clear();

        // 🚀 OTTIMIZZAZIONE ULTRA-VELOCE: Usa forEach invece di stream per ridurre
        // overhead
        // ConcurrentHashMap per thread-safety, ma senza parallelStream per velocità
        Map<String, List<Book>> tempIndex = new ConcurrentHashMap<>();

        for (Book book : books) {
            if (book == null)
                continue;

            String categoryName = book.getCategory();
            if (categoryName == null || categoryName.trim().isEmpty()) {
                categoryName = resolveString("%controllerdesign.category.unspecified");
            } else {
                categoryName = categoryName.trim();
            }

            // ⚡ Usa computeIfAbsent per efficienza massima
            tempIndex.computeIfAbsent(categoryName, k -> new CopyOnWriteArrayList<>()).add(book);
        }

        categoryIndex.putAll(tempIndex);

        long duration = System.currentTimeMillis() - startTime;

        // Conta libri totali in tutte le categorie
        int totalBooksInCategories = categoryIndex.values().stream()
                .mapToInt(List::size)
                .sum();

        logger.info(java.text.MessageFormat.format(resolveString("%design.log.info.categories.index.built"), duration));
        logger.info(java.text.MessageFormat.format(resolveString("%design.log.info.books.analyzed"), books.size()));
        logger.info(java.text.MessageFormat.format(resolveString("%design.log.info.categories.created"), categoryIndex.size()));
        logger.info(java.text.MessageFormat.format(resolveString("%design.log.info.books.categorized"), totalBooksInCategories));

        if (totalBooksInCategories != books.size()) {
            logger.warning(java.text.MessageFormat.format(resolveString("%design.log.warning.discrepancy.books.not.categorized"),
                    books.size() - totalBooksInCategories));
        }

        // Log delle categorie più popolose
        if (logger.isLoggable(java.util.logging.Level.FINE)) {
            categoryIndex.entrySet().stream()
                    .sorted((e1, e2) -> Integer.compare(e2.getValue().size(), e1.getValue().size()))
                    .limit(10)
                    .forEach(entry -> logger
                            .fine(java.text.MessageFormat.format(resolveString("%design.log.fine.category.summary"), entry.getKey(), entry.getValue().size())));
        }

        // Log delle categorie più popolose per monitoraggio prestazioni
        if (logger.isLoggable(java.util.logging.Level.FINE)) {
            categoryIndex.entrySet().stream()
                    .sorted((e1, e2) -> Integer.compare(e2.getValue().size(), e1.getValue().size()))
                    .limit(5)
                    .forEach(entry -> logger
                            .fine(java.text.MessageFormat.format(resolveString("%design.log.fine.category.detailed"), entry.getKey(), entry.getValue().size())));
        }
    }

    /**
     * Metodi helper per gestire la visibilità degli elementi di ricerca
     */
    private void showSearchResults() {
        resultsListView.setVisible(true);
        resultsListView.setDisable(false);
    }

    private void hideSearchResults() {
        resultsListView.setVisible(false);
        resultsListView.getItems().clear();
        resetSearchState();
    }

    private void resetSearchState() {
        allCurrentSearchResults = List.of();
        currentSearchPage = 0;
        searchingBooks.setDisable(false);
    }

    /**
     * Mostra i risultati della ricerca
     */
    private void mostraRisultati(List<Book> books) {
        Platform.runLater(() -> {
            resultsListView.getItems().clear();
            if (books.isEmpty()) {
                resultsListView.getItems().add(new Book(-1, resolveString("%search.no.results.found"), "", 0));
                resultsListView.setDisable(true);
            } else {
                resultsListView.getItems().addAll(books);
            }
            showSearchResults();
        });
    }

    /**
     * Imposta il pulsante attivo con animazioni
     */
    private void setActiveButton(Button button, FontIcon icon, Label label) {
        // Reset previous active button
        if (activeButton != null && activeIcon != null && activeLabel != null) {
            resetActiveStyle(activeButton, activeIcon, activeLabel);
        }

        // Applica nuovo stile con animazioni
        if (button != null) {
            button.getStyleClass().setAll("button-filledHome");
            if (icon != null) {
                icon.getStyleClass().setAll("fontawesomeWhite");
            }
            if (label != null) {
                label.getStyleClass().setAll("labelBoldHome");
                label.setVisible(true);

                // Assicurati che il testo sia stato tradotto correttamente prima dell'animazione
                ensureLabelTextIsTranslated(label);

                // Aggiungi un piccolo delay per assicurarsi che gli elementi siano renderizzati
                Platform.runLater(() -> {
                    UIAnimator.slideLeftButton(button);
                    // Piccolo delay aggiuntivo per permettere alla traduzione di applicarsi completamente
                    PauseTransition pause = new PauseTransition(javafx.util.Duration.millis(50));
                    pause.setOnFinished(event -> UIAnimator.typewriterLabel(label));
                    pause.play();
                });
            }
        }

        activeButton = button;
        activeIcon = icon;
        activeLabel = label;
    }

    /**
     * Assicurati che il testo della label sia stato tradotto correttamente
     */
    private void ensureLabelTextIsTranslated(Label label) {
        if (label == null) return;

        try {
            // Controlla se il testo attuale sembra essere una chiave di traduzione (inizia con %)
            String currentText = label.getText();
            if (currentText != null && currentText.startsWith("%")) {
                // È una chiave di traduzione, applicala manualmente
                String key = currentText.substring(1); // Rimuovi il %
                String translatedText = LanguageManager.getBundle().getString(key);
                if (translatedText != null && !translatedText.equals(key)) {
                    label.setText(translatedText);
                    logger.fine(java.text.MessageFormat.format(resolveString("%design.log.fine.translation.applied"), key, translatedText));
                }
            }
        } catch (RuntimeException e) {
            // Se qualcosa va storto, lascia il testo com'è
            logger.fine(java.text.MessageFormat.format(resolveString("%design.log.fine.translation.failed"), e.getMessage()));
        }
    }

    /**
     * Resetta lo stile del pulsante precedentemente attivo
     */
    private void resetActiveStyle(Button button, FontIcon icon, Label label) {
        if (button != null) {
            // Ripristina le classi originali del pulsante
            button.getStyleClass().setAll("navBarHome");
        }
        if (icon != null) {
            // Ripristina le classi originali dell'icona
            icon.getStyleClass().setAll("fontawesomeDefault");
        }
        if (label != null) {
            // Ripristina le classi originali della label
            label.getStyleClass().setAll("labelTitleHome");
            label.setVisible(false);
        }
    }

    /**
     * Imposta il contenuto con transizione
     */
    private void setContentWithTransition(String fxmlPath) {
        try {
            Parent newContent = Navigator.loadView(fxmlPath, null);
            if (newContent != null) {
                newContent.setOpacity(0.0);
                HBoxContent.getChildren().setAll(newContent);
                // Fade in nuovo contenuto
                FadeTransition fadeIn = new FadeTransition(Duration.millis(400), newContent);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                fadeIn.play();
            }
        } catch (Exception e) {
            logger.severe(java.text.MessageFormat.format(resolveString("%design.log.severe.error.loading.view"), fxmlPath, e.getMessage()));
        }
    }

    /**
     * Apre il dettaglio di un libro
     */
    private void openBookDetail(Book book) {
        try {
            // Naviga al dettaglio libro (hideSearchResults() è già chiamato dal Navigator)
            Navigator.goTo(EveryView.BOOK_DETAIL_SIMPLE.getPath(), Map.of("book", book));
        } catch (RuntimeException e) {
            logger.severe(java.text.MessageFormat.format(resolveString("%design.log.severe.error.opening.book.detail"), e.getMessage()));
            NotificationManager.showError(resolveString("%controllerdesign.error.book.open"));
        }
    }

    /**
     * Metodi helper per gestire la UI delle raccomandazioni
     */
    private void showRecommendationsForLoggedUser() {
        Platform.runLater(() -> {
            if (loginInfoButton != null)
                loginInfoButton.setVisible(false);
            if (recommendationsContainer != null)
                recommendationsContainer.setVisible(true);
        });
    }

    private void showRecommendationsForGuest() {
        Platform.runLater(() -> {
            if (recommendationsContainer != null)
                recommendationsContainer.setVisible(false);
            if (loginInfoButton != null)
                loginInfoButton.setVisible(true);
        });
    }

    private void showRecommendationsLoading() {
        Platform.runLater(() -> {
            if (recommendationsLoadingIndicator != null)
                recommendationsLoadingIndicator.setVisible(true);
            if (recommendationsContent != null)
                recommendationsContent.setVisible(false);
        });
    }

    private void hideRecommendationsLoading() {
        Platform.runLater(() -> {
            if (recommendationsLoadingIndicator != null)
                recommendationsLoadingIndicator.setVisible(false);
            if (recommendationsContent != null)
                recommendationsContent.setVisible(true);
        });
    }

    /**
     * Carica e mostra le raccomandazioni di base per l'utente
     */
    private void loadBasicRecommendations() {
        String userId = SessionManager.getInstance().getUsername();
        if (userId == null || userId.isEmpty()) {
            showRecommendationsForGuest();
            return;
        }

        showRecommendationsForLoggedUser();
        showRecommendationsLoading();

    }

    /**
     * Mostra le raccomandazioni nell'interfaccia
     */
    private void displayRecommendations(List<Book> books) {
        if (recommendationsContent == null) {
            logger.warning(resolveString("%design.log.warning.recommendations.content.null"));
            return;
        }

        Platform.runLater(() -> {
            recommendationsContent.getChildren().clear();

            if (books == null || books.isEmpty()) {
                // Nessuna raccomandazione disponibile
                Label noRecommendationsLabel = new Label(resolveString("%design.no.recommendations.available"));
                noRecommendationsLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d; -fx-font-style: italic;");
                recommendationsContent.getChildren().add(noRecommendationsLabel);
                return;
            }

            // Limita a massimo 6 libri per non sovraccaricare l'interfaccia
            int maxBooks = Math.min(books.size(), 6);
            for (int i = 0; i < maxBooks; i++) {
                Book book = books.get(i);
                VBox bookCard = createRecommendationBookCard(book);
                recommendationsContent.getChildren().add(bookCard);
            }

            logger.info(java.text.MessageFormat.format(resolveString("%design.log.info.recommendations.shown"), maxBooks));
        });
    }

    /**
     * Crea una card per un libro raccomandato
     */
    private VBox createRecommendationBookCard(Book book) {
        VBox card = new VBox(5);
        card.setPrefWidth(140);
        card.setPrefHeight(180);
        card.setAlignment(Pos.TOP_CENTER);
        card.setStyle(
                "-fx-background-color: #ffffff; -fx-background-radius: 8; -fx-border-color: #e0e0e0; -fx-border-radius: 8; -fx-border-width: 1; -fx-padding: 8; -fx-cursor: hand;");

        // Immagine del libro (placeholder)
        ImageView bookImage = new ImageView(ResourceCache.getImage(EveryView.TEST_IMAGE.getPath()));
        bookImage.setFitWidth(100);
        bookImage.setFitHeight(120);
        bookImage.setPreserveRatio(true);
        bookImage.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 4, 0, 0, 2);");

        // Titolo del libro (troncato)
        Label titleLabel = new Label();
        String title = book.getTitle();
        if (title.length() > 15) {
            title = title.substring(0, 12) + "...";
        }
        titleLabel.setText(title);
        titleLabel.setStyle(
                "-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-text-alignment: center;");
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(120);
        titleLabel.setAlignment(Pos.CENTER);

        // Autore (troncato)
        Label authorLabel = new Label();
        String authors = book.getAuthors();
        if (authors != null && authors.length() > 12) {
            authors = authors.substring(0, 9) + "...";
        }
        authorLabel.setText(authors != null ? authors : resolveString("%design.book.unknown.author"));
        authorLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #7f8c8d; -fx-text-alignment: center;");
        authorLabel.setWrapText(true);
        authorLabel.setMaxWidth(120);
        authorLabel.setAlignment(Pos.CENTER);

        // Hover effects
        card.setOnMouseEntered(event -> {
            card.setStyle(
                    "-fx-background-color: #f8f9fa; -fx-background-radius: 8; -fx-border-color: #3498db; -fx-border-radius: 8; -fx-border-width: 1; -fx-padding: 8; -fx-effect: dropshadow(three-pass-box, rgba(52,152,219,0.2), 6, 0, 0, 3); -fx-cursor: hand;");
        });
        card.setOnMouseExited(event -> {
            card.setStyle(
                    "-fx-background-color: #ffffff; -fx-background-radius: 8; -fx-border-color: #e0e0e0; -fx-border-radius: 8; -fx-border-width: 1; -fx-padding: 8; -fx-cursor: hand;");
        });

        // Click handler per aprire il dettaglio libro
        card.setOnMouseClicked(event -> openBookDetail(book));

        card.getChildren().addAll(bookImage, titleLabel, authorLabel);
        return card;
    }

    private static void buildInvertedIndex(List<Book> books) {
        invertedIndex.clear();
        authorIndex.clear();
        yearIndex.clear();

        books.parallelStream().forEach(book -> {
            // Indice invertito per ricerca full-text
            String text = (book.getTitle() + " " + book.getAuthors())
                    .toLowerCase()
                    .replaceAll("[^a-z0-9\\s]", " ");
            for (String token : text.split("\\s+")) {
                if (!token.isBlank()) {
                    invertedIndex.computeIfAbsent(token, k -> new CopyOnWriteArrayList<>()).add(book);
                }
            }

            // Indice ottimizzato per autore
            if (book.getAuthors() != null && !book.getAuthors().isBlank()) {
                String normalizedAuthor = book.getAuthors().toLowerCase().trim()
                        .replaceAll("^by\\s+", "") // Rimuovi "By " dall'inizio
                        .replaceAll("[^a-z0-9\\s]", " "); // Normalizza caratteri speciali
                authorIndex.computeIfAbsent(normalizedAuthor, k -> new CopyOnWriteArrayList<>()).add(book);
            }

            // Indice per anno
            if (book.getPublish_date_year() > 0) {
                yearIndex.computeIfAbsent(book.getPublish_date_year(), k -> new CopyOnWriteArrayList<>()).add(book);
            }
        });

        logger.info(resolveString("%design.log.info.optimizations.built"));
        logger.info(java.text.MessageFormat.format(resolveString("%design.log.info.fulltext.index"), invertedIndex.size()));
        logger.info(java.text.MessageFormat.format(resolveString("%design.log.info.authors.index"), authorIndex.size()));
        logger.info(java.text.MessageFormat.format(resolveString("%design.log.info.years.index"), yearIndex.size()));
    }

    private List<Book> localSearch(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        final String normalizedQuery = query.toLowerCase().trim();

        // Check TTL-based cache
        List<Book> cached = ClientCacheManager.get("query:" + normalizedQuery);
        if (cached != null) {
            return cached;
        }

        // Prima cerca negli autori (più specifico)
        List<Book> authorResults = authorIndex.entrySet().stream()
                .filter(entry -> entry.getKey().contains(normalizedQuery))
                .flatMap(entry -> entry.getValue().stream())
                .distinct()
                .limit(100)
                .collect(Collectors.toList());

        // Poi cerca nei titoli e altri campi
        List<Book> titleResults = invertedIndex.entrySet().stream()
                .filter(e -> e.getKey().contains(normalizedQuery))
                .flatMap(e -> e.getValue().stream())
                .distinct()
                .limit(150)
                .collect(Collectors.toList());

        // Combina risultati, dando priorità agli autori
        Set<Book> combined = new HashSet<>(authorResults);
        combined.addAll(titleResults);

        List<Book> result = combined.stream().limit(200).collect(Collectors.toList());

        // Cache with TTL
        ClientCacheManager.put("query:" + normalizedQuery, result);
        return result;
    }

    /**
     * Metodo unificato per ricerca locale con diversi filtri
     */
    private List<Book> localSearchFiltered(String query, Map<String, List<Book>> index, String cachePrefix,
            boolean requireExactYear, int year) {
        if (query == null || query.isBlank() || (requireExactYear && year <= 0)) {
            return List.of();
        }

        final String normalizedQuery = query.toLowerCase().trim();
        final String cacheKey = cachePrefix + ":" + normalizedQuery + (year != -1 ? ":" + year : "");

        // Controlla cache TTL-based
        List<Book> cached = ClientCacheManager.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        // Cerca usando l'indice specificato
        List<Book> results = index.entrySet().stream()
                .filter(entry -> entry.getKey().contains(normalizedQuery))
                .flatMap(entry -> entry.getValue().stream())
                .distinct()
                .filter(book -> year == -1 || (requireExactYear ? book.getPublish_date_year() == year
                        : book.getPublish_date_year() == year))
                .limit(100)
                .collect(Collectors.toList());

        // Salva in cache con TTL
        ClientCacheManager.put(cacheKey, results);

        return results;
    }

    /**
     * Ricerca locale per titolo usando gli indici precaricati
     */
    private List<Book> localSearchByTitle(String title, int year) {
        return localSearchFiltered(title, invertedIndex, "title", false, year);
    }

    /**
     * Ricerca locale per autore usando gli indici precaricati
     */
    private List<Book> localSearchByAuthor(String author, int year) {
        return localSearchFiltered(author, authorIndex, "author", false, year);
    }

    /**
     * Ricerca locale per autore e anno specifico (coppia obbligatoria)
     */
    private List<Book> localSearchByAuthorAndYear(String author, int year) {
        return localSearchFiltered(author, authorIndex, "author_year", true, year);
    }

    /**
     * Ottiene i risultati per la pagina corrente dalla ricerca locale
     */
    private List<Book> getCurrentPageResults() {
        int startIndex = currentSearchPage * SEARCH_PAGE_SIZE;
        int endIndex = Math.min(startIndex + SEARCH_PAGE_SIZE, allCurrentSearchResults.size());
        return allCurrentSearchResults.subList(startIndex, endIndex);
    }

    private int extractYear(String input) {
        Pattern pattern = Pattern.compile("\\b(\\d{4})\\b");
        Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            int y = Integer.parseInt(matcher.group(1));
            if (y >= 1900 && y <= 2100)
                return y;
        }
        return -1;
    }

    private String removeYear(String input) {
        return input.replaceAll("\\b\\d{4}\\b", "").trim();
    }

    private void cercaLibro(String query) {
        // Validazione input ricerca
        var validation = InputValidatorClient.validateSearchQuery(query);
        if (!validation.isValid()) {
            Platform.runLater(() -> {
                NotificationManager.showError(validation.getErrorMessage());
            });
            return;
        }

        if (query.isEmpty()) {
            Platform.runLater(() -> {
                resultsListView.setVisible(false); // Nasconde i risultati
                resultsListView.getItems().clear(); // Pulisce eventuali dat
                return;
            });

        }

        // Assicura che i libri siano caricati prima di fare la ricerca
        ensureBooksLoaded();

        ThreadPoolManager.executeIO(() -> {
            PerformanceMonitor.monitorSearchExecution(() -> {
                int year = extractYear(query);
                String term = removeYear(query);

                // OTTIMIZZAZIONE: Ottieni TUTTI i risultati locali possibili (non paginati)
                List<Book> allResults = switch (currentSearchFilter) {
                    case TUTTI -> localSearch(query);
                    case TITOLO -> localSearchByTitle(term, year);
                    case AUTORE -> localSearchByAuthor(term, -1); // -1 significa nessun anno specifico
                    case AUTORE_ANNO -> localSearchByAuthorAndYear(term, year);
                };

                // Salva tutti i risultati per la paginazione locale
                allCurrentSearchResults = allResults != null ? allResults : List.of();
                currentSearchPage = 0;

                // Mostra solo la prima pagina (primi 20 risultati)
                List<Book> firstPageResults = getCurrentPageResults();

                Platform.runLater(() -> {
                    mostraRisultati(firstPageResults);
                });

                return null; // Per il Supplier
            });

        });
    }

    @FXML
    public void closeListView(MouseEvent event) {
        hideSearchResults();
    }

    private void updateImage() {
        onSettingsButtonAction(null);
        // spostamento alla schermata impostazioni per modificare foto profilo,
        // mettere un info o una cosa del genere per avvisare l'utente che verrà
        // reindirizzato alle impostazioni per cambiare l'immagine
    }

    /**
     * Metodo helper per impostare il filtro di ricerca
     */
    private void setSearchFilter(SearchFilter filter, MenuItem menuItem, String menuText, Runnable updateMenu) {
        currentSearchFilter = filter;
        selectedMenuItem = menuItem;
        searchFilterMenu.setText(menuText);
        updateMenu.run();
        String currentText = searchingBooks.getText().trim();
        if (!currentText.isEmpty()) {
            cercaLibro(currentText);
        }
    }

    @FXML
    public void initialize() {

        profileImageView.setOnMouseClicked(event -> updateImage());

        // Ricerca in tempo reale mentre l'utente digita (con debounce)
        searchingBooks.textProperty().addListener((observable, oldValue, newValue) -> {
            String searchText = newValue.trim();
            if (!searchText.isEmpty() && searchText.length() >= 2) { // Minimo 2 caratteri per evitare ricerche troppo
                                                                     // frequenti
                // Debounce: aspetta 300ms prima di fare la ricerca
                if (debounceTimer != null) {
                    debounceTimer.cancel();
                }
                debounceTimer = new Timer();
                debounceTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        Platform.runLater(() -> {
                            List<Book> cached = ClientCacheManager.get("query:" + searchText.toLowerCase());
                            if (cached != null && !cached.isEmpty()) {
                                mostraRisultati(cached);
                            } else {
                                cercaLibro(searchText);
                            }
                        });
                    }
                }, 300); // 300ms debounce
            } else if (searchText.isEmpty()) {
                // Se il campo è vuoto, nascondi i risultati
                Platform.runLater(() -> {
                    if (resultsListView != null) {
                        resultsListView.setVisible(false);
                        resultsListView.getItems().clear();
                    }
                });
            }
        });

        // Mantieni anche il click per compatibilità
        searchingBooks.setOnMouseClicked(event -> {
            String currentText = searchingBooks.getText().trim().toLowerCase();
            if (!currentText.isEmpty()) {
                List<Book> cached = ClientCacheManager.get("query:" + currentText);
                if (cached != null && !cached.isEmpty()) {
                    mostraRisultati(cached);
                } else {
                    cercaLibro(currentText);
                }
            }
        });

        // Cache TTL-based inizializzata automaticamente dal ClientCacheManager

        // Carica raccomandazioni in modo incrementale
        loadBasicRecommendations();

        // Imposta il pulsante home come attivo all'avvio
        loadHome();

        // I libri verranno caricati on-demand quando l'utente inizia a cercare
        // Questo velocizza significativamente l'avvio dell'applicazione

        resultsListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Book book, boolean empty) {
                super.updateItem(book, empty);
                if (empty || book == null) {
                    setText(null);
                    setOnMouseClicked(null);
                } else if (book.getBook_id() == -1) {
                    setText(resolveString("%controllerdesign.search.no.results"));
                    setOnMouseClicked(null);
                } else {
                    setText(book.getTitle() + " - " + book.getAuthors() + " (" + book.getPublish_date_year() + ")");

                    // Gestisci il click sul libro per mostrarlo nella home page
                    setOnMouseClicked(event -> {
                        if (event.getClickCount() == 1) { // Click singolo
                            logger.fine(java.text.MessageFormat.format(resolveString("%design.log.fine.book.clicked"), book.getTitle(), book.getBook_id()));
                            if (book.getBook_id() != -1) { // Non cliccare su "Nessun risultato trovato"
                                // Chiudi immediatamente la lista per feedback immediato
                                resultsListView.setVisible(false);
                                resultsListView.getItems().clear();
                                searchingBooks.clear();

                                // Disabilita il campo di ricerca durante il caricamento
                                searchingBooks.setDisable(true);
                                showBookInHomePage(book);
                            }
                        }
                    });
                }
            }
        });

        // Ricerca solo quando si clicca sulla barra, non mentre si digita

        ResourceBundle bundle = LanguageManager.getBundle();

        // Metodo helper per aggiornare la selezione del menu
        Runnable updateMenuSelection = () -> {
            // Reset tutti i menu item
            searchByAll.setText(bundle.getString("dashboard.search.all"));
            searchByTitle.setText(bundle.getString("dashboard.search.title"));
            searchByAuthor.setText(bundle.getString("dashboard.search.authors"));
            searchByAuthorAndYear.setText(bundle.getString("dashboard.search.authors.years"));

            // Applica stile al selezionato
            if (selectedMenuItem != null) {
                String originalText = selectedMenuItem.getText();
                if (!originalText.startsWith("✓ ")) {
                    selectedMenuItem.setText("✓ " + originalText);
                }
            }
        };

        searchByAll.setOnAction(e -> setSearchFilter(SearchFilter.TUTTI, searchByAll,
                bundle.getString("dashboard.search.all"), updateMenuSelection));
        searchByTitle.setOnAction(e -> setSearchFilter(SearchFilter.TITOLO, searchByTitle,
                bundle.getString("dashboard.search.title"), updateMenuSelection));
        searchByAuthor.setOnAction(e -> setSearchFilter(SearchFilter.AUTORE, searchByAuthor,
                bundle.getString("dashboard.search.authors"), updateMenuSelection));
        searchByAuthorAndYear.setOnAction(e -> setSearchFilter(SearchFilter.AUTORE_ANNO, searchByAuthorAndYear,
                bundle.getString("dashboard.search.authors.years"), updateMenuSelection));

        // Inizializza con "Tutti" selezionato di default
        selectedMenuItem = searchByAll;
        updateMenuSelection.run();

        updateUserInfoUI();
        updateUserMenu();
    }

    public void updateUserInfoUI() {
        SessionManager session = SessionManager.getInstance();
        if (session.isLoggedIn()) {
            UIHelpers.updateToLogged(profileImageView, loginRegisterButton, iconLoginProfile,
                    contenitorIconLoginProfile);
            session.applyProfileImage(profileImageView);
            session.addProfileImageListener(f -> session.applyProfileImage(profileImageView));
        } else {
            UIHelpers.updateToNotLogged(profileImageView, loginRegisterButton, iconLoginProfile,
                    contenitorIconLoginProfile);
        }
    }

    @FXML
    public void openLoginWindow() throws IOException {

        Navigator.switchScene(
                (Stage) loginRegisterButton.getScene().getWindow(),
                EveryView.LOGIN.getPath(),
                controller -> {
                    if (controller instanceof ControllerLogin loginController) {
                        loginController.setOnLoginSuccess(() -> {
                            // goToMainScene() già ricarica tutto, updateUserInfo() non necessario
                            Navigator.goToMainScene();
                        });
                    }
                });

    }

    @FXML
    private MenuItem logoutMenuItem;

    @FXML
    private void handleLogout() {
        // Prima invalida la sessione lato server
        String token = SessionManager.getInstance().getSessionToken();
        if (token != null && !token.isEmpty()) {
            try {
                ServiceLocator.getUserService().invalidateSession(token);
                logger.info("Sessione invalidata lato server");
            } catch (Exception e) {
                logger.warning("Errore nell'invalidazione della sessione lato server: " + e.getMessage());
                // Continua comunque con il logout locale
            }
        }

        // Pulisci i backup delle librerie dell'utente precedente
        it.uninsubria.client.controller.homepage.libreria.LibraryListController.cleanupOldUserBackup();

        // Poi fai logout locale
        SessionManager.getInstance().logout();
        updateUserInfoUI();
        updateUserMenu();
        Navigator.goToMainScene();
        logger.info("Utente disconnesso");
    }

    /**
     * Mostra un libro selezionato dalla ricerca nella home page
     */
    private void showBookInHomePage(Book book) {
        try {
            logger.fine("showBookInHomePage called for book: " + book.getTitle());
            // Import necessario per viewHome
            it.uninsubria.client.controller.homepage.home.viewHome.setSelectedBookFromSearch(book);
            logger.fine("selectedBookFromSearch set");

            // Mostra la vista di loading con cerchio invece del piccolo progress indicator
            setActiveButton(homeButton, iconHome, titleHomeButton);

            // Crea una vista di loading personalizzata per aspettare il caricamento
            // dell'immagine
            createLoadingViewWithImageWait(book);

        } catch (RuntimeException e) {
            logger.log(Level.SEVERE, "Errore nel mostrare il libro nella home page", e);
            searchingBooks.setDisable(false);
        }
    }

    private void createLoadingViewWithImageWait(Book book) {
        // Mostra direttamente la home page senza aspettare il caricamento dell'immagine
        // L'immagine verrà caricata in background nella viewHome con il sistema di loading esistente
        logger.info(java.text.MessageFormat.format(resolveString("%design.log.info.show.home.immediately"), book.getTitle()));
        Platform.runLater(() -> {
            try {
                forceReloadHomePage();
                logger.fine(resolveString("%design.debug.force.reload.home.completed"));

                // Riabilita il campo di ricerca
                searchingBooks.setDisable(false);
            } catch (RuntimeException e) {
                logger.severe(java.text.MessageFormat.format(resolveString("%design.error.reload.home"), e.getMessage()));
                forceReloadHomePage();
                searchingBooks.setDisable(false);
            }
        });
    }

    /**
     * Forza il ricaricamento della home page per mostrare il libro selezionato
     */
    private void forceReloadHomePage() {
        // Invece di usare loadHome() normale, forzo il ricaricamento
        // Rimuovo il controllo che evita il ricaricamento se siamo già nella stessa
        // vista
        String fxmlPath = EveryView.HOME_VIEW.getPath();
        navigateToView(homeButton, iconHome, titleHomeButton, fxmlPath);
    }

    /**
     * Metodo helper per navigazione unificata
     */
    private void navigateToView(Button button, FontIcon icon, Label label, String fxmlPath) {
        hideSearchResults();
        setActiveButton(button, icon, label);
        setContentWithTransition(fxmlPath);
    }

    private void navigateToViewWithLoginCheck(Button button, FontIcon icon, Label label, String fxmlPath) {
        hideSearchResults();
        setActiveButton(button, icon, label);
        if (UIHelpers.showLoginPromptIfNotLogged(HBoxContent, loginInfoButton)) {
            setContentWithTransition(fxmlPath);
        }
    }

    /**
     * Carica la pagina home
     */
    public void loadHome() {
        navigateToView(homeButton, iconHome, titleHomeButton, EveryView.HOME_VIEW.getPath());
    }

    /**
     * Gestisce il click sul pulsante home
     */
    @FXML
    private void onHomeButtonAction(ActionEvent event) {
        loadHome();
    }

    /**
     * Gestisce il click sul pulsante categorie
     */
    @FXML
    private void onCategoriesButtonAction(ActionEvent event) {
        navigateToView(categoriesButton, categoriesIcon, titleCategoriesButton, EveryView.CATEGORIES_VIEW.getPath());
    }

    /**
     * Gestisce il click sul pulsante libreria
     */
    @FXML
    private void onlibrariesButtonAction(ActionEvent event) {
        navigateToViewWithLoginCheck(libraryButton, librariesIcon, titleLibrariesButton,
                EveryView.LIBRARIES_VIEW.getPath());
    }

    /**
     * Gestisce il click sul pulsante impostazioni
     */
    @FXML
    private void onSettingsButtonAction(ActionEvent event) {
        navigateToView(settingsButton, settingsIcon, titleSettingsButton, EveryView.SETTINGS_VIEW.getPath());
    }

    /**
     * Gestisce il click sul pulsante about
     */
    @FXML
    private void onAboutButtonAction(ActionEvent event) {
        navigateToView(aboutButton, aboutIcon, titleAboutButton, EveryView.ABOUT_VIEW.getPath());
    }

    /**
     * Gestisce il click sul pulsante exit
     */
    @FXML
    private void onExitButtonAction(ActionEvent event) {
        navigateToView(exitButton, exitIcon, titleExitButton, EveryView.EXIT_VIEW.getPath());
    }

    /**
     * Aggiorna il menu utente con la lista degli utenti disponibili
     */
    private void updateUserMenu() {
        // Rimuovi tutti gli elementi esistenti tranne il logout
        iconLoginProfile.getItems().removeIf(item -> !item.getText().equals(resolveString("%controllerdesign.logout")));

        // Aggiungi voci per switchare tra utenti
        var userSessions = SessionManager.getInstance().getAllUserSessions();
        if (userSessions.size() > 1) { // Solo se ci sono più utenti
            // Aggiungi separatore
            var separator = new SeparatorMenuItem();
            iconLoginProfile.getItems().add(0, separator);

            // Aggiungi voci per ogni utente
            for (var session : userSessions) {
                if (!session.getUserId().equals(SessionManager.getInstance().getActiveUserId())) {
                    var switchItem = new MenuItem(
                            "Passa a " + (session.getUsername() != null ? session.getUsername() : session.getEmail()));
                    switchItem.setOnAction(e -> switchToUser(session.getUserId()));
                    iconLoginProfile.getItems().add(0, switchItem);
                }
            }

            // Aggiungi header
            var headerItem = new MenuItem(resolveString("%controllerdesign.available.accounts"));
            headerItem.setDisable(true);
            iconLoginProfile.getItems().add(0, headerItem);
        }
    }

    /**
     * Passa a un utente specifico
     */
    private void switchToUser(String userId) {
        SessionManager.getInstance().switchToUser(userId);
        updateUserInfoUI();
        updateUserMenu();
        logger.info(java.text.MessageFormat.format(resolveString("%design.log.info.user.switched"), userId));
    }

    @Override
    public void onLanguageChanged(String newLanguage) {

        // Update UI texts that depend on the bundle
        updateUserInfoUI();
        updateUserMenu();

        // Refresh search menu items with new language
        refreshSearchMenuTexts();

        // Update other UI elements that use localized text
        if (homeButton != null) {
            // These are static elements, but we can refresh them if needed
            logger.fine("Language changed to: " + newLanguage + " - UI texts refreshed");
        }
    }

    private void refreshSearchMenuTexts() {
        // Update the search filter menu items with new language texts
        try {
            searchByAll.setText(resolveString("dashboard.search.all"));
            searchByTitle.setText(resolveString("dashboard.search.title"));
            searchByAuthor.setText(resolveString("dashboard.search.authors"));
            searchByAuthorAndYear.setText(resolveString("dashboard.search.authors.years"));
        } catch (Exception e) {
            logger.warning("Error refreshing search menu texts: " + e.getMessage());
        }
    }



    /**
     * Salva le categorie calcolate su disco per caricamento rapido successivo
     */
    public static void saveCategoriesToDisk() {
        if (!categoriesBuilt || categoryIndex.isEmpty()) {
            logger.fine(resolveString("%design.log.fine.no.categories.to.save"));
            return;
        }

        try {
            // Crea directory se non esiste (multiplatform)
            Path cacheDir = Paths.get(CATEGORIES_CACHE_DIR);
            Files.createDirectories(cacheDir);

            Path cacheFile = Paths.get(CATEGORIES_CACHE_FILE);

            // Crea oggetto serializzabile (solo metadati essenziali per ridurre dimensione)
            Map<String, List<Integer>> serializableCategories = new java.util.HashMap<>();
            for (Map.Entry<String, List<Book>> entry : categoryIndex.entrySet()) {
                List<Integer> bookIds = entry.getValue().stream()
                        .map(Book::getBook_id)
                        .collect(java.util.stream.Collectors.toList());
                serializableCategories.put(entry.getKey(), bookIds);
            }

            // Salva su disco
            objectMapper.writeValue(cacheFile.toFile(), serializableCategories);
            logger.info(java.text.MessageFormat.format(resolveString("%design.log.info.categories.saved.to.disk"),
                    serializableCategories.size(),
                    serializableCategories.values().stream().mapToInt(List::size).sum()));

        } catch (IOException e) {
            logger.warning(java.text.MessageFormat.format(resolveString("%design.error.save.categories.to.disk"), e.getMessage()));
        }
    }

    /**
     * Carica le categorie dalla cache su disco se disponibile e valida
     */
    public static boolean loadCategoriesFromDisk() {
        try {
            Path cacheFile = Paths.get(CATEGORIES_CACHE_FILE);
            if (!Files.exists(cacheFile)) {
                logger.fine(resolveString("%design.log.fine.cache.file.not.found"));
                return false;
            }

            // Carica da disco
            @SuppressWarnings("unchecked")
            Map<String, List<Integer>> serializableCategories = objectMapper.readValue(cacheFile.toFile(), Map.class);

            // Verifica che i libri corrispondano (almeno alcuni)
            if (allBooks.isEmpty()) {
                logger.warning("Impossibile caricare categorie da disco: libri non ancora caricati");
                return false;
            }

            // Ricostruisci la mappa delle categorie
            Map<String, List<Book>> loadedCategories = new java.util.concurrent.ConcurrentHashMap<>();
            Map<Integer, Book> bookMap = allBooks.stream()
                    .collect(java.util.stream.Collectors.toMap(Book::getBook_id, book -> book));

            for (Map.Entry<String, List<Integer>> entry : serializableCategories.entrySet()) {
                List<Book> categoryBooks = entry.getValue().stream()
                        .map(bookMap::get)
                        .filter(java.util.Objects::nonNull)
                        .collect(java.util.stream.Collectors.toList());

                if (!categoryBooks.isEmpty()) {
                    loadedCategories.put(entry.getKey(), categoryBooks);
                }
            }

            // Verifica che abbiamo caricato abbastanza categorie
            if (loadedCategories.size() < serializableCategories.size() * 0.8) {
                logger.warning("Cache categorie obsoleta: " + loadedCategories.size() + "/"
                        + serializableCategories.size() + " categorie valide");
                return false;
            }

            // Applica le categorie caricate
            categoryIndex.clear();
            categoryIndex.putAll(loadedCategories);
            categoriesBuilt = true;
            lastCategoryBuildTime = System.currentTimeMillis();

            logger.info("✅ Categorie caricate da disco: " + loadedCategories.size() + " categorie, " +
                    loadedCategories.values().stream().mapToInt(List::size).sum() + " libri totali");
            return true;

        } catch (IOException e) {
            logger.warning("❌ Errore nel caricare categorie da disco: " + e.getMessage());
            return false;
        }
    }

    /**
     * Pre-costruisce l'indice delle categorie se non già disponibile
     * Ora ottimizzato con caricamento da disco e salvataggio automatico
     */
    public static void prebuildCategoriesIfNeeded() {
        // ⚠️ ASSICURA CHE TUTTI I LIBRI SIANO CARICATI PRIMA DI PRE-COSTRUIRE
        ensureBooksLoaded();

        if (!categoriesBuilt && allBooks != null && !allBooks.isEmpty()) {
            long startTime = System.currentTimeMillis();
            buildCategoryIndexOptimized(allBooks);
            categoriesBuilt = true;
            lastCategoryBuildTime = System.currentTimeMillis();
            long duration = lastCategoryBuildTime - startTime;
            logger.info("🏗️ Categorie pre-calcolate in " + duration + "ms (" + categoryIndex.size() + " categorie)");
        } else if (allBooks == null || allBooks.isEmpty()) {
            logger.warning("⚠️ Impossibile pre-calcolare categorie: nessun libro disponibile");
        }
    }

}
