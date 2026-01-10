package it.uninsubria.client.controller.homepage.home;

import java.util.List;
import it.uninsubria.client.controller.ControllerBase;
import it.uninsubria.client.utils.classesUI.ServiceLocator;
import it.uninsubria.client.utils.classesUI.ThreadPoolManager;
import it.uninsubria.shared.model.Book;
import it.uninsubria.shared.model.Review;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.util.logging.Logger;

/**
 * Controller for displaying all reviews of a specific book.
 * Shows detailed ratings with stars and notes for each category.
 */
public class AllReviewsController extends ControllerBase {

    private static final Logger logger = Logger.getLogger(AllReviewsController.class.getName());

    @FXML
    private Label bookTitleLabel;
    @FXML
    private Label bookAuthorLabel;
    @FXML
    private Label totalReviewsLabel;
    @FXML
    private Label averageRatingLabel;
    @FXML
    private VBox reviewsContainer;
    @FXML
    private Button closeButton;

    private Book book;

    @FXML
    public void initialize() {
        // Inizializza il pulsante chiudi
        closeButton.setOnAction(e -> closeWindow());

        // Imposta i testi iniziali delle label
        totalReviewsLabel.setText(resolveString("%allreviews.total.reviews") + ": 0");
        averageRatingLabel.setText(resolveString("%allreviews.average.rating") + ": N/A");
    }

    /**
     * Imposta il libro per cui mostrare le recensioni
     */
    public void setBook(Book book) {
        this.book = book;
        updateBookInfo();
        loadReviews();
    }

    /**
     * Aggiorna le informazioni del libro nell'header
     */
    private void updateBookInfo() {
        if (book != null) {
            bookTitleLabel.setText(book.getTitle());
            bookAuthorLabel.setText(book.getAuthors() + " (" + book.getPublish_date_year() + ")");
        }
    }

    /**
     * Loads all reviews for the book in background thread
     */
    private void loadReviews() {
        if (book == null) return;

        ThreadPoolManager.executeIO(() -> {
            try {
                List<Review> reviews = ServiceLocator.getReviewsService().getReviewsByBook(book.getBook_id());
                Platform.runLater(() -> {
                    displayReviews(reviews);
                    updateStatistics(reviews);
                });
            } catch (Exception e) {
                logger.info("Error loading reviews: " + e.getMessage());
                Platform.runLater(() -> showNoReviewsMessage());
            }
        });
    }


    /**
     * Shows the statistics for reviews
     */
    private void updateStatistics(List<Review> reviews) {
        if (reviews == null || reviews.isEmpty()) {
            totalReviewsLabel.setText(resolveString("%allreviews.total.reviews") + ": 0");
            averageRatingLabel.setText(resolveString("%allreviews.average.rating") + ": N/A");
            return;
        }

        int totalReviews = reviews.size();
        double averageRating = reviews.stream()
                .mapToDouble(Review::averageScore)
                .average()
                .orElse(0.0);

        totalReviewsLabel.setText(resolveString("%allreviews.total.reviews") + ": " + totalReviews);
        averageRatingLabel.setText(resolveString("%allreviews.average.rating") + ": " + String.format("%.1f/5 ★", averageRating));
    }

    /**
     * Displays all reviews in the interface
     */
    private void displayReviews(List<Review> reviews) {
        reviewsContainer.getChildren().clear();

        if (reviews == null || reviews.isEmpty()) {
            showNoReviewsMessage();
            return;
        }

        for (Review review : reviews) {
            VBox reviewBox = createReviewBox(review);
            reviewsContainer.getChildren().add(reviewBox);
        }
    }

    /**
     * Crea un box per una singola recensione
     */
    private VBox createReviewBox(Review review) {
        VBox reviewBox = new VBox();
        reviewBox.setSpacing(8.0);
        reviewBox.getStyleClass().add("reviewBox");

        // Header con utente e stelle
        HBox headerBox = new HBox();
        headerBox.setSpacing(20.0);

        // Utente
        Label userLabel = new Label();
        String userName = review.getUser() != null ?
            (review.getUser().getName() != null ? review.getUser().getName() : review.getUser().getID()) :
            "Utente anonimo";
        userLabel.setText("👤 " + userName);
        userLabel.getStyleClass().add("reviewUserLabel");

        // Stelle
        Label starsLabel = new Label();
        int fullStars = (int) Math.floor(review.averageScore());
        int emptyStars = 5 - fullStars;
        String starsDisplay = "★".repeat(Math.max(1, fullStars)) +
                            "☆".repeat(Math.max(0, emptyStars)) +
                            " (" + String.format("%.1f", review.averageScore()) + "/5)";
        starsLabel.setText(starsDisplay);
        starsLabel.getStyleClass().add("reviewStarsLabel");

        headerBox.getChildren().addAll(userLabel, starsLabel);

        // Note dettagliate
        VBox notesBox = new VBox();
        notesBox.setSpacing(5.0);

        // Aggiungi tutti i punteggi con le relative note
        addScoreWithNote(notesBox, resolveString("%reviews.category.style"), review.getStyleScore(), review.getNoteStyleScore());
        addScoreWithNote(notesBox, resolveString("%reviews.category.content"), review.getContentScore(), review.getNoteContentScore());
        addScoreWithNote(notesBox, resolveString("%reviews.category.pleasantness"), review.getPleasantnessScore(), review.getNotePleasantnessScore());
        addScoreWithNote(notesBox, resolveString("%reviews.category.originality"), review.getOdnessScore(), review.getNoteOdnessScore());
        addScoreWithNote(notesBox, resolveString("%reviews.category.edition"), review.getEditionScore(), review.getNoteEditionScore());

        // Per il voto finale mostra solo la nota (è già mostrato in giallo altrove)
        addNoteOnly(notesBox, resolveString("%reviews.category.final"), review.getNoteFinalScore());

        reviewBox.getChildren().addAll(headerBox, notesBox);
        return reviewBox;
    }

    /**
     * Adds a score with its note if present, displaying stars
     */
    private void addScoreWithNote(VBox container, String label, int score, String note) {
        // Convert score to stars (allow 0 stars for score 0)
        int fullStars = Math.max(0, Math.min(5, score));
        int emptyStars = 5 - fullStars;
        String starsDisplay = "★".repeat(fullStars) + "☆".repeat(emptyStars);

        StringBuilder text = new StringBuilder(label + ": " + starsDisplay + " (" + score + "/5)");
        if (note != null && !note.trim().isEmpty()) {
            text.append(" - ").append(note);
        }

        Label scoreLabel = new Label(text.toString());
        scoreLabel.getStyleClass().add("reviewScoreLabel");
        scoreLabel.setWrapText(true);
        container.getChildren().add(scoreLabel);
    }

    /**
     * Adds only the note for final score (no stars or numeric score)
     */
    private void addNoteOnly(VBox container, String label, String note) {
        if (note != null && !note.trim().isEmpty()) {
            Label noteLabel = new Label(label + ": " + note);
            noteLabel.getStyleClass().add("reviewNoteLabel");
            noteLabel.setWrapText(true);
            container.getChildren().add(noteLabel);
        }
    }


    /**
     * Shows message when no reviews are available
     */
    private void showNoReviewsMessage() {
        Label noReviewsLabel = new Label(resolveString("%homepage.no.reviews"));
        noReviewsLabel.getStyleClass().add("reviewNoReviewsLabel");
        reviewsContainer.getChildren().add(noReviewsLabel);
    }

    /**
     * Closes the window
     */
    @FXML
    private void closeWindow() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }
}