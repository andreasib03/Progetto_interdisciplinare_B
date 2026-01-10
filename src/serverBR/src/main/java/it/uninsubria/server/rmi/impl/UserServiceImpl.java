package it.uninsubria.server.rmi.impl;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import it.uninsubria.server.dao.UserDAO;
import it.uninsubria.shared.model.User;
import it.uninsubria.shared.rmi.UserService;

import it.uninsubria.server.service.UserServiceCore;
import it.uninsubria.server.di.ServerDIContainer;
import it.uninsubria.shared.exception.DatabaseException;
import it.uninsubria.shared.exception.NetworkException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import it.uninsubria.shared.rmi.ServiceException;

public class UserServiceImpl extends UnicastRemoteObject implements UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserDAO dao;

    public UserServiceImpl(UserDAO dao) throws RemoteException {
        super();
        this.dao = dao;
    }

    /**
     * Helper method to handle exceptions consistently across service methods.
     * Converts various exceptions to appropriate ServiceException types.
     */
    private ServiceException handleServiceException(String operation, String identifier, Exception e) {
        logger.error("Errore {} per {}: {}", operation, identifier, e.getMessage());

        // Check for specific exception types and wrap appropriately
        Throwable cause = e.getCause();
        if (cause instanceof java.sql.SQLException) {
            return new ServiceException("Errore database " + operation,
                new DatabaseException("Database error during " + operation, e));
        } else if (cause instanceof java.rmi.RemoteException) {
            return new ServiceException("Errore rete " + operation,
                new NetworkException("Network error during " + operation, e));
        } else if (e instanceof RuntimeException) {
            return new ServiceException("Errore runtime " + operation, e);
        } else {
            return new ServiceException("Errore " + operation, e);
        }
    }

    @Override
    public boolean registerUser(User utente) throws RemoteException {
        try {
            UserServiceCore core = ServerDIContainer.getUserCore();
            if (core != null) return core.registerUser(utente);
            return dao.registerUser(utente);
        
        } catch (Exception e) {
            throw handleServiceException("registrazione utente", utente.getID(), e);
        }
    }

    @Override
    public boolean login(String usernameOrEmail, String password) throws RemoteException {
        try {
            UserServiceCore core = ServerDIContainer.getUserCore();
            boolean ok = (core != null) ? core.login(usernameOrEmail, password) : dao.login(usernameOrEmail, password);
            return ok;
        
        } catch (Exception e) {
            throw handleServiceException("login utente", usernameOrEmail, e);
        }
    }

    @Override
    public boolean phrasePassword(String userid, String phrase) throws RemoteException {
        try {
            UserServiceCore core = ServerDIContainer.getUserCore();
            if (core != null) return core.phrasePassword(userid, phrase);
            return dao.phrasePassword(userid, phrase);
        
        } catch (Exception e) {
            throw handleServiceException("recupero frase password", userid, e);
        }
    }

    @Override
    public User getUserByUsernameOrEmail(String usernameOrEmail) throws RemoteException {
        try {
            UserServiceCore core = ServerDIContainer.getUserCore();
            if (core != null) return core.getUserByUsernameOrEmail(usernameOrEmail);
            return dao.getUserByUsernameOrEmail(usernameOrEmail);

        } catch (Exception e) {
            throw handleServiceException("recupero utente", usernameOrEmail, e);
        }
    }

    @Override
    public boolean updateProfileImage(User user, byte[] imageBytes, String mimeType) throws RemoteException {
        try {
            UserServiceCore core = ServerDIContainer.getUserCore();
            if (core != null) return core.updateProfileImage(user, imageBytes, mimeType);
            return dao.updateProfileImage(user, imageBytes, mimeType);
        
        } catch (Exception e) {
            throw handleServiceException("aggiornamento immagine profilo", user.getID(), e);
        }
    }

    @Override
    public boolean updateProfileInfo(User user) throws RemoteException {
        try {
            UserServiceCore core = ServerDIContainer.getUserCore();
            if (core != null) return core.updateProfileInfo(user);
            return dao.updateProfileInfo(user);
        
        } catch (Exception e) {
            throw handleServiceException("aggiornamento informazioni profilo", user.getID(), e);
        }
    }

    public boolean getPasswordByUserID(String userID, String outPassword) throws RemoteException {
        try {
            UserServiceCore core = ServerDIContainer.getUserCore();
            if (core != null) return core.getPasswordByUserID(userID, outPassword);
            return dao.getPasswordByUserID(userID, outPassword);
        
        } catch (Exception e) {
            throw handleServiceException("aggiornamento password", userID, e);
        }
    }

    @Override
    public boolean updatePassword(String userID, String newPassword) throws RemoteException {
        try {
            UserServiceCore core = ServerDIContainer.getUserCore();
            if (core != null) return core.updatePassword(userID, newPassword);
            return dao.updatePassword(userID, newPassword);
        
        } catch (Exception e) {
            throw handleServiceException("aggiornamento password", userID, e);
        }
    }

    @Override
    public boolean deleteUser(String userID) throws RemoteException {
        try {
            UserServiceCore core = ServerDIContainer.getUserCore();
            if (core != null) return core.deleteUser(userID);
            return dao.deleteUser(userID);
        
        } catch (Exception e) {
            throw handleServiceException("aggiornamento password", userID, e);
        }
    }

    // JWT Session management methods
    @Override
    public String authenticateUser(String usernameOrEmail, String password) throws RemoteException {
        try {
            // First verify credentials using existing login method
            UserServiceCore core = ServerDIContainer.getUserCore();
            boolean loginSuccess = (core != null) ? core.login(usernameOrEmail, password) : dao.login(usernameOrEmail, password);

            if (loginSuccess) {
                // Get user details for token creation
                User user = (core != null) ? core.getUserByUsernameOrEmail(usernameOrEmail) : dao.getUserByUsernameOrEmail(usernameOrEmail);
                if (user != null) {
                    // Create JWT session (automaticamente invalida sessioni precedenti)
                    String token = it.uninsubria.server.util.SessionManager.createSession(user);
                    logger.info("JWT token created for user: {}", user.getID());
                    return token;
                }
            }

             logger.warn("Authentication failed for user: {}", usernameOrEmail);
             return null;

         } catch (Exception e) {
             throw handleServiceException("autenticazione utente", usernameOrEmail, e);
         }
    }

    @Override
    public boolean validateSession(String token) throws RemoteException {
        try {
            it.uninsubria.server.util.SessionManager.UserSession session =
                it.uninsubria.server.util.SessionManager.validateSession(token);
            return session != null;
        
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean invalidateSession(String token) throws RemoteException {
        try {
            return it.uninsubria.server.util.SessionManager.invalidateSession(token);
        
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String refreshSession(String token) throws RemoteException {
        try {
            String newToken = it.uninsubria.server.util.SessionManager.refreshSession(token);
            if (newToken != null) {
                logger.info("Session refreshed successfully");
            }
             return newToken;

         } catch (Exception e) {
             throw handleServiceException("refresh sessione", token, e);
         }
     }

     @Override
     public boolean hasActiveSession(String usernameOrEmail) throws RemoteException {
        try {
            // Ottieni l'utente per ID
            UserServiceCore core = ServerDIContainer.getUserCore();
            User user = (core != null) ? core.getUserByUsernameOrEmail(usernameOrEmail) : dao.getUserByUsernameOrEmail(usernameOrEmail);

            if (user != null) {
                return it.uninsubria.server.util.SessionManager.hasActiveSession(user.getUser_id());
            }

             return false;

         } catch (Exception e) {
             throw handleServiceException("controllo sessione attiva", usernameOrEmail, e);
         }
     }

     @Override
     public boolean verifyRecoveryPhrase(String email, String hashedPhrase) throws RemoteException {
        try {
            UserServiceCore core = ServerDIContainer.getUserCore();
            return (core != null) ? core.verifyRecoveryPhrase(email, hashedPhrase) : dao.verifyRecoveryPhrase(email, hashedPhrase);
        
         } catch (Exception e) {
             throw handleServiceException("verifica frase recupero", email, e);
         }
     }

     @Override
     public boolean resetPassword(String email, String newPassword) throws RemoteException {
        try {
            UserServiceCore core = ServerDIContainer.getUserCore();
             return (core != null) ? core.resetPassword(email, newPassword) : dao.resetPassword(email, newPassword);

         } catch (Exception e) {
             throw handleServiceException("reset password", email, e);
         }
    }

    @Override
    public boolean heartbeat(String token) throws RemoteException {
        try {
            // Valida che il token sia valido e aggiorna il timestamp dell'ultima attività
            var session = it.uninsubria.server.util.SessionManager.validateSession(token);
            if (session != null) {
                // Se valido, aggiorna il timestamp dell'ultima attività per mantenere viva la sessione
                it.uninsubria.server.util.SessionManager.updateLastActivity(token);
                logger.info("Heartbeat ricevuto e sessione aggiornata per token");
                return true;
            } else {
                logger.warn("Heartbeat ricevuto per token non valido");
                return false;
            }
        
        } catch (Exception e) {
            return false;
        }
    }
}
