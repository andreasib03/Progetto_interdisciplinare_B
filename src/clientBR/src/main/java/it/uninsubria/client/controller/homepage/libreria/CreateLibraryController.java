 package it.uninsubria.client.controller.homepage.libreria;

 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 import java.util.logging.Logger;
 import java.util.stream.Collectors;

 import it.uninsubria.client.controller.ControllerBase;
 import it.uninsubria.client.utils.classesUI.BookServiceManager;
 import it.uninsubria.client.utils.classesUI.InputValidatorClient;
 import it.uninsubria.client.utils.classesUI.NotificationManager;
 import it.uninsubria.client.utils.classesUI.ServiceLocator;
 import it.uninsubria.client.utils.classesUI.SessionManager;
 import it.uninsubria.client.utils.classesUI.ThreadPoolManager;
 import it.uninsubria.shared.model.Book;
 import it.uninsubria.shared.model.Library;
 import javafx.animation.PauseTransition;
 import javafx.application.Platform;
 import javafx.collections.FXCollections;
 import javafx.collections.ObservableList;
 import javafx.collections.transformation.FilteredList;
 import javafx.fxml.FXML;
 import javafx.scene.control.Button;
 import javafx.scene.control.Label;
 import javafx.scene.control.ListCell;
 import javafx.scene.control.ListView;
 import javafx.scene.control.TextField;
 import javafx.scene.control.CheckBox;
 import javafx.scene.control.Tooltip;
 import javafx.scene.layout.VBox;
 import javafx.stage.Stage;
 import javafx.util.Duration;

 public class CreateLibraryController extends ControllerBase {

     private static final Logger logger = Logger.getLogger(CreateLibraryController.class.getName());
    @FXML
    private Label libraryNameLabel;

    @FXML
    private TextField libraryNameField;

    @FXML
    private Label libraryNameStatusLabel;

    @FXML
    private Label selectBooksLabel;

    @FXML
    private VBox vboxContainer;

    @FXML
    private ListView<Book> booksListView;

    @FXML
    private TextField searchBooksField;

    @FXML
    private Button saveLibraryButton;

    private LibraryListController parentController;
    private Library libraryToEdit;
    private List<Library> userLibraries;
    private Map<Integer, String> booksInOtherLibraries; // book_id -> library_name



    private ObservableList<Book> availableBooks = FXCollections.observableArrayList();
    private FilteredList<Book> filteredBooks;


    private final BookServiceManager bookManager = BookServiceManager.getInstance();

    private PauseTransition pause;

    private boolean editMode = false;

    /**
     * Applica la localizzazione ai testi dei componenti FXML
     */
    private void applyLocalization() {
        if (libraryNameLabel != null) {
            libraryNameLabel.setText(resolveString("%createlibrary.label.name"));
        }
        if (libraryNameField != null) {
            libraryNameField.setPromptText(resolveString("%createlibrary.prompt.library.name"));
        }
        if (selectBooksLabel != null) {
            selectBooksLabel.setText(resolveString("%createlibrary.label.select.books"));
        }
        if (searchBooksField != null) {
            searchBooksField.setPromptText(resolveString("%createlibrary.prompt.search.books"));
        }
    }

    public void initialize() {
        // Applica localizzazione ai testi FXML
        applyLocalization();

        // Load user libraries to check for existing books
        loadUserLibraries();

        pause = new PauseTransition(Duration.seconds(0.8));
        // Mostra sempre il container per migliore UX - sarà disabilitato se necessario
        vboxContainer.setVisible(true);

        updateSaveButtonState();

        libraryNameField.textProperty().addListener((obs, oldText, newText) -> {
            if(!editMode){
                pause.stop();
                pause.setOnFinished(event -> checkLibraryName(newText));
                pause.playFromStart();
            }

            updateSaveButtonState();
        });

        // Usa i libri già caricati all'avvio dell'applicazione
        if (bookManager.isLoaded()) {
            Platform.runLater(() -> {
                List<Book> clonedBooks = bookManager.getAllBooks().stream()
                        .map(this::cloneBook)
                        .collect(Collectors.toList());
                availableBooks.setAll(clonedBooks);
                filteredBooks = new FilteredList<>(availableBooks, b -> true);
                booksListView.setItems(filteredBooks);
                setupListViewCellFactory();

                // Setup search functionality
                setupBookSearch();

                if(editMode && libraryToEdit != null){
                    libraryNameField.setText(libraryToEdit.getName());
                    libraryNameField.setEditable(false);
                    libraryNameStatusLabel.setText("✏️ Modalità modifica");
                    libraryNameStatusLabel.setStyle("-fx-text-fill: #007bff;");

                    loadSelectionsForEdit();
                    vboxContainer.setVisible(true);
                    updateSaveButtonState();
                }
            });
        } else {
            // Fallback: carica i libri se non sono stati precaricati
            bookManager.loadAllBooks().thenAccept(books -> {
                Platform.runLater(() -> {
                    List<Book> clonedBooks = books.stream()
                            .map(this::cloneBook)
                            .collect(Collectors.toList());
                    availableBooks.setAll(clonedBooks);
                    filteredBooks = new FilteredList<>(availableBooks, b -> true);
                    booksListView.setItems(filteredBooks);
                    setupListViewCellFactory();

                    // Setup search functionality
                    setupBookSearch();

                    if(editMode && libraryToEdit != null){
                        libraryNameField.setText(libraryToEdit.getName());
                        libraryNameField.setEditable(false);
                        libraryNameStatusLabel.setText("✏️ Modalità modifica");
                        libraryNameStatusLabel.setStyle("-fx-text-fill: #007bff;");

                        loadSelectionsForEdit();
                        vboxContainer.setVisible(true);
                        updateSaveButtonState();
                    }
                });
            });
        }

        saveLibraryButton.setOnAction(e -> saveLibrary());
    }

    /**
     * Called by parent controller when this window is opened.
     * Ensures we have the latest library data from database.
     */
    public void onWindowOpened() {
        logger.fine("Window opened, reloading libraries from database to ensure fresh data");

        // Forza il refresh del parent controller prima di caricare i dati locali
        if (parentController != null) {
            logger.fine("Requesting parent controller data refresh");
            parentController.requestDataRefresh();
        }

        // Poi carica i dati locali
        loadUserLibraries();
    }

    private Book cloneBook(Book original) {
        Book clone = new Book(
                original.getBook_id(),
                original.getTitle(),
                original.getAuthors(),
                original.getPublish_date_year()
        );
        // eventuali altri campi che vuoi copiare
        return clone;
    }


    private void checkLibraryName(String name) {
        name.trim();
        if(name.isBlank()) {
            updateStatus(resolveString("%createlibrary.validation.name.empty"), "#d9534f", false);
            return;
        }

        ThreadPoolManager.executeIO(() -> {
            try {
                String username = SessionManager.getInstance().getUsername();
                boolean exists = ServiceLocator.getLibraryService().libraryExistsForUser(username, name);

                Platform.runLater(() ->
                    updateStatus(exists ? resolveString("%createlibrary.validation.name.exists") : resolveString("%createlibrary.validation.name.valid"),
                    exists ? "#d9534f" : "#28a745",
                    !exists)); // true solo se il nome è valido (non esiste)
            } catch (Exception e) {
                Platform.runLater(() -> updateStatus(resolveString("%createlibrary.validation.error.check"), "#f0ad4e", false)); // In caso di errore, considera il nome non valido
            }
            return null;
        });
    }

    private void updateStatus(String text, String color, boolean isNameValid) {
            libraryNameStatusLabel.setText(text);
            libraryNameStatusLabel.setStyle("-fx-text-fill: " + color + ";");

            // Mostra sempre il container dei libri per migliore UX
            vboxContainer.setVisible(true);

            // Disabilita la lista e ricerca solo se il nome non è valido
            // (vuoto, già esistente, o errore di controllo)
            booksListView.setDisable(!isNameValid);
            searchBooksField.setDisable(!isNameValid);

            // Aggiorna lo stato del pulsante salva
            updateSaveButtonState();
    }
    public void setLibraryToEdit(Library lib) {
        this.libraryToEdit = lib;
        this.editMode = lib != null;

        // Always load fresh data from database
        logger.fine("Loading fresh libraries data from database");
        loadUserLibraries();
    }

    
    public void setParentController(LibraryListController controller) {
        this.parentController = controller;
    }


    private void loadUserLibraries() {
        // Load user libraries from database to ensure fresh data
        ThreadPoolManager.executeIO(() -> {
            try {
                String username = SessionManager.getInstance().getUsername();
                if (username != null) {
                    logger.fine("Loading libraries from database for user: " + username);
                    // Always get fresh data from database
                    List<Library> freshLibraries = ServiceLocator.getLibraryService().getUserLibraries(username);
                    logger.fine("Database returned " + freshLibraries.size() + " libraries");

                    for (Library lib : freshLibraries) {
                        logger.fine("  - Library: " + lib.getName() + " (ID: " + lib.getId() + ") with " + (lib.getLibriDisponibili() != null ? lib.getLibriDisponibili().size() : 0) + " books");
                    }

                    final List<Library> librariesToSet = freshLibraries;
                    Platform.runLater(() -> {
                        userLibraries = librariesToSet;
                        buildBooksInOtherLibrariesMap();
                        logger.fine("UI updated with fresh library data");

                        // Forza il refresh della ListView per aggiornare le checkbox dopo la ricostruzione della mappa
                        if (booksListView != null) {
                            booksListView.refresh();
                            logger.fine("ListView refreshed to update checkbox states");
                        }

                        // Riapplica le selezioni per la modalità modifica se necessario
                        if (editMode && libraryToEdit != null) {
                            loadSelectionsForEdit();
                            logger.fine("Selections reapplied after library data refresh");
                        }
                    });
                }
            } catch (Exception e) {
                logger.severe("Errore nel caricamento delle librerie dal database: " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> {
                    userLibraries = new ArrayList<>(); // Fallback to empty list
                    booksInOtherLibraries = new HashMap<>();
                    if (booksListView != null) {
                        booksListView.refresh();
                    }
                });
                  }
                return null;
            });
    }

    private void buildBooksInOtherLibrariesMap() {
        booksInOtherLibraries = new HashMap<>();
        if (userLibraries == null) {
            logger.fine("userLibraries is null");
            return;
        }

        logger.fine("Building books map for " + userLibraries.size() + " libraries");
        for (Library library : userLibraries) {
            // Se stiamo modificando una libreria, escludi i libri di quella libreria
            if (libraryToEdit != null && library.getId() == libraryToEdit.getId()) {
                logger.fine("Skipping current library being edited: " + library.getName());
                continue;
            }

            logger.fine("Processing library: " + library.getName() + " with " + (library.getLibriDisponibili() != null ? library.getLibriDisponibili().size() : 0) + " books");
            if (library.getLibriDisponibili() != null) {
                for (Book book : library.getLibriDisponibili()) {
                    booksInOtherLibraries.put(book.getBook_id(), library.getName());
                    logger.fine("Book " + book.getTitle() + " (ID: " + book.getBook_id() + ") is in library " + library.getName());
                }
            }
        }
        logger.fine("Final map has " + booksInOtherLibraries.size() + " book entries");
    }

    private void setupListViewCellFactory() {
        booksListView.setCellFactory(lv -> new ListCell<>() {
            private final CheckBox checkBox = new CheckBox();

            {
                checkBox.setOnAction(event -> {
                    Book book = getItem();
                    if (book != null) {
                        book.setSelected(checkBox.isSelected());
                        updateSaveButtonState();
                    }
                });
            }

            @Override
            protected void updateItem(Book book, boolean empty) {
                super.updateItem(book, empty);
                if (empty || book == null) {
                    setGraphic(null);
                } else {
                    checkBox.setText(book.getTitle());
                    checkBox.setSelected(book.isSelected());

                    // Check if book is in another library (ma non nella libreria che stiamo modificando)
                    if (booksInOtherLibraries != null && booksInOtherLibraries.containsKey(book.getBook_id())) {
                        String libraryName = booksInOtherLibraries.get(book.getBook_id());

                        // Se stiamo modificando una libreria esistente, permetti la selezione dei libri
                        // che sono già nella libreria che stiamo modificando
                        boolean isInCurrentLibrary = editMode && libraryToEdit != null &&
                            libraryToEdit.getLibriDisponibili() != null &&
                            libraryToEdit.getLibriDisponibili().stream()
                                .anyMatch(libBook -> libBook.getBook_id() == book.getBook_id());

                        if (!isInCurrentLibrary) {
                            checkBox.setDisable(true);
                            // Non deselezionare automaticamente se è già selezionato per modifica
                            if (!editMode) {
                                checkBox.setSelected(false);
                            }
                            checkBox.setText(book.getTitle() + " (già in '" + libraryName + "')");
                            Tooltip tooltip = new Tooltip("Questo libro è già presente nella libreria '" + libraryName + "'");
                            Tooltip.install(checkBox, tooltip);
                        } else {
                            // Libro è nella libreria corrente, permetti la modifica
                            checkBox.setDisable(false);
                            checkBox.setText(book.getTitle() + " ✓");
                            Tooltip tooltip = new Tooltip("Questo libro è già nella tua libreria (puoi modificarlo)");
                            Tooltip.install(checkBox, tooltip);
                        }
                    } else {
                        checkBox.setDisable(false);
                        checkBox.setText(book.getTitle());
                        Tooltip.uninstall(checkBox, null); // Remove tooltip if it exists
                    }

                    setGraphic(checkBox);
                }
            }
        });
    }

    private void setupBookSearch() {
        searchBooksField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (filteredBooks != null) {
                filteredBooks.setPredicate(book -> {
                    if (newValue == null || newValue.trim().isEmpty()) {
                        return true; // Mostra tutti i libri se il campo è vuoto
                    }

                    String lowerCaseFilter = newValue.toLowerCase();
                    String bookTitle = book.getTitle().toLowerCase();

                    return bookTitle.contains(lowerCaseFilter);
                });
            }
        });
    }

    private void loadSelectionsForEdit(){

        if(libraryToEdit == null || availableBooks.isEmpty()){
            return;
        }

        Set<Integer> libraryBookIds = libraryToEdit.getLibriDisponibili()
                                                   .stream()
                                                   .map(Book::getBook_id)
                                                   .collect(Collectors.toSet());       

        for (Book b : availableBooks) {
            b.setSelected(libraryBookIds.contains(b.getBook_id()));
        }
    }

    private void updateSaveButtonState(){
        // Il nome è valido se: non è vuoto, non contiene errori (❌), e non è già esistente
        boolean hasValidName = !libraryNameField.getText().trim().isEmpty() &&
                              !libraryNameStatusLabel.getText().contains("❌") &&
                              !libraryNameStatusLabel.getText().contains(resolveString("%createlibrary.validation.name.exists"));

        // Il pulsante è abilitato solo se nome valido, libri selezionati, e sezione libri abilitata
        saveLibraryButton.setDisable(!(hasValidName));
    }


    private void saveLibrary() {
        String name = libraryNameField.getText().trim();

        List<Book> selectedBooks = availableBooks.stream()
                .filter(Book::isSelected)
                .collect(Collectors.toList());

        // Validazione input
        var nameValidation = InputValidatorClient.validateLibraryName(name);
        if (!nameValidation.isValid()) {
            NotificationManager.showError(nameValidation.getErrorMessage());
            return;
        }

        if (selectedBooks.isEmpty()){
            NotificationManager.showError("Seleziona almeno un libro per la libreria");
            return;
        }

        // Disable the save button to prevent multiple saves
        saveLibraryButton.setDisable(true);

        if(editMode) {
            // Aggiorna libraryToEdit direttamente
            libraryToEdit.setLibriDisponibili(selectedBooks);
            logger.fine("Attempting to update existing library '" + libraryToEdit.getName() + "' with " + selectedBooks.size() + " books");
            ThreadPoolManager.executeCritical(() -> {
                try {
                    String username = SessionManager.getInstance().getUsername();
                    logger.fine("Calling updateLibraryForUser for user: " + username);
                    ServiceLocator.getLibraryService().updateLibraryForUser(username, libraryToEdit, libraryToEdit);
                    logger.fine("Library updated successfully");
                      Platform.runLater(() -> {
                          parentController.updateLibrary(libraryToEdit, libraryToEdit);
                          logger.fine("Library updated in parent controller");

                          // Forza un refresh completo del parent controller dopo l'aggiornamento
                          parentController.requestDataRefresh();

                          // Ricarica le librerie locali per avere dati aggiornati per future operazioni
                          logger.fine("Reloading local libraries for updated books map");
                          loadUserLibraries();

                          Stage stage = (Stage) saveLibraryButton.getScene().getWindow();
                          stage.close();
                 });
            } catch (Exception e) {
                logger.severe("Errore nel caricamento delle librerie dal database: " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> {
                    userLibraries = new ArrayList<>(); // Fallback to empty list
                    booksInOtherLibraries = new HashMap<>();
                    if (booksListView != null) {
                        booksListView.refresh();
                    }
                });
            }
        });
        } else {
            Library libToSave = new Library(name, selectedBooks);
            logger.fine("Attempting to save new library '" + name + "' with " + selectedBooks.size() + " books");
            for (Book book : selectedBooks) {
                logger.fine("  - Book: " + book.getTitle() + " (ID: " + book.getBook_id() + ")");
            }
            ThreadPoolManager.executeCritical(() -> {
                try {
                    String username = SessionManager.getInstance().getUsername();
                    logger.fine("Calling addLibraryForUser for user: " + username);

                     // Test the service connection first
                     logger.fine("Testing service connection...");
                     try {
                         List<Library> testLibraries = ServiceLocator.getLibraryService().getUserLibraries(username);
                         logger.fine("Service test successful, found " + testLibraries.size() + " existing libraries");
                         for (Library lib : testLibraries) {
                             logger.fine("  - Existing library: " + lib.getName() + " (ID: " + lib.getId() + ") with " + (lib.getLibriDisponibili() != null ? lib.getLibriDisponibili().size() : 0) + " books");
                         }
                     } catch (Exception testEx) {
                         logger.severe("DEBUG CreateLibraryController: Service test failed: " + testEx.getMessage());
                         testEx.printStackTrace();
                     }

                     Library saved = ServiceLocator.getLibraryService().addLibraryForUser(username, libToSave);
                     logger.fine("Library saved successfully with ID: " + (saved != null ? saved.getId() : "null"));
                     if (saved != null) {
                         logger.fine("Saved library has " + (saved.getLibriDisponibili() != null ? saved.getLibriDisponibili().size() : 0) + " books");
                         if (saved.getLibriDisponibili() != null) {
                             for (Book book : saved.getLibriDisponibili()) {
                                 logger.fine("  - Saved book: " + book.getTitle() + " (ID: " + book.getBook_id() + ")");
                             }
                         }
                     }

                      // Force cache invalidation by making another call
                      logger.fine("Forcing cache refresh by calling getUserLibraries again");
                      ServiceLocator.getLibraryService().getUserLibraries(username);
                       Platform.runLater(() -> {
                          // Add the newly saved library directly to the UI instead of refreshing everything
                          logger.fine("Adding saved library directly to parent controller");
                          parentController.addLibrary(saved);
                          logger.fine("Library added to parent controller");

                          // Forza un refresh completo del parent controller per assicurarsi che abbia i dati più recenti
                          parentController.requestDataRefresh();

                          // Ricarica le librerie locali per avere dati aggiornati per future operazioni
                          logger.fine("Reloading local libraries for updated books map");
                          loadUserLibraries();

                          Stage stage = (Stage) saveLibraryButton.getScene().getWindow();
                          stage.close();
                      });
                 } catch (Exception e) {
                     logger.severe("Errore durante il salvataggio della libreria: " + e.getMessage());
                     e.printStackTrace();
                     // Check if it's a validation error about books already in other libraries
                     if (e.getMessage() != null && e.getMessage().contains("è già presente")) {
                         logger.severe("DEBUG CreateLibraryController: Validation error - some books are already in other libraries");
                         Platform.runLater(() -> {
                             libraryNameStatusLabel.setText("Alcuni libri sono già presenti in altre librerie");
                             libraryNameStatusLabel.setStyle("-fx-text-fill: red;");
                             saveLibraryButton.setDisable(false);
                         });
                     } else {
                         Platform.runLater(() -> {
                             saveLibraryButton.setDisable(false); // Re-enable on error
                         });
                     }
                  }
            });
        }

            
    }
    
}
