package it.uninsubria.client.controller.homepage.settings;

import it.uninsubria.client.controller.ControllerBase;
import it.uninsubria.client.utils.classesUI.AlertMessageCustom;
import javafx.concurrent.Task;
import it.uninsubria.client.utils.classesUI.EveryView;
import it.uninsubria.client.utils.classesUI.Navigator;
import it.uninsubria.client.utils.classesUI.SessionManager;
import it.uninsubria.client.utils.classesUI.UIAnimator;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import it.uninsubria.client.utils.classesUI.ServiceLocator;


public class viewSettings extends ControllerBase {

    private Button activeButton = null;

    @FXML
    private HBox hboxLoadingCSS;

    @FXML
    private Button buttonAccessDenied;
    @FXML
    private Button buttonSettingsWindow;
    @FXML
    private Button buttonSettingsModifyProfile;
    @FXML
    private Button buttonSettingsChangePassword;
    @FXML
    private Button buttonSettingsDeleteAccount;

    @FXML
    private HBox hboxContentModifying;


    private void setActiveButton(Button clickedButton) {
        if (activeButton != null) {
            activeButton.getStyleClass().removeAll("button-filledSettings");
            activeButton.getStyleClass().add("navBarSettings");
        }

        // Applica nuovo stile
        clickedButton.getStyleClass().remove("navBarSettings");
        clickedButton.getStyleClass().add("button-filledSettings");
        UIAnimator.bounceButton(clickedButton, 0);
        activeButton = clickedButton;
    }

    private void loadWindowSettings(){
        handleLoad(buttonSettingsWindow, EveryView.WINDOW.getPath(), false);
    }

    private void loadModifyProfile(){
        handleLoad(buttonSettingsModifyProfile, EveryView.MODIFY.getPath(), true);
    }

    private void loadChangePassword(){
        handleLoad(buttonSettingsChangePassword, EveryView.CHANGE.getPath(), true);
    }

    private void loadDeleteAccount() {
        setActiveButton(buttonSettingsDeleteAccount);
        if(SessionManager.updateLoginInfoButtonVisibility(hboxContentModifying, buttonAccessDenied)){
            if(AlertMessageCustom.showConfirmation("Eliminazione account", "Sei sicuro di voler eliminare il tuo account? Questa azione non può essere annullata.")) {
                String userID = SessionManager.getInstance().getUser().getID();
                // Richiama il backend per eliminare l'account (con cascade gestita a livello DB)
                Task<Boolean> task = new Task<>() {
                    @Override
                    protected Boolean call() {
                        try {
                            return ServiceLocator.getUserService().deleteUser(userID);
                        } catch (Exception e) {
                            return false;
                        }
                    }
                };
                task.setOnSucceeded(e -> {
                    if (Boolean.TRUE.equals(task.getValue())) {
                        AlertMessageCustom.showInfo(null, resolveString("%settings.account.deleted.title"), resolveString("%settings.account.deleted.message"));
                        // Logout forzato dopo eliminazione account
                        SessionManager.getInstance().logout();
                        // Torna alla schermata di login
                        Platform.runLater(() ->{ 
                            Stage stage = (Stage) buttonSettingsDeleteAccount.getScene().getWindow();
                            Navigator.switchScene(stage, EveryView.MAIN.getPath()); 
                        });
                    } else {
                        AlertMessageCustom.showError(null, resolveString("%settings.account.delete.error.title"), resolveString("%settings.account.delete.error.message"));
                    }
                });
                new Thread(task, "delete-account").start();
            } 
        }
    }

    private void handleLoad(Button button, String fxml, boolean checkInfoRequired){
        setActiveButton(button);
        if(!checkInfoRequired || SessionManager.updateLoginInfoButtonVisibility(hboxContentModifying, buttonAccessDenied)){
            Navigator.loadAndSetContent(fxml, button, hboxContentModifying);
        }
    }

    @FXML
    private void onLoadWindowSettings(){
        loadWindowSettings();
    }

    @FXML
    private void onLoadModifyProfile(){
        loadModifyProfile();
    }

    @FXML
    private void onLoadChangePassword(){
        loadChangePassword();
    }

    @FXML
    private void onLoadDeleteaccount(){
        loadDeleteAccount();
    }
}
