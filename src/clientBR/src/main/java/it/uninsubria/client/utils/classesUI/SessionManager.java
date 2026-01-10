package it.uninsubria.client.utils.classesUI;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.prefs.Preferences;
import java.util.logging.Logger;
import it.uninsubria.client.utils.classesLogic.ProfileImageListener;
import it.uninsubria.shared.model.User;
import it.uninsubria.shared.model.Book;
import it.uninsubria.shared.rmi.UserService;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;


public class SessionManager {
    private static final Logger logger = Logger.getLogger(SessionManager.class.getName());
    
    // Specialized services
    private final EncryptionService encryptionService;
    private final SessionCache sessionCache;
    private final ProfileImageManager profileImageManager;
    private final HeartbeatService heartbeatService;
    
    // Legacy instance variables for backward compatibility
    private volatile boolean loggedIn = false;
    private volatile String username = null;
    private volatile String email = null;
    private volatile String sessionToken = null;
    private volatile File profileImageFile = null;
    private volatile User loggedUser;
    private volatile ObjectProperty<User> currentUser = new SimpleObjectProperty<>();
    
    // Cache timeout loaded from configuration
    private static final long CACHE_TIMEOUT_MS = ClientAppConfig.getCacheTimeoutMs();
    
    // Heartbeat interval
    private static final int HEARTBEAT_INTERVAL_MINUTES = 5;

    /**
     * Thread-safe singleton using holder class idiom
     */
    private static class SingletonHolder {
        private static final SessionManager INSTANCE = new SessionManager();
    }

    private SessionManager() {
        this.encryptionService = new EncryptionService();
        this.sessionCache = new SessionCache(encryptionService);
        this.profileImageManager = new ProfileImageManager();
        this.heartbeatService = new HeartbeatService(
            HEARTBEAT_INTERVAL_MINUTES,
            this::sendHeartbeatInternal
        );
        
        loadFromCache();
    }

    /**
     * Internal UserSession class
     */
    public static class UserSession {
        private String userId;
        private String username;
        private String email;
        private String sessionToken;
        private long loginTimestamp;
        private File profileImageFile;
        private User user;
        private List<Book> recentlyOpenedBooks;
        private static final int MAX_RECENT_BOOKS = 10;

        public UserSession(String userId, String username, String email, String sessionToken) {
            this.userId = userId;
            this.username = username;
            this.email = email;
            this.sessionToken = sessionToken;
            this.loginTimestamp = System.currentTimeMillis();
            this.recentlyOpenedBooks = new ArrayList<>();
        }

        public UserSession(String userId, String username, String email, String sessionToken, User user) {
            this(userId, username, email, sessionToken);
            this.user = user;
        }

        // Getters and setters
        public String getUserId() { return userId; }
        public String getUsername() { return username; }
        public String getEmail() { return email; }
        public String getSessionToken() { return sessionToken; }
        public long getLoginTimestamp() { return loginTimestamp; }
        public File getProfileImageFile() { return profileImageFile; }
        public void setProfileImageFile(File file) { this.profileImageFile = file; }
        public User getUser() { return user; }
        public void setUser(User user) { this.user = user; }
        public List<Book> getRecentlyOpenedBooks() { return recentlyOpenedBooks; }

        /**
         * Adds a book to recently opened list
         * Maintains only last MAX_RECENT_BOOKS books
         */
        public void addRecentlyOpenedBook(Book book) {
            if (book == null) return;
            recentlyOpenedBooks.removeIf(b -> b.getBook_id() == book.getBook_id());
            recentlyOpenedBooks.add(0, book);
            if (recentlyOpenedBooks.size() > MAX_RECENT_BOOKS) {
                recentlyOpenedBooks = recentlyOpenedBooks.subList(0, MAX_RECENT_BOOKS);
            }
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - loginTimestamp > CACHE_TIMEOUT_MS;
        }
    }

    public static SessionManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    // ===== LOAD/SAVE METHODS (delegated to SessionCache) =====

    /**
     * Load sessions from cache
     */
    private void loadSessionsFromCache() {
        UserSession activeSession = sessionCache.getActiveSession();
        Map<String, UserSession> allSessions = sessionCache.getAllSessions();

        if (allSessions != null && !allSessions.isEmpty()) {
            userSessions.putAll(allSessions);

            if (activeSession != null) {
                activeUserId = activeSession.getUserId();
                syncLegacyFieldsWithSession(activeSession);
            }

            logger.info("Loaded " + userSessions.size() + " user sessions from cache");
        }
    }

