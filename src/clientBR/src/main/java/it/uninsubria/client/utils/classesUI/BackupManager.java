package it.uninsubria.client.utils.classesUI;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import it.uninsubria.shared.model.Book;

import java.awt.Desktop;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import it.uninsubria.shared.utils.LoggerUtil;
import it.uninsubria.client.utils.classesUI.EncryptionManager.EncryptedData;

/**
 * Gestore per backup e restore dei dati utente.
 * Supporta backup completo e selettivo dei dati dell'applicazione.
 */
public class BackupManager {
    private static final Logger logger = LoggerUtil.getLogger(BackupManager.class);

    // Directory di backup (multiplatform)
    private static final String BACKUP_DIR = System.getProperty("user.home") + File.separator + "BookRecommender_Backups";
    private static final String BACKUP_EXTENSION = ".bkp.zip";

    private static final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .enable(SerializationFeature.INDENT_OUTPUT);

    /**
     * Dati di backup dell'utente
     */
    public static class BackupData {
        public String userId;
        public LocalDateTime backupDate;
        public String version = "2.0"; // Aggiornato per supporto crittografia
        public UserData userData;
        public AppSettings appSettings;
        public boolean encrypted = false;
        public EncryptedData encryptedUserData; // Dati utente crittografati

        public BackupData() {
            this.backupDate = LocalDateTime.now();
        }

        public BackupData(String userId) {
            this();
            this.userId = userId;
        }
    }

    /**
     * Dati utente da salvare nel backup
     */
    public static class UserData {
        public List<LibraryData> libraries = new ArrayList<>();
        public List<Integer> recentlyOpenedBookIds = new ArrayList<>();
        public Map<String, Object> preferences = new HashMap<>();
    }

    /**
     * Dati di una libreria
     */
    public static class LibraryData {
        public String name;
        public String description;
        public boolean isPublic;
        public List<Integer> bookIds = new ArrayList<>();
        public LocalDateTime createdDate;
    }

    /**
     * Impostazioni dell'applicazione
     */
    public static class AppSettings {
        public boolean darkMode;
        public String language;
        public int timeoutMinutes;
        public Map<String, Object> otherSettings = new HashMap<>();
    }

