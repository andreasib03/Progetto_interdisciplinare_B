package it.uninsubria.client.utils.classesUI;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.MenuButton;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;

public class UIHelpers {

    // UI Constants
    private static final double PROFILE_ICON_TRANSLATE_X = 180.0;
    private static final double PROFILE_IMAGE_CLIP_MARGIN = 3.0;
    private static final String PLACEHOLDER_IMAGE_PATH = "/images/placeholder_image.png";

    public static boolean showLoginPromptIfNotLogged(Pane container, Node promptNode) {
        boolean loggedIn = SessionManager.getInstance().isLoggedIn();
        container.getChildren().clear();
        
        if (loggedIn) {
            promptNode.setVisible(false);
            promptNode.setDisable(true);
            return true;
        } else {
            container.getChildren().add(promptNode);
            promptNode.setVisible(true);
            promptNode.setDisable(false);
            UIAnimator.animateFadeInScale(promptNode, 0.4);
            return false;
        }
    }

    /**
     * Updates UI components based on login status
     */
    private static void updateLoginUI(boolean isLoggedIn, ImageView profileImageView,
                                     Button loginRegisterButton, MenuButton iconLoginProfile,
                                     StackPane contenitorIconLoginProfile) {
        // Set visibility and enabled state based on login status
        loginRegisterButton.setVisible(!isLoggedIn);
        loginRegisterButton.setDisable(isLoggedIn);

        iconLoginProfile.setVisible(isLoggedIn);
        iconLoginProfile.setDisable(!isLoggedIn);

        contenitorIconLoginProfile.setVisible(isLoggedIn);
        contenitorIconLoginProfile.setDisable(!isLoggedIn);

        if (isLoggedIn) {
            // User is logged in - setup profile UI
            iconLoginProfile.setText(SessionManager.getInstance().getUsername());
            iconLoginProfile.translateXProperty().set(PROFILE_ICON_TRANSLATE_X);
            contenitorIconLoginProfile.translateXProperty().set(PROFILE_ICON_TRANSLATE_X);

            // Create circular clip for profile image
            double width = profileImageView.getFitWidth();
            double height = profileImageView.getFitHeight();
            double radius = Math.min(width, height) / 2;
            Circle clip = new Circle(width / 2, height / 2, radius - PROFILE_IMAGE_CLIP_MARGIN);
            profileImageView.setClip(clip);
        } else {
            // User is not logged in - cleanup profile UI
            profileImageView.setClip(null);
            profileImageView.setImage(ResourceCache.getImage(PLACEHOLDER_IMAGE_PATH));
        }
    }

    public static void updateToLogged(ImageView profileImageView, Button loginRegisterButton,
                                      MenuButton iconLoginProfile, StackPane contenitorIconLoginProfile) {
        updateLoginUI(true, profileImageView, loginRegisterButton, iconLoginProfile, contenitorIconLoginProfile);
    }

    public static void updateToNotLogged(ImageView profileImageView, Button loginRegisterButton,
                                         MenuButton iconLoginProfile, StackPane contenitorIconLoginProfile) {
        updateLoginUI(false, profileImageView, loginRegisterButton, iconLoginProfile, contenitorIconLoginProfile);
    }
}
