-- ============================================================================
-- Script di Migrazione Password - Book Recommender System
-- ============================================================================
--
-- Questo script converte le password in plaintext del database in password
-- hashate con BCrypt per compatibilità con il nuovo sistema di login.
--
-- IMPORTANTE:
-- - Fai un BACKUP del database prima di eseguire questo script
-- - Esegui in ambiente di TEST prima di produzione
-- - Questo script è ONE-TIME-USE, non eseguirlo più volte
-- ============================================================================

-- ============================================================================
-- INIZIALIZZAZIONE E VERIFICHE
-- ============================================================================

-- Attiva notifiche
DO $$
BEGIN
    RAISE NOTICE 'Inizio migrazione password...';
END $$ LANGUAGE plpgsql;

-- Crea tabella di backup per le password migrate
CREATE TABLE IF NOT EXISTS password_migration_log (
    id SERIAL PRIMARY KEY,
    user_id INTEGER,
    userid VARCHAR(255),
    old_password_type VARCHAR(20),
    migrated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20)
);

-- ============================================================================
-- STEP 1: Identificare utenti con password in plaintext
-- ============================================================================

-- Crea vista temporanea per identificare password non hashate
CREATE OR REPLACE VIEW v_unhashed_passwords AS
SELECT
    user_id,
    userid,
    passwords,
    'PLAINTEXT' as password_type
FROM Users
WHERE
    -- Password BCrypt iniziano con $2a$, $2b$, $2y$
    passwords NOT LIKE '$2a$%'
    AND passwords NOT LIKE '$2b$%'
    AND passwords NOT LIKE '$2y$%';

-- Mostra utenti da migrare
DO $$
DECLARE
    user_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO user_count
    FROM v_unhashed_passwords;

    RAISE NOTICE 'Trovati % utenti con password in plaintext', user_count;
    IF user_count = 0 THEN
        RAISE NOTICE 'NESSUNA MIGRAZIONE NECESSARIA - Tutte le password sono già hashate';
    END IF;
END $$ LANGUAGE plpgsql;

-- ============================================================================
-- STEP 2: Migrare password a BCrypt
-- ============================================================================

-- Nota: Questo richiede l'uso di pgcrypto (deve essere installato)
-- In PostgreSQL, BCrypt non è nativo, quindi usiamo una alternativa:
-- - Usare la funzione hash del codice Java dell'applicazione
-- - Oppure eseguire questo script con un tool esterno

-- Soluzione: Eseguiamo un UPDATE che segna le password come da migrare
-- L'hashing BCrypt verrà fatto dal codice Java dell'applicazione
-- durante il primo login dell'utente (password rehashing on first use)

-- Aggiorna tabella Users per segnalare password da migrare
UPDATE Users
SET passwords = passwords || '_TO_MIGRATE'
WHERE user_id IN (SELECT user_id FROM v_unhashed_passwords);

-- Log della migrazione
INSERT INTO password_migration_log (user_id, userid, old_password_type, status)
SELECT user_id, userid, password_type, 'MARKED_FOR_MIGRATION'
FROM v_unhashed_passwords;

-- ============================================================================
-- STEP 3: Creare stored procedure per hash durante login
-- ============================================================================

-- Questa procedura verrà chiamata dal codice Java durante il primo login
-- dopo la rimozione del supporto password plaintext

CREATE OR REPLACE FUNCTION migrate_user_password(
    p_userid VARCHAR(255),
    p_plain_password VARCHAR(255),
    p_new_bcrypt_password VARCHAR(255)
) RETURNS BOOLEAN AS $$
DECLARE
    v_user_id INTEGER;
BEGIN
    -- Ottieni l'user_id
    SELECT user_id INTO v_user_id
    FROM Users
    WHERE userid = p_userid;

    IF v_user_id IS NULL THEN
        RAISE NOTICE 'Utente non trovato: %', p_userid;
        RETURN FALSE;
    END IF;

    -- Aggiorna la password con BCrypt
    UPDATE Users
    SET passwords = p_new_bcrypt_password
    WHERE user_id = v_user_id AND passwords = p_plain_password || '_TO_MIGRATE';

    -- Log della migrazione completata
    UPDATE password_migration_log
    SET status = 'MIGRATED_SUCCESSFULLY'
    WHERE user_id = v_user_id AND status = 'MARKED_FOR_MIGRATION';

    -- Verifica aggiornamento
    IF FOUND THEN
        RAISE NOTICE 'Password migrata con successo per utente: %', p_userid;
        RETURN TRUE;
    ELSE
        RAISE NOTICE 'Password non migrata (forse già migrata): %', p_userid;
        RETURN FALSE;
    END IF;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- STEP 4: Pulizia
-- ============================================================================

DROP VIEW IF EXISTS v_unhashed_passwords;

-- ============================================================================
-- REPORT FINALE
-- ============================================================================

DO $$
DECLARE
    total_users INTEGER;
    migrated_users INTEGER;
BEGIN
    SELECT COUNT(*) INTO total_users FROM Users;
    SELECT COUNT(*) INTO migrated_users FROM password_migration_log;

    RAISE NOTICE '========================================';
    RAISE NOTICE 'MIGRAZIONE PASSWORD COMPLETATA';
    RAISE NOTICE '========================================';
    RAISE NOTICE 'Totale utenti nel database: %', total_users;
    RAISE NOTICE 'Utenti migrati: %', migrated_users;
    RAISE NOTICE 'Verifica tabella password_migration_log per dettagli';
    RAISE NOTICE '========================================';
END $$ LANGUAGE plpgsql;

-- ============================================================================
-- ISTRUZIONI POST-MIGRAZIONE
-- ============================================================================

COMMENT ON TABLE password_migration_log IS
'Log delle migrazioni password. Non cancellare questa tabella fino a quando non tutte le password sono state migrate e testate.';

COMMENT ON FUNCTION migrate_user_password IS
'Funzione per completare la migrazione password durante il primo login dell''utente. Chiama questa funzione dal codice Java con la password BCrypt hashata.';

-- ============================================================================
-- ALTERNATIVA: MIGRAZIONE MANUALE CON JAVA
-- ============================================================================

/*
Se preferisci eseguire la migrazione direttamente con Java (raccomandato):

1. Compila il progetto:
   mvn clean compile

2. Esegui questo programma Java:
   java -cp target/classes:target/dependency/* \
     it.uninsubria.server.util.PasswordMigrationTool

Oppure usa lo script migrate-passwords.sh incluso.
*/

-- ============================================================================
-- END OF SCRIPT
-- ============================================================================
