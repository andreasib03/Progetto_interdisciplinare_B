package it.uninsubria.client.utils.classesUI;

import java.net.URL;
import java.util.Optional;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.stage.Window;

public class AlertMessageCustom {

    /**
     * Mostra un messaggio di informazione.
     * @param owner la finestra proprietaria (può essere null)
     * @param title titolo della finestra
     * @param message testo del messaggio
     */
    public static void showInfo(Window owner, String title, String message) {
        showAlert(AlertType.INFORMATION, owner, title, message);
    }
    
    private Alert alert;

    public void errorMessage(String message) {
        alert = new Alert(AlertType.ERROR);
        alert.setTitle(LanguageManager.getBundle().getString("alert.error.title"));
        alert.setHeaderText(null);
        alert.setContentText(message);

        alert.getDialogPane().setStyle("-fx-font-family: 'Arial'; -fx-font-size: 14px; -fx-padding: 10px;");
        // Aggiungi uno stile per i bottoni
        alert.getDialogPane().lookupButton(ButtonType.OK).setStyle("-fx-font-size: 14px; -fx-background-color: #f44336; -fx-text-fill: white; -fx-padding: 5px 10px;");
        alert.showAndWait();
    }

    public void successMessage(String message){
        alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(LanguageManager.getBundle().getString("alert.success.title"));
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().setStyle("-fx-font-family: 'Arial'; -fx-font-size: 14px; -fx-padding: 10px;");
        // Aggiungi uno stile per i bottoni
        alert.getDialogPane().lookupButton(ButtonType.OK).setStyle("-fx-font-size: 14px; -fx-background-color: #f44336; -fx-text-fill: white; -fx-padding: 5px 10px;");
        alert.showAndWait();
    }

    /**
     * Mostra un messaggio di avviso.
     * @param owner la finestra proprietaria (può essere null)
     * @param title titolo della finestra
     * @param message testo del messaggio
     */
    public static void showWarning(Window owner, String title, String message) {
        showAlert(AlertType.WARNING, owner, title, message);
    }

    /**
     * Mostra un messaggio di errore.
     * @param owner la finestra proprietaria (può essere null)
     * @param title titolo della finestra
     * @param message testo del messaggio
     */
    public static void showError(Window owner, String title, String message) {
        showAlert(AlertType.ERROR, owner, title, message);
    }

    private static void showAlert(AlertType type, Window owner, String title, String message) {
        Alert alert = new Alert(type);
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

     public static boolean showConfirmation(Window owner, String title, String message) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    public static boolean showConfirmation(String title, String message) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        DialogPane dialogPane = alert.getDialogPane();
        URL cssResource = AlertMessageCustom.class.getResource("/css/CommonFeatures.css");
        if (cssResource != null) {
            dialogPane.getStylesheets().add(cssResource.toExternalForm());
        } else {
            System.err.println("CSS file not found!");
        }

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

}
