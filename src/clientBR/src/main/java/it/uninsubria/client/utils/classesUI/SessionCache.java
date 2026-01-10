package it.uninsubria.client.utils.classesUI;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.prefs.Preferences;
import java.util.logging.Logger;

public class SessionCache {
    private static final Logger logger = Logger.getLogger(SessionCache.class.getName());

    private static final String PREF_NODE = "BookRecommenderClient";
    private static final String KEY_ACTIVE_USER = "activeUserId";
    private static final String KEY_USERS_LIST = "usersList";
    private static final String KEY_REMEMBER_USERNAME = "rememberUsername";
    private static final String KEY_REMEMBER_PASSWORD = "rememberPassword";
    private static final String KEY_REMEMBER_ME = "rememberMe";

    private final EncryptionService encryptionService;
    private final Map<String, SessionManager.UserSession> userSessions = new ConcurrentHashMap<>();
    private volatile String activeUserId = null;

    public SessionCache(EncryptionService encryptionService) {
        this.encryptionService = encryptionService;
        loadAllUserSessions();
    }

    public SessionManager.UserSession getActiveSession() {
        if (activeUserId == null) {
            return null;
        }
        return userSessions.get(activeUserId);
    }

    public String getActiveUserId() {
        return activeUserId;
    }

    public void setActiveUserId(String userId) {
        this.activeUserId = userId;
        saveAllUserSessions();
    }

    public SessionManager.UserSession getSession(String userId) {
        return userSessions.get(userId);
    }

    public Map<String, SessionManager.UserSession> getAllSessions() {
        return new ConcurrentHashMap<>(userSessions);
    }

    public void addSession(SessionManager.UserSession session) {
        if (session == null || session.getUserId() == null) {
            return;
        }
        userSessions.put(session.getUserId(), session);
        activeUserId = session.getUserId();
        saveAllUserSessions();
    }

    public void removeSession(String userId) {
        userSessions.remove(userId);
        if (userId.equals(activeUserId)) {
            activeUserId = null;
        }
        saveAllUserSessions();
    }

    public void clearAllSessions() {
        userSessions.clear();
        activeUserId = null;
        saveAllUserSessions();
    }

    private void loadAllUserSessions() {
        try {
            Preferences prefs = Preferences.userRoot().node(PREF_NODE);

            activeUserId = prefs.get(KEY_ACTIVE_USER, "");
            String usersList = prefs.get(KEY_USERS_LIST, "");

            if (usersList.isEmpty()) {
                return;
            }

            String[] userIds = usersList.split(",");
            for (String userId : userIds) {
                if (!userId.trim().isEmpty()) {
                    SessionManager.UserSession session = loadUserSession(prefs, userId.trim());
                    if (session != null) {
                        userSessions.put(userId.trim(), session);
                    }
                }
            }

            logger.info("Loaded " + userSessions.size() + " user sessions from cache");
        } catch (Exception e) {
            logger.severe("Failed to load user sessions: " + e.getMessage());
        }
    }

    private void saveAllUserSessions() {
        try {
            Preferences prefs = Preferences.userRoot().node(PREF_NODE);

            prefs.put(KEY_ACTIVE_USER, activeUserId != null ? activeUserId : "");

            StringBuilder usersList = new StringBuilder();
            for (String userId : userSessions.keySet()) {
                if (usersList.length() > 0) {
                    usersList.append(",");
                }
                usersList.append(userId);
            }
            prefs.put(KEY_USERS_LIST, usersList.toString());

            for (SessionManager.UserSession session : userSessions.values()) {
                saveUserSession(prefs, session);
            }

            prefs.flush();
            logger.fine("Saved " + userSessions.size() + " user sessions to cache");
        } catch (Exception e) {
            logger.severe("Failed to save user sessions: " + e.getMessage());
        }
    }

