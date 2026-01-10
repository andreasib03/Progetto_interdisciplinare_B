package it.uninsubria.client.controller;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test per verificare che l'internalizzazione funzioni correttamente
 */
public class InternationalizationTest {

    @Test
    public void testCacheManagerOperations() {
        // Test che le operazioni di cache funzionino senza lanciare eccezioni
        try {
            it.uninsubria.client.cache.ClientCacheManager.put("testKey", "testValue");
            String value = it.uninsubria.client.cache.ClientCacheManager.get("testKey");
            assertEquals("Il valore dovrebbe essere recuperato correttamente", "testValue", value);

            it.uninsubria.client.cache.ClientCacheManager.invalidate("testKey");
            String nullValue = it.uninsubria.client.cache.ClientCacheManager.get("testKey");
            assertNull("Il valore dovrebbe essere null dopo l'invalidazione", nullValue);

        } catch (Exception e) {
            fail("Le operazioni di cache non dovrebbero lanciare eccezioni: " + e.getMessage());
        }
    }
}