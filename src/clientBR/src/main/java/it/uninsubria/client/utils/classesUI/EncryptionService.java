package it.uninsubria.client.utils.classesUI;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.prefs.Preferences;
import java.util.logging.Logger;

public class EncryptionService {
    private static final Logger logger = Logger.getLogger(EncryptionService.class.getName());

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";

    private final String encryptionKey;

    public EncryptionService() {
        this.encryptionKey = initializeEncryptionKey();
    }

    private String initializeEncryptionKey() {
        try {
            Preferences prefs = Preferences.userRoot();
            String storedKey = prefs.get("encryptionKey", null);

            if (storedKey != null && !storedKey.trim().isEmpty()) {
                return storedKey;
            } else {
                byte[] keyBytes = new byte[32];
                new java.security.SecureRandom().nextBytes(keyBytes);
                String newKey = java.util.Base64.getEncoder().encodeToString(keyBytes);

                prefs.put("encryptionKey", newKey);
                prefs.flush();

                logger.info("Generated and saved new encryption key");
                return newKey;
            }
        } catch (Exception e) {
            logger.warning("Failed to initialize encryption key: " + e.getMessage());
            byte[] keyBytes = new byte[32];
            new java.security.SecureRandom().nextBytes(keyBytes);
            return java.util.Base64.getEncoder().encodeToString(keyBytes);
        }
    }

    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return plaintext;
        }

        try {
            SecretKeySpec secretKey = generateKey();
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedBytes = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            logger.warning("Failed to encrypt: " + e.getMessage());
            return null;
        }
    }

    public String decrypt(String encrypted) {
        if (encrypted == null || encrypted.isEmpty()) {
            return encrypted;
        }

        try {
            SecretKeySpec secretKey = generateKey();
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encrypted));
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.warning("Failed to decrypt: " + e.getMessage());
            return null;
        }
    }

    private SecretKeySpec generateKey() {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = sha.digest(encryptionKey.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(keyBytes, ALGORITHM);
        } catch (Exception e) {
            logger.warning("Failed to generate encryption key: " + e.getMessage());
            throw new RuntimeException("Failed to generate encryption key", e);
        }
    }
}
