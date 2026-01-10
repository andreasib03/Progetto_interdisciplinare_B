package it.uninsubria.client.utils.classesUI;

import it.uninsubria.client.utils.classesUI.EncryptionManager.EncryptedData;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test per la funzionalità di crittografia avanzata
 */
public class EncryptionManagerTest {

    @Test
    public void testEncryptDecrypt() throws Exception {
        String originalText = "Test di crittografia avanzata per backup sensibili";
        String password = "PasswordSicura123!";

        // Crittografa
        EncryptedData encrypted = EncryptionManager.encrypt(originalText, password);

        // Verifica che i dati crittografati siano presenti
        assertNotNull(encrypted.encryptedContent);
        assertNotNull(encrypted.iv);
        assertNotNull(encrypted.salt);
        assertFalse(encrypted.encryptedContent.isEmpty());
        assertFalse(encrypted.iv.isEmpty());
        assertFalse(encrypted.salt.isEmpty());

        // Decrittografa
        String decryptedText = EncryptionManager.decrypt(encrypted, password);

        // Verifica che il testo decrittografato corrisponda all'originale
        assertEquals(originalText, decryptedText);
    }

    @Test
    public void testWrongPassword() throws Exception {
        String originalText = "Test con password errata";
        String correctPassword = "PasswordCorretta123!";
        String wrongPassword = "PasswordErrata456!";

        // Crittografa con password corretta
        EncryptedData encrypted = EncryptionManager.encrypt(originalText, correctPassword);

        // Prova a decrittografare con password errata
        boolean exceptionThrown = false;
        try {
            EncryptionManager.decrypt(encrypted, wrongPassword);
        } catch (Exception e) {
            exceptionThrown = true;
        }

        assertTrue("Dovrebbe lanciare un'eccezione con password errata", exceptionThrown);
    }

    @Test
    public void testPasswordVerification() throws Exception {
        String password = "VerificaPassword789!";
        String wrongPassword = "PasswordErrata101!";

        // Crittografa
        EncryptedData encrypted = EncryptionManager.encrypt("test", password);

        // Verifica password corretta
        assertTrue(EncryptionManager.verifyPassword(encrypted, password));

        // Verifica password errata
        assertFalse(EncryptionManager.verifyPassword(encrypted, wrongPassword));
    }

    @Test
    public void testPasswordStrength() {
        // Password forti (almeno 8 caratteri con maiuscole, minuscole, numeri e simboli)
        assertTrue(EncryptionManager.isPasswordStrong("Password123!"));
        assertTrue(EncryptionManager.isPasswordStrong("Abc123!@#"));
        assertTrue(EncryptionManager.isPasswordStrong("MySecurePass2024!"));

        // Password deboli
        assertFalse(EncryptionManager.isPasswordStrong("")); // Vuota
        assertFalse(EncryptionManager.isPasswordStrong("123")); // Troppo corta, solo numeri
        assertFalse(EncryptionManager.isPasswordStrong("password")); // Solo lettere minuscole
        assertFalse(EncryptionManager.isPasswordStrong("PASSWORD")); // Solo lettere maiuscole
        assertFalse(EncryptionManager.isPasswordStrong("Password")); // Solo lettere
        assertFalse(EncryptionManager.isPasswordStrong("Password123")); // Senza simboli speciali
        assertTrue(EncryptionManager.isPasswordStrong("Pass123!")); // Password valida
        assertFalse(EncryptionManager.isPasswordStrong("pass123!")); // Senza maiuscole
        assertFalse(EncryptionManager.isPasswordStrong("PASS123!")); // Senza minuscole
        assertFalse(EncryptionManager.isPasswordStrong("Pass!")); // Senza numeri
    }

    @Test
    public void testEmptyData() throws Exception {
        String emptyText = "";
        String password = "EmptyTest456!";

        // Crittografa testo vuoto
        EncryptedData encrypted = EncryptionManager.encrypt(emptyText, password);
        String decrypted = EncryptionManager.decrypt(encrypted, password);

        assertEquals(emptyText, decrypted);
    }

    @Test
    public void testLargeData() throws Exception {
        // Crea una stringa grande per testare prestazioni
        StringBuilder largeText = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            largeText.append("Questa è una riga di testo per testare la crittografia di dati grandi. ");
        }
        String originalText = largeText.toString();
        String password = "LargeDataTest789!";

        // Crittografa
        EncryptedData encrypted = EncryptionManager.encrypt(originalText, password);

        // Decrittografa
        String decrypted = EncryptionManager.decrypt(encrypted, password);

        assertEquals(originalText, decrypted);
    }

    @Test
    public void testSpecialCharacters() throws Exception {
        String specialText = "Testo con caratteri speciali: àèìòù @#$%^&*()_+{}|:<>?[]\\;',./";
        String password = "SpecialChars123!";

        EncryptedData encrypted = EncryptionManager.encrypt(specialText, password);
        String decrypted = EncryptionManager.decrypt(encrypted, password);

        assertEquals(specialText, decrypted);
    }

    @Test
    public void testUnicodeCharacters() throws Exception {
        String unicodeText = "Testo Unicode: 🚀📱💻🎵🎬🌟🔥❤️👍🏆";
        String password = "UnicodeTest456!";

        EncryptedData encrypted = EncryptionManager.encrypt(unicodeText, password);
        String decrypted = EncryptionManager.decrypt(encrypted, password);

        assertEquals(unicodeText, decrypted);
    }

    @Test
    public void testWeakPasswordEncryption() throws Exception {
        String text = "Test con password debole";
        String weakPassword = "weak"; // Password debole

        // La crittografia non valida la password, solo l'UI
        EncryptedData encrypted = EncryptionManager.encrypt(text, weakPassword);
        assertNotNull(encrypted);

        // Ma la password debole può essere usata per crittografare
        String decrypted = EncryptionManager.decrypt(encrypted, weakPassword);
        assertEquals(text, decrypted);
    }

    @Test
    public void testNullInputs() {
        try {
            EncryptionManager.encrypt(null, "password");
            fail("Dovrebbe lanciare un'eccezione per testo nullo");
        } catch (Exception e) {
            // Eccezione attesa
        }

        try {
            EncryptionManager.encrypt("text", null);
            fail("Dovrebbe lanciare un'eccezione per password nulla");
        } catch (Exception e) {
            // Eccezione attesa
        }
    }
}