package it.uninsubria.client.controller.homepage.libreria;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import it.uninsubria.client.controller.ControllerBase;
import it.uninsubria.client.utils.classesUI.EveryView;
import it.uninsubria.client.utils.classesUI.LanguageManager;
import it.uninsubria.client.utils.classesUI.ThemeManager;
import it.uninsubria.client.utils.classesUI.ServiceLocator;
import it.uninsubria.client.utils.classesUI.ResourceCache;
import it.uninsubria.client.utils.classesUI.SessionManager;
import it.uninsubria.client.utils.classesUI.ThreadPoolManager;
import it.uninsubria.client.utils.classesUI.NotificationManager;
import it.uninsubria.shared.model.Book;
import it.uninsubria.shared.model.Library;
import it.uninsubria.shared.utils.LoggerUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.control.ComboBox;
import javafx.collections.FXCollections;

public class LibraryListController extends ControllerBase {

    // CSS Class names for theme-aware styling
    private static final String LIBRARY_CARD_BASE_CLASS = "library-card-base";
    private static final String LIBRARY_CARD_HOVER_CLASS = "library-card-hover";
    private static final String BOOK_CARD_BASE_CLASS = "book-card-base";
    private static final String BOOK_CARD_HOVER_CLASS = "book-card-hover";

    // CSS Class names for buttons and text - theme-aware
    private static final String BUTTON_PRIMARY_STYLE_CLASS = "libraryButtonPrimary";
    private static final String BUTTON_WARNING_STYLE_CLASS = "libraryButtonWarning";
    private static final String TEXT_PRIMARY_STYLE_CLASS = "libraryTextPrimary";
    private static final String TEXT_SECONDARY_STYLE_CLASS = "libraryTextSecondary";
    private static final String TEXT_MUTED_STYLE_CLASS = "libraryTextMuted";
    private static final String TEXT_BOOK_TITLE_STYLE_CLASS = "libraryTextBookTitle";
    private static final String TEXT_EMPTY_STYLE_CLASS = "libraryTextEmpty";
    private static final String TEXT_EMPTY_BOOKS_STYLE_CLASS = "libraryTextEmptyBooks";
    private static final String IMAGE_SHADOW_STYLE_CLASS = "libraryImageShadow";

    // Dimensions
    private static final double CARD_WIDTH = 450;
    private static final double CARD_MAX_WIDTH = 450;
    private static final double BOOK_CARD_WIDTH = 150;
    private static final double BOOK_CARD_HEIGHT = 200;
    private static final double BOOK_IMAGE_WIDTH = 120;
    private static final double BOOK_IMAGE_HEIGHT = 150;
    private static final double LIBRARY_IMAGE_SIZE = 50;
    private static final double BOOK_TITLE_MAX_WIDTH = 120;

    @FXML private VBox libraryContainer;
    @FXML private Button createNewLibrary;
    @FXML private Button removeOldLibrary;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterComboBox;
    @FXML private Label totalLibrariesLabel;
    @FXML private Label totalBooksLabel;
    @FXML private Label emptyLibrariesLabel;
    @FXML private Label avgBooksLabel;
    @FXML private HBox recentlyOpenedContainer;

    private final List<Library> libraries = new ArrayList<>();
    private final List<Library> filteredLibraries = new ArrayList<>();
    private static final Logger logger = Logger.getLogger(LibraryListController.class.getName());

    private String getBackupFileName() {
        String username = SessionManager.getInstance().getUsername();
        return username != null ? "libraries_backup_" + username + ".json" : "libraries_backup.json";
    }

    public void refreshLibraries() {
        LoggerUtil.debug(logger, "Refreshing libraries from database");
        try {
            loadLibrariesFromDatabase();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error during refreshLibraries: " + e.getMessage(), e);
            // Fallback: almeno aggiorna l'UI con i dati correnti
            Platform.runLater(() -> {
                applyFilters(searchField != null ? searchField.getText() : "",
                           filterComboBox != null ? filterComboBox.getValue() : resolveString("%library.filter.all"));
                updateLibraryStats();
                renderLibraries();
            });
        }
    }

