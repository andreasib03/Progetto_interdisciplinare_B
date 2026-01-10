package it.uninsubria.client.controller.homepage;

import it.uninsubria.client.controller.ControllerBase;
import it.uninsubria.shared.model.Book;
import it.uninsubria.shared.utils.AppConstants;
import it.uninsubria.client.utils.classesUI.ServiceLocator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

/**
 * Controller semplificato per visualizzare i dettagli di un libro senza recensioni.
 * Usato quando si accede dalla finestra "Nessuna recensione disponibile".
 * Ora mostra anche suggerimenti di libri simili.
 */
public class BookDetailSimpleController extends ControllerBase {

    private static final Logger logger = Logger.getLogger(BookDetailSimpleController.class.getName());

    @FXML
    private Label bookTitleLabel;
    @FXML
    private Label bookAuthorLabel;
    @FXML
    private Label bookYearLabel;
    @FXML
    private Label bookPublisherLabel;
    @FXML
    private Label bookPriceLabel;
    @FXML
    private Label bookCategoryLabel;
    @FXML
    private Label bookIdLabel;
    @FXML
    private TextArea bookDescriptionArea;
    @FXML
    private ScrollPane descriptionScrollPane;
    @FXML
    private Button closeButton;
    @FXML
    private VBox suggestionsVBox;
    @FXML
    private VBox suggestionsContainer;

    private Book currentBook;

    @FXML
    public void initialize() {
        closeButton.setOnAction(e -> closeWindow());
    }

    /**
     * Imposta il libro da visualizzare
     */
    public void setBook(Book book) {
        this.currentBook = book;
        populateBookDetails();
        loadBookSuggestions();
    }

    /**
     * Popola l'interfaccia con i dettagli del libro
     */
    private void populateBookDetails() {
        if (currentBook == null) return;

        // Titolo e autore
        bookTitleLabel.setText(currentBook.getTitle());
        bookAuthorLabel.setText("di " + (currentBook.getAuthors() != null ? currentBook.getAuthors() : resolveString("%bookdetail.unknown.author")));

        // Anno pubblicazione
        if (currentBook.getPublish_date_year() > 0) {
            bookYearLabel.setText(String.valueOf(currentBook.getPublish_date_year()));
        } else {
            bookYearLabel.setText(resolveString("%bookdetail.unknown.year"));
        }

        // Editore
        bookPublisherLabel.setText(currentBook.getPublisher() != null && !currentBook.getPublisher().trim().isEmpty() ?
            currentBook.getPublisher() : resolveString("%bookdetail.unknown.publisher"));

        // Prezzo
        if (currentBook.getPrice() != null && !currentBook.getPrice().trim().isEmpty()) {
            try {
                double priceValue = Double.parseDouble(currentBook.getPrice());
                bookPriceLabel.setText(String.format("%.2f €", priceValue));
            } catch (NumberFormatException e) {
                bookPriceLabel.setText(currentBook.getPrice());
            }
        } else {
            bookPriceLabel.setText(resolveString("%bookdetail.price.unavailable"));
        }

        // Categoria
        bookCategoryLabel.setText(currentBook.getCategory() != null && !currentBook.getCategory().trim().isEmpty() ?
            currentBook.getCategory() : resolveString("%bookdetail.category.unspecified"));

        // ID libro
        bookIdLabel.setText(String.valueOf(currentBook.getBook_id()));

        // Descrizione
        if (currentBook.getDescription() != null && !currentBook.getDescription().trim().isEmpty()) {
            bookDescriptionArea.setText(currentBook.getDescription());
        } else {
            bookDescriptionArea.setText(resolveString("%bookdetail.no.description"));
        }
    }

