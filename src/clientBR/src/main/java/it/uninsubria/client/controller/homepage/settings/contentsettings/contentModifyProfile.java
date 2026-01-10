package it.uninsubria.client.controller.homepage.settings.contentsettings;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import it.uninsubria.client.controller.ControllerBase;
import it.uninsubria.client.utils.classesUI.ServiceLocator;
import it.uninsubria.client.utils.classesUI.SessionManager;
import it.uninsubria.client.utils.classesUI.ResourceCache;
import it.uninsubria.client.utils.classesUI.ThreadPoolManager;
import it.uninsubria.shared.model.User;
import it.uninsubria.shared.utils.LoggerUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import it.uninsubria.client.utils.classesUI.LanguageManager;


public class contentModifyProfile extends ControllerBase {

    @FXML TextField userIDTextField;
    @FXML TextField nameTextField;
    @FXML TextField surnameTextField;
    @FXML TextField emailTextField;
    @FXML TextField CFTextField;

    @FXML
    private ImageView changeImageProfile;

    @FXML
    private Button cancelImageProfile;

    @FXML
    private Button saveButton;

    @FXML
    private ProgressIndicator loadingIndicator;

    private File selectedImageFile;

    @FXML
    public void saveModifyProfileData(){
        // Show loading state
        if (saveButton != null) saveButton.setDisable(true);
        if (loadingIndicator != null) loadingIndicator.setVisible(true);

        User currentUser = SessionManager.getInstance().getUser();

        if (currentUser == null) {
            hideLoadingState();
            showAlert("Errore", "Nessun utente autenticato.", Alert.AlertType.ERROR);
            return;
        }

        String name = nameTextField.getText().trim();
        String surname = surnameTextField.getText().trim();

        if (name.isEmpty() || surname.isEmpty()) {
            hideLoadingState();
            showAlert("Errore", "Nome e cognome sono campi obbligatori.", Alert.AlertType.ERROR);
            return;
        }

        // Solo nome e cognome possono essere modificati per evitare conflitti di unicità
        currentUser.setName(name);
        currentUser.setSurname(surname);

        byte[] imageBytes = null;
        String imageType = null;

  

        if (selectedImageFile != null) {
            try (FileInputStream fis = new FileInputStream(selectedImageFile)) {
                imageBytes = fis.readAllBytes();
                String filename = selectedImageFile.getName().toLowerCase();
                if (filename.endsWith(".png")) imageType = "image/png";
                else if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) imageType = "image/jpeg";
                else if (filename.endsWith(".gif")) imageType = "image/gif";
            } catch (IOException e) {
                showAlert("Errore", "Errore durante la lettura dell'immagine: " + e.getMessage(), Alert.AlertType.ERROR);
                return;
            }
        }

        final byte[] finalImageBytes = imageBytes;
        final String finalImageType = imageType;

