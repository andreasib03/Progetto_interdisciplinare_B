package it.uninsubria.client.controller.homepage.libreria;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import it.uninsubria.client.utils.classesUI.LanguageManager;
import it.uninsubria.client.cache.ClientCacheManager;

import it.uninsubria.client.controller.ControllerBase;
import it.uninsubria.client.utils.classesUI.EveryView;
import it.uninsubria.client.utils.classesUI.ThemeManager;
import it.uninsubria.client.utils.classesUI.ThreadPoolManager;
import it.uninsubria.client.controller.homepage.home.viewHome;
import it.uninsubria.shared.model.Book;
import it.uninsubria.shared.model.Library;
import it.uninsubria.shared.model.Review;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import it.uninsubria.client.utils.classesUI.SessionManager;
import it.uninsubria.client.utils.classesUI.ServiceLocator;
import it.uninsubria.shared.rmi.UserService;

public class LibraryDetailController extends ControllerBase {

    // UI Constants - Using CSS classes instead of hardcoded styles for theme compatibility

    // UI Dimensions
    private static final double LIST_VIEW_HEIGHT = 300;
    private static final double CELL_HEIGHT_APPROX = 24;

    // UI Constants - Using CSS classes instead of hardcoded styles for theme compatibility
    private static final String EMPTY_LIBRARY_TEXT = resolveString("%library.noBooks");
    private static final String SUGGESTIONS_BUTTON_TEXT = resolveString("%library.button.my.suggestions");

    // CSS Class Names for theme-aware styling
    private static final String EMPTY_LIBRARY_STYLE_CLASS = "libraryEmptyText";
    private static final String LIST_VIEW_STYLE_CLASS = "libraryListView";
    private static final String BOOK_LABEL_STYLE_CLASS = "libraryBookLabel";
    private static final String REVIEW_BUTTON_STYLE_CLASS = "libraryReviewButton";
    private static final String SUGGEST_BUTTON_STYLE_CLASS = "librarySuggestButton";
    private static final String REMOVE_BUTTON_STYLE_CLASS = "libraryRemoveButton";
    private static final String SUGGESTIONS_BUTTON_STYLE_CLASS = "librarySuggestionsButton";

    // Messages
    private static final String LOGIN_REQUIRED_REVIEW = resolveString("%library.noaccess");

    // Logger
    private static final Logger logger = Logger.getLogger(LibraryDetailController.class.getName());

    @FXML private Label libraryNameLabel;
    @FXML private VBox booksContainer;

    private Library currentLibrary;
    // store user-provided suggestions locally (UI-side; to be wired to backend later)
    private List<Book> userSuggestionsCache = new ArrayList<>();

    // 🔹 Metodo richiamato quando viene aperta la vista
    public void setLibrary(Library library) {
        this.currentLibrary = library;
        if (libraryNameLabel != null) libraryNameLabel.setText(library.getName());
        renderBooks();
        renderUserSuggestionsPanel();
    }

