-- Database optimization script for BookRecommender
-- Add strategic indexes for improved query performance

-- Index for title searches (ILIKE operations)
CREATE INDEX IF NOT EXISTS idx_books_title ON Books USING gin (to_tsvector('english', title));

-- Index for author searches (ILIKE operations)
CREATE INDEX IF NOT EXISTS idx_books_authors ON Books USING gin (to_tsvector('english', authors));

-- Index for category filtering
CREATE INDEX IF NOT EXISTS idx_books_category ON Books (category);

-- Index for publisher searches
CREATE INDEX IF NOT EXISTS idx_books_publisher ON Books (publisher);

-- Composite index for author + year queries
CREATE INDEX IF NOT EXISTS idx_books_author_year ON Books (authors, publish_date_year);

-- Composite index for title + year queries
CREATE INDEX IF NOT EXISTS idx_books_title_year ON Books (title, publish_date_year);

-- Index for year-based queries
CREATE INDEX IF NOT EXISTS idx_books_publish_year ON Books (publish_date_year);

-- Index for primary key (already exists, but ensuring it's optimized)
-- CREATE UNIQUE INDEX IF NOT EXISTS idx_books_id ON Books (book_id);

-- Index for Books_Libraries junction table
CREATE INDEX IF NOT EXISTS idx_books_libraries_book_id ON Books_Libraries (book_id);
CREATE INDEX IF NOT EXISTS idx_books_libraries_library_id ON Books_Libraries (libraries_id);

-- Index for Suggestions table
CREATE INDEX IF NOT EXISTS idx_suggestions_base_book ON Suggestions (base_book_id);
CREATE INDEX IF NOT EXISTS idx_suggestions_suggested_book ON Suggestions (suggested_book_id);

-- Index for book_reviews table
CREATE INDEX IF NOT EXISTS idx_book_reviews_book_id ON book_reviews (book_id);
CREATE INDEX IF NOT EXISTS idx_book_reviews_user_id ON book_reviews (user_id);

-- Composite index for user-book reviews
CREATE INDEX IF NOT EXISTS idx_book_reviews_user_book ON book_reviews (user_id, book_id);

-- Index for Users table
CREATE INDEX IF NOT EXISTS idx_users_username ON Users (username);
CREATE INDEX IF NOT EXISTS idx_users_email ON Users (email);

-- Index for Libraries table
CREATE INDEX IF NOT EXISTS idx_libraries_name ON Libraries (name);
CREATE INDEX IF NOT EXISTS idx_libraries_owner ON Libraries (owner_id);

-- Analyze tables to update statistics
ANALYZE Books;
ANALYZE Books_Libraries;
ANALYZE Suggestions;
ANALYZE book_reviews;
ANALYZE Users;
ANALYZE Libraries;