-- ============================================================================
-- Test Data Seeding Script - Book Recommender System
-- ============================================================================
--
-- Questo script crea dati di test per l'ambiente di sviluppo/testing.
-- Include: utente test, libreria, libri, recensioni, suggerimenti
--
-- IMPORTANTE: ESEGUIRE SOLO IN AMBIENTE DI TEST/SVILUPPO!
-- MAI ESEGUIRE IN PRODUZIONE!
--
-- ============================================================================

-- ============================================================================
-- INIZIALIZZAZIONE
-- ============================================================================

-- Attiva notifiche
DO $$
BEGIN
    RAISE NOTICE 'Inizio seeding dati di test...';
END $$ LANGUAGE plpgsql;

-- Pulisci dati di test esistenti (per idempotenza)
-- ============================================================================

DO $$
BEGIN
    -- Cancella utente test se esiste
    DELETE FROM Users WHERE userid = 'testProva';
    DELETE FROM Users WHERE userid = 'testUser2';

    -- Cancella dati correlati
    DELETE FROM Suggested_Books
      WHERE user_id IN (SELECT user_id FROM Users WHERE userid IN ('testProva', 'testUser2'));

    DELETE FROM Book_Reviews
      WHERE user_id IN (SELECT user_id FROM Users WHERE userid IN ('testProva', 'testUser2'));

    DELETE FROM Books_Libraries
      WHERE libraries_id IN (SELECT library_id FROM Library WHERE name_library = 'Libreria Test');

    DELETE FROM Library
      WHERE name_library = 'Libreria Test';

    RAISE NOTICE 'Puliti dati di test precedenti';
END $$ LANGUAGE plpgsql;

-- ============================================================================
-- STEP 1: CREAZIONE UTENTI TEST
-- ============================================================================

DO $$
BEGIN
    -- Utente test 1: testProva / Test123!
    -- Password: 'Test123!' hashata con BCrypt (esempio, in produzione generata real)
    INSERT INTO Users (names, surnames, CF, email, userid, passwords, passwordRecovery)
    VALUES (
        'Test',
        'User',
        'TSTUSR80A01H501T',
        'test@example.com',
        'testProva',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7yO/4j3YqZGz1', -- BCrypt hash di 'Test123!' (esempio)
        'recoveryphrase123'  -- In produzione, dovrebbe essere hashato
    )
    ON CONFLICT (userid) DO NOTHING;

    -- Utente test 2: testUser2 / Test456!
    INSERT INTO Users (names, surnames, CF, email, userid, passwords, passwordRecovery)
    VALUES (
        'Test',
        'User2',
        'TSTUSR81B01H501T',
        'test2@example.com',
        'testUser2',
        '$2a$10$rN3qo8uLOickgx2ZMRZoMyeIjZAgcfl7yO/4j3YqZGz2', -- BCrypt hash di 'Test456!' (esempio)
        'recoveryphrase456'
    )
    ON CONFLICT (userid) DO NOTHING;

    RAISE NOTICE 'Utenti test creati';
END $$ LANGUAGE plpgsql;

-- ============================================================================
-- STEP 2: CREAZIONE LIBRERIA TEST
-- ============================================================================

DO $$
DECLARE
    v_test_user1_id INTEGER;
    v_test_user2_id INTEGER;
    v_library_id INTEGER;
BEGIN
    -- Ottieni ID utenti test
    SELECT user_id INTO v_test_user1_id FROM Users WHERE userid = 'testProva';
    SELECT user_id INTO v_test_user2_id FROM Users WHERE userid = 'testUser2';

    -- Crea libreria per utente test 1
    INSERT INTO Library (user_id, name_library)
    VALUES (v_test_user1_id, 'Libreria Test')
    ON CONFLICT (user_id, name_library) DO NOTHING;

    -- Ottieni ID libreria
    SELECT library_id INTO v_library_id FROM Library WHERE user_id = v_test_user1_id AND name_library = 'Libreria Test';

    RAISE NOTICE 'Libreria test creata (ID: %)', v_library_id;
END $$ LANGUAGE plpgsql;

-- ============================================================================
-- STEP 3: AGGIUNTA LIBRI ALLA LIBRERIA
-- ============================================================================