    // 🔹 Mostra i libri nella VBox con drag & drop
    private void renderBooks() {
        booksContainer.getChildren().clear();

        if (currentLibrary == null || currentLibrary.getLibriDisponibili() == null || currentLibrary.getLibriDisponibili().isEmpty()) {
            Label empty = new Label(EMPTY_LIBRARY_TEXT);
            empty.getStyleClass().add(EMPTY_LIBRARY_STYLE_CLASS);
            booksContainer.getChildren().add(empty);
            renderUserSuggestionsPanel();
            return;
        }

        // Creare ListView per drag & drop
        ListView<Book> booksListView = new ListView<>();
        booksListView.setPrefHeight(LIST_VIEW_HEIGHT);
        booksListView.getStyleClass().add(LIST_VIEW_STYLE_CLASS);

        // Impostare i libri nella ListView
        ObservableList<Book> booksObservable = FXCollections.observableArrayList(currentLibrary.getLibriDisponibili());
        booksListView.setItems(booksObservable);

        // Personalizzare le celle con azioni
        booksListView.setCellFactory(lv -> new ListCell<Book>() {
            private final HBox container = new HBox(10);
            private final Label bookLabel = new Label();
            private final Button reviewButton = new Button(resolveString("%library.button.review"));
            private final Button suggestButton = new Button(resolveString("%library.button.suggest"));
            private final Button removeButton = new Button(resolveString("%library.button.remove"));

            {
                container.setAlignment(Pos.CENTER_LEFT);
                container.setPadding(new Insets(8));
                bookLabel.getStyleClass().add(BOOK_LABEL_STYLE_CLASS);

                reviewButton.getStyleClass().add(REVIEW_BUTTON_STYLE_CLASS);
                suggestButton.getStyleClass().add(SUGGEST_BUTTON_STYLE_CLASS);
                removeButton.getStyleClass().add(REMOVE_BUTTON_STYLE_CLASS);

                container.getChildren().addAll(bookLabel, reviewButton, suggestButton, removeButton);
            }

            @Override
            protected void updateItem(Book book, boolean empty) {
                super.updateItem(book, empty);
                if (empty || book == null) {
                    setGraphic(null);
                } else {
                    bookLabel.setText(book.getTitle() + " - " + book.getAuthors());
                    reviewButton.setOnAction(e -> openReviewScreen(book));
                    suggestButton.setDisable(currentLibrary == null || currentLibrary.getLibriDisponibili() == null ||
                                           currentLibrary.getLibriDisponibili().size() <= 1);
                    suggestButton.setOnAction(e -> openSuggestDialogWithCheckbox(book));
                    removeButton.setOnAction(e -> removeBookFromLibrary(book));
                    setGraphic(container);
                }
            }
        });

        // Implementare drag & drop per riordinamento
        booksListView.setOnDragDetected(event -> {
            if (booksListView.getSelectionModel().getSelectedItem() != null) {
                Dragboard db = booksListView.startDragAndDrop(TransferMode.MOVE);
                ClipboardContent content = new ClipboardContent();
                content.putString(String.valueOf(booksListView.getSelectionModel().getSelectedIndex()));
                db.setContent(content);
                event.consume();
            }
        });

        booksListView.setOnDragOver(event -> {
            if (event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });

        booksListView.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasString()) {
                int draggedIdx = Integer.parseInt(db.getString());

                // Calcola l'indice di destinazione basato sulla posizione Y del mouse
                double y = event.getY();
                int dropIdx = (int) (y / CELL_HEIGHT_APPROX); // Approssimazione dell'altezza di una cella
                dropIdx = Math.max(0, Math.min(dropIdx, booksObservable.size()));

                if (draggedIdx != dropIdx && draggedIdx >= 0 && draggedIdx < booksObservable.size()) {
                    Book draggedBook = booksObservable.get(draggedIdx);
                    booksObservable.remove(draggedIdx);

                    // Inserisci nella posizione corretta
                    if (dropIdx > draggedIdx) {
                        dropIdx--; // Compensazione per la rimozione precedente
                    }
                    booksObservable.add(Math.min(dropIdx, booksObservable.size()), draggedBook);

                    // Aggiornare l'ordine nella libreria
                    currentLibrary.setLibriDisponibili(new ArrayList<>(booksObservable));
                    saveLibraryOrder();
                }

                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });

        booksContainer.getChildren().add(booksListView);

        // resolveString("%library.add.suggestions.button.comment")
        Button mySugBtn = new Button(SUGGESTIONS_BUTTON_TEXT);
        mySugBtn.getStyleClass().add(SUGGESTIONS_BUTTON_STYLE_CLASS);
        mySugBtn.setOnAction(e -> openMySuggestions());
        booksContainer.getChildren().add(mySugBtn);

