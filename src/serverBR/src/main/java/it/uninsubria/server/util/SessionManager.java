package it.uninsubria.server.util;

import it.uninsubria.shared.exception.SecurityException;
import it.uninsubria.shared.model.User;
import it.uninsubria.shared.utils.AppConstants;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

/**
 * Session manager for handling user sessions with JWT tokens.
 * Provides secure session creation, validation, and cleanup.
 */
public final class SessionManager {

    /** Logger for this class. */
    private static final Logger LOGGER =
            Logger.getLogger(SessionManager.class.getName());

    /** In-memory store for active sessions. */
    private static final ConcurrentHashMap<String, UserSession> ACTIVE_SESSIONS=
            new ConcurrentHashMap<>();

    /** Cleanup scheduler for expired sessions. */
    private static final ScheduledExecutorService CLEANUP_SCHEDULER =
            Executors.newSingleThreadScheduledExecutor();

    /** Session timeout in milliseconds (30 minutes without heartbeat). */
    private static final long SESSION_TIMEOUT = AppConstants.Time.TIMEOUT_30_MINUTES;

    /** Maximum inactivity time before session is considered dead (15 minutes). */
    private static final long MAX_INACTIVITY_TIME = AppConstants.Time.TIMEOUT_15_MINUTES;

    /** Terminator wait time for scheduler shutdown. */
    private static final int TERMINATION_WAIT_SECONDS = 5;

    /** Prevents external instantiation. */
    private SessionManager() { /* utility class */ }

    static {
        // Schedule cleanup task to run every hour
        CLEANUP_SCHEDULER.scheduleAtFixedRate(
                SessionManager::cleanupExpiredSessions,
                1, 1, TimeUnit.HOURS);
    }

    /**
     * Creates a new session for the user and returns a token.
     *
     * @param user the user to create session for
     * @return JWT token for the session
     * @throws SecurityException if session creation fails
     */
    public static String createSession(User user) {
        try {
            // INVALIDA TUTTE LE SESSIONI PRECEDENTI DELL'UTENTE
            // per garantire che un utente possa essere loggato in un solo client alla volta
            int invalidatedCount = invalidateUserSessions(user.getUser_id());
            if (invalidatedCount > 0) {
                LOGGER.info("Invalidated " + invalidatedCount + " previous sessions for user: " + user.getID());
            }

            String token = JWTUtil.generateToken(
                    user.getUser_id(), user.getID(), user.getEmail());
            UserSession session = new UserSession(
                    user.getUser_id(), user.getID(), user.getEmail(),
                    System.currentTimeMillis(), System.currentTimeMillis()
                    + SESSION_TIMEOUT
            );
            ACTIVE_SESSIONS.put(token, session);
            LOGGER.info("Session created for user: "
                + user.getID()
                + " (Active sessions: " + ACTIVE_SESSIONS.size() + ")");
            return token;
        } catch (Exception e) {
            LOGGER.severe("Failed to create session for user "
            + user.getID()
            + ": "
            + e.getMessage());
            throw new SecurityException("Session creation failed", e);
        }
    }

    /**
     * Validates a session token and returns session.
     * Uses atomic operation to avoid TOCTOU race conditions.
     *
     * @param token session token
     * @return UserSession if valid, otherwise null
     */
    public static UserSession validateSession(String token) {
        if (token == null || token.trim().isEmpty()) {
            LOGGER.warning("Empty or null token provided");
            return null;
        }
        if (!JWTUtil.isValidTokenFormat(token)) {
            LOGGER.warning("Invalid token format provided");
            return null;
        }
        try {
            UserSession validatedSession = ACTIVE_SESSIONS.computeIfPresent(token, (k, session) -> {
                if (session == null) {
                    LOGGER.warning("Session not found for token");
                    return null;
                }
                if (session.isExpired()) {
                    LOGGER.warning("Session expired for user: " + session.getUsername());
                    return null;
                }
                JWTUtil.validateToken(token);
                session.updateLastActivity();
                LOGGER.fine("Session validated for user: " + session.getUsername());
                return session;
            });

            if (validatedSession == null) {
                ACTIVE_SESSIONS.remove(token);
            }

            return validatedSession;
        } catch (SecurityException e) {
            LOGGER.warning("Session validation failed: " + e.getMessage());
            ACTIVE_SESSIONS.remove(token);
            return null;
        }

    }

    /**
     * Invalidates a session token.
     *
     * @param token the token to invalidate
     * @return true if invalidated, false otherwise
     */
    public static boolean invalidateSession(String token) {
        if (token == null) return false;
        UserSession removed = ACTIVE_SESSIONS.remove(token);
        if (removed != null) {
            LOGGER.info("Session invalidated for user: " + removed.getUsername() +
                    " (Active sessions: " + ACTIVE_SESSIONS.size() + ")");
            return true;
        }
        return false;
    }

    /**
     * Updates the last activity timestamp for a session (used by heartbeat).
     *
     * @param token session token
     * @return true if updated, false if session not found or invalid
     */
    public static boolean updateLastActivity(String token) {
        if (token == null) return false;
        UserSession session = ACTIVE_SESSIONS.get(token);
        if (session != null && !session.isExpired()) {
            session.updateLastActivity();
            LOGGER.fine("Last activity updated for session: " + session.getUsername());
            return true;
        }
        return false;
    }

