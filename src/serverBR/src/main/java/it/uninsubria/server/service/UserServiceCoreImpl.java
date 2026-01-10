package it.uninsubria.server.service;

import it.uninsubria.server.cache.CacheManager;
import it.uninsubria.server.dao.UserDAO;
import it.uninsubria.shared.model.User;

public class UserServiceCoreImpl implements UserServiceCore {
    private final UserDAO userDAO;

    public UserServiceCoreImpl(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    @Override
    public boolean registerUser(User utente) throws Exception {
        boolean result = userDAO.registerUser(utente);
        if (result) {
            // Invalidate cache for the new user
            CacheManager.invalidateUserData(utente.getID());
            CacheManager.invalidateUserData(utente.getEmail());
        }
        return result;
    }

    @Override
    public boolean login(String usernameOrEmail, String password) throws Exception {
        return userDAO.login(usernameOrEmail, password);
    }

    @Override
    public boolean phrasePassword(String userid, String phrase) throws Exception {
        return userDAO.phrasePassword(userid, phrase);
    }

    @Override
    public User getUserByUsernameOrEmail(String usernameOrEmail) throws Exception {
        String cacheKey = "user_" + usernameOrEmail;
        User cached = CacheManager.get("global", cacheKey, User.class);
        if (cached != null) {
            return cached;
        }

        User user = userDAO.getUserByUsernameOrEmail(usernameOrEmail);
        if (user != null) {
            CacheManager.put("global", cacheKey, user, 5 * 60 * 1000L, "users"); // 5 minutes TTL for users
        }
        return user;
    }

    @Override
    public boolean updateProfileImage(User user, byte[] imageBytes, String mimeType) throws Exception {
        return userDAO.updateProfileImage(user, imageBytes, mimeType);
    }

    @Override
    public boolean updateProfileInfo(User user) throws Exception {
        boolean result = userDAO.updateProfileInfo(user);
        if (result) {
            // Invalidate cache for the updated user
            CacheManager.invalidateUserData(user.getID());
            CacheManager.invalidateUserData(user.getEmail());
        }
        return result;
    }

    public boolean getPasswordByUserID(String userID, String outPassword) throws Exception {
        return userDAO.getPasswordByUserID(userID, outPassword);
    }

    @Override
    public boolean updatePassword(String userID, String newPassword) throws Exception {
        return userDAO.updatePassword(userID, newPassword);
    }

    @Override
    public boolean deleteUser(String userID) throws Exception {
        return userDAO.deleteUser(userID);
    }

    @Override
    public boolean verifyRecoveryPhrase(String email, String hashedPhrase) throws Exception {
        return userDAO.verifyRecoveryPhrase(email, hashedPhrase);
    }

    @Override
    public boolean resetPassword(String email, String newPassword) throws Exception {
        return userDAO.resetPassword(email, newPassword);
    }
}
