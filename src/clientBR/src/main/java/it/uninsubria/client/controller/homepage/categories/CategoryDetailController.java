package it.uninsubria.client.controller.homepage.categories;

import it.uninsubria.client.controller.ControllerBase;
import it.uninsubria.client.controller.homepage.home.ControllerDesign;
import it.uninsubria.client.utils.classesUI.EveryView;
import it.uninsubria.client.utils.classesUI.Navigator;
import it.uninsubria.client.utils.classesUI.ParametrizedController;
import it.uninsubria.client.utils.classesUI.ServiceLocator;
import it.uninsubria.client.utils.classesUI.ThreadPoolManager;
import it.uninsubria.shared.model.Book;
import it.uninsubria.shared.model.Library;
import it.uninsubria.shared.utils.AppConstants;
import it.uninsubria.shared.utils.LoggerUtil;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Controller for the detailed category view that displays books within a specific category.
 * Provides search, sorting, and filtering functionality for category books.
 *
 * <p>This controller manages:
 * <ul>
 * <li>Loading and displaying books for a specific category</li>
 * <li>Search functionality within the category</li>
 * <li>Sorting options (by title, author, year)</li>
 * <li>Adding books to user libraries</li>
 * <li>Navigation to individual book details</li>
 * </ul>
 *
 * <p>Performance optimizations include:
 * <ul>
 * <li>Caching of category books</li>
 * <li>Asynchronous loading</li>
 * <li>Virtualized list view with fixed cell size</li>
 * </ul>
 *
 * @author Book Recommender Team
 * @version 1.0
 * @since 1.0
 */
public class CategoryDetailController extends ControllerBase implements Initializable, ParametrizedController {

    private static final Logger logger = LoggerUtil.getLogger(CategoryDetailController.class);

    // UI Constants

    // Dialog dimensions
    private static final double DIALOG_WIDTH = 400;
    private static final double DIALOG_HEIGHT = 300;

    // UI dimensions
    private static final double DESCRIPTION_MAX_HEIGHT = 40;

    // Performance optimizations
    private static final long CATEGORY_CACHE_DURATION = AppConstants.Time.TIMEOUT_10_MINUTES;
    private boolean booksLoaded = false;
    private long lastBooksLoadTime = 0;

    @FXML private VBox mainContainer;
    @FXML private Label categoryTitleLabel;
    @FXML private Label statsLabel;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortComboBox;
    @FXML private ListView<Book> booksListView;
    @FXML private Button backButton;
    @FXML private Button resetFiltersButton;
    @FXML private Button reloadButton;
    @FXML private VBox emptyStateContainer;

    private String currentCategory;
    private ObservableList<Book> categoryBooks = FXCollections.observableArrayList();
    private FilteredList<Book> filteredBooks;
    private SortedList<Book> sortedBooks;

    /**
     * Initializes the category detail controller.
     * Sets up the UI components and prepares search/sort functionality.
     *
     * @param location the location used to resolve relative paths for the root object
     * @param resources the resources used to localize the root object
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            setupUI();
            setupSearchAndSort();
        } catch (Exception e) {
            logger.log(java.util.logging.Level.SEVERE, "Error initializing CategoryDetailController", e);
            // Fallback: setup minimal UI if initialization fails
            setupMinimalUI();
        }
    }

    /**
     * Initializes the controller with parameters passed from the navigation system.
     * Expects a "category" parameter containing the category name to display.
     *
     * @param params a map containing initialization parameters, must include "category" key
     */
    @Override
    public void initData(Map<String, Object> params) {
        if (params != null && params.containsKey("category")) {
            Object categoryParam = params.get("category");
            if (categoryParam instanceof String) {
                setCategory((String) categoryParam);
            }
        }
    }

    /**
     * Sets the current category to display and loads its books.
     * Resets cache if switching to a different category.
     *
     * @param categoryName the name of the category to display
     */
    public void setCategory(String categoryName) {
        String previousCategory = this.currentCategory;
        this.currentCategory = categoryName;

        // Reset cache se categoria cambiata
        if (previousCategory != null && !categoryName.equalsIgnoreCase(previousCategory)) {
            booksLoaded = false;
            lastBooksLoadTime = 0;
        }

        // Aggiorna UI con la nuova categoria
        updateUIWithCategory();

        // Load books for the new category
        loadCategoryBooks();
    }

