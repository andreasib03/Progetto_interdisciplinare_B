package it.uninsubria.client.controller.homepage.settings.contentsettings;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import it.uninsubria.client.controller.ControllerBase;
import it.uninsubria.client.interfaces.TextReceiver;
import it.uninsubria.client.utils.classesUI.BackupManager.BackupInfo;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import it.uninsubria.client.utils.classesUI.BackupManager;
import it.uninsubria.client.utils.classesUI.EncryptionManager;
import it.uninsubria.client.utils.classesUI.LanguageManager;
import it.uninsubria.client.utils.classesUI.SessionManager;
import it.uninsubria.client.utils.classesUI.ThemeManager;
import it.uninsubria.client.utils.classesUI.ThreadPoolManager;
import it.uninsubria.client.utils.classesUI.TimeoutManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;

public class contentWindowSettings extends ControllerBase implements TextReceiver {

        @FXML
        private Label titleLabelsSettings;

        @FXML
        private ComboBox<String> languageComboBox;

        @FXML
        private Button lightButton;
        @FXML
        private Button darkButton;

        @FXML
        private ComboBox<String> timeoutComboBox;

    @FXML
    private Button backupButton;

    @FXML
    private Button openBackupFolderButton;

        @FXML
        private Button restoreButton;

        @FXML
        private Label backupStatusLabel;

        @FXML
        private HBox HBoxBackupRestore;


        @Override
        public void setTestoDinamico(String testo) {
            titleLabelsSettings.setVisible(true);
            titleLabelsSettings.setText(testo);
        }


        @FXML
        private void onLanguageSelected() throws IOException {
            // Show informational message about language change
            showInfoAlert(resolveString("%settings.language.change.instruction"));
        }

        @FXML
        public void initialize() {
            languageComboBox.getItems().addAll("Italiano", "English");

            String currentLang = LanguageManager.getLanguage();
            if("en".equals(currentLang)){
                languageComboBox.setValue("English");
            } else {
                languageComboBox.setValue("Italiano");
            }

            // Disable language change in settings - can only be changed in launcher
            languageComboBox.setDisable(true);

            // Add tooltip to explain why it's disabled
            languageComboBox.setTooltip(new Tooltip(resolveString("%settings.language.disabled.tooltip")));

            setupLightMode();
            setupDarkMode();

            if(ThemeManager.isDarkModeEnabled()){
                setActiveButton(darkButton);
            } else {
                setActiveButton(lightButton);
            }

            setupTimeoutOptions();
            setupBackupRestoreVisibility();

            // Initial UI text setup
            refreshUITexts();
        }

        private Button activeButton = null;

        private void setActiveButton(Button clickedButton){

            if(activeButton != null){
                activeButton.getStyleClass().removeAll("selectedButton");
                activeButton.getStyleClass().add("noSelectedButton");
            }

            clickedButton.getStyleClass().remove("noSelectedButton");
            clickedButton.getStyleClass().add("selectedButton");

            activeButton = clickedButton;
        }

        private void setupLightMode(){
            lightButton.setOnAction(e -> {
                ThemeManager.setDarkMode(false, lightButton.getScene());
                setActiveButton(lightButton);
            });
        }

        private void setupDarkMode(){
            darkButton.setOnAction(e -> {
                ThemeManager.setDarkMode(true, darkButton.getScene());
                setActiveButton(darkButton);
            });
        }

            
        
        private String getLocalizedTimeoutOption(int minutes) {
            switch (minutes) {
                case 5: return resolveString("%settings.timeout.option1");
                case 10: return resolveString("%settings.timeout.option2");
                case 15: return resolveString("%settings.timeout.option3");
                case 30: return resolveString("%settings.timeout.option4");
                case 0: return resolveString("%settings.timeout.option5");
                default: return "";
            }
        }

        private int getTimeoutMinutesFromString(String timeoutString) {
            if (timeoutString.equals(getLocalizedTimeoutOption(5))) return 5;
            if (timeoutString.equals(getLocalizedTimeoutOption(10))) return 10;
            if (timeoutString.equals(getLocalizedTimeoutOption(15))) return 15;
            if (timeoutString.equals(getLocalizedTimeoutOption(30))) return 30;
            return 0; // NEVER
        }

