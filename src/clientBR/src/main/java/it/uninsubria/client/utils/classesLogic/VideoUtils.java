package it.uninsubria.client.utils.classesLogic;

import java.util.logging.Logger;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.shape.Rectangle;

public class VideoUtils {
    private static final Logger logger = Logger.getLogger(VideoUtils.class.getName());

    public static void setupBackgroundVideo(VBox container, String videoResourcePath, double arcWidth, double arcHeight) {
        try {
            var resource = VideoUtils.class.getResource(videoResourcePath);

            if (resource == null) {
                logger.severe("Video file not found at: " + videoResourcePath);
                setupFallbackBackground(container, arcWidth, arcHeight);
                return;
            }

            String videoPath = resource.toExternalForm();
            logger.info("Percorso video caricato: " + videoPath);

            Media media = new Media(videoPath);
            MediaPlayer mediaPlayer = new MediaPlayer(media);
            mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            mediaPlayer.setAutoPlay(true);

            MediaView mediaView = new MediaView(mediaPlayer);
            mediaView.setPreserveRatio(false);
            mediaView.fitWidthProperty().bind(container.widthProperty());
            mediaView.fitHeightProperty().bind(container.heightProperty());

            Rectangle clip = new Rectangle();
            clip.widthProperty().bind(mediaView.fitWidthProperty());
            clip.heightProperty().bind(mediaView.fitHeightProperty());
            clip.setArcWidth(arcWidth);
            clip.setArcHeight(arcHeight);
            mediaView.setClip(clip);

            VBox.setVgrow(mediaView, Priority.ALWAYS);
            container.getChildren().add(mediaView);

            logger.info("Video caricato e avviato.");

        } catch (Exception e) {
            logger.log(java.util.logging.Level.SEVERE, "Errore durante setupBackgroundVideo, utilizzo fallback", e);
            setupFallbackBackground(container, arcWidth, arcHeight);
        }
    }

    /**
     * Sets up a static background image as fallback when video cannot be played
     */
    private static void setupFallbackBackground(VBox container, double arcWidth, double arcHeight) {
        try {
            // Try to load a background image
            var imageResource = VideoUtils.class.getResource("/images/logo.png");
            if (imageResource == null) {
                // If logo not found, try placeholder
                imageResource = VideoUtils.class.getResource("/images/placeholder_image.png");
            }

            if (imageResource != null) {
                Image backgroundImage = new Image(imageResource.toExternalForm());
                ImageView imageView = new ImageView(backgroundImage);
                imageView.setPreserveRatio(false);
                imageView.fitWidthProperty().bind(container.widthProperty());
                imageView.fitHeightProperty().bind(container.heightProperty());

                Rectangle clip = new Rectangle();
                clip.widthProperty().bind(imageView.fitWidthProperty());
                clip.heightProperty().bind(imageView.fitHeightProperty());
                clip.setArcWidth(arcWidth);
                clip.setArcHeight(arcHeight);
                imageView.setClip(clip);

                VBox.setVgrow(imageView, Priority.ALWAYS);
                container.getChildren().add(imageView);

                logger.info("Immagine di fallback caricata.");
            } else {
                // If no image available, set a solid background color
                container.setStyle("-fx-background-color: linear-gradient(to bottom, #667eea 0%, #764ba2 100%);");
                logger.info("Sfondo colore di fallback applicato.");
            }

        } catch (Exception e) {
            logger.log(java.util.logging.Level.SEVERE, "Errore durante setup fallback background", e);
            // Final fallback: solid color
            container.setStyle("-fx-background-color: linear-gradient(to bottom, #667eea 0%, #764ba2 100%);");
        }
    }
}
