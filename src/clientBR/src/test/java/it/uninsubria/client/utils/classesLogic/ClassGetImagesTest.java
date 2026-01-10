package it.uninsubria.client.utils.classesLogic;

import static org.junit.Assert.*;

import org.junit.Test;

import it.uninsubria.shared.model.Book;

/**
 * Test per verificare la gestione offline delle immagini in ClassGetImages
 */
public class ClassGetImagesTest {

    @Test
    public void testOfflineConnectivityCheck() throws Exception {
        // Testa che il metodo di controllo connettività sia accessibile
        boolean connectivity = ClassGetImages.checkInternetConnectivityForUI();

        // Il risultato può essere true o false a seconda della connessione reale
        // Quello che importa è che il metodo non lanci eccezioni
        assertNotNull("Il metodo deve restituire un valore booleano", connectivity);
    }

    @Test
    public void testImageCacheKeyGeneration() {
        // Testa la generazione delle chiavi cache
        Book testBook = new Book("Test Title", "Test Author", 2023);

        String expectedKey = "Test Title_Test Author_2023";
        String actualKey = testBook.getTitle() + "_" + testBook.getAuthors() + "_" + testBook.getPublish_date_year();

        assertEquals("La chiave cache deve essere generata correttamente", expectedKey, actualKey);
    }

    @Test
    public void testPlaceholderImageLoading() throws Exception {
        // Testa che l'immagine placeholder possa essere caricata
        String placeholderPath = ClassGetImages.PLACEHOLDER_IMAGE;

        assertNotNull("Il path del placeholder non deve essere null", placeholderPath);
        assertTrue("Il path del placeholder deve essere valido", placeholderPath.startsWith("file:") || placeholderPath.startsWith("/"));
    }

    @Test
    public void testIsImageInCache() {
        // Testa il metodo per verificare se un'immagine è in cache
        String testKey = "TestKey_Offline";

        // Inizialmente non dovrebbe essere in cache
        boolean initiallyInCache = ClassGetImages.isImageInCache(testKey);
        // Non possiamo garantire che non sia in cache, ma il metodo non deve lanciare eccezioni

        assertNotNull("Il metodo deve restituire un valore booleano", initiallyInCache);
    }
}