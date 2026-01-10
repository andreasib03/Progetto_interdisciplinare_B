package it.uninsubria.server.dao;

import it.uninsubria.shared.model.User;

public interface UserDAO {

    boolean registerUser(User utente);
    boolean login(String usernameOrEmail, String password);
    boolean phrasePassword(String email, String phrase);
    User getUserByUsernameOrEmail(String usernameOrEmail);
    boolean updateProfileImage(User user, byte[] imageBytes, String mimeType);
    boolean updateProfileInfo(User user) throws Exception;
    boolean getPasswordByUserID(String userID, String outPassword);
    boolean updatePassword(String userID, String newPassword);
    boolean deleteUser(String userID);

    // Password recovery methods
    boolean verifyRecoveryPhrase(String email, String hashedPhrase);
    boolean resetPassword(String email, String newPassword);
    
}
