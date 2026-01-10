-- Tabella per tracciare le interazioni utente-libro per raccomandazioni intelligenti
CREATE TABLE IF NOT EXISTS user_interactions (
    id SERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    book_id INTEGER NOT NULL,
    interaction_type VARCHAR(50) NOT NULL, -- VIEW, LIKE, DISLIKE, ADD_TO_LIBRARY, RATE, etc.
    rating INTEGER, -- 1-5 stelle, nullable
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    session_id VARCHAR(255),

    -- Vincolo per evitare duplicati dello stesso tipo per user-book
    UNIQUE(user_id, book_id, interaction_type)
);

-- Indici per performance
CREATE INDEX IF NOT EXISTS idx_user_book ON user_interactions (user_id, book_id);
CREATE INDEX IF NOT EXISTS idx_user_timestamp ON user_interactions (user_id, timestamp);
CREATE INDEX IF NOT EXISTS idx_interaction_type ON user_interactions (interaction_type);
CREATE INDEX IF NOT EXISTS idx_book_id ON user_interactions (book_id);

-- Tabella per cache delle similarità libri (opzionale, per performance)
CREATE TABLE IF NOT EXISTS book_similarities (
    id SERIAL PRIMARY KEY,
    book_id_1 INTEGER NOT NULL,
    book_id_2 INTEGER NOT NULL,
    similarity_score DOUBLE PRECISION NOT NULL,
    similarity_type VARCHAR(50) NOT NULL, -- AUTHOR, GENRE, DESCRIPTION, etc.
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Vincolo per evitare duplicati
    UNIQUE(book_id_1, book_id_2, similarity_type)
);

-- Indici per performance
CREATE INDEX IF NOT EXISTS idx_book1 ON book_similarities (book_id_1);
CREATE INDEX IF NOT EXISTS idx_book2 ON book_similarities (book_id_2);
CREATE INDEX IF NOT EXISTS idx_similarity ON book_similarities (similarity_score DESC);