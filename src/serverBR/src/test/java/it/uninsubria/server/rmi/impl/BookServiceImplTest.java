package it.uninsubria.server.rmi.impl;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import it.uninsubria.server.dao.BookDAO;
import it.uninsubria.shared.model.Book;
import it.uninsubria.shared.model.ReviewManager;

public class BookServiceImplTest {
    static class FakeBookDAO implements BookDAO {
        int findAllCalls = 0;
        @Override public Book bookDetails(int book_id) throws Exception { throw new UnsupportedOperationException(); }
        @Override public List<Book> findByTitle(String title) throws Exception { throw new UnsupportedOperationException(); }
        @Override public List<Book> findByAuthor(String author) throws Exception { throw new UnsupportedOperationException(); }
        @Override public List<Book> findByAuthorAndYear(String autore, int anno) throws Exception { throw new UnsupportedOperationException(); }
        @Override public Book findByID(int id) throws Exception { throw new UnsupportedOperationException(); }
        @Override public List<Book> findAll() throws Exception { findAllCalls++; List<Book> l = new ArrayList<>(); l.add(new Book(1, "TestTitle", "TestAuthor", 2025)); return l; }
        @Override public List<Book> getSuggestedBooks(int idLibro) throws Exception {
            List<Book> suggestions = new ArrayList<>();
            if (idLibro == 1) {
                suggestions.add(new Book(2, "Suggested Book 1", "Author 1", 2023));
                suggestions.add(new Book(3, "Suggested Book 2", "Author 2", 2024));
            }
            return suggestions;
        }
        @Override public List<ReviewManager> getAggregatedReviews(int idLibro) throws Exception { throw new UnsupportedOperationException(); }
        @Override public List<Book> findByTitleAndYear(String title, int anno) throws Exception { throw new UnsupportedOperationException("Unimplemented method"); }
        @Override public List<Book> getLibraryBooks(int libraryId) throws Exception { return new ArrayList<>(); }
        @Override public List<Book> findAllPaged(int offset, int limit) throws Exception { List<Book> l = new ArrayList<>(); l.add(new Book(1, "TestTitle", "TestAuthor", 2025)); return l; }
        @Override public List<Book> findByTitlePaged(String title, int offset, int limit) throws Exception { List<Book> l = new ArrayList<>(); l.add(new Book(1, "TestTitle", "TestAuthor", 2025)); return l; }
        @Override public List<Book> findByAuthorPaged(String author, int offset, int limit) throws Exception { List<Book> l = new ArrayList<>(); l.add(new Book(1, "TestTitle", "TestAuthor", 2025)); return l; }
        @Override public int getTotalBooksCount() throws Exception { return 1; }
    }

    @Test
    public void testCachingFindAll() throws Exception {
        // Clear cache before test to ensure clean state
        it.uninsubria.server.cache.CacheManager.clear();
        // Also clear disk cache
        try {
            java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get("./cache/global/books_all.cache"));
        } catch (Exception e) {
            // Ignore if file doesn't exist
        }

        // Test the service directly with a fake DAO (bypasses ServerDIContainer)
        FakeBookDAO dao = new FakeBookDAO();
        BookServiceImpl service = new BookServiceImpl(dao);

        List<Book> first = service.searchGlobally();
        assertEquals(1, first.size());
        assertEquals(1, dao.findAllCalls);
        List<Book> second = service.searchGlobally();
        assertEquals(1, dao.findAllCalls); // cached
        assertEquals(first, second);
    }

    @Test
    public void testGetSuggestedBooks() throws Exception {
        // Clear cache before test to ensure clean state
        it.uninsubria.server.cache.CacheManager.clear();
        // Also clear disk cache
        try {
            java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get("./cache/global/books_all.cache"));
        } catch (Exception e) {
            // Ignore if file doesn't exist
        }

        FakeBookDAO dao = new FakeBookDAO();
        BookServiceImpl service = new BookServiceImpl(dao);
        List<Book> suggestions = service.getSuggestedBooks(1);
        assertNotNull(suggestions);
        assertEquals(2, suggestions.size());
        assertEquals("Suggested Book 1", suggestions.get(0).getTitle());
        assertEquals("Suggested Book 2", suggestions.get(1).getTitle());
    }
}
