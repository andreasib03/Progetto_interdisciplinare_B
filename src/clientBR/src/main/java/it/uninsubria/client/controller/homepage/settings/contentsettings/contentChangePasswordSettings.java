package it.uninsubria.client.controller.homepage.settings.contentsettings;

import java.rmi.RemoteException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import it.uninsubria.client.interfaces.TextReceiver;
import it.uninsubria.client.utils.classesUI.LanguageManager;
import it.uninsubria.client.utils.classesUI.ServiceLocator;
import it.uninsubria.client.utils.classesUI.SessionManager;
import it.uninsubria.client.utils.classesUI.ThreadPoolManager;
import it.uninsubria.shared.model.User;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.util.Duration;

public class contentChangePasswordSettings implements TextReceiver {

    private static final Logger logger = Logger.getLogger(contentChangePasswordSettings.class.getName());

    private ResourceBundle bundle;

    @FXML
    private PasswordField currentPasswordField;

    @FXML
    private TextField newPasswordField;
    @FXML
    private TextField confirmNewPasswordField;
    @FXML
    private Label checkVerifiedPassword;
    @FXML
    private Label passwordCheckNewPasswords;
    @FXML
    private Button buttonSaveChangePassword;

    private boolean currentPasswordValid = false;
    private boolean newPasswordsValid = false;

    private final PauseTransition debounce = new PauseTransition(Duration.millis(600));

