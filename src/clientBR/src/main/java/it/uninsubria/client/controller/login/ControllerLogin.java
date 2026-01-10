package it.uninsubria.client.controller.login;

import it.uninsubria.client.rmi.RMIClient;
import it.uninsubria.shared.utils.AppConfig;
import it.uninsubria.client.utils.classesLogic.VideoUtils;
import it.uninsubria.client.utils.classesUI.AlertMessageCustom;
import it.uninsubria.client.utils.classesUI.EveryView;
import it.uninsubria.client.utils.classesUI.InputValidatorClient;
import it.uninsubria.client.utils.classesUI.Navigator;
import it.uninsubria.client.utils.classesUI.ServiceLocator;
import it.uninsubria.client.utils.classesUI.SessionManager;
import it.uninsubria.client.utils.classesUI.ThemeManager;
import it.uninsubria.client.utils.classesUI.TimeoutManager;
import it.uninsubria.client.utils.classesUI.LanguageManager;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import it.uninsubria.shared.model.User;
import it.uninsubria.shared.rmi.UserService;



public class ControllerLogin{

    @FXML
    private TextField usernameOrEmailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label popupLabel;

    @FXML
    private Hyperlink hyperLinkForgotPassword;
    @FXML
    private Hyperlink hyperLinkLogin;
    @FXML
    private Hyperlink infoLink;

    @FXML
    private VBox videoVBox;

    @FXML
    private Button loginButton;
    @FXML
    private Button cancelButton;

    @FXML
    private ProgressIndicator loginProgressIndicator;

    @FXML
    private CheckBox rememberMeCheckBox;

    private final UserService userService = ServiceLocator.getUserService();


    private void connectToServer(){
        try {
            RMIClient.connectToServer(AppConfig.getServerHost());
        } catch (Exception e) {
            showPopup("Impossibile connettersi al server");
            e.printStackTrace();
        }
    }

    private void bindLoginButton(){
        loginButton.disableProperty().bind(usernameOrEmailField.textProperty().isEmpty().or(passwordField.textProperty().isEmpty()));
    }

    /**
     * Popola automaticamente i campi se ci sono credenziali ricordate
     */
    private void populateRememberedCredentials() {
        String[] credentials = SessionManager.getInstance().getRememberMeCredentials();
        if (credentials != null) {
            usernameOrEmailField.setText(credentials[0]);
            passwordField.setText(credentials[1]);
            rememberMeCheckBox.setSelected(true);
        }
    }

    private void setupInfoPopup(){
        FadeTransition fadeIn = createFadeTransition(popupLabel, 0, 1);
        FadeTransition fadeOut = createFadeTransition(popupLabel, 1, 0);

        infoLink.setOnMouseEntered(e -> {
            popupLabel.setVisible(true);
            popupLabel.setManaged(true);
            fadeIn.play();
        });

        EventHandler<MouseEvent> hidePopup = e -> {
            if (!infoLink.isHover() && !popupLabel.isHover()) {
                fadeOut.setOnFinished(ev -> {
                    popupLabel.setVisible(false);
                    popupLabel.setManaged(false);
                });
                fadeOut.play();
            }
        };

        infoLink.setOnMouseExited(hidePopup);
        popupLabel.setOnMouseExited(hidePopup);
    }

    private FadeTransition createFadeTransition(Label label, double from, double to) {
        FadeTransition ft = new FadeTransition(Duration.millis(200), label);
        ft.setFromValue(from);
        ft.setToValue(to);
        return ft;
    }

    private void showPopup(String message) {
        popupLabel.setText(message);
        popupLabel.setVisible(true);
    }

    @FXML
    public void initialize(){

        loginButton.setOnAction(event -> exitView());
        loginButton.setDefaultButton(true);

        bindLoginButton();
        connectToServer();
        setupInfoPopup();
        populateRememberedCredentials();
        VideoUtils.setupBackgroundVideo(videoVBox, "/video/Background_Login_video.mp4", 70, 70);
        Platform.runLater(() ->{
            Stage stage = (Stage) cancelButton.getScene().getWindow();
            stage.setOnCloseRequest(event -> {
                // Controlla se siamo nella pagina principale controllando l'id del root
                Scene currentScene = stage.getScene();
                if (currentScene != null) {
                    String rootId = currentScene.getRoot().getId();
                    if ("root".equals(rootId)) {
                        // Siamo nella pagina principale, possiamo chiudere l'applicazione
                        Platform.exit();
                    } else {
                        // Non siamo nella pagina principale, vai alla pagina principale
                        Navigator.switchScene(stage, EveryView.MAIN.getPath());
                        event.consume();
                    }
                }
            });
        });
        
    }


    //Spostare tutto questo in un controller base così almeno posso richiamare pubblicamente i metodi e quelli privati lasciarli lì senza problemi