    /**
     * Sets up the user interface components and event handlers.
     * Initializes buttons, search fields, combo boxes, and list views.
     */
    private void setupUI() {
        // Setup back button
        FontIcon backIcon = new FontIcon("fas-arrow-left");
        backIcon.setIconSize(16);
        backButton.setGraphic(backIcon);
        backButton.setOnAction(e -> goBackToCategories());

        // Setup search field with fallback
        try {
            searchField.setPromptText(resolveString("%categories.detail.search.prompt"));
        } catch (Exception e) {
            searchField.setPromptText("Search books in this category...");
        }

        // Setup reset filters button
        resetFiltersButton.setOnAction(e -> resetFilters());

        // Setup reload button
        reloadButton.setOnAction(e -> reloadBooks());

        // Setup sort combo box with fallback
        try {
            sortComboBox.setItems(FXCollections.observableArrayList(
                resolveString("%categories.detail.sort.title.asc"),
                resolveString("%categories.detail.sort.title.desc"),
                resolveString("%categories.detail.sort.author.asc"),
                resolveString("%categories.detail.sort.author.desc"),
                resolveString("%categories.detail.sort.year.newest"),
                resolveString("%categories.detail.sort.year.oldest")
            ));
            sortComboBox.setValue(resolveString("%categories.detail.sort.title.asc"));
        } catch (Exception e) {
            sortComboBox.setItems(FXCollections.observableArrayList(
                "Title (A-Z)", "Title (Z-A)", "Author (A-Z)", "Author (Z-A)",
                "Year (Newest)", "Year (Oldest)"
            ));
            sortComboBox.setValue("Title (A-Z)");
        }

        // Setup books list view with optimized cell factory
        booksListView.setCellFactory(listView -> new BookListCell());
        booksListView.getStyleClass().add("categoryBooksListView");

        // 🚀 OTTIMIZZAZIONE: Virtualizzazione per performance con molti libri
        booksListView.setFixedCellSize(80); // Altezza fissa per ogni cella per ottimizzazioni

        // Imposta placeholder di default
        booksListView.setPlaceholder(new Label("Select a category to view books"));

        // Initialize localized texts
        initializeUITexts();
    }

    /**
     * Initialize UI texts with localized strings
     */
    private void initializeUITexts() {
        try {
            backButton.setText(resolveString("%categories.back.button"));
            categoryTitleLabel.setText(resolveString("%categories.title.loading"));
            statsLabel.setText(resolveString("%categories.stats.loading"));
            resetFiltersButton.setText(resolveString("%categories.reset"));
            reloadButton.setText(resolveString("%categories.refresh"));
            // Note: sort label is not an @FXML field, so it needs to be handled differently if needed
        } catch (Exception e) {
            logger.log(java.util.logging.Level.WARNING, "Error initializing UI texts, using defaults", e);
        }
    }