    /**
     * Carica e mostra suggerimenti di libri simili
     */
    private void loadBookSuggestions() {
        if (currentBook == null) return;

        // Carica suggerimenti in background usando il metodo esistente
        CompletableFuture.supplyAsync(() -> {
            try {
                return ServiceLocator.getBookService().getSuggestedBooks(currentBook.getBook_id());
            } catch (Exception e) {
                logger.warning("Errore nel caricamento suggerimenti: " + e.getMessage());
                return null;
            }
        }).thenAccept((List<Book> suggestions) -> {
            Platform.runLater(() -> {
                if (suggestions != null && !suggestions.isEmpty()) {
                    displaySuggestions(suggestions);
                } else {
                    showNoSuggestionsMessage(resolveString("%bookdetail.suggestions.none"));
                }
            });
        }).exceptionally(throwable -> {
            Platform.runLater(() -> {
                showNoSuggestionsMessage(resolveString("%bookdetail.suggestions.error"));
                logger.warning("Errore nel caricamento suggerimenti: " + throwable.getMessage());
            });
            return null;
        });
    }

    /**
     * Mostra i suggerimenti nell'interfaccia
     */
    private void displaySuggestions(List<Book> suggestions) {
        suggestionsContainer.getChildren().clear();

        for (Book suggestion : suggestions) {
            HBox suggestionBox = new HBox(10);
            suggestionBox.getStyleClass().add("suggestion-item");

            Label titleLabel = new Label(suggestion.getTitle());
            titleLabel.getStyleClass().add("suggestion-title");
            titleLabel.setWrapText(true);

            Label authorLabel = new Label("di " + (suggestion.getAuthors() != null ? suggestion.getAuthors() : resolveString("%bookdetail.unknown.author")));
            authorLabel.getStyleClass().add("suggestion-author");
            authorLabel.setWrapText(true);

            suggestionBox.getChildren().addAll(titleLabel, authorLabel);

            // Aggiungi click handler per aprire il dettaglio del libro suggerito
            suggestionBox.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2) { // Doppio click
                    openBookDetail(suggestion);
                }
            });

            suggestionsContainer.getChildren().add(suggestionBox);
        }
    }

    /**
     * Mostra messaggio quando non ci sono suggerimenti
     */
    private void showNoSuggestionsMessage(String message) {
        suggestionsContainer.getChildren().clear();

        Label noSuggestionsLabel = new Label(message);
        noSuggestionsLabel.getStyleClass().add("no-suggestions-label");
        noSuggestionsLabel.setWrapText(true);

        suggestionsContainer.getChildren().add(noSuggestionsLabel);
    }

    /**
     * Apre la finestra di dettaglio per un libro suggerito
     */
    private void openBookDetail(Book book) {
        try {
            // Riutilizza la stessa logica di viewHome per aprire il dettaglio libro
            // Questo dovrebbe aprire una nuova finestra BookDetailSimple per il libro selezionato
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/views/BookDetailSimpleView.fxml"),
                it.uninsubria.client.utils.classesUI.LanguageManager.getBundle()
            );
            javafx.scene.Parent root = loader.load();

            BookDetailSimpleController controller = loader.getController();
            controller.setBook(book);

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle(resolveString("%book.details.title") + " - " + book.getTitle());
            stage.setScene(new javafx.scene.Scene(root, AppConstants.UI.WINDOW_WIDTH, AppConstants.UI.WINDOW_HEIGHT));
            stage.setResizable(true);
            stage.initModality(javafx.stage.Modality.NONE);
            stage.show();

        } catch (Exception e) {
            logger.severe("Errore nell'apertura dettaglio libro suggerito: " + e.getMessage());
            // Fallback: mostra alert di errore
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle(resolveString("%bookdetail.error.title"));
            alert.setHeaderText(resolveString("%bookdetail.error.header"));
            alert.setContentText(resolveString("%bookdetail.error.message"));
            alert.showAndWait();
        }
    }

    /**
     * Chiude la finestra
     */
    @FXML
    private void closeWindow() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }

    @Override
    public void onLanguageChanged(String newLanguage) {
        // Re-populate book details with new language for localized texts
        if (currentBook != null) {
            populateBookDetails();
        }

        // Update close button text
        if (closeButton != null) {
            // The close button text is set in FXML with %homepage.allreviews.close
            // It should automatically update when the bundle changes
        }
    }
}