    /**
     * Save all user sessions to cache (compatibility method)
     */
    public void saveAllUserSessions() {
        // Save current active session to cache
        UserSession activeSession = getActiveUserSession();
        if (activeSession != null) {
            sessionCache.addSession(activeSession);
        }
    }

    /**
     * Save to cache (compatibility method)
     */
    public void saveToCache() {
        saveAllUserSessions();
    }

    /**
     * Load from cache (compatibility method)
     */
    public boolean loadFromCache() {
        loadSessionsFromCache();
        return isLoggedIn();
    }

    /**
     * Clear cache
     */
    public void clearCache() {
        stopHeartbeat();
        sessionCache.clearAllSessions();
        resetLegacyFields();
    }

    // ===== MULTI-SESSION MANAGEMENT (delegated to SessionCache) =====

    /**
     * Add a new user session
     */
    public void addUserSession(String userId, String username, String email, String token, User user) {
        UserSession session = new UserSession(userId, username, email, token, user);
        
        // Save to cache
        sessionCache.addSession(session);
        
        // Update local state
        activeUserId = userId;
        userSessions.put(userId, session);
        syncLegacyFieldsWithSession(session);
        
        // Start heartbeat
        startHeartbeat();
        
        logger.info("Session added for user: " + (username != null ? username : email));
    }

    /**
     * Switch to specific user
     */
    public void switchToUser(String userId) {
        if (userSessions.containsKey(userId)) {
            UserSession session = userSessions.get(userId);
             
            // Save current state before switching
            if (activeUserId != null && userSessions.containsKey(activeUserId)) {
                UserSession currentSession = userSessions.get(activeUserId);
                currentSession.setProfileImageFile(profileImageFile);
            }
             
            // Update cache
            sessionCache.setActiveUserId(userId);
             
            // Load new user
            activeUserId = userId;
            syncLegacyFieldsWithSession(session);
             
            // Restart heartbeat (stop first to avoid warning)
            stopHeartbeat();
            startHeartbeat();
             
            logger.info("Switched to user: " + (session.getUsername() != null ? session.getUsername() : session.getEmail()));
        }
    }

    /**
     * Remove a user session
     */
    public void removeUserSession(String userId) {
        if (userSessions.containsKey(userId)) {
            sessionCache.removeSession(userId);
            userSessions.remove(userId);

            if (userId.equals(activeUserId)) {
                if (!userSessions.isEmpty()) {
                    performCompleteLogout();
                } else {
                    String nextUserId = userSessions.keySet().iterator().next();
                    switchToUser(nextUserId);
                }
            }
        }
    }

    /**
     * Get all user sessions
     */
    public List<UserSession> getAllUserSessions() {
        return new ArrayList<>(userSessions.values());
    }

    /**
     * Get all user sessions with display names for UI (thread-safe copy)
     */
    public List<Map.Entry<String, String>> getAllUserSessionsWithNames() {
        List<Map.Entry<String, String>> result = new ArrayList<>();
        synchronized (userSessions) {
            result = userSessions.entrySet().stream()
                .map(entry -> new java.util.AbstractMap.SimpleEntry<>(
                    entry.getKey(),
                    entry.getValue().getUsername() != null ? entry.getValue().getUsername() : entry.getValue().getEmail()
                ))
                .collect(java.util.stream.Collectors.toList());
        }
        return result;
    }

    /**
     * Get active user session
     */
    public UserSession getActiveUserSession() {
        if (activeUserId == null) {
            return null;
        }
        return userSessions.get(activeUserId);
    }

    // ===== RECENTLY OPENED BOOKS =====

    /**
     * Add recently opened book for active user
     */
    public void addRecentlyOpenedBook(Book book) {
        UserSession activeSession = getActiveUserSession();
        if (activeSession != null) {
            activeSession.addRecentlyOpenedBook(book);
            // Update session in cache
            sessionCache.addSession(activeSession);
        }
    }

    /**
     * Get recently opened books for active user
     */
    public List<Book> getRecentlyOpenedBooks() {
        UserSession activeSession = getActiveUserSession();
        return activeSession != null 
            ? new ArrayList<>(activeSession.getRecentlyOpenedBooks()) 
            : new ArrayList<>();
    }

    /**
     * Set recently opened books (for restore)
     */
    public void setRecentlyOpenedBooks(List<Integer> bookIds) {
        UserSession activeSession = getActiveUserSession();
        if (activeSession != null) {
            activeSession.getRecentlyOpenedBooks().clear();
            sessionCache.addSession(activeSession);
        }
    }

    // ===== COMPATIBILITY METHODS =====

