package it.uninsubria.client.controller.homepage.libreria;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import it.uninsubria.client.utils.classesUI.ServiceLocator;
import it.uninsubria.client.utils.classesUI.SessionManager;
import it.uninsubria.client.utils.classesUI.ThreadPoolManager;
import it.uninsubria.client.controller.homepage.home.viewHome;
import it.uninsubria.client.utils.classesUI.LanguageManager;
import it.uninsubria.shared.model.Book;
import it.uninsubria.shared.model.Review;
import it.uninsubria.shared.model.User;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextArea;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

public class ValutazioneController {
    private static final Logger logger = Logger.getLogger(ValutazioneController.class.getName());

    /**
     * Risolve una chiave di localizzazione. Se inizia con %, usa il bundle delle properties,
     * altrimenti restituisce la stringa così com'è.
     */
    private static String resolveString(String key) {
        if (key != null && key.startsWith("%")) {
            String actualKey = key.substring(1); // Rimuovi il %
            try {
                return it.uninsubria.client.utils.classesUI.LanguageManager.getBundle().getString(actualKey);
            } catch (Exception e) {
                // Se la chiave non esiste, restituisci la chiave originale per debug
                return key;
            }
        }
        return key;
    }
    @FXML
    private GridPane valutazioneGrid;

    @FXML
    private Button sumbitButton;

    // FXML components for criteria
    @FXML private Label styleLabel;
    @FXML private Spinner<Integer> styleSpinner;
    @FXML private TextArea styleNoteArea;

    @FXML private Label contentLabel;
    @FXML private Spinner<Integer> contentSpinner;
    @FXML private TextArea contentNoteArea;

    @FXML private Label pleasantnessLabel;
    @FXML private Spinner<Integer> pleasantnessSpinner;
    @FXML private TextArea pleasantnessNoteArea;

    @FXML private Label originalityLabel;
    @FXML private Spinner<Integer> originalitySpinner;
    @FXML private TextArea originalityNoteArea;

    @FXML private Label editionLabel;
    @FXML private Spinner<Integer> editionSpinner;
    @FXML private TextArea editionNoteArea;

    @FXML private Label finalLabel;
    @FXML private Label finalCalculatedLabel;
    @FXML private TextArea finalNoteArea;

    // Chiavi interne per i criteri (non tradotte)
    private static final String STYLE_KEY = "Stile";
    private static final String CONTENT_KEY = "Contenuto";
    private static final String PLEASANTNESS_KEY = "Gradevolezza";
    private static final String ORIGINALITY_KEY = "Originalità";
    private static final String EDITION_KEY = "Edizione";
    private static final String FINAL_KEY = "Voto Finale";

    
    private final Map<String, Integer> punteggi = new HashMap<>();
    private Integer libraryId;
    private final Map<String, TextArea> notePerCriterio = new HashMap<>();
    private final Map<String, Spinner<Integer>> criterioSpinners = new HashMap<>();

    private Book book;
    private boolean editMode = false;
    private Review existingReview;

    public void setLibraryId(Integer id) {
        this.libraryId = id;
    }

    public void setBook(Book book) {
        // Fall back: store book; libraryId can be provided later by LibraryDetailController
        this.book = book;
        initializeFields();
        // Auto-fill if user already has a review for this book
        if (book != null && it.uninsubria.client.utils.classesUI.SessionManager.getInstance().isLoggedIn()) {
            Integer userId = null;
            try {
                userId = SessionManager.getInstance().getUser().getUser_id();
            } catch (Exception ignored) {}
            if (userId != null) {
                final Book b = book;
                final Integer finalUserId = userId;
                 CompletableFuture<List<Review>> future = CompletableFuture.supplyAsync(() -> {
                     try {
                         return it.uninsubria.client.utils.classesUI.ServiceLocator.getReviewsService()
                             .getReviewsForBookAndUser(finalUserId.intValue(), b.getBook_id());
                     } catch (Exception e) {
                         return new ArrayList<Review>();
                     }
                 });
                future.thenAccept(reviews -> {
                    if (reviews != null && !reviews.isEmpty()) {
                        Review existing = reviews.get(0);
                        Platform.runLater(() -> prepareForEdit(b, existing));
                    }
                });
            }
        }
    }