-- Aggiunge libri ID 1-5 alla libreria test
-- Assumiamo che i libri 1-5 esistano già nel database
DO $$
DECLARE
    v_library_id INTEGER;
    v_books_added INTEGER;
BEGIN
    SELECT library_id INTO v_library_id FROM Library WHERE name_library = 'Libreria Test' LIMIT 1;

    -- Inserisci associazioni libreria-libro (se non esistono già)
    INSERT INTO Books_Libraries (libraries_id, book_id)
    SELECT v_library_id, book_id
    FROM (SELECT 1 as book_id UNION
           SELECT 2 UNION
           SELECT 3 UNION
           SELECT 4 UNION
           SELECT 5) AS books
    ON CONFLICT (libraries_id, book_id) DO NOTHING;

    GET DIAGNOSTICS v_books_added = ROW_COUNT;
    RAISE NOTICE 'Libri aggiunti alla libreria: %', v_books_added;
END $$ LANGUAGE plpgsql;

-- ============================================================================
-- STEP 4: CREAZIONE RECENSIONI TEST
-- ============================================================================

DO $$
DECLARE
    v_library_id INTEGER;
BEGIN
    SELECT library_id INTO v_library_id FROM Library WHERE name_library = 'Libreria Test' LIMIT 1;
    -- Ottieni user_id
    DECLARE v_user_id INTEGER;
    SELECT user_id INTO v_user_id FROM Users WHERE userid = 'testProva' LIMIT 1;

    -- Recensione per libro 1
    INSERT INTO Book_Reviews (
        user_id, book_id, libraries_id,
        style, style_note, content, content_note,
        pleasentness, pleasentness_note,
        odness, odness_note,
        editions, editions_note
    ) VALUES (
        v_user_id, 1, v_library_id,
        5, 'Stile coinvolgente', 5, 'Contenuto eccellente', 4,
        4, 'Lettura piacevole', 4, 'Trama originale', 4,
        'Edizione buona', 'Qualità carta ottima'
    )
    ON CONFLICT (user_id, book_id, libraries_id) DO NOTHING;

    -- Recensione per libro 2
    INSERT INTO Book_Reviews (
        user_id, book_id, libraries_id,
        style, style_note, content, content_note,
        pleasentness, pleasentness_note,
        odness, odness_note,
        editions, editions_note
    ) VALUES (
        v_user_id, 2, v_library_id,
        4, 'Stile semplice', 4, 'Contenuto buono', 3,
        3, 'Lettura discreta', 3, 'Trama standard', 4,
        'Edizione standard', 'Normale'
    )
    ON CONFLICT (user_id, book_id, libraries_id) DO NOTHING;

    -- Recensione per libro 3
    INSERT INTO Book_Reviews (
        user_id, book_id, libraries_id,
        style, style_note, content, content_note,
        pleasentness, pleasentness_note,
        odness, odness_note,
        editions, editions_note
    ) VALUES (
        v_user_id, 3, v_library_id,
        4, 'Stile elegante', 4, 'Contenuto interessante', 4,
        5, 'Molto piacevole', 5, 'Tema avvincente', 5,
        'Edizione pregiata', 'Brossura eccellente'
    )
    ON CONFLICT (user_id, book_id, libraries_id) DO NOTHING;

    RAISE NOTICE 'Recensioni test create';
END $$ LANGUAGE plpgsql;

-- ============================================================================
-- STEP 5: CREAZIONE SUGGERIMENTI TEST
-- ============================================================================

DO $$
DECLARE
    v_library_id INTEGER;
    v_suggested_id INTEGER;