    /**
     * Loads books for the current category from the server.
     * Uses caching to avoid unnecessary network requests.
     * Books are loaded asynchronously to prevent UI blocking.
     */
    private void loadCategoryBooks() {
        if (currentCategory == null) return;

        // 🚀 OTTIMIZZAZIONE: Controlla cache locale prima di ricaricare
        long currentTime = System.currentTimeMillis();
        if (booksLoaded && (currentTime - lastBooksLoadTime) < CATEGORY_CACHE_DURATION) {
            logger.fine("Using cached books for category: " + currentCategory +
                        " (loaded " + ((currentTime - lastBooksLoadTime) / AppConstants.Time.MILLIS_PER_SECOND) + "s ago)");
            return; // I libri sono già caricati nella categoryBooks
        }

        // Mostra indicatore di caricamento
        Platform.runLater(() -> {
            booksListView.setPlaceholder(new Label("Caricamento libri della categoria..."));
            emptyStateContainer.setVisible(false);
        });

        // ⚡ CARICAMENTO ASINCRONO: Non bloccare il thread UI!
        long startTime = System.currentTimeMillis();
        CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, List<Book>> categoryIndex = ControllerDesign.getCategories();
                List<Book> books = categoryIndex.getOrDefault(currentCategory.toLowerCase(), new ArrayList<>());
                logger.fine("Retrieved " + books.size() + " books for category: " + currentCategory);
                return books;
            } catch (Exception e) {
                logger.log(java.util.logging.Level.SEVERE, "Error loading books for category: " + currentCategory, e);
                throw new RuntimeException(e);
            }
        }).thenAccept(books -> {
            // 🎯 AGGIORNA UI NEL THREAD JAVAFX
            Platform.runLater(() -> {
                try {
                    categoryBooks.setAll(books);

                    long totalTime = System.currentTimeMillis() - startTime;
                    logger.info("Category books loaded in " + totalTime + "ms (" + books.size() + " books for " + currentCategory + ")");

                    // Aggiorna cache
                    booksLoaded = true;
                    lastBooksLoadTime = System.currentTimeMillis();

                } catch (Exception e) {
                    logger.log(java.util.logging.Level.SEVERE, "Error updating UI with category books", e);
                    showErrorAlert("Errore nell'aggiornamento dell'interfaccia: " + e.getMessage());
                }
            });
        }).exceptionally(throwable -> {
            // ❌ GESTIONE ERRORI
            Platform.runLater(() -> {
                logger.log(java.util.logging.Level.SEVERE, "Error loading category books", throwable);
                showErrorAlert("Errore nel caricamento dei libri della categoria: " + throwable.getMessage());
                categoryBooks.clear();
                booksListView.setPlaceholder(new Label("Errore caricamento libri"));
            });
            return null;
        });
    }

    private void showErrorAlert(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            try {
                alert.setTitle(resolveString("%categories.detail.error.title"));
            } catch (Exception e) {
                alert.setTitle("Error");
            }
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private void setupSearchAndSort() {
        // Setup filtered list
        filteredBooks = new FilteredList<>(categoryBooks, p -> true);

        // Setup search functionality
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredBooks.setPredicate(book -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }

                String lowerCaseFilter = newValue.toLowerCase();
                return book.getTitle().toLowerCase().contains(lowerCaseFilter) ||
                       book.getAuthors().toLowerCase().contains(lowerCaseFilter) ||
                       (book.getDescription() != null && book.getDescription().toLowerCase().contains(lowerCaseFilter));
            });
        });

        // Setup sorted list
        sortedBooks = new SortedList<>(filteredBooks);
        sortComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            sortedBooks.setComparator(getComparatorForSortOption(newVal));
        });
        // Set initial comparator
        if (sortComboBox.getValue() != null) {
            sortedBooks.setComparator(getComparatorForSortOption(sortComboBox.getValue()));
        }

        // Bind to list view
        booksListView.setItems(sortedBooks);

        // Update empty state when list changes
        sortedBooks.addListener((javafx.collections.ListChangeListener<Book>) c -> {
            updateEmptyState();
            updateUI();
        });
    }

    private Comparator<Book> getComparatorForSortOption(String sortOption) {
        if (sortOption.equals(resolveString("%categories.detail.sort.title.asc"))) {
            return Comparator.comparing(Book::getTitle, String.CASE_INSENSITIVE_ORDER);
        } else if (sortOption.equals(resolveString("%categories.detail.sort.title.desc"))) {
            return Comparator.comparing(Book::getTitle, String.CASE_INSENSITIVE_ORDER).reversed();
        } else if (sortOption.equals(resolveString("%categories.detail.sort.author.asc"))) {
            return Comparator.comparing(Book::getAuthors, String.CASE_INSENSITIVE_ORDER);
        } else if (sortOption.equals(resolveString("%categories.detail.sort.author.desc"))) {
            return Comparator.comparing(Book::getAuthors, String.CASE_INSENSITIVE_ORDER).reversed();
        } else if (sortOption.equals(resolveString("%categories.detail.sort.year.newest"))) {
            return Comparator.comparing(Book::getPublish_date_year, Comparator.reverseOrder());
        } else if (sortOption.equals(resolveString("%categories.detail.sort.year.oldest"))) {
            return Comparator.comparing(Book::getPublish_date_year);
        } else {
            return Comparator.comparing(Book::getTitle, String.CASE_INSENSITIVE_ORDER);
        }
    }

    private void updateUI() {
        if (currentCategory == null) return;

        // Update title
        categoryTitleLabel.setText(capitalizeFirstLetter(currentCategory));

        // Update stats
        int totalBooks = categoryBooks.size();
        int visibleBooks = sortedBooks.size();

        String statsText;
        try {
            statsText = String.format("%d %s", totalBooks, resolveString("%categories.detail.stats.total"));
            if (visibleBooks != totalBooks) {
                statsText += String.format(" • %d %s", visibleBooks, resolveString("%categories.detail.stats.shown"));
            }
        } catch (Exception e) {
            statsText = String.format("%d books total", totalBooks);
            if (visibleBooks != totalBooks) {
                statsText += String.format(" • %d shown", visibleBooks);
            }
        }
        statsLabel.setText(statsText);

        updateEmptyState();
    }

    private void updateEmptyState() {
        boolean hasBooks = !sortedBooks.isEmpty();
        booksListView.setVisible(hasBooks);
        emptyStateContainer.setVisible(!hasBooks);
    }

    // Helper methods
    private String capitalizeFirstLetter(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }

    private Label createStyledLabel(String text, String style) {
        Label label = new Label(text);
        if (style != null) {
            label.setStyle(style);
        }
        return label;
    }

    private Button createStyledButton(String text, String style) {
        Button button = new Button(text);
        if (style != null) {
            button.setStyle(style);
        }
        return button;
    }

    private void resetFilters() {
        // Clear search field and reset sorting
        searchField.clear();
        if (sortComboBox.getItems().size() > 0) {
            sortComboBox.setValue(sortComboBox.getItems().get(0)); // Reset to first sort option
        }
        logger.info("Filters reset for category: " + currentCategory);
    }

    private void reloadBooks() {
        // Forza il ricaricamento ignorando la cache
        if (currentCategory != null) {
            logger.info("Forcing books reload for category: " + currentCategory);
            booksLoaded = false;
            lastBooksLoadTime = 0;
            loadCategoryBooks();
        }
    }

    private void goBackToCategories() {
        // Navigate back to categories view
        Map<String, Object> params = new HashMap<>();
        Navigator.goTo(EveryView.CATEGORIES_VIEW.getPath(), params);
    }

    /**
     * Custom ListCell implementation for displaying books in the category detail view.
     * Each cell shows book information including title, author, year, description,
     * and action buttons for viewing details or adding to library.
     */
    private class BookListCell extends ListCell<Book> {
        private final VBox content;
        private final Label titleLabel;
        private final Label authorLabel;
        private final Label yearLabel;
        private final Label descriptionLabel;
        private Button viewDetailsButton;
        private Button addToLibraryButton;

        /**
         * Creates a new book list cell with all UI components initialized.
         * Sets up the layout, labels, and action buttons for the cell.
         */
        public BookListCell() {
            super();

            content = new VBox(5);
            content.setPadding(new Insets(10));
            content.getStyleClass().add("categoryBookCard");

            titleLabel = new Label();
            titleLabel.getStyleClass().add("categoryBookTitle");
            titleLabel.setWrapText(true);

            authorLabel = new Label();
            authorLabel.getStyleClass().add("categoryBookAuthor");

            yearLabel = new Label();
            yearLabel.getStyleClass().add("categoryBookYear");

            descriptionLabel = new Label();
            descriptionLabel.getStyleClass().add("categoryBookDescription");
            descriptionLabel.setWrapText(true);
            descriptionLabel.setMaxHeight(DESCRIPTION_MAX_HEIGHT);

            try {
                viewDetailsButton = createStyledButton(resolveString("%categories.detail.view.details"), null);
            } catch (Exception e) {
                viewDetailsButton = createStyledButton("View Details", null);
            }
            if (viewDetailsButton != null) {
                viewDetailsButton.getStyleClass().add("categoryBookButton");
                viewDetailsButton.setOnAction(e -> {
                    Book book = getItem();
                    if (book != null) {
                        viewBookDetails(book);
                    }
                });
            }

            try {
                addToLibraryButton = createStyledButton(resolveString("%categories.detail.add.to.library"), null);
            } catch (Exception e) {
                addToLibraryButton = createStyledButton("Add to Library", null);
            }
            if (addToLibraryButton != null) {
                addToLibraryButton.getStyleClass().add("categoryBookButton");
                addToLibraryButton.setOnAction(e -> {
                    Book book = getItem();
                    if (book != null) {
                        addBookToLibrary(book);
                    }
                });
            }

            HBox buttonBox = new HBox(5);
            buttonBox.setAlignment(Pos.CENTER_RIGHT);
            if (viewDetailsButton != null) {
                buttonBox.getChildren().add(viewDetailsButton);
            }
            if (addToLibraryButton != null) {
                buttonBox.getChildren().add(addToLibraryButton);
            }

            content.getChildren().addAll(titleLabel, authorLabel, yearLabel, descriptionLabel, buttonBox);
            VBox.setVgrow(descriptionLabel, Priority.ALWAYS);
        }

        @Override
        protected void updateItem(Book book, boolean empty) {
            super.updateItem(book, empty);

            if (empty || book == null) {
                setGraphic(null);
            } else {
                titleLabel.setText(book.getTitle());
                authorLabel.setText(resolveString("%categories.detail.book.by") + " " + book.getAuthors());

                String yearText;
                try {
                    yearText = book.getPublish_date_year() > 0 ?
                        String.valueOf(book.getPublish_date_year()) : resolveString("%categories.detail.book.unknown.year");
                } catch (Exception e) {
                    yearText = book.getPublish_date_year() > 0 ?
                        String.valueOf(book.getPublish_date_year()) : "Unknown year";
                }
                yearLabel.setText(yearText);

                String description = book.getDescription();
                if (description != null && !description.trim().isEmpty()) {
                    descriptionLabel.setText(description.length() > 150 ?
                        description.substring(0, 147) + "..." : description);
                } else {
                    try {
                        descriptionLabel.setText(resolveString("%categories.detail.book.no.description"));
                    } catch (Exception e) {
                        descriptionLabel.setText("No description available");
                    }
                }

                setGraphic(content);
            }
        }

        private void addBookToLibrary(Book book) {
            // Check if book is already in any user library by querying the server
            ThreadPoolManager.executeIO(() -> {
                try {
                    String username = it.uninsubria.client.utils.classesUI.SessionManager.getInstance().getUsername();
                    if (username == null) {
                        Platform.runLater(() -> CategoryDetailController.this.showErrorAlert("Devi effettuare il login per aggiungere libri alle librerie."));
                        return null;
                    }

                    // Get fresh data from server
                    List<Library> userLibraries = ServiceLocator.getLibraryService().getUserLibraries(username);

                    Platform.runLater(() -> {
                        if (userLibraries.isEmpty()) {
                            Platform.runLater(() -> {
                                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                                alert.setTitle(resolveString("%categories.detail.no.library.title"));
                                alert.setHeaderText(resolveString("%categories.detail.no.library.header"));
                                alert.setContentText(resolveString("%categories.detail.no.library.message"));
                                alert.showAndWait();
                            });
                            return;
                        }

                        // Check if book is already in any library
                        final Library[] existingLibrary = {null};
                        for (Library library : userLibraries) {
                            if (library.getLibriDisponibili() != null &&
                                library.getLibriDisponibili().stream()
                                      .anyMatch(b -> b.getBook_id() == book.getBook_id())) {
                                existingLibrary[0] = library;
                                break;
                            }
                        }

                        if (existingLibrary[0] != null) {
                            Platform.runLater(() -> {
                                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                                alert.setTitle(resolveString("%categories.detail.book.already.exists.title"));
                                alert.setHeaderText(resolveString("%categories.detail.book.already.exists.header"));
                                alert.setContentText(java.text.MessageFormat.format(resolveString("%categories.detail.book.already.exists.message"), book.getTitle()));
                                alert.showAndWait();
                            });
                            return;
                        }

                        // Create dialog to select libraries
                        showLibrarySelectionDialog(book, userLibraries);
                    });

                } catch (Exception e) {
                    Platform.runLater(() -> showErrorAlert("Errore nel caricamento delle librerie: " + e.getMessage()));
                }
                return null;
            });
        }

        private void showLibrarySelectionDialog(Book book, List<Library> userLibraries) {
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle(resolveString("%categories.detail.add.book.dialog.title"));

            VBox dialogContent = new VBox(10);
            dialogContent.setPadding(new Insets(20));

            Label instructionLabel = createStyledLabel("Seleziona la libreria a cui aggiungere '" + book.getTitle() + "':", null);
            instructionLabel.setStyle("-fx-font-weight: bold;");

            VBox librariesBox = new VBox(5);

            ToggleGroup libraryGroup = new ToggleGroup();

            for (Library library : userLibraries) {
                RadioButton radioButton = new RadioButton(library.getName());
                radioButton.setToggleGroup(libraryGroup);
                radioButton.setUserData(library);
                librariesBox.getChildren().add(radioButton);
            }

            Button addButton = createStyledButton("Aggiungi", null);
            addButton.getStyleClass().add("categoryBookButton");
            addButton.setDisable(true); // Initially disabled

            // Enable button when a library is selected
            libraryGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
                addButton.setDisable(newToggle == null);
            });

            addButton.setOnAction(e -> {
                RadioButton selectedRadio = (RadioButton) libraryGroup.getSelectedToggle();
                if (selectedRadio == null) return;

                Library selectedLibrary = (Library) selectedRadio.getUserData();
                String username = it.uninsubria.client.utils.classesUI.SessionManager.getInstance().getUsername();

                // Add the book to the selected library
                List<Book> updatedBooks = new ArrayList<>(selectedLibrary.getLibriDisponibili() != null ?
                                                         selectedLibrary.getLibriDisponibili() : new ArrayList<>());
                updatedBooks.add(book);
                selectedLibrary.setLibriDisponibili(updatedBooks);

                // Update library in background
                ThreadPoolManager.executeIO(() -> {
                    try {
                        ServiceLocator.getLibraryService().updateLibraryForUser(username, selectedLibrary, selectedLibrary);
                        Platform.runLater(() -> {
                            Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                            successAlert.setTitle(resolveString("%categories.detail.book.added.title"));
                            successAlert.setHeaderText(null);
                            successAlert.setContentText(String.format(resolveString("%categories.detail.book.added.message"), selectedLibrary.getName()));
                            successAlert.showAndWait();
                            dialog.close();
                        });
                     } catch (Exception ex) {
                         Platform.runLater(() -> {
                             CategoryDetailController.this.showErrorAlert("Errore durante l'aggiunta del libro: " + ex.getMessage());
                         });
                     }
                    return null;
                });
            });

            Button cancelButton = new Button("Annulla");
            cancelButton.setOnAction(e -> dialog.close());

            HBox buttonBox = new HBox(10, addButton, cancelButton);
            buttonBox.setAlignment(Pos.CENTER_RIGHT);

            dialogContent.getChildren().addAll(instructionLabel, librariesBox, buttonBox);

            ScrollPane scrollPane = new ScrollPane(dialogContent);
            scrollPane.setFitToWidth(true);

            Scene scene = new Scene(scrollPane, DIALOG_WIDTH, DIALOG_HEIGHT);
            dialog.setScene(scene);
            dialog.showAndWait();
        }

        private void viewBookDetails(Book book) {
            try {
                // Navigate to book detail view
                Map<String, Object> params = new HashMap<>();
                params.put("book", book);
                Navigator.goTo(EveryView.BOOK_DETAIL_SIMPLE.getPath(), params);
                 } catch (Exception e) {
                     CategoryDetailController.this.showErrorAlert("Impossibile visualizzare i dettagli del libro: " + e.getMessage());
                 }
         }

    }

    /**
     * Setup minimal UI when full initialization fails due to missing resources
     */
    private void setupMinimalUI() {
        try {
            // Setup back button with minimal configuration
            FontIcon backIcon = new FontIcon("fas-arrow-left");
            backIcon.setIconSize(16);
            backButton.setGraphic(backIcon);
            backButton.setOnAction(e -> goBackToCategories());

            // Setup basic search field
            searchField.setPromptText("Search books...");

            // Setup books list view with basic placeholder
            booksListView.setCellFactory(listView -> new BookListCell());
            booksListView.setPlaceholder(new Label("Select a category to view books"));

            // Setup sort combo with basic options
            sortComboBox.setItems(FXCollections.observableArrayList(
                "Title (A-Z)", "Title (Z-A)", "Author (A-Z)", "Author (Z-A)",
                "Year (Newest)", "Year (Oldest)"
            ));
            sortComboBox.setValue("Title (A-Z)");

            // Setup reload and reset buttons
            reloadButton.setOnAction(e -> reloadBooks());
            resetFiltersButton.setOnAction(e -> resetFilters());

        } catch (Exception e) {
            logger.log(java.util.logging.Level.SEVERE, "Error in setupMinimalUI", e);
        }
    }

    /**
     * Update UI elements that depend on the current category
     */
    private void updateUIWithCategory() {
        if (currentCategory != null) {
            // Update category title
            try {
                categoryTitleLabel.setText(resolveString("%categories.detail.title") + " " + currentCategory);
            } catch (Exception e) {
                categoryTitleLabel.setText("Category: " + currentCategory);
            }

            // Update search prompt with localized text if available
            try {
                searchField.setPromptText(resolveString("%categories.detail.search.prompt"));
            } catch (Exception e) {
                // Keep default prompt if localization fails
            }

            // Update sort options with localized text if available
            try {
                sortComboBox.setItems(FXCollections.observableArrayList(
                    resolveString("%categories.detail.sort.title.asc"),
                    resolveString("%categories.detail.sort.title.desc"),
                    resolveString("%categories.detail.sort.author.asc"),
                    resolveString("%categories.detail.sort.author.desc"),
                    resolveString("%categories.detail.sort.year.newest"),
                    resolveString("%categories.detail.sort.year.oldest")
                ));
                sortComboBox.setValue(resolveString("%categories.detail.sort.title.asc"));
            } catch (Exception e) {
                // Keep default sort options if localization fails
            }
        }
    }
}