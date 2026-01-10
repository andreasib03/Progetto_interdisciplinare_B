package it.uninsubria.shared.rmi;
import java.rmi.Remote;
import java.rmi.RemoteException;

import it.uninsubria.shared.model.User;

/**
 * Remote interface for user management operations in the Book Recommender system.
 * Provides methods for user registration, authentication, profile management,
 * and session handling via RMI (Remote Method Invocation).
 *
 * <p>This interface defines the contract for user-related operations that can be
 * invoked remotely by client applications. All methods may throw {@link RemoteException}
 * for communication-related issues.
 *
 * @author Book Recommender Team
 * @version 1.0
 * @since 1.0
 */
public interface UserService extends Remote{
    
    /**
     * Registers a new user in the system.
     *
     * @param utente the user object containing registration information
     * @return true if registration was successful, false otherwise
     * @throws RemoteException if a communication error occurs
     */
    boolean registerUser(User utente) throws RemoteException;
    boolean login(String userid, String password) throws RemoteException;
    boolean phrasePassword(String userid, String phrase) throws RemoteException;
    User getUserByUsernameOrEmail(String usernameOrEmail) throws RemoteException;
    boolean updateProfileImage(User user, byte[] imageBytes, String mimeType) throws RemoteException;
    boolean updateProfileInfo(User user) throws RemoteException;
    boolean getPasswordByUserID(String userID, String password) throws RemoteException;
    boolean updatePassword(String userID, String newPassword) throws RemoteException;
    boolean deleteUser(String userID) throws RemoteException;

    // Password recovery methods
    boolean verifyRecoveryPhrase(String email, String hashedPhrase) throws RemoteException;
    boolean resetPassword(String email, String newPassword) throws RemoteException;
    
    // JWT Session management methods
    /**
     * Authenticates a user and returns a JWT token for session management.
     *
     * @param usernameOrEmail the username or email of the user
     * @param password the user's password
     * @return a JWT token if authentication is successful, null otherwise
     * @throws RemoteException if a communication error occurs
     */
    String authenticateUser(String usernameOrEmail, String password) throws RemoteException;
    /**
     * Validates if a JWT session token is still active and valid.
     *
     * @param token the JWT token to validate
     * @return true if the token is valid and not expired, false otherwise
     * @throws RemoteException if a communication error occurs
     */
    boolean validateSession(String token) throws RemoteException;
    boolean invalidateSession(String token) throws RemoteException;
    String refreshSession(String token) throws RemoteException;
    boolean hasActiveSession(String usernameOrEmail) throws RemoteException;

    // Heartbeat for session maintenance
    boolean heartbeat(String token) throws RemoteException;
}