BEGIN
    SELECT library_id INTO v_library_id FROM Library WHERE name_library = 'Libreria Test' LIMIT 1;
    -- Ottieni user_id
    DECLARE v_user_id INTEGER;
    SELECT user_id INTO v_user_id FROM Users WHERE userid = 'testProva' LIMIT 1;

    -- Suggerimento: Libro 1 → Libro 2
    INSERT INTO Suggested_Books (suggested_id, user_id, libraries_id, base_book_id, suggested_book_id)
    VALUES (1, v_user_id, v_library_id, 1, 2)
    ON CONFLICT (suggested_id, user_id, libraries_id, base_book_id, suggested_book_id) DO NOTHING;

    -- Suggerimento: Libro 1 → Libro 3
    INSERT INTO Suggested_Books (suggested_id, user_id, libraries_id, base_book_id, suggested_book_id)
    VALUES (2, v_user_id, v_library_id, 1, 3)
    ON CONFLICT (suggested_id, user_id, libraries_id, base_book_id, suggested_book_id) DO NOTHING;

    -- Suggerimento: Libro 2 → Libro 1
    INSERT INTO Suggested_Books (suggested_id, user_id, libraries_id, base_book_id, suggested_book_id)
    VALUES (3, v_user_id, v_library_id, 2, 1)
    ON CONFLICT (suggested_id, user_id, libraries_id, base_book_id, suggested_book_id) DO NOTHING;

    -- Suggerimento: Libro 3 → Libro 4
    INSERT INTO Suggested_Books (suggested_id, user_id, libraries_id, base_book_id, suggested_book_id)
    VALUES (4, v_user_id, v_library_id, 3, 4)
    ON CONFLICT (suggested_id, user_id, libraries_id, base_book_id, suggested_book_id) DO NOTHING;

    -- Suggerimento: Libro 3 → Libro 5
    INSERT INTO Suggested_Books (suggested_id, user_id, libraries_id, base_book_id, suggested_book_id)
    VALUES (5, v_user_id, v_library_id, 3, 5)
    ON CONFLICT (suggested_id, user_id, libraries_id, base_book_id, suggested_book_id) DO NOTHING;

    RAISE NOTICE 'Suggerimenti test creati';
END $$ LANGUAGE plpgsql;

-- ============================================================================
-- VERIFICAZIONE E REPORT
-- ============================================================================

DO $$
DECLARE
    v_user_count INTEGER;
    v_library_count INTEGER;
    v_review_count INTEGER;
    v_suggestion_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_user_count FROM Users WHERE userid LIKE 'test%';
    SELECT COUNT(*) INTO v_library_count FROM Library WHERE name_library = 'Libreria Test';
    SELECT COUNT(*) INTO v_review_count FROM Book_Reviews WHERE user_id IN (SELECT user_id FROM Users WHERE userid LIKE 'test%');
    SELECT COUNT(*) INTO v_suggestion_count FROM Suggested_Books WHERE user_id IN (SELECT user_id FROM Users WHERE userid LIKE 'test%');

    RAISE NOTICE '========================================';
    RAISE NOTICE 'SEEDING DATI DI TEST COMPLETATO';
    RAISE NOTICE '========================================';
    RAISE NOTICE 'Utenti test creati: %', v_user_count;
    RAISE NOTICE 'Librerie test create: %', v_library_count;
    RAISE NOTICE 'Recensioni test create: %', v_review_count;
    RAISE NOTICE 'Suggerimenti test creati: %', v_suggestion_count;
    RAISE NOTICE '========================================';

    RAISE NOTICE '';
    RAISE NOTICE 'CREDENZIALI UTENTE TEST 1:';
    RAISE NOTICE '  Username: testProva';
    RAISE NOTICE '  Password: Test123!';
    RAISE NOTICE '';
    RAISE NOTICE 'CREDENZIALI UTENTE TEST 2:';
    RAISE NOTICE '  Username: testUser2';
    RAISE NOTICE '  Password: Test456!';
    RAISE NOTICE '========================================';
END $$ LANGUAGE plpgsql;

-- ============================================================================
-- NOTE IMPORTANTI
-- ============================================================================

-- Password BCrypt usate in questo script sono ESEMPI
-- Per password reali, genera hash con:
-- Java: PasswordHashUtil.hashPassword("your_password")
-- Oppure usa online tool BCrypt: https://bcrypt-generator.com/

-- Libri ID 1-5 devono esistere nel database
-- Se non esistono, esegui prima lo script di importazione libri

-- ============================================================================
-- ISTRUZIONI DI USO
-- ============================================================================

COMMENT ON TABLE Users IS
'IMPORTANTE: Questo script crea dati di test. NON eseguire in produzione!';

-- Per eseguire questo script:
-- psql -U postgres -d bookrecommender -f seed_test_data.sql

-- Per eseguire come utente specifico:
-- psql -U postgres -d bookrecommender -f seed_test_data.sql

-- Per rimuovere dati di test:
-- psql -U postgres -d bookrecommender -c "DELETE FROM Users WHERE userid LIKE 'test%';"

-- ============================================================================
-- END OF SCRIPT
-- ============================================================================