    /**
     * Forza un refresh completo delle librerie dal database
     * Utile quando si sospetta che l'UI non sia sincronizzata
     */
    public void forceFullRefresh() {
        LoggerUtil.debug(logger, "Forcing full refresh from database");
        // Azzera la cache locale prima di ricaricare
        libraries.clear();
        filteredLibraries.clear();
        // Ricarica tutto dal database
        refreshLibraries();
    }

    /**
     * Metodo pubblico per richiedere un refresh dei dati
     * Chiamato da CreateLibraryController quando necessario
     */
    public void requestDataRefresh() {
        LoggerUtil.debug(logger, "Data refresh requested by CreateLibraryController");
        refreshLibraries();
    }

       public void initialize() {
            LoggerUtil.debug(logger, "Initializing, loading libraries from database");

           if(createNewLibrary != null) {
               createNewLibrary.setOnAction(e -> openCreateLibraryWindow(null));
           }
           if (removeOldLibrary != null) {
               removeOldLibrary.setOnAction(e -> removeSelectedLibrary());
           }

           // Configura ricerca e filtri
           setupSearchAndFilters();

           // Always load fresh data from database on initialization
           loadLibrariesFromDatabase();

           // Carica i libri recentemente aperti
           loadRecentlyOpenedBooks();
       }

    private void loadLibrariesFromDatabase(){
        String username = SessionManager.getInstance().getUsername();
        LoggerUtil.debug(logger, "Loading libraries for user: " + username);

        if(username == null){
            LoggerUtil.debug(logger, "Username is null, returning");
            return;
        }

        ThreadPoolManager.executeIOWithFeedback(() -> {
            LoggerUtil.debug(logger, "Calling getUserLibraries for username: " + username);
            List<Library> dbLibraries = ServiceLocator.getLibraryService().getUserLibraries(username);
            LoggerUtil.debug(logger, "Retrieved " + dbLibraries.size() + " libraries from database");

            for(Library lib : dbLibraries){
                LoggerUtil.debug(logger, "Processing library: " + lib.getName() + " (ID: " + lib.getId() + ")");
                List<Integer> bookIds = ServiceLocator.getLibraryService().getBooksInLibrariesImpl(lib.getId());
                LoggerUtil.debug(logger, "Library " + lib.getName() + " has " + bookIds.size() + " book IDs");

                List<Book> books = new ArrayList<>();
                for(Integer bookId : bookIds){
                    Book b = ServiceLocator.getBookService().getBookDetails(bookId);
                    if(b != null){
                        books.add(b);
                    } else {
                        LoggerUtil.debug(logger, "Book with ID " + bookId + " not found");
                    }
                }
                lib.setLibriDisponibili(books);
                LoggerUtil.debug(logger, "Library " + lib.getName() + " now has " + books.size() + " books loaded");
            }

            Platform.runLater(() -> {
                // Sincronizza locale con database
                synchronizeWithDatabase(dbLibraries);
            });

            return null;
        }, "Caricamento librerie in corso...", "Librerie caricate con successo");
    }

    private void synchronizeWithDatabase(List<Library> dbLibraries) {
        LoggerUtil.debug(logger, "Synchronizing local libraries with database - Local: " + libraries.size() + ", Database: " + dbLibraries.size());

        // Crea mappe per confronto efficiente
        Map<Integer, Library> localMap = libraries.stream()
            .collect(java.util.stream.Collectors.toMap(Library::getId, lib -> lib));
        Map<Integer, Library> dbMap = dbLibraries.stream()
            .collect(java.util.stream.Collectors.toMap(Library::getId, lib -> lib));

        // Trova librerie da aggiungere (presenti in DB ma non localmente)
        List<Library> toAdd = dbLibraries.stream()
            .filter(lib -> !localMap.containsKey(lib.getId()))
            .collect(java.util.stream.Collectors.toList());

        // Trova librerie da rimuovere (presenti localmente ma non in DB)
        List<Library> toRemove = libraries.stream()
            .filter(lib -> !dbMap.containsKey(lib.getId()))
            .collect(java.util.stream.Collectors.toList());

        LoggerUtil.debug(logger, "Libraries to add: " + toAdd.size() + ", to remove: " + toRemove.size());

        // Applica sincronizzazione
        libraries.removeAll(toRemove);
        libraries.addAll(toAdd);

        // Se il database è vuoto ma abbiamo un backup, tenta di ripristinare
        if (libraries.isEmpty() && dbLibraries.isEmpty()) {
            LoggerUtil.debug(logger, "Database is empty, attempting to restore from backup");
            restoreLibrariesFromFile();
        }

        // Fai backup delle librerie correnti
        if (!libraries.isEmpty()) {
            backupLibrariesToFile();
        }

        // Aggiorna filteredLibraries
        filteredLibraries.clear();
        filteredLibraries.addAll(libraries);

        updateLibraryStats();
        renderLibraries();

        LoggerUtil.debug(logger, "Synchronization complete. Total libraries: " + libraries.size());
    }