    public void loginUsername(String username) {
        if (activeUserId != null && userSessions.containsKey(activeUserId)) {
            UserSession session = userSessions.get(activeUserId);
            session.username = username;
            this.username = username;
            this.loggedIn = true;
            sessionCache.addSession(session);
        }
    }

    public void loginEmail(String email) {
        if (activeUserId != null && userSessions.containsKey(activeUserId)) {
            UserSession session = userSessions.get(activeUserId);
            session.email = email;
            this.email = email;
            this.loggedIn = true;
            sessionCache.addSession(session);
        }
    }

    public void loginWithToken(String username, String email, String token) {
        String userId = username != null ? username : email;
        if (userId != null) {
            addUserSession(userId, username, email, token, null);
        }
    }

    public void logout() {
        // logout COMPLETO e DEFINITIVO - pulisce tutto indipendentemente dalle sessioni salvate
        performCompleteLogout();
    }

    private void performCompleteLogout() {
        String token = sessionToken;
        if (token != null && !token.isEmpty()) {
            try {
                UserService userService = ServiceLocator.getUserService();
                if (userService != null) {
                    userService.invalidateSession(token);
                }
            } catch (Exception e) {
                logger.warning("Failed to invalidate session on server: " + e.getMessage());
            }
        }

        stopHeartbeat();
        resetLegacyFields();

        userSessions.clear();

        sessionCache.clearAllSessions();
    }

    // ===== PROFILE IMAGE MANAGEMENT (delegated to ProfileImageManager) =====

    public void addProfileImageListener(ProfileImageListener listener) {
        profileImageManager.addListener(listener);
    }

    public void removeProfileImageListener(ProfileImageListener listener) {
        profileImageManager.removeListener(listener);
    }

    public File setProfileImageFile(File file) {
        this.profileImageFile = file;
        
        if (activeUserId != null && userSessions.containsKey(activeUserId)) {
            userSessions.get(activeUserId).setProfileImageFile(file);
            profileImageManager.notifyProfileImageChanged(file);
        }
        
        return file;
    }

    public File setProfileImageFile(byte[] imageData, String mimeType) {
        File file = profileImageManager.saveProfileImage(imageData, mimeType, activeUserId);
        if (file != null) {
            this.profileImageFile = file;
            
            if (activeUserId != null && userSessions.containsKey(activeUserId)) {
                userSessions.get(activeUserId).setProfileImageFile(file);
                profileImageManager.notifyProfileImageChanged(file);
            }
        }
        return file;
    }

    public File getProfileImageFile() {
        return profileImageFile;
    }

    public void applyProfileImage(ImageView imageView) {
        profileImageManager.updateProfileImageView(imageView, profileImageFile);
    }

    public void applyProfileImageForUser(String userId, ImageView imageView) {
        if (userSessions.containsKey(userId)) {
            File file = userSessions.get(userId).getProfileImageFile();
            profileImageManager.updateProfileImageView(imageView, file);
        }
    }

    public static void setProfileImage(ImageView imageView, byte[] imageData, String mimeType) {
        ProfileImageManager manager = new ProfileImageManager();
        manager.updateProfileImageView(imageView, imageData, mimeType);
    }

    public static void updateProfileImageView(ImageView imageView, File imageFile) {
        ProfileImageManager manager = new ProfileImageManager();
        manager.updateProfileImageView(imageView, imageFile);
    }

    public static void updateProfileImageView(ImageView imageView, byte[] imageData, String mimeType) {
        ProfileImageManager manager = new ProfileImageManager();
        manager.updateProfileImageView(imageView, imageData, mimeType);
    }

    public static boolean updateLoginInfoButtonVisibility(HBox HBoxMethod, Button buttonSelecting) {
        boolean loggedIn = SessionManager.getInstance().isLoggedIn();
        try {
            if (loggedIn) {
                HBoxMethod.getChildren().clear();
                buttonSelecting.setVisible(false);
                buttonSelecting.setDisable(true);
                return true;
            } else {
                HBoxMethod.getChildren().clear();
                HBoxMethod.getChildren().add(buttonSelecting);
                buttonSelecting.setVisible(true);
                buttonSelecting.setDisable(false);
                buttonSelecting.setScaleX(0.8);
                buttonSelecting.setScaleY(0.8);
                UIAnimator.animateFadeInScale(buttonSelecting, 0.4);
            }
        } catch (Exception e) {
            logger.warning("Error updating login button visibility: " + e.getMessage());
        }
        return false;
    }

    // ===== GETTERS/SETTERS (legacy compatibility) =====

    public void setUser(User user) {
        this.loggedUser = user;
        
        if (activeUserId != null && userSessions.containsKey(activeUserId)) {
            userSessions.get(activeUserId).setUser(user);
            sessionCache.addSession(userSessions.get(activeUserId));
        }
        
        this.currentUser.set(user);
    }

