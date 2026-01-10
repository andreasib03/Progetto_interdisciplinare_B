package it.uninsubria.server.service;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import it.uninsubria.server.cache.CacheManager;
import it.uninsubria.server.dao.TestBookDAO;
import it.uninsubria.shared.model.Book;

public class BookServiceCoreTest {

    @Before
    public void setUp() {
        CacheManager.clear();
    }

    @After
    public void tearDown() {
        CacheManager.clear();
    }

    @Test
    public void testCachingFindAll() throws Exception {
        TestBookDAO dao = new TestBookDAO();
        BookServiceCore core = new BookServiceCoreImpl(dao);

        List<Book> first = core.searchGlobally();
        assertNotNull(first);
        assertEquals(1, first.size());
        assertEquals(1, dao.getFindAllCalls());

        List<Book> second = core.searchGlobally();
        assertEquals(1, dao.getFindAllCalls()); // should not call DAO again due to cache
        assertEquals(first, second);
    }

    @Test
    public void testInvalidateCacheReload() throws Exception {
        TestBookDAO dao = new TestBookDAO();
        BookServiceCore core = new BookServiceCoreImpl(dao);
        // First call populates cache
        core.searchGlobally();
        assertEquals(1, dao.getFindAllCalls());
        // Invalidate and fetch again
        core.invalidateBooksCache();
        core.searchGlobally();
        assertEquals(2, dao.getFindAllCalls());
    }

    @Test
    public void testSearchByTitleDelegation() throws Exception {
        TestBookDAO dao = new TestBookDAO();
        BookServiceCore core = new BookServiceCoreImpl(dao);
        List<Book> res = core.searchByTitle("Test");
        assertNotNull(res);
        assertEquals(1, dao.getFindByTitleCalls());
        assertEquals(1, res.size());
    }
}
