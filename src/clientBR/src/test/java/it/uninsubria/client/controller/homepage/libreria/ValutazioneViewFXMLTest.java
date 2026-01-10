package it.uninsubria.client.controller.homepage.libreria;

import it.uninsubria.client.utils.classesUI.LanguageManager;
import javafx.fxml.FXMLLoader;
import org.junit.Test;

import java.net.URL;

import static org.junit.Assert.*;

/**
 * Test per verificare che l'FXML delle recensioni possa essere caricato correttamente
 */
public class ValutazioneViewFXMLTest {

    @Test
    public void testFXMLLoading() throws Exception {
        // Test that FXML can be loaded with ResourceBundle
        URL fxmlUrl = getClass().getResource("/fxml/views/library/ValutazioneView.fxml");
        assertNotNull("FXML file should exist", fxmlUrl);

        // Load resource bundle
        var bundle = LanguageManager.getBundle();
        assertNotNull("Resource bundle should be available", bundle);

        // Test loading FXML with resources
        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        loader.setResources(bundle);

        // This should not throw an exception
        var root = loader.load();
        assertNotNull("FXML root should not be null", root);

        // Check that controller is created
        var controller = loader.getController();
        assertNotNull("Controller should be created", controller);
        assertTrue("Controller should be ValutazioneController",
                  controller instanceof ValutazioneController);
    }
}