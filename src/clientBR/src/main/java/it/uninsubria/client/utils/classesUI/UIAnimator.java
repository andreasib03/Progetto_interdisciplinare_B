package it.uninsubria.client.utils.classesUI;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.util.Duration;

public class UIAnimator {

    public static void typewriterLabel(Label label) {
        String fullText = label.getText();   // prendi il testo già nella label
        label.setText("");                   // parti da vuoto

        int millisPerChar = 50;             // default

        Timeline timeline = new Timeline();
        for (int i = 0; i < fullText.length(); i++) {
            final int index = i;
            KeyFrame kf = new KeyFrame(
                Duration.millis(i * millisPerChar),
                e -> label.setText(fullText.substring(0, index + 1))
            );
            timeline.getKeyFrames().add(kf);
        }
        timeline.play();
    }

    public static void bounceButton(Button button, int translateX) {
        
        // Reset scala prima di animare
        button.setTranslateX(translateX);

        button.setScaleX(1.0);
        button.setScaleY(1.0);
        ScaleTransition st = new ScaleTransition(Duration.millis(200), button);
        st.setToX(1.1);
        st.setToY(1.1);
        st.setAutoReverse(true);
        st.setCycleCount(2);
        st.play();

    }




    public static void slideLeftButton(Button button) {
        // Reset posizione prima di animare
        button.setTranslateX(0);

        TranslateTransition tt = new TranslateTransition(Duration.millis(200), button);
        tt.setByX(-1); // spostamento verso sinistra (negativo = sinistra, positivo = destra)
        tt.setCycleCount(1); // una volta sola
        tt.setInterpolator(Interpolator.EASE_BOTH); // movimento fluido
        tt.play();

        button.setStyle("-fx-font-weight: Bold;");
    }

    
    
    public static void animateFadeInScale(Node node, double durationSec) {
        FadeTransition fade = new FadeTransition(Duration.seconds(durationSec), node);
        fade.setFromValue(0);
        fade.setToValue(1);

        ScaleTransition scale = new ScaleTransition(Duration.seconds(durationSec), node);
        scale.setFromX(0.8);
        scale.setToX(1.0);
        scale.setFromY(0.8);
        scale.setToY(1.0);

        new ParallelTransition(fade, scale).play();
    }
}
