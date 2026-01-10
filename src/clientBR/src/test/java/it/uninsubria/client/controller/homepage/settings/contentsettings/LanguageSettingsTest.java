package it.uninsubria.client.controller.homepage.settings.contentsettings;

import it.uninsubria.client.utils.classesUI.LanguageManager;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test per verificare che le impostazioni lingua funzionino correttamente
 */
public class LanguageSettingsTest {

    @Test
    public void testLanguageManagerBasics() {
        // Test that LanguageManager works correctly
        String originalLanguage = LanguageManager.getLanguage();
        assertNotNull("Language should not be null", originalLanguage);

        // Test that ResourceBundle is available
        var bundle = LanguageManager.getBundle();
        assertNotNull("ResourceBundle should not be null", bundle);

        // Test that language change works (though it won't persist)
        LanguageManager.setLanguage("en");
        assertEquals("Language should be changed to English", "en", LanguageManager.getLanguage());

        LanguageManager.setLanguage("it");
        assertEquals("Language should be changed back to Italian", "it", LanguageManager.getLanguage());

        // Restore original language
        LanguageManager.setLanguage(originalLanguage);
    }
}