    private void backupLibrariesToFile() {
        try {
            // Crea un backup JSON semplice delle librerie
            StringBuilder json = new StringBuilder();
            json.append("[\n");
            for (int i = 0; i < libraries.size(); i++) {
                Library lib = libraries.get(i);
                json.append("  {\n");
                json.append("    \"name\": \"").append(lib.getName().replace("\"", "\\\"")).append("\",\n");
                json.append("    \"books\": [\n");
                List<Book> books = lib.getLibriDisponibili();
                if (books != null) {
                    for (int j = 0; j < books.size(); j++) {
                        Book book = books.get(j);
                        json.append("      {\n");
                        json.append("        \"id\": ").append(book.getBook_id()).append(",\n");
                        json.append("        \"title\": \"").append(book.getTitle().replace("\"", "\\\"")).append("\"\n");
                        json.append("      }");
                        if (j < books.size() - 1) json.append(",");
                        json.append("\n");
                    }
                }
                json.append("    ]\n");
                json.append("  }");
                if (i < libraries.size() - 1) json.append(",");
                json.append("\n");
            }
            json.append("]\n");

            String backupFileName = getBackupFileName();
            Files.write(Paths.get(backupFileName), json.toString().getBytes());
            logger.fine("Libraries backed up to " + backupFileName);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error backing up libraries: " + e.getMessage(), e);
        }
    }

    private void restoreLibrariesFromFile() {
        try {
            Path backupPath = Paths.get(getBackupFileName());
            if (!Files.exists(backupPath)) {
                logger.fine("No backup file found for user");
                return;
            }
            
            LoggerUtil.debug(logger, "Found backup file for user, attempting to restore libraries");

            // Per ora, solo log - implementazione completa del parsing JSON sarebbe complessa
            // In una implementazione completa, parseremmo il JSON e ricreeremmo le librerie

            LoggerUtil.debug(logger, "Backup file exists but restore not implemented yet");

        } catch (Exception e) {
            logger.log(Level.WARNING, "Error restoring libraries from backup: " + e.getMessage(), e);
        }
    }

