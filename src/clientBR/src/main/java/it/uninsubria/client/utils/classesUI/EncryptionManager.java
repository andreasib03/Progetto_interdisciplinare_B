package it.uninsubria.client.utils.classesUI;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;

/**
 * Gestore per la crittografia avanzata dei dati sensibili.
 * Utilizza AES-256 con modalità GCM e derivazione chiave PBKDF2.
 */
public class EncryptionManager {
    // Costanti per la crittografia
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final String SECRET_KEY_ALGORITHM = "AES";
    private static final String KEY_DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int KEY_LENGTH = 256;
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final int PBKDF2_ITERATIONS = 65536;
    private static final int SALT_LENGTH = 16;

    /**
     * Classe per contenere i dati crittografati
     */
    public static class EncryptedData {
        public String encryptedContent;
        public String iv;
        public String salt;

        public EncryptedData() {}

        public EncryptedData(String encryptedContent, String iv, String salt) {
            this.encryptedContent = encryptedContent;
            this.iv = iv;
            this.salt = salt;
        }
    }

    /**
     * Crittografa una stringa utilizzando una password
     * Genera un salt casuale per ogni operazione per massima sicurezza
     */
    public static EncryptedData encrypt(String plainText, String password) throws Exception {
        // Genera IV casuale
        byte[] iv = generateRandomBytes(GCM_IV_LENGTH);

        // Genera salt casuale per ogni operazione (nessun salt fisso)
        byte[] salt = generateRandomBytes(SALT_LENGTH);

        // Deriva chiave dalla password
        SecretKey key = deriveKey(password, salt);

        // Crittografa
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec);

        byte[] cipherText = cipher.doFinal(plainText.getBytes("UTF-8"));

        // Codifica in Base64 per storage
        return new EncryptedData(
            Base64.getEncoder().encodeToString(cipherText),
            Base64.getEncoder().encodeToString(iv),
            Base64.getEncoder().encodeToString(salt)
        );
    }

    /**
     * Decrittografa una stringa utilizzando una password
     */
    public static String decrypt(EncryptedData encryptedData, String password) throws Exception {
        // Decodifica i dati
        byte[] cipherText = Base64.getDecoder().decode(encryptedData.encryptedContent);
        byte[] iv = Base64.getDecoder().decode(encryptedData.iv);
        byte[] salt = Base64.getDecoder().decode(encryptedData.salt);

        // Deriva chiave dalla password
        SecretKey key = deriveKey(password, salt);

        // Decrittografa
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec);

        byte[] plainText = cipher.doFinal(cipherText);
        return new String(plainText, "UTF-8");
    }

    /**
     * Deriva una chiave AES-256 da una password utilizzando PBKDF2
     */
    private static SecretKey deriveKey(String password, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH);
        SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGORITHM);
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, SECRET_KEY_ALGORITHM);
    }

    /**
     * Genera bytes casuali sicuri
     */
    private static byte[] generateRandomBytes(int length) {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        return bytes;
    }

    /**
     * Verifica se una password è corretta per dati crittografati
     */
    public static boolean verifyPassword(EncryptedData encryptedData, String password) {
        try {
            decrypt(encryptedData, password);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Genera un hash sicuro per il confronto delle password (per future implementazioni)
     */
    public static String hashPassword(String password) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(password.getBytes());
        return Base64.getEncoder().encodeToString(hash);
    }

    /**
     * Valida la forza di una password
     */
    public static boolean isPasswordStrong(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }

        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;

        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else if (!Character.isWhitespace(c)) hasSpecial = true;
        }

        return hasUpper && hasLower && hasDigit && hasSpecial;
    }
}