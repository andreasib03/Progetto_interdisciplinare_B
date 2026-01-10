package it.uninsubria.client.controller.homepage.help;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import org.json.JSONArray;
import org.json.JSONObject;
    
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ContactFormController {

    // Logger
    private static final Logger logger = Logger.getLogger(ContactFormController.class.getName());

    // Constants
    private static final String FILE_PATH = "richieste.json";
    private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    // Messages
    private static final String ERROR_TITLE = "Errore";
    private static final String ERROR_EMPTY_FIELDS = "Compila tutti i campi";
    private static final String SUCCESS_TITLE = "Salvato";
    private static final String SUCCESS_MESSAGE = "Grazie per la tua opinione!";
    private static final String SAVE_ERROR_MESSAGE = "Errore durante il salvataggio";

    @FXML
    private TextField subjectField;

    @FXML
    private TextArea messageArea;
    
    @FXML
    private void onSendClicked() {
        String subject = subjectField.getText().trim();
        String message = messageArea.getText().trim();

        if (subject.isEmpty() || message.isEmpty()) {
            showAlert(ERROR_TITLE, ERROR_EMPTY_FIELDS, Alert.AlertType.WARNING);
            return;
        }

        boolean success = saveRequestToJson(subject, message);
        if (success) {
            showAlert(SUCCESS_TITLE, SUCCESS_MESSAGE, Alert.AlertType.INFORMATION);
            subjectField.clear();
            messageArea.clear();
            Stage stage = (Stage) subjectField.getScene().getWindow();
            stage.close();
        } else {
            showAlert(ERROR_TITLE, SAVE_ERROR_MESSAGE, Alert.AlertType.ERROR);
        }
    }
    
    private boolean saveRequestToJson(String subject, String message) {
        try {
            JSONArray array;
    
                // Se il file esiste, lo legge. Altrimenti crea nuovo array
            if (Files.exists(Paths.get(FILE_PATH))) {
                String content = new String(Files.readAllBytes(Paths.get(FILE_PATH)));
                array = new JSONArray(content);
            } else {
                array = new JSONArray();
            }
    
            JSONObject newRequest = new JSONObject();
            newRequest.put("data", LocalDateTime.now().format(DateTimeFormatter.ofPattern(DATE_PATTERN)));
            newRequest.put("oggetto", subject);
            newRequest.put("messaggio", message);

            array.put(newRequest);

            try (FileWriter file = new FileWriter(FILE_PATH)) {
                file.write(array.toString(4)); // 4 = indentazione
            }

            logger.info("Contact request saved successfully");
            return true;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error saving contact request", e);
            return false;
        }
    }
    
    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
