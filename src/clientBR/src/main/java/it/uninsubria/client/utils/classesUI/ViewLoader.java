package it.uninsubria.client.utils.classesUI;

import java.io.IOException;
import java.util.Objects;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;

public class ViewLoader {
    public static Parent load(String fxmlPath) throws IOException {
        FXMLLoader loader = new FXMLLoader(
            Objects.requireNonNull(ViewLoader.class.getResource(fxmlPath))
        );
        loader.setResources(LanguageManager.getBundle());
        return loader.load();
    }

    public static void loadInto(Stage stage, String fxmlPath) throws IOException {
        Parent root = load(fxmlPath);

        if(stage.getScene() == null) {
            stage.getScene().setRoot(root);
            ThemeManager.applyTheme(stage.getScene());
        }{
            stage.setScene(new javafx.scene.Scene(root));
            ThemeManager.applyTheme(stage.getScene());
        }
    }
}
