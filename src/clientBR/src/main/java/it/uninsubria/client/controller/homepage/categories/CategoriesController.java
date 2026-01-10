package it.uninsubria.client.controller.homepage.categories;

import it.uninsubria.client.controller.ControllerBase;
import it.uninsubria.client.controller.homepage.home.ControllerDesign;
import it.uninsubria.client.utils.classesUI.LanguageManager;
import it.uninsubria.client.utils.classesUI.ParametrizedController;
import it.uninsubria.shared.model.Book;
import it.uninsubria.shared.utils.AppConstants;
import javafx.application.Platform;

import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Controller for the categories view that displays book categories in a grid layout.
 * Handles category loading, filtering, and navigation to category detail views.
 *
 * <p>This controller manages the main categories interface, including:
 * <ul>
 * <li>Loading and displaying book categories</li>
 * <li>Search and filter functionality</li>
 * <li>Navigation to detailed category views</li>
 * <li>Performance optimization with caching and pagination</li>
 * </ul>
 *
 * @author Book Recommender Team
 * @version 1.0
 * @since 1.0
 */
public class CategoriesController extends ControllerBase implements Initializable, ParametrizedController {

    // UI Constants

    // UI Dimensions
    private static final double CARD_WIDTH = 200;
    private static final double CARD_HEIGHT = 150;
    private static final double SEARCH_WIDTH = 300;

    // Pagination and Performance
    private static final int POOL_SIZE = 10;

    // Logger
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(CategoriesController.class.getName());

    @FXML private VBox mainContainer;
    @FXML private FlowPane categoriesGrid;
    @FXML private Label statsLabel;

    // Loading UI components
    @FXML private VBox loadingContainer;
    @FXML private ProgressIndicator loadingProgress;
    @FXML private Label loadingLabel;

    private ObservableList<CategoryCard> allCategories = FXCollections.observableArrayList();
    private FilteredList<CategoryCard> filteredCategories;
    private SortedList<CategoryCard> sortedCategories;

    // Pool di card per ottimizzazione performance - evita ricreazione continua
    private final Map<String, VBox> cardPool = new HashMap<>();
    private final List<String> activeCards = new ArrayList<>();

    // Caching per evitare ricaricamenti inutili - aumentato grazie alle ottimizzazioni
    private boolean categoriesLoaded = false;
    private long lastCategoriesLoadTime = 0;
    private static final long CATEGORIES_CACHE_DURATION = AppConstants.Time.TIMEOUT_15_MINUTES;

    /**
     * Initializes the categories controller.
     * Sets up the UI components and loads categories if not cached.
     *
     * @param location the location used to resolve relative paths for the root object
     * @param resources the resources used to localize the root object
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Setup UI solo la prima volta
        if (!categoriesLoaded) {
            setupUI();
            // Nascondi loading container inizialmente
            loadingContainer.setVisible(false);
        }

        // Controlla se ricaricare le categorie o usare la cache
        long currentTime = System.currentTimeMillis();
        if (!categoriesLoaded || (currentTime - lastCategoriesLoadTime) > CATEGORIES_CACHE_DURATION) {
            loadCategories();
            categoriesLoaded = true;
            lastCategoriesLoadTime = currentTime;
        }
        // L'UI si aggiorna automaticamente grazie al listener su sortedCategories

        updateStats();
    }

    /**
     * Initializes the controller with parameters passed from the navigation system.
     * Currently, this controller doesn't require initialization parameters.
     *
     * @param params a map containing initialization parameters (may be null or empty)
     */
    @Override
    public void initData(Map<String, Object> params) {
        // Implementazione vuota per ora
    }

