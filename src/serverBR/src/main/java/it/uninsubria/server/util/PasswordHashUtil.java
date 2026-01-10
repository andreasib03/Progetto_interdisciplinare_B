package it.uninsubria.server.util;

import at.favre.lib.crypto.bcrypt.BCrypt;

/**
 * Utility class for password hashing and verification using BCrypt.
 * Provides secure password storage with automatic salt generation.
 */
public class PasswordHashUtil {
    
    private static final int COST_FACTOR = 12; // Recommended minimum cost factor
    
    /**
     * Hashes a password using BCrypt with automatic salt generation.
     * 
     * @param plainPassword the plain text password to hash
     * @return the hashed password as a string
     * @throws IllegalArgumentException if plainPassword is null or empty
     */
    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        
        return BCrypt.withDefaults().hashToString(COST_FACTOR, plainPassword.toCharArray());
    }
    
    /**
     * Verifies a plain password against a hashed password.
     * 
     * @param plainPassword the plain text password to verify
     * @param hashedPassword the hashed password to verify against
     * @return true if the password matches, false otherwise
     * @throws IllegalArgumentException if either parameter is null or empty
     */
    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || plainPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Plain password cannot be null or empty");
        }
        
        if (hashedPassword == null || hashedPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Hashed password cannot be null or empty");
        }
        
        BCrypt.Result result = BCrypt.verifyer().verify(plainPassword.toCharArray(), hashedPassword);
        return result.verified;
    }
    
    /**
     * Checks if a password is already hashed (starts with $2b$, $2a$, etc.).
     * 
     * @param password the password to check
     * @return true if the password appears to be BCrypt hashed, false otherwise
     */
    public static boolean isHashed(String password) {
        return password != null && password.startsWith("$2");
    }
}