    private SessionManager.UserSession loadUserSession(Preferences prefs, String userId) {
        try {
            String username = prefs.get("user_" + userId + "_username", "");
            String email = prefs.get("user_" + userId + "_email", "");
            String token = prefs.get("user_" + userId + "_token", "");

            if (token.isEmpty()) {
                return null;
            }

            String decryptedToken = encryptionService.decrypt(token);
            if (decryptedToken == null) {
                return null;
            }

            SessionManager.UserSession session = new SessionManager.UserSession(userId, username, email, decryptedToken);

            File profileImageFile = loadProfileImageFile(userId);
            if (profileImageFile != null && profileImageFile.exists()) {
                session.setProfileImageFile(profileImageFile);
            }

            return session;
        } catch (Exception e) {
            logger.warning("Failed to load session for user " + userId + ": " + e.getMessage());
            return null;
        }
    }

    private void saveUserSession(Preferences prefs, SessionManager.UserSession session) {
        try {
            String userId = session.getUserId();
            prefs.put("user_" + userId + "_username", session.getUsername() != null ? session.getUsername() : "");
            prefs.put("user_" + userId + "_email", session.getEmail() != null ? session.getEmail() : "");
            prefs.put("user_" + userId + "_token", encryptionService.encrypt(session.getSessionToken()));
        } catch (Exception e) {
            logger.warning("Failed to save session for user " + session.getUserId() + ": " + e.getMessage());
        }
    }

    private File loadProfileImageFile(String userId) {
        java.nio.file.Path userDir = java.nio.file.Paths.get(
            System.getProperty("user.home"), 
            ".bookrecommender", 
            "profiles"
        );
        
        if (!java.nio.file.Files.exists(userDir)) {
            return null;
        }

        try (java.util.stream.Stream<java.nio.file.Path> stream = java.nio.file.Files.list(userDir)) {
            return stream
                .filter(p -> p.getFileName().toString().startsWith(userId + "."))
                .filter(java.nio.file.Files::isRegularFile)
                .findFirst()
                .map(java.nio.file.Path::toFile)
                .orElse(null);
        } catch (Exception e) {
            logger.warning("Failed to load profile image file for user " + userId + ": " + e.getMessage());
            return null;
        }
    }

    public void saveRememberMeCredentials(String username, String password) {
        try {
            Preferences prefs = Preferences.userRoot().node(PREF_NODE);
            prefs.put(KEY_REMEMBER_USERNAME, username);
            prefs.put(KEY_REMEMBER_PASSWORD, encryptionService.encrypt(password));
            prefs.putBoolean(KEY_REMEMBER_ME, true);
            prefs.flush();
            logger.fine("Saved remember me credentials for user: " + username);
        } catch (Exception e) {
            logger.severe("Failed to save remember me credentials: " + e.getMessage());
        }
    }

    public String[] getRememberMeCredentials() {
        try {
            Preferences prefs = Preferences.userRoot().node(PREF_NODE);
            boolean rememberMe = prefs.getBoolean(KEY_REMEMBER_ME, false);
            
            if (!rememberMe) {
                return null;
            }

            String username = prefs.get(KEY_REMEMBER_USERNAME, "");
            String encryptedPassword = prefs.get(KEY_REMEMBER_PASSWORD, "");

            if (username.isEmpty() || encryptedPassword.isEmpty()) {
                return null;
            }

            String decryptedPassword = encryptionService.decrypt(encryptedPassword);
            if (decryptedPassword == null) {
                return null;
            }

            return new String[]{username, decryptedPassword};
        } catch (Exception e) {
            logger.severe("Failed to load remember me credentials: " + e.getMessage());
            return null;
        }
    }

    public void clearRememberMeCredentials() {
        try {
            Preferences prefs = Preferences.userRoot().node(PREF_NODE);
            prefs.remove(KEY_REMEMBER_USERNAME);
            prefs.remove(KEY_REMEMBER_PASSWORD);
            prefs.remove(KEY_REMEMBER_ME);
            prefs.flush();
            logger.fine("Cleared remember me credentials");
        } catch (Exception e) {
            logger.severe("Failed to clear remember me credentials: " + e.getMessage());
        }
    }
}