    /**
     * Crea un backup completo dei dati utente
     */
    public static String createBackup(String userId) throws Exception {
        logger.info("Avvio creazione backup per utente: " + userId);

        // Crea directory backup se non esiste
        Path backupDir = Paths.get(BACKUP_DIR);
        Files.createDirectories(backupDir);

        // Genera nome file backup
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String backupFileName = userId + "_" + timestamp + BACKUP_EXTENSION;
        Path backupFile = backupDir.resolve(backupFileName);

        // Raccogli dati da salvare
        BackupData backupData = collectBackupData(userId);

        // Verifica che ci siano dati da salvare
        if (backupData.userData == null) {
            backupData.userData = new UserData();
        }
        if (backupData.appSettings == null) {
            backupData.appSettings = new AppSettings();
        }

        // Salva in file ZIP
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(backupFile))) {
            // Salva dati JSON (scrivi prima in memoria per evitare problemi di stream)
            try (ByteArrayOutputStream jsonBytes = new ByteArrayOutputStream()) {
                objectMapper.writeValue(jsonBytes, backupData);
                byte[] jsonData = jsonBytes.toByteArray();

                ZipEntry jsonEntry = new ZipEntry("backup_data.json");
                zos.putNextEntry(jsonEntry);
                zos.write(jsonData);
                zos.closeEntry();
            }

            // Salva eventuali file aggiuntivi (immagini, ecc.)
            addAdditionalFiles(zos, userId);
        }

        logger.info("Backup creato con successo: " + backupFile.toString());
        return backupFile.toString();
    }

    /**
     * Crea un backup crittografato dei dati utente
     */
    public static String createEncryptedBackup(String userId, String password) throws Exception {
        logger.info("Avvio creazione backup crittografato per utente: " + userId);

        // Verifica forza password
        if (!EncryptionManager.isPasswordStrong(password)) {
            throw new IllegalArgumentException("La password deve contenere almeno 8 caratteri con lettere maiuscole, minuscole, numeri e simboli");
        }

        // Crea directory backup se non esiste
        Path backupDir = Paths.get(BACKUP_DIR);
        Files.createDirectories(backupDir);

        // Genera nome file backup
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String backupFileName = userId + "_encrypted_" + timestamp + BACKUP_EXTENSION;
        Path backupFile = backupDir.resolve(backupFileName);

        // Raccogli dati da salvare
        BackupData backupData = collectBackupData(userId);
        backupData.encrypted = true;

        // Crittografa i dati sensibili
        String userDataJson = objectMapper.writeValueAsString(backupData.userData);
        backupData.encryptedUserData = EncryptionManager.encrypt(userDataJson, password);
        backupData.userData = null; // Rimuovi dati non crittografati

        // Salva in file ZIP
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(backupFile))) {
            // Salva dati JSON (con dati crittografati)
            ZipEntry jsonEntry = new ZipEntry("backup_data.json");
            zos.putNextEntry(jsonEntry);
            objectMapper.writeValue(zos, backupData);
            zos.closeEntry();

            // Salva eventuali file aggiuntivi (immagini, ecc.)
            addAdditionalFiles(zos, userId);
        }

        logger.info("Backup crittografato creato con successo: " + backupFile.toString());
        return backupFile.toString();
    }

    /**
     * Raccoglie tutti i dati da salvare nel backup
     */
    private static BackupData collectBackupData(String userId) {
        BackupData data = new BackupData(userId);

        try {
            // Dati utente
            data.userData = new UserData();
            // Salva gli ID dei libri recentemente aperti
            data.userData.recentlyOpenedBookIds = SessionManager.getInstance().getRecentlyOpenedBooks().stream()
                .map(Book::getBook_id)
                .toList();
        } catch (RuntimeException e) {
            logger.warning("Errore nell'accesso ai dati di sessione utente: " + e.getMessage());
            // Crea dati vuoti ma validi
            data.userData = new UserData();
            data.userData.recentlyOpenedBookIds = new ArrayList<>();
        }

        // Librerie utente (se disponibili)
        try {
            // Nota: Questa parte richiederebbe l'accesso ai dati delle librerie
            // Per ora lasciamo vuoto, può essere implementato quando necessario
            data.userData.libraries = new ArrayList<>();
        } catch (Exception e) {
            logger.warning("Impossibile raccogliere dati librerie: " + e.getMessage());
            data.userData.libraries = new ArrayList<>();
        }

        // Impostazioni applicazione
        try {
            data.appSettings = new AppSettings();
            data.appSettings.darkMode = ThemeManager.isDarkModeEnabled();
            data.appSettings.language = LanguageManager.getLanguage();
        } catch (Exception e) {
            logger.warning("Impossibile raccogliere impostazioni app: " + e.getMessage());
            data.appSettings = new AppSettings();
        }

        return data;
    }

    /**
     * Aggiunge file aggiuntivi al backup (immagini, ecc.)
     */
    private static void addAdditionalFiles(ZipOutputStream zos, String userId) throws IOException {
        // Per ora, possiamo aggiungere placeholder per future implementazioni
        // Es: immagini profilo, file di configurazione personalizzati, ecc.
    }

    /**
     * Ripristina dati da un file di backup
     */
    public static void restoreBackup(String backupFilePath, String userId) throws Exception {
        logger.info("Avvio restore backup: " + backupFilePath + " per utente: " + userId);

        Path backupFile = Paths.get(backupFilePath);
        if (!Files.exists(backupFile)) {
            throw new FileNotFoundException("File di backup non trovato: " + backupFilePath);
        }

        BackupData backupData = null;

        // Leggi file ZIP
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(backupFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().equals("backup_data.json")) {
                    backupData = objectMapper.readValue(zis, BackupData.class);
                    break;
                }
            }
        }

        if (backupData == null) {
            throw new IllegalStateException("Dati di backup non trovati nel file");
        }

        // Verifica che il backup sia per l'utente corretto
        if (!userId.equals(backupData.userId)) {
            throw new SecurityException("Il backup non appartiene all'utente specificato");
        }

        // Ripristina dati
        restoreBackupData(backupData);

        logger.info("Restore backup completato con successo");
    }

    /**
     * Ripristina dati da un file di backup crittografato
     */
    public static void restoreEncryptedBackup(String backupFilePath, String userId, String password) throws Exception {
        logger.info("Avvio restore backup crittografato: " + backupFilePath + " per utente: " + userId);

        Path backupFile = Paths.get(backupFilePath);
        if (!Files.exists(backupFile)) {
            throw new FileNotFoundException("File di backup non trovato: " + backupFilePath);
        }

        BackupData backupData = null;

        // Leggi file ZIP
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(backupFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().equals("backup_data.json")) {
                    backupData = objectMapper.readValue(zis, BackupData.class);
                    break;
                }
            }
        }

        if (backupData == null) {
            throw new IllegalStateException("Dati di backup non trovati nel file");
        }

        // Verifica che il backup sia crittografato
        if (!backupData.encrypted || backupData.encryptedUserData == null) {
            throw new IllegalStateException("Il backup non è crittografato");
        }

        // Verifica che il backup sia per l'utente corretto
        if (!userId.equals(backupData.userId)) {
            throw new SecurityException("Il backup non appartiene all'utente specificato");
        }

        // Decrittografa i dati utente
        try {
            String decryptedUserDataJson = EncryptionManager.decrypt(backupData.encryptedUserData, password);
            backupData.userData = objectMapper.readValue(decryptedUserDataJson, UserData.class);
        } catch (Exception e) {
            throw new SecurityException("Password errata o file di backup corrotto");
        }

        // Ripristina dati
        restoreBackupData(backupData);

        logger.info("Restore backup crittografato completato con successo");
    }

    /**
     * Ripristina i dati dal backup
     */
    private static void restoreBackupData(BackupData data) {
        // Ripristina libri recenti
        if (data.userData != null && data.userData.recentlyOpenedBookIds != null) {
            // Nota: Per ora, non ripristiniamo i libri recenti dato che abbiamo solo gli ID
            // In un'implementazione completa, dovremmo recuperare i Book dal database
            // SessionManager.getInstance().setRecentlyOpenedBooks(data.userData.recentlyOpenedBookIds);
        }

        // Ripristina impostazioni tema
        if (data.appSettings != null) {
            if (data.appSettings.darkMode != ThemeManager.isDarkModeEnabled()) {
                // Nota: Per cambiare tema servirebbe accesso alla Scene corrente
                // Questo può essere gestito dal chiamante
                logger.info("Tema da ripristinare: " + (data.appSettings.darkMode ? "dark" : "light"));
            }

            // Altri settings possono essere ripristinati qui
        }
    }

    /**
     * Lista tutti i backup disponibili per un utente
     */
    public static List<BackupInfo> listBackups(String userId) throws IOException {
        List<BackupInfo> backups = new ArrayList<>();
        Path backupDir = Paths.get(BACKUP_DIR);

        if (!Files.exists(backupDir)) {
            return backups;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(backupDir, userId + "_*" + BACKUP_EXTENSION)) {
            for (Path backupFile : stream) {
                try {
                    BackupInfo info = getBackupInfo(backupFile);
                    if (info != null) {
                        backups.add(info);
                    }
                } catch (IOException | SecurityException e) {
                    logger.warning("Errore nell'accesso al file di backup: " + backupFile + " - " + e.getMessage());
                } catch (Exception e) {
                    logger.warning("Errore imprevisto nella lettura del backup: " + backupFile + " - " + e.getMessage());
                }
            }
        }

        // Ordina per data decrescente (più recenti prima)
        backups.sort((a, b) -> b.backupDate.compareTo(a.backupDate));

        return backups;
    }

    /**
     * Informazioni su un backup
     */
    public static class BackupInfo {
        public String fileName;
        public String filePath;
        public LocalDateTime backupDate;
        public long fileSize;
        public String userId;
        public boolean encrypted = false;

        @Override
        public String toString() {
            String type = encrypted ? "Crittografato" : "Standard";
            return String.format("Backup %s: %s (%s, %d KB)",
                type,
                fileName,
                backupDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                fileSize / 1024);
        }
    }

    /**
     * Ottiene informazioni su un file di backup
     */
    private static BackupInfo getBackupInfo(Path backupFile) throws IOException {
        BackupInfo info = new BackupInfo();
        info.fileName = backupFile.getFileName().toString();
        info.filePath = backupFile.toString();
        info.fileSize = Files.size(backupFile);

        // Estrai data dal nome file (formato: userId[_encrypted]_yyyyMMdd_HHmmss.bkp.zip)
        String fileName = info.fileName;
        int firstUnderscore = fileName.indexOf('_');
        int dateStart = -1;
        int dateEnd = fileName.lastIndexOf(BACKUP_EXTENSION);

        if (firstUnderscore > 0) {
            // Controlla se è encrypted
            String afterUserId = fileName.substring(firstUnderscore + 1);
            if (afterUserId.startsWith("encrypted_")) {
                info.encrypted = true;
                dateStart = firstUnderscore + 11; // "_encrypted_"
            } else {
                dateStart = firstUnderscore + 1;
            }

            // Estrai userId
            info.userId = fileName.substring(0, firstUnderscore);

            // Estrai data
            if (dateStart > 0 && dateEnd > dateStart) {
                String dateStr = fileName.substring(dateStart, dateEnd);
                try {
                    info.backupDate = LocalDateTime.parse(dateStr, DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                } catch (Exception e) {
                    // Fallback: usa data modifica file
                    info.backupDate = LocalDateTime.now();
                }
            }
        }

        // Verifica crittografia leggendo il file (fallback)
        if (!info.encrypted) {
            try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(backupFile))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (entry.getName().equals("backup_data.json")) {
                        BackupData backupData = objectMapper.readValue(zis, BackupData.class);
                        info.encrypted = backupData.encrypted;
                        break;
                    }
                }
            } catch (Exception e) {
                // Ignora errori, mantiene info.encrypted = false
            }
        }

        return info;
    }

    /**
     * Elimina un backup
     */
    public static void deleteBackup(String backupFilePath) throws IOException {
        Path backupFile = Paths.get(backupFilePath);
        Files.deleteIfExists(backupFile);
        logger.info("Backup eliminato: " + backupFilePath);
    }

    /**
     * Ottiene il percorso della directory dei backup
     */
    public static String getBackupDirectory() {
        return BACKUP_DIR;
    }

    /**
     * Apre la cartella dei backup nel file manager del sistema
     */
    public static void openBackupFolder() throws Exception {
        Path backupDir = Paths.get(BACKUP_DIR);
        Files.createDirectories(backupDir);

        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(backupDir.toFile());
        } else {
            throw new UnsupportedOperationException("Desktop operations not supported on this system");
        }
    }

    /**
     * Verifica se la directory dei backup è accessibile
     */
    public static boolean isBackupDirectoryAccessible() {
        try {
            Path backupDir = Paths.get(BACKUP_DIR);
            Files.createDirectories(backupDir);
            // Test scrittura
            Path testFile = backupDir.resolve("test.tmp");
            Files.write(testFile, "test".getBytes());
            Files.delete(testFile);
            return true;
        } catch (Exception e) {
            logger.warning("Directory backup non accessibile: " + e.getMessage());
            return false;
        }
    }
}