    private void initializeFields() {
        // Init UI state for the book
        logger.info(java.text.MessageFormat.format(resolveString("%review.log.review.for"), (book != null ? book.getTitle() : "null")));
    }

    @FXML
    public void initialize() {
        // Configure spinners
        configureSpinner(styleSpinner, STYLE_KEY);
        configureSpinner(contentSpinner, CONTENT_KEY);
        configureSpinner(pleasantnessSpinner, PLEASANTNESS_KEY);
        configureSpinner(originalitySpinner, ORIGINALITY_KEY);
        configureSpinner(editionSpinner, EDITION_KEY);

        // Configure note areas
        notePerCriterio.put(STYLE_KEY, styleNoteArea);
        notePerCriterio.put(CONTENT_KEY, contentNoteArea);
        notePerCriterio.put(PLEASANTNESS_KEY, pleasantnessNoteArea);
        notePerCriterio.put(ORIGINALITY_KEY, originalityNoteArea);
        notePerCriterio.put(EDITION_KEY, editionNoteArea);
        notePerCriterio.put(FINAL_KEY, finalNoteArea);

        sumbitButton.setOnAction(e -> onInvia());
    }

    private void configureSpinner(Spinner<Integer> spinner, String key) {
        spinner.setEditable(false);
        spinner.getEditor().setDisable(true);
        spinner.getEditor().setStyle("-fx-opacity: 1;");
        punteggi.put(key, 1);
        spinner.valueProperty().addListener((obs, oldVal, newVal) -> punteggi.put(key, newVal));
        criterioSpinners.put(key, spinner);
    }

    // Nuovo metodo per abilitare la modifica esistente
    public void prepareForEdit(Book book, Review review) {
        this.book = book;
        this.editMode = (review != null);
        this.existingReview = review;
        if (review != null) prefillFromReview(review);
    }

    private void prefillFromReview(Review r) {
        if (r == null) return;

        // Prefill spinners and note areas using direct FXML references
        styleSpinner.getValueFactory().setValue(r.getStyleScore());
        styleNoteArea.setText(r.getNoteStyleScore());

        contentSpinner.getValueFactory().setValue(r.getContentScore());
        contentNoteArea.setText(r.getNoteContentScore());

        pleasantnessSpinner.getValueFactory().setValue(r.getPleasantnessScore());
        pleasantnessNoteArea.setText(r.getNotePleasantnessScore());

        originalitySpinner.getValueFactory().setValue(r.getOdnessScore());
        originalityNoteArea.setText(r.getNoteOdnessScore());

        editionSpinner.getValueFactory().setValue(r.getEditionScore());
        editionNoteArea.setText(r.getNoteEditionScore());

        // Per il "Voto Finale" solo la nota
        finalNoteArea.setText(r.getNoteFinalScore());
    }

    private void onInvia() {
        User currentUser = SessionManager.getInstance().getUser();
        Integer userId = (book != null && currentUser != null) ? currentUser.getUser_id() : null;
        if (book == null || userId == null || userId < 0) {
            Platform.runLater(() -> new Alert(Alert.AlertType.WARNING, resolveString("%review.error.user.book.not.selected")).showAndWait());
            return;
        }
        int bookId = book.getBook_id();

        Map<String, String> noteConvertite = notePerCriterio.entrySet().stream().collect(
            Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getText())
        );