        private void setupBackupRestoreVisibility() {
            String userId = SessionManager.getInstance().getUsername();
            if(userId == null || userId.isEmpty()) {
                HBoxBackupRestore.setVisible(false);
            } else {
                HBoxBackupRestore.setVisible(true);
            }
        }

        /**
         * Apre la cartella dei backup nel file manager del sistema
         */
        @FXML
        private void onOpenBackupFolder() {
            try {
                BackupManager.openBackupFolder();
            } catch (Exception e) {
                showErrorAlert(resolveString("%settings.backup.access.error") + ": " + e.getMessage());
            }
        }

        private void setupTimeoutOptions() {
            refreshTimeoutOptions();
        }

        private void refreshTimeoutOptions() {
            // Clear existing items
            timeoutComboBox.getItems().clear();

            // Aggiungi gli elementi al ComboBox con la lingua corrente
            timeoutComboBox.getItems().addAll(getLocalizedTimeoutOption(5), getLocalizedTimeoutOption(10),
                                              getLocalizedTimeoutOption(15), getLocalizedTimeoutOption(30), getLocalizedTimeoutOption(0));

            // Carica l'impostazione corrente dell'utente
            String currentTimeout = TimeoutManager.getInstance().getCurrentTimeoutString();

            // Imposta il valore solo se è diverso da "Mai" (per mostrare il prompt text quando possibile)
            if (currentTimeout != null && !currentTimeout.isEmpty() && !currentTimeout.equals(getLocalizedTimeoutOption(0))) {
                timeoutComboBox.setValue(currentTimeout);
            }
            // Se è "Mai" o null, lascia il ComboBox senza valore per mostrare il prompt text "Mai"
        }

        private void refreshUITexts() {
            // Refresh timeout options with new language
            refreshTimeoutOptions();

            // Update prompt text for timeout ComboBox
            timeoutComboBox.setPromptText(getLocalizedTimeoutOption(0));

            // Note: Other UI elements in this settings panel are mostly labels and buttons
            // that don't need dynamic text updates as they are static or use resolveString
        }

        @FXML
        private void onTimeoutSelected() {
            String selectedTimeout = timeoutComboBox.getValue();
            if (selectedTimeout != null) {
                int minutes = getTimeoutMinutesFromString(selectedTimeout);
                TimeoutManager.getInstance().setTimeoutMinutes(minutes);
            }
        }




