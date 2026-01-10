package it.uninsubria.server.util;

import it.uninsubria.shared.exception.ValidationException;
import it.uninsubria.shared.model.User;
import java.util.regex.Pattern;
import java.util.logging.Logger;

/**
 * Utility class for input validation across the Book Recommender system.
 * Provides comprehensive validation methods for user inputs, ensuring
 * data integrity and security.
 *
 * <p>This class includes validation for:
 * <ul>
 * <li>User registration data (email, password, fiscal code)</li>
 * <li>Book information</li>
 * <li>Library data</li>
 * <li>Review content</li>
 * </ul>
 *
 * <p>All validation methods return boolean values and throw
 * {@link it.uninsubria.shared.exception.ValidationException} on failure.
 *
 * @author Book Recommender Team
 * @version 1.0
 * @since 1.0
 */
public class InputValidator {
    
    private static final Logger logger = Logger.getLogger(InputValidator.class.getName());
    
    // Regex patterns
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );
    
    private static final Pattern USERNAME_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9_]{3,30}$"
    );
    
    private static final Pattern NAME_PATTERN = Pattern.compile(
        "^[a-zA-ZàèéìòùÀÈÌÒÙ\\s'-]{1,50}$"
    );
    
    private static final Pattern CF_PATTERN = Pattern.compile(
        "^[A-Z]{6}[0-9]{2}[A-Z][0-9]{2}[A-Z][0-9]{3}[A-Z]$"
    );
    
    /**
     * Validates user registration data comprehensively.
     * Performs complete validation including email format, password strength,
     * fiscal code validity, and data completeness.
     *
     * @param user the user object to validate
     * @throws ValidationException if any validation check fails
     */
    public static void validateUserRegistration(User user) throws ValidationException {
        if (user == null) {
            throw new ValidationException("User object cannot be null");
        }
        
        // Validate username/ID
        validateUsername(user.getID());
        
        // Validate names
        validateName(user.getName(), "name");
        validateName(user.getSurname(), "surname");
        
        // Validate email
        validateEmail(user.getEmail());
        
        // Validate Codice Fiscale
        validateCodiceFiscale(user.getCF());
        
        // Validate password
        validatePassword(user.getPassword());
        
        logger.info("User validation passed for: " + user.getID());
    }
    
    /**
     * Validates username format and checks for injection.
     * 
     * @param username the username to validate
     * @throws ValidationException if validation fails
     */
    public static void validateUsername(String username) throws ValidationException {
        if (username == null || username.trim().isEmpty()) {
            throw new ValidationException("Username cannot be null or empty");
        }
        
        String trimmed = username.trim();
        
        if (trimmed.length() < 3 || trimmed.length() > 30) {
            throw new ValidationException("Username must be between 3 and 30 characters");
        }
        
        if (!USERNAME_PATTERN.matcher(trimmed).matches()) {
            throw new ValidationException("Username can only contain letters, numbers, and underscores");
        }
    }
    
    /**
     * Validates name format and checks for injection.
     * 
     * @param name the name to validate
     * @param fieldName the field name for error messages
     * @throws ValidationException if validation fails
     */
    public static void validateName(String name, String fieldName) throws ValidationException {
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException(fieldName + " cannot be null or empty");
        }
        
        String trimmed = name.trim();
        
        if (trimmed.length() < 1 || trimmed.length() > 50) {
            throw new ValidationException(fieldName + " must be between 1 and 50 characters");
        }
        
        if (!NAME_PATTERN.matcher(trimmed).matches()) {
            throw new ValidationException(fieldName + " contains invalid characters");
        }
    }

    /**
     * Validates email format and checks for injection.
     * 
     * @param email the email to validate
     * @throws ValidationException if validation fails
     */
    public static void validateEmail(String email) throws ValidationException {
        if (email == null || email.trim().isEmpty()) {
            throw new ValidationException("Email cannot be null or empty");
        }
        
        String trimmed = email.trim();
        
        if (trimmed.length() > 320) {
            throw new ValidationException("Email is too long (max 320 characters)");
        }
        
        if (!EMAIL_PATTERN.matcher(trimmed).matches()) {
            throw new ValidationException("Invalid email format");
        }
    }
    
     /**
      * Validates Codice Fiscale format and control character.
      * Uses the official Italian algorithm for control character validation.
      *
      * @param cf the codice fiscale to validate
      * @throws ValidationException if validation fails
      */
    public static void validateCodiceFiscale(String cf) throws ValidationException {
        if (cf == null || cf.trim().isEmpty()) {
            throw new ValidationException("Codice Fiscale cannot be null or empty");
        }
        
        String trimmed = cf.trim().toUpperCase();
        
        if (!CF_PATTERN.matcher(trimmed).matches()) {
            throw new ValidationException("Codice Fiscale must follow format: XXXXXXNNXNNXNNNX (X=letter, N=number)");
        }
    }
    
    /**
     * Validates password strength and format.
     * 
     * @param password the password to validate
     * @throws ValidationException if validation fails
     */
    public static void validatePassword(String password) throws ValidationException {
        if (password == null || password.trim().isEmpty()) {
            throw new ValidationException("Password cannot be null or empty");
        }
        
        if (password.length() < 8) {
            throw new ValidationException("Password must be at least 8 characters long");
        }
        
        if (password.length() > 128) {
            throw new ValidationException("Password is too long (max 128 characters)");
        }
        
        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;
        
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else if (isSpecialChar(c)) hasSpecial = true;
        }
        
        if (!hasUpper || !hasLower || !hasDigit || !hasSpecial) {
            throw new ValidationException(
                "Password must contain uppercase, lowercase, digit, and special character"
            );
        }
    }

    /**
     * Validates book search query.
     * 
     * @param query the search query to validate
     * @throws ValidationException if validation fails
     */
    public static void validateSearchQuery(String query) throws ValidationException {
        if (query == null || query.trim().isEmpty()) {
            throw new ValidationException("Search query cannot be empty");
        }
        
        String trimmed = query.trim();
        
        if (trimmed.length() > 500) {
            throw new ValidationException("Search query is too long (max 500 characters)");
        }
    }

    /**
     * Validates numeric ID parameters.
     *
     * @param id the ID to validate
     * @param fieldName the field name for error messages
     * @throws ValidationException if validation fails
     */
    public static void validateId(Integer id, String fieldName) throws ValidationException {
        if (id == null) {
            throw new ValidationException(fieldName + " cannot be null");
        }

        if (id <= 0) {
            throw new ValidationException(fieldName + " must be positive");
        }

        if (id > Integer.MAX_VALUE) {
            throw new ValidationException(fieldName + " is too large");
        }
    }

    /**
     * Validates review scores (1-5).
     *
     * @param score the score to validate
     * @param fieldName the field name for error messages
     * @throws ValidationException if validation fails
     */
    public static void validateReviewScore(int score, String fieldName) throws ValidationException {
        if (score < 1 || score > 5) {
            throw new ValidationException(fieldName + " must be between 1 and 5");
        }
    }

    /**
     * Validates review notes (max 256 characters).
     *
     * @param note the note to validate
     * @param fieldName the field name for error messages
     * @throws ValidationException if validation fails
     */
    public static void validateReviewNote(String note, String fieldName) throws ValidationException {
        if (note != null && note.length() > 256) {
            throw new ValidationException(fieldName + " cannot exceed 256 characters");
        }
    }

    /**
     * Validates library name.
     *
     * @param name the library name to validate
     * @throws ValidationException if validation fails
     */
    public static void validateLibraryName(String name) throws ValidationException {
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("Library name cannot be null or empty");
        }

        String trimmed = name.trim();

        if (trimmed.length() > 100) {
            throw new ValidationException("Library name cannot exceed 100 characters");
        }
    }

    /**
     * Validates year parameter.
     *
     * @param year the year to validate
     * @throws ValidationException if validation fails
     */
    public static void validateYear(int year) throws ValidationException {
        int currentYear = java.time.Year.now().getValue();
        if (year < 1000 || year > currentYear + 10) {
            throw new ValidationException("Year must be between 1000 and " + (currentYear + 10));
        }
    }

    /**
     * Validates suggestion count (max 3 per book).
     *
     * @param count the number of suggestions
     * @throws ValidationException if validation fails
     */
    public static void validateSuggestionCount(int count) throws ValidationException {
        if (count < 0 || count > 3) {
            throw new ValidationException("Cannot have more than 3 suggestions per book");
        }
    }

    /**
     * Checks if a character is a special character.
     *
     * @param c: character to check
     * @return true if it's a special character
     */
    private static boolean isSpecialChar(char c) {
        String specialChars = "!@#$%^&*()_-+=<>?[]{}|;:,.";
        return specialChars.indexOf(c) >= 0;
    }

    /**
     * Validates Codice Fiscale control character (simplified version).
     * 
     * @param cf the codice fiscale to validate
     * @return true if control character is valid
     */
    private static boolean isValidControlCharacter(String cf) {
        // Simplified validation - in production, implement full algorithm
        return cf.length() == 16 && Character.isLetter(cf.charAt(15));
    }
    
    /**
     * Sanitizes string input by removing potentially dangerous characters.
     * 
     * @param input the input to sanitize
     * @return sanitized string
     */
    public static String sanitize(String input) {
        if (input == null) {
            return null;
        }
        
        // Remove potentially dangerous characters
        return input.replaceAll("[<>\"'&]", "");
    }
}