    @FXML
    public void onOpenForgotPassword(ActionEvent event){
        try {
            // Crea un nuovo Stage per la finestra di recupero password
            Stage forgotPasswordStage = new Stage();

            // Carica l'FXML ForgotPassword
            FXMLLoader loader = new FXMLLoader(getClass().getResource(EveryView.FORGOT_PASSWORD.getPath()));
            loader.setResources(LanguageManager.getBundle());

            Parent root = loader.load();

            // Ottieni il controller e passa l'userService
            ControllerFormForgotPassword controller = loader.getController();
            controller.setUserService(userService);

            // Crea la scena
            Scene scene = new Scene(root);
            forgotPasswordStage.setScene(scene);

            // Applica il tema corrente (light/dark mode) alla nuova finestra
            ThemeManager.applyTheme(scene);

            // Configura la finestra
            forgotPasswordStage.setTitle(LanguageManager.getBundle().getString("login.forgot.password.title"));
            forgotPasswordStage.setResizable(false);

            // Rendi la finestra modale (blocca la finestra principale)
            Stage ownerStage = (Stage) hyperLinkForgotPassword.getScene().getWindow();
            forgotPasswordStage.initOwner(ownerStage);
            forgotPasswordStage.initModality(Modality.APPLICATION_MODAL);

            // Mostra la finestra
            forgotPasswordStage.show();

            // Reset dell'hyperlink quando la finestra si chiude
            forgotPasswordStage.setOnHidden(windowEvent -> {
                Platform.runLater(() -> {
                    hyperLinkForgotPassword.setVisited(false);
                    hyperLinkForgotPassword.setStyle(""); // Reset eventuali stili inline
                });
            });

        } catch (Exception e) {
            System.err.println("Errore nell'apertura della finestra password dimenticata: " + e.getMessage());
            e.printStackTrace();
            showAlertPopup("Errore nell'apertura della finestra di recupero password");
        }
    }

    @FXML
    private void closeView(){
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        Navigator.switchScene(stage, EveryView.MAIN.getPath());
    }

    protected static void showAlertPopup(String title){
        AlertMessageCustom alert = new AlertMessageCustom();
        alert.errorMessage(title);
    }
    protected static void showSuccessPopup(String title){
        AlertMessageCustom alert = new AlertMessageCustom();
        alert.successMessage(title);
    }


    private Runnable onLoginSuccess;

    public void setOnLoginSuccess(Runnable callback) {
        this.onLoginSuccess = callback;
    }

    @FXML
    private void exitView(){

        String usernameOrEmail = usernameOrEmailField.getText();
        String password = passwordField.getText();

        if(usernameOrEmail.isEmpty() || password.isEmpty()){
            showAlertPopup("Inserisci username e password");
            return;
        }

        // Validazione input lato client
        if (usernameOrEmail.contains("@")) {
            // È un'email
            var emailValidation = InputValidatorClient.validateEmail(usernameOrEmail);
            if (!emailValidation.isValid()) {
                showAlertPopup(emailValidation.getErrorMessage());
                return;
            }
        } else {
            // È un username
            var usernameValidation = InputValidatorClient.validateUsername(usernameOrEmail);
            if (!usernameValidation.isValid()) {
                showAlertPopup(usernameValidation.getErrorMessage());
                return;
            }
        }

        // Mostra il loading indicator e disabilita il pulsante
        loginProgressIndicator.setVisible(true);
        loginButton.disableProperty().unbind();
        loginButton.setDisable(true);

        it.uninsubria.client.utils.classesUI.ThreadPoolManager.executeIO(() -> {
            try{
                boolean hasActiveSession = userService.hasActiveSession(usernameOrEmail);

                Platform.runLater(() -> {
                    if (hasActiveSession) {
                        boolean proceed = AlertMessageCustom.showConfirmation(
                            "Sessione esistente",
                            "Hai già una sessione attiva in un altro dispositivo. Procedendo effettuerai il logout dall'altro dispositivo. Continuare?"
                        );
                        if (!proceed) {
                            hideLoadingAndEnableButton();
                            return;
                        }
                    }

                    it.uninsubria.client.utils.classesUI.ThreadPoolManager.executeIO(() -> {
                        try {
                            String token = userService.authenticateUser(usernameOrEmail, password);
                            if (token != null && !token.isEmpty()) {
                                User user = userService.getUserByUsernameOrEmail(usernameOrEmail);

                                Platform.runLater(() -> {
                                    showSuccessPopup("Login effettuato!");

                                    String userId = user.getUser_id().toString();
                                    SessionManager.getInstance().addUserSession(
                                        userId,
                                        usernameOrEmail.contains("@") ? null : usernameOrEmail,
                                        usernameOrEmail.contains("@") ? usernameOrEmail : null,
                                        token,
                                        user
                                    );

                                    if (rememberMeCheckBox.isSelected()) {
                                        SessionManager.getInstance().saveRememberMeCredentials(usernameOrEmail, password);
                                        if (TimeoutManager.getInstance().getCurrentTimeoutMinutes() == 0) {
                                            TimeoutManager.getInstance().setTimeoutMinutes(30);
                                        }
                                    } else {
                                        SessionManager.getInstance().clearRememberMeCredentials();
                                    }

                                    if (onLoginSuccess != null) {
                                        onLoginSuccess.run();
                                    }

                                    hideLoadingAndEnableButton();
                                });
                            } else {
                                Platform.runLater(() -> {
                                    showAlertPopup("Credenziali errate, riprova.");
                                    hideLoadingAndEnableButton();
                                });
                            }
                        } catch (Exception e) {
                            Platform.runLater(() -> {
                                showPopup("Errore di connessione. Riprova più tardi.");
                                hideLoadingAndEnableButton();
                            });
                            e.printStackTrace();
                        }
                    });
                });
            }catch(Exception e){
                Platform.runLater(() -> {
                    showPopup("Errore di connessione. Riprova più tardi.");
                    hideLoadingAndEnableButton();
                });
                e.printStackTrace();
            }
        });
    }

    private void hideLoadingAndEnableButton() {
        loginProgressIndicator.setVisible(false);
        loginButton.disableProperty().unbind();
        loginButton.setDisable(false);
    }

    @FXML
    public void loginView(ActionEvent event){
        Navigator.switchScene((Stage) loginButton.getScene().getWindow(), EveryView.LOGIN.getPath());
    }

    @FXML
    public void registerView(ActionEvent event){
        Navigator.switchScene((Stage) hyperLinkLogin.getScene().getWindow(),EveryView.REGISTRATION.getPath());
    }
}