        @FXML
        private void onBackupAction() {
            String userId = SessionManager.getInstance().getUsername();
            if(userId == null || userId.isEmpty()) {
                showErrorAlert(resolveString("%settings.backup.login.required"));
                return;
            } else {
                // Verifica che la directory di backup sia accessibile
                if (!BackupManager.isBackupDirectoryAccessible()) {
                    showErrorAlert(resolveString("%settings.backup.directory.access.error"));
                    return;
                }

                // Chiedi tipo di backup
                ChoiceDialog<String> choiceDialog = new ChoiceDialog<>("Standard", "Standard", "Crittografato");
                choiceDialog.setTitle(resolveString("%settings.backup.type.title"));
                choiceDialog.setHeaderText(resolveString("%settings.backup.type.header"));
                choiceDialog.setContentText(resolveString("%settings.backup.type.label"));

                Optional<String> result = choiceDialog.showAndWait();
                if (!result.isPresent()) {
                    return; // Utente ha annullato
                }

                // Disabilita il pulsante durante l'operazione
                backupButton.setDisable(true);

                boolean encrypted = "Crittografato".equals(result.get());
                String password = null;

                if (encrypted) {
                    // Chiedi password per backup crittografato
                    TextInputDialog passwordDialog = new TextInputDialog();
                    passwordDialog.setTitle(resolveString("%settings.backup.password.title"));
                    passwordDialog.setHeaderText(resolveString("%settings.backup.password.header"));
                    passwordDialog.setContentText(resolveString("%settings.backup.password.label"));

                    // Crea campo password
                    PasswordField passwordField = new PasswordField();
                    passwordDialog.getDialogPane().setContent(passwordField);
                    passwordDialog.setContentText(null);

                    Optional<String> passwordResult = passwordDialog.showAndWait();
                    if (!passwordResult.isPresent() || passwordResult.get().trim().isEmpty()) {
                        backupButton.setDisable(false); // Riabilita il pulsante se annullato
                        return; // Utente ha annullato
                    }

                    password = passwordResult.get().trim();

                    // Verifica forza password
                    if (!EncryptionManager.isPasswordStrong(password)) {
                        showErrorAlert(resolveString("%settings.backup.password.weak"));
                        return;
                    }

                    // Conferma password
                    TextInputDialog confirmDialog = new TextInputDialog();
                    confirmDialog.setTitle(resolveString("%settings.backup.confirm.title"));
                    confirmDialog.setHeaderText(resolveString("%settings.backup.confirm.header"));
                    PasswordField confirmField = new PasswordField();
                    confirmDialog.getDialogPane().setContent(confirmField);
                    confirmDialog.setContentText(null);

                    Optional<String> confirmResult = confirmDialog.showAndWait();
                    if (!confirmResult.isPresent() || !password.equals(confirmResult.get())) {
                        showErrorAlert(resolveString("%settings.backup.password.mismatch"));
                        backupButton.setDisable(false); // Riabilita il pulsante se c'è un errore
                        return;
                    }
                }

                final String finalPassword = password;
                final boolean finalEncrypted = encrypted;

                // Disabilita il pulsante durante l'operazione
                backupButton.setDisable(true);
                backupStatusLabel.setText(resolveString("%settings.backup.creation.progress"));

                ThreadPoolManager.executeIO(() -> {
                    try {
                        String backupPath;
                        if (finalEncrypted) {
                            backupPath = BackupManager.createEncryptedBackup(userId, finalPassword);
                        } else {
                            backupPath = BackupManager.createBackup(userId);
                        }
                        return backupPath;
                    } catch (Exception e) {
                        throw new RuntimeException("Errore durante la creazione del backup: " + e.getMessage(), e);
                    }
                }).thenAccept(backupPath -> {
                    Platform.runLater(() -> {
                        backupButton.setDisable(false);
                        String backupType = finalEncrypted ? resolveString("%settings.backup.encrypted.label") : "";
                        backupStatusLabel.setText(java.text.MessageFormat.format(resolveString("%settings.backup.creation.success.status"), backupType, new File(backupPath).getName()));

                        // Mostra dialogo di conferma
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle(resolveString("%settings.backup.success.title"));
                        alert.setHeaderText(resolveString("%settings.backup.success.header"));
                        alert.setContentText(resolveString("%settings.backup.location") + "\n" + backupPath +
                            (finalEncrypted ? "\n\n" + resolveString("%settings.backup.encrypted.warning") : ""));
                        alert.showAndWait();
                    });
                }).exceptionally(throwable -> {
                    Platform.runLater(() -> {
                        backupButton.setDisable(false);
                        backupStatusLabel.setText(resolveString("%settings.backup.creation.error"));
                        showErrorAlert(resolveString("%settings.backup.creation.error") + ": " + throwable.getMessage());
                    });
                    return null;
                });
            }
        }

