package it.uninsubria.server.db;
import java.sql.Statement;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.stream.Collectors;
import java.util.logging.Logger;

import it.uninsubria.server.util.ConnectionPoolManager;
import it.uninsubria.shared.utils.AppConstants;

/**
 * Database initialization utility for setting up and managing the database schema.
 */
public class DBInitializer{

    private static final Logger logger = Logger.getLogger(DBInitializer.class.getName());

     /**
      * Ricrea le tabelle problematiche (Book_Reviews e Suggested_Books) per applicare le correzioni allo schema
      *
      * @throws Exception if database operations fail
      */
     public static void recreateProblematicTables() throws Exception {
        logger.info("🔄 Ricreando tabelle problematiche per applicare correzioni allo schema...");

        Connection conn = null;
        try {
            conn = ConnectionPoolManager.getConnection();

            try (Statement stmt = conn.createStatement()) {
                // Drop delle tabelle problematiche se esistono
                stmt.execute("DROP TABLE IF EXISTS Suggested_Books CASCADE;");
                stmt.execute("DROP TABLE IF EXISTS Book_Reviews CASCADE;");
                // NON droppare Library perché è necessaria per i dati di test

                logger.info("✅ Vecchie tabelle eliminate, ricreando con schema corretto...");

                // Ricrea le tabelle usando gli script corretti
                String createTablesSql = "-- Ricrea Book_Reviews con schema corretto\n" +
                    "CREATE TABLE IF NOT EXISTS Book_Reviews (\n" +
                    "    book_reviews_id SERIAL PRIMARY KEY,\n" +
                    "    user_id INT NOT NULL,\n" +
                    "    book_id INT NOT NULL,\n" +
                    "    style INT CHECK (style BETWEEN 1 AND 5),\n" +
                    "    style_note TEXT CHECK (char_length(style_note) <= 256),\n" +
                    "    content INT CHECK (content BETWEEN 1 AND 5),\n" +
                    "    content_note TEXT CHECK (char_length(content_note) <= 256),\n" +
                    "    pleasentness INT CHECK (pleasentness BETWEEN 1 AND 5),\n" +
                    "    pleasentness_note TEXT CHECK (char_length(pleasentness_note) <= 256),\n" +
                    "    odness INT CHECK (odness BETWEEN 1 AND 5),\n" +
                    "    odness_note TEXT CHECK (char_length(odness_note) <= 256),\n" +
                    "    editions INT CHECK (editions BETWEEN 1 AND 5),\n" +
                    "    editions_note TEXT CHECK (char_length(editions_note) <= 256),\n" +
                    "    CONSTRAINT fk_review_user FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE ON UPDATE CASCADE,\n" +
                    "    CONSTRAINT fk_review_book FOREIGN KEY (book_id) REFERENCES Books(book_id) ON DELETE CASCADE ON UPDATE CASCADE\n" +
                    ");\n" +
                    "-- Ricrea Suggested_Books\n" +
                    "CREATE TABLE IF NOT EXISTS Suggested_Books (\n" +
                    "    id SERIAL PRIMARY KEY,\n" +
                    "    suggested_id INT NOT NULL,\n" +
                    "    user_id INT NOT NULL,\n" +
                    "    libraries_id INT NOT NULL,\n" +
                    "    base_book_id INT NOT NULL,\n" +
                    "    suggested_book_id INT NOT NULL,\n" +
                    "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n" +
                    "    CONSTRAINT fk_sugg_user FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE ON UPDATE CASCADE,\n" +
                    "    CONSTRAINT fk_sugg_library FOREIGN KEY (libraries_id) REFERENCES Library(library_id) ON DELETE CASCADE ON UPDATE CASCADE,\n" +
                    "    CONSTRAINT fk_sugg_base FOREIGN KEY (base_book_id) REFERENCES Books(book_id) ON DELETE CASCADE ON UPDATE CASCADE,\n" +
                    "    CONSTRAINT fk_sugg_suggested FOREIGN KEY (suggested_book_id) REFERENCES Books(book_id) ON DELETE CASCADE ON UPDATE CASCADE\n" +
                    ");\n" +
                    "CREATE UNIQUE INDEX IF NOT EXISTS idx_library_user_name ON Library(user_id, name_library);\n" +
                    "CREATE INDEX IF NOT EXISTS idx_suggestion_user_library ON Suggested_Books(user_id, libraries_id);";

                stmt.execute(createTablesSql);

                // Reset sequenze per le tabelle ricreate
                stmt.execute("ALTER SEQUENCE IF EXISTS book_reviews_book_reviews_id_seq RESTART WITH 1;");
                stmt.execute("ALTER SEQUENCE IF EXISTS Suggested_Books_id_seq RESTART WITH 1;");

                // Assicurati che esistano utenti e libreria di test (solo se non esistono)
                try (ResultSet userRs = stmt.executeQuery("SELECT COUNT(*) FROM Users")) {
                    if (userRs.next() && userRs.getInt(1) == 0) {
                        stmt.execute("INSERT INTO Users (names, surnames, CF, email, userid, passwords) VALUES " +
                            "('Mario', 'Rossi', 'RSSMRI80A01H501A', 'mario.rossi@example.com', 'mario', '$2a$10$example.hash'), " +
                            "('Luca', 'Verdi', 'VRDLCA85B02H501B', 'luca.verdi@example.com', 'luca', '$2a$10$example.hash');");
                        logger.info("Utenti di test creati in recreateProblematicTables");
                    }
                }

                try (ResultSet libRs = stmt.executeQuery("SELECT COUNT(*) FROM Library")) {
                    if (libRs.next() && libRs.getInt(1) == 0) {
                        stmt.execute("INSERT INTO Library (user_id, name_library) VALUES (1, 'Libreria Personale');");
                        logger.info("Libreria di test creata in recreateProblematicTables");
                    }
                }

                logger.info("✅ Tabelle ricreate con schema corretto, sequenze resettate e libreria di test creata");
            }

        } finally {
            if (conn != null) {
                conn.close();
            }
        }
    }

