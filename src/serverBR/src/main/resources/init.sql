-- ===========================================
-- 1. TABELLA: Books
-- ===========================================

CREATE TABLE IF NOT EXISTS Books (
    book_id SERIAL PRIMARY KEY,
    title TEXT NOT NULL,
    authors TEXT NOT NULL,
    descriptions TEXT,
    category TEXT,
    publisher TEXT,
    price TEXT,
    publish_date_month TEXT,
    publish_date_year INT
);

-- ===========================================
-- 2. TABELLA: Users
-- ===========================================


CREATE TABLE IF NOT EXISTS Users (
    user_id SERIAL PRIMARY KEY,
    names TEXT NOT NULL,
    surnames TEXT NOT NULL,
    CF VARCHAR(16) UNIQUE NOT NULL,
    email TEXT UNIQUE NOT NULL,
    userid TEXT UNIQUE NOT NULL,
    passwords TEXT NOT NULL,
    profile_image BYTEA,
    profile_image_type TEXT
);

ALTER TABLE Users
    ADD COLUMN IF NOT EXISTS passwordRecovery TEXT;

-- ===========================================
-- 3. TABELLA: Library (1 user → molte librerie)
-- ===========================================

CREATE TABLE IF NOT EXISTS Library (
    library_id SERIAL PRIMARY KEY,
    name_library TEXT NOT NULL,
    user_id INT NOT NULL,

    CONSTRAINT fk_library_user FOREIGN KEY (user_id)
        REFERENCES Users(user_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    CONSTRAINT unique_user_library UNIQUE (user_id, name_library)
);

-- ===========================================
-- 4. TABELLA: Books_Libraries (N:M fra libri e librerie)
-- ===========================================

CREATE TABLE IF NOT EXISTS Books_Libraries (
    libraries_id INT NOT NULL,
    book_id INT NOT NULL,

    PRIMARY KEY (libraries_id, book_id),

    CONSTRAINT fk_bl_library FOREIGN KEY (libraries_id)
        REFERENCES Library(library_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    CONSTRAINT fk_bl_book FOREIGN KEY (book_id)
        REFERENCES Books(book_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

-- ===========================================
-- 5. TABELLA: Book_Reviews
-- ===========================================

CREATE TABLE IF NOT EXISTS Book_Reviews (
    book_reviews_id SERIAL PRIMARY KEY,
    user_id INT NOT NULL,
    book_id INT NOT NULL,

    style INT CHECK (style BETWEEN 1 AND 5),
    style_note TEXT CHECK (char_length(style_note) <= 256),

    content INT CHECK (content BETWEEN 1 AND 5),
    content_note TEXT CHECK (char_length(content_note) <= 256),

    pleasentness INT CHECK (pleasentness BETWEEN 1 AND 5),
    pleasentness_note TEXT CHECK (char_length(pleasentness_note) <= 256),

    odness INT CHECK (odness BETWEEN 1 AND 5),
    odness_note TEXT CHECK (char_length(odness_note) <= 256),

    editions INT CHECK (editions BETWEEN 1 AND 5),
    editions_note TEXT CHECK (char_length(editions_note) <= 256),

    CONSTRAINT fk_review_user FOREIGN KEY (user_id)
        REFERENCES Users(user_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    CONSTRAINT fk_review_book FOREIGN KEY (book_id)
        REFERENCES Books(book_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

-- Migrazioni idempotenti
ALTER TABLE Book_Reviews DROP CONSTRAINT IF EXISTS fk_review_libraries;
ALTER TABLE Book_Reviews DROP CONSTRAINT IF EXISTS unique_user_book_review;

ALTER TABLE Book_Reviews 
    ADD COLUMN IF NOT EXISTS libraries_id INT NOT NULL;

ALTER TABLE Book_Reviews
    ADD CONSTRAINT fk_review_libraries FOREIGN KEY (libraries_id, book_id)
        REFERENCES Books_Libraries(libraries_id, book_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE;

ALTER TABLE Book_Reviews
    ADD CONSTRAINT unique_user_book_review UNIQUE (user_id, book_id, libraries_id);

ALTER TABLE Book_Reviews
    ADD COLUMN IF NOT EXISTS final_score INT;

ALTER TABLE Book_Reviews
    ADD COLUMN IF NOT EXISTS final_note_score TEXT CHECK (char_length(final_note_score) <= 256);

UPDATE Book_Reviews
SET final_score = ROUND((style + content + pleasentness + odness + editions) / 5.0)
WHERE final_score IS NULL;

-- ===========================================
-- 6. TABELLA: Suggested_Books
-- ===========================================

CREATE TABLE IF NOT EXISTS Suggested_Books (
    suggested_id SERIAL PRIMARY KEY,

    user_id INT NOT NULL,
    libraries_id INT NOT NULL,

    base_book_id INT NOT NULL,
    suggested_book_id INT NOT NULL,

    CONSTRAINT fk_sugg_user FOREIGN KEY (user_id)
        REFERENCES Users(user_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    CONSTRAINT fk_sugg_library FOREIGN KEY (libraries_id)
        REFERENCES Library(library_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    CONSTRAINT fk_sugg_base FOREIGN KEY (base_book_id)
        REFERENCES Books(book_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    CONSTRAINT fk_sugg_suggested FOREIGN KEY (suggested_book_id)
        REFERENCES Books(book_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    -- Evita suggerire due volte la stessa cosa
    CONSTRAINT unique_user_suggestion UNIQUE (user_id, libraries_id, base_book_id, suggested_book_id),

    -- Evita suggerimenti ridicoli (stesso libro che si suggerisce da solo)
    CONSTRAINT chk_suggestion_not_self CHECK (base_book_id <> suggested_book_id)
);

-- Migrazione per correggere il nome della colonna chiave primaria
-- Se esiste una colonna 'id', rinominala in 'suggested_id'
DO $$
DECLARE
    column_count INTEGER;
BEGIN
    -- Conta quante colonne chiave abbiamo
    SELECT COUNT(*) INTO column_count
    FROM information_schema.columns
    WHERE table_name = 'suggested_books'
    AND column_name IN ('id', 'suggested_id')
    AND table_schema = 'public';

    -- Se abbiamo solo 'id', rinominala
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'suggested_books'
        AND column_name = 'id'
        AND table_schema = 'public'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'suggested_books'
        AND column_name = 'suggested_id'
        AND table_schema = 'public'
    ) THEN
        -- Rinomina 'id' in 'suggested_id'
        ALTER TABLE Suggested_Books RENAME COLUMN id TO suggested_id;
        RAISE NOTICE 'Colonna id rinominata in suggested_id nella tabella Suggested_Books';
    ELSIF column_count = 0 THEN
        -- Se non abbiamo nessuna colonna chiave, è un problema
        RAISE NOTICE 'ATTENZIONE: Tabella Suggested_Books non ha colonna chiave primaria!';
    END IF;
END $$;

ALTER TABLE Suggested_Books
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- ===========================================
-- 7. INDEX UTILI
-- ===========================================

CREATE UNIQUE INDEX IF NOT EXISTS idx_library_user_name
    ON Library(user_id, name_library);

CREATE INDEX IF NOT EXISTS idx_suggestion_user_library
    ON Suggested_Books(user_id, libraries_id);

-- ===========================================
-- 7. DATI DI TEST: Library
-- ===========================================