    /**
     * Invalidates all sessions for a specific user.
     *
     * @param userId user ID whose sessions to invalidate
     * @return number of sessions invalidated
     */
    public static int invalidateUserSessions(Integer userId) {
        if (userId == null) return 0;
        int count = 0;
        List<String> toRemove = new ArrayList<>();
        for (Map.Entry<String, UserSession> ent : ACTIVE_SESSIONS.entrySet()) {
            if (ent.getValue().getUserId().equals(userId)) {
                toRemove.add(ent.getKey());
            }
        }
        for (String k : toRemove) {
            UserSession rs = ACTIVE_SESSIONS.remove(k);
            if (rs != null) {
                count++;
                LOGGER.info("Session invalidated for user ID: " + userId);
            }
        }
        return count;
    }

    /**
     * Refreshes an existing session token.
     *
     * @param token existing token
     * @return new token or null
     */
    public static String refreshSession(String token) {
        UserSession session = validateSession(token);
        if (session == null) return null;
        try {
            String newToken = JWTUtil.refreshToken(token);
            UserSession newSession = new UserSession(
                    session.getUserId(), session.getUsername(), session.getEmail(),
                    session.getCreatedAt(), System.currentTimeMillis() + SESSION_TIMEOUT
            );
            ACTIVE_SESSIONS.remove(token);
            ACTIVE_SESSIONS.put(newToken, newSession);
            LOGGER.info("Session refreshed for user: " + session.getUsername());
            return newToken;
        } catch (Exception e) {
            LOGGER.severe("Failed to refresh session: " + e.getMessage());
            return null;
        }
    }

    /**
     * Gets the number of active sessions.
     *
     * @return count of active sessions
     */
    public static int getActiveSessionCount() {
        return ACTIVE_SESSIONS.size();
    }

    /**
     * Checks if a user already has an active session.
     *
     * @param userId the user ID to check
     * @return true if the user has an active session, false otherwise
     */
    public static boolean hasActiveSession(Integer userId) {
        if (userId == null) return false;
        return ACTIVE_SESSIONS.values().stream()
                .anyMatch(session -> userId.equals(session.getUserId()));
    }

    /**
     * Gets a session by token.
     *
     * @param token token
     * @return UserSession or null
     */
    public static UserSession getSession(String token) {
        return ACTIVE_SESSIONS.get(token);
    }

    /**
     * Cleans up expired sessions.
     */
    private static void cleanupExpiredSessions() {
        int removedCount = 0;
        List<String> toRemove = new ArrayList<>();
        long now = System.currentTimeMillis();

        for (Map.Entry<String, UserSession> ent : ACTIVE_SESSIONS.entrySet()) {
            UserSession session = ent.getValue();

            // Rimuovi se scaduta normalmente O troppo tempo senza attività
            boolean isExpiredNormally = session.isExpired();
            boolean isInactive = (now - session.getLastActivity()) > MAX_INACTIVITY_TIME;

            if (isExpiredNormally || isInactive) {
                toRemove.add(ent.getKey());
                if (isExpiredNormally) {
                    LOGGER.fine("Session expired normally for user: " + session.getUsername());
                } else if (isInactive) {
                    LOGGER.info("Session removed due to inactivity for user: " + session.getUsername() +
                            " (last activity: " + (now - session.getLastActivity()) / AppConstants.Time.MILLIS_PER_SECOND + "s ago)");
                }
            }
        }

        for (String k : toRemove) {
            ACTIVE_SESSIONS.remove(k);
            removedCount++;
        }

        if (removedCount > 0) {
            LOGGER.info("Cleaned up " + removedCount +
                    " expired/inactive sessions. Active: " + ACTIVE_SESSIONS.size());
        }
    }

    /**
     * Shuts down the session manager.
     */
    public static void shutdown() {
        LOGGER.info("Shutting down SessionManager...");
        CLEANUP_SCHEDULER.shutdown();
        try {
            if (!CLEANUP_SCHEDULER.awaitTermination(TERMINATION_WAIT_SECONDS,
                    TimeUnit.SECONDS)) {
                CLEANUP_SCHEDULER.shutdownNow();
            }
        } catch (InterruptedException e) {
            CLEANUP_SCHEDULER.shutdownNow();
            Thread.currentThread().interrupt();
        }
        int sessionCount = ACTIVE_SESSIONS.size();
        ACTIVE_SESSIONS.clear();
        LOGGER.info("SessionManager shutdown. Cleared " + sessionCount + " sessions.");
    }

    /**
     * Inner class representing a user session.
     */
    public static final class UserSession {
        private final Integer userId;
        private final String username;
        private final String email;
        private final long createdAt;
        private long lastActivity;
        private final long expiresAt;

        public UserSession(Integer userId, String username, String email,
                long createdAt, long expiresAt) {
            this.userId = userId;
            this.username = username;
            this.email = email;
            this.createdAt = createdAt;
            this.lastActivity = createdAt;
            this.expiresAt = expiresAt;
        }

        public Integer getUserId() { return userId; }
        public String getUsername() { return username; }
        public String getEmail() { return email; }
        public long getCreatedAt() { return createdAt; }
        public long getLastActivity() { return lastActivity; }
        public long getExpiresAt() { return expiresAt; }

        public void updateLastActivity() {
            this.lastActivity = System.currentTimeMillis();
        }
        public boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
        public long getTimeUntilExpiration() {
            return Math.max(0, expiresAt - System.currentTimeMillis());
        }
    }
}