    /**
     * Sets up the user interface components and event handlers.
     * Initializes the categories grid, search functionality, and loading indicators.
     */
    private void setupUI() {
        // Setup search field with icon
        HBox searchBox = new HBox(10);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        searchBox.setPadding(new Insets(10));

         TextField searchField = new TextField();
         searchField.setPromptText(resolveString("%categories.search.prompt"));
         searchField.setPrefWidth(SEARCH_WIDTH);
         searchField.getStyleClass().add("categorySearchField");

        searchBox.getChildren().add(searchField);

        // Setup sort combo box
        ComboBox<String> sortComboBox = new ComboBox<>();
        sortComboBox.setItems(FXCollections.observableArrayList(
            SortOption.BOOK_COUNT_DESC.getLocalizedString(),
            SortOption.BOOK_COUNT_ASC.getLocalizedString(),
            SortOption.NAME_ASC.getLocalizedString(),
            SortOption.NAME_DESC.getLocalizedString()
        ));
        sortComboBox.setValue(SortOption.BOOK_COUNT_DESC.getLocalizedString());
        sortComboBox.getStyleClass().add("categoriesComboBox");

        // Setup reset filters button
        Button resetFiltersButton = new Button(resolveString("%categories.reset"));
        resetFiltersButton.getStyleClass().add("categoriesButtonReset");
        resetFiltersButton.setOnAction(e -> resetFilters());

        // Pulsante refresh manuale
        Button refreshButton = new Button("🔄 " + resolveString("%categories.refresh"));
        refreshButton.setOnAction(e -> refreshCategories());

        HBox controlsBox = new HBox(20);
        controlsBox.setAlignment(Pos.CENTER_LEFT);
        controlsBox.setPadding(new Insets(0, 20, 10, 20));
        controlsBox.getChildren().addAll(searchBox, new Label(resolveString("%categories.sort")), sortComboBox, resetFiltersButton, refreshButton);

        // Add controls to main container
        mainContainer.getChildren().add(0, controlsBox);

        // Add recommendations section
        addRecommendationsSection();

        // Setup categories grid
        categoriesGrid.setHgap(15);
        categoriesGrid.setVgap(15);
        categoriesGrid.setPadding(new Insets(20));

        // 🎯 INDIcatore di caricamento iniziale - ora gestito dal loadingContainer

        // Inizializza il sistema di pooling
        initializeCardPooling();

        // Setup filtered list
        filteredCategories = new FilteredList<>(allCategories, p -> true);

        // Setup search functionality
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredCategories.setPredicate(category -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }

                String lowerCaseFilter = newValue.toLowerCase();
                return category.getCategoryName().toLowerCase().contains(lowerCaseFilter);
            });
        });

        // Setup sorted list
        sortedCategories = new SortedList<>(filteredCategories);
        sortedCategories.comparatorProperty().bind(
            Bindings.createObjectBinding(() -> {
                String selectedString = sortComboBox.getValue();
                // Trova il SortOption corrispondente alla stringa selezionata
                for (SortOption option : SortOption.values()) {
                    if (option.getLocalizedString().equals(selectedString)) {
                        return getComparatorForSortOption(option);
                    }
                }
                // Default fallback
                return getComparatorForSortOption(SortOption.BOOK_COUNT_DESC);
            }, sortComboBox.valueProperty())
        );

        // Bind to grid
        sortedCategories.addListener((javafx.collections.ListChangeListener<CategoryCard>) c -> {
            // OTTIMIZZAZIONE: Usa pooling intelligente invece di ricreare tutto
            updateCategoriesGridOptimized();
        });
    }

    /**
     * Loads book categories from the server and populates the UI.
     * Uses caching to avoid unnecessary network requests.
     * Categories are loaded asynchronously to prevent UI blocking.
     */
    private void loadCategories() {
        // ⚡ VERIFICA CACHE: Se già caricato di recente, non ricaricare
        long currentTime = System.currentTimeMillis();
        if (categoriesLoaded && (currentTime - lastCategoriesLoadTime) < CATEGORIES_CACHE_DURATION) {
            logger.info("Using cached categories (loaded " + ((currentTime - lastCategoriesLoadTime) / AppConstants.Time.MILLIS_PER_SECOND) + "s ago)");
            updateStats();
            return;
        }

        // 🎯 MOSTRA INDICATORE DI CARICAMENTO
        Platform.runLater(() -> {
            loadingContainer.setVisible(true);
            loadingProgress.setProgress(-1); // Indeterminate progress
            loadingLabel.setText(resolveString("%categories.loading.categories"));
            categoriesGrid.setVisible(false);
        });

        // 🚀 CARICAMENTO ASINCRONO: Non bloccare il thread UI!
        long startTime = System.currentTimeMillis();
        CompletableFuture.supplyAsync(() -> {
            // ⚡ Prima prova: categorie già pre-calcolate (istantaneo)
            Map<String, List<Book>> categoryIndex = ControllerDesign.getCategories();

            long indexRetrievalTime = System.currentTimeMillis();
            logger.fine("Category index retrieval time: " + (indexRetrievalTime - startTime) + "ms");

            // Se non ci sono categorie, dobbiamo calcolarle (fallback asincrono)
            if (categoryIndex.isEmpty()) {
                logger.warning("Categories not pre-calculated, computing asynchronously...");
                categoryIndex = ControllerDesign.getCategoriesFallback();
                logger.info("Categories computed in background: " + categoryIndex.size() + " categories");
            }

            // Crea le card in background
            List<CategoryCard> categoryCards = categoryIndex.entrySet().stream()
                .map(entry -> new CategoryCard(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

            long cardsCreationTime = System.currentTimeMillis();
            logger.fine("Category cards creation time: " + (cardsCreationTime - indexRetrievalTime) + "ms");

            return categoryCards;

        }).thenAccept(categoryCards -> {
            // 🎯 AGGIORNA UI NEL THREAD JAVAFX (non bloccante)
            Platform.runLater(() -> {
                // Rimuovi indicatore di caricamento e mostra la grid
                loadingContainer.setVisible(false);
                categoriesGrid.setVisible(true);
                categoriesGrid.getChildren().clear();

                allCategories.setAll(categoryCards);

                long totalTime = System.currentTimeMillis() - startTime;
                logger.info("Total categories loading time: " + totalTime + "ms (" + categoryCards.size() + " categories)");

                // Forza aggiornamento stats
                updateStats();

                // L'UI si aggiornerà automaticamente grazie al listener su sortedCategories
                logger.info("Categories loaded and UI updated successfully");
            });
        }).exceptionally(throwable -> {
            // ❌ GESTIONE ERRORI
            Platform.runLater(() -> {
                logger.log(java.util.logging.Level.SEVERE, "Error loading categories", throwable);

                // Nascondi loading e mostra errore
                loadingContainer.setVisible(false);
                categoriesGrid.setVisible(true);
                categoriesGrid.getChildren().clear();

                Label errorLabel = new Label("❌ " + resolveString("%categories.error.loading"));
                errorLabel.getStyleClass().add("categoriesErrorLabel");
                categoriesGrid.getChildren().add(errorLabel);

                // Forza aggiornamento stats (mostrerà 0 categorie)
                updateStats();
            });
            return null;
        });
    }

    /**
     * Forza il ricaricamento delle categorie (ignora la cache)
     */
    /**
     * Forces a refresh of the categories data from the server.
     * Bypasses the cache and reloads all categories.
     * Typically called when the user explicitly requests a refresh.
     */
    public void refreshCategories() {
        logger.info("Forcing categories refresh...");
        categoriesLoaded = false;
        lastCategoriesLoadTime = 0;

        // Forza anche il refresh della cache di ControllerDesign
        ControllerDesign.refreshCategoriesCache();

        // Mostra indicatore di refresh
        Platform.runLater(() -> {
            loadingContainer.setVisible(true);
            loadingLabel.setText(resolveString("%categories.refreshing"));
        });

        loadCategories();
        categoriesLoaded = true;
        lastCategoriesLoadTime = System.currentTimeMillis();
        logger.info("Categories refresh completed");
    }

    private Comparator<CategoryCard> getComparatorForSortOption(SortOption sortOption) {
        switch (sortOption) {
            case BOOK_COUNT_DESC:
                return Comparator.comparing(CategoryCard::getBookCount).reversed();
            case BOOK_COUNT_ASC:
                return Comparator.comparing(CategoryCard::getBookCount);
            case NAME_ASC:
                return Comparator.comparing(CategoryCard::getCategoryName);
            case  NAME_DESC:
                return Comparator.comparing(CategoryCard::getCategoryName).reversed();
            default:
                return Comparator.comparing(CategoryCard::getBookCount).reversed();
        }
    }

    private void updateStats() {
        int totalCategories = sortedCategories.size();
        int totalBooks = sortedCategories.stream()
            .mapToInt(CategoryCard::getBookCount)
            .sum();

        // Calculate additional statistics
        double avgBooksPerCategory = totalCategories > 0 ? (double) totalBooks / totalCategories : 0;
        int maxBooksInCategory = sortedCategories.stream()
            .mapToInt(CategoryCard::getBookCount)
            .max()
            .orElse(0);

        // Find most popular category
        String mostPopularCategory = sortedCategories.stream()
            .max(Comparator.comparing(CategoryCard::getBookCount))
            .map(CategoryCard::getCategoryName)
            .orElse("N/A");

        String statsText = String.format("%d %s • %d %s • %s: %.1f %s • %s: %s (%d %s)",
            totalCategories, resolveString("%categories.categories"),
            totalBooks, resolveString("%categories.totalBooks"),
            resolveString("%categories.media"), avgBooksPerCategory, resolveString("%categories.media_libricat"),
            resolveString("%categories.popular"), capitalizeFirstLetter(mostPopularCategory), maxBooksInCategory, resolveString("%categories.books"));

        statsLabel.setText(statsText);
    }

    private void resetFilters() {
        // Reset will be handled by the listeners when search field is cleared
        // and sort combo box is reset to default
    }

    private String capitalizeFirstLetter(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }

    private void addRecommendationsSection() {
        // Create recommendations header
        Label recommendationsLabel = new Label(resolveString("%categories.suggestions"));
        recommendationsLabel.getStyleClass().add("categoriesRecommendationsLabel");

        // Create recommendations content
        HBox recommendationsBox = new HBox(15);
        recommendationsBox.setAlignment(Pos.CENTER_LEFT);
        recommendationsBox.setPadding(new Insets(10));
        recommendationsBox.getStyleClass().add("categoriesRecommendationsBox");

        // Popular categories suggestion
        VBox popularBox = new VBox(5);
        popularBox.setAlignment(Pos.CENTER_LEFT);
        Label popularTitle = new Label(resolveString("%categories.popular.title"));
        popularTitle.getStyleClass().add("categoriesRecommendationsTitle");
        Label popularDesc = new Label(resolveString("%categories.popular.desc"));
        popularDesc.getStyleClass().addAll("categoriesRecommendationsDesc", "categoriesTextSecondary");

        // New categories suggestion
        VBox newBox = new VBox(5);
        newBox.setAlignment(Pos.CENTER_LEFT);
        Label newTitle = new Label(resolveString("%categories.new.title"));
        newTitle.getStyleClass().add("categoriesRecommendationsTitle");
        Label newDesc = new Label(resolveString("%categories.new.desc"));
        newDesc.getStyleClass().addAll("categoriesRecommendationsDesc", "categoriesTextSecondary");

        popularBox.getChildren().addAll(popularTitle, popularDesc);
        newBox.getChildren().addAll(newTitle, newDesc);

        recommendationsBox.getChildren().addAll(popularBox, new Separator(javafx.geometry.Orientation.VERTICAL), newBox);

        // Add to main container
        VBox recommendationsContainer = new VBox(10);
        recommendationsContainer.getChildren().addAll(recommendationsLabel, recommendationsBox);
        mainContainer.getChildren().add(recommendationsContainer);
    }

    /**
     * Inizializza il sistema di pooling per le card delle categorie
     */
    private void initializeCardPooling() {
        // Prealloca alcune card vuote nel pool per velocità iniziale
        for (int i = 0; i < Math.min(POOL_SIZE, allCategories.size()); i++) {
            String dummyKey = "pool_" + i;
            cardPool.put(dummyKey, createEmptyCard());
        }
    }

    /**
     * Aggiorna il grid delle categorie in modo ottimizzato usando pooling
     */
    private void updateCategoriesGridOptimized() {
        long startTime = System.currentTimeMillis();

        // Rimuovi card non più necessarie dal pool attivo
        List<String> toRemove = new ArrayList<>();
        for (String categoryName : activeCards) {
            boolean stillExists = sortedCategories.stream()
                .anyMatch(card -> card.getCategoryName().equals(categoryName));
            if (!stillExists) {
                toRemove.add(categoryName);
            }
        }
        activeCards.removeAll(toRemove);

        // Rilascia le card rimosse nel pool (nascondile per riutilizzo futuro)
        for (String categoryName : toRemove) {
            if (cardPool.containsKey(categoryName)) {
                VBox card = cardPool.get(categoryName);
                card.setVisible(false);
                card.setManaged(false);
            }
        }

        // Aggiorna il grid con le card attive
        categoriesGrid.getChildren().clear();
        activeCards.clear();

        for (CategoryCard categoryCard : sortedCategories) {
            String categoryName = categoryCard.getCategoryName();
            activeCards.add(categoryName);

            // Usa pooling: riutilizza card esistenti o creane di nuove
            VBox card = getOrCreateCard(categoryName, categoryCard);
            card.setVisible(true);
            card.setManaged(true);

            categoriesGrid.getChildren().add(card);
        }

        long duration = System.currentTimeMillis() - startTime;
        logger.fine("Categories grid updated in " + duration + "ms (" + sortedCategories.size() + " categories)");
    }

    /**
     * Ottiene una card dal pool o ne crea una nuova
     */
    private VBox getOrCreateCard(String categoryName, CategoryCard categoryCard) {
        if (cardPool.containsKey(categoryName)) {
            // Riutilizza card esistente - aggiorna solo i dati
            VBox existingCard = cardPool.get(categoryName);
            updateCardContent(existingCard, categoryCard);
            return existingCard;
        } else {
            // Crea nuova card e aggiungila al pool
            VBox newCard = categoryCard.getCard();
            cardPool.put(categoryName, newCard);
            return newCard;
        }
    }

    /**
     * Aggiorna il contenuto di una card esistente (più efficiente che ricrearla)
     */
    private void updateCardContent(VBox card, CategoryCard categoryCard) {
        // Aggiorna solo i testi invece di ricreare tutto
        if (card.getChildren().size() >= 4) {
            Label nameLabel = (Label) card.getChildren().get(0);
            Label countLabel = (Label) card.getChildren().get(1);
            Label previewLabel = (Label) card.getChildren().get(2);

            nameLabel.setText(capitalizeFirstLetter(categoryCard.getCategoryName()));
            countLabel.setText(categoryCard.getBookCount() + " " + resolveString("%categories.books"));
            previewLabel.setText(categoryCard.getBookPreview());
        }
    }

    /**
     * Crea una card vuota per il pool iniziale
     */
    private VBox createEmptyCard() {
        VBox card = new VBox(10);
        card.setPrefSize(CARD_WIDTH, CARD_HEIGHT);
        card.getStyleClass().add("categoriesCard");
        card.setAlignment(Pos.TOP_LEFT);
        card.setVisible(false);
        card.setManaged(false);
        return card;
    }

    // Inner class for category cards
    /**
     * Represents a category card in the UI grid.
     * Contains category information and manages the visual representation.
     * Each card displays category name, book count, and preview of books.
     */
    private class CategoryCard {
        private final String categoryName;
        private final List<Book> books;
        private final VBox card;

        /**
         * Creates a new category card with the specified category name and books.
         *
         * @param categoryName the name of the category
         * @param books the list of books in this category
         */
        public CategoryCard(String categoryName, List<Book> books) {
            this.categoryName = categoryName;
            this.books = books;
            this.card = createCard();
        }

        private VBox createCard() {
            VBox card = new VBox(10);
            card.setPrefSize(CARD_WIDTH, CARD_HEIGHT);
            card.getStyleClass().add("categoriesCard");
            card.setAlignment(Pos.TOP_LEFT);

            // Category name
            Label nameLabel = new Label(CategoriesController.this.capitalizeFirstLetter(categoryName));
            nameLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
            nameLabel.setWrapText(true);

            // Book count
            Label countLabel = new Label(books.size() + " " + resolveString("%categories.books"));
            countLabel.setFont(Font.font("System", 12));
            countLabel.getStyleClass().add("categoriesTextSecondary");

            // Sample books preview
            Label previewLabel = new Label(getBookPreview());
            previewLabel.setFont(Font.font("System", 11));
            previewLabel.getStyleClass().add("categoriesTextMuted");
            previewLabel.setWrapText(true);
            previewLabel.setMaxHeight(40);

            // View button
            Button viewButton = new Button(resolveString("%categories.button"));
            viewButton.getStyleClass().add("categoriesButtonPrimary");
            viewButton.setOnAction(e -> openCategoryDetail(categoryName));

            card.getChildren().addAll(nameLabel, countLabel, previewLabel, viewButton);
            VBox.setVgrow(previewLabel, Priority.ALWAYS);

            // Hover effect (usando pseudo-classi CSS invece di setStyle)
            // Gli stili hover sono definiti nella classe .categoriesCard:hover

            return card;
        }



        private String getBookPreview() {
            if (books.isEmpty()) return "";

            List<String> titles = books.stream()
                .limit(2)
                .map(Book::getTitle)
                .map(title -> title.length() > 30 ? title.substring(0, 27) + "..." : title)
                .collect(Collectors.toList());

            return String.join("\n", titles);
        }

        /**
         * Opens the detailed view for a specific category.
         * Navigates to the CategoryDetailController with the selected category.
         *
         * @param categoryName the name of the category to display in detail
         */
        private void openCategoryDetail(String categoryName) {
            try {
                Map<String, Object> params = new HashMap<>();
                params.put("category", categoryName);
                it.uninsubria.client.utils.classesUI.Navigator.openNewWindow(
                    it.uninsubria.client.utils.classesUI.EveryView.CATEGORY_DETAIL_VIEW.getPath(), "", params
                );

            } catch (Exception e) {
                logger.log(java.util.logging.Level.SEVERE, "Error opening category detail for: " + categoryName, e);
            }
        }


        /**
         * Gets the name of the category.
         *
         * @return the category name
         */
        public String getCategoryName() {
            return categoryName;
        }

        /**
         * Gets the number of books in this category.
         *
         * @return the book count
         */
        public int getBookCount() {
            return books.size();
        }

        /**
         * Gets the JavaFX VBox component representing this category card.
         *
         * @return the card component
         */
        public VBox getCard() {
            return card;
        }
    }
}