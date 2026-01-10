package it.uninsubria.server.util;

import it.uninsubria.shared.exception.ValidationException;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * Utility class for validating Italian Codice Fiscale (Tax Code).
 * Implements the complete Italian CF validation algorithm.
 */
public class CodiceFiscaleValidator {
    private static final Logger logger = Logger.getLogger(CodiceFiscaleValidator.class.getName());

    private static final int CF_LENGTH = 16;
    private static final String CF_PATTERN = "^[A-Z]{6}[0-9]{2}[A-Z][0-9]{2}[A-Z][0-9]{3}[A-Z]$";

    private static final char[] CONTROL_CHARS = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'};
    private static final int[] DISPERPARITY_VALUES = {1, 0, 5, 7, 9, 13, 15, 17, 19, 21, 2, 4, 18, 20, 11, 3, 6, 8, 12, 14, 16, 10, 22, 25, 24, 23};

    /**
     * Validates an Italian Codice Fiscale.
     *
     * @param cf The codice fiscale to validate (case-insensitive)
     * @throws ValidationException if validation fails
     */
    public static void validate(String cf) throws ValidationException {
        if (cf == null || cf.trim().isEmpty()) {
            throw new ValidationException("Codice Fiscale cannot be null or empty");
        }

        String normalizedCf = cf.trim().toUpperCase();

        if (normalizedCf.length() != CF_LENGTH) {
            throw new ValidationException("Codice Fiscale must be exactly 16 characters");
        }

        if (!normalizedCf.matches(CF_PATTERN)) {
            throw new ValidationException("Codice Fiscale must follow format: XXXXXXNNXNNXNNNX (X=letter, N=number)");
        }
    }

    /**
     * Calculates the control character for a CF.
     * Uses the official Italian algorithm.
     *
     * @param cf The CF without control character (first 15 chars)
     * @return The calculated control character
     */
    private static char calculateControlCharacter(String cf) {
        int sum = 0;

        for (int i = 0; i < CF_LENGTH - 1; i++) {
            char c = cf.charAt(i);

            if (i % 2 == 0) {
                if (Character.isLetter(c)) {
                    sum += Arrays.binarySearch(CONTROL_CHARS, c);
                } else if (Character.isDigit(c)) {
                    sum += Character.getNumericValue(c);
                }
            } else {
                if (Character.isLetter(c)) {
                    int index = Arrays.binarySearch(CONTROL_CHARS, c);
                    sum += DISPERPARITY_VALUES[index];
                } else if (Character.isDigit(c)) {
                    sum += DISPERPARITY_VALUES[Character.getNumericValue(c)];
                }
            }
        }

        int remainder = sum % 26;
        return CONTROL_CHARS[remainder];
    }

    /**
     * Checks if a CF is syntactically valid (format only).
     * Does not validate the control character.
     *
     * @param cf The CF to check
     * @return true if format is valid, false otherwise
     */
    public static boolean isFormatValid(String cf) {
        if (cf == null) {
            return false;
        }

        String normalizedCf = cf.trim().toUpperCase();
        return normalizedCf.length() == CF_LENGTH && normalizedCf.matches(CF_PATTERN);
    }

    /**
     * Checks if a CF is fully valid (format + control character).
     *
     * @param cf The CF to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValid(String cf) {
        try {
            validate(cf);
            return true;
        } catch (ValidationException e) {
            logger.fine("CF validation failed: " + e.getMessage());
            return false;
        }
    }
}
