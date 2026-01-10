package it.uninsubria.client.controller.homepage.help;

import it.uninsubria.client.controller.ControllerBase;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;
import java.net.URL;
import java.util.ResourceBundle;


public class VideoListController extends ControllerBase implements Initializable {

    @FXML private VBox videoListContainer;

    // Video data constants
    private static final String[] VIDEO_PATHS = {
        "/video/1.mp4",
        "/video/2.mp4",
        "/video/3.mp4"
    };

    private static final String[] VIDEO_TITLES = {
        "Avvio applicazione",
        "Creazione account",
        "Ricerca libro"
    };

    private static final String[] VIDEO_DESCRIPTIONS = {
        "Video di sfondo 1",
        "Video di sfondo 1",
        "Video di sfondo 1"
    };

    private static final String[] VIDEO_PREVIEWS = {
        "/video/previews/splash_preview.png",
        "/video/previews/splash_preview.png",
        "/video/previews/splash_preview.png"
    };

    // UI Constants
    private static final double PREVIEW_WIDTH = 200;
    private static final double PREVIEW_HEIGHT = 120;
    private static final double VIDEO_WINDOW_WIDTH = 800;
    private static final double VIDEO_WINDOW_HEIGHT = 600;
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        for (int i = 0; i < VIDEO_PATHS.length; i++) {
            VBox card = createVideoCard(VIDEO_PATHS[i], VIDEO_TITLES[i], VIDEO_DESCRIPTIONS[i], VIDEO_PREVIEWS[i]);
            videoListContainer.getChildren().add(card);
        }
    }

    private VBox createVideoCard(String videoPath, String title, String description, String previewPath) {
        VBox card = new VBox();
        card.getStyleClass().add("cardVideoStyle");
        card.setSpacing(5);

        // Preview image
        ImageView preview = new ImageView(new Image(getClass().getResourceAsStream(previewPath)));
        preview.setFitWidth(PREVIEW_WIDTH);
        preview.setFitHeight(PREVIEW_HEIGHT);
        preview.setPreserveRatio(true);

        // Titolo e descrizione
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("titleLabelVideo");

        Label descriptionLabel = new Label(description);
        descriptionLabel.getStyleClass().add("descriptionLabelVideo");

        VBox textBox = new VBox(titleLabel, descriptionLabel);
        textBox.setSpacing(3);

        card.getChildren().addAll(preview, textBox);
        
        // Click to play
        card.setOnMouseClicked(e -> playVideo(videoPath, title));

        return card;
    }

    private void playVideo(String videoResourcePath, String title) {
        Media media = new Media(getClass().getResource(videoResourcePath).toExternalForm());
        MediaPlayer mediaPlayer = new MediaPlayer(media);
        MediaView mediaView = new MediaView(mediaPlayer);

        VBox root = new VBox(mediaView);
        root.setStyle("-fx-background-color: black;");
        root.setPrefSize(VIDEO_WINDOW_WIDTH, VIDEO_WINDOW_HEIGHT);

        Scene scene = new Scene(root);

        Stage videoStage = new Stage();
        videoStage.setTitle(String.format(resolveString("%help.video.playback.title"), title));
        videoStage.setScene(scene);
        videoStage.show();

        mediaPlayer.play();

        // Ferma il video quando chiudi la finestra
        videoStage.setOnCloseRequest(e -> mediaPlayer.dispose());
    }

}
