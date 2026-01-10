package it.uninsubria.client.cache;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test per ClientCacheManager
 */
public class ClientCacheManagerTest {

    @Before
    public void setUp() {
        ClientCacheManager.clear();
    }

    @After
    public void tearDown() {
        ClientCacheManager.clear();
    }

    @Test
    public void testPutAndGet() {
        String key = "testKey";
        String value = "testValue";

        ClientCacheManager.put(key, value);
        String retrieved = ClientCacheManager.get(key);

        assertEquals("Il valore recuperato dovrebbe essere uguale a quello memorizzato", value, retrieved);
    }

    @Test
    public void testGetNonExistentKey() {
        String retrieved = ClientCacheManager.get("nonExistentKey");
        assertNull("Dovrebbe restituire null per una chiave inesistente", retrieved);
    }

    @Test
    public void testInvalidate() {
        String key = "testKey";
        String value = "testValue";

        ClientCacheManager.put(key, value);
        assertNotNull("Il valore dovrebbe esistere prima dell'invalidazione", ClientCacheManager.get(key));

        ClientCacheManager.invalidate(key);
        assertNull("Il valore dovrebbe essere null dopo l'invalidazione", ClientCacheManager.get(key));
    }
}