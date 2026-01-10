package it.uninsubria.client.controller.homepage.libreria;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import it.uninsubria.shared.model.SuggestionEntry;
import it.uninsubria.shared.model.Book;
import it.uninsubria.shared.rmi.BookService;
import it.uninsubria.client.controller.ControllerBase;
import it.uninsubria.client.utils.classesUI.ServiceLocator;
import it.uninsubria.client.utils.classesUI.SessionManager;
import it.uninsubria.client.utils.classesUI.ThreadPoolManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TreeView;
import javafx.scene.control.TreeItem;
import javafx.application.Platform;

public class MySuggestionsController extends ControllerBase implements Initializable {

    // Logger
    private static final Logger logger = Logger.getLogger(MySuggestionsController.class.getName());

    // UI Constants - using resolveString for localization
    private static final String ROOT_NODE_TEXT = ControllerBase.resolveString("%mysuggestions.root.node");
    private static final String EMPTY_SUGGESTIONS_ROOT = ControllerBase.resolveString("%mysuggestions.empty.root");
    private static final String NO_SUGGESTIONS_TEXT = ControllerBase.resolveString("%mysuggestions.no.suggestions");
    private static final String HELP_TEXT = ControllerBase.resolveString("%mysuggestions.help.title");
    private static final String STEP1_TEXT = ControllerBase.resolveString("%mysuggestions.help.step1");
    private static final String STEP2_TEXT = ControllerBase.resolveString("%mysuggestions.help.step2");
    private static final String STEP3_TEXT = ControllerBase.resolveString("%mysuggestions.help.step3");
    private static final String ERROR_LOADING_TEXT = ControllerBase.resolveString("%mysuggestions.error.loading");

    @FXML private TreeView<String> suggestionsTree;

    private Map<String, List<SuggestionEntry>> suggestionsByBaseBook = new HashMap<>();
    private Integer libraryId = null; // null = global suggestions, not null = library-specific

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("🚀 [MySuggestionsController] Inizializzazione controller suggerimenti" +
                   (libraryId != null ? " per libreria " + libraryId : " (globale)"));
        // Aggiungi listener per double-click sui nodi base (libri)
        // Rimossa la funzionalità di modifica double-click per la sezione visualizzazione
        onRefresh();
    }

    public void setLibraryId(Integer libraryId) {
        this.libraryId = libraryId;
    }

    private void loadSuggestions() {
        final int uid = SessionManager.getInstance().getUser().getUser_id();

        ThreadPoolManager.executeIO(() -> {
            try {
                logger.info("🔄 [MySuggestionsController] Iniziando caricamento suggerimenti per utente " + uid +
                           (libraryId != null ? " (libreria " + libraryId + ")" : " (globale)"));
                BookService bookService = ServiceLocator.getBookService();
                List<SuggestionEntry> entries;

                if (libraryId != null) {
                    entries = bookService.getUserSuggestionsForLibrary(uid, libraryId);
                } else {
                    entries = bookService.getAllUserSuggestions(uid);
                }

                logger.info("📚 CARICAMENTO SUGGERIMENTI DA DB: Trovati " + (entries != null ? entries.size() : 0) + " suggerimenti per utente " + uid +
                           (libraryId != null ? " (libreria " + libraryId + ")" : " (globale)"));

                if (entries != null && !entries.isEmpty()) {
                    logger.fine("📋 Elenco suggerimenti dal DB (" + entries.size() + " totali):");
                    for (SuggestionEntry entry : entries) {
                        logger.fine("   - ID:" + entry.getSuggestedId() + " | Base:" + entry.getBaseBookId() + " -> Suggerito:" + entry.getSuggestedBookId() + " | Libreria:" + entry.getLibraryId());
                    }
                    logger.info("✅ [MySuggestionsController] Suggerimenti caricati dal DB, procedo con la visualizzazione");
                    // Raggruppa suggerimenti per libro base
                    Map<Integer, List<SuggestionEntry>> groupedByBase = new HashMap<>();
                    for (SuggestionEntry se : entries) {
                        groupedByBase.computeIfAbsent(se.getBaseBookId(), k -> new ArrayList<>()).add(se);
                    }

                    TreeItem<String> root = new TreeItem<>(ROOT_NODE_TEXT);
                    root.setExpanded(true);

                    for (Map.Entry<Integer, List<SuggestionEntry>> entry : groupedByBase.entrySet()) {
                        int baseBookId = entry.getKey();
                        List<SuggestionEntry> suggestions = entry.getValue();

                        try {
                            Book baseBook = bookService.getBookDetails(baseBookId);
                            String baseTitle = baseBook != null ? baseBook.getTitle() : "Libro #" + baseBookId;

                            TreeItem<String> baseItem = new TreeItem<>(baseTitle + " (" + suggestions.size() + " suggerimenti)");
                            baseItem.setExpanded(true);

                            for (SuggestionEntry se : suggestions) {
                                try {
                                    Book suggestedBook = bookService.getBookDetails(se.getSuggestedBookId());
                                    String suggestedTitle = suggestedBook != null ? suggestedBook.getTitle() : "Libro #" + se.getSuggestedBookId();
                                    TreeItem<String> suggestionItem = new TreeItem<>(suggestedTitle);
                                    baseItem.getChildren().add(suggestionItem);
                                } catch (Exception inner) {
                                    logger.log(Level.WARNING, "Failed to load suggested book: " + se.getSuggestedBookId(), inner);
                                }
                            }

                            if (!baseItem.getChildren().isEmpty()) {
                                root.getChildren().add(baseItem);
                                suggestionsByBaseBook.put(baseTitle, suggestions);
                            }
                        } catch (Exception inner) {
                            logger.log(Level.WARNING, "Failed to load base book: " + baseBookId, inner);
                        }
                    }

            Platform.runLater(() -> {
                suggestionsTree.setRoot(root);
            });
            } else {
                // Se non ci sono suggerimenti, mostra messaggio informativo invece di aggiungere esempi
                logger.info("ℹ️ Nessun suggerimento trovato per utente " + uid + ", mostro messaggio informativo...");
                showEmptySuggestionsMessage();
            }
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Errore durante il caricamento dei suggerimenti", ex);
                Platform.runLater(() -> {
                    TreeItem<String> root = new TreeItem<>(ERROR_LOADING_TEXT);
                    suggestionsTree.setRoot(root);
                });
            }
        });
    }

   @FXML
   private void onRefresh() {
     suggestionsTree.setRoot(null);
     suggestionsByBaseBook.clear();
     loadSuggestions();
   }

    private void showEmptySuggestionsMessage() {
        TreeItem<String> root = new TreeItem<>(EMPTY_SUGGESTIONS_ROOT);
        TreeItem<String> emptyItem = new TreeItem<>(NO_SUGGESTIONS_TEXT);
        TreeItem<String> helpItem = new TreeItem<>(HELP_TEXT);
        TreeItem<String> step1Item = new TreeItem<>(STEP1_TEXT);
        TreeItem<String> step2Item = new TreeItem<>(STEP2_TEXT);
        TreeItem<String> step3Item = new TreeItem<>(STEP3_TEXT);

        root.getChildren().addAll(Arrays.asList(emptyItem, helpItem, step1Item, step2Item, step3Item));
        root.setExpanded(true);

        Platform.runLater(() -> {
            suggestionsTree.setRoot(root);
        });
    }
}