        ThreadPoolManager.executeIO(() -> {
            try{
                if (editMode && existingReview != null) {
                    User user = SessionManager.getInstance().getUser();
                    if (user == null) throw new RuntimeException(resolveString("%review.error.user.not.available"));
                    int style = punteggi.get(STYLE_KEY);
                    int content = punteggi.get(CONTENT_KEY);
                    int pleasantness = punteggi.get(PLEASANTNESS_KEY);
                    int odness = punteggi.get(ORIGINALITY_KEY);
                    int edition = punteggi.get(EDITION_KEY);
                    // Calcola automaticamente il voto finale come media degli altri 5 punteggi
                    int finalScore = Math.round((style + content + pleasantness + odness + edition) / 5.0f);
                    Review updated = new Review(user, book, noteConvertite.get(STYLE_KEY),
                        noteConvertite.get(CONTENT_KEY), noteConvertite.get(PLEASANTNESS_KEY),
                        noteConvertite.get(ORIGINALITY_KEY), noteConvertite.get(EDITION_KEY),
                        noteConvertite.get(FINAL_KEY), style, content, pleasantness, odness, edition, finalScore);

                    boolean updateResult = it.uninsubria.client.utils.classesUI.ServiceLocator.getReviewsService()
                            .updateReview(userId, bookId, updated);
                    Platform.runLater(() -> {
                        if (updateResult) {
                            // Alert di successo per aggiornamento recensione esistente
                            Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                            successAlert.setTitle(LanguageManager.getBundle().getString("review.update.success.title"));
                            successAlert.setHeaderText(LanguageManager.getBundle().getString("review.update.success.header"));
                            successAlert.setContentText(LanguageManager.getBundle().getString("review.update.success.message"));
                            successAlert.showAndWait();

                            logger.info(resolveString("%review.log.update.success"));
                            // Forza refresh completo della homepage per riflettere le modifiche
                            viewHome.refreshHomePage();
                            ((Stage) sumbitButton.getScene().getWindow()).close();
                        } else {
                            // Alert di errore per aggiornamento fallito
                            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                            errorAlert.setTitle(LanguageManager.getBundle().getString("review.update.error.title"));
                            errorAlert.setHeaderText(LanguageManager.getBundle().getString("review.update.error.header"));
                            errorAlert.setContentText(LanguageManager.getBundle().getString("review.update.error.message"));
                            errorAlert.showAndWait();

                            System.err.println("❌ Errore durante l'aggiornamento della recensione.");
                        }
                    });
                 } else {
                      // Calcola automaticamente il voto finale come media degli altri 5 punteggi prima del salvataggio
                      int style = punteggi.get(STYLE_KEY);
                      int content = punteggi.get(CONTENT_KEY);
                      int pleasantness = punteggi.get(PLEASANTNESS_KEY);
                      int odness = punteggi.get(ORIGINALITY_KEY);
                      int edition = punteggi.get(EDITION_KEY);
                      int finalScore = Math.round((style + content + pleasantness + odness + edition) / 5.0f);

                      // Aggiorna la mappa punteggi con il voto finale calcolato
                      punteggi.put(FINAL_KEY, finalScore);

                      boolean saveResult = ServiceLocator.getReviewsService().saveReview(
                          userId,
                          bookId,
                          libraryId,
                          punteggi,
                          noteConvertite
                      );

                     Platform.runLater(() -> {
                         if (saveResult) {
                             // Alert di successo per salvataggio nuova recensione
                             Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                            successAlert.setTitle(LanguageManager.getBundle().getString("review.save.success.title"));
                            successAlert.setHeaderText(LanguageManager.getBundle().getString("review.save.success.header"));
                            successAlert.setContentText(LanguageManager.getBundle().getString("review.save.success.message"));
                             successAlert.showAndWait();

                             logger.info(resolveString("%review.log.save.success"));
                             // Forza refresh completo della homepage per riflettere le nuove recensioni
                             viewHome.refreshHomePage();
                             ((Stage) sumbitButton.getScene().getWindow()).close();
                         } else {
                             // Alert di errore per salvataggio fallito
                             Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                            errorAlert.setTitle(LanguageManager.getBundle().getString("review.save.error.title"));
                            errorAlert.setHeaderText(LanguageManager.getBundle().getString("review.save.error.header"));
                            errorAlert.setContentText(LanguageManager.getBundle().getString("review.save.error.message"));
                             errorAlert.showAndWait();

                             System.err.println("❌ Errore durante il salvataggio della recensione.");
                         }
                     });
                }
             } catch(Exception e){
                 Platform.runLater(() -> {
                     // Alert di errore generale per problemi tecnici
                     Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                        errorAlert.setTitle(LanguageManager.getBundle().getString("review.technical.error.title"));
                        errorAlert.setHeaderText(LanguageManager.getBundle().getString("review.technical.error.header"));
                        errorAlert.setContentText(LanguageManager.getBundle().getString("review.technical.error.message"));
                     errorAlert.showAndWait();

                     System.err.println("❌ Errore tecnico durante il salvataggio/update: " + e.getMessage());
                 });
             }
            return null;
        });
    }
}