     /**
      * Popola il database con dati di test per suggerimenti e recensioni
      *
      * @throws Exception if database operations fail
      */
     public static void populateTestData() throws Exception {
        logger.info("Popolando dati di test per suggerimenti e recensioni...");

        Connection conn = null;
        try {
            conn = ConnectionPoolManager.getConnection();

            // Controlla se il database è già popolato (utenti + suggerimenti esistenti)
            boolean isDatabasePopulated = false;
            try (Statement checkStmt = conn.createStatement();
                 ResultSet rs = checkStmt.executeQuery("SELECT COUNT(*) FROM Users")) {
                if (rs.next() && rs.getInt(1) > 0) {
                    // Controlla anche se ci sono suggerimenti esistenti (NON dati di test)
                    try (ResultSet suggRs = checkStmt.executeQuery("SELECT COUNT(*) FROM Suggested_Books WHERE user_id != 1")) {
                        if (suggRs.next() && suggRs.getInt(1) > 0) {
                            isDatabasePopulated = true;
                            logger.info("Database già popolato con dati utente reali - salto inserimento dati di test");
                        }
                    }
                }
            }

            if (!isDatabasePopulated) {
                // Dati di test per Users
                String usersSql = "INSERT INTO Users (names, surnames, CF, email, userid, passwords) VALUES " +
                    "('Mario', 'Rossi', 'RSSMRI80A01H501A', 'mario.rossi@example.com', 'mario', '$2a$10$example.hash'), " +
                    "('Luca', 'Verdi', 'VRDLCA85B02H501B', 'luca.verdi@example.com', 'luca', '$2a$10$example.hash');";

                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(usersSql);
                    logger.info("Utenti di test creati.");
                }
            }

            if (!isDatabasePopulated) {
                // Dati di test per Library
                String librarySql = "INSERT INTO Library (user_id, name_library) VALUES (1, 'Libreria Personale');";

                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(librarySql);
                    logger.info("Libreria di test creata.");
                }

                // Dati di test per Suggested_Books
                String suggestionsSql = "INSERT INTO Suggested_Books (suggested_id, user_id, libraries_id, base_book_id, suggested_book_id) VALUES " +
                    "(1, 1, 1, 1, 2), (1, 1, 1, 1, 3), (1, 1, 1, 1, 4), (1, 1, 1, 2, 1), (1, 1, 1, 2, 3), " +
                    "(1, 1, 1, 3, 1), (1, 1, 1, 3, 2), (1, 1, 1, 4, 5), (1, 1, 1, 5, 4);";

                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(suggestionsSql);
                    logger.info("Dati suggerimenti inseriti.");
                }

