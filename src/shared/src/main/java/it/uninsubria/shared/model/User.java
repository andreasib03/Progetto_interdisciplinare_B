package it.uninsubria.shared.model;

import java.io.Serializable;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a user in the Book Recommender system.
 * <p>
 * This class contains all user-related information including personal details,
 * authentication credentials, and profile data. Users can create libraries,
 * write reviews, and receive book recommendations based on their preferences.
 * </p>
 * <p>
 * All passwords are stored securely using BCrypt hashing.
 * Profile images are stored as byte arrays with associated MIME types.
 * </p>
 *
 * @author Book Recommender Team
 * @version 1.0
 * @see Library
 * @see Review
 * @since 1.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)

public class User implements Serializable{
    /**
     * Database primary key for the user.
     * Automatically generated as a serial number.
     */
    private Integer user_id;

    /**
     * User's email address.
     * Must be unique across all users.
     */
    private String email;

    /**
     * Italian tax code (Codice Fiscale).
     * Must be exactly 16 uppercase alphanumeric characters.
     */
    private String CF;

    /**
     * User's first name.
     * Maximum 30 characters, no numbers allowed.
     */
    private String name;

    /**
     * User's last name.
     * Maximum 30 characters, no numbers allowed.
     */
    private String surname;

    /**
     * Hashed password using BCrypt.
     * Never stored in plain text.
     */
    private String password;

    /**
     * Unique username for login.
     * Minimum 5 characters, must contain at least one letter.
     */
    private String id;

    /**
     * Hashed recovery phrase for password reset functionality.
     */
    private String recoveryPhrase;

    /**
     * List of personal libraries owned by this user.
     */
    private List<Library> library;

    /**
     * Profile image stored as byte array.
     * Supports various image formats.
     */
    private byte[] profileImage;

    /**
     * MIME type of the profile image.
     * Examples: "image/png", "image/jpeg", "image/gif"
     */
    private String profileImageType;




    /* Check idoneità campi */

    /**
     * Validates a username according to system requirements.
     * <p>
     * Requirements:
     * <ul>
     *   <li>Not null</li>
     *   <li>Minimum 5 characters</li>
     *   <li>Contains at least one letter</li>
     * </ul>
     *
     * @param id the username to validate
     * @return true if the username meets all requirements, false otherwise
     */
    public static boolean checkID(String id){
        if (id == null || id.length() < 5){
            return false;
        }

        for(char c : id.toCharArray()){
            if(Character.isLetter(c))
                return true;

        }
        return false;
    }

    @Deprecated
    public String getID() {
        return id;
    }

    @Deprecated
    public void setID(String ID) {
        this.id = ID;
    }

    public static boolean checkNome(String nome) {
        if (nome == null || nome.length() > 30) {
            return true;
        }
        for (char c : nome.toCharArray()) {
            if (Character.isDigit(c))
                return true;
            
        }
        return false;
    }

    public static boolean checkCognome(String cognome) {
        if (cognome == null || cognome.length() > 30) {
            return true;
        }
        for (char c : cognome.toCharArray()) {
            if (Character.isDigit(c)) 
                return true;
        }
        return false;
    }

    public static boolean checkEmail(String email) {
        if (email == null || email.isEmpty() || email.length() > 320)
            return false;


        // Espressione regolare per la validazione dell'email
        String emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";

        return email.matches(emailRegex);
    }

    public static boolean checkCF(String codiceFiscale) {
        if (codiceFiscale == null || codiceFiscale.length() != 16) 
            return false;

        return codiceFiscale.matches("[A-Z]{6}[0-9]{2}[A-Z][0-9]{2}[A-Z][0-9]{3}[A-Z]$");
    }

    private static boolean isValidControlCharacter(String codiceFiscale) {
        int[] valoriDispari = {1, 0, 5, 7, 9, 13, 15, 17, 19, 21, 2, 4, 18, 20, 11, 3, 6, 8, 12, 14, 16, 10, 22, 25, 24, 23};
        int[] valoriPari = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};

        int somma = 0;

        for (int i = 0; i < 15; i++) {
            char c = codiceFiscale.charAt(i);
            int valore;

            if (Character.isDigit(c)) {
                valore = c - '0';
            } else if (Character.isUpperCase(c)) {
                valore = c - 'A';
            } else {
                return false;
            }

            somma += (i % 2 == 0) ? valoriDispari[valore] : valoriPari[valore];
        }

        int controllo = somma % 26;
        char carattereControlloCalcolato = (char) ('A' + controllo);

        return codiceFiscale.charAt(15) == carattereControlloCalcolato;
    }

    public static boolean checkPassword(String password) {
        if (password == null || password.length() < 8) 
            return false;
        

        boolean maiuscole = false;
        boolean minuscole = false;
        boolean numeri = false;
        boolean carattereSpeciali = false;

        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) {
                maiuscole = true;
            } else if (Character.isLowerCase(c)) {
                minuscole = true;
            } else if (Character.isDigit(c)) {
                numeri = true;
            } else if (e_CarattereSpeciale(c)) {
                carattereSpeciali = true;
            } else if (!Character.isLetterOrDigit(c)) {
                return false;
            }
        }

        return maiuscole && minuscole && numeri && carattereSpeciali;
    }

    private static boolean e_CarattereSpeciale(char c) {
        String carattereSpeciale = "!@#$%^&*()_-+=<>?[]{}|;:,.";
        return carattereSpeciale.indexOf(c) >= 0;
    }
    /**
     * Rappresentazione stringa dell'utente (esclude dati sensibili)
     * Password e recovery phrase NON sono incluse per motivi di sicurezza
     */
     @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append(email).append(" - ").append(CF).append(" - ").append(name).append(" - ").append(surname).append(" - ").append(id);
        return sb.toString();
    }
}