    public User getUser() {
        UserSession activeSession = getActiveUserSession();
        if (activeSession != null && activeSession.getUser() != null) {
            return activeSession.getUser();
        }
        return loggedUser;
    }

    public void setUserInfo(User user) {
        setUser(user);
    }

    public ReadOnlyObjectProperty<User> currentUserProperty() {
        return currentUser;
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public boolean isLoggedOut() {
        return !loggedIn;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public String getActiveUserId() {
        return activeUserId;
    }

    public boolean hasMultipleUsers() {
        return userSessions.size() > 1;
    }

    public int getUserCount() {
        return userSessions.size();
    }

    public boolean isGuest() {
        return !loggedIn;
    }

    // ===== LANGUAGE MANAGEMENT =====

    private static final String PREF_NODE = "BookRecommenderClient";

    public String getCurrentLanguage() {
        try {
            Preferences prefs = Preferences.userRoot().node(PREF_NODE);
            return prefs.get("currentLanguage", "it");
        } catch (Exception e) {
            logger.warning("Failed to get current language: " + e.getMessage());
            return "it";
        }
    }

    public void setCurrentLanguage(String language) {
        try {
            Preferences prefs = Preferences.userRoot().node(PREF_NODE);
            prefs.put("currentLanguage", language);
            prefs.flush();
        } catch (Exception e) {
            logger.warning("Failed to set current language: " + e.getMessage());
        }
    }

    public String resolveString(String key) {
        return LanguageManager.getString(key);
    }

    public String resolveString(String key, Object... args) {
        return LanguageManager.getString(key, args);
    }

    public static String resolveStringStatic(String key) {
        return LanguageManager.getString(key);
    }

    // ===== REMEMBER ME CREDENTIALS (delegated to SessionCache) =====

    public void saveRememberMeCredentials(String username, String password) {
        sessionCache.saveRememberMeCredentials(username, password);
    }

    public String[] getRememberMeCredentials() {
        return sessionCache.getRememberMeCredentials();
    }

    public void clearRememberMeCredentials() {
        sessionCache.clearRememberMeCredentials();
    }

    // ===== HEARTBEAT SYSTEM (delegated to HeartbeatService) =====

    private void startHeartbeat() {
        if (sessionToken == null || sessionToken.isEmpty()) {
            logger.info("Heartbeat not started: no session token available");
            return;
        }
        
        heartbeatService.start();
        
        logger.info("Started heartbeat every " + HEARTBEAT_INTERVAL_MINUTES + " minutes for user: " +
            (username != null ? username : (email != null ? email : "unknown")));
    }

    private void stopHeartbeat() {
        heartbeatService.stop();
    }

    private void sendHeartbeatInternal() {
        // Check if still logged in and have valid token
        if (!isLoggedIn() || sessionToken == null || sessionToken.isEmpty()) {
            logger.info("Heartbeat skipped: user not logged in or token not available");
            return;
        }
        
        try {
            boolean heartbeatSuccess = ServiceLocator.getUserService().heartbeat(sessionToken);
            
            if (heartbeatSuccess) {
                logger.info("✅ Heartbeat sent successfully to server for session: " +
                    (username != null ? username : (email != null ? email : "unknown")));
            } else {
                logger.severe("❌ Heartbeat rejected by server - session may have expired");
                performCompleteLogout();
            }
        } catch (java.rmi.ConnectException e) {
            logger.severe("❌ Heartbeat connection error: server not reachable - " + e.getMessage());
        } catch (java.rmi.RemoteException e) {
            logger.severe("❌ Heartbeat RMI error: " + e.getMessage());
        } catch (Exception e) {
            logger.severe("❌ Heartbeat unexpected error: " + e.getMessage());
        }
    }

    // ===== HELPER METHODS =====

    private void syncLegacyFieldsWithSession(UserSession session) {
        this.username = session.getUsername();
        this.email = session.getEmail();
        this.sessionToken = session.getSessionToken();
        this.loggedIn = true;
        this.loggedUser = session.getUser();
        this.currentUser.set(session.getUser());
        this.profileImageFile = session.getProfileImageFile();
    }

    private void resetLegacyFields() {
        this.activeUserId = null;
        this.loggedIn = false;
        this.username = null;
        this.email = null;
        this.sessionToken = null;
        this.profileImageFile = null;
        this.loggedUser = null;
        this.currentUser.set(null);
    }

    // Local session storage for fast access
    private final Map<String, UserSession> userSessions = new ConcurrentHashMap<>();
    private volatile String activeUserId = null;
}
