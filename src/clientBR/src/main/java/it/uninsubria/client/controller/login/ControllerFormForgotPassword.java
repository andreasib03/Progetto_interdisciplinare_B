package it.uninsubria.client.controller.login;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import it.uninsubria.shared.model.User;
import it.uninsubria.shared.rmi.UserService;
import it.uninsubria.client.utils.classesUI.LanguageManager;



public class ControllerFormForgotPassword{
    @FXML
    TextField emailField;

    @FXML
    TextField recoveryPhraseField;

    @FXML
    Button sendButton;

    private UserService userService;

    /**
     * Imposta l'UserService per questo controller
     */
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    @FXML
    public void initialize(){

    sendButton.setOnAction(e -> {
            String email = emailField.getText().trim();
            String recoveryPhrase = recoveryPhraseField.getText().trim();

            if (email.isEmpty() || recoveryPhrase.isEmpty()) {
                ControllerLogin.showAlertPopup("Inserisci sia l'email che la frase di recupero");
                return;
            }

            try {
                // Verifica la frase di recupero - hasha la frase inserita dall'utente
                boolean valid = userService.verifyRecoveryPhrase(email, hashRecoveryPhrase(recoveryPhrase));
                if (valid) {
                    // Se valida, mostra dialog per reimpostare password
                    showPasswordResetDialog(email, (Stage) sendButton.getScene().getWindow());
                } else {
                    ControllerLogin.showAlertPopup("Frase di recupero non valida o email non trovata");
                }
            } catch (Exception ex) {
                ControllerLogin.showAlertPopup("Errore durante la verifica: " + ex.getMessage());
            }
        });
    }


        /**
     * Mostra dialog per reimpostare la password
     */
    private void showPasswordResetDialog(String email, Stage parentStage) {
        // Crea dialog per nuova password
        javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog();
        dialog.setTitle(LanguageManager.getBundle().getString("forgot.password.dialog.title"));
        dialog.setHeaderText(LanguageManager.getBundle().getString("forgot.password.dialog.header"));
        dialog.setContentText(LanguageManager.getBundle().getString("forgot.password.dialog.message"));

        // Imposta il campo come PasswordField
        javafx.scene.control.PasswordField passwordField = new javafx.scene.control.PasswordField();
        dialog.getDialogPane().setContent(passwordField);
        passwordField.setPromptText(LanguageManager.getBundle().getString("forgot.password.new.password.prompt"));

        java.util.Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            // Quando si usa un PasswordField personalizzato, dobbiamo ottenere il valore direttamente dal campo
            String newPassword = passwordField.getText().trim();
            if (!newPassword.isEmpty()) {
                // Valida la password con gli stessi controlli della registrazione
                if (User.checkPassword(newPassword)) {
                    try {
                        // Reimposta la password
                        boolean success = userService.resetPassword(email, newPassword);
                        if (success) {
                            ControllerLogin.showSuccessPopup("Password reimpostata con successo!");
                            parentStage.close(); // Chiude la finestra di recupero
                        } else {
                            ControllerLogin.showAlertPopup("Errore nella reimpostazione della password");
                        }
                    } catch (Exception e) {
                        ControllerLogin.showAlertPopup("Errore: " + e.getMessage());
                    }
                } else {
                    ControllerLogin.showAlertPopup("Password non valida. Deve contenere almeno 8 caratteri, una maiuscola, una minuscola, un numero e un carattere speciale.");
                }
            } else {
                ControllerLogin.showAlertPopup("La password non può essere vuota");
            }
        }
    }

    /**
     * Hasher la frase di recupero per sicurezza
     */
    private String hashRecoveryPhrase(String phrase) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(phrase.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Errore nell'hashing della frase di recupero: " + e.getMessage());
            return phrase; // Fallback: salva la frase in chiaro (non sicuro!)
        }
    }
}