    private void setupSearchAndFilters() {
        if (filterComboBox != null) {
            filterComboBox.setItems(FXCollections.observableArrayList(
                resolveString("%library.filter.all"),
                resolveString("%library.filter.with.books"),
                resolveString("%library.filter.empty"),
                resolveString("%library.filter.recent")
            ));
            filterComboBox.setValue(resolveString("%library.filter.all"));
        }

        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldText, newText) -> {
                applyFilters(newText, filterComboBox.getValue());
            });
        }

        if (filterComboBox != null) {
            filterComboBox.setItems(FXCollections.observableArrayList(
                resolveString("%library.filter.all"),
                resolveString("%library.filter.with.books"),
                resolveString("%library.filter.empty"),
                resolveString("%library.filter.recent")
            ));
            filterComboBox.setValue(resolveString("%library.filter.all"));
        }
    }

    private void applyFilters(String searchText, String filterType) {
        filteredLibraries.clear();

        for (Library lib : libraries) {
            boolean matchesSearch = searchText == null || searchText.trim().isEmpty() ||
                lib.getName().toLowerCase().contains(searchText.toLowerCase());

            boolean matchesFilter;
            if (filterType.equals(resolveString("%library.filter.with.books"))) {
                matchesFilter = lib.getLibriDisponibili() != null && !lib.getLibriDisponibili().isEmpty();
            } else if (filterType.equals(resolveString("%library.filter.empty"))) {
                matchesFilter = lib.getLibriDisponibili() == null || lib.getLibriDisponibili().isEmpty();
            } else if (filterType.equals(resolveString("%library.filter.recent"))) {
                matchesFilter = true; // Per ora tutte, poi possiamo aggiungere logica per data creazione
            } else {
                matchesFilter = true; // "Tutte"
            }

            if (matchesSearch && matchesFilter) {
                filteredLibraries.add(lib);
            }
        }

        renderLibraries();
    }

    private void renderLibraries() {
        LoggerUtil.debug(logger, "Rendering " + filteredLibraries.size() + " libraries");

        libraryContainer.getChildren().clear();
        libraryContainer.setStyle("-fx-background-color: transparent;");

        if (filteredLibraries.isEmpty()) {
            LoggerUtil.debug(logger, "No libraries to render, showing empty message");
            Label empty = new Label(resolveString("%library.nolibrary.created"));
            empty.getStyleClass().add(TEXT_EMPTY_STYLE_CLASS);
            libraryContainer.getChildren().add(empty);
            return;
        }

        for (Library lib : filteredLibraries) {
            LoggerUtil.debug(logger, "Rendering library: " + lib.getName());

            // Card principale con design migliorato
            VBox card = new VBox(8);
            card.setPadding(new Insets(12));
            card.getStyleClass().add(LIBRARY_CARD_BASE_CLASS);
            card.setPrefWidth(CARD_WIDTH);
            card.setMaxWidth(CARD_MAX_WIDTH);

            // Header con immagine e titolo
            HBox header = new HBox(12);
            header.setAlignment(Pos.CENTER_LEFT);

            // Immagine libreria (placeholder migliorato)
            ImageView img = new ImageView(ResourceCache.getImage(EveryView.TEST_IMAGE.getPath()));
            img.setFitWidth(LIBRARY_IMAGE_SIZE);
            img.setFitHeight(LIBRARY_IMAGE_SIZE);
            img.getStyleClass().add(IMAGE_SHADOW_STYLE_CLASS);

            // Contenitore titolo e statistiche
            VBox titleStats = new VBox(4);
            titleStats.setAlignment(Pos.CENTER_LEFT);
            titleStats.setStyle("-fx-background-color: transparent;");

            Label name = new Label(lib.getName());
            name.getStyleClass().add(TEXT_PRIMARY_STYLE_CLASS);

            // Statistiche libreria
            int bookCount = lib.getLibriDisponibili() != null ? lib.getLibriDisponibili().size() : 0;
            Label stats = new Label(java.text.MessageFormat.format(resolveString("%library.stats.books"), bookCount));
            stats.getStyleClass().add(TEXT_SECONDARY_STYLE_CLASS);

            titleStats.getChildren().addAll(name, stats);

            header.getChildren().addAll(img, titleStats);

            // Azioni libreria
            HBox actions = new HBox(8);
            actions.setAlignment(Pos.CENTER_LEFT);

            Button open = new Button(resolveString("%library.button.open"));
            open.getStyleClass().add(BUTTON_PRIMARY_STYLE_CLASS);
            open.setOnAction(e -> openLibrary(lib));

            Button edit = new Button(resolveString("%library.button.edit"));
            edit.getStyleClass().add(BUTTON_WARNING_STYLE_CLASS);
            edit.setOnAction(e -> openCreateLibraryWindow(lib));

            CheckBox selectCheckBox = new CheckBox(resolveString("%library.checkbox.select"));
            selectCheckBox.setOnAction(e -> lib.setSelected(selectCheckBox.isSelected()));

            actions.getChildren().addAll(open, edit, selectCheckBox);

            // Aggiungere hover effect con classi CSS
            card.setOnMouseEntered(e -> {
                card.getStyleClass().remove(LIBRARY_CARD_BASE_CLASS);
                card.getStyleClass().add(LIBRARY_CARD_HOVER_CLASS);
            });
            card.setOnMouseExited(e -> {
                card.getStyleClass().remove(LIBRARY_CARD_HOVER_CLASS);
                card.getStyleClass().add(LIBRARY_CARD_BASE_CLASS);
            });

            card.getChildren().addAll(header, actions);
            libraryContainer.getChildren().add(card);
        }
    }

    private void updateLibraryStats() {
        int totalLibraries = libraries.size();
        int totalBooks = libraries.stream()
            .mapToInt(lib -> lib.getLibriDisponibili() != null ? lib.getLibriDisponibili().size() : 0)
            .sum();
        int emptyLibraries = (int) libraries.stream()
            .filter(lib -> lib.getLibriDisponibili() == null || lib.getLibriDisponibili().isEmpty())
            .count();
        double avgBooks = totalLibraries > 0 ? (double) totalBooks / totalLibraries : 0.0;

        // Aggiorna le label
        if (totalLibrariesLabel != null) {
            totalLibrariesLabel.setText(resolveString("%library.list.total.libraries") + " " + totalLibraries);
        }
        if (totalBooksLabel != null) {
            totalBooksLabel.setText(resolveString("%library.list.total.books") + " " + totalBooks);
        }
        if (emptyLibrariesLabel != null) {
            emptyLibrariesLabel.setText(resolveString("%library.list.empty.libraries") + " " + emptyLibraries);
        }
        if (avgBooksLabel != null) {
            avgBooksLabel.setText(resolveString("%library.list.average.books") + " " + String.format("%.1f", avgBooks));
        }
    }

    

    private void removeSelectedLibrary(){
        List<Library> toRemove = libraries.stream()
                                        .filter(Library::isSelected)
                                        .collect(Collectors.toList());

        if(toRemove.isEmpty()){
            return;
        }

        ThreadPoolManager.executeCritical(() -> {
            try {
                String username = SessionManager.getInstance().getUsername();
                if (username == null){
                    return;
                }

                // Prima rimuovi dal database
                LoggerUtil.debug(logger, "Starting deletion of " + toRemove.size() + " libraries");
                for (Library lib : toRemove) {
                    LoggerUtil.debug(logger, "Deleting library: " + lib.getName() + " (ID: " + lib.getId() + ")");
                    ServiceLocator.getLibraryService().deleteLibraryForUser(username, lib);
                    LoggerUtil.debug(logger, "Successfully deleted library: " + lib.getName());
                }

                // Solo se la rimozione dal database è andata a buon fine, aggiorna la UI
                Platform.runLater(() -> {
                    libraries.removeAll(toRemove);
                    // Applica i filtri correnti per aggiornare filteredLibraries
                    String currentSearch = searchField != null ? searchField.getText() : "";
                    String currentFilter = filterComboBox != null ? filterComboBox.getValue() : resolveString("%library.filter.all");
                    applyFilters(currentSearch, currentFilter);
                    updateLibraryStats();

                    // Forza un refresh dal database per assicurarsi che tutti i dati siano sincronizzati
                    // I libri eliminati dalle librerie dovrebbero ora essere disponibili altrove
                    LoggerUtil.debug(logger, "Forcing database refresh after library deletion");
                    refreshLibraries();

                    LoggerUtil.debug(logger, "UI updated after deletion, remaining libraries: " + libraries.size());
                });

            } catch (Exception e) {
                System.err.println("Errore durante la rimozione dal database: " + e.getMessage());
                logger.log(Level.SEVERE, "Errore durante la rimozione dal database", e);
                Platform.runLater(() -> {
                    // In caso di errore, ricarica i dati dal database per assicurarsi che la UI sia sincronizzata
                    loadLibrariesFromDatabase();
                });
            }
        });

        NotificationManager.showInfo("Rimozione librerie in corso...");
    }

    private void openCreateLibraryWindow(Library libToEdit) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(EveryView.CREATE_LIBRARY_VIEW.getPath()), LanguageManager.getBundle());
            Parent root = loader.load();

            CreateLibraryController controller = loader.getController();
            controller.setParentController(this);
            controller.setLibraryToEdit(libToEdit);
            controller.onWindowOpened(); // Ensure fresh library data is loaded

            Stage stage = new Stage();
            Scene libraryScene = new Scene(root);
            ThemeManager.applyTheme(libraryScene); // Applica il tema corrente (light/dark mode)
            stage.setScene(libraryScene);
            stage.setTitle(libToEdit == null ? resolveString("%library.create.title") : resolveString("%library.edit.title"));
            stage.show();

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error opening create/edit library window", e);
        }
    }

    private void openLibrary(Library lib) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(EveryView.LIBRARY_DETAIL_VIEW.getPath()),LanguageManager.getBundle());
            Parent detailRoot = loader.load();

            LibraryDetailController controller = loader.getController();
            controller.setParentControllerOpen(this);
            controller.setLibrary(lib); // 🔹 Passa la libreria selezionata

            // Sostituisci il contenuto nel container principale
            Stage stage = new Stage();
            stage.setScene(new Scene(detailRoot));
            stage.show();

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error opening library detail view", e);
        }
        // Qui in futuro puoi caricare la vista con i libri della libreria
        // es. Navigator.loadView(EveryView.LIBRARY_DETAILS_VIEW.getPath(), lib);
    }

    private void loadRecentlyOpenedBooks() {
        if (recentlyOpenedContainer == null) return;

        List<Book> recentlyOpenedBooks = SessionManager.getInstance().getRecentlyOpenedBooks();
        renderRecentlyOpenedBooks(recentlyOpenedBooks);
    }

    private void renderRecentlyOpenedBooks(List<Book> books) {
        if (recentlyOpenedContainer == null) return;

        recentlyOpenedContainer.getChildren().clear();

        if (books.isEmpty()) {
            // Mostra messaggio quando non ci sono libri recenti
            Label noRecentBooks = new Label(resolveString("%library.nobooks.opened"));
            noRecentBooks.getStyleClass().add(TEXT_EMPTY_BOOKS_STYLE_CLASS);
            recentlyOpenedContainer.getChildren().add(noRecentBooks);
            return;
        }

        // Mostra fino a 4 libri più recenti
        int maxBooks = Math.min(books.size(), 4);
        for (int i = 0; i < maxBooks; i++) {
            Book book = books.get(i);
            VBox bookCard = createRecentlyOpenedBookCard(book);
            recentlyOpenedContainer.getChildren().add(bookCard);
        }
    }

    private VBox createRecentlyOpenedBookCard(Book book) {
        VBox card = new VBox(8);
        card.setPrefWidth(BOOK_CARD_WIDTH);
        card.setPrefHeight(BOOK_CARD_HEIGHT);
        card.setAlignment(Pos.TOP_CENTER);
        card.getStyleClass().add(BOOK_CARD_BASE_CLASS);

        // Immagine del libro (placeholder per ora)
        ImageView bookImage = new ImageView(ResourceCache.getImage(EveryView.TEST_IMAGE.getPath()));
        bookImage.setFitWidth(BOOK_IMAGE_WIDTH);
        bookImage.setFitHeight(BOOK_IMAGE_HEIGHT);
        bookImage.setPreserveRatio(true);
        bookImage.getStyleClass().add(IMAGE_SHADOW_STYLE_CLASS);

        // Titolo del libro (troncato se troppo lungo)
        Label titleLabel = new Label();
        String title = book.getTitle();
        if (title.length() > 20) {
            title = title.substring(0, 17) + "...";
        }
        titleLabel.setText(title);
        titleLabel.getStyleClass().add(TEXT_BOOK_TITLE_STYLE_CLASS);
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(BOOK_TITLE_MAX_WIDTH);
        titleLabel.setAlignment(Pos.CENTER);

        // Autore (troncato se troppo lungo)
        Label authorLabel = new Label();
        String authors = book.getAuthors();
        if (authors != null && authors.length() > 15) {
            authors = authors.substring(0, 12) + "...";
        }
        authorLabel.setText(authors != null ? authors : resolveString("%design.book.unknown.author"));
        authorLabel.getStyleClass().add(TEXT_MUTED_STYLE_CLASS);
        authorLabel.setWrapText(true);
        authorLabel.setMaxWidth(BOOK_TITLE_MAX_WIDTH);
        authorLabel.setAlignment(Pos.CENTER);

        // Aggiungi click handler per aprire il dettaglio del libro
        card.setOnMouseClicked(event -> openBookDetail(book));
        card.setOnMouseEntered(event -> {
            card.getStyleClass().remove(BOOK_CARD_BASE_CLASS);
            card.getStyleClass().add(BOOK_CARD_HOVER_CLASS);
        });
        card.setOnMouseExited(event -> {
            card.getStyleClass().remove(BOOK_CARD_HOVER_CLASS);
            card.getStyleClass().add(BOOK_CARD_BASE_CLASS);
        });

        card.getChildren().addAll(bookImage, titleLabel, authorLabel);
        return card;
    }

    /**
     * Aggiorna una libreria esistente nella lista locale
     * @param oldLibrary la libreria precedente
     * @param newLibrary la libreria aggiornata
     */
    public void updateLibrary(Library oldLibrary, Library newLibrary) {
        LoggerUtil.debug(logger, "updateLibrary called - old: " + (oldLibrary != null ? oldLibrary.getName() : "null") +
                        ", new: " + (newLibrary != null ? newLibrary.getName() : "null"));

        int index = -1;
        for (int i = 0; i < libraries.size(); i++) {
            if (libraries.get(i).getId() == oldLibrary.getId()) {
                index = i;
                break;
            }
        }

        if (index != -1) {
            libraries.set(index, newLibrary);
            LoggerUtil.debug(logger, "Library updated at index " + index + ", new book count: " +
                           (newLibrary.getLibriDisponibili() != null ? newLibrary.getLibriDisponibili().size() : 0));
        } else {
            LoggerUtil.debug(logger, "Library not found for update, adding as new");
            libraries.add(newLibrary);
        }

        // Forza un refresh dal database per assicurarsi che tutti i dati siano sincronizzati
        // Questo è importante perché le modifiche potrebbero aver influenzato altre librerie
        LoggerUtil.debug(logger, "Forcing database refresh after library update");
        refreshLibraries();
    }

    /**
     * Aggiunge una nuova libreria alla lista locale
     * @param library la libreria da aggiungere
     */
    public void addLibrary(Library library) {
        LoggerUtil.debug(logger, "addLibrary called with library: " + (library != null ? library.getName() : "null") + " (ID: " + (library != null ? library.getId() : "null") + ")");

        // Verifica se la libreria già esiste
        boolean exists = libraries.stream().anyMatch(lib -> lib.getId() == library.getId());
        if (exists) {
            LoggerUtil.debug(logger, "Library already exists (ID: " + library.getId() + ", Name: '" + library.getName() + "'), skipping add");
            return;
        }

        libraries.add(library);
        LoggerUtil.debug(logger, "Library added to local list, total libraries: " + libraries.size());

        // Aggiorna l'interfaccia
        Platform.runLater(() -> {
            updateLibraryStats();
            renderLibraries();
        });
    }

    private void openBookDetail(Book book) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(EveryView.BOOK_DETAIL_SIMPLE.getPath()), LanguageManager.getBundle());
            Parent detailRoot = loader.load();

            it.uninsubria.client.controller.homepage.BookDetailController controller =
                loader.getController();
            controller.setBook(book);

            Stage stage = new Stage();
            Scene detailScene = new Scene(detailRoot);
            ThemeManager.applyTheme(detailScene); // Applica il tema corrente
            stage.setScene(detailScene);
            stage.setTitle("Dettagli Libro");
            stage.show();

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore nell'aprire il dettaglio del libro", e);
        }
    }

    /**
     * Pulisce il backup dell'utente precedente quando cambia utente
     */
    public static void cleanupOldUserBackup() {
        try {
            // Trova tutti i file di backup degli utenti
            Path currentDir = Paths.get(".");
            Files.list(currentDir)
                .filter(path -> path.getFileName().toString().startsWith("libraries_backup_") &&
                               path.getFileName().toString().endsWith(".json"))
                .forEach(path -> {
                    try {
                        // Per ora non cancelliamo automaticamente, solo log
                        // Potremmo implementare una logica per cancellare backup vecchi
                        LoggerUtil.debug(logger, "Found old backup file: " + path.getFileName());
                    } catch (Exception e) {
                        LoggerUtil.debug(logger, "Error checking backup file: " + e.getMessage());
                    }
                });
        } catch (Exception e) {
            System.err.println("DEBUG LibraryListController: Error during backup cleanup: " + e.getMessage());
        }
    }
}
