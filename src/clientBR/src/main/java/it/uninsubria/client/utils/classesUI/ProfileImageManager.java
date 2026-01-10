package it.uninsubria.client.utils.classesUI;

import it.uninsubria.client.utils.classesLogic.ProfileImageListener;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.File;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

public class ProfileImageManager {
    private static final Logger logger = Logger.getLogger(ProfileImageManager.class.getName());

    private final CopyOnWriteArrayList<ProfileImageListener> listeners = new CopyOnWriteArrayList<>();

    public void addListener(ProfileImageListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(ProfileImageListener listener) {
        listeners.remove(listener);
    }

    public void notifyProfileImageChanged(File imageFile) {
        for (ProfileImageListener listener : listeners) {
            try {
                listener.onProfileImageChanged(imageFile);
            } catch (Exception e) {
                logger.warning("Error notifying profile image listener: " + e.getMessage());
            }
        }
    }

    public void updateProfileImageView(ImageView imageView, File imageFile) {
        if (imageView == null) {
            return;
        }

        if (imageFile != null && imageFile.exists()) {
            try {
                Image image = new Image(imageFile.toURI().toString(), true);
                imageView.setImage(image);
            } catch (Exception e) {
                logger.warning("Failed to load profile image: " + e.getMessage());
                imageView.setImage(null);
            }
        } else {
            imageView.setImage(null);
        }
    }

    public void updateProfileImageView(ImageView imageView, byte[] imageData, String mimeType) {
        if (imageView == null) {
            return;
        }

        if (imageData != null && imageData.length > 0) {
            try {
                Image image = new Image(new java.io.ByteArrayInputStream(imageData));
                imageView.setImage(image);
            } catch (Exception e) {
                logger.warning("Failed to load profile image from bytes: " + e.getMessage());
                imageView.setImage(null);
            }
        } else {
            imageView.setImage(null);
        }
    }

    public File saveProfileImage(byte[] imageData, String mimeType, String userId) {
        if (imageData == null || imageData.length == 0) {
            return null;
        }

        try {
            String extension = mimeType != null && mimeType.contains("/") 
                ? mimeType.split("/")[1] 
                : "jpg";
            
            java.nio.file.Path userDir = java.nio.file.Paths.get(
                System.getProperty("user.home"), 
                ".bookrecommender", 
                "profiles"
            );
            
            java.nio.file.Files.createDirectories(userDir);
            
            File imageFile = userDir.resolve(userId + "." + extension).toFile();
            java.nio.file.Files.write(imageFile.toPath(), imageData);
            
            logger.info("Saved profile image for user " + userId + " to " + imageFile.getPath());
            return imageFile;
        } catch (Exception e) {
            logger.warning("Failed to save profile image: " + e.getMessage());
            return null;
        }
    }
}