        @FXML
        private void onRestoreAction() {
            String userId = SessionManager.getInstance().getUsername();
            if (userId == null || userId.isEmpty()) {
                showErrorAlert(resolveString("%settings.backup.restore.login.required"));
                return;
            }

            try {
                // Ottieni lista dei backup disponibili
                List<BackupInfo> backups = BackupManager.listBackups(userId);

                if (backups.isEmpty()) {
                    showErrorAlert(resolveString("%settings.backup.not.found"));
                    return;
                }

                // Per ora, ripristina l'ultimo backup disponibile
                // In futuro potremmo aggiungere una UI per selezionare il backup
                BackupInfo latestBackup = backups.get(0);

                String password = null;
                if (latestBackup.encrypted) {
                    // Chiedi password per backup crittografato
                    TextInputDialog passwordDialog = new TextInputDialog();
                    passwordDialog.setTitle(resolveString("%settings.backup.restore.password.title"));
                    passwordDialog.setHeaderText(resolveString("%settings.backup.restore.password.header"));
                    PasswordField passwordField = new PasswordField();
                    passwordDialog.getDialogPane().setContent(passwordField);
                    passwordDialog.setContentText(null);

                    Optional<String> passwordResult = passwordDialog.showAndWait();
                    if (!passwordResult.isPresent() || passwordResult.get().trim().isEmpty()) {
                        restoreButton.setDisable(false); // Riabilita il pulsante se annullato
                        return; // Utente ha annullato
                    }
                    password = passwordResult.get().trim();
                }

                // Chiedi conferma
                Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                confirmAlert.setTitle(resolveString("%settings.backup.restore.confirm.title"));
                confirmAlert.setHeaderText(resolveString("%settings.backup.restore.confirm.header"));
                String contentText = resolveString("%settings.backup.info") + " " + latestBackup.toString() + "\n\n" + resolveString("%settings.backup.restore.warning");
                if (latestBackup.encrypted) {
                    contentText += "\n\n" + resolveString("%settings.backup.restore.encrypted.info");
                }
                contentText += "\n\n" + resolveString("%settings.backup.restore.confirm");
                confirmAlert.setContentText(contentText);

                 if (confirmAlert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
                     restoreButton.setDisable(false); // Riabilita il pulsante se annullato
                     return;
                 }

                // Disabilita il pulsante durante l'operazione
                restoreButton.setDisable(true);
                backupStatusLabel.setText(resolveString("%settings.backup.restore.progress"));

                final String finalPassword = password;
                final boolean isEncrypted = latestBackup.encrypted;

                ThreadPoolManager.executeIO(() -> {
                    try {
                        if (isEncrypted) {
                            BackupManager.restoreEncryptedBackup(latestBackup.filePath, userId, finalPassword);
                        } else {
                            BackupManager.restoreBackup(latestBackup.filePath, userId);
                        }
                        return true;
                    } catch (Exception e) {
                        throw new RuntimeException("Errore durante il ripristino del backup: " + e.getMessage(), e);
                    }
                }).thenAccept(success -> {
                    Platform.runLater(() -> {
                        restoreButton.setDisable(false);
                        backupStatusLabel.setText(resolveString("%settings.backup.restore.success.status"));

                        // Mostra dialogo di conferma e richiedi riavvio
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle(resolveString("%settings.backup.restore.success.title"));
                        alert.setHeaderText(resolveString("%settings.backup.restore.success.header"));
                        alert.setContentText(resolveString("%settings.backup.restore.success.message"));
                        alert.showAndWait();

                        // Riavvia l'applicazione
                        System.exit(0);
                    });
                }).exceptionally(throwable -> {
                    Platform.runLater(() -> {
                        restoreButton.setDisable(false);
                        backupStatusLabel.setText(resolveString("%settings.backup.restore.error"));
                        showErrorAlert(resolveString("%settings.backup.restore.error") + ": " + throwable.getMessage());
                    });
                    return null;
                });

            } catch (Exception e) {
                showErrorAlert(resolveString("%settings.backup.access.error") + ": " + e.getMessage());
            }
        }

        private void showErrorAlert(String message) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(resolveString("%settings.error.title"));
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        }

        private void showInfoAlert(String message) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(resolveString("%settings.info.title"));
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        }

        /*private void applyStylesheet() {
         //   Scene scene = darkModeToggle.getScene();
            // if (!scene.getStylesheets().contains(getClass().getResource(EveryView.CSS_SETTINGS.getPath()).toExternalForm())) {
              //   scene.getStylesheets().add(getClass().getResource(EveryView.CSS_SETTINGS.getPath()).toExternalForm());
             //}
         }*/


}