        ThreadPoolManager.executeIO(() -> {
            try {
                boolean profileInfoSuccess = true;
                boolean imageSuccess = true;

                // Aggiorna sempre le informazioni del profilo (nome, cognome)
                profileInfoSuccess = ServiceLocator.getUserService().updateProfileInfo(currentUser);

                // Aggiorna l'immagine solo se è stata selezionata
                if (finalImageBytes != null) {
                    imageSuccess = ServiceLocator.getUserService()
                        .updateProfileImage(currentUser, finalImageBytes, finalImageType);
                }

                if (profileInfoSuccess && imageSuccess) {
                    SessionManager.getInstance().setUser(currentUser);

                    if (finalImageBytes != null && imageSuccess) {
                        try {
                            User updatedUser = ServiceLocator.getUserService().getUserByUsernameOrEmail(currentUser.getID());
                            if (updatedUser != null) {
                                currentUser.setProfileImage(updatedUser.getProfileImage());
                                currentUser.setProfileImageType(updatedUser.getProfileImageType());
                                SessionManager.getInstance().setUser(currentUser);
                                if (updatedUser.getProfileImage() != null) {
                                    SessionManager.getInstance().setProfileImageFile(updatedUser.getProfileImage(), updatedUser.getProfileImageType());
                                }
                            }
                        } catch (Exception ex) {
                            LoggerUtil.getLogger(contentModifyProfile.class).warning("Failed to reload profile image from server: " + ex.getMessage());
                        }
                    }

                    javafx.application.Platform.runLater(() -> {
                        hideLoadingState();
                        showAlert("Successo", "Profilo aggiornato correttamente!", Alert.AlertType.INFORMATION);
                    });
                } else {
                    javafx.application.Platform.runLater(() -> {
                        hideLoadingState();
                        showAlert("Errore", "Impossibile aggiornare il profilo.", Alert.AlertType.ERROR);
                    });
                }

            } catch (Exception e) {
                Platform.runLater(() -> {
                    hideLoadingState();
                    showAlert("Errore", "Errore durante l'aggiornamento del profilo: " + e.getMessage(), Alert.AlertType.ERROR);
                });
            }
        });
    }

    private void hideLoadingState() {
        if (saveButton != null) saveButton.setDisable(false);
        if (loadingIndicator != null) loadingIndicator.setVisible(false);
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }


    @FXML
    private void initialize() {


        changeImageProfile.setPickOnBounds(true); // Consente click anche su zone trasparenti
        changeImageProfile.setCursor(Cursor.HAND); // Feedback visivo di "clickabilità"


        changeImageProfile.setImage(ResourceCache.getImage("/images/placeholder_image.png"));

        changeImageProfile.setOnMouseClicked(event -> chooseImage());

        cancelImageProfile.setOnMouseClicked(event -> defaultImage());

        File file = SessionManager.getInstance().getProfileImageFile();
        if (file != null) {
            changeImageProfile.setImage(new Image(file.toURI().toString()));
        }

        // Prima configura lo stato dei campi (abilitati/disabilitati)
        userIDTextField.setDisable(true);
        emailTextField.setDisable(true);
        CFTextField.setDisable(true);
        nameTextField.setDisable(false);
        surnameTextField.setDisable(false);

        User user = SessionManager.getInstance().getUser();
        if (user != null){
            userIDTextField.setText(user.getID());
            nameTextField.setText(user.getName());
            surnameTextField.setText(user.getSurname());
            emailTextField.setText(user.getEmail());
            CFTextField.setText(user.getCF());
        } else {
            // Riprova dopo un breve delay nel caso i dati non siano ancora disponibili
            Platform.runLater(() -> {
                try {
                    Thread.sleep(500); // Aspetta 500ms
                    User retryUser = SessionManager.getInstance().getUser();
                    if (retryUser != null) {
                        userIDTextField.setText(retryUser.getID());
                        nameTextField.setText(retryUser.getName());
                        surnameTextField.setText(retryUser.getSurname());
                        emailTextField.setText(retryUser.getEmail());
                        CFTextField.setText(retryUser.getCF());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        // Add tooltips for accessibility
        userIDTextField.setTooltip(new Tooltip("Il tuo username univoco (non modificabile per mantenere l'unicità)"));
        nameTextField.setTooltip(new Tooltip("Il tuo nome come appare nel profilo"));
        surnameTextField.setTooltip(new Tooltip("Il tuo cognome come appare nel profilo"));
        emailTextField.setTooltip(new Tooltip("Il tuo indirizzo email (non modificabile per mantenere l'unicità)"));
        CFTextField.setTooltip(new Tooltip("Il tuo Codice Fiscale (non modificabile per mantenere l'unicità)"));
        saveButton.setTooltip(new Tooltip("Salva le modifiche al profilo (solo nome e cognome)"));

        // Assicurati che tutti i campi abbiano lo stato corretto
        Platform.runLater(() -> {
            // Campi disabilitati (non modificabili)
            userIDTextField.setDisable(true);
            emailTextField.setDisable(true);
            CFTextField.setDisable(true);

            // Campi abilitati (modificabili)
            nameTextField.setDisable(false);
            surnameTextField.setDisable(false);
        });


    }

    private void chooseImage(){
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(LanguageManager.getBundle().getString("settings.select.image"));
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Immagini", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        Stage stage = (Stage) changeImageProfile.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            selectedImageFile = file;
            Image image = new Image(file.toURI().toString());
            changeImageProfile.setImage(image);
            SessionManager.getInstance().setProfileImageFile(selectedImageFile);
        }
    }

    private void defaultImage(){
        selectedImageFile = null;
        changeImageProfile.setImage(ResourceCache.getImage("/images/placeholder_image.png"));
        SessionManager.getInstance().setProfileImageFile(null);
    }


    public File getSelectedImageFile() {
        return selectedImageFile;
    }

}
