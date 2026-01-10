package it.uninsubria.server.service;

import it.uninsubria.shared.model.User;

/**
 * Core user service interface providing user management operations.
 * Handles user registration, authentication, profile management, and password recovery.
 * 
 * <p>This service is the business logic layer for user-related operations,
 * managing interactions with the database through the UserDAO.</p>
 * 
 * <p>All methods that modify user state throw Exception to allow proper
 * error handling and logging at the implementation level.</p>
 */
public interface UserServiceCore {

    /**
     * Registers a new user in the system.
     * 
     * @param utente the User object containing registration details
     *               (name, surname, email, username, password, etc.)
     * @return true if registration was successful, false otherwise
     * @throws Exception if registration fails due to validation or database errors
     */
    boolean registerUser(User utente) throws Exception;

    /**
     * Authenticates a user using username/email and password.
     * 
     * @param usernameOrEmail the username or email address for authentication
     * @param password the password for authentication
     * @return true if authentication was successful, false otherwise
     * @throws Exception if authentication fails
     */
    boolean login(String usernameOrEmail, String password) throws Exception;

    /**
     * Verifies recovery phrase for password reset functionality.
     * 
     * @param userid the user identifier
     * @param phrase the recovery phrase to verify
     * @return true if phrase matches stored phrase, false otherwise
     * @throws Exception if verification fails
     */
    boolean phrasePassword(String userid, String phrase) throws Exception;

    /**
     * Retrieves a user by username or email address.
     * 
     * @param usernameOrEmail the username or email to search for
     * @return the User object if found, null otherwise
     * @throws Exception if retrieval fails
     */
    User getUserByUsernameOrEmail(String usernameOrEmail) throws Exception;

    /**
     * Updates the profile image for a user.
     * 
     * @param user the User object containing user identification
     * @param imageBytes the image data as byte array
     * @param mimeType the MIME type of the image (e.g., "image/jpeg", "image/png")
     * @return true if update was successful, false otherwise
     * @throws Exception if update fails
     */
    boolean updateProfileImage(User user, byte[] imageBytes, String mimeType) throws Exception;

    /**
     * Updates user profile information (name, surname, etc.).
     * 
     * @param user the User object with updated profile information
     * @return true if update was successful, false otherwise
     * @throws Exception if update fails
     */
    boolean updateProfileInfo(User user) throws Exception;

    /**
     * Verifies the current password for a user.
     * 
     * @param userID the user identifier
     * @param outPassword the password to verify
     * @return true if password matches, false otherwise
     * @throws Exception if verification fails
     */
    boolean getPasswordByUserID(String userID, String outPassword) throws Exception;

    /**
     * Updates the password for a user.
     * 
     * @param userID the user identifier
     * @param newPassword the new password to set
     * @return true if password update was successful, false otherwise
     * @throws Exception if update fails
     */
    boolean updatePassword(String userID, String newPassword) throws Exception;

    /**
     * Deletes a user from the system.
     * 
     * @param userID the user identifier to delete
     * @return true if deletion was successful, false otherwise
     * @throws Exception if deletion fails
     */
    boolean deleteUser(String userID) throws Exception;

    // Password recovery methods

    /**
     * Verifies the recovery phrase for password reset.
     * 
     * @param email the email address associated with the user account
     * @param hashedPhrase the hashed recovery phrase to verify
     * @return true if recovery phrase is valid, false otherwise
     * @throws Exception if verification fails
     */
    boolean verifyRecoveryPhrase(String email, String hashedPhrase) throws Exception;

    /**
     * Resets the password for a user using email verification.
     * 
     * @param email the email address for password reset
     * @param newPassword the new password to set
     * @return true if password reset was successful, false otherwise
     * @throws Exception if reset fails
     */
    boolean resetPassword(String email, String newPassword) throws Exception;
}
