package it.uninsubria.client.controller.registration;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.stream.Stream;
import java.util.logging.Logger;

import org.kordamp.ikonli.javafx.FontIcon;

import it.uninsubria.client.controller.ControllerBase;
import javafx.scene.control.TextField;
import it.uninsubria.client.controller.login.ControllerLogin;
import it.uninsubria.client.utils.classesLogic.VideoUtils;
import it.uninsubria.client.utils.classesUI.AlertMessageCustom;
import it.uninsubria.client.utils.classesUI.EveryView;
import it.uninsubria.client.utils.classesUI.InputValidatorClient;
import it.uninsubria.client.utils.classesUI.LanguageManager;
import it.uninsubria.client.utils.classesUI.Navigator;
import it.uninsubria.client.utils.classesUI.ServiceLocator;
import it.uninsubria.shared.model.User;
import it.uninsubria.shared.rmi.UserService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class ControllerRegistration extends ControllerBase{
    private static final Logger logger = Logger.getLogger(ControllerRegistration.class.getName());

    @FXML
    private Text passwordRequirementsText;
    @FXML
    private TextField ID_codeField;
    @FXML
    private TextField nameField;
    @FXML
    private TextField surnameField;
    @FXML
    private TextField emailField;
    @FXML
    private TextField CF_Field;

    // Campo rimosso - la frase di recupero sarà inserita tramite dialog

    @FXML
    private TextField passwordTextField;
    @FXML
    private PasswordField passwordField;

    @FXML
    private FontIcon isVisibleOrNot;

    @FXML
    private Hyperlink hyperLinkBackLogin;

    @FXML
    private Button cancelRegistration;

    @FXML
    private Button registerButton; // segnalare per vedere se funziona il tutto per caratteri ecc.

    @FXML
    private VBox videoVBox;

    private boolean isVisible = false;
    AlertMessageCustom alertMessage = new AlertMessageCustom();
    private final UserService userService = ServiceLocator.getUserService();

    private void setupPasswordFieldToggle() {
        passwordTextField.textProperty().bindBidirectional(passwordField.textProperty());
        passwordTextField.setVisible(false);
        passwordTextField.setManaged(false);
        passwordField.setVisible(true);
        passwordField.setManaged(true);
    }

    private void setupPasswordValidation() {
        passwordField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (User.checkPassword(newVal)) {
                passwordRequirementsText.setFill(javafx.scene.paint.Color.GREEN);
                passwordRequirementsText.setText(LanguageManager.getBundle().getString("register.password.valid"));
            } else {
                passwordRequirementsText.setFill(javafx.scene.paint.Color.RED);
                passwordRequirementsText.setText(LanguageManager.getBundle().getString("register.password.not.valid"));
            }
        });
    }

    @FXML
    private void togglePasswordVisibility(){

        isVisible = !isVisible;
        passwordTextField.setVisible(isVisible);
        passwordTextField.setManaged(isVisible);
        passwordField.setVisible(!isVisible);
        passwordField.setManaged(!isVisible);
        isVisibleOrNot.setIconLiteral(isVisible ? "far-eye" : "fas-low-vision");

        if (isVisible) {
            passwordTextField.requestFocus();
            passwordTextField.positionCaret(passwordTextField.getText().length());
        } else {
            passwordField.requestFocus();
            passwordField.positionCaret(passwordField.getText().length());
        }
    }

    /**
     * Mostra un dialog per chiedere se l'utente vuole aggiungere una frase di recupero
     * e, in caso affermativo, permette di inserirla
     * @return la frase di recupero inserita dall'utente, o null se rifiuta
     */
    private String askForRecoveryPhrase() {
        // Alert di conferma iniziale
        javafx.scene.control.Alert confirmAlert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle(resolveString("%register.phrase"));
        confirmAlert.setHeaderText(resolveString("%register.phrase.title"));
        confirmAlert.setContentText(resolveString("%register.phrase.desc") + "\n\n" +
                                   resolveString("%register.phrase.desc.important"));

        // Personalizza i pulsanti
        confirmAlert.getButtonTypes().setAll(
            new javafx.scene.control.ButtonType(resolveString("%register.phrase.button"), javafx.scene.control.ButtonBar.ButtonData.YES),
            new javafx.scene.control.ButtonType(resolveString("%register.phrase.buttonCancel"), javafx.scene.control.ButtonBar.ButtonData.NO)
        );

        // Mostra l'alert e aspetta la risposta
        java.util.Optional<javafx.scene.control.ButtonType> result = confirmAlert.showAndWait();
        if (!result.isPresent() || result.get().getButtonData() != javafx.scene.control.ButtonBar.ButtonData.YES) {
            return null; // Utente ha rifiutato
        }

        // Se l'utente ha confermato, mostra un dialog per inserire la frase
        javafx.scene.control.Alert phraseAlert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.NONE);
        phraseAlert.setTitle(resolveString("%register.phrase"));
        phraseAlert.setHeaderText(resolveString("%register.phrase.phrase.warning"));
        phraseAlert.setContentText(resolveString("%register.phrase.phrase.info") + "\n\n" +
                                   resolveString("%register.phrase.phrase.example"));

        // Crea un TextField personalizzato
        javafx.scene.control.TextField phraseField = new javafx.scene.control.TextField();
        phraseField.setPromptText(resolveString("%register.phrase.phrase.title"));
        phraseAlert.getDialogPane().setContent(phraseField);

        // Pulsanti personalizzati
        javafx.scene.control.ButtonType confirmButton = new javafx.scene.control.ButtonType(resolveString("%register.phrase.phrase.button"), javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        javafx.scene.control.ButtonType backButton = new javafx.scene.control.ButtonType(resolveString("%register.phrase.phrase.back"), javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);

        phraseAlert.getButtonTypes().setAll(confirmButton, backButton);

        java.util.Optional<javafx.scene.control.ButtonType> alertResult = phraseAlert.showAndWait();
        if (alertResult.isPresent() && alertResult.get() == confirmButton) {
            String phrase = phraseField.getText().trim();
            if (phrase.isEmpty()) {
                alertMessage.errorMessage(resolveString("%register.phrase.phrase.info.warning"));
                return null;
            }
            return phrase;
        }

        return null; // Utente ha annullato
    }

    @FXML
    public void initialize(){
        passwordRequirementsText.getStyleClass().add("notValid");
        setupPasswordFieldToggle();
        setupPasswordValidation();
        VideoUtils.setupBackgroundVideo(videoVBox, "/video/Background_Login_video.mp4", 70, 70);

        // Il campo recoveryPhraseField è stato rimosso dall'FXML

        Platform.runLater(() ->{
            Stage stage = (Stage) cancelRegistration.getScene().getWindow();
            stage.setOnCloseRequest(event -> {
                Scene currentScene = stage.getScene();
                String rootId = currentScene.getRoot().getId();
                if("root".equals(rootId)){
                    Platform.exit();
                } else {
                    Navigator.switchScene(stage, EveryView.LOGIN.getPath());
                    event.consume();
                }
            });
        });

        registerButton.setOnAction(e -> {
            handleRegistration();
        });

    }

    public void setOnLoginSuccess(Runnable callback) {}


    @FXML
    public void loginView() throws IOException{

        Navigator.switchScene(
            (Stage) hyperLinkBackLogin.getScene().getWindow(),
            EveryView.LOGIN.getPath(),
            controller -> {
                if(controller instanceof ControllerLogin loginController){
                    loginController.setOnLoginSuccess(() -> {
                        // goToMainScene() già ricarica tutto, updateUserInfo() non necessario
                        Navigator.goToMainScene();
                    });
                }
            }
        );

    }

    @FXML
    public void closeRegistration(){
        Stage stage = (Stage) cancelRegistration.getScene().getWindow();
        Navigator.switchScene(stage, EveryView.LOGIN.getPath());
    }

    // Metodo askingPhrase() rimosso - sostituito da askForRecoveryPhrase()

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


    private void handleRegistration(){
        String email = emailField.getText();
        String CF = CF_Field.getText();
        String nome = nameField.getText();
        String cognome = surnameField.getText();
        String password = passwordField.getText();
        String ID = ID_codeField.getText();

        // Controllo che tutti i campi siano compilati
        if (Stream.of(ID, nome, cognome, email, CF, password).anyMatch(String::isEmpty)) {
            alertMessage.errorMessage(resolveString("%register.fields.required"));
            return;
        }

        // Validazione input con messaggi specifici
        var emailValidation = InputValidatorClient.validateEmail(email);
        if (!emailValidation.isValid()) {
            alertMessage.errorMessage(emailValidation.getErrorMessage());
            return;
        }

        var nameValidation = InputValidatorClient.validateName(nome);
        if (!nameValidation.isValid()) {
            alertMessage.errorMessage(nameValidation.getErrorMessage());
            return;
        }

        var surnameValidation = InputValidatorClient.validateName(cognome);
        if (!surnameValidation.isValid()) {
            alertMessage.errorMessage(surnameValidation.getErrorMessage());
            return;
        }

        var fiscalCodeValidation = InputValidatorClient.validateFiscalCode(CF);
        if (!fiscalCodeValidation.isValid()) {
            alertMessage.errorMessage(fiscalCodeValidation.getErrorMessage());
            return;
        }

        var passwordValidation = InputValidatorClient.validatePassword(password);
        if (!passwordValidation.isValid()) {
            alertMessage.errorMessage(passwordValidation.getErrorMessage());
            return;
        }

        // Validazione ID - deve avvenire PRIMA di chiedere la frase di recupero
        if (!User.checkID(ID)) {
            alertMessage.errorMessage(resolveString("%register.id.invalid"));
            return;
        }

        // TUTTI I CAMPI SONO VALIDATI - Ora chiedi la frase di recupero
        String recoveryPhrase = askForRecoveryPhrase();

        try{
             User user;
             if (recoveryPhrase != null) {
                 // Registrazione con frase di recupero - salva la frase già hashata
                 String hashedRecoveryPhrase = hashRecoveryPhrase(recoveryPhrase);
                 user = User.builder().id(ID).name(nome).surname(cognome).email(email).CF(CF).password(password).recoveryPhrase(hashedRecoveryPhrase).build();
                 logger.fine("Utente con frase di recupero: " + user.toString());
             } else {
                 // Registrazione senza frase di recupero
                 user = User.builder().id(ID).name(nome).surname(cognome).email(email).CF(CF).password(password).build();
                 logger.fine("Utente senza frase di recupero: " + user.toString());
            }

            boolean success = userService.registerUser(user);
            if(success){
                if (recoveryPhrase != null) {
                    alertMessage.successMessage(resolveString("%register.success.with.phrase"));
                } else {
                    alertMessage.successMessage(resolveString("%register.success"));
                }
                // Naviga al login con callback per gestire il login dopo registrazione
                Navigator.switchScene(
                    (Stage) registerButton.getScene().getWindow(),
                    EveryView.LOGIN.getPath(),
                    controller -> {
                        if (controller instanceof ControllerLogin loginController) {
                            loginController.setOnLoginSuccess(() -> {
                                // Dopo login riuscito, vai alla scena principale
                                Navigator.goToMainScene();
                            });
                        }
                    }
                );
            }else{
                alertMessage.errorMessage(resolveString("%register.failed.user.exists"));
            }
        }catch(Exception e){
            alertMessage.errorMessage(resolveString("%register.error") + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

}