    private void onSaveChangePassword() {
        if (!currentPasswordValid || !newPasswordsValid) {
            logger.info("Tentativo di salvataggio con validazioni non superate.");
            return;
        }

        String newPassword = newPasswordField.getText();
        String username = SessionManager.getInstance().getUser().getID();

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                try {
                    return ServiceLocator.getUserService().updatePassword(username, newPassword);
                } catch (RemoteException e) {
                    logger.log(Level.SEVERE, "Remote exception during password update", e);
                    return false;
                }
            }
        };

        task.setOnSucceeded(e -> {
            Boolean updated = task.getValue();
            if (Boolean.TRUE.equals(updated)) {
                setCurrentPasswordSuccess(bundle.getString("settings.password.updated"));
                currentPasswordValid = false;
                setNewPasswordFieldsEnabled(false);
            } else {
                setCurrentPasswordError(bundle.getString("settings.password.update.error"));
            }
            updateButtonStatus();
        });

          task.setOnFailed(e -> {
              logger.log(Level.SEVERE, "Task failed during password update", task.getException());
              setCurrentPasswordError(bundle.getString("settings.password.connection.error"));
              updateButtonStatus();
          });

         ThreadPoolManager.executeCritical(() -> {
             task.run();
         });
    }

    public void initialize(){
        // Initialize resource bundle
        bundle = LanguageManager.getBundle();

        setNewPasswordFieldsEnabled(false);
        buttonSaveChangePassword.setDisable(true);

        currentPasswordField.textProperty().addListener((observable, oldValue, newValue) -> {
            if(currentPasswordValid){
                currentPasswordValid = false;
                clearCurrentPasswordFeedback();
                updateButtonStatus();
            }

            setNewPasswordFieldsEnabled(false);

            debounce.playFromStart();
        });

        debounce.setOnFinished(ev -> {
            String pass = currentPasswordField.getText();
            if(pass != null && !pass.isBlank()){
                verifyCurrentPasswordAsync(pass);
            } else {
                Platform.runLater(() -> {
                    setCurrentPasswordError(bundle.getString("settings.password.current.required"));
                    currentPasswordValid = false;
                    setNewPasswordFieldsEnabled(false);
                    updateButtonStatus();
                });
            }
        });

        currentPasswordField.setOnAction(ev -> {
            debounce.stop();
            String pass = currentPasswordField.getText();
            if (pass != null && !pass.isBlank()) {
                verifyCurrentPasswordAsync(pass);
            } else {
                setCurrentPasswordError(bundle.getString("settings.password.current.required"));
                currentPasswordValid = false;
                setNewPasswordFieldsEnabled(false);
                updateButtonStatus();
            }
        });

        newPasswordField.textProperty().addListener((obs, oldV, newV) -> validateNewPassword());
        confirmNewPasswordField.textProperty().addListener((obs, oldV, newV) -> validateNewPassword());

        buttonSaveChangePassword.setOnAction((event) -> onSaveChangePassword());


    }

    @FXML
    private Label titleLabelsSettings;


    @Override
    public void setTestoDinamico(String testo) {
        titleLabelsSettings.setVisible(true);
        titleLabelsSettings.setText(testo);
    }

    private void setNewPasswordFieldsEnabled(boolean enabled){
        Platform.runLater(() -> {
            newPasswordField.setDisable(!enabled);
            confirmNewPasswordField.setDisable(!enabled);

            if(!enabled){
                newPasswordField.clear();
                confirmNewPasswordField.clear();
                clearNewPasswordsFeedback();
                newPasswordsValid = false;
            }

            updateButtonStatus();
        });
    }

    private void verifyCurrentPasswordAsync(String password){
        String username = SessionManager.getInstance().getUser().getID();

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                try {
                    return ServiceLocator.getUserService().getPasswordByUserID(username, password);
                } catch (RemoteException e) {
                    logger.log(Level.SEVERE, "Remote exception during password check", e);
                    return null;
                }
            }
        };

        task.setOnSucceeded(e -> {
            Boolean result = task.getValue();
            if (result == null) {
                setCurrentPasswordError(bundle.getString("settings.password.connection.error"));
                currentPasswordValid = false;
                setNewPasswordFieldsEnabled(false);
            } else if (result) {
                setCurrentPasswordSuccess(bundle.getString("settings.password.correct"));
                currentPasswordValid = true;
                setNewPasswordFieldsEnabled(true);
            } else {
                setCurrentPasswordError(bundle.getString("settings.password.incorrect"));
                currentPasswordValid = false;
                setNewPasswordFieldsEnabled(false);
            }
            updateButtonStatus();
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            logger.log(Level.SEVERE, "Task failed during password verification", ex);
            setCurrentPasswordError(bundle.getString("settings.password.connection.error"));
            currentPasswordValid = false;
            setNewPasswordFieldsEnabled(false);
            updateButtonStatus();
         });

         ThreadPoolManager.executeCritical(() -> {
             task.run();
         });
     }

    private void validateNewPassword(){

        if (newPasswordField.isDisabled() || confirmNewPasswordField.isDisabled()) {
            newPasswordsValid = false;
            clearNewPasswordsFeedback();
            updateButtonStatus();
            return;
        }

        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmNewPasswordField.getText();

        if(newPassword == null || newPassword.isBlank() || confirmPassword == null || confirmPassword.isBlank()){
            setNewPasswordsError(bundle.getString("settings.password.empty"));
            newPasswordsValid = false;
        } else if(!newPassword.equals(confirmPassword)){
            setNewPasswordsError(bundle.getString("settings.password.mismatch"));
            newPasswordsValid = false;
        } else if(currentPasswordValid && newPassword.equals(currentPasswordField.getText())){
            setNewPasswordsError(bundle.getString("settings.password.same.as.current"));
            newPasswordsValid = false;
        } else if(!User.checkPassword(newPassword)){
            setNewPasswordsError(bundle.getString("settings.password.invalid"));
            newPasswordsValid = false;
        } else {
            setNewPasswordsSuccess(bundle.getString("settings.password.valid"));
            newPasswordsValid = true;
        }

        updateButtonStatus();

    }

    private void updateButtonStatus() {
        Platform.runLater(() -> buttonSaveChangePassword.setDisable(!(currentPasswordValid && newPasswordsValid)));
    }

    // Helper methods for visual feedback
    private void setCurrentPasswordSuccess(String message) {
        Platform.runLater(() -> {
            checkVerifiedPassword.setText(message);
            applyLabelStyle(checkVerifiedPassword, "labelVerde");
        });
    }

    private void setCurrentPasswordError(String message) {
        Platform.runLater(() -> {
            checkVerifiedPassword.setText(message);
            applyLabelStyle(checkVerifiedPassword, "labelRossa");
        });
    }

    private void clearCurrentPasswordFeedback() {
        Platform.runLater(() -> {
            checkVerifiedPassword.setText("");
            clearLabelStyle(checkVerifiedPassword);
        });
    }

    private void setNewPasswordsSuccess(String message) {
        Platform.runLater(() -> {
            passwordCheckNewPasswords.setText(message);
            applyLabelStyle(passwordCheckNewPasswords, "labelVerde");
        });
    }

    private void setNewPasswordsError(String message) {
        Platform.runLater(() -> {
            passwordCheckNewPasswords.setText(message);
            applyLabelStyle(passwordCheckNewPasswords, "labelRossa");
        });
    }

    private void clearNewPasswordsFeedback() {
        Platform.runLater(() -> {
            passwordCheckNewPasswords.setText("");
            clearLabelStyle(passwordCheckNewPasswords);
        });
    }

    private void applyLabelStyle(Label label, String styleClass) {
        ObservableList<String> styles = label.getStyleClass();
        // Remove existing validation styles
        styles.removeAll("labelVerde", "labelRossa");
        // Add the new style
        styles.add(styleClass);
    }

    private void clearLabelStyle(Label label) {
        ObservableList<String> styles = label.getStyleClass();
        styles.removeAll("labelVerde", "labelRossa");
    }

}