        renderUserSuggestionsPanel();
    }

    private void openReviewScreen(Book book){
        if (!SessionManager.getInstance().isLoggedIn()) {
            new Alert(AlertType.WARNING, LOGIN_REQUIRED_REVIEW).showAndWait();
            return;
        }
        CompletableFuture<List<Review>> reviewsFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return ServiceLocator.getReviewsService().getReviewsByBook(book.getBook_id());
            } catch (Exception e) {
                return new ArrayList<Review>();
            }
        });
        reviewsFuture.thenAccept(reviews -> {
            Integer userId = null;
            try { userId = SessionManager.getInstance().getUser().getUser_id(); } catch (Exception ignored) {}
            Review existing = null;
            if (userId != null) {
                for (Review r : reviews) {
                    if (r.getUser() != null && r.getUser().getUser_id() == userId) { existing = r; break; }
                }
            }
            final Review existingFinal = existing;
            Platform.runLater(() -> openRatingWindow(book, existingFinal));
        }).exceptionally(ex -> {
            Platform.runLater(() -> openRatingWindow(book, null));
            return null;
        });
        logger.info(java.text.MessageFormat.format(resolveString("%library.open.review"), book.getTitle()));
    }

    /**
     * Apre la finestra di valutazione per un libro
     * @param book Il libro da valutare
     * @param existingReview La review esistente se presente, null altrimenti
     */
    private void openRatingWindow(Book book, Review existingReview) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(EveryView.RATING_VIEW.getPath()));
            loader.setResources(LanguageManager.getBundle());
            Parent root = loader.load();
            ValutazioneController controller = loader.getController();
            if (currentLibrary != null) controller.setLibraryId(currentLibrary.getId());
            if (existingReview != null) {
                logger.fine("c'è già una recensione per questo utente e libro");
                controller.prepareForEdit(book, existingReview);
            } else {
                controller.setBook(book);
            }
            Stage stage = new Stage();
            stage.setTitle(String.format(resolveString("%library.review.title"), book.getTitle()));
            Scene reviewScene = new Scene(root);
            ThemeManager.applyTheme(reviewScene); // Applica il tema corrente (light/dark mode)
            stage.setScene(reviewScene);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void openSuggestDialogWithCheckbox(Book baseBook) {
        // Check if there are existing suggestions
        String username = SessionManager.getInstance().getUsername();
        if (username != null && currentLibrary != null) {
            try {
                Integer userId = null;
                UserService userService = ServiceLocator.getUserService();
                if (userService != null) {
                    var u = userService.getUserByUsernameOrEmail(username);
                    userId = (u != null) ? u.getUser_id() : null;
                }

                if (userId != null) {
                    List<Book> existingSuggestions = ServiceLocator.getBookService()
                        .getUserSuggestionsForBookAndLibrary(userId, currentLibrary.getId(), baseBook.getBook_id());

                    if (!existingSuggestions.isEmpty()) {
                        // Show confirmation alert
                        Alert alert = new Alert(AlertType.CONFIRMATION);
                        alert.setTitle(resolveString("%library.suggestions.existing.title"));
                        alert.setHeaderText(resolveString("%library.suggestions.existing.message"));
                        alert.setContentText(resolveString("%library.suggestions.existing.confirm"));

                        Optional<ButtonType> result = alert.showAndWait();
                        if (result.isPresent() && result.get() == ButtonType.OK) {
                            // User wants to edit existing suggestions
                            openSuggestDialogWithCheckbox(baseBook, true, existingSuggestions);
                        }
                        return;
                    }
                }
            } catch (Exception e) {
                logger.warning("Errore controllo suggerimenti esistenti: " + e.getMessage());
            }
        }

        // No existing suggestions, open new dialog
        openSuggestDialogWithCheckbox(baseBook, false, new ArrayList<>());
    }

    private void openSuggestDialogWithCheckbox(Book baseBook, boolean isEditing, List<Book> existingSuggestions) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(isEditing ?
            String.format(resolveString("%library.suggestions.edit.title"), baseBook.getTitle()) :
            String.format(resolveString("%library.suggestions.new.title"), baseBook.getTitle()));

        VBox root = new VBox(10);
        root.setPadding(new Insets(10));

        Label label = new Label(isEditing ? resolveString("%library.suggestions.edit.label") : resolveString("%library.suggestions.new.label"));
        List<Book> pool = new ArrayList<>();
        if (currentLibrary != null && currentLibrary.getLibriDisponibili() != null) {
            pool.addAll(currentLibrary.getLibriDisponibili());
            pool.removeIf(b -> b.getBook_id() == baseBook.getBook_id());
        }
        final Set<Integer> selectedBookIds = existingSuggestions.stream()
            .map(Book::getBook_id)
            .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new)); // Pre-select existing suggestion IDs
        final Button conferma = new Button(resolveString("%library.button.confirm"));
        conferma.setDisable(selectedBookIds.isEmpty());

        ListView<Book> listView = new ListView<>(FXCollections.observableArrayList(pool));
        listView.setCellFactory(lv -> new ListCell<Book>() {
            private final CheckBox checkBox = new CheckBox();
            private final Label labelCell = new Label();
            private final HBox hbox = new HBox(8, checkBox, labelCell);
            private Book item;

            {
                hbox.setAlignment(Pos.CENTER_LEFT);
                checkBox.setOnAction(e -> {
                    if (item == null) return;
                    if (checkBox.isSelected()) {
                        if (selectedBookIds.size() < 3) {
                            selectedBookIds.add(item.getBook_id());
                        } else {
                            checkBox.setSelected(false);
                        }
                    } else {
                        selectedBookIds.remove(item.getBook_id());
                    }
                    conferma.setDisable(selectedBookIds.isEmpty());
                });
            }

            @Override
            protected void updateItem(Book book, boolean empty) {
                super.updateItem(book, empty);
                if (empty || book == null) {
                    setGraphic(null);
                    item = null;
                } else {
                    item = book;
                    labelCell.setText(book.getTitle() + " - " + book.getAuthors());
                    checkBox.setSelected(selectedBookIds.contains(book.getBook_id()));
                    setGraphic(hbox);
                }
            }
        });

        conferma.setOnAction(ev -> {
            if (selectedBookIds.isEmpty()) {
                new Alert(AlertType.WARNING, resolveString("%library.suggestions.select.at.least.one")).showAndWait();
                return;
            }
            stage.close();

            String username = SessionManager.getInstance().getUsername();
            if (username == null) {
                Platform.runLater(() -> new Alert(AlertType.WARNING, resolveString("%library.suggestions.login.required")).showAndWait());
                return;
            }
            // Create a final copy of selectedBookIds for the thread
            final Set<Integer> finalSelectedBookIds = new LinkedHashSet<>(selectedBookIds);

            ThreadPoolManager.executeIO(() -> {
                try {
                    Integer userId = null;
                    UserService userService = ServiceLocator.getUserService();

                    if (userService != null) {
                        var u = userService.getUserByUsernameOrEmail(username);
                        userId = (u != null) ? u.getUser_id() : null;
                    }
                    if (userId == null)
                        throw new RuntimeException(resolveString("%library.suggestions.error.no.userid"));

                    final boolean[] allOK = {true};

                    if (isEditing) {
                        // Handle editing existing suggestions
                        // Convert finalSelectedBookIds back to Book objects for the update method
                        List<Book> selectedBooks = pool.stream()
                            .filter(book -> finalSelectedBookIds.contains(book.getBook_id()))
                            .collect(java.util.stream.Collectors.toList());
                        allOK[0] = updateExistingSuggestions(userId, baseBook, existingSuggestions, selectedBooks);
                    } else {
                        // Handle creating new suggestions - use batch API to ensure all books get the same suggested_id
                        try {
                            // Create a batch to get the next available suggested_id for this user
                            int batchId = ServiceLocator.getSuggestionsService().createSuggestionBatch(userId, currentLibrary != null ? currentLibrary.getId() : null);

                            // Add all selected books to the same batch (same suggested_id)
                            List<Integer> suggestedBookIdsList = new ArrayList<>(finalSelectedBookIds);

                            List<Integer> resultIds = ServiceLocator.getSuggestionsService().addSuggestionsToBatch(
                                batchId,
                                userId,
                                currentLibrary != null ? currentLibrary.getId() : null,
                                baseBook.getBook_id(),
                                suggestedBookIdsList
                            );

                             // Check if all suggestions were saved successfully
                              logger.info(java.text.MessageFormat.format(resolveString("%library.save.suggestions.attempt"), finalSelectedBookIds.size(), resultIds.size()));
                             if (resultIds.size() != finalSelectedBookIds.size()) {
                                 allOK[0] = false;
                                 Platform.runLater(() ->
                                      new Alert(AlertType.WARNING, resolveString("%library.suggestions.warning.partial.save")).showAndWait()
                                 );
                             } else {
                                  logger.info("✅ [LibraryDetailController] " + java.text.MessageFormat.format(resolveString("%library.suggestions.success.message"), batchId));
                             }

                        } catch (Exception ex) {
                            allOK[0] = false;
                            Platform.runLater(() ->
                                new Alert(AlertType.ERROR, java.text.MessageFormat.format(resolveString("%library.suggestions.error.save"), ex.getMessage())).showAndWait()
                            );
                            logger.warning(ex.getMessage());
                        }
                    }

                    Platform.runLater(() -> {
                         if (allOK[0]) {
                              new Alert(AlertType.INFORMATION, (isEditing ? resolveString("%library.suggestions.success.edit") : resolveString("%library.suggestions.success.new")) + "\n\n" + resolveString("%library.suggestions.success.note") + "\n" + resolveString("%library.suggestions.success.refresh")).showAndWait();
                              // Forza refresh completo della homepage per riflettere i nuovi suggerimenti
                              viewHome.refreshHomePage();
                              // Invalida cache TTL-based per i suggerimenti di questo libro
                              ClientCacheManager.invalidate("suggestions:" + baseBook.getBook_id());
                             // Update cache for display
                            userSuggestionsCache.clear();
                            List<Book> selectedBooksForCache = pool.stream()
                                .filter(book -> finalSelectedBookIds.contains(book.getBook_id()))
                                .collect(java.util.stream.Collectors.toList());
                            userSuggestionsCache.addAll(selectedBooksForCache);
                            renderUserSuggestionsPanel();
                        } else {
                                new Alert(AlertType.WARNING, resolveString("%library.suggestions.warning.partial.save")).showAndWait();
                                logger.info(String.valueOf(allOK[0]));
                        }
                    });

                } catch(Exception ex){
                    Platform.runLater(() ->
                    new Alert(AlertType.ERROR, java.text.MessageFormat.format(resolveString("%library.suggestions.error.save"), ex.getMessage())).showAndWait()
                    );
                }
            });
        });

        root.getChildren().addAll(label, listView, conferma);
        stage.setScene(new Scene(root, 420, 320));
        stage.showAndWait();
    }

    // 🆕 Metodo per rimuovere un libro dalla libreria
    private void removeBookFromLibrary(Book book) {
        if (currentLibrary == null || currentLibrary.getLibriDisponibili() == null) return;

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle(resolveString("%library.remove.confirm.title"));
        confirmation.setHeaderText(java.text.MessageFormat.format(resolveString("%library.remove.confirm.header"), book.getTitle()));
        confirmation.setContentText(resolveString("%library.remove.confirm.message"));

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Rimuovere il libro dalla lista locale
            currentLibrary.getLibriDisponibili().remove(book);

            // Salvare le modifiche nel database
            ThreadPoolManager.executeIO(() -> {
                try {
                    String username = SessionManager.getInstance().getUsername();
                    if (username != null) {
                        ServiceLocator.getLibraryService().updateLibraryForUser(
                            username, currentLibrary, currentLibrary);
                        Platform.runLater(() -> {
                            renderBooks(); // Rirenderizzare la lista
                            new Alert(Alert.AlertType.INFORMATION,
                                resolveString("%library.remove.success")).showAndWait();
                        });
                    }
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        new Alert(Alert.AlertType.ERROR,
                            java.text.MessageFormat.format(resolveString("%library.remove.error"), e.getMessage())).showAndWait();
                    });
                }
            });
        }
    }

    // 🆕 Metodo per salvare il nuovo ordine dei libri
    private void saveLibraryOrder() {
        if (currentLibrary == null) return;

        ThreadPoolManager.executeIO(() -> {
            try {
                String username = SessionManager.getInstance().getUsername();
                if (username != null) {
                    ServiceLocator.getLibraryService().updateLibraryForUser(
                        username, currentLibrary, currentLibrary);
                    Platform.runLater(() -> {
                        new Alert(Alert.AlertType.INFORMATION,
                              resolveString("%library.reorder.success")).showAndWait();
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    new Alert(Alert.AlertType.ERROR,
                        java.text.MessageFormat.format(resolveString("%library.reorder.error"), e.getMessage())).showAndWait();
                });
                }
            });
    }

    private boolean updateExistingSuggestions(int userId, Book baseBook, List<Book> existingSuggestions, List<Book> selected) {
        try {
            // Prima rimuovi TUTTI i suggerimenti esistenti per questo libro/utente/libreria
            // Questo approccio è più sicuro e semplice
            logger.info(java.text.MessageFormat.format(resolveString("%library.suggestions.remove.all"), baseBook.getBook_id()));
            for (Book existingBook : existingSuggestions) {
                try {
                    boolean deleted = ServiceLocator.getBookService().deleteUserSuggestion(userId, baseBook.getBook_id(), existingBook.getBook_id());
                    logger.info(java.text.MessageFormat.format(resolveString("%library.suggestions.delete.single"), existingBook.getBook_id(), deleted));
                } catch (Exception e) {
                    logger.warning(java.text.MessageFormat.format(resolveString("%library.suggestions.delete.warning"), existingBook.getBook_id(), e.getMessage()));
                }
            }

            // Ora aggiungi i nuovi suggerimenti selezionati
            if (!selected.isEmpty()) {
                try {
                    // Crea un nuovo batch per i suggerimenti aggiornati
                    int newBatchId = ServiceLocator.getSuggestionsService().createSuggestionBatch(userId, currentLibrary.getId());
                    logger.info(java.text.MessageFormat.format(resolveString("%library.suggestions.batch.created"), newBatchId));

                    List<Integer> suggestedBookIds = selected.stream()
                        .map(Book::getBook_id)
                        .toList();

                    logger.info(java.text.MessageFormat.format(resolveString("%library.suggestions.adding"), suggestedBookIds.size(), suggestedBookIds));

                    // Aggiungi tutti i libri selezionati al nuovo batch
                    List<Integer> resultIds = ServiceLocator.getSuggestionsService().addSuggestionsToBatch(
                        newBatchId,
                        userId,
                        currentLibrary.getId(),
                        baseBook.getBook_id(),
                        suggestedBookIds
                    );

                    logger.info(java.text.MessageFormat.format(resolveString("%library.suggestions.added.success"), resultIds));

                    // Verifica che tutti i suggerimenti siano stati aggiunti
                    if (resultIds.size() != suggestedBookIds.size()) {
                        logger.warning(java.text.MessageFormat.format(resolveString("%library.suggestions.error.count"), suggestedBookIds.size(), resultIds.size()));
                        Platform.runLater(() ->
                            new Alert(AlertType.WARNING, resolveString("%library.suggestions.warning.log")).showAndWait()
                        );
                        return false;
                    }

                    logger.info(resolveString("%library.suggestions.saved.all"));
                    return true;

                } catch (Exception e) {
                    logger.warning(java.text.MessageFormat.format(resolveString("%library.suggestions.save.error.add"), e.getMessage()));
                    e.printStackTrace();
                    Platform.runLater(() ->
                        new Alert(AlertType.ERROR, java.text.MessageFormat.format(resolveString("%library.suggestions.error.save"), e.getMessage())).showAndWait()
                    );
                    return false;
                }
            } else {
                // Nessun suggerimento selezionato - solo rimozione completata
                logger.info(resolveString("%library.suggestions.none.selected"));
                return true;
            }

        } catch (Exception e) {
            logger.warning(java.text.MessageFormat.format(resolveString("%library.suggestions.update.error"), e.getMessage()));
            e.printStackTrace();
            return false;
        }
    }

    private void renderUserSuggestionsPanel() {
        // Rimuovi eventuali pannelli di suggerimenti precedenti
        booksContainer.getChildren().removeIf(n -> n.getId() != null && n.getId().toString().equals("userSuggestionsBox"));
        // Non mostrare più i suggerimenti come label nella pagina principale
        // I suggerimenti ora sono accessibili solo tramite il pulsante "Le mie proposte"
    }

    @FXML
    public void initialize() {
        // inizializzazione vuota
    }

    public void setParentControllerOpen(LibraryListController controller) {}

    private void openMySuggestions() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(EveryView.SUGGESTIONS_DETAIL_VIEW.getPath()), LanguageManager.getBundle());
            Parent root = loader.load();

            // Ottieni il controller e imposta l'ID della libreria corrente
            MySuggestionsController controller = loader.getController();
            if (currentLibrary != null) {
                controller.setLibraryId(currentLibrary.getId());
            }

            Stage stage = new Stage();
            stage.setTitle(java.text.MessageFormat.format(
                resolveString("%library.my.suggestions.title.library"),
                currentLibrary != null ? currentLibrary.getName() : "Libreria"));
            Scene suggestionsScene = new Scene(root);
            ThemeManager.applyTheme(suggestionsScene); // Applica il tema corrente (light/dark mode)
            stage.setScene(suggestionsScene);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
