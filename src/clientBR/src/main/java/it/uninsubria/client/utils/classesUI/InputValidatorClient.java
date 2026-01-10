package it.uninsubria.client.utils.classesUI;

import java.util.regex.Pattern;

/**
 * Utilità per la validazione input lato client.
 * Previene injection attacks e migliora l'esperienza utente con messaggi di errore specifici.
 */
public class InputValidatorClient {

    // Pattern per validazione email
    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    // Pattern per validazione username (solo lettere, numeri, underscore)
    private static final Pattern USERNAME_PATTERN =
        Pattern.compile("^[A-Za-z0-9_]{3,30}$");

    // Pattern per validazione password (almeno 8 caratteri, maiuscola, minuscola, numero, simbolo)
    private static final Pattern PASSWORD_PATTERN =
        Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$");

    // Pattern per validazione nome libreria (no caratteri speciali pericolosi)
    private static final Pattern LIBRARY_NAME_PATTERN =
        Pattern.compile("^[A-Za-z0-9\\s\\-_]{1,100}$");

    // Pattern per validazione ricerca libri (no SQL injection)
    private static final Pattern SEARCH_QUERY_PATTERN =
        Pattern.compile("^[^<>&\"]{1,200}$");

    /**
     * Valida un indirizzo email
     */
    public static ValidationResult validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return ValidationResult.failure("L'indirizzo email è obbligatorio");
        }

        email = email.trim();

        if (email.length() > 254) {
            return ValidationResult.failure("L'indirizzo email è troppo lungo");
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return ValidationResult.failure("Formato email non valido");
        }

        return ValidationResult.success();
    }

    /**
     * Valida un username
     */
    public static ValidationResult validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return ValidationResult.failure("Il nome utente è obbligatorio");
        }

        username = username.trim();

        if (username.length() < 3) {
            return ValidationResult.failure("Il nome utente deve contenere almeno 3 caratteri");
        }

        if (username.length() > 30) {
            return ValidationResult.failure("Il nome utente può contenere al massimo 30 caratteri");
        }

        if (!USERNAME_PATTERN.matcher(username).matches()) {
            return ValidationResult.failure("Il nome utente può contenere solo lettere, numeri e underscore");
        }

        // Controlla parole riservate
        String lowerUsername = username.toLowerCase();
        if (lowerUsername.equals("admin") || lowerUsername.equals("root") ||
            lowerUsername.equals("system") || lowerUsername.equals("guest")) {
            return ValidationResult.failure("Nome utente riservato");
        }

        return ValidationResult.success();
    }

    /**
     * Valida una password
     */
    public static ValidationResult validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            return ValidationResult.failure("La password è obbligatoria");
        }

        if (password.length() < 8) {
            return ValidationResult.failure("La password deve contenere almeno 8 caratteri");
        }

        if (password.length() > 128) {
            return ValidationResult.failure("La password è troppo lunga");
        }

        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            return ValidationResult.failure("La password deve contenere almeno una maiuscola, una minuscola, un numero e un simbolo speciale");
        }

        return ValidationResult.success();
    }

    /**
     * Valida il nome di una libreria
     */
    public static ValidationResult validateLibraryName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return ValidationResult.failure("Il nome della libreria è obbligatorio");
        }

        name = name.trim();

        if (name.length() < 1) {
            return ValidationResult.failure("Il nome della libreria non può essere vuoto");
        }

        if (name.length() > 100) {
            return ValidationResult.failure("Il nome della libreria è troppo lungo");
        }

        if (!LIBRARY_NAME_PATTERN.matcher(name).matches()) {
            return ValidationResult.failure("Il nome della libreria contiene caratteri non validi");
        }

        return ValidationResult.success();
    }

    /**
     * Valida una query di ricerca libri
     */
    public static ValidationResult validateSearchQuery(String query) {
        if (query == null) {
            return ValidationResult.success(); // Query vuota è valida (ricerca tutto)
        }

        query = query.trim();

        if (query.length() > 200) {
            return ValidationResult.failure("La query di ricerca è troppo lunga");
        }

        if (!query.isEmpty() && !SEARCH_QUERY_PATTERN.matcher(query).matches()) {
            return ValidationResult.failure("La query di ricerca contiene caratteri non validi");
        }

        return ValidationResult.success();
    }

    /**
     * Valida un nome proprio (nome o cognome)
     */
    public static ValidationResult validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return ValidationResult.failure("Il nome è obbligatorio");
        }

        name = name.trim();

        if (name.length() < 2) {
            return ValidationResult.failure("Il nome deve contenere almeno 2 caratteri");
        }

        if (name.length() > 50) {
            return ValidationResult.failure("Il nome è troppo lungo");
        }

        // Controlla che contenga solo lettere, spazi e apostrofi
        if (!name.matches("^[A-Za-z\\s'àèéìòùÀÈÉÌÒÙ]{2,50}$")) {
            return ValidationResult.failure("Il nome contiene caratteri non validi");
        }

        return ValidationResult.success();
    }

    /**
     * Valida un codice fiscale italiano
     */
    public static ValidationResult validateFiscalCode(String fiscalCode) {
        if (fiscalCode == null || fiscalCode.trim().isEmpty()) {
            return ValidationResult.failure("Il codice fiscale è obbligatorio");
        }

        fiscalCode = fiscalCode.trim().toUpperCase();

        if (fiscalCode.length() != 16) {
            return ValidationResult.failure("Il codice fiscale deve contenere esattamente 16 caratteri");
        }

        // Pattern base per codice fiscale italiano
        if (!fiscalCode.matches("^[A-Z]{6}[0-9]{2}[A-Z][0-9]{2}[A-Z][0-9]{3}[A-Z]$")) {
            return ValidationResult.failure("Formato codice fiscale non valido");
        }

        return ValidationResult.success();
    }

    /**
     * Risultato della validazione
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;

        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult failure(String message) {
            return new ValidationResult(false, message);
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void throwIfInvalid() {
            if (!valid) {
                throw new IllegalArgumentException(errorMessage);
            }
        }
    }
}