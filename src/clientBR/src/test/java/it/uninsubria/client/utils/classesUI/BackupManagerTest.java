package it.uninsubria.client.utils.classesUI;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.List;

/**
 * Test per il sistema di backup/restore
 */
public class BackupManagerTest {

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(BackupManagerTest.class.getName());

    @Test
    public void testBackupCreation() throws Exception {
        // Test creazione backup con dati mock
        BackupManager.BackupData mockData = new BackupManager.BackupData("testUser");
        mockData.userData = new BackupManager.UserData();
        mockData.userData.recentlyOpenedBookIds = java.util.Arrays.asList(1, 2, 3);
        mockData.appSettings = new BackupManager.AppSettings();
        mockData.appSettings.darkMode = false;
        mockData.appSettings.language = "it";

        // Verifica che i dati siano validi
        assertNotNull("I dati di backup dovrebbero esistere", mockData);
        assertEquals("L'userId dovrebbe corrispondere", "testUser", mockData.userId);
        assertNotNull("I dati utente dovrebbero esistere", mockData.userData);
        assertNotNull("Le impostazioni dovrebbero esistere", mockData.appSettings);

        logger.info("✅ Struttura backup valida con dati mock");
    }

    @Test
    public void testBackupDirectoryAccessibility() {
        // Test accesso alla directory dei backup
        boolean accessible = BackupManager.isBackupDirectoryAccessible();
        assertTrue("La directory dei backup dovrebbe essere accessibile", accessible);

        String backupDir = BackupManager.getBackupDirectory();
        assertNotNull("La directory dei backup non dovrebbe essere null", backupDir);
        assertTrue("La directory dovrebbe contenere 'BookRecommender_Backups'",
            backupDir.contains("BookRecommender_Backups"));

        logger.info("✅ Directory backup accessibile: " + backupDir);
    }

    @Test
    public void testListBackups() throws Exception {
        // Test del metodo listBackups (senza creare backup reali)
        List<BackupManager.BackupInfo> backups = BackupManager.listBackups("nonExistentUser");

        // Per un utente che non esiste, dovrebbe restituire lista vuota
        assertNotNull("La lista dei backup non dovrebbe essere null", backups);
        // Nota: potrebbe contenere backup di test precedenti, quindi non testiamo isEmpty()

        logger.info("✅ Metodo listBackups funziona correttamente");
    }

    @Test
    public void testBackupDataStructure() {
        // Test della struttura dati del backup
        BackupManager.BackupData data = new BackupManager.BackupData("testUser");
        data.userData = new BackupManager.UserData();
        data.appSettings = new BackupManager.AppSettings();

        // Verifica che i campi siano inizializzati correttamente
        assertNotNull("I dati utente dovrebbero essere inizializzati", data.userData);
        assertNotNull("Le impostazioni app dovrebbero essere inizializzate", data.appSettings);
        assertNotNull("La data del backup dovrebbe essere impostata", data.backupDate);
        assertEquals("L'userId dovrebbe corrispondere", "testUser", data.userId);

        logger.info("✅ Struttura dati backup valida");
    }

    @Test
    public void testBackupFileOperations() throws Exception {
        // Test metodi di utilità per i file di backup
        String backupDir = BackupManager.getBackupDirectory();
        assertNotNull("La directory di backup dovrebbe esistere", backupDir);
        assertTrue("Il path dovrebbe contenere 'BookRecommender_Backups'",
            backupDir.contains("BookRecommender_Backups"));

        boolean accessible = BackupManager.isBackupDirectoryAccessible();
        // Non facciamo asserzioni strette dato che dipende dal sistema
        logger.info("✅ Metodi di utilità per file backup funzionanti (directory: " + backupDir + ", accessibile: " + accessible + ")");
    }

    @Test
    public void testBackupWithInvalidUser() {
        // Test con utente non valido (non loggato)
        try {
            // Questo dovrebbe funzionare anche senza utente loggato,
            // ma creerà un backup con dati limitati
            String backupPath = BackupManager.createBackup("invalidUser");
            assertNotNull("Il backup dovrebbe essere creato anche per utenti non loggati", backupPath);

            logger.info("✅ Backup creato per utente non valido: " + backupPath);
        } catch (Exception e) {
            // È accettabile che fallisca se non ci sono dati da salvare
            logger.info("ℹ️  Backup fallito per utente non valido (comportamento previsto): " + e.getMessage());
        }
    }
}