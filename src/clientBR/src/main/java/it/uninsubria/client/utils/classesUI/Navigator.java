package it.uninsubria.client.utils.classesUI;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import it.uninsubria.client.controller.ControllerBase;
import it.uninsubria.client.controller.homepage.home.ControllerDesign;
import it.uninsubria.client.interfaces.TextReceiver;
import it.uninsubria.shared.model.Book;
import javafx.animation.FadeTransition;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Duration;

public class Navigator {

    private static Stage mainStage;
    private static final Deque<String> navigationStack = new ArrayDeque<>();
    private static final Map<String, Object> parameters = new HashMap<>();

    public static void setMainStage(Stage stage) {
        mainStage = stage;
    }

    public static ControllerDesign getMainController(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(Navigator.class.getResource(EveryView.MAIN.getPath()), LanguageManager.getBundle());
            loader.load();
            return loader.getController();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

        // Navigazione base
    public static void goTo(String fxmlPath) {
        goTo(fxmlPath, null, true);
    }
        // Navigazione con parametri
    public static void goTo(String fxmlPath, Map<String, Object> params) {
        goTo(fxmlPath, params, true);
    }

        // Navigazione con controllo su stack (true = salva nel backStack)
    private static void goTo(String fxmlPath, Map<String, Object> params, boolean pushToStack) {
        try {
            FXMLLoader loader = new FXMLLoader(Navigator.class.getResource(fxmlPath));
            Parent root = loader.load();

            // Passaggio parametri se richiesto
            if (params != null) {
                parameters.clear();
                parameters.putAll(params);

                // Se il controller implementa ParametrizedController, passa i parametri
                Object controller = loader.getController();
                if (controller instanceof ParametrizedController pc) {
                    pc.initData(params);
                }
            }

            // Applica fade transition
            FadeTransition ft = new FadeTransition(Duration.millis(400), root);
            ft.setFromValue(0);
            ft.setToValue(1);


            Scene scene = new Scene(root);
            ThemeManager.applyTheme(scene);
            mainStage.setScene(scene);
            mainStage.show();



            ft.play();

            if (pushToStack) {
                navigationStack.push(fxmlPath);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void goBack() {
        if (navigationStack.size() > 1) {
            // Rimuove la schermata corrente
            navigationStack.pop();
            String previous = navigationStack.peek();
            goTo(previous, null, false);
        }
    }

    public static Object getParam(String key) {
        return parameters.get(key);
    }

    
    private static ControllerDesign mainController;

    public static void goToMainScene() {
        if(mainStage != null){
            try {
                FXMLLoader loader = new FXMLLoader(Navigator.class.getResource(EveryView.MAIN.getPath()),LanguageManager.getBundle());
                Parent root = loader.load();
                mainController = loader.getController();

                Scene scene = new Scene(root);
                ThemeManager.applyTheme(scene);
                mainStage.setScene(scene);
                mainStage.show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.err.println("Main stage non impostato.");
        }
        
    }

    public static void updateUserInfo() {
        if (mainController != null) {
            mainController.updateUserInfoUI();
        } else {
            System.err.println("Impossibile aggiornare l'UI: controller principale non trovato.");
        }

    }

    public static void goToMain() {
        loadScene(EveryView.MAIN.getPath(), "Home");
    }


    public static void goToLogin() {
        loadScene(EveryView.LOGIN.getPath(), "Login");
    }

    public static void goToRegistration() {
        loadScene(EveryView.REGISTRATION.getPath(), "Registrazione");
    }

    public static void goToLibrary() {
        loadScene("/fxml/library.fxml", "Libreria Personale");
    }

    public static void goToRating() {
        loadScene("/fxml/rating.fxml", "Valuta Libro");
    }

    private static void loadScene(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(Navigator.class.getResource(fxmlPath)));
            Parent root = loader.load();

            Scene scene = new Scene(root);
            ThemeManager.applyTheme(scene);
            mainStage.setScene(scene);
            mainStage.setTitle(title);
            mainStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void openNewWindow(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(Navigator.class.getResource(fxmlPath)));
            Parent root = loader.load();
            Stage stage = new Stage();
            Scene scene = new Scene(root);
            ThemeManager.applyTheme(scene);
            stage.setScene(scene);
            stage.setTitle(title);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    
    private static boolean isTransitioning = false;

    public static void switchScene(Stage stage, String fxmlPath) {
        if(isTransitioning) return;
        isTransitioning = true;

        try {
            Scene currentScene = stage.getScene();
            Parent currentRoot = currentScene.getRoot();

            FadeTransition fadeOut = new FadeTransition(Duration.millis(200), currentRoot);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);

            fadeOut.setOnFinished(event -> {
                try {
                    FXMLLoader loader = new FXMLLoader(Navigator.class.getResource(fxmlPath), LanguageManager.getBundle());

                    Parent newRoot = loader.load();
                    Scene newScene = new Scene(newRoot);
                    ThemeManager.applyTheme(newScene);
                    FadeTransition fadeIn = new FadeTransition(Duration.millis(200), newRoot);
                    fadeIn.setFromValue(0.0);
                    fadeIn.setToValue(1.0);
                    fadeIn.setOnFinished(ev -> isTransitioning = false);
                    fadeIn.play();

                    stage.setScene(newScene);
                } catch (Exception e) {
                    e.printStackTrace();
                    isTransitioning = false;
                }
            });

            fadeOut.play();
        } catch (Exception e) {
            e.printStackTrace();
            isTransitioning = false;
        }
    }


    public static void switchScene(Stage stage, String fxmlPath, Consumer<Object> controllerCallback) throws IOException {
        try {

            FXMLLoader loader = new FXMLLoader(Navigator.class.getResource(fxmlPath), LanguageManager.getBundle());
            Parent root = loader.load();

            // Ottieni il controller
            Object controller = loader.getController();

            if(controllerCallback != null){
                controllerCallback.accept(controller);
            }

            Scene scene = new Scene(root);
            ThemeManager.applyTheme(scene);

            stage.setScene(scene);
            stage.show();

            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    /**
     * Overload senza callback sul controller
     */


    public static void closeScene(Node anyNode){
        if (anyNode != null && anyNode.getScene() != null) {
            Stage stage = (Stage) anyNode.getScene().getWindow();
            if (stage != null) {
                stage.close();
            }
        } else {
            System.err.println("Impossibile chiudere la finestra: nodo o scena null.");
        }
    }

    public static <T> T loadAndSetContent(String fxmlPath, Button buttonSelecting, HBox targetBox) {
        try {
            FXMLLoader loader = new FXMLLoader(Navigator.class.getResource(fxmlPath));
            loader.setResources(LanguageManager.getBundle());
            Parent newContent = loader.load();

            T controller = loader.getController();
            if (controller instanceof TextReceiver) {

                ((TextReceiver) controller).setTestoDinamico(buttonSelecting.getText());
            }
            Scene scene = targetBox.getScene();
            if (scene != null) {
                ThemeManager.applyTheme(scene);
            }

            targetBox.getChildren().setAll(newContent); // Sostituisce il contenuto
            return controller;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Parent loadView(String path, SubController subController) throws IOException {
        FXMLLoader loader = new FXMLLoader(Navigator.class.getResource(path));
        loader.setResources(LanguageManager.getBundle());
        Parent content = loader.load();
        if (subController != null) {
            subController.initData(ControllerBase.getServiceManager());
        }
        return content;
    }

    public static void navigateTo(String string, Map<String,Book> of) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'navigateTo'");
    }


}
