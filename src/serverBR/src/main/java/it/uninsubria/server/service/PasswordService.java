package it.uninsubria.server.service;

import java.util.logging.Logger;

import it.uninsubria.server.util.PasswordHashUtil;

/**
 * Service for password-related operations.
 * Encapsulates password hashing and verification logic.
 */
public class PasswordService {
    private static final Logger logger = Logger.getLogger(PasswordService.class.getName());

    /**
     * Verifies a password against a stored hash.
     *
     * @param plainPassword the plain text password to verify
     * @param hashedPassword the stored hashed password
     * @return true if the password matches, false otherwise
     */
    public boolean verifyPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) {
            logger.warning("Cannot verify password: null arguments provided");
            return false;
        }
        return PasswordHashUtil.verifyPassword(plainPassword, hashedPassword);
    }

    /**
     * Hashes a plain text password using bcrypt.
     *
     * @param plainPassword the plain text password to hash
     * @return the hashed password
     */
    public String hashPassword(String plainPassword) {
        if (plainPassword == null) {
            logger.warning("Cannot hash password: null argument provided");
            throw new IllegalArgumentException("Password cannot be null");
        }
        return PasswordHashUtil.hashPassword(plainPassword);
    }

    /**
     * Validates password strength.
     *
     * @param password the password to validate
     * @return true if password meets strength requirements, false otherwise
     */
    public boolean isPasswordValid(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        boolean hasUppercase = false;
        boolean hasLowercase = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;
        String specialChars = "!@#$%^&*()_-+=<>?[]{}|;:,.";
        
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUppercase = true;
            else if (Character.isLowerCase(c)) hasLowercase = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else if (specialChars.indexOf(c) >= 0) hasSpecial = true;
        }
        
        return hasUppercase && hasLowercase && hasDigit && hasSpecial;
    }
}
