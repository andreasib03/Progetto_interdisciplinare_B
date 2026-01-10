package it.uninsubria.server.util;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import it.uninsubria.shared.exception.SecurityException;
import it.uninsubria.shared.utils.AppConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.io.Decoders;

/**
 * JWT (JSON Web Token) utility for secure session management.
 * Provides token generation, validation, and claims management.
 *
 * <p>REQUIREMENTS:
 * The JWT_SECRET environment variable MUST be set before using this class.
 * Set it with a base64-encoded secret key:
 * <pre>
 * export JWT_SECRET=$(openssl rand -base64 32)
 * </pre>
 * Or on Windows:
 * <pre>
 * set JWT_SECRET=&lt;your-base64-encoded-key&gt;
 * </pre>
 */
public final class JWTUtil {

    /** Logger for this class. */
    private static final java.util.logging.Logger LOGGER =
            java.util.logging.Logger.getLogger(JWTUtil.class.getName());

    /** Secret key for signing tokens. MUST be set via JWT_SECRET environment variable. */
    private static final String SECRET = getSecretFromEnvironment();
    private static final Key SECRET_KEY = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));

    /** Token expiration time (24 hours). */
    private static final long EXPIRATION_TIME = AppConstants.Time.TIMEOUT_1_DAY;

    /** Token issuer. */
    private static final String ISSUER = "BookRecommenderApp";

    /**
     * Retrieves JWT secret from environment variable or uses default for development.
     *
     * @return base64-encoded JWT secret key
     */
    private static String getSecretFromEnvironment() {
        String secret = System.getenv("JWT_SECRET");
        if (secret == null || secret.trim().isEmpty()) {
            LOGGER.warning("JWT_SECRET environment variable not set. Using default key for development.");
            return "VGhpc0lzQURlZmF1bHRTZWNyZXRGb3JEZXZlbG9wbWVudE9ubHk=";
        }
        return secret;
    }

    /** Private constructor to prevent instantiation. */
    private JWTUtil() { /* utility class */ }

    /**
     * Generates a JWT token for the specified user.
     *
     * @param userId the user ID
     * @param username the username
     * @param email the user email
     * @return the generated JWT token
     * @throws SecurityException if token generation fails
     */
    @SuppressWarnings("deprecation")
	public static String generateToken(final Integer userId,
                                       final String username,
                                       final String email) {
        try {
            Map<String, Object> claims = new HashMap<>();
            claims.put("userId", userId);
            claims.put("username", username);
            claims.put("email", email);
            claims.put("role", "USER"); // Default role

            Date now = new Date();
            Date expiration = new Date(now.getTime() + EXPIRATION_TIME);

            return Jwts.builder()
                    .setClaims(claims)
                    .setIssuer(ISSUER)
                    .setSubject(username)
                    .setIssuedAt(now)
                    .setExpiration(expiration)
                    .signWith(SECRET_KEY)
                    .compact();

        } catch (Exception e) {
            LOGGER.severe("Failed to generate JWT token: " + e.getMessage());
            throw new SecurityException("Token generation failed", e);
        }
    }

    /**
     * Validates and parses a JWT token.
     *
     * @param token the JWT token to validate
     * @return the claims contained in the token
     * @throws SecurityException if validation fails
     */
    @SuppressWarnings("deprecation")
	public static Claims validateToken(final String token) {
        try {
            return io.jsonwebtoken.Jwts.parser()
                    .setSigningKey(SECRET_KEY)
                    .requireIssuer(ISSUER)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            LOGGER.warning("JWT token expired: " + e.getMessage());
            throw new SecurityException("Token expired", e);
        } catch (UnsupportedJwtException e) {
            LOGGER.warning("Unsupported JWT token: " + e.getMessage());
            throw new SecurityException("Unsupported token", e);
        } catch (MalformedJwtException e) {
            LOGGER.warning("Malformed JWT token: " + e.getMessage());
            throw new SecurityException("Invalid token format", e);
        } catch (SecurityException e) {
            LOGGER.warning("JWT security exception: " + e.getMessage());
            throw new SecurityException("Token validation failed", e);
        } catch (IllegalArgumentException e) {
            LOGGER.warning("JWT illegal argument: " + e.getMessage());
            throw new SecurityException("Invalid token argument", e);
        } catch (Exception e) {
            LOGGER.severe("Unexpected error during JWT validation: " + e.getMessage());
            throw new SecurityException("Token validation error", e);
        }
    }

    /** Extracts user ID from token. */
    public static Integer getUserIdFromToken(final String token) {
        try {
            Claims claims = validateToken(token);
            return claims.get("userId", Integer.class);
        } catch (Exception e) {
            LOGGER.warning("Failed to extract user ID from token: " + e.getMessage());
            throw new SecurityException("Invalid user token", e);
        }
    }

    /** Extracts username from token. */
    public static String getUsernameFromToken(final String token) {
        try {
            return validateToken(token).getSubject();
        } catch (Exception e) {
            LOGGER.warning("Failed to extract username from token: " + e.getMessage());
            throw new SecurityException("Invalid user token", e);
        }
    }

    /** Checks if a token is expired. */
    public static boolean isTokenExpired(final String token) {
        try {
            return validateToken(token).getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /** Gets expiration date from token. */
    public static Date getExpirationFromToken(final String token) {
        try {
            return validateToken(token).getExpiration();
        } catch (Exception e) {
            LOGGER.warning("Failed to extract expiration from token: " + e.getMessage());
            throw new SecurityException("Invalid token", e);
        }
    }

    /** Refreshes an existing token with the same claims. */
    @SuppressWarnings("deprecation")
	public static String refreshToken(final String token) {
        try {
            Claims oldClaims = validateToken(token);
            Map<String, Object> newClaims = new HashMap<>(oldClaims);
            Date now = new Date();
            Date expiration = new Date(now.getTime() + EXPIRATION_TIME);
            return io.jsonwebtoken.Jwts.builder()
                    .setClaims(newClaims)
                    .setIssuer(ISSUER)
                    .setSubject(oldClaims.getSubject())
                    .setIssuedAt(now)
                    .setExpiration(expiration)
                    .signWith(SECRET_KEY)
                    .compact();
        } catch (Exception e) {
            LOGGER.severe("Failed to refresh JWT token: " + e.getMessage());
            throw new SecurityException("Token refresh failed", e);
        }
    }

    /** Check token format. */
    public static boolean isValidTokenFormat(final String token) {
        if (token == null || token.trim().isEmpty()) return false;
        String[] parts = token.split("\\.");
        return parts.length == 3;
    }

    /** Get time until expiration. */
    public static long getTimeUntilExpiration(final String token) {
        try {
            long remaining = getExpirationFromToken(token).getTime() -
                    System.currentTimeMillis();
            return Math.max(0, remaining);
        } catch (Exception e) {
            return 0;
        }
    }
}
