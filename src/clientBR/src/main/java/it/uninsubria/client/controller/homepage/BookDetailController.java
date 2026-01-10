package it.uninsubria.client.controller.homepage;

import it.uninsubria.client.controller.ControllerBase;
import it.uninsubria.client.utils.classesUI.Navigator;
import it.uninsubria.client.utils.classesUI.ParametrizedController;
import it.uninsubria.client.utils.classesUI.ServiceLocator;
import it.uninsubria.shared.model.Book;
import it.uninsubria.shared.model.ReviewManager;
import it.uninsubria.shared.rmi.BookService;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BookDetailController extends ControllerBase implements ParametrizedController {

    private static final Logger logger = Logger.getLogger(BookDetailController.class.getName());

    // Constants for review criteria
    private static final String[] REVIEW_CRITERIA = {
        "Stile", "Contenuto", "Gradevolezza", "Originalità", "Edizione", "Voto Finale"
    };

    // UI Style classes (CSS-based, theme-aware)
    private static final String CRITERION_BOX_CLASS = "criterionBox";
    private static final String COMMENT_BOX_CLASS = "commentBox";
    private static final String SUGGESTION_BOX_CLASS = "suggestionBox";
    private static final String AUTHOR_LABEL_CLASS = "authorLabel";
    private static final String VIEW_BUTTON_CLASS = "viewButton";
    private static final String SECONDARY_LABEL_CLASS = "secondaryLabel";
    private static final String ITALIC_LABEL_CLASS = "italicLabel";

    @FXML private Button backButton;
    @FXML private Label bookTitleLabel;
    @FXML private Label bookAuthorLabel;
    @FXML private Label bookYearLabel;
    @FXML private TextArea bookDescriptionArea;

    @FXML private VBox reviewsSection;
    @FXML private VBox noReviewsBox;
    @FXML private VBox reviewsContent;
    @FXML private VBox averagesBox;
    @FXML private GridPane averagesGrid;
    @FXML private VBox distributionBox;
    @FXML private VBox criteriaDistribution;
    @FXML private VBox commentsBox;
    @FXML private VBox commentsContainer;

    @FXML private VBox suggestionsSection;
    @FXML private VBox suggestionsContainer;

    @FXML private StackPane loadingOverlay;

    private Book currentBook;

    public void setBook(Book book) {
        this.currentBook = book;
        logger.log(Level.INFO, "Loading book details for: " + book.getTitle());

        // Track this book as recently opened
        it.uninsubria.client.utils.classesUI.SessionManager.getInstance().addRecentlyOpenedBook(book);

        // Track user interaction for recommendations
        String userId = it.uninsubria.client.utils.classesUI.SessionManager.getInstance().getUsername();
        if (userId != null) {
            logger.log(Level.FINE, "Tracked book view for user: " + userId + ", book: " + book.getBook_id());
        }

        // Show loading overlay initially
        if (loadingOverlay != null) {
            loadingOverlay.setVisible(true);
        }

        // Load basic book info immediately (synchronous)
        loadBookDetails();

        // Hide loading overlay after basic info is loaded
        Platform.runLater(() -> {
            if (loadingOverlay != null) {
                loadingOverlay.setVisible(false);
            }
        });

        // Load additional data asynchronously (reviews and suggestions)
        loadAdditionalData();
    }

    private void loadAdditionalData() {
        // Load reviews and suggestions in parallel (these can load in background)
        CompletableFuture.runAsync(this::loadReviews);
       CompletableFuture.runAsync(this::loadSuggestions);

        // No need to hide loading here since basic info is already shown
        // Reviews and suggestions can update their sections when ready
    }

    @Override
    public void initData(Map<String, Object> params) {
        if (params != null && params.containsKey("book")) {
            Object bookParam = params.get("book");
            if (bookParam instanceof Book) {
                setBook((Book) bookParam);
            }
        }
    }

    @FXML
    private void initialize() {
        backButton.setOnAction(e -> goBack());
    }

    private void goBack() {
        // Navigate back to previous view
        Navigator.goBack();
    }

    private void loadBookDetails() {
        if (currentBook == null) return;

        bookTitleLabel.setText(currentBook.getTitle());
        bookAuthorLabel.setText(resolveString("%book.detail.by") + " " + currentBook.getAuthors());

        String year = currentBook.getPublish_date_year() > 0 ?
            String.valueOf(currentBook.getPublish_date_year()) : resolveString("%book.detail.unknown.year");
        bookYearLabel.setText(year);

        String description = currentBook.getDescription();
        if (description != null && !description.trim().isEmpty()) {
            bookDescriptionArea.setText(description);
        } else {
            bookDescriptionArea.setText(resolveString("%book.detail.no.description"));
        }
    }

    private void loadReviews() {
        if (currentBook == null) return;

        CompletableFuture.supplyAsync(() -> {
            try {
                BookService bookService = ServiceLocator.getBookService();
                return bookService.getAggregatedReviews(currentBook.getBook_id());
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error loading reviews for book: " + currentBook.getBook_id(), e);
                Platform.runLater(() -> showError("Errore nel caricamento delle recensioni"));
                return List.<ReviewManager>of();
            }
        }).thenAccept(reviewManagers -> {
            Platform.runLater(() -> displayReviews(reviewManagers));
        });
    }

    private void displayReviews(List<ReviewManager> reviewManagers) {
        if (reviewManagers.isEmpty() || reviewManagers.get(0).getReviews().isEmpty()) {
            noReviewsBox.setVisible(true);
            reviewsContent.setVisible(false);
            return;
        }

        noReviewsBox.setVisible(false);
        reviewsContent.setVisible(true);

        ReviewManager rm = reviewManagers.get(0);

        // Display averages
        displayAverages(rm);

        // Display score distribution
        displayScoreDistribution(rm);

        // Display comments
        displayComments(rm);
    }

    private void displayAverages(ReviewManager rm) {
        averagesGrid.getChildren().clear();
        averagesGrid.setHgap(20);
        averagesGrid.setVgap(10);

        double[] averages = {
            rm.getAvgStyle(), rm.getAvgContent(), rm.getAvgPleasantness(),
            rm.getAvgOdness(), rm.getAvgEditions(), rm.getAvgFinal()
        };

        for (int i = 0; i < REVIEW_CRITERIA.length; i++) {
            Label criterionLabel = createBoldLabel(REVIEW_CRITERIA[i] + ":");
            Label averageLabel = new Label(String.format("%.1f ★", averages[i]));
            averageLabel.setFont(Font.font("System", 12));

            averagesGrid.add(criterionLabel, 0, i);
            averagesGrid.add(averageLabel, 1, i);
        }
    }

    private void displayScoreDistribution(ReviewManager rm) {
        criteriaDistribution.getChildren().clear();

        double[] averages = {
            rm.getAvgStyle(), rm.getAvgContent(), rm.getAvgPleasantness(),
            rm.getAvgOdness(), rm.getAvgEditions(), rm.getAvgFinal()
        };

        // For now, show a simple text representation
        // In a real implementation, you'd create progress bars or charts
        for (int i = 0; i < REVIEW_CRITERIA.length; i++) {
            VBox criterionBox = createStyledVBox(5);
            criterionBox.getStyleClass().add(CRITERION_BOX_CLASS);

            Label titleLabel = createBoldLabel(REVIEW_CRITERIA[i]);
            Label distributionLabel = createSecondaryLabel(
                String.format("Media: %.1f - %d recensioni", averages[i], rm.getReviews().size()));

            criterionBox.getChildren().addAll(titleLabel, distributionLabel);
            criteriaDistribution.getChildren().add(criterionBox);
        }
    }

    private void displayComments(ReviewManager rm) {
        commentsContainer.getChildren().clear();

        if (rm.getReviews().isEmpty()) {
            Label noCommentsLabel = createItalicLabel("Nessun commento disponibile.");
            commentsContainer.getChildren().add(noCommentsLabel);
            return;
        }

        for (var review : rm.getReviews()) {
            VBox commentBox = new VBox(8);
            commentBox.getStyleClass().add(COMMENT_BOX_CLASS);

            // User info
            Label userLabel = new Label("Utente " + (review.getUser() != null ?
                review.getUser().getName() + " " + review.getUser().getSurname() : "Anonimo"));
            userLabel.setFont(Font.font("System", FontWeight.BOLD, 11));

            // Comments
            VBox commentsVBox = new VBox(3);

            addCommentIfNotEmpty(commentsVBox, "Stile: ", review.getNoteStyleScore());
            addCommentIfNotEmpty(commentsVBox, "Contenuto: ", review.getNoteContentScore());
            addCommentIfNotEmpty(commentsVBox, "Gradevolezza: ", review.getNotePleasantnessScore());
            addCommentIfNotEmpty(commentsVBox, "Originalità: ", review.getNoteOdnessScore());
            addCommentIfNotEmpty(commentsVBox, "Edizione: ", review.getNoteEditionScore());
            addCommentIfNotEmpty(commentsVBox, "Commento finale: ", review.getNoteFinalScore());

            if (commentsVBox.getChildren().isEmpty()) {
                Label noCommentsLabel = createItalicLabel("Nessun commento testuale.");
                commentsVBox.getChildren().add(noCommentsLabel);
            }

            commentBox.getChildren().addAll(userLabel, commentsVBox);
            commentsContainer.getChildren().add(commentBox);
        }
    }

    private void addCommentIfNotEmpty(VBox container, String prefix, String comment) {
        if (comment != null && !comment.trim().isEmpty()) {
            Label commentLabel = new Label(prefix + comment);
            commentLabel.setFont(Font.font("System", 11));
            commentLabel.setWrapText(true);
            container.getChildren().add(commentLabel);
        }
    }

    private void loadSuggestions() {
        if (currentBook == null) return;

        CompletableFuture.supplyAsync(() -> {
            try {
                BookService bookService = ServiceLocator.getBookService();
                return bookService.getSuggestedBooks(currentBook.getBook_id());
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error loading suggestions for book: " + currentBook.getBook_id(), e);
                Platform.runLater(() -> showError("Errore nel caricamento dei suggerimenti"));
                return List.<Book>of();
            }
        }).thenAccept(suggestions -> {
            Platform.runLater(() -> displaySuggestions(suggestions));
        });
    }

    private void displaySuggestions(List<Book> suggestions) {
        suggestionsContainer.getChildren().clear();

        if (suggestions.isEmpty()) {
            Label noSuggestionsLabel = createItalicLabel("Nessun suggerimento disponibile per questo libro.");
            suggestionsContainer.getChildren().add(noSuggestionsLabel);
            return;
        }

        for (Book book : suggestions) {
            HBox suggestionBox = new HBox(10);
            suggestionBox.getStyleClass().add(SUGGESTION_BOX_CLASS);
            suggestionBox.setAlignment(Pos.CENTER_LEFT);

            VBox bookInfo = new VBox(2);
            Label titleLabel = createBoldLabel(book.getTitle());

            Label authorLabel = new Label("di " + book.getAuthors());
            authorLabel.setFont(Font.font("System", 10));
            authorLabel.getStyleClass().add(AUTHOR_LABEL_CLASS);

            bookInfo.getChildren().addAll(titleLabel, authorLabel);

            Button viewButton = new Button("Visualizza");
            viewButton.getStyleClass().add(VIEW_BUTTON_CLASS);
            viewButton.setOnAction(e -> navigateToBookDetail(book));

            suggestionBox.getChildren().addAll(bookInfo, viewButton);
            HBox.setHgrow(bookInfo, Priority.ALWAYS);

            suggestionsContainer.getChildren().add(suggestionBox);
        }
    }

    // UI Helper methods
    private Label createBoldLabel(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("System", FontWeight.BOLD, 12));
        return label;
    }

    private Label createSecondaryLabel(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("System", 11));
        label.getStyleClass().add(SECONDARY_LABEL_CLASS);
        return label;
    }

    private Label createItalicLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add(ITALIC_LABEL_CLASS);
        return label;
    }

    private VBox createStyledVBox(double spacing) {
        VBox vbox = new VBox(spacing);
        return vbox;
    }

    private void navigateToBookDetail(Book book) {
        // Use Navigator to go to book detail view with the selected book
        Navigator.navigateTo("BookDetailView", Map.of("book", book));
    }

    private void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(resolveString("%alert.error.title"));
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}