                // Dati di test per Book_Reviews
                String reviewsSql = "INSERT INTO Book_Reviews (user_id, book_id, libraries_id, style, style_note, content, content_note, pleasentness, pleasentness_note, odness, odness_note, editions, editions_note) VALUES " +
                    "(1, 1, 1, 4, 'Ottimo stile narrativo, coinvolgente fin dalle prime pagine', 5, 'Contenuto profondo e ben strutturato', 4, 'Lettura piacevole e scorrevole', 4, 'Trama originale con colpi di scena interessanti', 4, 'Edizione ben curata'), " +
                    "(1, 2, 1, 4, 'Stile elegante e raffinato', 4, 'Contenuto interessante ma non innovativo', 3, 'Lettura piacevole ma non coinvolgente', 4, 'Trama abbastanza originale', 4, 'Edizione molto curata'), " +
                    "(1, 3, 1, 5, 'Stile magistrale, scrittura eccezionale', 5, 'Contenuto profondo e significativo', 5, 'Lettura estremamente piacevole', 4, 'Trama molto originale e innovativa', 4, 'Edizione buona'), " +
                    "(2, 1, 1, 3, 'Stile semplice ma efficace', 4, 'Contenuto buono', 3, 'Lettura discreta', 3, 'Trama non molto originale', 4, 'Edizione standard'), " +
                    "(2, 4, 1, 4, 'Stile coinvolgente', 5, 'Contenuto eccellente', 4, 'Lettura molto piacevole', 4, 'Trama originale', 4, 'Edizione ottima');";

                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(reviewsSql);
                    logger.info("Dati recensioni inseriti.");
                }
            }

            logger.info("Dati di test popolati con successo!");

        } catch (Exception e) {
            logger.severe("Errore durante il popolamento dei dati di test: " + e.getMessage());
            throw e;
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
    }


    // Old method removed - now using DataSeeder.seedBooksFromCSV()



    /**
     * Initializes the database with the given connection parameters.
     *
     * @param host the database host
     * @param dbName the database name
     * @param user the database user
     * @param password the database password
     * @throws Exception if initialization fails
     */
    public static void initialize(String host, String dbName, String user, String password) throws Exception{
        String url = "jdbc:postgresql://" + host + ":" + AppConstants.Network.POSTGRESQL_DEFAULT_PORT + "/" + dbName;

        // Initialize connection pool first
        ConnectionPoolManager.initialize(url, user, password);

        // Get a connection for initialization and ensure it's closed after use
        Connection conn = null;
        try {
            conn = ConnectionPoolManager.getConnection();

            // Ensure pg_trgm extension is enabled for fuzzy search
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE EXTENSION IF NOT EXISTS pg_trgm;");
                logger.info("pg_trgm extension enabled.");
            } catch (SQLException e) {
                logger.warning("Errore nell'abilitare pg_trgm: " + e.getMessage());
            }

            logger.info("Connessione DB riuscita. Inizializzo struttura...");

            String sql;
            try(BufferedReader reader = new BufferedReader(
                new InputStreamReader(DBInitializer.class.getClassLoader().getResourceAsStream("init.sql")))){

                sql = reader.lines().collect(Collectors.joining("\n"));

                try(Statement stmt = conn.createStatement()){
                    stmt.execute(sql);
                }

                try(Statement alter = conn.createStatement()){
                    alter.executeUpdate("ALTER TABLE users ADD COLUMN IF NOT EXISTS profile_image BYTEA");
                    alter.executeUpdate("ALTER TABLE users ADD COLUMN IF NOT EXISTS profile_image_type TEXT");
                    logger.info("✅ Controllo colonne Users completato.");
                } catch (SQLException e) {
                    logger.warning("Errore nell'aggiornamento schema: " + e.getMessage());
                }
            }

            // Ensure optional schema changes are present (e.g., new columns)
            try {
                ensureSchemaSync(conn);
            } catch (java.sql.SQLException e) {
                logger.warning("Errore during ensureSchemaSync: " + e.getMessage());
            }

            // Initialize user interactions schema for recommendations (sempre, anche su DB esistenti)
            try {
                initializeUserInteractionsSchema(conn);
            } catch (SQLException e) {
                logger.severe("❌ ERRORE CRITICO nell'inizializzazione schema user_interactions: " + e.getMessage());
                throw e; // Rilancia per bloccare l'avvio se manca questa tabella essenziale
            }

            if(DataSeeder.isBooksTableEmpty(conn)){
                DataSeeder.seedBooksFromCSV(conn);
            }
        } catch (Exception e) {
            logger.severe("Errore nell'inizializzazione del database: " + e.getMessage());
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    logger.warning("Errore nella chiusura della connessione: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Initialize user interactions schema for recommendation system
     * Verifica se la tabella esiste già prima di crearla
     */
    private static void initializeUserInteractionsSchema(Connection conn) throws SQLException {
        logger.info("🔍 Verifica/inizializzazione schema user_interactions per il sistema di raccomandazioni...");

        try {
            // Verifica se la tabella user_interactions esiste già
            try (Statement checkStmt = conn.createStatement();
                 ResultSet rs = checkStmt.executeQuery(
                     "SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'user_interactions')")) {

                if (rs.next() && rs.getBoolean(1)) {
                    logger.info("✅ Tabella user_interactions già esistente - skip inizializzazione.");
                    return;
                }
            }

            logger.info("📝 Tabella user_interactions non trovata, procedo con la creazione...");

            // Verifica che il file SQL esista
            var resourceStream = DBInitializer.class.getClassLoader().getResourceAsStream("user_interactions_schema.sql");
            if (resourceStream == null) {
                throw new SQLException("File user_interactions_schema.sql non trovato nelle risorse!");
            }

            // Leggi il file SQL
            String sql;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resourceStream))) {
                sql = reader.lines().collect(Collectors.joining("\n"));
            }

            if (sql == null || sql.trim().isEmpty()) {
                throw new SQLException("File user_interactions_schema.sql è vuoto!");
            }

            logger.info("📄 Schema SQL letto (" + sql.length() + " caratteri), eseguo creazione tabella...");

            // Esegui lo schema SQL
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
                logger.info("✅ Schema user_interactions inizializzato con successo!");

                // Verifica che la tabella sia stata creata
                try (Statement verifyStmt = conn.createStatement();
                     ResultSet verifyRs = verifyStmt.executeQuery(
                        "SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'user_interactions')")) {
                    if (verifyRs.next() && verifyRs.getBoolean(1)) {
                        logger.info("✅ Verifica creazione tabella user_interactions: SUCCESSO");
                    } else {
                        throw new SQLException("Tabella user_interactions non trovata dopo la creazione!");
                    }
                }
            }

        } catch (Exception e) {
            logger.severe("❌ ERRORE nell'inizializzazione schema user_interactions: " + e.getMessage());
            if (e.getCause() != null) {
                logger.severe("   Causa: " + e.getCause().getMessage());
            }
            throw new SQLException("Failed to initialize user_interactions schema: " + e.getMessage(), e);
        }
    }

    /**
     * Clears all data from the database tables.
     *
     * @param conn the database connection
     * @throws SQLException if database operations fail
     */
    public static void clearDatabase(Connection conn) throws SQLException {
        logger.info("Pulizia del database in corso...");

        String[] tables = { "Books_Libraries", "Book_Reviews", "Suggested_Books", "Library", "Books", "Users" };
        try (Statement stmt = conn.createStatement()) {
            for (String table : tables) {
                stmt.executeUpdate("DELETE FROM " + table);
                stmt.executeUpdate("ALTER TABLE IF EXISTS Book_Reviews DROP COLUMN IF EXISTS voto_finale");
                stmt.executeUpdate("ALTER TABLE IF EXISTS Book_Reviews DROP COLUMN IF EXISTS library_id");
                stmt.executeUpdate("ALTER TABLE IF EXISTS Book_Reviews DROP COLUMN IF EXISTS libraries_id");
            }

            // Reset delle sequenze ID
            String[] sequences = {
                "books_book_id_seq",
                "users_user_id_seq",
                "library_library_id_seq",
                "book_reviews_book_reviews_id_seq",
                "Suggested_Books_id_seq"
            };
            for (String seq : sequences) {
                stmt.executeUpdate("ALTER SEQUENCE " + seq + " RESTART WITH 1");
            }

        }
        logger.info("Database svuotato e sequenze resettate.");
    }

    /**
     * Reloads books data from CSV file.
     *
     * @param conn the database connection
     * @throws Exception if CSV loading fails
     */
    public static void reloadBooksFromCSV(Connection conn) throws Exception {
        DataSeeder.seedBooksFromCSV(conn); // usa il nuovo DataSeeder
    }
    
    /**
     * Closes the connection pool and cleans up resources.
     */
    public static void shutdown() {
        ConnectionPoolManager.shutdown();
    }

    /**
     * Ensures the database schema is synchronized with the required columns.
     *
     * @param conn the database connection
     * @throws java.sql.SQLException if schema operations fail
     */
    public static void ensureSchemaSync(Connection conn) throws java.sql.SQLException {
        boolean needCols = false;
        String checkFinalScore = "SELECT 1 FROM information_schema.columns WHERE table_name = 'book_reviews' AND column_name = 'final_score'";
        try (java.sql.PreparedStatement ps = conn.prepareStatement(checkFinalScore);
             java.sql.ResultSet rs = ps.executeQuery()) {
            if (! rs.next()) {
                needCols = true;
            }
        } catch (java.sql.SQLException e) {
            // If metadata query fails, default to attempting to add columns
            needCols = true;
        }
        if (needCols) {
            try (java.sql.Statement st = conn.createStatement()) {
                st.executeUpdate("ALTER TABLE IF EXISTS Book_Reviews ADD COLUMN IF NOT EXISTS final_score INT");
                st.executeUpdate("ALTER TABLE IF EXISTS Book_Reviews ADD COLUMN IF NOT EXISTS final_note_score TEXT");
                st.executeUpdate("UPDATE Book_Reviews SET final_score = ROUND((style + content + pleasentness + odness + editions) / 5.0) WHERE final_score IS NULL");
            }
        }
    }

    /**
     * Applies the initial SQL script to set up the database schema.
     *
     * @param conn the database connection
     * @throws Exception if SQL execution fails
     */
    public static void applyInitSQL(Connection conn) throws Exception {
        String sql;
        try (java.io.BufferedReader reader = new java.io.BufferedReader(
            new java.io.InputStreamReader(DBInitializer.class.getClassLoader().getResourceAsStream("init.sql")))) {
            sql = reader.lines().collect(java.util.stream.Collectors.joining("\n"));
        }
        try (java.sql.Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }
}
